package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseRules;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Vanilla-advancements-inspired discipline tree and selected-node details. */
public final class ProgressionBrowseScreen extends Screen {
    public enum Mode { KNOWLEDGE, RESEARCH }
    private static final String[] DISCIPLINES = {"resources", "production", "living", "combat", "ecology", "world", "alchemy"};
    private final Mode mode;
    private String discipline = "resources";
    private int page;
    private int selected;
    private List<ProgressionScreenData.KnowledgeRow> knowledge = List.of();
    private List<ProgressionScreenData.ResearchRow> research = List.of();
    private Button purchase;
    private Button previous;
    private Button next;
    private int dataRevision = -1;
    private float uiScale = 1.0F;

    public ProgressionBrowseScreen(Mode mode) {
        super(Component.translatable(mode == Mode.KNOWLEDGE ? "moreminecrafts.ui.knowledge.title" : "moreminecrafts.ui.research.title"));
        this.mode = mode;
    }

    public Mode mode() { return mode; }
    public int validationPage() { return page; }
    public int validationSelection() { return selected; }
    public String validationDiscipline() { return discipline; }

    @Override protected void init() {
        uiScale = Math.min(1.0F, Math.min((width - 32.0F) / 1120.0F, (height - 32.0F) / 650.0F));
        refresh();
    }

    private int p(int value) { return Math.round(value * uiScale); }

    @Override public void tick() {
        super.tick();
        if (dataRevision != ProgressionClientApi.revision()) refresh();
    }

