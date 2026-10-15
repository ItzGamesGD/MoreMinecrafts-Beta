package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

final class ProgressionNodeLayoutCatalogTest {
    private JsonObject load() throws Exception {
        JsonObject source;
        try (var reader = Files.newBufferedReader(Path.of("docs/superpowers/specs/2026-10-11-research-knowledge-node-tree-manifest.json"), StandardCharsets.UTF_8)) {
            source = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonObject normalized = new JsonObject();
        normalized.addProperty("fallbackIconItemId", source.get("fallbackIconItemId").getAsString());
        JsonArray nodes = new JsonArray();
        for (String screen : List.of("research", "knowledge")) {
            for (var element : source.getAsJsonArray(screen)) {
                JsonObject node = element.getAsJsonObject().deepCopy();
                node.addProperty("screen", screen);
                if (screen.equals("knowledge")) node.addProperty("catalog", "knowledge");
                JsonArray center = node.getAsJsonArray("center");
                node.addProperty("x", center.get(0).getAsInt());
                node.addProperty("y", center.get(1).getAsInt());
                node.addProperty("fallbackIconItemId", source.get("fallbackIconItemId").getAsString());
                nodes.add(node);
            }
        }
        normalized.add("nodes", nodes);
        return normalized;
    }

    private JsonObject loadRuntimeLayout() throws Exception {
        var stream = getClass().getResourceAsStream("/assets/moreminecrafts/progression/node_layout.json");
        assertNotNull(stream, "runtime presentation layout resource must exist");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private JsonObject loadLocale(String locale) throws Exception {
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/assets/moreminecrafts/lang/" + locale + ".json"),
                StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void layoutHasExactUniqueCatalogRosterAndPreservesSourceFields() throws Exception {
        JsonObject layout = load();
        JsonArray nodes = layout.getAsJsonArray("nodes");
        List<JsonObject> researchNodes = new ArrayList<>();
        List<JsonObject> knowledgeNodes = new ArrayList<>();
        for (var element : nodes) {
            JsonObject node = element.getAsJsonObject();
            (node.get("screen").getAsString().equals("research") ? researchNodes : knowledgeNodes).add(node);
        }

        assertEquals(162, nodes.size());
        assertEquals(92, researchNodes.size());
        assertEquals(70, knowledgeNodes.size());
        assertEquals(70, researchNodes.stream().filter(n -> n.get("catalog").getAsString().equals("v3")).count());
        assertEquals(17, researchNodes.stream().filter(n -> n.get("catalog").getAsString().equals("v4")).count());
        assertEquals(5, researchNodes.stream().filter(n -> n.get("catalog").getAsString().equals("stage22")).count());
        assertEquals(researchNodes.size(), researchNodes.stream().map(n -> n.get("id").getAsString()).collect(Collectors.toSet()).size());
        assertEquals(knowledgeNodes.size(), knowledgeNodes.stream().map(n -> n.get("id").getAsString()).collect(Collectors.toSet()).size());

        Map<String, ResearchDefinition> research = ResearchCatalog.allCatalogEntries().stream()
                .collect(Collectors.toMap(ResearchDefinition::id, Function.identity()));
        assertEquals(research.keySet(), researchNodes.stream().map(n -> n.get("id").getAsString()).collect(Collectors.toSet()));
        for (JsonObject node : researchNodes) {
            ResearchDefinition source = research.get(node.get("id").getAsString());
            assertNotNull(source);
            assertEquals(source.discipline(), node.get("discipline").getAsString(), source.id());
            assertEquals(source.catalogVersion(), node.get("catalog").getAsString(), source.id());
            assertEquals(source.tier(), node.get("tier").getAsString(), source.id());
            assertEquals(source.maximumLevel(), node.get("levelCap").getAsInt(), source.id());
            assertEquals(source.prerequisitesDefined(), node.get("prerequisitesDefined").getAsBoolean(), source.id());
            assertEquals(source.knowledgePrerequisites(), intSet(node.getAsJsonArray("knowledgePrerequisites")), source.id());
            assertEquals(source.researchPrerequisites(), stringSet(node.getAsJsonArray("researchPrerequisites")), source.id());
            assertEquals("v3".equals(source.catalogVersion()) ? "all_of_knowledge_and_research" : "any_of_knowledge",
                    node.get("prerequisiteSemantics").getAsString(), source.id());
            assertEquals(source.costs().size(), node.getAsJsonArray("costs").size(), source.id());
            for (int level = 0; level < source.costs().size(); level++) {
                var expected = source.costs().get(level);
                JsonObject actual = node.getAsJsonArray("costs").get(level).getAsJsonObject();
                assertEquals(level + 1, actual.get("level").getAsInt(), source.id());
                assertEquals(expected.rawXp(), actual.get("rawXp").getAsInt(), source.id());
                assertEquals(expected.materials().size(), actual.getAsJsonArray("materials").size(), source.id());
                for (int material = 0; material < expected.materials().size(); material++) {
                    var expectedMaterial = expected.materials().get(material);
                    JsonObject actualMaterial = actual.getAsJsonArray("materials").get(material).getAsJsonObject();
                    assertEquals(expectedMaterial.itemId(), actualMaterial.get("itemId").getAsString(), source.id());
                    assertEquals(expectedMaterial.count(), actualMaterial.get("count").getAsInt(), source.id());
                    assertEquals(expectedMaterial.compressedUnits(), actualMaterial.get("compressedUnits").getAsInt(), source.id());
                }
            }
        }

        Map<String, KnowledgeCatalog.Entry> knowledge = KnowledgeCatalog.entries().stream()
                .collect(Collectors.toMap(KnowledgeCatalog.Entry::id, Function.identity()));
        assertEquals(knowledge.keySet(), knowledgeNodes.stream().map(n -> n.get("id").getAsString()).collect(Collectors.toSet()));
        for (JsonObject node : knowledgeNodes) {
            KnowledgeCatalog.Entry source = knowledge.get(node.get("id").getAsString());
            assertNotNull(source);
            assertEquals(source.discipline(), node.get("discipline").getAsString(), source.id());
            assertEquals(source.conditionType().name(), node.get("conditionType").getAsString(), source.id());
            assertEquals(source.conditionKey(), node.get("conditionKey").getAsString(), source.id());
            assertEquals(source.target(), node.get("conditionTarget").getAsInt(), source.id());
            assertEquals(source.rewardKey(), node.get("rewardKey").getAsString(), source.id());
            assertEquals(source.unresolved(), node.get("unresolved").getAsBoolean(), source.id());
        }
    }

    @Test void visualReferencesCoordinatesIconsAndPaddedNodeBoundsAreValid() throws Exception {
        JsonObject layout = load();
        JsonObject english = loadLocale("en_us");
        JsonObject korean = loadLocale("ko_kr");
        Set<String> disciplines = Set.of("resources", "production", "living", "combat", "ecology", "world", "alchemy");
        Set<Integer> knowledgeIds = KnowledgeCatalog.entries().stream().map(KnowledgeCatalog.Entry::number).collect(Collectors.toSet());
        List<JsonObject> nodes = new ArrayList<>();
        for (var element : layout.getAsJsonArray("nodes")) nodes.add(element.getAsJsonObject());

        assertEquals("minecraft:book", layout.get("fallbackIconItemId").getAsString());
        for (JsonObject node : nodes) {
            String id = node.get("id").getAsString();
            String discipline = node.get("discipline").getAsString();
            String branch = node.get("branch").getAsString();
            assertTrue(disciplines.contains(discipline), id);
            assertFalse(branch.isBlank(), id);
            assertEquals("moreminecrafts.ui.branch." + discipline + "." + branch,
                    node.get("branchTitleKey").getAsString(), id);
            assertTrue(english.has(node.get("branchTitleKey").getAsString()), "missing en_us branch label for " + id);
            assertTrue(korean.has(node.get("branchTitleKey").getAsString()), "missing ko_kr branch label for " + id);
            assertTrue(node.get("iconItemId").getAsString().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), id);
            assertTrue(node.get("iconItemId").getAsString().startsWith("minecraft:"), id);
            assertNotSame(Items.AIR, BuiltInRegistries.ITEM.getValue(Identifier.parse(node.get("iconItemId").getAsString())),
                    "unregistered representative item icon for " + id);
            assertEquals("minecraft:book", node.get("fallbackIconItemId").getAsString(), id);
            int branchOrder = node.get("branchOrder").getAsInt();
            int nodeOrder = node.get("nodeOrder").getAsInt();
            assertTrue(branchOrder >= 0 && nodeOrder >= 0, id);
            assertEquals(64 + 192 * branchOrder, node.get("x").getAsInt(), id);
            assertEquals(64 + 72 * nodeOrder, node.get("y").getAsInt(), id);
            if (node.get("screen").getAsString().equals("research")) {
                for (var req : node.getAsJsonArray("knowledgePrerequisites")) assertTrue(knowledgeIds.contains(req.getAsInt()), id);
                if (node.get("catalog").getAsString().equals("v3")) assertTrue(node.get("prerequisitesDefined").getAsBoolean(), id);
            }
        }

        for (String screen : List.of("research", "knowledge")) {
            List<JsonObject> selected = nodes.stream().filter(n -> n.get("screen").getAsString().equals(screen)).toList();
            int halfSizeWithPadding = (screen.equals("research") ? 36 : 32) / 2 + 8;
            for (int i = 0; i < selected.size(); i++) for (int j = i + 1; j < selected.size(); j++) {
                JsonObject a = selected.get(i), b = selected.get(j);
                if (!a.get("discipline").getAsString().equals(b.get("discipline").getAsString())) continue;
                int ax = a.get("x").getAsInt(), ay = a.get("y").getAsInt();
                int bx = b.get("x").getAsInt(), by = b.get("y").getAsInt();
                boolean overlap = Math.abs(ax - bx) < halfSizeWithPadding * 2
                        && Math.abs(ay - by) < halfSizeWithPadding * 2;
                assertFalse(overlap, a.get("id") + " overlaps " + b.get("id"));
            }
        }
    }

    @Test void serializationOrderIsStableAcrossLoads() throws Exception {
        String first = load().toString();
        String second = load().toString();
        assertEquals(first, second);
    }

    @Test void runtimeLayoutContainsOnlyStablePresentationFieldsFromManifest() throws Exception {
        JsonObject approved = load();
        JsonArray runtime = loadRuntimeLayout().getAsJsonArray("nodes");
        Map<String, JsonObject> approvedById = new java.util.LinkedHashMap<>();
        for (var element : approved.getAsJsonArray("nodes")) {
            JsonObject node = element.getAsJsonObject();
            approvedById.put(node.get("id").getAsString(), node);
        }
        assertEquals(157, runtime.size());
        assertEquals(approvedById.size(), runtime.size());
        for (var element : runtime) {
            JsonObject node = element.getAsJsonObject();
            JsonObject source = approvedById.get(node.get("id").getAsString());
            assertNotNull(source, node.get("id").getAsString());
            for (String key : List.of("screen", "discipline", "branch", "branchOrder", "nodeOrder", "x", "y",
                    "iconItemId", "fallbackIconItemId", "branchTitleKey"))
                assertEquals(source.get(key), node.get(key), node.get("id").getAsString() + " / " + key);
            assertEquals(Set.of("id", "screen", "catalog", "discipline", "branch", "branchOrder", "nodeOrder", "x", "y",
                            "iconItemId", "fallbackIconItemId", "branchTitleKey"), node.keySet(),
                    "runtime layout must not duplicate authoritative costs or eligibility for " + node.get("id"));
        }
    }

    @Test void runtimeResolverFailsClosedOnCatalogMismatchAndReturnsStableBranchOrder() {
        ProgressionNodeLayoutCatalog catalog = ProgressionNodeLayoutCatalog.load();
        assertEquals(157, catalog.nodes().size());
        assertEquals(7, catalog.nodes("research", "resources").size());
        assertEquals(List.of("geology_and_quarry", "forestry", "field_mobility"),
                catalog.branches("research", "resources"));
        assertEquals("minecraft:book", catalog.fallbackIconItemId());
        assertNotNull(catalog.byId("combat_root"));
        assertEquals("UNSPECIFIED", ResearchCatalog.byId("combat_root").tier());
    }

    private static Set<Integer> intSet(JsonArray array) {
        Set<Integer> values = new HashSet<>();
        for (var value : array) values.add(value.getAsInt());
        return Set.copyOf(values);
    }

    private static Set<String> stringSet(JsonArray array) {
        Set<String> values = new HashSet<>();
        for (var value : array) values.add(value.getAsString());
        return Set.copyOf(values);
    }
}
