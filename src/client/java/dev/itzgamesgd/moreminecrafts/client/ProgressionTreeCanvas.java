package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometry;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionNodeLayoutCatalog;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeCatalog;
import dev.itzgamesgd.moreminecrafts.progression.ResearchCatalog;
import dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import dev.itzgamesgd.moreminecrafts.progression.QuestTreeLayout;
import dev.itzgamesgd.moreminecrafts.progression.QuestTreeStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector2i;
import net.minecraft.client.renderer.RenderPipelines;

/** Draws and navigates a single discipline's presentation-only node canvas. */
public final class ProgressionTreeCanvas {
    private static final ClientTooltipPositioner BOUNDED_TOOLTIP_POSITIONER = (screenWidth, screenHeight,
            x, y, tooltipWidth, tooltipHeight) -> new Vector2i(
            clampTooltipOrigin(x + 12, screenWidth, tooltipWidth),
            clampTooltipOrigin(y + 12, screenHeight, tooltipHeight));
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace(
            "textures/gui/advancements/backgrounds/stone.png");
    private static final Identifier OBTAINED_BOX = Identifier.withDefaultNamespace("advancements/box_obtained");
    private static final Identifier UNOBTAINED_BOX = Identifier.withDefaultNamespace("advancements/box_unobtained");
    private static final Identifier OBTAINED_FRAME = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    private static final Identifier UNOBTAINED_FRAME = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");
    private static final Set<String> REPORTED_MISSING_ICONS = new HashSet<>();

    private static final Map<String, String> QUEST_ICONS = Map.of("xpq.cmb.cave_dwellers", "minecraft:spider_eye",
            "xpq.cmb.bestiary_25", "minecraft:book", "xpq.cmb.habitats_5", "minecraft:slime_ball");

    private final QuestStructurePreview preview;
    private final String screen;
    private final QuestTreeStructure questStructure;
    private final QuestTreeLayout.Result questLayout;
    private final List<ProgressionNodeLayoutCatalog.Node> nodes;
    private final Map<String, ProgressionNodeLayoutCatalog.Node> byId;
    private final double canvasWidth;
    private final double canvasHeight;
    private ProgressionCanvasGeometry.Rect viewport;
    private ProgressionCanvasGeometry.Camera camera = new ProgressionCanvasGeometry.Camera(0, 0, 1.0);
    private String selectedId;
    private String hoveredId;
    private String validationHoveredId;
    private int tooltipScroll;
    private int tooltipX, tooltipY, tooltipWidth, tooltipHeight;
    private String pinnedQuestId;
    private String tooltipQuestId;
    private boolean selectionTooltip;
    private final java.util.Set<String> reportedMissingNames = new HashSet<>();

    public ProgressionTreeCanvas(String screen, String discipline) {
        this(screen, discipline, null);
    }

    /** Test-only: renders the proposed structure supplied by the visual-validation harness. */
    ProgressionTreeCanvas(QuestStructurePreview preview) {
        this("quests", "resources", preview);
    }

    private ProgressionTreeCanvas(String screen, String discipline, QuestStructurePreview preview) {
        this.preview = preview;
        this.screen = screen;
        if ("quests".equals(screen)) {
            this.questStructure = preview != null ? preview.structure() : QuestTreeStructure.current();
            List<String> problems = questStructure.validate();
            if (!problems.isEmpty()) throw new IllegalStateException("Invalid quest tree structure: " + problems);
            this.questLayout = QuestTreeLayout.layout(questStructure);
            List<ProgressionNodeLayoutCatalog.Node> questNodes = new ArrayList<>();
            for (var placed : questLayout.nodes()) {
                var chapter = questStructure.chapters().get(placed.laneIndex());
                questNodes.add(new ProgressionNodeLayoutCatalog.Node(placed.id(), "quests", "quest", chapter.id(), chapter.id(),
                        placed.laneIndex(), placed.index(), placed.x(), placed.y(),
                        preview != null ? preview.iconFor(placed.chapterId())
                                : QUEST_ICONS.getOrDefault(placed.id(), ProgressionNodeLayoutCatalog.FALLBACK_ICON),
                        ProgressionNodeLayoutCatalog.FALLBACK_ICON, chapter.titleKey()));
            }
            this.nodes = List.copyOf(questNodes);
        } else {
            this.questStructure = null;
            this.questLayout = null;
            this.nodes = ProgressionNodeLayoutCatalog.load().nodes(screen, discipline);
        }
        this.byId = new HashMap<>();
        for (var node : nodes) byId.put(node.id(), node);
        int maxX = nodes.stream().mapToInt(ProgressionNodeLayoutCatalog.Node::x).max().orElse(0);
        int maxY = nodes.stream().mapToInt(ProgressionNodeLayoutCatalog.Node::y).max().orElse(0);
        this.canvasWidth = questLayout != null ? questLayout.width() : maxX + 64.0;
        this.canvasHeight = questLayout != null ? questLayout.height() : maxY + 64.0;
        this.selectedId = nodes.isEmpty() ? null : nodes.get(0).id();
    }

