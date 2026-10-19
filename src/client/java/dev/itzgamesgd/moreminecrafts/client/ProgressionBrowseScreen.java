package dev.itzgamesgd.moreminecrafts.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometry;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasInput;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionNodeLayoutCatalog;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionPurchasePolicy;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Separate Knowledge/Research screen backed by a deterministic node tree. */
public final class ProgressionBrowseScreen extends Screen {
    public enum Mode { KNOWLEDGE, RESEARCH, QUEST }
    private static final Identifier VANILLA_WINDOW = Identifier.withDefaultNamespace("textures/gui/advancements/window.png");
    private final Mode mode;
    private final String discipline;
    private final ProgressionCanvasInput.Gesture gesture = new ProgressionCanvasInput.Gesture();
    private int selected;
    private int dataRevision = -1;
    private List<ProgressionScreenData.KnowledgeRow> knowledge = List.of();
    private List<ProgressionScreenData.ResearchRow> research = List.of();
    private Map<String, ProgressionScreenData.KnowledgeRow> knowledgeById = Map.of();
    private Map<String, ProgressionScreenData.ResearchRow> researchById = Map.of();
    private List<ProgressionScreenData.QuestRow> quests = List.of();
    private Map<String, ProgressionScreenData.QuestRow> questsById = Map.of();
    private ProgressionTreeCanvas canvas;
    private ProgressionCanvasGeometry.ResponsiveLayout responsive;
    private Button branchButton;
    private int branchButtonX;
    private int branchButtonY;
    private int branchButtonWidth;
    private boolean branchMenuOpen;
    private boolean doubleClickPress;
    /** TEST-ONLY structure preview; null in normal play. Set only through {@link #forStructurePreview}. */
    private QuestStructurePreview preview;
    private long renderNanosTotal;
    private long renderNanosMax;
    private int renderFrames;

    public ProgressionBrowseScreen(Mode mode) { this(mode, "resources"); }

    public ProgressionBrowseScreen(Mode mode, String discipline) {
        super(Component.translatable(mode == Mode.KNOWLEDGE ? "moreminecrafts.ui.knowledge.title"
                : mode == Mode.QUEST ? "moreminecrafts.ui.quest.title" : "moreminecrafts.ui.research.title"));
        this.mode = mode;
        this.discipline = List.of("resources", "production", "living", "combat", "ecology", "world", "alchemy")
                .contains(discipline) ? discipline : "resources";
    }

    /** Package-private on purpose: only the visual-validation harness constructs the preview screen. */
    static ProgressionBrowseScreen forStructurePreview(QuestStructurePreview data) {
        ProgressionBrowseScreen screen = new ProgressionBrowseScreen(Mode.QUEST);
        screen.preview = data;
        return screen;
    }

