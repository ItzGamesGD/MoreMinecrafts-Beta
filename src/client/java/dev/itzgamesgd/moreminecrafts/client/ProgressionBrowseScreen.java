package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometry;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionNodeLayoutCatalog;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseRules;
import java.util.HashMap;
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
    public enum Mode { KNOWLEDGE, RESEARCH }
    private static final Identifier VANILLA_WINDOW = Identifier.withDefaultNamespace("textures/gui/advancements/window.png");
    private final Mode mode;
    private final String discipline;
    private int selected;
    private int dataRevision = -1;
    private int detailsScroll;
    private List<ProgressionScreenData.KnowledgeRow> knowledge = List.of();
    private List<ProgressionScreenData.ResearchRow> research = List.of();
    private Map<String, ProgressionScreenData.KnowledgeRow> knowledgeById = Map.of();
    private Map<String, ProgressionScreenData.ResearchRow> researchById = Map.of();
    private ProgressionTreeCanvas canvas;
    private ProgressionCanvasGeometry.ResponsiveLayout responsive;
    private boolean detailsOpen;
    private boolean draggingCanvas;
    private String pressedNodeId;
    private double pressX;
    private double pressY;
    private double lastDragX;
    private double lastDragY;
    private Button purchase;
    private Button detailsButton;
    private Button closeDetails;
    private Button branchButton;
    private Button fitButton;
    private Button focusButton;
    private Button zoomOut;
    private Button zoomIn;

    public ProgressionBrowseScreen(Mode mode) {
        this(mode, "resources");
    }

    public ProgressionBrowseScreen(Mode mode, String discipline) {
        super(Component.translatable(mode == Mode.KNOWLEDGE ? "moreminecrafts.ui.knowledge.title" : "moreminecrafts.ui.research.title"));
        this.mode = mode;
        this.discipline = List.of("resources", "production", "living", "combat", "ecology", "world", "alchemy")
                .contains(discipline) ? discipline : "resources";
    }

    public Mode mode() { return mode; }
    public int validationPage() { return 0; }
    public int validationSelection() { return selected; }
    public String validationDiscipline() { return discipline; }
    public String validationSelectedNodeId() { return canvas == null ? "" : canvas.selectedId(); }
    public boolean validationCompact() { return responsive != null && responsive.compact(); }
    public ProgressionTreeCanvas validationCanvas() { return canvas; }
    public boolean validationClickNode(String id) {
        var node = canvas.nodes().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (node == null) return false;
        double x = ProgressionCanvasGeometry.worldToScreenX(canvas.camera(), responsive.viewport().x(), node.x());
        double y = ProgressionCanvasGeometry.worldToScreenY(canvas.camera(), responsive.viewport().y(), node.y());
        if (!responsive.viewport().contains(x, y)) {
            canvas.select(id, true);
            x = ProgressionCanvasGeometry.worldToScreenX(canvas.camera(), responsive.viewport().x(), node.x());
            y = ProgressionCanvasGeometry.worldToScreenY(canvas.camera(), responsive.viewport().y(), node.y());
        }
        boolean accepted = mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
        mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)));
        return accepted;
    }
    public void validationOpenDetails() {
        if (responsive.compact() && detailsButton != null) detailsButton.onPress(
                new MouseButtonEvent(detailsButton.getX() + detailsButton.getWidth() / 2.0,
                        detailsButton.getY() + detailsButton.getHeight() / 2.0, new MouseButtonInfo(0, 0)));
    }
    public void validationCloseDetails() {
        if (closeDetails != null) closeDetails.onPress(new MouseButtonEvent(closeDetails.getX() + 1,
                closeDetails.getY() + 1, new MouseButtonInfo(0, 0)));
    }
    public void validationFitAll() { if (fitButton != null) fitButton.onPress(new MouseButtonEvent(fitButton.getX() + 1,
            fitButton.getY() + 1, new MouseButtonInfo(0, 0))); }
    public void validationFocusSelected() { if (focusButton != null) focusButton.onPress(new MouseButtonEvent(focusButton.getX() + 1,
            focusButton.getY() + 1, new MouseButtonInfo(0, 0))); }
    public boolean validationSelectedCentered() {
        var node = canvas.nodes().stream().filter(value -> value.id().equals(canvas.selectedId())).findFirst().orElse(null);
        if (node == null) return false;
        var view = responsive.viewport();
        return Math.abs(ProgressionCanvasGeometry.screenToWorldX(canvas.camera(), view.x(), view.x() + view.width() / 2.0) - node.x()) < 0.01
                && Math.abs(ProgressionCanvasGeometry.screenToWorldY(canvas.camera(), view.y(), view.y() + view.height() / 2.0) - node.y()) < 0.01;
    }
    public boolean validationTreeReady() { return canvas != null && !canvas.nodes().isEmpty(); }
    public boolean validationHoverNode(String id) {
        boolean exists = canvas != null && canvas.nodes().stream().anyMatch(node -> node.id().equals(id));
        if (exists) canvas.validationHover(id);
        return exists;
    }
    public void validationClearHover() { if (canvas != null) canvas.validationHover(null); }
    public boolean validationExerciseCanvasControls() {
        var view = responsive.viewport();
        double startX = 0, startY = 0;
        boolean foundBlank = false;
        for (int y = view.y() + 4; y < view.bottom() && !foundBlank; y += 8) {
            for (int x = view.x() + 4; x < view.right(); x += 8) {
                if (canvas.hitTest(x, y) == null) { startX = x; startY = y; foundBlank = true; break; }
            }
        }
        if (!foundBlank) return false;
        var beforePan = canvas.camera();
        mouseClicked(new MouseButtonEvent(startX, startY, new MouseButtonInfo(0, 0)), false);
        mouseDragged(new MouseButtonEvent(startX + 12, startY + 8, new MouseButtonInfo(0, 0)), 12, 8);
        mouseReleased(new MouseButtonEvent(startX + 12, startY + 8, new MouseButtonInfo(0, 0)));
        if (beforePan.equals(canvas.camera())) return false;
        double zoomBefore = canvas.camera().zoom();
        mouseScrolled(view.x() + view.width() / 2.0, view.y() + view.height() / 2.0, 0, 1);
        if (canvas.camera().zoom() <= zoomBefore) return false;
        String selectedBefore = canvas.selectedId();
        if (branchButton != null) branchButton.onPress(new MouseButtonEvent(branchButton.getX() + 1,
                branchButton.getY() + 1, new MouseButtonInfo(0, 0)));
        List<String> branches = ProgressionNodeLayoutCatalog.load().branches(
                mode == Mode.KNOWLEDGE ? "knowledge" : "research", discipline);
        return branches.size() <= 1 || !selectedBefore.equals(canvas.selectedId());
    }
    public boolean validationKeyboardNodeFocus() {
        setFocused(null);
        for (int key : new int[]{80, 79, 82, 81}) {
            String before = canvas.selectedId();
            keyPressed(new KeyEvent(key, 0, 0));
            if (!before.equals(canvas.selectedId())) {
                syncSelectedIndex();
                updatePurchaseState();
                return true;
            }
        }
        return false;
    }
    public boolean validationActivatePurchaseWithKeyboard() {
        if (purchase == null || !purchase.active) return false;
        setFocused(purchase);
        return keyPressed(new KeyEvent(40, 257, 0));
    }

    @Override protected void init() {
        responsive = ProgressionCanvasGeometry.responsiveLayout(width, height);
        refresh(false);
    }

    @Override public void tick() {
        super.tick();
        if (dataRevision != ProgressionClientApi.revision()) refresh(false);
    }

    private int margin() { return responsive.compact() ? 8 : 20; }

    private void refresh(boolean resetCamera) {
        clearWidgets();
        responsive = ProgressionCanvasGeometry.responsiveLayout(width, height);
        knowledge = ProgressionClientApi.knowledge(discipline);
        research = ProgressionClientApi.research(discipline);
        knowledgeById = knowledge.stream().collect(java.util.stream.Collectors.toMap(
                ProgressionScreenData.KnowledgeRow::id, row -> row));
        researchById = research.stream().collect(java.util.stream.Collectors.toMap(
                ProgressionScreenData.ResearchRow::id, row -> row));
        dataRevision = ProgressionClientApi.revision();

        if (canvas == null || resetCamera) {
            canvas = new ProgressionTreeCanvas(mode == Mode.KNOWLEDGE ? "knowledge" : "research", discipline);
            canvas.initializeCamera(responsive.viewport(), mode == Mode.KNOWLEDGE);
        } else canvas.setViewport(responsive.viewport());
        syncSelectedIndex();
        addNavigationWidgets();
        addCanvasControls();
    }

    private void syncSelectedIndex() {
        if (mode == Mode.KNOWLEDGE) {
            for (int i = 0; i < knowledge.size(); i++) if (knowledge.get(i).id().equals(canvas.selectedId())) { selected = i; return; }
        } else {
            for (int i = 0; i < research.size(); i++) if (research.get(i).id().equals(canvas.selectedId())) { selected = i; return; }
        }
        selected = 0;
    }

    private void addNavigationWidgets() {
        int margin = margin();
        int top = responsive.tabs().y() - (responsive.compact() ? 22 : 30);
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.back"), b ->
                minecraft.gui.setScreen(new ProgressionDisciplineSelectionScreen(mode)))
                .bounds(margin, top, responsive.compact() ? 54 : 76, 20).build());
    }

    private void addCanvasControls() {
        int x = responsive.footer().x();
        int y = responsive.footer().y() + 2;
        boolean compact = responsive.compact();
        int branchWidth = compact ? 66 : 134;
        int fitWidth = compact ? 36 : 72;
        int focusWidth = compact ? 38 : 88;
        int zoomWidth = compact ? 22 : 30;
        int detailsWidth = compact ? 40 : 0;
        int purchaseWidth = mode == Mode.RESEARCH ? (compact ? 54 : 116) : 0;
        int gap = compact ? 2 : 5;
        int margin = margin();
        String branchKey = compact ? "moreminecrafts.ui.canvas.branch.short" : currentBranchKey();
        branchButton = addRenderableWidget(Button.builder(Component.translatable(branchKey), b -> nextBranch())
                .bounds(x, y, branchWidth, 22).build());
        x += branchWidth + gap;
        fitButton = addRenderableWidget(Button.builder(Component.translatable(compact
                        ? "moreminecrafts.ui.canvas.fit.short" : "moreminecrafts.ui.canvas.fit"), b -> canvas.fitAll())
                .bounds(x, y, fitWidth, 22).build());
        x += fitWidth + gap;
        focusButton = addRenderableWidget(Button.builder(Component.translatable(compact
                        ? "moreminecrafts.ui.canvas.focus.short" : "moreminecrafts.ui.canvas.focus"), b -> canvas.focusSelected())
                .bounds(x, y, focusWidth, 22).build());
        x += focusWidth + gap;
        zoomOut = addRenderableWidget(Button.builder(Component.literal("−"), b -> zoomAtCenter(-1))
                .bounds(x, y, zoomWidth, 22).build());
        x += zoomWidth + gap;
        zoomIn = addRenderableWidget(Button.builder(Component.literal("+"), b -> zoomAtCenter(1))
                .bounds(x, y, zoomWidth, 22).build());
        x += zoomWidth + gap;
        if (compact) {
            detailsButton = addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.canvas.details.short"), b -> {
                detailsOpen = true; detailsScroll = 0; updateOverlayButtons();
            }).bounds(x, y, detailsWidth, 22).build());
            x += detailsWidth + gap;
        } else detailsButton = null;
        if (mode == Mode.RESEARCH) {
            purchase = addRenderableWidget(Button.builder(Component.translatable(compact
                            ? "moreminecrafts.ui.purchase.short" : "moreminecrafts.ui.purchase"), b -> openConfirmation())
                    .bounds(x, y, purchaseWidth, 22).build());
        } else purchase = null;
        int closeDetailsX = compact ? width - margin - 58
                : responsive.details().x() + responsive.details().width() - 96;
        closeDetails = addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.canvas.close_details"), b -> {
            detailsOpen = false; updateOverlayButtons();
        }).bounds(closeDetailsX, responsive.compact() ? 14 : responsive.details().y() + 6,
                compact ? 50 : 86, 22).build());
        closeDetails.visible = detailsOpen;
        if (detailsButton != null) detailsButton.visible = !detailsOpen;
        updatePurchaseState();
        updateOverlayButtons();
    }

    private void updateOverlayButtons() {
        if (closeDetails != null) closeDetails.visible = detailsOpen && responsive.compact();
        if (detailsButton != null) detailsButton.visible = !detailsOpen;
        if (branchButton != null) branchButton.visible = !detailsOpen;
        if (fitButton != null) fitButton.visible = !detailsOpen;
        if (focusButton != null) focusButton.visible = !detailsOpen;
        if (zoomOut != null) zoomOut.visible = !detailsOpen;
        if (zoomIn != null) zoomIn.visible = !detailsOpen;
        if (purchase != null) purchase.visible = !responsive.compact() || !detailsOpen;
    }

    private void updatePurchaseState() {
        if (purchase == null) return;
        var row = selectedResearch();
        purchase.active = row != null && row.nextCost() != null && row.prerequisitesDefined()
                && row.availability() == ResearchPurchaseRules.Result.PURCHASED;
    }

    private void nextBranch() {
        List<String> branches = ProgressionNodeLayoutCatalog.load().branches(
                mode == Mode.KNOWLEDGE ? "knowledge" : "research", discipline);
        if (branches.isEmpty()) return;
        int selectedOrder = -1;
        var node = canvas.nodes().stream().filter(n -> n.id().equals(canvas.selectedId())).findFirst().orElse(null);
        if (node != null) selectedOrder = branches.indexOf(node.branch());
        String next = branches.get(Math.floorMod(selectedOrder + 1, branches.size()));
        canvas.select(canvas.branchFirst(next), true);
        syncSelectedIndex();
        rebuildWidgetsOnly();
    }

    private String currentBranchKey() {
        if (canvas == null) return "moreminecrafts.ui.canvas.branch";
        return canvas.nodes().stream().filter(n -> n.id().equals(canvas.selectedId())).findFirst()
                .map(ProgressionNodeLayoutCatalog.Node::branchTitleKey).orElse("moreminecrafts.ui.canvas.branch");
    }

    private void rebuildWidgetsOnly() {
        int previousRevision = dataRevision;
        refresh(false);
        dataRevision = previousRevision;
    }

    private void zoomAtCenter(int steps) {
        var viewport = responsive.viewport();
        canvas.zoomAt(viewport.x() + viewport.width() / 2.0, viewport.y() + viewport.height() / 2.0, steps);
    }

    private void openConfirmation() {
        ProgressionScreenData.ResearchRow row = selectedResearch();
        if (mode != Mode.RESEARCH || row == null || row.nextCost() == null
                || row.availability() != ResearchPurchaseRules.Result.PURCHASED || !row.prerequisitesDefined()) return;
        minecraft.gui.setScreen(new ResearchPurchaseConfirmScreen(this, row));
    }

    private ProgressionScreenData.ResearchRow selectedResearch() {
        return canvas == null ? null : researchById.get(canvas.selectedId());
    }
    private ProgressionScreenData.KnowledgeRow selectedKnowledge() {
        return canvas == null ? null : knowledgeById.get(canvas.selectedId());
    }

    /** Opens the same confirmation step used by the visible purchase control. */
    public void validationActivatePurchase() { openConfirmation(); }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int margin = margin();
        graphics.fill(0, 0, width, height, 0xFF272727);
        graphics.text(font, title, margin + (responsive.compact() ? 62 : 86), margin + 6, 0xFFF2F0E9);
        graphics.centeredText(font, Component.translatable("moreminecrafts.discipline." + discipline),
                width / 2, responsive.tabs().y() + 6, 0xFF292722);
        var viewport = responsive.viewport();
        drawFrameTexture(graphics, viewport.x() - 5, viewport.y() - 5, viewport.width() + 10, viewport.height() + 10);
        canvas.render(graphics, font, mouseX, mouseY, knowledgeById, researchById);
        graphics.outline(viewport.x() - 1, viewport.y() - 1, viewport.width() + 2, viewport.height() + 2, 0xFF171717);
        if (!responsive.compact()) renderDetails(graphics, responsive.details(), mouseX, mouseY, detailsScroll);
        if (detailsOpen && responsive.compact()) renderDetailsOverlay(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable(responsive.compact()
                ? "moreminecrafts.ui.canvas.connection_legend.short" : "moreminecrafts.ui.canvas.connection_legend"),
                margin, height - margin - 1, 0xFFA9A392);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderDetailsOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int margin = 8, x = margin, y = margin, w = width - margin * 2, h = height - margin * 2;
        graphics.fill(0, 0, width, height, 0xDD111111);
        drawPanel(graphics, x, y, w, h);
        graphics.text(font, Component.translatable("moreminecrafts.ui.canvas.selected_details"), x + 12, y + 8, 0xFF161616);
        int bodyTop = y + 34, footerTop = y + h - 32;
        graphics.enableScissor(x + 8, bodyTop, x + w - 8, footerTop);
        try { renderSelectedDetails(graphics, x + 12, bodyTop - detailsScroll, w - 24); }
        finally { graphics.disableScissor(); }
        if (mode == Mode.RESEARCH && purchase != null) purchase.visible = true;
        if (closeDetails != null) closeDetails.visible = true;
    }

    private void renderDetails(GuiGraphicsExtractor graphics, ProgressionCanvasGeometry.Rect panel, int mouseX, int mouseY, int scroll) {
        drawPanel(graphics, panel.x(), panel.y(), panel.width(), panel.height());
        graphics.text(font, Component.translatable("moreminecrafts.ui.canvas.selected_details"), panel.x() + 12, panel.y() + 10, 0xFF181818);
        graphics.enableScissor(panel.x() + 8, panel.y() + 32, panel.right() - 8, panel.bottom() - 34);
        try { renderSelectedDetails(graphics, panel.x() + 12, panel.y() + 38 - scroll, panel.width() - 24); }
        finally { graphics.disableScissor(); }
    }

    private void renderSelectedDetails(GuiGraphicsExtractor graphics, int x, int y, int w) {
        if (mode == Mode.KNOWLEDGE) {
            var row = selectedKnowledge();
            if (row == null) return;
            int yy = drawWrapped(graphics, Component.translatable(row.titleKey()), x, y, w, 0xFF181818);
            yy += 5;
            yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.knowledge.condition"), x, yy, w, 0xFF312F2A);
            yy = drawWrapped(graphics, Component.translatable(row.descriptionKey()), x, yy, w, row.unresolved() ? 0xFF8B3E35 : 0xFF46423A);
            yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.progress", row.conditionProgress(), row.conditionTarget()), x, yy + 4, w, 0xFF46423A);
            yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.knowledge.reward"), x, yy + 4, w, 0xFF312F2A);
            yy = drawWrapped(graphics, Component.translatable(rewardKey(row.rewardKey())), x, yy, w, 0xFF46423A);
            drawWrapped(graphics, Component.translatable(row.unlocked() ? "moreminecrafts.ui.state.unlocked" : "moreminecrafts.ui.state.locked"),
                    x, yy + 6, w, row.unlocked() ? 0xFF287137 : 0xFF684E22);
        } else {
            var row = selectedResearch();
            if (row == null) return;
            int yy = drawWrapped(graphics, Component.translatable(row.titleKey()), x, y, w, 0xFF181818);
            yy += 4;
            yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.research.tier_level",
                    Component.translatable(tierKey(row.tier())), row.level(), row.maximumLevel()), x, yy, w, 0xFF34312B);
            yy = drawWrapped(graphics, Component.translatable(row.descriptionKey()), x, yy + 3, w, 0xFF46423A);
            if (!row.prerequisitesDefined()) {
                yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.unresolved"), x, yy + 4, w, 0xFF8B3E35);
                yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.reward.unresolved"), x, yy, w, 0xFF8B3E35);
            } else {
                if (!row.requiredKnowledge().isEmpty()) {
                    String ids = row.requiredKnowledge().stream().sorted().map(n -> "#" + n).reduce((a, b) -> a + ", " + b).orElse("");
                    yy = drawWrapped(graphics, Component.translatable(row.catalogVersion().equals("v4")
                            ? "moreminecrafts.ui.research.any_one_requirement" : "moreminecrafts.ui.research.prerequisites", ids), x, yy + 4, w, 0xFF34312B);
                }
                if (!row.prerequisiteResearch().isEmpty()) {
                    String ids = String.join(", ", row.prerequisiteResearch());
                    yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.research.requirements", "—", ids), x, yy, w, 0xFF34312B);
                }
            }
            yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.purchase.state." + row.availability().name().toLowerCase()),
                    x, yy + 4, w, row.availability() == ResearchPurchaseRules.Result.PURCHASED ? 0xFF287137 : 0xFF684E22);
            if (row.nextCost() != null) {
                yy = drawWrapped(graphics, Component.translatable("moreminecrafts.ui.cost_xp", row.nextCost().rawXp()), x, yy + 4, w, 0xFF34312B);
                for (var cost : row.nextCost().materials()) {
                    String itemName = ProgressionClientApi.materialName(cost.itemId());
                    String key = cost.compressedUnits() > 0 ? "moreminecrafts.ui.cost_material_compressed" : "moreminecrafts.ui.cost_material";
                    Component value = cost.compressedUnits() > 0
                            ? Component.translatable(key, itemName, cost.count(), cost.compressedUnits())
                            : Component.translatable(key, itemName, cost.count());
                    yy = drawWrapped(graphics, value, x, yy, w, 0xFF46423A);
                }
            }
            String statusKey = row.level() > 0 ? "moreminecrafts.ui.state.purchased" : "moreminecrafts.ui.state.locked";
            drawWrapped(graphics, Component.translatable(statusKey), x, yy + 5, w, row.level() > 0 ? 0xFF287137 : 0xFF684E22);
        }
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, Component value, int x, int y, int width, int color) {
        if (width <= 0 || y > height + 1200) return y + 12;
        return graphics.textWithWordWrap(font, value, x, y, width, color);
    }

    private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF171717);
        if (w > 6 && h > 6) graphics.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFD6D4CF);
        if (w > 18 && h > 14) graphics.fill(x + 7, y + 7, x + w - 7, y + 10, 0xFFB5B5B2);
    }

    static void drawFrameTexture(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, VANILLA_WINDOW, x, y, 0, 0, width, height,
                252, 140, 256, 256);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (detailsOpen && responsive.compact()) {
            if (closeDetails != null && event.x() >= closeDetails.getX() && event.x() < closeDetails.getX() + closeDetails.getWidth()
                    && event.y() >= closeDetails.getY() && event.y() < closeDetails.getY() + closeDetails.getHeight())
                return super.mouseClicked(event, doubleClick);
            int footerY = height - 8 - 26;
            if (mode == Mode.RESEARCH && event.y() >= footerY && purchase != null) return super.mouseClicked(event, doubleClick);
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        if (responsive.viewport().contains(event.x(), event.y())) {
            if (event.button() == 0) {
                pressX = event.x(); pressY = event.y();
                pressedNodeId = canvas.hitTest(event.x(), event.y());
                draggingCanvas = false;
                return true;
            }
            if (event.button() == 2) { draggingCanvas = true; lastDragX = event.x(); lastDragY = event.y(); return true; }
        }
        return false;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!draggingCanvas && (pressedNodeId != null || pressX != 0 || pressY != 0)
                && Math.hypot(event.x() - pressX, event.y() - pressY) >= 4.0) {
            draggingCanvas = pressedNodeId == null;
            if (!draggingCanvas) pressedNodeId = null;
        }
        if (draggingCanvas) {
            canvas.panBy(dx, dy);
            lastDragX = event.x(); lastDragY = event.y();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (!draggingCanvas && pressedNodeId != null
                && Math.hypot(event.x() - pressX, event.y() - pressY) < 4.0) {
            canvas.select(pressedNodeId, false);
            syncSelectedIndex();
            updatePurchaseState();
        }
        draggingCanvas = false;
        pressedNodeId = null;
        pressX = 0;
        pressY = 0;
        return super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (detailsOpen && responsive.compact()) {
            detailsScroll = Math.max(0, Math.min(1200, detailsScroll - (int) Math.signum(verticalAmount) * 18));
            return true;
        }
        if (!responsive.compact() && responsive.details().contains(mouseX, mouseY)) {
            detailsScroll = Math.max(0, Math.min(1200, detailsScroll - (int) Math.signum(verticalAmount) * 18));
            return true;
        }
        if (responsive.viewport().contains(mouseX, mouseY)) {
            canvas.zoomAt(mouseX, mouseY, (int) Math.signum(verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && detailsOpen) {
            detailsOpen = false; updateOverlayButtons(); return true;
        }
        if (detailsOpen && responsive.compact()) {
            if (event.isUp()) detailsScroll = Math.max(0, detailsScroll - 18);
            else if (event.isDown()) detailsScroll = Math.min(1200, detailsScroll + 18);
            else return super.keyPressed(event);
            return true;
        }
        if (event.isConfirmation() && getFocused() != null) return super.keyPressed(event);
        if (event.isLeft()) canvas.moveFocus(ProgressionCanvasGeometry.Direction.LEFT);
        else if (event.isRight()) canvas.moveFocus(ProgressionCanvasGeometry.Direction.RIGHT);
        else if (event.isUp()) canvas.moveFocus(ProgressionCanvasGeometry.Direction.UP);
        else if (event.isDown()) canvas.moveFocus(ProgressionCanvasGeometry.Direction.DOWN);
        else if (event.isConfirmation()) {
            if (responsive.compact()) { detailsOpen = true; updateOverlayButtons(); }
            return true;
        } else return super.keyPressed(event);
        syncSelectedIndex(); updatePurchaseState();
        return true;
    }

    @Override public void onClose() {
        if (detailsOpen) { detailsOpen = false; updateOverlayButtons(); }
        else minecraft.gui.setScreen(new ProgressionDisciplineSelectionScreen(mode));
    }

    private static String rewardKey(String key) {
        return switch (key) {
            case "rare_mob_damage" -> "moreminecrafts.ui.reward.unresolved";
            case "parachute_recipe" -> "moreminecrafts.ui.reward.recipe";
            case "sapling_drop" -> "moreminecrafts.ui.reward.sapling";
            default -> key.endsWith("_damage") ? "moreminecrafts.ui.reward.damage"
                    : key.endsWith("_xp") ? "moreminecrafts.ui.reward.xp"
                    : key.endsWith("_guide") || key.endsWith("_info") || key.endsWith("_hint") ? "moreminecrafts.ui.reward.guide"
                    : key.endsWith("_durability") ? "moreminecrafts.ui.reward.durability"
                    : "moreminecrafts.ui.reward.unlock";
        };
    }
    private static String tierKey(String tier) {
        return switch (tier) {
            case "T1" -> "moreminecrafts.ui.tier.t1";
            case "T2" -> "moreminecrafts.ui.tier.t2";
            case "T3" -> "moreminecrafts.ui.tier.t3";
            default -> "moreminecrafts.ui.tier.unresolved";
        };
    }
}
