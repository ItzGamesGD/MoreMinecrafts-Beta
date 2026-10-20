package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Stress tests for the quest-tree layout at 200 nodes. Test data only: nothing here registers a quest,
 * changes progress, or touches the production three-node tree.
 */
class QuestTreeStressTest {
    private static final Path OUT = Path.of("build/xp-quest-structure");
    private static final int MIN_CENTRE_GAP = 40;
    private static final int HALF_FRAME = 13;

    record Scenario(String name, QuestTreeStructure structure, QuestTreeLayout.Result layout, double medianMicros,
                    int overlaps, int outsideLane, int edgeCount) { }

    static QuestTreeStructure fromMapping() throws IOException {
        JsonObject mapping = QuestStructureProposalTest.mapping();
        Map<String, List<String>> byChapter = new LinkedHashMap<>();
        for (JsonElement element : mapping.getAsJsonArray("quests")) {
            JsonObject entry = element.getAsJsonObject();
            byChapter.computeIfAbsent(entry.get("chapter").getAsString(), key -> new ArrayList<>())
                    .add(entry.get("quest_id").getAsString());
        }
        List<QuestTreeStructure.Chapter> chapters = new ArrayList<>();
        for (String chapterId : mapping.getAsJsonArray("chapter_order").asList().stream().map(JsonElement::getAsString).toList()) {
            chapters.add(QuestTreeStructure.Chapter.of(chapterId, "proposal.chapter." + chapterId,
                    byChapter.getOrDefault(chapterId, List.of())));
        }
        return new QuestTreeStructure(chapters, edgesFromLedger());
    }

    static List<QuestTreeStructure.Edge> edgesFromLedger() throws IOException {
        JsonArray edges = QuestStructureProposalTest.read(QuestStructureProposalTest.STRUCTURE.resolve("edge_ledger.json"))
                .getAsJsonArray("edges");
        List<QuestTreeStructure.Edge> out = new ArrayList<>();
        for (JsonElement element : edges) {
            JsonObject edge = element.getAsJsonObject();
            // Gameplay candidates are not enabled, so only presentation links feed the structure.
            if (edge.get("enforcement").getAsString().equals("presentation-only")) {
                out.add(new QuestTreeStructure.Edge(edge.get("parent").getAsString(), edge.get("child").getAsString()));
            }
        }
        return out;
    }

    static Scenario run(String name, QuestTreeStructure structure) {
        assertTrue(structure.validate().isEmpty(), name + " structure invalid: " + structure.validate());
        QuestTreeLayout.Result result = QuestTreeLayout.layout(structure);
        int overlaps = 0;
        for (int i = 0; i < result.nodes().size(); i++) {
            for (int j = i + 1; j < result.nodes().size(); j++) {
                var a = result.nodes().get(i);
                var b = result.nodes().get(j);
                if (Math.hypot(a.x() - b.x(), a.y() - b.y()) < MIN_CENTRE_GAP) overlaps++;
            }
        }
        int outside = 0;
        Map<String, QuestTreeLayout.Lane> lanes = new HashMap<>();
        for (var lane : result.lanes()) lanes.put(lane.chapterId(), lane);
        for (var node : result.nodes()) {
            var lane = lanes.get(node.chapterId());
            if (node.y() - HALF_FRAME < lane.top() || node.y() + HALF_FRAME > lane.bottom()) outside++;
            if (node.x() + HALF_FRAME > result.width()) outside++;
        }
        long[] timings = new long[30];
        for (int i = 0; i < timings.length; i++) {
            long start = System.nanoTime();
            QuestTreeLayout.layout(structure);
            timings[i] = System.nanoTime() - start;
        }
        java.util.Arrays.sort(timings);
        double medianMicros = timings[timings.length / 2] / 1000.0;
        return new Scenario(name, structure, result, medianMicros, overlaps, outside, structure.edges().size());
    }

