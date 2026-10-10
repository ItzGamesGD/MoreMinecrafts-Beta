package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.ResearchCatalog;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Data-driven, provisional list/detail screens. Purchase availability comes from shared server rules. */
public final class ProgressionBrowseScreen extends Screen {
    public enum Mode { KNOWLEDGE, RESEARCH }
    private final Mode mode;
    private int page;
    private int selected;
    private List<ProgressionScreenData.KnowledgeRow> knowledge = List.of();
    private List<ProgressionScreenData.ResearchRow> research = List.of();
    private Button purchase;
    private Button previous;
    private Button next;
    private int dataRevision = -1;

    public ProgressionBrowseScreen(Mode mode) {
        super(Component.literal(mode == Mode.KNOWLEDGE ? "지식" : "연구"));
        this.mode = mode;
    }

    @Override protected void init() { refresh(); }

    @Override public void tick() {
        super.tick();
        if (dataRevision != ProgressionClientApi.revision()) refresh();
    }

    private void refresh() {
        clearWidgets();
        knowledge = ProgressionClientApi.knowledge(null);
        research = ProgressionClientApi.research(null);
        dataRevision = ProgressionClientApi.revision();
        int left = (width - 390) / 2, top = Math.max(28, (height - 220) / 2);
        addRenderableWidget(Button.builder(Component.literal("← 선택"), b ->
                minecraft.gui.setScreen(new ProgressionSelectionScreen())).bounds(left, top - 24, 64, 20).build());
        int count = mode == Mode.KNOWLEDGE ? knowledge.size() : research.size();
        int pageSize = 7;
        int pages = Math.max(1, (count + pageSize - 1) / pageSize);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < pageSize; i++) {
            final int index = page * pageSize + i;
            if (index >= count) break;
            Component label = mode == Mode.KNOWLEDGE
                    ? Component.translatable(knowledge.get(index).titleKey())
                    : Component.translatable(research.get(index).titleKey());
            if (mode == Mode.RESEARCH) label = Component.literal("Lv." + research.get(index).level() + " ").append(label);
            if (mode == Mode.KNOWLEDGE && knowledge.get(index).unlocked()) label = Component.literal("✓ ").append(label);
            addRenderableWidget(Button.builder(label, b -> { selected = index; refresh(); })
                    .bounds(left + 4, top + 5 + i * 20, 188, 18).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; refresh(); })
                .bounds(left + 4, top + 184, 28, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; refresh(); })
                .bounds(left + 164, top + 184, 28, 18).build());
        previous.active = page > 0;
        next.active = page + 1 < pages;
        if (mode == Mode.RESEARCH) {
            purchase = addRenderableWidget(Button.builder(Component.literal("구매 요청"), b -> {
                if (selected >= 0 && selected < research.size()) {
                    var row = research.get(selected);
                    if (row.nextCost() != null) ProgressionClientApi.purchase(row.id(), row.level() + 1);
                }
            }).bounds(left + 203, top + 183, 100, 20).build());
            purchase.active = selected >= 0 && selected < research.size()
                    && research.get(selected).availability() == dev.itzgamesgd.moreminecrafts.progression.ResearchPurchaseRules.Result.PURCHASED;
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = (width - 390) / 2, y = Math.max(28, (height - 220) / 2);
        g.fill(x, y, x + 390, y + 210, 0xEE172129);
        g.text(font, title, x + 8, y - 19, 0xFFFFFFFF);
        g.fill(x + 198, y + 3, x + 384, y + 176, 0xB8202D35);
        if (mode == Mode.KNOWLEDGE && selected >= 0 && selected < knowledge.size()) {
            var row = knowledge.get(selected);
            g.text(font, Component.translatable(row.titleKey()), x + 205, y + 10, 0xFFFFFFFF);
            g.text(font, Component.translatable(row.descriptionKey()), x + 205, y + 25, 0xFFE2E9ED);
            g.text(font, Component.translatable("moreminecrafts.discipline." + row.discipline()), x + 205, y + 40, 0xFFE2E9ED);
            text(g, "상태: " + (row.unlocked() ? "해금" : "미해금"), x + 205, y + 55, 0xFFE2E9ED);
            text(g, "조건: " + row.conditionKey(), x + 205, y + 70, 0xFFE2E9ED);
            text(g, "진행: " + row.conditionProgress() + " / " + row.conditionTarget(), x + 205, y + 85, 0xFFE2E9ED);
            text(g, "보상: " + row.rewardKey(), x + 205, y + 100, 0xFFE2E9ED);
        } else if (mode == Mode.RESEARCH && selected >= 0 && selected < research.size()) {
            var row = research.get(selected);
            g.text(font, Component.translatable(row.titleKey()), x + 205, y + 10, 0xFFFFFFFF);
            g.text(font, Component.translatable(row.descriptionKey()), x + 205, y + 25, 0xFFE2E9ED);
            g.text(font, Component.translatable("moreminecrafts.discipline." + row.discipline()), x + 205, y + 40, 0xFFE2E9ED);
            text(g, "카탈로그/티어: " + row.catalogVersion() + " / " + row.tier(), x + 205, y + 55, 0xFFE2E9ED);
            text(g, "레벨: " + row.level() + " / " + row.maximumLevel(), x + 205, y + 70, 0xFFE2E9ED);
            text(g, "선행 조건: " + (row.prerequisitesDefined()
                    ? row.requiredKnowledge() + " / " + row.prerequisiteResearch() : "미확정"), x + 205, y + 85, 0xFFE2E9ED);
            text(g, "구매 상태: " + row.availability(), x + 205, y + 100, 0xFFE2E9ED);
            if (row.nextCost() != null) {
                text(g, "원시 XP: " + row.nextCost().rawXp(), x + 205, y + 115, 0xFFE2E9ED);
                int cy = y + 130;
                for (var material : row.nextCost().materials()) {
                    text(g, material.compressedUnits() > 0
                            ? material.itemId() + " × " + material.compressedUnits() + " compressed (deferred) or " + material.count() + " original"
                            : material.itemId() + " × " + material.count(), x + 205, cy, 0xFFE2E9ED);
                    cy += 15;
                    if (cy > y + 169) break;
                }
            }
            int result = ProgressionClientApi.lastActionResult();
            if (row.id().equals(ProgressionClientApi.lastAction())) text(g, "서버 응답: " + result, x + 205, y + 166, 0xFFFFFF80);
        }
    }

    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        g.text(font, Component.literal(value), x, y, color);
    }
}