    public List<ProgressionNodeLayoutCatalog.Node> nodes() { return nodes; }
    public String selectedId() { return selectedId; }
    public String hoveredId() { return hoveredId; }
    public void validationHover(String id) { validationHoveredId = byId.containsKey(id) ? id : null; }
    public ProgressionCanvasGeometry.Camera camera() { return camera; }
    public double canvasWidth() { return canvasWidth; }
    public double canvasHeight() { return canvasHeight; }
    public boolean tooltipContains(double x, double y) { return tooltipContains(x, y, 0); }
    /** The margin lets the pointer cross the small gap between a node and its tooltip. */
    public boolean tooltipContains(double x, double y, int margin) {
        return tooltipWidth > 0 && x >= tooltipX - margin && x < tooltipX + tooltipWidth + margin
                && y >= tooltipY - margin && y < tooltipY + tooltipHeight + margin;
    }
    /** Keyboard focus or a click keeps the selected quest's tooltip open without hovering. */
    public void setSelectionTooltip(boolean value) { selectionTooltip = value; }
    /** Share of the viewport width spanned by the canvas content at the current zoom (initial view fit). */
    public double contentFillRatio() {
        return viewport == null || viewport.width() == 0 ? 0 : canvasWidth * camera.zoom() / viewport.width();
    }
    public void validationZoom(int steps) {
        if (viewport != null) zoomAt(viewport.x() + viewport.width() / 2.0, viewport.y() + viewport.height() / 2.0, steps);
    }
    public int chapterCount() { return questStructure == null ? 0 : questStructure.chapters().size(); }
    public boolean allNodesVisible() {
        if (viewport == null) return false;
        for (var node : nodes) {
            var frame = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
            if (frame.x() < viewport.x() || frame.y() < viewport.y() || frame.right() > viewport.right()
                    || frame.bottom() > viewport.bottom()) return false;
        }
        return true;
    }
    /** Smallest centre-to-centre distance between two nodes in screen pixels (0 for fewer than two nodes). */
    public int minNodeSpacingPx() {
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < nodes.size(); i++) for (int j = i + 1; j < nodes.size(); j++) {
            double dx = screenX(nodes.get(i).x()) - screenX(nodes.get(j).x());
            double dy = screenY(nodes.get(i).y()) - screenY(nodes.get(j).y());
            best = Math.min(best, (int) Math.round(Math.hypot(dx, dy)));
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }
    /** All visible tooltip text for a quest, joined; validation uses it to reject raw registry ids. */
    public String questTooltipText(ProgressionScreenData.QuestRow quest) {
        StringBuilder text = new StringBuilder();
        for (TooltipRow row : questTooltipRows(quest)) text.append(row.text().getString()).append('\n');
        return text.toString();
    }
    /** True when the open tooltip overlaps no node frame (selected or neighbouring). */
    public boolean tooltipClearsAllNodes() {
        if (tooltipWidth <= 0 || viewport == null) return true;
        for (var node : nodes) {
            var frame = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
            if (tooltipX < frame.right() && tooltipX + tooltipWidth > frame.x()
                    && tooltipY < frame.bottom() && tooltipY + tooltipHeight > frame.y()) return false;
        }
        return true;
    }
    /** Node counts at the current camera: total, intersecting the viewport, partly clipped, and overlapping pairs. */
    public int[] nodeStats() {
        int visible = 0, clipped = 0, overlaps = 0;
        if (viewport == null) return new int[] {nodes.size(), 0, 0, 0};
        List<ProgressionCanvasGeometry.Rect> frames = new ArrayList<>();
        for (var node : nodes) {
            var f = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
            boolean intersects = f.x() < viewport.right() && f.right() > viewport.x() && f.y() < viewport.bottom() && f.bottom() > viewport.y();
            if (!intersects) continue;
            visible++;
            if (f.x() < viewport.x() || f.y() < viewport.y() || f.right() > viewport.right() || f.bottom() > viewport.bottom()) clipped++;
            frames.add(f);
        }
        for (int i = 0; i < frames.size(); i++) for (int j = i + 1; j < frames.size(); j++) {
            var a = frames.get(i);
            var b = frames.get(j);
            if (a.x() < b.right() && a.right() > b.x() && a.y() < b.bottom() && a.bottom() > b.y()) overlaps++;
        }
        return new int[] {nodes.size(), visible, clipped, overlaps};
    }
    /** Validation: centre the camera on a world point at an exact zoom (clamped like a real pan). */
    public void validationViewAt(double worldX, double worldY, double zoom) {
        if (viewport == null) return;
        double z = Math.max(ProgressionCanvasGeometry.MIN_ZOOM, Math.min(ProgressionCanvasGeometry.MAX_ZOOM, zoom));
        camera = new ProgressionCanvasGeometry.Camera(worldX - viewport.width() / (2.0 * z), worldY - viewport.height() / (2.0 * z), z);
        camera = ProgressionCanvasGeometry.clampPan(camera, viewport, canvasWidth, canvasHeight);
    }
    public void validationFitAll() {
        if (viewport == null) return;
        camera = ProgressionCanvasGeometry.clampPan(ProgressionCanvasGeometry.fitAll(viewport, canvasWidth, canvasHeight),
                viewport, canvasWidth, canvasHeight);
    }
    public String validationLaneCenter(String chapterId) {
        if (questLayout == null) return null;
        for (var lane : questLayout.lanes()) if (lane.chapterId().equals(chapterId))
            return (questLayout.width() / 2.0) + "," + (lane.top() + lane.height() / 2.0);
        return null;
    }
    /** World coordinates under the viewport centre, used to measure zoom drift. */
    public double[] validationCentreWorld() {
        if (viewport == null) return new double[] {0, 0};
        return new double[] {
                ProgressionCanvasGeometry.screenToWorldX(camera, viewport.x(), viewport.x() + viewport.width() / 2.0),
                ProgressionCanvasGeometry.screenToWorldY(camera, viewport.y(), viewport.y() + viewport.height() / 2.0)};
    }
    public boolean selectedNodeInViewport() {
        var node = byId.get(selectedId);
        if (node == null || viewport == null) return false;
        var f = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
        return f.x() >= viewport.x() && f.y() >= viewport.y() && f.right() <= viewport.right() && f.bottom() <= viewport.bottom();
    }
    /** Nodes (other than the open tooltip's own) covered by the open tooltip. */
    public int tooltipCoveredNodes() {
        if (tooltipWidth <= 0 || viewport == null) return 0;
        int covered = 0;
        for (var node : nodes) {
            var f = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
            if (tooltipX < f.right() && tooltipX + tooltipWidth > f.x() && tooltipY < f.bottom() && tooltipY + tooltipHeight > f.y()) covered++;
        }
        return covered;
    }
    public int[] tooltipBounds() { return new int[] {tooltipX, tooltipY, tooltipWidth, tooltipHeight}; }
    public ProgressionCanvasGeometry.Rect selectedNodeFrame() {
        var node = byId.get(hoveredId != null ? hoveredId : selectedId);
        return node == null || viewport == null ? null
                : ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
    }
    /** Validation only: pans so the selected node sits {@code inset} px from the given viewport edge. */
    public void validationPlaceSelectedAtEdge(String edge, int inset) {
        var node = byId.get(validationHoveredId != null ? validationHoveredId : selectedId);
        if (node == null || viewport == null) return;
        double zoom = camera.zoom();
        double targetX = viewport.x() + viewport.width() / 2.0, targetY = viewport.y() + viewport.height() / 2.0;
        switch (edge) {
            case "left" -> targetX = viewport.x() + inset;
            case "right" -> targetX = viewport.right() - inset;
            case "top" -> targetY = viewport.y() + inset;
            case "bottom" -> targetY = viewport.bottom() - inset;
            default -> { }
        }
        camera = new ProgressionCanvasGeometry.Camera(node.x() - (targetX - viewport.x()) / zoom,
                node.y() - (targetY - viewport.y()) / zoom, zoom);
    }
    public void validationResetCamera() { if (viewport != null) initializeCamera(viewport, false); }
    public void scrollTooltip(int amount) { tooltipScroll = Math.max(0, tooltipScroll + amount); }
    public int tooltipScroll() { return tooltipScroll; }
    public boolean tooltipWithinScreen(int width, int height) {
        boolean inViewport = viewport == null || (tooltipX >= viewport.x() && tooltipY >= viewport.y()
                && tooltipX + tooltipWidth <= viewport.right() && tooltipY + tooltipHeight <= viewport.bottom());
        return inViewport && tooltipWidth > 0 && tooltipX >= 0 && tooltipY >= 0
                && tooltipX + tooltipWidth <= width && tooltipY + tooltipHeight <= height;
    }

