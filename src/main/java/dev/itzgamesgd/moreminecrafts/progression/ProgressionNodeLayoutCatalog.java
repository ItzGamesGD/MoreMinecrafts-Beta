package dev.itzgamesgd.moreminecrafts.progression;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable, presentation-only placement metadata. Gameplay eligibility remains catalog-owned. */
public final class ProgressionNodeLayoutCatalog {
    public static final String FALLBACK_ICON = "minecraft:book";
    public static final int RESEARCH_NODE_SIZE = 36;
    public static final int KNOWLEDGE_NODE_SIZE = 32;
    public static final int COLLISION_PADDING = 8;
    private static final String RESOURCE = "/assets/moreminecrafts/progression/node_layout.json";
    private static final Set<String> DISCIPLINES = Set.of(
            "resources", "production", "living", "combat", "ecology", "world", "alchemy");
    private static final ProgressionNodeLayoutCatalog INSTANCE = loadResource();

    public record Node(String id, String screen, String catalog, String discipline, String branch,
                       int branchOrder, int nodeOrder, int x, int y, String iconItemId,
                       String fallbackIconItemId, String branchTitleKey) {}
    private record Resource(int schemaVersion, String fallbackIconItemId, List<Node> nodes) {}

    private final String fallbackIconItemId;
    private final List<Node> nodes;
    private final Map<String, Node> byId;

    private ProgressionNodeLayoutCatalog(Resource resource) {
        this.fallbackIconItemId = resource.fallbackIconItemId();
        this.nodes = List.copyOf(resource.nodes());
        Map<String, Node> index = new HashMap<>();
        for (Node node : nodes) {
            if (index.putIfAbsent(node.id(), node) != null)
                throw new IllegalStateException("Duplicate progression layout node " + node.id());
        }
        this.byId = Map.copyOf(index);
        validate();
    }

    public static ProgressionNodeLayoutCatalog load() { return INSTANCE; }
    public String fallbackIconItemId() { return fallbackIconItemId; }
    public List<Node> nodes() { return nodes; }
    public Node byId(String id) { return byId.get(id); }

    /** Stable screen and manifest order; discipline remains an explicit filter. */
    public List<Node> nodes(String screen, String discipline) {
        return nodes.stream().filter(node -> node.screen().equals(screen)
                && node.discipline().equals(discipline)).toList();
    }

    public List<String> branches(String screen, String discipline) {
        return nodes(screen, discipline).stream().map(Node::branch).distinct().toList();
    }