    public Mode mode() { return mode; }
    public int validationPage() { return 0; }
    public int validationSelection() { return selected; }
    public String validationDiscipline() { return discipline; }
    public String validationSelectedNodeId() { return canvas == null ? "" : canvas.selectedId(); }
    public boolean validationCompact() { return responsive != null && responsive.compact(); }
    public ProgressionTreeCanvas validationCanvas() { return canvas; }
    public boolean validationTreeReady() { return canvas != null && !canvas.nodes().isEmpty(); }
    public int validationQuestTargetCount(String id) {
        return questsById.getOrDefault(id, new ProgressionScreenData.QuestRow("", "", "", "", "", 0, 0, 0,
                false, false, List.of())).groups().stream().mapToInt(group -> group.targets().size()).sum();
    }
    public int validationQuestGroupCount(String id) {
        return questsById.getOrDefault(id, new ProgressionScreenData.QuestRow("", "", "", "", "", 0, 0, 0,
                false, false, List.of())).groups().size();
    }
    public String validationSelectedId() { return canvas == null ? null : canvas.selectedId(); }
    public boolean validationQuestNamesResolved(String id) {
        var row = questsById.get(id);
        return row != null && canvas.questNamesResolved(row);
    }
    public String validationQuestTooltipText(String id) {
        var row = questsById.get(id);
        return row == null ? "" : canvas.questTooltipText(row);
    }
    public String validationQuestIdAt(int index) { return quests.get(index).id(); }
    public boolean validationNodeUnderTooltip(String id) { return canvas != null && canvas.nodeUnderTooltip(id); }
    /** Simulates a user clicking empty canvas (dismisses the selection tooltip), through the real click path. */
    public boolean validationClickEmpty() {
        var view = responsive.viewport();
        double x = view.x() + 3, y = view.y() + 3;
        if (canvas.hitTest(x, y) != null) return false;
        boolean handled = mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
        mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)));
        return handled;
    }
    public boolean validationIsPreview() { return preview != null; }
    public String validationLevel() { return canvas == null ? "none" : canvas.level().name(); }
    public double validationZoomValue() { return canvas == null ? 0 : canvas.zoom(); }
    public int[] validationLargeMetrics() { return canvas == null ? new int[10] : canvas.largeGraphMetrics(); }
    public int validationOverlappingFrames() { return canvas == null ? 0 : canvas.overlappingFrames(); }
    /** Sends a key through the real keyPressed path; {@code shift} sets the shift modifier. */
    public boolean validationKey(int key, boolean shift) { return keyPressed(new KeyEvent(key, 0, shift ? 1 : 0)); }
    public double validationViewportHeight() { return responsive.viewport().height(); }
    public double validationCanvasHeight() { return canvas == null ? 0 : canvas.canvasHeight(); }
    public int validationViewportX() { return responsive.viewport().x(); }
    public int validationViewportY() { return responsive.viewport().y(); }
    public boolean validationTooltipVisible() { return canvas != null && canvas.tooltipVisible(); }
    public int[] validationChapterCardCentre(String chapter) { return canvas == null ? null : canvas.validationChapterCardCentre(chapter); }
    /** Simulates a mouse click at screen pixels through the real screen path (cards, nodes, empty canvas). */
    public boolean validationClickAt(double x, double y) {
        boolean handled = mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
        mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)));
        return handled;
    }
    public int validationPreviewSubBranches() { return preview == null ? 0 : preview.subBranchCount(); }
    public int[] validationNodeStats() { return canvas == null ? new int[4] : canvas.nodeStats(); }
    public int validationChapterCount() { return canvas == null ? 0 : canvas.chapterCount(); }
    public int validationTooltipCoveredNodes() { return canvas == null ? 0 : canvas.tooltipCoveredNodes(); }
    public boolean validationSelectedInViewport() { return canvas != null && canvas.selectedNodeInViewport(); }
    public void validationViewAt(double x, double y, double zoom) { if (canvas != null) canvas.validationViewAt(x, y, zoom); }
    public void validationFitAll() { if (canvas != null) canvas.validationFitAll(); }
    public String validationLaneCenter(String chapter) { return canvas == null ? null : canvas.validationLaneCenter(chapter); }
    public double[] validationCentreWorld() { return canvas == null ? new double[2] : canvas.validationCentreWorld(); }
    public void validationPan(double dx, double dy) { if (canvas != null) canvas.panBy(dx, dy); }
    public String validationSelectedId2() { return canvas == null ? "" : canvas.selectedId(); }
    public void validationResetRenderStats() { renderNanosTotal = 0; renderNanosMax = 0; renderFrames = 0; }
    /** "frames=N avg_us=A max_us=M" for canvas render calls since the last reset (preview only). */
    public String validationRenderStats() {
        return "frames=" + renderFrames + " avg_us=" + (renderFrames == 0 ? 0 : renderNanosTotal / renderFrames / 1000)
                + " max_us=" + renderNanosMax / 1000;
    }
    public double validationContentFill() { return canvas == null ? 0 : canvas.contentFillRatio(); }
    public double validationZoomLevel() { return canvas == null ? 0 : canvas.camera().zoom(); }
    public void validationZoom(int steps) { if (canvas != null) canvas.validationZoom(steps); }
    public boolean validationAllNodesVisible() { return canvas != null && canvas.allNodesVisible(); }
    public int validationMinNodeSpacingPx() { return canvas == null ? 0 : canvas.minNodeSpacingPx(); }
    public void validationPlaceNodeAtEdge(String edge) { if (canvas != null) canvas.validationPlaceSelectedAtEdge(edge, 20); }
    public void validationResetCamera() { if (canvas != null) canvas.validationResetCamera(); }
    /** "x,y,w,h;clearsNode" for the open quest tooltip. */
    public String validationTooltipGeometry() {
        if (canvas == null) return "none";
        int[] b = canvas.tooltipBounds();
        boolean clears = canvas.tooltipClearsAllNodes();
        return b[0] + "," + b[1] + "," + b[2] + "," + b[3] + ";" + clears;
    }
    public boolean validationTooltipClearsNode() { return validationTooltipGeometry().endsWith(";true"); }
    public void validationScrollQuestTooltip(int lines) { if (canvas != null) canvas.scrollTooltip(lines); }
    public int validationTooltipScroll() { return canvas == null ? 0 : canvas.tooltipScroll(); }
    public boolean validationTooltipWithinScreen() {
        return canvas != null && canvas.tooltipWithinScreen(width, height);
    }
    public List<String> validationBranches() { return branches(); }
    public boolean validationSelectBranch(String branch) { return selectBranch(branch); }
    public boolean validationSelectBranchForNode(String id) {
        String branch = canvas.nodes().stream().filter(node -> node.id().equals(id))
                .map(ProgressionNodeLayoutCatalog.Node::branch).findFirst().orElse(null);
        return branch != null && selectBranch(branch);
    }
    public boolean validationClickBranchForNode(String id) {
        String branch = canvas.nodes().stream().filter(node -> node.id().equals(id))
                .map(ProgressionNodeLayoutCatalog.Node::branch).findFirst().orElse(null);
        int index = branches().indexOf(branch);
        if (index < 0 || !branchMenuOpen) return false;
        int rowY = branchButtonY + 21 + index * 18 + 9;
        int menuX = branchButtonX + Math.max(branchButtonWidth, 112) / 2;
        boolean handled = mouseClicked(new MouseButtonEvent(menuX, rowY, new MouseButtonInfo(0, 0)), false);
        return handled && branch.equals(currentBranch()) && canvas.selectedId().equals(canvas.branchFirst(branch));
    }
    public boolean validationOpenBranchMenu() { branchMenuOpen = true; return branchMenuOpen; }
    public void validationCloseBranchMenu() { branchMenuOpen = false; }
    public boolean validationBranchMenuOpen() { return branchMenuOpen; }
    public void validationFocusSelected() { if (canvas != null) canvas.focusSelected(); }
    public boolean validationSelectedCentered() {
        var node = canvas.nodes().stream().filter(value -> value.id().equals(canvas.selectedId())).findFirst().orElse(null);
        if (node == null) return false;
        var view = responsive.viewport();
        return Math.abs(ProgressionCanvasGeometry.screenToWorldX(canvas.camera(), view.x(), view.x() + view.width() / 2.0) - node.x()) < 0.01
                && Math.abs(ProgressionCanvasGeometry.screenToWorldY(canvas.camera(), view.y(), view.y() + view.height() / 2.0) - node.y()) < 0.01;
    }
    public boolean validationHoverNode(String id) {
        boolean exists = canvas != null && canvas.nodes().stream().anyMatch(node -> node.id().equals(id));
        if (exists) canvas.validationHover(id);
        return exists;
    }
    public void validationClearHover() { if (canvas != null) canvas.validationHover(null); }

    /** Exercises Screen callbacks only; physical X11 input is reported separately by the capture harness. */
    public boolean validationClickNode(String id) {
        var node = canvas.nodes().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (node == null) return false;
        if (!responsive.viewport().contains(ProgressionCanvasGeometry.worldToScreenX(canvas.camera(), responsive.viewport().x(), node.x()),
                ProgressionCanvasGeometry.worldToScreenY(canvas.camera(), responsive.viewport().y(), node.y()))) canvas.select(id, true);
        double x = ProgressionCanvasGeometry.worldToScreenX(canvas.camera(), responsive.viewport().x(), node.x());
        double y = ProgressionCanvasGeometry.worldToScreenY(canvas.camera(), responsive.viewport().y(), node.y());
        String hitBefore = canvas.hitTest(x, y);
        boolean accepted = mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
        mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)));
        MoreMinecrafts.LOGGER.info("VISUAL_NODE_CLICK id={} x={} y={} hit={} accepted={} selected={} pending={}",
                id, x, y, hitBefore, accepted, canvas.selectedId(), ProgressionClientApi.purchasePending(id));
        return accepted;
    }

    public boolean validationExerciseCanvasControls() {
        var view = responsive.viewport();
        double startX = 0, startY = 0;
        boolean foundBlank = false;
        for (int y = view.y() + 4; y < view.bottom() && !foundBlank; y += 8)
            for (int x = view.x() + 4; x < view.right(); x += 8)
                if (canvas.hitTest(x, y) == null) { startX = x; startY = y; foundBlank = true; break; }
        if (!foundBlank) return false;
        var beforePan = canvas.camera();
        mouseClicked(new MouseButtonEvent(startX, startY, new MouseButtonInfo(0, 0)), false);
        mouseDragged(new MouseButtonEvent(startX + 12, startY + 8, new MouseButtonInfo(0, 0)), 12, 8);
        mouseReleased(new MouseButtonEvent(startX + 12, startY + 8, new MouseButtonInfo(0, 0)));
        if (beforePan.equals(canvas.camera())) return false;
        double zoomBefore = canvas.camera().zoom();
        mouseScrolled(view.x() + view.width() / 2.0, view.y() + view.height() / 2.0, 0, 1);
        if (canvas.camera().zoom() <= zoomBefore) return false;
        List<String> options = branches();
        if (options.size() > 1) {
            String current = currentBranch();
            return selectBranch(options.stream().filter(branch -> !branch.equals(current)).findFirst().orElseThrow());
        }
        return true;
    }

    public boolean validationKeyboardNodeFocus() {
        setFocused(null);
        for (int key : new int[]{80, 79, 82, 81}) {
            String before = canvas.selectedId();
            keyPressed(new KeyEvent(key, 0, 0));
            if (!before.equals(canvas.selectedId())) { syncSelectedIndex(); return true; }
        }
        return false;
    }

    @Override protected void init() {
        responsive = ProgressionCanvasGeometry.responsiveLayout(width, height);
        refresh(false);
    }

    @Override public void tick() {
        super.tick();
        if (preview == null && dataRevision != ProgressionClientApi.revision()) refresh(false);
    }

    private int margin() { return responsive.compact() ? 8 : 20; }
    private String catalogScreen() { return mode == Mode.KNOWLEDGE ? "knowledge" : mode == Mode.QUEST ? "quests" : "research"; }

    private void refresh(boolean resetCamera) {
        clearWidgets();
        responsive = ProgressionCanvasGeometry.responsiveLayout(width, height);
        knowledge = ProgressionClientApi.knowledge(discipline);
        research = ProgressionClientApi.research(discipline);
        if (preview != null) {
            quests = List.copyOf(preview.rows().values());
        } else {
            List<String> questIds = dev.itzgamesgd.moreminecrafts.progression.QuestTreeStructure.current().allNodeIds();
            quests = ProgressionClientApi.quests(null).stream().filter(row -> questIds.contains(row.id())).toList();
        }
        knowledgeById = knowledge.stream().collect(java.util.stream.Collectors.toMap(
                ProgressionScreenData.KnowledgeRow::id, row -> row));
        researchById = research.stream().collect(java.util.stream.Collectors.toMap(
                ProgressionScreenData.ResearchRow::id, row -> row));
        questsById = quests.stream().collect(java.util.stream.Collectors.toMap(
                ProgressionScreenData.QuestRow::id, row -> row));
        dataRevision = ProgressionClientApi.revision();
        if (canvas == null || resetCamera) {
            canvas = preview != null ? new ProgressionTreeCanvas(preview) : new ProgressionTreeCanvas(catalogScreen(), discipline);
            canvas.initializeCamera(responsive.viewport(), mode == Mode.KNOWLEDGE);
        } else canvas.setViewport(responsive.viewport());
        syncSelectedIndex();
        addNavigationWidgets();
    }

    private void syncSelectedIndex() {
        if (mode == Mode.KNOWLEDGE) {
            for (int i = 0; i < knowledge.size(); i++) if (knowledge.get(i).id().equals(canvas.selectedId())) { selected = i; return; }
        } else if (mode == Mode.RESEARCH) {
            for (int i = 0; i < research.size(); i++) if (research.get(i).id().equals(canvas.selectedId())) { selected = i; return; }
        } else {
            for (int i = 0; i < quests.size(); i++) if (quests.get(i).id().equals(canvas.selectedId())) { selected = i; return; }
        }
        selected = 0;
    }

    private void addNavigationWidgets() {
        int margin = margin();
        int y = responsive.compact() ? 8 : 10;
        int backWidth = responsive.compact() ? 54 : 76;
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.back"), b -> {
            branchMenuOpen = false;
            minecraft.gui.setScreen(mode == Mode.QUEST ? new ProgressionSelectionScreen()
                    : new ProgressionDisciplineSelectionScreen(mode));
        }).bounds(margin, y, backWidth, 20).build());
        if (mode == Mode.QUEST) return;
        branchButtonX = margin + backWidth + 8;
        branchButtonY = y;
        branchButtonWidth = Math.max(96, Math.min(190, width - branchButtonX - margin));
        branchButton = addRenderableWidget(Button.builder(Component.translatable(currentBranchKey()), b ->
                branchMenuOpen = !branchMenuOpen)
                .bounds(branchButtonX, branchButtonY, branchButtonWidth, 20).build());
    }

    private List<String> branches() {
        return ProgressionNodeLayoutCatalog.load().branches(catalogScreen(), discipline);
    }

    private String currentBranch() {
        if (canvas == null) return "";
        return canvas.nodes().stream().filter(node -> node.id().equals(canvas.selectedId()))
                .map(ProgressionNodeLayoutCatalog.Node::branch).findFirst().orElse("");
    }

    private String currentBranchKey() {
        if (canvas == null) return "moreminecrafts.ui.canvas.branch";
        return canvas.nodes().stream().filter(node -> node.id().equals(canvas.selectedId()))
                .map(ProgressionNodeLayoutCatalog.Node::branchTitleKey)
                .findFirst().orElse("moreminecrafts.ui.canvas.branch");
    }

    private boolean selectBranch(String branch) {
        if (!branches().contains(branch)) return false;
        String first = canvas.branchFirst(branch);
        if (first == null) return false;
        canvas.select(first, true);
        branchMenuOpen = false;
        if (mode == Mode.QUEST) canvas.setSelectionTooltip(true);
        syncSelectedIndex();
        if (branchButton != null) branchButton.setMessage(Component.translatable(currentBranchKey()));
        return true;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF272727);
        graphics.centeredText(font, preview != null ? Component.literal(preview.banner()) : mode == Mode.QUEST ? title : Component.translatable("moreminecrafts.discipline." + discipline),
                width / 2, responsive.tabs().y() + 6, mode == Mode.QUEST ? 0xFFE0D9C7 : 0xFF292722);
        var viewport = responsive.viewport();
        drawFrameTexture(graphics, viewport.x() - 5, viewport.y() - 5, viewport.width() + 10, viewport.height() + 10);
        long renderStart = preview != null ? System.nanoTime() : 0L;
        canvas.render(graphics, font, mouseX, mouseY, knowledgeById, researchById, questsById);
        if (preview != null) {
            long elapsed = System.nanoTime() - renderStart;
            renderNanosTotal += elapsed;
            renderNanosMax = Math.max(renderNanosMax, elapsed);
            renderFrames++;
        }
        graphics.outline(viewport.x() - 1, viewport.y() - 1, viewport.width() + 2, viewport.height() + 2, 0xFF171717);
        if (branchMenuOpen) renderBranchMenu(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderBranchMenu(GuiGraphicsExtractor graphics) {
        List<String> options = branches();
        int rowHeight = 18;
        int x = branchButtonX;
        int y = branchButtonY + 21;
        int maxHeight = Math.max(0, height - y - margin());
        int visibleRows = Math.min(options.size(), maxHeight / rowHeight);
        int menuWidth = Math.max(branchButtonWidth, 112);
        graphics.fill(x, y, x + menuWidth, y + visibleRows * rowHeight, 0xFF171717);
        graphics.fill(x + 1, y + 1, x + menuWidth - 1, y + visibleRows * rowHeight - 1, 0xFFE0DED8);
        String selectedBranch = currentBranch();
        for (int i = 0; i < visibleRows; i++) {
            String branch = options.get(i);
            var node = canvas.nodes().stream().filter(value -> value.branch().equals(branch)).findFirst().orElse(null);
            if (node == null) continue;
            int rowY = y + i * rowHeight;
            if (branch.equals(selectedBranch)) graphics.fill(x + 2, rowY + 1, x + menuWidth - 2, rowY + rowHeight, 0xFFB6B3A8);
            Component label = Component.translatable(node.branchTitleKey());
            String text = font.plainSubstrByWidth(label.getString(), Math.max(8, menuWidth - 12));
            graphics.text(font, text, x + 6, rowY + 5, 0xFF24231F, false);
        }
    }

    static void drawFrameTexture(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, VANILLA_WINDOW, x, y, 0, 0, width, height,
                252, 140, 256, 256);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (branchMenuOpen) {
            if (event.button() != 0) { branchMenuOpen = false; return true; }
            int rowHeight = 18;
            int menuY = branchButtonY + 21;
            int menuWidth = Math.max(branchButtonWidth, 112);
            List<String> options = branches();
            int visibleRows = Math.min(options.size(), Math.max(0, height - menuY - margin()) / rowHeight);
            if (event.x() >= branchButtonX && event.x() < branchButtonX + menuWidth
                    && event.y() >= menuY && event.y() < menuY + visibleRows * rowHeight) {
                int index = (int) ((event.y() - menuY) / rowHeight);
                selectBranch(options.get(index));
                return true;
            }
            branchMenuOpen = false;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        if (mode == Mode.QUEST && canvas.tooltipContains(event.x(), event.y())) return true;
        if (!responsive.viewport().contains(event.x(), event.y())) return false;
        if (event.button() != 0) return false;
        if (mode == Mode.QUEST) {
            String chapter = canvas.chapterCardAt(event.x(), event.y());
            if (chapter != null) { canvas.focusChapter(chapter); return true; }
        }
        if (mode == Mode.QUEST && canvas.hitTest(event.x(), event.y()) == null) canvas.setSelectionTooltip(false);
        gesture.begin(event.button(), event.x(), event.y(), canvas.hitTest(event.x(), event.y()));
        doubleClickPress = doubleClick;
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (gesture.dragged(event.x(), event.y())) {
            canvas.panBy(dx, dy);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        String nodeId = gesture.release(event.button(), event.x(), event.y(), canvas.hitTest(event.x(), event.y()), doubleClickPress);
        doubleClickPress = false;
        if (nodeId != null) {
            canvas.select(nodeId, false);
            if (mode == Mode.QUEST) canvas.setSelectionTooltip(true);
            syncSelectedIndex();
            if (mode == Mode.RESEARCH) {
                ProgressionScreenData.ResearchRow row = researchById.get(nodeId);
                if (row != null && ProgressionPurchasePolicy.canRequest(true, row.prerequisitesDefined(), row.level(),
                        row.maximumLevel(), row.nextCost() != null, row.availability()))
                    ProgressionClientApi.purchaseIfEligible(row);
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (branchMenuOpen) return true;
        if (canvas.tooltipContains(mouseX, mouseY)) {
            canvas.scrollTooltip((int) Math.signum(verticalAmount) * 3);
            return true;
        }
        if (responsive.viewport().contains(mouseX, mouseY)) {
            canvas.zoomAt(mouseX, mouseY, (int) Math.signum(verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && branchMenuOpen) { branchMenuOpen = false; return true; }
        // Documented alternative for tooltip scrolling: Shift+Up/Down or PageUp/PageDown. Plain arrows move focus.
        if (mode == Mode.QUEST && (event.hasShiftDown() && (event.isUp() || event.isDown())
                || event.input() == InputConstants.KEY_PAGEUP || event.input() == InputConstants.KEY_PAGEDOWN)) {
            boolean down = event.isDown() || event.input() == InputConstants.KEY_PAGEDOWN;
            canvas.scrollTooltip(down ? 3 : -3);
            return true;
        }
        if (event.isLeft()) canvas.moveFocus(ProgressionCanvasGeometry.Direction.LEFT);
        else if (event.isRight()) canvas.moveFocus(ProgressionCanvasGeometry.Direction.RIGHT);
        else if (event.isUp()) canvas.moveFocus(ProgressionCanvasGeometry.Direction.UP);
        else if (event.isDown()) canvas.moveFocus(ProgressionCanvasGeometry.Direction.DOWN);
        else return super.keyPressed(event);
        if (mode == Mode.QUEST) canvas.setSelectionTooltip(true); // keyboard focus opens the selected quest's tooltip
        syncSelectedIndex();
        if (branchButton != null) branchButton.setMessage(Component.translatable(currentBranchKey()));
        return true;
    }

    @Override public void onClose() {
        if (branchMenuOpen) { branchMenuOpen = false; return; }
        minecraft.gui.setScreen(mode == Mode.QUEST ? new ProgressionSelectionScreen()
                : new ProgressionDisciplineSelectionScreen(mode));
    }
}
