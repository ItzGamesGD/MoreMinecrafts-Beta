package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Owner-approved horizontal research scenes (2026-11-25): every discipline file must hold exactly its research nodes,
 * every drawn connector must be a real prerequisite, and nodes must not overlap. Cross-lane connectors are an
 * approved exception and are not rejected here.
 */
final class ResearchSheetLayoutTest {
    static final List<String> DISCIPLINES = List.of("resources", "ecology", "world", "combat", "living", "alchemy", "production");

    private static JsonObject read(String path) throws Exception {
        var stream = ResearchSheetLayoutTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void everyDisciplineScenePlacesExactlyItsResearchNodesWithRealConnectors() throws Exception {
        JsonArray layoutNodes = read("/assets/moreminecrafts/progression/node_layout.json").getAsJsonArray("nodes");
        for (String discipline : DISCIPLINES) {
            JsonObject scene = read("/assets/moreminecrafts/progression/research_sheets/" + discipline + ".json");
            assertEquals("research", scene.get("screen").getAsString(), discipline);
            assertEquals(discipline, scene.get("discipline").getAsString(), discipline);
            int nodeGui = scene.has("nodeGuiSize") ? scene.get("nodeGuiSize").getAsInt() : 32;
            int nodeArt = nodeGui * 2;
            int art = scene.get("artPxPerGuiPx").getAsInt();
            assertEquals(2, art, discipline);

            Set<String> expected = new HashSet<>();
            for (JsonElement e : layoutNodes) {
                JsonObject node = e.getAsJsonObject();
                if ("research".equals(node.get("screen").getAsString()) && discipline.equals(node.get("discipline").getAsString()))
                    expected.add(node.get("id").getAsString());
            }
            Map<String, int[]> slots = new HashMap<>();
            for (JsonElement e : scene.getAsJsonArray("slots")) {
                JsonObject slot = e.getAsJsonObject();
                assertNull(slots.put(slot.get("id").getAsString(), new int[]{slot.get("x").getAsInt(), slot.get("y").getAsInt()}), discipline);
            }
            assertEquals(expected, slots.keySet(), discipline);

            int w = scene.getAsJsonObject("sheet").get("w").getAsInt(), h = scene.getAsJsonObject("sheet").get("h").getAsInt();
            for (var entry : slots.entrySet()) {
                int[] p = entry.getValue();
                assertTrue(p[0] - nodeArt / 2 >= 0 && p[0] + nodeArt / 2 <= w, discipline + " " + entry.getKey() + " x");
                assertTrue(p[1] - nodeArt / 2 >= 0 && p[1] + nodeArt / 2 <= h, discipline + " " + entry.getKey() + " y");
            }
            var ids = List.copyOf(slots.keySet());
            for (int i = 0; i < ids.size(); i++) for (int j = i + 1; j < ids.size(); j++) {
                int[] a = slots.get(ids.get(i)), b = slots.get(ids.get(j));
                assertTrue(Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1])) >= nodeArt + 16 * 2,
                        discipline + " nodes too close: " + ids.get(i) + " " + ids.get(j));
            }