    private static ProgressionNodeLayoutCatalog loadResource() {
        var stream = ProgressionNodeLayoutCatalog.class.getResourceAsStream(RESOURCE);
        if (stream == null) throw new ExceptionInInitializerError("Missing progression node layout " + RESOURCE);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Resource resource = new Gson().fromJson(reader, Resource.class);
            if (resource == null || resource.nodes() == null)
                throw new IllegalStateException("Invalid progression node layout resource");
            return new ProgressionNodeLayoutCatalog(resource);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private void validate() {
        if (fallbackIconItemId == null || !FALLBACK_ICON.equals(fallbackIconItemId))
            throw new IllegalStateException("Progression icon fallback must be " + FALLBACK_ICON);
        Set<Integer> knowledgeNumbers = KnowledgeCatalog.entries().stream()
                .map(KnowledgeCatalog.Entry::number).collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, ResearchDefinition> research = new HashMap<>();
        ResearchCatalog.allCatalogEntries().forEach(entry -> research.put(entry.id(), entry));
        Map<String, KnowledgeCatalog.Entry> knowledge = new HashMap<>();
        KnowledgeCatalog.entries().forEach(entry -> knowledge.put(entry.id(), entry));

        Set<String> researchIds = new HashSet<>(), knowledgeIds = new HashSet<>();
        Map<String, List<Node>> byCanvas = new HashMap<>();
        for (Node node : nodes) {
            if (node == null || node.id() == null || node.id().isBlank())
                throw new IllegalStateException("Progression layout contains an empty node ID");
            if (!DISCIPLINES.contains(node.discipline()))
                throw new IllegalStateException("Unknown discipline for layout node " + node.id());
            if (node.branch() == null || node.branch().isBlank() || node.branchOrder() < 0 || node.nodeOrder() < 0)
                throw new IllegalStateException("Invalid branch order for layout node " + node.id());
            String expectedTitleKey = "moreminecrafts.ui.branch." + node.discipline() + "." + node.branch();
            if (!expectedTitleKey.equals(node.branchTitleKey()))
                throw new IllegalStateException("Invalid branch title key for layout node " + node.id());
            if (node.iconItemId() == null || !node.iconItemId().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                throw new IllegalStateException("Invalid icon item ID for layout node " + node.id());
            if (!FALLBACK_ICON.equals(node.fallbackIconItemId()))
                throw new IllegalStateException("Invalid icon fallback for layout node " + node.id());
            if (node.x() != 64 + 192 * node.branchOrder() || node.y() != 64 + 72 * node.nodeOrder())
                throw new IllegalStateException("Unexpected deterministic coordinates for layout node " + node.id());

            if ("research".equals(node.screen())) {
                ResearchDefinition source = research.get(node.id());
                if (source == null || !node.catalog().equals(source.catalogVersion())
                        || !node.discipline().equals(source.discipline()))
                    throw new IllegalStateException("Research layout/source mismatch for " + node.id());
                if (!researchIds.add(node.id())) throw new IllegalStateException("Duplicate research layout node " + node.id());
                if ("v3".equals(node.catalog()) && source.prerequisitesDefined())
                    throw new IllegalStateException("V3 prerequisites unexpectedly defined for " + node.id());
                if (source.knowledgePrerequisites().stream().anyMatch(number -> !knowledgeNumbers.contains(number)))
                    throw new IllegalStateException("Unknown knowledge reference for layout node " + node.id());
            } else if ("knowledge".equals(node.screen())) {
                KnowledgeCatalog.Entry source = knowledge.get(node.id());
                if (source == null || !"knowledge".equals(node.catalog())
                        || !node.discipline().equals(source.discipline()))
                    throw new IllegalStateException("Knowledge layout/source mismatch for " + node.id());
                if (!knowledgeIds.add(node.id())) throw new IllegalStateException("Duplicate knowledge layout node " + node.id());
            } else {
                throw new IllegalStateException("Unknown screen kind for layout node " + node.id());
            }
            byCanvas.computeIfAbsent(node.screen() + ":" + node.discipline(), ignored -> new ArrayList<>()).add(node);
        }

        if (!researchIds.equals(research.keySet()))
            throw new IllegalStateException("Research layout IDs differ from authoritative source roster");
        if (!knowledgeIds.equals(knowledge.keySet()))
            throw new IllegalStateException("Knowledge layout IDs differ from authoritative source roster");
        for (var entry : byCanvas.entrySet()) validateCanvas(entry.getKey(), entry.getValue());
    }

    private static void validateCanvas(String canvas, List<Node> nodes) {
        List<Node> ordered = nodes.stream().sorted(Comparator.comparingInt(Node::branchOrder)
                .thenComparingInt(Node::nodeOrder).thenComparing(Node::id)).toList();
        Set<String> slots = new HashSet<>();
        for (Node node : ordered) {
            if (!slots.add(node.branchOrder() + ":" + node.nodeOrder()))
                throw new IllegalStateException("Duplicate coordinate slot on " + canvas + " at "
                        + node.branchOrder() + "," + node.nodeOrder());
        }
        for (int first = 0; first < ordered.size(); first++) {
            Node a = ordered.get(first);
            int half = ("research".equals(a.screen()) ? RESEARCH_NODE_SIZE : KNOWLEDGE_NODE_SIZE) / 2 + COLLISION_PADDING;
            for (int second = first + 1; second < ordered.size(); second++) {
                Node b = ordered.get(second);
                if (Math.abs(a.x() - b.x()) < half * 2 && Math.abs(a.y() - b.y()) < half * 2)
                    throw new IllegalStateException("Overlapping padded nodes on " + canvas + ": " + a.id() + " and " + b.id());
            }
        }
    }
}
