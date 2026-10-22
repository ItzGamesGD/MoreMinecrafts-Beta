package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class QuestPresentationDataTest {
    @Test void everyRegisteredQuestHasExactlyOneNodeAndUnregisteredQuestsStayOut() {
        Set<String> registered = XpQuestCatalogData.catalog().quests().stream().filter(XpQuestCatalogData.Spec::registered)
                .map(XpQuestCatalogData.Spec::id).collect(Collectors.toSet());
        var tree = QuestPresentationData.tree(registered);
        assertEquals(75, registered.size());
        assertEquals(registered, Set.copyOf(tree.allNodeIds()));
        assertEquals(75, tree.allNodeIds().size());
        assertTrue(tree.validate().isEmpty());
        assertTrue(tree.edges().isEmpty(), "no proposed gameplay dependency is enabled");
        assertTrue(QuestPresentationData.tree(Set.of()).chapters().isEmpty());
        assertTrue(QuestPresentationData.tree(Set.of("xpq.not.registered")).allNodeIds().isEmpty());
        for (String id : registered) assertTrue(QuestPresentationData.icon(id).matches("[a-z]+:[a-z0-9_./]+"));
    }
}