    private void refresh() {
        clearWidgets();
        knowledge = ProgressionClientApi.knowledge(discipline);
        research = ProgressionClientApi.research(discipline);
        dataRevision = ProgressionClientApi.revision();
        int left = (width - Math.min(width - p(32), p(1120))) / 2;
        int top = Math.max(p(20), (height - Math.min(height - p(32), p(650))) / 2);
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.back"), b ->
                minecraft.gui.setScreen(new ProgressionSelectionScreen())).bounds(left, top, p(76), p(20)).build());

        int contentY = top + p(76);
        int pageSize = 7;
        int count = mode == Mode.KNOWLEDGE ? knowledge.size() : research.size();
        int pages = Math.max(1, (count + pageSize - 1) / pageSize);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < pageSize; i++) {
            int local = i;
            int index = page * pageSize + i;
            if (index >= count) break;
            Component label;
            boolean unlocked;
            if (mode == Mode.KNOWLEDGE) {
                var row = knowledge.get(index);
                unlocked = row.unlocked();
                label = Component.literal((unlocked ? "◆ " : row.unresolved() ? "? " : "◇ "))
                        .append(Component.translatable(row.titleKey()));
            } else {
                var row = research.get(index);
                unlocked = row.level() > 0;
                label = Component.literal((row.prerequisitesDefined() ? (unlocked ? "◆ " : "◇ ") : "? ")
                        + "Lv." + row.level() + "  ").append(Component.translatable(row.titleKey()));
            }
            int nx = left + p(25) + (local % 2) * p(206);
            int ny = contentY + (local / 2) * p(49);
            Component tip;
            if (mode == Mode.KNOWLEDGE) {
                var knowledgeRow = knowledge.get(index);
                tip = Component.translatable(knowledgeRow.titleKey()).append(Component.literal("\n"))
                        .append(Component.translatable("moreminecrafts.ui.knowledge.condition")).append(Component.literal(": "))
                        .append(Component.translatable(knowledgeRow.unresolved() ? "moreminecrafts.ui.unresolved" : knowledgeRow.descriptionKey()))
                        .append(Component.literal("\n"))
                        .append(Component.translatable(knowledgeRow.unresolved() ? "moreminecrafts.ui.reward.unresolved" : rewardKey(knowledgeRow.rewardKey())))
                        .append(Component.literal("\n"))
                        .append(Component.translatable(knowledgeRow.unlocked()
                                ? "moreminecrafts.ui.state.unlocked" : "moreminecrafts.ui.state.locked"));
            } else {
                var researchRow = research.get(index);
                tip = Component.translatable(researchRow.titleKey()).append(Component.literal("\n"))
                        .append(Component.translatable(researchRow.descriptionKey()))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("moreminecrafts.ui.research.tier_level",
                                Component.translatable(tierKey(researchRow.tier())), researchRow.level(), researchRow.maximumLevel()));
            }
            Button node = addRenderableWidget(Button.builder(label, b -> { selected = index; refresh(); })
                    .tooltip(Tooltip.create(tip)).bounds(nx, ny, p(194), p(34)).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("‹"), b -> { page--; refresh(); })
                .bounds(left + p(28), top + p(415), p(36), p(20)).build());
        next = addRenderableWidget(Button.builder(Component.literal("›"), b -> { page++; refresh(); })
                .bounds(left + p(70), top + p(415), p(36), p(20)).build());
        previous.active = page > 0;
        next.active = page + 1 < pages;
        if (mode == Mode.RESEARCH) {
            purchase = addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.purchase"), b -> openConfirmation())
                    .bounds(left + p(635), top + p(415), p(190), p(25)).build());
            purchase.active = selected >= 0 && selected < research.size()
                    && research.get(selected).prerequisitesDefined()
                    && research.get(selected).availability() == ResearchPurchaseRules.Result.PURCHASED;
        }
        // Stable ordering follows the existing automation hooks: back, node entries, page buttons, purchase.
        int tabWidth = p(108);
        int tabX = left + p(28);
        for (int i = 0; i < DISCIPLINES.length; i++) {
            String key = DISCIPLINES[i];
            Button tab = addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.discipline." + key), b -> {
                discipline = key; page = 0; selected = 0; refresh();
            }).bounds(tabX + i * (tabWidth + p(3)), top + p(31), tabWidth, p(22)).build());
            tab.active = !key.equals(discipline);
        }
    }

    private void openConfirmation() {
        if (mode != Mode.RESEARCH || selected < 0 || selected >= research.size()) return;
        var row = research.get(selected);
        if (row.nextCost() == null || row.availability() != ResearchPurchaseRules.Result.PURCHASED
                || !row.prerequisitesDefined()) return;
        minecraft.gui.setScreen(new ResearchPurchaseConfirmScreen(this, row));
    }

    /** Opens the same confirmation step used by the visible purchase control. */
    public void validationActivatePurchase() { openConfirmation(); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(width - p(32), p(1120)), panelH = Math.min(height - p(32), p(650));
        int x = (width - panelW) / 2, y = (height - panelH) / 2;
        frame(g, x, y, panelW, panelH);
        g.text(font, title, x + p(22), y + p(13), 0xFF181818);
        g.text(font, Component.translatable("moreminecrafts.discipline." + discipline), x + p(25), y + p(58), 0xFF333333);
        int listX = x + p(18), listY = y + p(83);
        g.fill(listX, listY, listX + p(435), listY + p(320), 0xFF292929);
        g.fill(listX + p(4), listY + p(4), listX + p(431), listY + p(316), 0xFFB5B5B2);
        g.fill(listX + p(18), listY + p(16), listX + p(21), listY + p(300), 0xFF474747);
        for (int i = 0; i < 7; i++) {
            int index = page * 7 + i;
            int cx = listX + p(23) + (i % 2) * p(206);
            int cy = listY + p(9) + (i / 2) * p(49);
            // Spine is a navigation guide only; requirements are shown separately from this line.
            g.fill(listX + p(18), cy + p(16), cx, cy + p(19), 0xFF6D675B);
            if (index == selected) g.fill(cx - p(2), cy - p(2), cx + p(196), cy + p(36), 0xFF8D784B);
            boolean known;
            if (mode == Mode.KNOWLEDGE) known = index < knowledge.size() && knowledge.get(index).unlocked();
            else known = index < research.size() && research.get(index).level() > 0;
            g.fill(cx + p(3), cy + p(3), cx + p(27), cy + p(27), known ? 0xFF4E8B55 : 0xFF515151);
            g.fill(cx + p(7), cy + p(7), cx + p(23), cy + p(23), known ? 0xFF9FCE70 : 0xFF282828);
        }
        int detailsX = x + p(472), detailsY = y + p(83), detailsW = panelW - p(490);
        g.fill(detailsX, detailsY, detailsX + detailsW, detailsY + p(320), 0xFFB5B5B2);
        g.fill(detailsX + p(4), detailsY + p(4), detailsX + detailsW - p(4), detailsY + p(316), 0xFFD0D0CD);
        if (mode == Mode.KNOWLEDGE && selected >= 0 && selected < knowledge.size()) renderKnowledge(g, knowledge.get(selected), detailsX + p(14), detailsY + p(14), detailsW - p(28));
        if (mode == Mode.RESEARCH && selected >= 0 && selected < research.size()) renderResearch(g, research.get(selected), detailsX + p(14), detailsY + p(14), detailsW - p(28));
        g.text(font, Component.translatable("moreminecrafts.ui.page", page + 1, Math.max(1,
                ((mode == Mode.KNOWLEDGE ? knowledge.size() : research.size()) + 6) / 7)), x + p(28), y + p(421), 0xFF333333);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void renderKnowledge(GuiGraphicsExtractor g, ProgressionScreenData.KnowledgeRow row, int x, int y, int width) {
        g.text(font, Component.translatable(row.titleKey()), x, y, 0xFF151515);
        int yy = y + p(25);
        if (row.unresolved()) {
            line(g, "moreminecrafts.ui.unresolved", x, yy, 0xFF7A3B2C); yy += p(20);
            line(g, "moreminecrafts.ui.knowledge.condition", x, yy, 0xFF252525); yy += p(16);
            g.text(font, Component.translatable("moreminecrafts.ui.unresolved"), x, yy, 0xFF7A3B2C); yy += p(21);
        }
        if (!row.unresolved()) {
            g.text(font, Component.translatable(row.descriptionKey()), x, yy, 0xFF292929); yy += p(22);
            line(g, "moreminecrafts.ui.knowledge.condition", x, yy, 0xFF252525); yy += p(16);
            g.text(font, Component.translatable(row.descriptionKey()), x, yy, 0xFF4A4A4A); yy += p(21);
            g.text(font, Component.translatable("moreminecrafts.ui.progress", row.conditionProgress(), row.conditionTarget()), x, yy, 0xFF4A4A4A); yy += p(22);
            line(g, "moreminecrafts.ui.knowledge.reward", x, yy, 0xFF252525); yy += p(17);
            g.text(font, Component.translatable(rewardKey(row.rewardKey())), x, yy, 0xFF4A4A4A); yy += p(20);
        }
        g.text(font, Component.translatable(row.unlocked() ? "moreminecrafts.ui.state.unlocked" : "moreminecrafts.ui.state.locked"), x, yy + p(3),
                row.unlocked() ? 0xFF287137 : 0xFF684E22);
    }

    private void renderResearch(GuiGraphicsExtractor g, ProgressionScreenData.ResearchRow row, int x, int y, int width) {
        g.text(font, Component.translatable(row.titleKey()), x, y, 0xFF151515);
        int yy = y + p(24);
        g.text(font, Component.translatable(row.descriptionKey()), x, yy, 0xFF333333); yy += p(21);
        g.text(font, Component.translatable("moreminecrafts.ui.research.tier_level",
                Component.translatable(tierKey(row.tier())),
                row.level(), row.maximumLevel()), x, yy, 0xFF333333); yy += p(21);
        if (!row.prerequisitesDefined()) {
            line(g, "moreminecrafts.ui.unresolved", x, yy, 0xFF7A3B2C); yy += p(19);
        } else {
            line(g, "moreminecrafts.ui.research.prerequisites", x, yy, 0xFF252525); yy += p(17);
            String knowledgeReq = row.requiredKnowledge().stream().sorted().map(n -> "#" + n).reduce((a,b) -> a + ", " + b).orElse("—");
            String researchReq = row.prerequisiteResearch().stream().map(ProgressionBrowseScreen::researchTitle)
                    .reduce((a,b) -> a + ", " + b).orElse("—");
            g.text(font, Component.translatable("moreminecrafts.ui.research.requirements", knowledgeReq, researchReq), x, yy, 0xFF4A4A4A); yy += p(20);
        }
        g.text(font, Component.translatable("moreminecrafts.ui.purchase.state." + row.availability().name().toLowerCase()), x, yy, colorFor(row.availability())); yy += p(20);
        if (row.nextCost() != null) {
            g.text(font, Component.translatable("moreminecrafts.ui.cost_xp", row.nextCost().rawXp()), x, yy, 0xFF282828); yy += p(19);
            for (var cost : row.nextCost().materials()) {
                if (yy > y + p(245)) break;
                String itemName = Component.translatable("item." + cost.itemId().replace(':', '.')).getString();
                String costKey = cost.compressedUnits() > 0
                        ? "moreminecrafts.ui.cost_material_compressed" : "moreminecrafts.ui.cost_material";
                g.text(font, cost.compressedUnits() > 0
                        ? Component.translatable(costKey, itemName, cost.count(), cost.compressedUnits())
                        : Component.translatable(costKey, itemName, cost.count()), x, yy, 0xFF424242);
                yy += p(16);
            }
        }
        String result = row.level() > 0 ? "moreminecrafts.ui.state.purchased" : "moreminecrafts.ui.state.locked";
        g.text(font, Component.translatable(result), x, Math.min(y + p(276), yy + p(4)), row.level() > 0 ? 0xFF287137 : 0xFF684E22);
        int action = ProgressionClientApi.lastActionResult();
        if (row.id().equals(ProgressionClientApi.lastAction()) && action >= 0)
            g.text(font, Component.translatable("moreminecrafts.ui.server_result", action), x, y + p(294), 0xFF294B79);
    }

    private static String researchTitle(String id) {
        var entry = dev.itzgamesgd.moreminecrafts.progression.ResearchCatalog.byId(id);
        return entry == null ? Component.translatable("moreminecrafts.ui.unresolved").getString()
                : Component.translatable(entry.titleKey()).getString();
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

    private static int colorFor(ResearchPurchaseRules.Result result) {
        return switch (result) {
            case PURCHASED -> 0xFF286C35;
            case MISSING_KNOWLEDGE, MISSING_PREREQUISITE, INVALID_STATE -> 0xFF8A482D;
            case MISSING_MATERIALS, INSUFFICIENT_XP -> 0xFF886216;
            default -> 0xFF4A4A4A;
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

    private void line(GuiGraphicsExtractor g, String key, int x, int y, int color) {
        g.text(font, Component.translatable(key), x, y, color);
    }

    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF181818);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFFDEDEDB);
        g.fill(x + 7, y + 7, x + w - 7, y + 10, 0xFFB5B5B2);
    }
}
