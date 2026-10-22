package dev.itzgamesgd.moreminecrafts.progression;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Recovered chapter/sub-branch/icon assignments, restricted to existing registered quests. No game rules. */
public final class QuestPresentationData {
    private record Data(List<QuestTreeStructure.Chapter> chapters, Map<String, String> icons) {}
    private static final Data DATA = load();
    private QuestPresentationData() {}
    private static Data load() {
        var stream = QuestPresentationData.class.getResourceAsStream(
                "/assets/moreminecrafts/progression/quest_presentation.json");
        if (stream == null) throw new IllegalStateException("Missing recovered quest presentation");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Data.class);
        } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
    }
    public static QuestTreeStructure tree(Set<String> registered) {
        var chapters = DATA.chapters().stream().map(ch -> new QuestTreeStructure.Chapter(ch.id(), ch.titleKey(),
                ch.subBranches().stream().map(sub -> new QuestTreeStructure.SubBranch(sub.id(), sub.titleKey(),
                        sub.nodeIds().stream().filter(registered::contains).toList()))
                        .filter(sub -> !sub.nodeIds().isEmpty()).toList()))
                .filter(ch -> !ch.subBranches().isEmpty()).toList();
        return new QuestTreeStructure(chapters, List.of());
    }
    public static QuestTreeStructure registeredTree() {
        return tree(XpQuestCatalogData.catalog().quests().stream().filter(XpQuestCatalogData.Spec::registered)
                .map(XpQuestCatalogData.Spec::id).collect(java.util.stream.Collectors.toSet()));
    }
    public static String icon(String id) { return DATA.icons().getOrDefault(id, "minecraft:book"); }
}