            Map<String, Set<String>> drawn = new HashMap<>();
            for (JsonElement e : scene.getAsJsonArray("edges")) {
                JsonObject edge = e.getAsJsonObject();
                String from = edge.get("from").getAsString(), to = edge.get("to").getAsString();
                assertTrue(slots.containsKey(from) && slots.containsKey(to), discipline + " " + from + "->" + to);
                assertTrue(ResearchCatalog.byId(to).researchPrerequisites().contains(from), from + " -> " + to + " is not a prerequisite");
                drawn.computeIfAbsent(to, k -> new HashSet<>()).add(from);
                JsonArray route = edge.getAsJsonArray("route");
                assertTrue(route.size() >= 2, discipline);
                for (int i = 0; i + 1 < route.size(); i++) {
                    var a = route.get(i).getAsJsonArray(); var b = route.get(i + 1).getAsJsonArray();
                    assertTrue(a.get(0).getAsInt() == b.get(0).getAsInt() || a.get(1).getAsInt() == b.get(1).getAsInt(),
                            discipline + " connector segments must be orthogonal");
                }
                var first = route.get(0).getAsJsonArray();
                var last = route.get(route.size() - 1).getAsJsonArray();
                int[] pp = slots.get(from), cc = slots.get(to);
                // endpoint check applies to the 2026-11-25 64 px scenes; the approved 32 px Resources pilot is not re-judged here
                if (nodeGui == 64) {
                    assertTrue(Math.abs(first.get(0).getAsInt() - pp[0]) <= nodeArt / 2 && Math.abs(first.get(1).getAsInt() - pp[1]) <= nodeArt / 2,
                            discipline + " connector starts inside parent " + from);
                    assertTrue(Math.abs(last.get(0).getAsInt() - cc[0]) <= nodeArt / 2 && Math.abs(last.get(1).getAsInt() - cc[1]) <= nodeArt / 2,
                            discipline + " connector ends inside child " + to);
                }
            }
            for (String id : slots.keySet()) {
                for (String prerequisite : ResearchCatalog.byId(id).researchPrerequisites()) {
                    if (!slots.containsKey(prerequisite)) continue;
                    // transitive edges may be omitted, as in the approved Resources pilot test
                    Set<String> seen = new HashSet<>();
                    var queue = new java.util.ArrayDeque<>(drawn.getOrDefault(id, Set.of()));
                    boolean found = false;
                    while (!queue.isEmpty() && !found) {
                        String next = queue.pop();
                        if (!seen.add(next)) continue;
                        found = next.equals(prerequisite);
                        queue.addAll(drawn.getOrDefault(next, Set.of()));
                    }
                    assertTrue(found, discipline + " prerequisite not shown: " + prerequisite + " -> " + id);
                }
            }
        }
    }

    @Test void kitHasLargeFramesForSixtyFourPixelNodes() {
        for (String state : List.of("locked", "available", "owned", "maxed"))
            assertNotNull(getClass().getResource("/assets/moreminecrafts/textures/gui/research_tree/node_" + state + "_lg.png"), state);
    }
    static final List<String> KNOWLEDGE_DISCIPLINES = DISCIPLINES;

    @Test void everyKnowledgeScenePlacesExactlyItsKnowledgeNodesWithoutConnectors() throws Exception {
        JsonArray layoutNodes = read("/assets/moreminecrafts/progression/node_layout.json").getAsJsonArray("nodes");
        int total = 0;
        for (String discipline : KNOWLEDGE_DISCIPLINES) {
            JsonObject scene = read("/assets/moreminecrafts/progression/knowledge_sheets/" + discipline + ".json");
            assertEquals("knowledge", scene.get("screen").getAsString(), discipline);
            assertEquals(discipline, scene.get("discipline").getAsString(), discipline);
            assertEquals(64, scene.get("nodeGuiSize").getAsInt(), discipline);
            assertEquals(0, scene.getAsJsonArray("edges").size(), discipline + " knowledge has no prerequisite edges");
            Set<String> expected = new HashSet<>();
            for (JsonElement e : layoutNodes) {
                JsonObject node = e.getAsJsonObject();
                if ("knowledge".equals(node.get("screen").getAsString()) && discipline.equals(node.get("discipline").getAsString()))
                    expected.add(node.get("id").getAsString());
            }
            Map<String, int[]> slots = new HashMap<>();
            for (JsonElement e : scene.getAsJsonArray("slots")) {
                JsonObject slot = e.getAsJsonObject();
                assertNull(slots.put(slot.get("id").getAsString(), new int[]{slot.get("x").getAsInt(), slot.get("y").getAsInt()}), discipline);
            }
            assertEquals(expected, slots.keySet(), discipline);
            total += slots.size();
            int w = scene.getAsJsonObject("sheet").get("w").getAsInt(), h = scene.getAsJsonObject("sheet").get("h").getAsInt();
            for (var entry : slots.entrySet()) {
                int[] p = entry.getValue();
                assertTrue(p[0] - 64 >= 0 && p[0] + 64 <= w && p[1] - 64 >= 0 && p[1] + 64 <= h, discipline + " " + entry.getKey());
            }
            var ids = List.copyOf(slots.keySet());
            for (int i = 0; i < ids.size(); i++) for (int j = i + 1; j < ids.size(); j++) {
                int[] a = slots.get(ids.get(i)), b = slots.get(ids.get(j));
                assertTrue(Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1])) >= 128 + 32, discipline + " nodes too close");
            }
        }
        assertEquals(70, total, "knowledge roster is 70 nodes");
    }
}