    static QuestTreeStructure synthetic(int[] sizes, boolean crossLinks) {
        List<QuestTreeStructure.Chapter> chapters = new ArrayList<>();
        List<QuestTreeStructure.Edge> edges = new ArrayList<>();
        for (int c = 0; c < sizes.length; c++) {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < sizes[c]; i++) ids.add("syn" + c + "." + i);
            chapters.add(QuestTreeStructure.Chapter.of("syn" + c, "syn.title." + c, ids));
            if (crossLinks && c > 0) {
                // Forward links only: a chapter's first node unlocks the previous chapter's last node.
                edges.add(new QuestTreeStructure.Edge("syn" + (c - 1) + "." + (sizes[c - 1] - 1), "syn" + c + ".0"));
            }
        }
        return new QuestTreeStructure(chapters, edges);
    }

    @Test
    void realTwoHundredQuestGraphLaysOutWithoutOverlapAndIsWithinBudget() throws IOException {
        Scenario real = run("real-200", fromMapping());
        assertEquals(200, real.layout().nodes().size());
        assertEquals(0, real.overlaps(), "overlapping node centres");
        assertEquals(0, real.outsideLane(), "node outside its lane or canvas");
        assertEquals(15, real.layout().lanes().size());
        // Layout is pure arithmetic over a few hundred nodes; anything near a frame budget is a regression.
        assertTrue(real.medianMicros() < 5_000, "median layout time " + real.medianMicros() + " us");
        report(List.of(real, unevenScenario(), denseScenario(), sparseScenario(), crossChapterScenario()));
    }

    static Scenario unevenScenario() {
        return run("uneven-200", synthetic(new int[] {1, 3, 8, 16, 30, 31, 40, 71}, false));
    }

    static Scenario denseScenario() {
        return run("dense-71", synthetic(new int[] {71}, false));
    }

    static Scenario sparseScenario() {
        return run("sparse-1", synthetic(new int[] {1}, false));
    }

    static Scenario crossChapterScenario() {
        return run("cross-chapter-200", synthetic(new int[] {1, 3, 8, 16, 30, 31, 40, 71}, true));
    }

    @Test
    void syntheticScenariosStayOverlapFreeAcrossUnevenChapters() {
        for (Scenario s : List.of(unevenScenario(), denseScenario(), sparseScenario(), crossChapterScenario())) {
            assertEquals(0, s.overlaps(), s.name() + " overlaps");
            assertEquals(0, s.outsideLane(), s.name() + " outside lane");
        }
        assertEquals(200, unevenScenario().layout().nodes().size());
        assertEquals(7, crossChapterScenario().edgeCount());
    }

    @Test
    void cyclicAndUnknownDependenciesAreRejectedInStressGraphs() {
        QuestTreeStructure base = synthetic(new int[] {3, 3}, false);
        QuestTreeStructure cyclic = new QuestTreeStructure(base.chapters(), List.of(
                new QuestTreeStructure.Edge("syn0.0", "syn1.0"), new QuestTreeStructure.Edge("syn1.0", "syn0.0")));
        assertFalse(cyclic.validate().isEmpty(), "cycle must be reported");
        QuestTreeStructure unknown = new QuestTreeStructure(base.chapters(),
                List.of(new QuestTreeStructure.Edge("syn0.0", "missing")));
        assertFalse(unknown.validate().isEmpty(), "unknown endpoint must be reported");
        QuestTreeStructure duplicate = new QuestTreeStructure(List.of(
                QuestTreeStructure.Chapter.of("a", "k", List.of("x")),
                QuestTreeStructure.Chapter.of("b", "k", List.of("x"))), List.of());
        assertFalse(duplicate.validate().isEmpty(), "duplicate id must be reported");
    }

    private static void report(List<Scenario> scenarios) {
        try {
            Files.createDirectories(OUT);
            StringBuilder json = new StringBuilder("{\"scenarios\":[");
            for (int i = 0; i < scenarios.size(); i++) {
                Scenario s = scenarios.get(i);
                json.append(i == 0 ? "" : ",").append("{\"name\":\"").append(s.name()).append("\",\"nodes\":")
                        .append(s.layout().nodes().size()).append(",\"lanes\":").append(s.layout().lanes().size())
                        .append(",\"canvas\":[").append(s.layout().width()).append(',').append(s.layout().height())
                        .append("],\"overlaps\":").append(s.overlaps()).append(",\"outside_lane\":").append(s.outsideLane())
                        .append(",\"median_layout_us\":").append(String.format("%.1f", s.medianMicros())).append('}');
            }
            json.append("]}\n");
            Files.writeString(OUT.resolve("stress-results.json"), json.toString(), StandardCharsets.UTF_8);
            // Full real-graph placement feeds the offline preview generator.
            StringBuilder layout = new StringBuilder("{\"width\":").append(scenarios.get(0).layout().width())
                    .append(",\"height\":").append(scenarios.get(0).layout().height()).append(",\"lanes\":[");
            var real = scenarios.get(0).layout();
            for (int i = 0; i < real.lanes().size(); i++) {
                var lane = real.lanes().get(i);
                layout.append(i == 0 ? "" : ",").append("{\"chapter\":\"").append(lane.chapterId()).append("\",\"top\":")
                        .append(lane.top()).append(",\"height\":").append(lane.height()).append(",\"count\":")
                        .append(lane.nodeCount()).append('}');
            }
            layout.append("],\"nodes\":[");
            for (int i = 0; i < real.nodes().size(); i++) {
                var node = real.nodes().get(i);
                layout.append(i == 0 ? "" : ",").append("{\"id\":\"").append(node.id()).append("\",\"chapter\":\"")
                        .append(node.chapterId()).append("\",\"x\":").append(node.x()).append(",\"y\":").append(node.y()).append('}');
            }
            layout.append("],\"edges\":[");
            var edges = scenarios.get(0).structure().edges();
            for (int i = 0; i < edges.size(); i++) {
                layout.append(i == 0 ? "" : ",").append("[\"").append(edges.get(i).parentId()).append("\",\"")
                        .append(edges.get(i).childId()).append("\"]");
            }
            layout.append("]}\n");
            Files.writeString(OUT.resolve("real-layout.json"), layout.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("could not write stress output", e);
        }
    }
}
