package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionCanvasGeometry;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionNodeLayoutCatalog;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.RenderPipelines;

/** Draws and navigates a single discipline's presentation-only node canvas. */
public final class ProgressionTreeCanvas {
    private static final Identifier BACKGROUND = ModIds.id("textures/gui/progression/tree_background.png");
    private static final Identifier BORDERS = ModIds.id("textures/gui/progression/node_borders.png");
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

    public ProgressionTreeCanvas(String screen, String discipline) {
        this.screen = screen;
        this.nodes = ProgressionNodeLayoutCatalog.load().nodes(screen, discipline);
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
    public ProgressionCanvasGeometry.Camera camera() { return camera; }
    public double canvasWidth() { return canvasWidth; }
    public double canvasHeight() { return canvasHeight; }

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

    public void fitAll() {
        if (viewport == null || nodes.isEmpty()) return;
        camera = ProgressionCanvasGeometry.clampPan(
                ProgressionCanvasGeometry.fitAll(viewport, canvasWidth, canvasHeight), viewport, canvasWidth, canvasHeight);
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
        double worldX = ProgressionCanvasGeometry.screenToWorldX(camera, viewport.x(), mouseX);
        double worldY = ProgressionCanvasGeometry.screenToWorldY(camera, viewport.y(), mouseY);
        int baseSize = "research".equals(screen) ? ProgressionNodeLayoutCatalog.RESEARCH_NODE_SIZE
                : ProgressionNodeLayoutCatalog.KNOWLEDGE_NODE_SIZE;
        for (int i = nodes.size() - 1; i >= 0; i--) {
            var node = nodes.get(i);
            if (Math.abs(node.x() - worldX) <= baseSize / 2.0
                    && Math.abs(node.y() - worldY) <= baseSize / 2.0) return node.id();
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
                       Map<String, ProgressionScreenData.ResearchRow> researchRows) {
        if (viewport == null) return;
        drawBackground(graphics);
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        try {
            drawBranches(graphics, font);
            hoveredId = hitTest(mouseX, mouseY);
            for (var node : nodes) {
                var knowledge = knowledgeRows.get(node.id());
                var research = researchRows.get(node.id());
                drawNode(graphics, font, node, knowledge, research, node.id().equals(selectedId), node.id().equals(hoveredId));
            }
            if (hoveredId != null) {
                Component tooltip = tooltip(knowledgeRows.get(hoveredId), researchRows.get(hoveredId));
                if (tooltip != null) {
                    int tooltipWidth = Math.max(120, Math.min(240, viewport.width() - 16));
                    graphics.setTooltipForNextFrame(font, font.split(tooltip, tooltipWidth), mouseX, mouseY);
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void drawBackground(GuiGraphicsExtractor graphics) {
        for (int y = viewport.y(); y < viewport.bottom(); y += 32) for (int x = viewport.x(); x < viewport.right(); x += 32) {
            int width = Math.min(32, viewport.right() - x), height = Math.min(32, viewport.bottom() - y);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0, 0, width, height, width, height);
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
                int half = (int) Math.round(("research".equals(screen) ? 36 : 32) * zoom / 2.0);
                int left = screenX(node.x()) - half;
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

    private void drawNode(GuiGraphicsExtractor graphics, Font font, ProgressionNodeLayoutCatalog.Node node,
                          ProgressionScreenData.KnowledgeRow knowledge,
                          ProgressionScreenData.ResearchRow research, boolean selected, boolean hovered) {
        int size = Math.max(8, (int) Math.round(("research".equals(screen) ? 36 : 32) * camera.zoom()));
        int left = screenX(node.x()) - size / 2;
        int top = screenY(node.y()) - size / 2;
        int state = selected ? 3 : hovered ? 4 : state(knowledge, research);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BORDERS, left, top, state * 36, 0, size, size, 36, 36, 252, 36);
        ItemStack icon = icon(node);
        int iconSize = Math.max(8, (int) Math.round(16 * Math.min(1.0, camera.zoom())));
        graphics.pose().pushMatrix();
        graphics.pose().translate(screenX(node.x()), screenY(node.y()));
        graphics.pose().scale(iconSize / 16.0F, iconSize / 16.0F);
        graphics.item(icon, -8, -8);
        graphics.pose().popMatrix();
        if (research != null) {
            String tier = research.tier().matches("T[1-3]") ? research.tier() : "?";
            String level = tier + " " + research.level() + "/" + research.maximumLevel();
            graphics.text(font, level, left, top + size + 1, 0xFFE8E2D1, false);
        }
    }

    private int state(ProgressionScreenData.KnowledgeRow knowledge, ProgressionScreenData.ResearchRow research) {
        if (knowledge != null) return knowledge.unresolved() ? 6 : knowledge.unlocked() ? 2 : 0;
        if (research == null) return 0;
        if (!research.prerequisitesDefined()) return 6;
        if (research.level() > 0) return 5;
        return research.availability() == ResearchPurchaseRules.Result.PURCHASED ? 1 : 0;
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

    private Component tooltip(ProgressionScreenData.KnowledgeRow knowledge, ProgressionScreenData.ResearchRow research) {
        if (knowledge != null) {
            return Component.translatable(knowledge.titleKey()).append(Component.literal("\n"))
                    .append(Component.translatable("moreminecrafts.ui.knowledge.condition")).append(Component.literal(": "))
                    .append(Component.translatable(knowledge.descriptionKey())).append(Component.literal("\n"))
                    .append(Component.translatable("moreminecrafts.ui.progress", knowledge.conditionProgress(), knowledge.conditionTarget()));
        }
        if (research != null) {
            return Component.translatable(research.titleKey()).append(Component.literal("\n"))
                    .append(Component.translatable("moreminecrafts.ui.research.tier_level",
                            Component.translatable(tierKey(research.tier())),
                            research.level(), research.maximumLevel())).append(Component.literal("\n"))
                    .append(Component.translatable("moreminecrafts.ui.purchase.state." + research.availability().name().toLowerCase()))
                    .append(research.nextCost() == null ? Component.empty() : Component.literal("\n"))
                    .append(research.nextCost() == null ? Component.empty()
                            : Component.translatable("moreminecrafts.ui.cost_xp", research.nextCost().rawXp()));
        }
        return null;
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
