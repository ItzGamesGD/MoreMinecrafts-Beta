package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Validates the 200-quest structure proposal against the authoritative catalog. Presentation only. */
class QuestStructureProposalTest {
    static final Path STRUCTURE = Path.of("docs/design/xp-quest/structure");

    static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    static JsonObject mapping() throws IOException {
        return read(STRUCTURE.resolve("quest_structure_mapping.json"));
    }

    static Map<String, JsonObject> catalog() throws IOException {
        Map<String, JsonObject> byId = new HashMap<>();
        JsonArray quests = read(Path.of("src/main/resources/data/moreminecrafts/xp_quest/catalog.json"))
                .getAsJsonArray("quests");
        for (JsonElement element : quests) byId.put(element.getAsJsonObject().get("id").getAsString(), element.getAsJsonObject());
        return byId;
    }

    @Test
    void everyCatalogQuestAppearsExactlyOnceWithMatchingFacts() throws IOException {
        Map<String, JsonObject> catalog = catalog();
        JsonArray entries = mapping().getAsJsonArray("quests");
        assertEquals(200, catalog.size(), "catalog size changed; the proposal must be regenerated");
        assertEquals(200, entries.size());
        Set<String> seen = new HashSet<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            String id = entry.get("quest_id").getAsString();
            assertTrue(seen.add(id), "duplicate quest " + id);
            JsonObject source = catalog.get(id);
            assertTrue(source != null, "unknown quest " + id);
            assertEquals(source.get("tab").getAsString(), entry.get("category").getAsString(), id);
            assertEquals(source.get("status").getAsString(), entry.get("status").getAsString(), id);
            assertEquals(source.get("tier").getAsString(), entry.get("tier").getAsString(), id);
            assertEquals(source.get("registered").getAsBoolean(), entry.get("registered").getAsBoolean(), id);
            assertEquals(source.get("rewardRawXp").getAsInt(), entry.get("reward_raw_xp").getAsInt(), id);
            assertTrue(entry.get("sub_branch").getAsString().startsWith(entry.get("chapter").getAsString() + "."), id);
        }
        assertEquals(catalog.keySet(), seen);
    }

    @Test
    void presentationOrderIsAContiguousPermutationAndChaptersAreContiguous() throws IOException {
        JsonArray entries = mapping().getAsJsonArray("quests");
        Set<Integer> orders = new HashSet<>();
        String current = null;
        Set<String> closed = new HashSet<>();
        for (JsonElement element : entries) {
            JsonObject entry = element.getAsJsonObject();
            orders.add(entry.get("presentation_order").getAsInt());
            String chapter = entry.get("chapter").getAsString();
            if (!chapter.equals(current)) {
                assertTrue(closed.add(chapter), "chapter " + chapter + " is split across the order");
                current = chapter;
            }
        }
        assertEquals(200, orders.size());
        assertEquals(1, orders.stream().min(Integer::compare).orElseThrow());
        assertEquals(200, orders.stream().max(Integer::compare).orElseThrow());
    }

    @Test
    void chaptersAndSubBranchesAreWellFormedAndLocalized() throws IOException {
        JsonObject mapping = mapping();
        JsonArray chapters = mapping.getAsJsonArray("chapters");
        Set<String> chapterIds = new HashSet<>();
        Set<String> subIds = new HashSet<>();
        int total = 0;
        for (JsonElement element : chapters) {
            JsonObject chapter = element.getAsJsonObject();
            String id = chapter.get("id").getAsString();
            assertTrue(chapterIds.add(id), "duplicate chapter " + id);
            assertFalse(chapter.getAsJsonObject("title").get("en").getAsString().isBlank(), id + " en title");
            assertFalse(chapter.getAsJsonObject("title").get("ko").getAsString().isBlank(), id + " ko title");
            assertTrue(chapter.get("icon_item_id").getAsString().startsWith("minecraft:"), id + " icon");
            for (JsonElement sub : chapter.getAsJsonArray("sub_branches")) {
                JsonObject branch = sub.getAsJsonObject();
                String subId = branch.get("id").getAsString();
                assertTrue(subIds.add(subId), "duplicate sub-branch " + subId);
                assertTrue(subId.startsWith(id + "."), subId);
                assertFalse(branch.getAsJsonObject("title").get("en").getAsString().isBlank(), subId);
                assertFalse(branch.getAsJsonObject("title").get("ko").getAsString().isBlank(), subId);
                assertTrue(branch.get("quest_count").getAsInt() > 0, subId + " is empty");
            }
            total += chapter.get("quest_count").getAsInt();
        }
        assertEquals(15, chapterIds.size());
        assertEquals(200, total);
    }

    @Test
    void categoryTotalsMatchTheBriefAndTheCatalog() throws IOException {
        Map<String, Integer> counts = new TreeMap<>();
        for (JsonElement element : mapping().getAsJsonArray("quests")) {
            counts.merge(element.getAsJsonObject().get("category").getAsString(), 1, Integer::sum);
        }
        Map<String, Integer> expected = new TreeMap<>(Map.ofEntries(
                Map.entry("exploration", 24), Map.entry("dimension", 10), Map.entry("combat", 30),
                Map.entry("boss", 9), Map.entry("mining", 19), Map.entry("farming", 19),
                Map.entry("fishing", 16), Map.entry("crafting", 15), Map.entry("building", 8),
                Map.entry("alchemy", 9), Map.entry("equipment", 20), Map.entry("growth", 21)));
        assertEquals(expected, counts);
    }

    @Test
    void edgeLedgerIsAcyclicReferencesRealQuestsAndEnablesNothing() throws IOException {
        Map<String, JsonObject> catalog = catalog();
        JsonArray edges = read(STRUCTURE.resolve("edge_ledger.json")).getAsJsonArray("edges");
        Map<String, Set<String>> children = new HashMap<>();
        for (JsonElement element : edges) {
            JsonObject edge = element.getAsJsonObject();
            String parent = edge.get("parent").getAsString();
            String child = edge.get("child").getAsString();
            assertTrue(catalog.containsKey(parent) && catalog.containsKey(child), "unknown endpoint in " + edge);
            assertFalse(parent.equals(child), "self dependency " + edge);
            String enforcement = edge.get("enforcement").getAsString();
            if (enforcement.startsWith("gameplay")) {
                assertTrue(enforcement.contains("NOT ENABLED"), "gameplay edge must not be enabled: " + edge);
                assertEquals("awaiting_owner_approval", edge.get("status").getAsString(), "gameplay edge: " + edge);
            } else {
                assertEquals("presentation-only", enforcement);
                assertEquals("proposed", edge.get("status").getAsString());
            }
            children.computeIfAbsent(parent, key -> new HashSet<>()).add(child);
        }
        // Depth-first cycle check across presentation and gameplay candidates together.
        Set<String> done = new HashSet<>();
        for (String start : children.keySet()) assertFalse(hasCycle(start, children, done, new HashSet<>()), "cycle through " + start);
    }

    private static boolean hasCycle(String node, Map<String, Set<String>> children, Set<String> done, Set<String> path) {
        if (done.contains(node)) return false;
        if (!path.add(node)) return true;
        for (String next : children.getOrDefault(node, Set.of())) if (hasCycle(next, children, done, path)) return true;
        path.remove(node);
        done.add(node);
        return false;
    }

    @Test
    void localizationGapIsReportedHonestly() throws IOException {
        // The proposal does not invent quest copy. This pins the current coverage so any change is deliberate.
        JsonObject coverage = read(STRUCTURE.resolve("localization_coverage.json")).getAsJsonObject("quest_copy");
        for (String lang : new String[] {"en", "ko"}) {
            JsonObject fields = coverage.getAsJsonObject(lang).getAsJsonObject("quest_fields_present");
            assertEquals(3, fields.get("title").getAsInt(), lang + " titles: update this pin with the copy work");
            assertEquals(200, coverage.getAsJsonObject(lang).get("quest_fields_expected_each").getAsInt());
        }
    }
}
