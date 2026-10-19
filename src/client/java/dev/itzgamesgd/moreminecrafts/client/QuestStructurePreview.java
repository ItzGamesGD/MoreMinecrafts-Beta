package dev.itzgamesgd.moreminecrafts.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.QuestTreeStructure;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;

/**
 * TEST-ONLY presentation data: the proposed 200-quest structure loaded into the existing quest-tree renderer.
 *
 * <p>It is unreachable in normal play: {@link #load()} returns null unless the visual-validation harness
 * is enabled and an explicit proposal directory is supplied. It registers nothing, claims nothing and never
 * calls the server. Node states and every label are synthetic and marked as preview labels; they are not
 * production translations or real progress.
 */
final class QuestStructurePreview {
    static final String DIRECTORY_ENV = "MOREMINECRAFTS_STRUCTURE_PREVIEW_DIR";
    private static final String HARNESS_ENV = "MOREMINECRAFTS_VISUAL_VALIDATION";
    private static final String[] SAMPLE_MOBS = {"zombie", "skeleton", "creeper", "spider", "cave_spider", "enderman",
            "witch", "slime", "magma_cube", "blaze", "ghast", "piglin", "hoglin", "zoglin", "drowned", "husk", "stray",
            "phantom", "pillager", "vindicator", "evoker", "ravager", "vex", "guardian", "silverfish", "endermite",
            "shulker", "wither_skeleton", "zombified_piglin", "bogged", "breeze", "creaking", "warden", "squid",
            "glow_squid", "polar_bear", "wolf", "llama", "panda", "goat"};

    private final QuestTreeStructure structure;
    private final Map<String, ProgressionScreenData.QuestRow> rows = new LinkedHashMap<>();
    private final Map<String, Component> titles = new HashMap<>();
    private final Map<String, String> details = new HashMap<>();
    private final Map<String, Component> chapterTitles = new HashMap<>();
    private final Map<String, String> icons = new HashMap<>();
    private final Map<String, String> subOfQuest = new HashMap<>();
    private final Map<String, Component> subTitles = new HashMap<>();
    private final int subBranchCount;
    private final boolean korean;
    private final Map<String, String> questIcons = new HashMap<>();
    private final Map<String, String> statuses = new HashMap<>();
    private final Map<String, Boolean> registered = new HashMap<>();

    private QuestStructurePreview(QuestTreeStructure structure, int subBranchCount, boolean korean) {
        this.structure = structure;
        this.subBranchCount = subBranchCount;
        this.korean = korean;
    }

    /** Returns the preview, or null when it is not explicitly enabled (always the case in normal play). */
    static QuestStructurePreview load(String languageCode) {
        if (!"1".equals(System.getenv(HARNESS_ENV))) return null;
        String directory = System.getenv(DIRECTORY_ENV);
        if (directory == null || directory.isBlank()) return null;
        try {
            Path root = Path.of(directory);
            JsonObject mapping = parse(root.resolve("quest_structure_mapping.json"));
            JsonObject ledger = parse(root.resolve("edge_ledger.json"));
            JsonObject icons = parse(root.resolve("quest_icons.json"));
            boolean ko = languageCode != null && languageCode.startsWith("ko");
            List<QuestTreeStructure.Chapter> chapters = new ArrayList<>();
            Map<String, List<String>> idsByChapter = new LinkedHashMap<>();
            for (JsonElement element : mapping.getAsJsonArray("quests")) {
                JsonObject quest = element.getAsJsonObject();
                idsByChapter.computeIfAbsent(quest.get("chapter").getAsString(), key -> new ArrayList<>())
                        .add(quest.get("quest_id").getAsString());
            }
            int subs = 0;
            QuestStructurePreview[] holder = new QuestStructurePreview[1];
            List<JsonObject> chapterObjects = new ArrayList<>();
            for (JsonElement element : mapping.getAsJsonArray("chapters")) chapterObjects.add(element.getAsJsonObject());
            for (JsonObject chapter : chapterObjects) {
                String id = chapter.get("id").getAsString();
                chapters.add(QuestTreeStructure.Chapter.of(id, "proposal.chapter." + id, idsByChapter.getOrDefault(id, List.of())));
                subs += chapter.getAsJsonArray("sub_branches").size();
            }
            List<QuestTreeStructure.Edge> edges = new ArrayList<>();
            for (JsonElement element : ledger.getAsJsonArray("edges")) {
                JsonObject edge = element.getAsJsonObject();
                // Gameplay candidates stay disabled: only presentation-only links are drawn.
                if ("presentation-only".equals(edge.get("enforcement").getAsString()))
                    edges.add(new QuestTreeStructure.Edge(edge.get("parent").getAsString(), edge.get("child").getAsString()));
            }
            QuestStructurePreview preview = new QuestStructurePreview(new QuestTreeStructure(chapters, edges), subs, ko);
            preview.populate(mapping, chapterObjects);
            for (JsonElement element : icons.getAsJsonArray("icons")) {
                JsonObject icon = element.getAsJsonObject();
                preview.questIcons.put(icon.get("quest_id").getAsString(), icon.get("icon_item_id").getAsString());
            }
            return preview;
        } catch (IOException | RuntimeException e) {
            dev.itzgamesgd.moreminecrafts.MoreMinecrafts.LOGGER.error("STRUCTURE_PREVIEW_LOAD_FAILED {}", e.toString());
            return null;
        }
    }

