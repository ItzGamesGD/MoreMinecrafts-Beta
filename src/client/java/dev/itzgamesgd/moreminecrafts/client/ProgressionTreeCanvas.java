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
            Math.max(0, Math.min(x + 12, screenWidth - tooltipWidth)),
            Math.max(0, Math.min(y + 12, screenHeight - tooltipHeight)));
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
    public void validationHover(String id) { validationHoveredId = byId.containsKey(id) ? id : null; }
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
                       Map<String, ProgressionScreenData.ResearchRow> researchRows) {
        if (viewport == null) return;
        drawBackground(graphics);
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        try {
            drawBranches(graphics, font);
            hoveredId = validationHoveredId != null ? validationHoveredId : hitTest(mouseX, mouseY);
            for (var node : nodes) {
                var knowledge = knowledgeRows.get(node.id());
                var research = researchRows.get(node.id());
                drawNode(graphics, font, node, knowledge, research, node.id().equals(selectedId), node.id().equals(hoveredId));
            }
        } finally {
            graphics.disableScissor();
        }
        if (hoveredId != null) {
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

    private void drawNode(GuiGraphicsExtractor graphics, Font font, ProgressionNodeLayoutCatalog.Node node,
                          ProgressionScreenData.KnowledgeRow knowledge,
                          ProgressionScreenData.ResearchRow research, boolean selected, boolean hovered) {
        var frame = ProgressionCanvasGeometry.nodeFrameBounds(screenX(node.x()), screenY(node.y()), camera.zoom());
        int size = frame.width();
        int left = frame.x();
        int top = frame.y();
        boolean achieved = knowledge != null ? knowledge.unlocked() : research != null && research.level() > 0;
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
        int maxWidth = Math.max(100, Math.min(300, guiWidth - 24));
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

    private static void addText(List<ClientTooltipComponent> target, Font font, Component value, int maxWidth) {
        font.split(value, maxWidth).forEach(line -> target.add(ClientTooltipComponent.create(line)));
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