    public void initializeCamera(ProgressionCanvasGeometry.Rect viewport, boolean knowledge) {
        this.viewport = viewport;
        if (nodes.isEmpty()) return;
        var selected = byId.get(selectedId);
        camera = ProgressionCanvasGeometry.initialCamera(viewport, canvasWidth, canvasHeight,
                selected.x(), selected.y(), knowledge);
        camera = ProgressionCanvasGeometry.clampPan(camera, viewport, canvasWidth, canvasHeight);
    }

    public void setViewport(ProgressionCanvasGeometry.Rect viewport) {
        if (this.viewport == null || this.viewport.width() != viewport.width() || this.viewport.height() != viewport.height()) {
            this.viewport = viewport;
            camera = ProgressionCanvasGeometry.clampPan(camera, viewport, canvasWidth, canvasHeight);
        } else this.viewport = viewport;
    }

    public void select(String id, boolean focus) {
        if (!byId.containsKey(id)) return;
        selectedId = id;
        if (focus && viewport != null) {
            var node = byId.get(id);
            camera = ProgressionCanvasGeometry.focusSelected(camera, viewport, node.x(), node.y());
            camera = ProgressionCanvasGeometry.clampPan(camera, viewport, canvasWidth, canvasHeight);
        }
    }

    public void focusSelected() {
        if (viewport == null || selectedId == null) return;
        var node = byId.get(selectedId);
        camera = ProgressionCanvasGeometry.focusSelected(camera, viewport, node.x(), node.y());
        camera = ProgressionCanvasGeometry.clampPan(camera, viewport, canvasWidth, canvasHeight);
    }

    public void zoomAt(double mouseX, double mouseY, int steps) {
        if (viewport == null || !viewport.contains(mouseX, mouseY)) return;
        camera = ProgressionCanvasGeometry.zoomAt(camera, viewport, mouseX, mouseY, steps);
        camera = ProgressionCanvasGeometry.clampPan(camera, viewport, canvasWidth, canvasHeight);
    }

    public void panBy(double dx, double dy) {
        if (viewport == null) return;
        camera = ProgressionCanvasGeometry.clampPan(
                ProgressionCanvasGeometry.panBy(camera, dx, dy), viewport, canvasWidth, canvasHeight);
    }

    public String hitTest(double mouseX, double mouseY) {
        if (viewport == null || !viewport.contains(mouseX, mouseY)) return null;
        for (int i = nodes.size() - 1; i >= 0; i--) {
            var node = nodes.get(i);
            var frame = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
            if (frame.contains(mouseX, mouseY)) return node.id();
        }
        return null;
    }

    public String branchFirst(String branch) {
        return ProgressionCanvasGeometry.branchFirst(points(), branch) == null ? null
                : ProgressionCanvasGeometry.branchFirst(points(), branch).id();
    }

    public String moveFocus(ProgressionCanvasGeometry.Direction direction) {
        var next = ProgressionCanvasGeometry.nearestDirectional(points(), selectedId, direction);
        if (next != null) select(next.id(), true);
        return next == null ? selectedId : next.id();
    }

    public String stepSelection(int delta) {
        var next = ProgressionCanvasGeometry.previousOrNext(points(), selectedId, delta);
        if (next != null) select(next.id(), true);
        return next == null ? selectedId : next.id();
    }

    private List<ProgressionCanvasGeometry.NodePoint> points() {
        return nodes.stream().map(node -> new ProgressionCanvasGeometry.NodePoint(node.id(), node.branch(), node.x(), node.y(),
                node.branchOrder(), node.nodeOrder())).toList();
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
                       Map<String, ProgressionScreenData.KnowledgeRow> knowledgeRows,
                       Map<String, ProgressionScreenData.ResearchRow> researchRows,
                       Map<String, ProgressionScreenData.QuestRow> questRows) {
        if (viewport == null) return;
        drawBackground(graphics);
        if (questLayout != null) drawQuestWorkspaceGrid(graphics);
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        try {
            if (questLayout != null) { drawQuestLanes(graphics, font, questRows); drawQuestEdges(graphics, questRows); }
            else drawBranches(graphics, font);
            if ("research".equals(screen)) drawPrerequisiteEdges(graphics, researchRows);
            String nodeHit = validationHoveredId != null ? validationHoveredId : hitTest(mouseX, mouseY);
            if (nodeHit != null && questRows.containsKey(nodeHit)) pinnedQuestId = nodeHit;
            else if (!tooltipContains(mouseX, mouseY, 8)) pinnedQuestId = null;
            hoveredId = "quests".equals(screen) ? (pinnedQuestId != null ? pinnedQuestId : selectionTooltip ? selectedId : null) : nodeHit;
            for (var node : nodes) {
                var knowledge = knowledgeRows.get(node.id());
                var research = researchRows.get(node.id());
                drawNode(graphics, font, node, knowledge, research, questRows.get(node.id()),
                        node.id().equals(selectedId), node.id().equals(hoveredId));
            }
        } finally {
            graphics.disableScissor();
        }
        if (hoveredId != null) {
            if ("quests".equals(screen)) {
                drawQuestTooltip(graphics, font, byId.get(hoveredId), questRows.get(hoveredId));
                return;
            }
            List<ClientTooltipComponent> tooltip = tooltip(font, byId.get(hoveredId),
                    knowledgeRows.get(hoveredId), researchRows.get(hoveredId), graphics.guiWidth());
            if (!tooltip.isEmpty()) {
                int tooltipX = validationHoveredId == null ? mouseX : screenX(byId.get(hoveredId).x());
                int tooltipY = validationHoveredId == null ? mouseY : screenY(byId.get(hoveredId).y());
                graphics.tooltip(font, tooltip, tooltipX, tooltipY, BOUNDED_TOOLTIP_POSITIONER, null, false);
            }
        }
    }

