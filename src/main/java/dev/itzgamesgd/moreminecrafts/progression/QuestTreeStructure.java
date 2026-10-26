package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Presentation structure of the XP Quest tree: ordered chapters, each made of ordered sub-branches of quest ids,
 * plus explicit dependency edges. It carries no quest data. Edges only exist once real structure is supplied, so
 * the current slice declares none; nothing here implies a prerequisite between neighbouring quests.
 */
public record QuestTreeStructure(List<Chapter> chapters, List<Edge> edges) {
    /** A named run of quests inside a chapter. Each sub-branch starts on a new row when laid out for a large graph. */
    public record SubBranch(String id, String titleKey, List<String> nodeIds) {
        public SubBranch { nodeIds = List.copyOf(nodeIds); }
    }

    public record Chapter(String id, String titleKey, List<SubBranch> subBranches) {
        public Chapter { subBranches = List.copyOf(subBranches); }

        /** A chapter whose quests form a single sub-branch. */
        public static Chapter of(String id, String titleKey, List<String> nodeIds) {
            return new Chapter(id, titleKey, List.of(new SubBranch(id + ".all", titleKey, nodeIds)));
        }

        /** Every quest in presentation order: sub-branch by sub-branch. */
        public List<String> nodeIds() {
            List<String> ids = new ArrayList<>();
            for (SubBranch sub : subBranches) ids.addAll(sub.nodeIds());
            return List.copyOf(ids);
        }
    }

    /** {@code parentId} must be completed before {@code childId} in a future structure; drawn parent to child. */
    public record Edge(String parentId, String childId) {}

    /** The current early slice: a single Ecology chapter with one sub-branch and no dependencies. */
    public static QuestTreeStructure current() {
        return new QuestTreeStructure(List.of(Chapter.of("ecology", "moreminecrafts.ui.quest.branch.ecology",
                List.of("xpq.cmb.cave_dwellers", "xpq.cmb.bestiary_25", "xpq.cmb.habitats_5"))), List.of());
    }

    public List<String> allNodeIds() {
        List<String> ids = new ArrayList<>();
        for (Chapter chapter : chapters) ids.addAll(chapter.nodeIds());
        return List.copyOf(ids);
    }

    /** Structural problems: duplicate ids, empty sub-branches, edges to unknown nodes, self loops and cycles. */
    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        Set<String> known = new HashSet<>();
        for (Chapter chapter : chapters) {
            for (SubBranch sub : chapter.subBranches()) {
                if (sub.nodeIds().isEmpty()) problems.add("empty sub-branch " + sub.id());
                for (String id : sub.nodeIds()) if (!known.add(id)) problems.add("duplicate node " + id);
            }
        }
        for (Edge edge : edges) {
            if (!known.contains(edge.parentId()) || !known.contains(edge.childId()))
                problems.add("edge references an unknown node: " + edge);
            if (edge.parentId().equals(edge.childId())) problems.add("self-dependency " + edge);
        }
        if (hasCycle(known)) problems.add("dependency cycle");
        return problems;
    }

    private boolean hasCycle(Set<String> known) {
        Set<String> done = new HashSet<>(), visiting = new HashSet<>();
        for (String id : known) if (visit(id, done, visiting)) return true;
        return false;
    }

    private boolean visit(String id, Set<String> done, Set<String> visiting) {
        if (done.contains(id)) return false;
        if (!visiting.add(id)) return true;
        for (Edge edge : edges) if (edge.parentId().equals(id) && visit(edge.childId(), done, visiting)) return true;
        visiting.remove(id);
        done.add(id);
        return false;
    }
}