    private static JsonObject parse(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void populate(JsonObject mapping, List<JsonObject> chapterObjects) {
        Map<String, String> chapterOfQuest = new HashMap<>();
        for (JsonObject chapter : chapterObjects) {
            String id = chapter.get("id").getAsString();
            JsonObject title = chapter.getAsJsonObject("title");
            chapterTitles.put("proposal.chapter." + id, Component.literal(title.get(korean ? "ko" : "en").getAsString()));
            icons.put(id, chapter.get("icon_item_id").getAsString());
            for (JsonElement sub : chapter.getAsJsonArray("sub_branches")) {
                JsonObject subObject = sub.getAsJsonObject();
                subTitles.put(subObject.get("id").getAsString(),
                        Component.literal(subObject.getAsJsonObject("title").get(korean ? "ko" : "en").getAsString()));
            }
        }
        int index = 0;
        Map<String, Integer> perChapter = new HashMap<>();
        for (JsonElement element : mapping.getAsJsonArray("quests")) {
            JsonObject quest = element.getAsJsonObject();
            String id = quest.get("quest_id").getAsString();
            String chapter = quest.get("chapter").getAsString();
            String sub = quest.get("sub_branch").getAsString();
            chapterOfQuest.put(id, chapter);
            subOfQuest.put(id, sub);
            int number = perChapter.merge(chapter, 1, Integer::sum);
            String chapterName = chapterTitles.get("proposal.chapter." + chapter).getString();
            boolean longLabel = index % 7 == 3;
            String label = (korean ? "[미리보기] " : "[PREVIEW] ") + chapterName + " " + number
                    + (longLabel ? (korean ? " — 툴팁 줄바꿈을 확인하기 위해 일부러 길게 만든 임시 미리보기 라벨입니다"
                            : " — a deliberately long temporary preview label used to test tooltip wrapping") : "");
            titles.put(id, Component.literal(label));
            statuses.put(id, quest.get("status").getAsString());
            registered.put(id, quest.get("registered").getAsBoolean());
            details.put(id, (korean ? "미리보기: 상태 " : "Preview: status ") + quest.get("status").getAsString()
                    + (quest.get("registered").getAsBoolean() ? (korean ? " · 등록됨" : " · registered") : (korean ? " · 미등록" : " · unregistered"))
                    + " · " + quest.get("tier").getAsString());
            int target = 10 + (index % 5) * 10;
            // Mixed synthetic states: every 4th complete, every 4th+1 partly done, the rest untouched.
            boolean complete = index % 4 == 0;
            int progress = complete ? target : (index % 4 == 1 ? target / 2 : 0);
            rows.put(id, new ProgressionScreenData.QuestRow(id, quest.get("category").getAsString(),
                    id, id, id, progress, target, quest.get("reward_raw_xp").getAsInt(), complete, false, groupsFor(index, id)));
            index++;
        }
    }

    /** Synthetic target lists so tooltips cover plain, grouped and scrolling shapes. Vanilla mob names only. */
    private static List<ProgressionScreenData.QuestGroup> groupsFor(int index, String id) {
        if (index % 17 == 0) {
            List<ProgressionScreenData.QuestTarget> targets = new ArrayList<>();
            for (int i = 0; i < SAMPLE_MOBS.length; i++) targets.add(target(SAMPLE_MOBS[i], i % 5 == 0));
            return List.of(new ProgressionScreenData.QuestGroup("species", targets, false));
        }
        if (index % 11 == 0) {
            return List.of(
                    new ProgressionScreenData.QuestGroup("desert", List.of(target("husk", true), target("spider", false)), true),
                    new ProgressionScreenData.QuestGroup("swamp", List.of(target("witch", false), target("slime", false), target("drowned", false)), false),
                    new ProgressionScreenData.QuestGroup("snowy", List.of(target("stray", false), target("polar_bear", false)), false));
        }
        if (index % 5 == 0) return List.of(new ProgressionScreenData.QuestGroup("species",
                List.of(target("zombie", true), target("skeleton", false), target("creeper", false)), false));
        return List.of();
    }

    private static ProgressionScreenData.QuestTarget target(String path, boolean complete) {
        return new ProgressionScreenData.QuestTarget("minecraft:" + path, "entity.minecraft." + path, complete);
    }

    QuestTreeStructure structure() { return structure; }
    Map<String, ProgressionScreenData.QuestRow> rows() { return rows; }
    Component questTitle(String id) { return titles.get(id); }
    String detail(String id) { return details.get(id); }
    String subOf(String id) { return subOfQuest.get(id); }
    Component subTitle(String subId) { return subTitles.get(subId); }
    Component chapterTitle(String key) { return chapterTitles.get(key); }
    /** Deterministic per-quest icon (D4); falls back to the chapter icon only if the file has no entry. */
    String iconForQuest(String questId, String chapterId) { return questIcons.getOrDefault(questId, iconFor(chapterId)); }
    java.util.Collection<String> iconItemIds() { return questIcons.values(); }
    String statusOf(String questId) { return statuses.getOrDefault(questId, "NOW"); }
    boolean isRegistered(String questId) { return registered.getOrDefault(questId, false); }
    String iconFor(String chapterId) { return icons.getOrDefault(chapterId, "minecraft:book"); }
    int subBranchCount() { return subBranchCount; }
    boolean korean() { return korean; }
    String banner() {
        return korean ? "[미리보기] 200개 퀘스트 구조 제안 (테스트 전용 · 실제 데이터 아님)"
                : "[PREVIEW] 200-quest structure proposal (test-only, not real data)";
    }
}