    private void drawBackground(GuiGraphicsExtractor graphics) {
        for (int y = viewport.y(); y < viewport.bottom(); y += 16) for (int x = viewport.x(); x < viewport.right(); x += 16) {
            int width = Math.min(16, viewport.right() - x), height = Math.min(16, viewport.bottom() - y);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, width, height, 16, 16, 16, 16);
        }
    }

    /** Faint workspace grid (world units) so panning and zooming read as movement over a larger canvas. */
    private void drawQuestWorkspaceGrid(GuiGraphicsExtractor graphics) {
        graphics.fill(viewport.x(), viewport.y(), viewport.right(), viewport.bottom(), 0x66000000);
        double cell = 32 * camera.zoom();
        if (cell < 12) return;
        int firstX = (int) Math.floor(ProgressionCanvasGeometry.screenToWorldX(camera, viewport.x(), viewport.x()) / 32.0);
        int lastX = (int) Math.ceil(ProgressionCanvasGeometry.screenToWorldX(camera, viewport.x(), viewport.right()) / 32.0);
        int firstY = (int) Math.floor(ProgressionCanvasGeometry.screenToWorldY(camera, viewport.y(), viewport.y()) / 32.0);
        int lastY = (int) Math.ceil(ProgressionCanvasGeometry.screenToWorldY(camera, viewport.y(), viewport.bottom()) / 32.0);
        for (int gx = firstX; gx <= lastX; gx++) {
            int x = screenX(gx * 32.0);
            if (x >= viewport.x() && x < viewport.right()) graphics.fill(x, viewport.y(), x + 1, viewport.bottom(), 0x14FFFFFF);
        }
        for (int gy = firstY; gy <= lastY; gy++) {
            int y = screenY(gy * 32.0);
            if (y >= viewport.y() && y < viewport.bottom()) graphics.fill(viewport.x(), y, viewport.right(), y + 1, 0x14FFFFFF);
        }
    }

    /** Chapter lanes: a band that continues past the viewport edges and a label gutter at the lane start. */
    private Component chapterTitle(String key) {
        Component override = preview == null ? null : preview.chapterTitle(key);
        return override != null ? override : Component.translatable(key);
    }

    private Component questTitle(ProgressionScreenData.QuestRow quest) {
        Component override = preview == null ? null : preview.questTitle(quest.id());
        return override != null ? override : Component.translatable(quest.titleKey());
    }

    private void drawQuestLanes(GuiGraphicsExtractor graphics, Font font,
                                Map<String, ProgressionScreenData.QuestRow> questRows) {
        int gutterLeft = screenX(0), gutterRight = screenX(QuestTreeLayout.GUTTER);
        for (var lane : questLayout.lanes()) {
            int top = screenY(lane.top()), bottom = screenY(lane.bottom());
            graphics.fill(viewport.x(), top, viewport.right(), bottom, 0x3A000000);
            graphics.fill(viewport.x(), top, viewport.right(), top + 1, 0xFF7A6840);
            graphics.fill(viewport.x(), bottom - 1, viewport.right(), bottom, 0xFF7A6840);
            graphics.fill(gutterLeft, top, gutterRight, bottom, 0xA0140F08);
            graphics.fill(gutterRight - 2, top, gutterRight, bottom, 0xFFC8A951);
            int done = 0;
            for (var placed : questLayout.nodes()) {
                var row = questRows.get(placed.id());
                if (placed.chapterId().equals(lane.chapterId()) && row != null && row.complete()) done++;
            }
            if (preview != null) drawPreviewSubBranches(graphics, font, lane);
            int labelWidth = gutterRight - gutterLeft - 14;
            if (labelWidth < 24) continue;
            String title = chapterTitle(lane.titleKey()).getString();
            if (font.width(title) > labelWidth) title = font.plainSubstrByWidth(title, Math.max(0, labelWidth - 8)) + "…";
            String progress = Component.translatable("moreminecrafts.ui.quest.chapter.progress", done, lane.nodeCount()).getString();
            int centerX = (gutterLeft + gutterRight) / 2, centerY = (top + bottom) / 2;
            graphics.enableScissor(gutterLeft, top, gutterRight, bottom);
            try {
                graphics.text(font, title, centerX - font.width(title) / 2, centerY - 11, 0xFFF2D58A, true);
                graphics.text(font, progress, centerX - font.width(progress) / 2, centerY + 2, 0xFFB8B4AA, false);
            } finally { graphics.disableScissor(); }
        }
    }

    /** Preview only: a divider and caption where a new sub-branch starts inside a lane. */
    private void drawPreviewSubBranches(GuiGraphicsExtractor graphics, Font font, QuestTreeLayout.Lane lane) {
        if (camera.zoom() < 0.45) return;
        String previous = null;
        int half = ProgressionCanvasGeometry.nodeFrameSize(camera.zoom()) / 2;
        for (var placed : questLayout.nodes()) {
            if (!placed.chapterId().equals(lane.chapterId())) continue;
            String sub = preview.subOf(placed.id());
            if (!sub.equals(previous)) {
                previous = sub;
                int x = screenX(placed.x()) - half, y = screenY(placed.y()) - half;
                String caption = font.plainSubstrByWidth(preview.subTitle(sub).getString(), Math.max(20, (int) (QuestTreeLayout.NODE_SPACING * 2.6 * camera.zoom())));
                graphics.fill(x - 3, y - 12, x - 2, y + 2 * half + 4, 0x66C8A951);
                graphics.text(font, caption, x, y - 11, 0xFFB8B08A, false);
            }
        }
    }

    /** Draws the explicit dependency edges of the structure (parent to child); none exist in the current slice. */
    private void drawQuestEdges(GuiGraphicsExtractor graphics, Map<String, ProgressionScreenData.QuestRow> questRows) {
        for (var edge : questStructure.edges()) {
            var parent = byId.get(edge.parentId());
            var child = byId.get(edge.childId());
            if (parent == null || child == null) continue;
            var row = questRows.get(edge.parentId());
            int color = row != null && row.complete() ? 0xFFE0B84C : 0xFF756D5E;
            int startX = screenX(parent.x()), startY = screenY(parent.y());
            int endX = screenX(child.x()), endY = screenY(child.y());
            int bendX = startX + (endX - startX) / 2;
            graphics.fill(Math.min(startX, bendX), startY - 1, Math.max(startX, bendX) + 1, startY + 1, color);
            graphics.fill(bendX - 1, Math.min(startY, endY), bendX + 1, Math.max(startY, endY) + 1, color);
            graphics.fill(Math.min(bendX, endX), endY - 1, Math.max(bendX, endX) + 1, endY + 1, color);
        }
    }

    private void drawBranches(GuiGraphicsExtractor graphics, Font font) {
        Map<Integer, List<ProgressionNodeLayoutCatalog.Node>> branches = new java.util.TreeMap<>();
        for (var node : nodes) branches.computeIfAbsent(node.branchOrder(), ignored -> new ArrayList<>()).add(node);
        double zoom = camera.zoom();
        for (var entry : branches.entrySet()) {
            List<ProgressionNodeLayoutCatalog.Node> branch = entry.getValue();
            branch.sort(java.util.Comparator.comparingInt(ProgressionNodeLayoutCatalog.Node::nodeOrder));
            int firstY = screenY(branch.get(0).y());
            int lastY = screenY(branch.get(branch.size() - 1).y());
            int spineX = screenX(64 + 192 * entry.getKey() - 40);
            graphics.fill(spineX, Math.min(firstY, lastY), spineX + 2, Math.max(firstY + 1, lastY + 1), 0xFF756D5E);
            for (var node : branch) {
                int centerY = screenY(node.y());
                int left = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), centerY, zoom).x();
                graphics.fill(Math.min(spineX, left), centerY - 1, Math.max(spineX, left), centerY + 1, 0xFF756D5E);
            }
            var sample = branch.get(0);
            String label = Component.translatable(sample.branchTitleKey()).getString();
            int labelWidth = Math.min(176, (int) Math.floor(192 * zoom) - 12);
            if (labelWidth < 36) continue;
            if (font.width(label) > labelWidth - 8) label = font.plainSubstrByWidth(label, labelWidth - 12) + "…";
            int labelCenterX = screenX(64 + 192 * sample.branchOrder() - 40);
            int labelX = labelCenterX - labelWidth / 2;
            int labelY = screenY(20);
            graphics.fill(labelX - 2, labelY - 2, labelX + labelWidth + 2, labelY + 14, 0xB522211F);
            graphics.text(font, label, labelX, labelY, 0xFFE0D9C7, false);
        }
    }

    /** Draws the authoritative research graph; branch columns are only visual grouping. */
    private void drawPrerequisiteEdges(GuiGraphicsExtractor graphics,
            Map<String, ProgressionScreenData.ResearchRow> researchRows) {
        for (var row : researchRows.values()) {
            var child = byId.get(row.id());
            if (child == null) continue;
            for (String prerequisiteId : row.prerequisiteResearch()) {
                var parent = byId.get(prerequisiteId);
                if (parent == null) continue;
                int startX = screenX(parent.x());
                int startY = screenY(parent.y());
                int endX = screenX(child.x());
                int endY = screenY(child.y());
                int bendX = startX + (endX - startX) / 2;
                drawHorizontal(graphics, startX, bendX, startY);
                drawVertical(graphics, bendX, startY, endY);
                drawHorizontal(graphics, bendX, endX, endY);
            }
        }
    }

    private static void drawHorizontal(GuiGraphicsExtractor graphics, int x1, int x2, int y) {
        graphics.fill(Math.min(x1, x2), y - 1, Math.max(x1, x2) + 1, y + 1, 0xFF756D5E);
    }

    private static void drawVertical(GuiGraphicsExtractor graphics, int x, int y1, int y2) {
        graphics.fill(x - 1, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, 0xFF756D5E);
    }

    private void drawNode(GuiGraphicsExtractor graphics, Font font, ProgressionNodeLayoutCatalog.Node node,
                          ProgressionScreenData.KnowledgeRow knowledge,
                          ProgressionScreenData.ResearchRow research, ProgressionScreenData.QuestRow quest,
                          boolean selected, boolean hovered) {
        var frame = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
        int size = frame.width();
        int left = frame.x();
        int top = frame.y();
        boolean achieved = knowledge != null ? knowledge.unlocked() : research != null ? research.level() > 0
                : quest != null && quest.complete();
        boolean purchasable = research != null && research.level() < research.maximumLevel()
                && research.prerequisitesDefined() && research.availability() == ResearchPurchaseRules.Result.PURCHASED;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, achieved ? OBTAINED_BOX : UNOBTAINED_BOX,
                left, top, size, size);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, achieved ? OBTAINED_FRAME : UNOBTAINED_FRAME,
                left, top, size, size);
        if (purchasable) graphics.outline(left, top, size, size, 0xFFE5D7A4);
        if (selected) graphics.outline(left - 2, top - 2, size + 4, size + 4, 0xFFFFFFFF);
        ItemStack icon = icon(node);
        int iconSize = Math.max(8, Math.min(size - 6, (int) Math.round(16 * Math.min(1.0, camera.zoom()))));
        graphics.pose().pushMatrix();
        graphics.pose().translate(screenX(node.x()), screenY(node.y()));
        graphics.pose().scale(iconSize / 16.0F, iconSize / 16.0F);
        graphics.item(icon, -8, -8);
        graphics.pose().popMatrix();
    }

    private ItemStack icon(ProgressionNodeLayoutCatalog.Node node) {
        Item item = resolveIcon(node);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Validates every mapping in the opt-in client harness, including nodes off the current canvas. */
    public static int validateIconMappings() {
        int missing = 0;
        for (var node : ProgressionNodeLayoutCatalog.load().nodes()) {
            Item configured = BuiltInRegistries.ITEM.getValue(Identifier.parse(node.iconItemId()));
            if (configured == null || configured == Items.AIR) {
                missing++;
                resolveIcon(node);
            }
        }
        Item fallback = BuiltInRegistries.ITEM.getValue(Identifier.parse(ProgressionNodeLayoutCatalog.FALLBACK_ICON));
        if (fallback == null || fallback == Items.AIR)
            throw new IllegalStateException("Required progression icon fallback is missing from the runtime item registry");
        return missing;
    }

    private static Item resolveIcon(ProgressionNodeLayoutCatalog.Node node) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(node.iconItemId()));
        if (item == null || item == Items.AIR) {
            if (REPORTED_MISSING_ICONS.add(node.id()))
                MoreMinecrafts.LOGGER.warn("Missing progression node icon node={} itemId={}; using {}",
                        node.id(), node.iconItemId(), node.fallbackIconItemId());
            item = BuiltInRegistries.ITEM.getValue(Identifier.parse(node.fallbackIconItemId()));
        }
        return item;
    }

    private List<ClientTooltipComponent> tooltip(Font font, ProgressionNodeLayoutCatalog.Node node,
                                                  ProgressionScreenData.KnowledgeRow knowledge,
                                                  ProgressionScreenData.ResearchRow research, int guiWidth) {
        int maxWidth = Math.max(100, Math.min(300, guiWidth - 56));
        List<ClientTooltipComponent> lines = new ArrayList<>();
        if (knowledge != null) {
            addText(lines, font, Component.translatable(knowledge.titleKey()).withStyle(ChatFormatting.WHITE), maxWidth);
            addText(lines, font, Component.translatable("moreminecrafts.ui.discipline_branch",
                    Component.translatable("moreminecrafts.discipline." + knowledge.discipline()),
                    Component.translatable(node.branchTitleKey())), maxWidth);
            addText(lines, font, Component.translatable("moreminecrafts.ui.knowledge.condition").append(Component.literal(": "))
                    .append(Component.translatable(knowledge.descriptionKey())), maxWidth);
            addText(lines, font, Component.translatable("moreminecrafts.ui.progress",
                    knowledge.conditionProgress(), knowledge.conditionTarget()), maxWidth);
            addText(lines, font, Component.translatable("moreminecrafts.ui.knowledge.reward").append(Component.literal(": "))
                    .append(Component.translatable(rewardKey(knowledge.rewardKey()))), maxWidth);
            addText(lines, font, Component.translatable(knowledge.unlocked()
                    ? "moreminecrafts.ui.state.unlocked" : "moreminecrafts.ui.state.locked"), maxWidth);
            if (knowledge.unresolved()) addText(lines, font,
                    Component.translatable("moreminecrafts.ui.unresolved").withStyle(ChatFormatting.RED), maxWidth);
            return lines;
        }
        if (research != null) {
            addText(lines, font, Component.translatable(research.titleKey()).withStyle(ChatFormatting.WHITE), maxWidth);
            addText(lines, font, Component.translatable("moreminecrafts.ui.discipline_branch",
                    Component.translatable("moreminecrafts.discipline." + research.discipline()),
                    Component.translatable(node.branchTitleKey())), maxWidth);
            addText(lines, font, Component.translatable("moreminecrafts.ui.research.tier_level",
                    Component.translatable(tierKey(research.tier())), research.level(), research.maximumLevel()), maxWidth);
            String effectLabel = research.maximumLevel() == 1
                    ? (research.level() > 0 ? "moreminecrafts.ui.research.effect.current" : "moreminecrafts.ui.research.effect.next")
                    : "moreminecrafts.ui.research.effect.summary";
            addText(lines, font, Component.translatable(effectLabel), maxWidth);
            addText(lines, font, Component.translatable(research.descriptionKey()), maxWidth);
            if (research.maximumLevel() > 1) addText(lines, font,
                    Component.translatable("moreminecrafts.ui.research.level_effect_unspecified").withStyle(ChatFormatting.GRAY), maxWidth);
            if (!research.prerequisitesDefined()) {
                addText(lines, font, Component.translatable("moreminecrafts.ui.unresolved").withStyle(ChatFormatting.RED), maxWidth);
            } else {
                if (!research.requiredKnowledge().isEmpty()) {
                    String required = research.requiredKnowledge().stream().sorted()
                            .map(ProgressionTreeCanvas::knowledgeName).reduce((a, b) -> a + ", " + b).orElse("");
                    addText(lines, font, Component.translatable(research.catalogVersion().equals("v4")
                            ? "moreminecrafts.ui.research.any_one_requirement" : "moreminecrafts.ui.research.prerequisites", required), maxWidth);
                }
                if (!research.prerequisiteResearch().isEmpty()) {
                    String required = research.prerequisiteResearch().stream().sorted().map(ProgressionTreeCanvas::researchName)
                            .reduce((a, b) -> a + ", " + b).orElse("");
                    addText(lines, font, Component.translatable("moreminecrafts.ui.research.prerequisite_research", required), maxWidth);
                }
            }
            if (research.nextCost() != null) {
                addText(lines, font, Component.translatable("moreminecrafts.ui.tooltip.cost_xp",
                        research.nextCost().rawXp(), ProgressionClientApi.availableXp()), maxWidth);
                for (var cost : research.nextCost().materials()) {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(cost.itemId()));
                    if (item == null || item == Items.AIR) continue;
                    int held = ProgressionClientApi.materialCount(cost.itemId());
                    String materialKey = cost.compressedUnits() > 0
                            ? "moreminecrafts.ui.tooltip.material_compressed_row" : "moreminecrafts.ui.tooltip.material_row";
                    Component rowLabel = cost.compressedUnits() > 0
                            ? Component.translatable(materialKey, ProgressionClientApi.materialName(cost.itemId()),
                                    cost.count(), cost.compressedUnits(), held)
                            : Component.translatable(materialKey, ProgressionClientApi.materialName(cost.itemId()), cost.count(), held);
                    String label = rowLabel.getString();
                    lines.add(new ProgressionMaterialTooltipComponent(new ItemStack(item), Component.literal(label)));
                }
            }
            addText(lines, font, Component.translatable("moreminecrafts.ui.purchase.state."
                    + research.availability().name().toLowerCase()), maxWidth);
            if (ProgressionClientApi.purchasePending(research.id())) {
                addText(lines, font, Component.translatable("moreminecrafts.ui.purchase.pending"), maxWidth);
            } else if (research.availability() == ResearchPurchaseRules.Result.PURCHASED
                    && research.prerequisitesDefined() && research.nextCost() != null) {
                addText(lines, font, Component.translatable("moreminecrafts.ui.purchase.click_to_buy"), maxWidth);
            }
        }
        return lines;
    }

    private record TooltipRow(Component text, boolean scrollable) {}

    private Component entityName(ProgressionScreenData.QuestTarget target) {
        if (net.minecraft.locale.Language.getInstance().has(target.titleKey())) return Component.translatable(target.titleKey());
        if (reportedMissingNames.add(target.entityId()))
            dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.warn("Missing quest entity name key {} for {}",
                    target.titleKey(), target.entityId());
        return Component.translatable("moreminecrafts.ui.quest.unknown_entity");
    }

    /** True when every target of the quest has a finished localized name (used by validation). */
    public boolean questNamesResolved(ProgressionScreenData.QuestRow quest) {
        for (var group : quest.groups()) for (var target : group.targets())
            if (!net.minecraft.locale.Language.getInstance().has(target.titleKey())) return false;
        return true;
    }

    private List<TooltipRow> questTooltipRows(ProgressionScreenData.QuestRow quest) {
        List<TooltipRow> rows = new ArrayList<>();
        rows.add(new TooltipRow(Component.translatable("moreminecrafts.ui.quest.tooltip.title",
                questTitle(quest), quest.progress(), quest.target()).withStyle(ChatFormatting.WHITE), false));
        rows.add(new TooltipRow(Component.translatable("moreminecrafts.ui.quest.reward", quest.rewardRawXp())
                .withStyle(ChatFormatting.GOLD), false));
        if (preview != null) {
            String sub = preview.subOf(quest.id());
            rows.add(new TooltipRow(Component.literal(preview.detail(quest.id())).withStyle(ChatFormatting.AQUA), false));
            if (sub != null) rows.add(new TooltipRow(preview.subTitle(sub).copy().withStyle(ChatFormatting.DARK_AQUA), false));
        }
        int total = quest.groups().stream().mapToInt(group -> group.targets().size()).sum();
        boolean choose = quest.groups().size() == 1 && quest.target() < total;
        if (choose) rows.add(new TooltipRow(Component.translatable("moreminecrafts.ui.quest.tooltip.choose",
                quest.target(), total).withStyle(ChatFormatting.GRAY), false));
        boolean grouped = quest.groups().size() > 1;
        for (ProgressionScreenData.QuestGroup group : quest.groups()) {
            if (grouped) rows.add(new TooltipRow(Component.translatable("moreminecrafts.ui.quest.group." + group.id())
                    .append(Component.literal(group.complete() ? " ✓" : " ○"))
                    .withStyle(group.complete() ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true));
            else if (choose) rows.add(new TooltipRow(Component.translatable("moreminecrafts.ui.quest.group.candidates",
                    group.targets().size()).withStyle(ChatFormatting.YELLOW), true));
            List<ProgressionScreenData.QuestTarget> targets = new ArrayList<>(group.targets());
            if (choose) targets.sort(java.util.Comparator.comparing(target -> entityName(target).getString()));
            for (ProgressionScreenData.QuestTarget target : targets) {
                rows.add(new TooltipRow(Component.literal(target.complete() ? (grouped ? "  ✓ " : "✓ ") : (grouped ? "  ○ " : "○ "))
                        .append(entityName(target)).withStyle(target.complete() ? ChatFormatting.GREEN : ChatFormatting.GRAY), true));
            }
        }
        return rows;
    }

    private void drawQuestTooltip(GuiGraphicsExtractor graphics, Font font,
                                  ProgressionNodeLayoutCatalog.Node node, ProgressionScreenData.QuestRow quest) {
        if (quest == null) return;
        if (!quest.id().equals(tooltipQuestId)) { tooltipQuestId = quest.id(); tooltipScroll = 0; }
        int screenW = graphics.guiWidth(), screenH = graphics.guiHeight();
        final int pad = 6, lineHeight = 10, separator = 4, margin = 6, scrollbar = 5;
        List<TooltipRow> rows = questTooltipRows(quest);
        int maxWidth = Math.max(120, Math.min(250, viewport.width() - margin * 2));
        int natural = 0;
        for (TooltipRow row : rows) natural = Math.max(natural, font.width(row.text()));
        int width = Math.max(110, Math.min(maxWidth, natural + pad * 2 + scrollbar));
        int textWidth = width - pad * 2 - scrollbar;
        List<net.minecraft.util.FormattedCharSequence> fixed = new ArrayList<>(), list = new ArrayList<>();
        for (TooltipRow row : rows) (row.scrollable() ? list : fixed).addAll(font.split(row.text(), textWidth));
        int fixedHeight = fixed.size() * lineHeight;
        int listHeight = list.size() * lineHeight;
        int separatorHeight = list.isEmpty() ? 0 : separator;
        int maxHeight = Math.max(60, Math.min(viewport.height() - margin * 2, Math.min(190, (int) (screenH * 0.7))));
        int fullHeight = pad * 2 + fixedHeight + separatorHeight + listHeight;
        var frame = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
        List<ProgressionCanvasGeometry.Rect> allFrames = new ArrayList<>();
        for (var other : nodes) allFrames.add(ProgressionCanvasGeometry.nodeFrameBounds(
                screenX(other.x()), screenY(other.y()), camera.zoom()));
        int[] placement = null;
        // Prefer three visible list rows, then two, then one, before ever overlapping a node.
        for (int wantedRows = 3; wantedRows >= 1 && placement == null; wantedRows--) {
            int minUseful = Math.min(fullHeight, pad * 2 + fixedHeight + separatorHeight + lineHeight * wantedRows);
            int[] candidate = placeTooltip(frame, allFrames, width, Math.min(fullHeight, maxHeight), minUseful, viewport, margin);
            if (wantedRows == 1 || candidate[3] == 1) placement = candidate;
        }
        int height = placement[2];
        int visibleHeight = Math.max(lineHeight, (height - pad * 2 - fixedHeight - separatorHeight) / lineHeight * lineHeight);
        boolean scrolls = !list.isEmpty() && listHeight > visibleHeight;
        if (!scrolls) { visibleHeight = listHeight; }
        height = pad * 2 + fixedHeight + separatorHeight + visibleHeight;
        tooltipWidth = width;
        tooltipHeight = height;
        tooltipX = placement[0];
        tooltipY = placement[1] + placement[2] <= frame.y() ? placement[1] + placement[2] - height : placement[1];
        graphics.fill(tooltipX, tooltipY, tooltipX + width, tooltipY + height, 0xFF100D0A);
        graphics.outline(tooltipX, tooltipY, width, height, 0xFFC8A951);
        graphics.outline(tooltipX + 1, tooltipY + 1, width - 2, height - 2, 0x55000000);

        int y = tooltipY + pad;
        for (var line : fixed) { graphics.text(font, line, tooltipX + pad, y, 0xFFE8E5DE, false); y += lineHeight; }
        int listTop = y + separatorHeight;
        int visibleLines = Math.max(1, visibleHeight / lineHeight);
        int maxScroll = Math.max(0, list.size() - visibleLines);
        tooltipScroll = Math.min(tooltipScroll, maxScroll);
        if (separatorHeight > 0) graphics.fill(tooltipX + pad, y + 1, tooltipX + width - pad, y + 2, 0xFF49443A);
        graphics.enableScissor(tooltipX + 1, listTop, tooltipX + width - 1, listTop + visibleHeight);
        try {
            int rowY = listTop - tooltipScroll * lineHeight;
            for (var line : list) { graphics.text(font, line, tooltipX + pad, rowY, 0xFFE0DED8, false); rowY += lineHeight; }
        } finally { graphics.disableScissor(); }
        if (scrolls) {
            int trackX = tooltipX + width - scrollbar;
            graphics.fill(trackX, listTop, trackX + 2, listTop + visibleHeight, 0xFF49443A);
            int thumbHeight = Math.max(12, visibleHeight * visibleLines / Math.max(1, list.size()));
            int travel = Math.max(0, visibleHeight - thumbHeight);
            int thumbY = listTop + (int) Math.round(travel * tooltipScroll / (double) Math.max(1, maxScroll));
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFFE0D9C7);
        }
    }

    /**
     * Picks the placement that keeps every node uncovered and offers the most room: below, above, then beside the node. The height is
     * trimmed to the room available (the list scrolls), but never below {@code minUseful}. Returns {x, y, height, clear} where clear is 1 when no node is covered.
     */
    private static int[] placeTooltip(ProgressionCanvasGeometry.Rect node, List<ProgressionCanvasGeometry.Rect> allFrames,
                                      int width, int wantedHeight, int minUseful, ProgressionCanvasGeometry.Rect bounds, int margin) {
        int left = bounds.x(), top = bounds.y(), right = bounds.right(), bottom = bounds.bottom();
        final int gap = 4;
        int centeredX = Math.max(left + margin, Math.min(node.x() + node.width() / 2 - width / 2, right - width - margin));
        int below = bottom - margin - (node.bottom() + gap);
        int above = node.y() - gap - (top + margin);
        int hBelow = Math.min(wantedHeight, below), hAbove = Math.min(wantedHeight, above);
        int hSide = Math.min(wantedHeight, bounds.height() - margin * 2);
        int sideY = Math.max(top + margin, Math.min(node.y() + node.height() / 2 - 14, bottom - hSide - margin));
        int[][] candidates = {
                {centeredX, node.bottom() + gap, hBelow},
                {centeredX, node.y() - gap - hAbove, hAbove},
                {node.right() + gap, sideY, hSide},
                {node.x() - gap - width, sideY, hSide}};
        int[] best = null, bestFit = null;
        long bestScore = Long.MAX_VALUE;
        for (int[] c : candidates) {
            boolean fits = c[2] >= minUseful && c[0] >= left + margin && c[0] + width <= right - margin
                    && c[1] >= top + margin && c[1] + c[2] <= bottom - margin;
            long overlap = 0;
            for (var frame : allFrames) {
                long w = Math.max(0, Math.min(c[0] + width, frame.right()) - Math.max(c[0], frame.x()));
                long h = Math.max(0, Math.min(c[1] + c[2], frame.bottom()) - Math.max(c[1], frame.y()));
                overlap += w * h;
            }
            if (fits && overlap == 0 && (bestFit == null || c[2] > bestFit[2])) bestFit = new int[] {c[0], c[1], c[2], 1};
            long score = overlap * 4 + (fits ? 0 : 1_000_000L) + (wantedHeight - c[2]);
            if (score < bestScore) { bestScore = score; best = c; }
        }
        if (bestFit != null) return bestFit;
        int x = Math.max(left + margin, Math.min(best[0], right - width - margin));
        int h = Math.max(Math.min(minUseful, bounds.height() - margin * 2), best[2]);
        int y = Math.max(top + margin, Math.min(best[1], bottom - h - margin));
        return new int[] {x, y, h, 0};
    }

    private static void addText(List<ClientTooltipComponent> target, Font font, Component value, int maxWidth) {
        font.split(value, maxWidth).forEach(line -> target.add(ClientTooltipComponent.create(line)));
    }

    private static int clampTooltipOrigin(int preferred, int screenExtent, int tooltipExtent) {
        int margin = 12;
        int maximum = Math.max(margin, screenExtent - tooltipExtent - margin);
        return Math.max(margin, Math.min(preferred, maximum));
    }

    private static String knowledgeName(int number) {
        return KnowledgeCatalog.entries().stream().filter(entry -> entry.number() == number)
                .findFirst().map(entry -> Component.translatable(entry.titleKey()).getString())
                .orElse(Component.translatable("moreminecrafts.ui.unresolved").getString());
    }

    private static String researchName(String id) {
        var definition = ResearchCatalog.byId(id);
        return definition == null ? Component.translatable("moreminecrafts.ui.unresolved").getString()
                : Component.translatable(definition.titleKey()).getString();
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

    private int screenX(double worldX) {
        return (int) Math.round(ProgressionCanvasGeometry.worldToScreenX(camera, viewport.x(), worldX));
    }
    private int screenY(double worldY) {
        return (int) Math.round(ProgressionCanvasGeometry.worldToScreenY(camera, viewport.y(), worldY));
    }

}
