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

    private final String screen;
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
        this.screen = screen;
        if ("quests".equals(screen)) {
            List<String> ids = List.of("xpq.cmb.cave_dwellers", "xpq.cmb.bestiary_25", "xpq.cmb.habitats_5");
            String[] icons = {"minecraft:spider_eye", "minecraft:book", "minecraft:slime_ball"};
            List<ProgressionNodeLayoutCatalog.Node> questNodes = new ArrayList<>();
            var grid = QuestTreeLayout.grid(ids.size());
            for (int i = 0; i < ids.size(); i++) questNodes.add(new ProgressionNodeLayoutCatalog.Node(ids.get(i),
                    "quests", "quest", "ecology", "ecology", 0, i, grid.get(i).x(), grid.get(i).y(),
                    icons[i], ProgressionNodeLayoutCatalog.FALLBACK_ICON, "moreminecrafts.ui.quest.branch.ecology"));
            this.nodes = List.copyOf(questNodes);
        } else this.nodes = ProgressionNodeLayoutCatalog.load().nodes(screen, discipline);
        this.byId = new HashMap<>();
        for (var node : nodes) byId.put(node.id(), node);
        int maxX = nodes.stream().mapToInt(ProgressionNodeLayoutCatalog.Node::x).max().orElse(0);
        int maxY = nodes.stream().mapToInt(ProgressionNodeLayoutCatalog.Node::y).max().orElse(0);
        this.canvasWidth = maxX + 64.0;
        this.canvasHeight = maxY + 64.0;
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
        return tooltipWidth > 0 && tooltipX >= 0 && tooltipY >= 0
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
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        try {
            drawBranches(graphics, font);
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

    /** One bordered group with its title; spacing is presentation only and implies no prerequisite. */
    private void drawQuestGroup(GuiGraphicsExtractor graphics, Font font) {
        int minX = nodes.stream().mapToInt(ProgressionNodeLayoutCatalog.Node::x).min().orElse(0);
        int maxX = nodes.stream().mapToInt(ProgressionNodeLayoutCatalog.Node::x).max().orElse(0);
        int minY = nodes.stream().mapToInt(ProgressionNodeLayoutCatalog.Node::y).min().orElse(0);
        int maxY = nodes.stream().mapToInt(ProgressionNodeLayoutCatalog.Node::y).max().orElse(0);
        int left = screenX(minX - QuestTreeLayout.GROUP_PADDING_X), right = screenX(maxX + QuestTreeLayout.GROUP_PADDING_X);
        int top = screenY(minY - QuestTreeLayout.GROUP_PADDING_Y - QuestTreeLayout.GROUP_LABEL_HEIGHT);
        int bottom = screenY(maxY + QuestTreeLayout.GROUP_PADDING_Y);
        graphics.fill(left, top, right, bottom, 0x66201E1B);
        graphics.outline(left, top, right - left, bottom - top, 0xFF756D5E);
        String label = Component.translatable(nodes.get(0).branchTitleKey()).getString();
        int labelWidth = Math.max(0, right - left - 12);
        if (font.width(label) > labelWidth) label = font.plainSubstrByWidth(label, Math.max(0, labelWidth - 8)) + "…";
        graphics.text(font, label, left + 6, top + 5, 0xFFE0D9C7, false);
    }

    private void drawBranches(GuiGraphicsExtractor graphics, Font font) {
        if ("quests".equals(screen)) { drawQuestGroup(graphics, font); return; }
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
                Component.translatable(quest.titleKey()), quest.progress(), quest.target()).withStyle(ChatFormatting.WHITE), false));
        rows.add(new TooltipRow(Component.translatable("moreminecrafts.ui.quest.reward", quest.rewardRawXp())
                .withStyle(ChatFormatting.GOLD), false));
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
        int maxWidth = Math.max(120, Math.min(250, screenW - margin * 2));
        int natural = 0;
        for (TooltipRow row : rows) natural = Math.max(natural, font.width(row.text()));
        int width = Math.max(110, Math.min(maxWidth, natural + pad * 2 + scrollbar));
        int textWidth = width - pad * 2 - scrollbar;
        List<net.minecraft.util.FormattedCharSequence> fixed = new ArrayList<>(), list = new ArrayList<>();
        for (TooltipRow row : rows) (row.scrollable() ? list : fixed).addAll(font.split(row.text(), textWidth));
        int fixedHeight = fixed.size() * lineHeight;
        int listHeight = list.size() * lineHeight;
        int separatorHeight = list.isEmpty() ? 0 : separator;
        int maxHeight = Math.max(60, Math.min(screenH - margin * 2, Math.min(190, (int) (screenH * 0.7))));
        int fullHeight = pad * 2 + fixedHeight + separatorHeight + listHeight;
        var frame = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
        List<ProgressionCanvasGeometry.Rect> allFrames = new ArrayList<>();
        for (var other : nodes) allFrames.add(ProgressionCanvasGeometry.nodeFrameBounds(
                screenX(other.x()), screenY(other.y()), camera.zoom()));
        int minUseful = Math.min(fullHeight, pad * 2 + fixedHeight + separatorHeight + lineHeight * 3);
        int[] placement = placeTooltip(frame, allFrames, width, Math.min(fullHeight, maxHeight), minUseful,
                screenW, screenH, margin);
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
        graphics.outline(tooltipX, tooltipY, width, height, 0xFFE0D9C7);

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
     * trimmed to the room available (the list scrolls), but never below {@code minUseful}. Returns {x, y, height}.
     */
    private static int[] placeTooltip(ProgressionCanvasGeometry.Rect node, List<ProgressionCanvasGeometry.Rect> allFrames,
                                      int width, int wantedHeight, int minUseful, int screenW, int screenH, int margin) {
        final int gap = 4;
        int centeredX = Math.max(margin, Math.min(node.x() + node.width() / 2 - width / 2, screenW - width - margin));
        int below = screenH - margin - (node.bottom() + gap);
        int above = node.y() - gap - margin;
        int hBelow = Math.min(wantedHeight, below), hAbove = Math.min(wantedHeight, above);
        int hSide = Math.min(wantedHeight, screenH - margin * 2);
        int sideY = Math.max(margin, Math.min(node.y() + node.height() / 2 - 14, screenH - hSide - margin));
        int[][] candidates = {
                {centeredX, node.bottom() + gap, hBelow},
                {centeredX, node.y() - gap - hAbove, hAbove},
                {node.right() + gap, sideY, hSide},
                {node.x() - gap - width, sideY, hSide}};
        int[] best = null, bestFit = null;
        long bestScore = Long.MAX_VALUE;
        for (int[] c : candidates) {
            boolean fits = c[2] >= minUseful && c[0] >= margin && c[0] + width <= screenW - margin
                    && c[1] >= margin && c[1] + c[2] <= screenH - margin;
            long overlap = 0;
            for (var frame : allFrames) {
                long w = Math.max(0, Math.min(c[0] + width, frame.right()) - Math.max(c[0], frame.x()));
                long h = Math.max(0, Math.min(c[1] + c[2], frame.bottom()) - Math.max(c[1], frame.y()));
                overlap += w * h;
            }
            if (fits && overlap == 0 && (bestFit == null || c[2] > bestFit[2])) bestFit = c;
            long score = overlap * 4 + (fits ? 0 : 1_000_000L) + (wantedHeight - c[2]);
            if (score < bestScore) { bestScore = score; best = c; }
        }
        if (bestFit != null) return bestFit;
        int x = Math.max(margin, Math.min(best[0], screenW - width - margin));
        int h = Math.max(Math.min(minUseful, screenH - margin * 2), best[2]);
        int y = Math.max(margin, Math.min(best[1], screenH - h - margin));
        return new int[] {x, y, h};
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
