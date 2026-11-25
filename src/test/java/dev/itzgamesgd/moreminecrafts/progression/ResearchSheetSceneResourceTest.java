package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The owner-approved Research / Resources sheet (2026-11-25) must stay consistent with the research catalog. */
final class ResearchSheetSceneResourceTest {
    private static final String KIT = "/assets/moreminecrafts/textures/gui/research_tree/";

    private static JsonObject sheet() throws Exception {
        var stream = ResearchSheetSceneResourceTest.class.getResourceAsStream(
                "/assets/moreminecrafts/progression/research_sheets/resources.json");
        assertNotNull(stream, "resources research sheet must exist");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void everyResourcesResearchNodeHasOneSlotInsideItsZone() throws Exception {
        JsonObject sheet = sheet();
        Set<String> expected = new HashSet<>();
        for (var definition : ResearchCatalog.allCatalogEntries())
            if ("resources".equals(definition.discipline())) expected.add(definition.id());
        Map<String, int[]> slots = new HashMap<>();
        for (JsonElement e : sheet.getAsJsonArray("slots")) {
            JsonObject slot = e.getAsJsonObject();
            assertNull(slots.put(slot.get("id").getAsString(), new int[]{slot.get("x").getAsInt(), slot.get("y").getAsInt()}),
                    "duplicate slot");
        }
        assertEquals(expected, slots.keySet());
        int w = sheet.getAsJsonObject("sheet").get("w").getAsInt(), h = sheet.getAsJsonObject("sheet").get("h").getAsInt();
        for (var entry : slots.entrySet()) {
            int[] p = entry.getValue();
            assertTrue(p[0] - 32 >= 0 && p[0] + 32 <= w && p[1] - 32 >= 0 && p[1] + 32 <= h, entry.getKey());
            assertEquals(0, p[0] % 2, entry.getKey());   // whole GUI pixels
            assertEquals(0, p[1] % 2, entry.getKey());
        }
        var pts = List.copyOf(slots.values());
        for (int i = 0; i < pts.size(); i++) for (int j = i + 1; j < pts.size(); j++) {
            int gap = Math.max(Math.abs(pts.get(i)[0] - pts.get(j)[0]), Math.abs(pts.get(i)[1] - pts.get(j)[1])) - 64;
            assertTrue(gap >= 64, "node frames must keep at least 32 GUI px apart");
        }
    }

    @Test void drawnEdgesAreRealPrerequisitesAndCoverEveryPrerequisite() throws Exception {
        JsonObject sheet = sheet();
        Map<String, Set<String>> drawn = new HashMap<>();
        Map<String, int[]> slots = new HashMap<>();
        for (JsonElement e : sheet.getAsJsonArray("slots"))
            slots.put(e.getAsJsonObject().get("id").getAsString(),
                    new int[]{e.getAsJsonObject().get("x").getAsInt(), e.getAsJsonObject().get("y").getAsInt()});
        for (JsonElement e : sheet.getAsJsonArray("edges")) {
            JsonObject edge = e.getAsJsonObject();
            String from = edge.get("from").getAsString(), to = edge.get("to").getAsString();
            assertTrue(ResearchCatalog.byId(to).researchPrerequisites().contains(from), from + " -> " + to + " is not a prerequisite");
            drawn.computeIfAbsent(to, k -> new HashSet<>()).add(from);
            var route = edge.getAsJsonArray("route");
            assertTrue(route.size() >= 2);
            for (int i = 0; i + 1 < route.size(); i++) {
                var a = route.get(i).getAsJsonArray(); var b = route.get(i + 1).getAsJsonArray();
                assertTrue(a.get(0).getAsInt() == b.get(0).getAsInt() || a.get(1).getAsInt() == b.get(1).getAsInt(),
                        "connector segments must be orthogonal");
            }
            var last = route.get(route.size() - 1).getAsJsonArray();
            assertArrayEquals(slots.get(to), new int[]{last.get(0).getAsInt(), last.get(1).getAsInt()}, "route ends at " + to);
        }
        // Every real prerequisite must be reachable through drawn edges (transitive edges may be omitted).
        for (String id : slots.keySet()) {
            for (String prerequisite : ResearchCatalog.byId(id).researchPrerequisites()) {
                if (!slots.containsKey(prerequisite)) continue;
                Set<String> seen = new HashSet<>();
                var queue = new ArrayDeque<>(drawn.getOrDefault(id, Set.of()));
                boolean found = false;
                while (!queue.isEmpty() && !found) {
                    String next = queue.pop();
                    if (!seen.add(next)) continue;
                    found = next.equals(prerequisite);
                    queue.addAll(drawn.getOrDefault(next, Set.of()));
                }
                assertTrue(found, prerequisite + " -> " + id + " is not shown");
            }
        }
    }

    @Test void approvedKitTexturesArePresent() {
        List<String> names = new java.util.ArrayList<>(List.of("node_locked", "node_available", "node_owned", "node_maxed",
                "node_hover", "node_selected", "banner_normal", "banner_highlight", "banner_dim", "screen_frame",
                "sheet_research_resources"));
        for (String piece : List.of("h", "v", "corner_ne", "corner_nw", "corner_se", "corner_sw", "t_n", "t_e", "t_s", "t_w", "cross"))
            for (String state : List.of("off", "on")) names.add("link_" + piece + "_" + state);
        for (String button : List.of("back", "zoom_in", "zoom_out", "close"))
            for (String state : List.of("normal", "hover", "pressed", "disabled")) names.add("button_" + button + "_" + state);
        assertEquals(49, names.size());
        for (String name : names) assertNotNull(getClass().getResource(KIT + name + ".png"), name);
    }
}
