package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/** Deterministic rules for Slime research (`v3.research.41`): split suppression and recursive descendants. */
public final class ResearchSlimeSplitRules {
    private ResearchSlimeSplitRules() {}

    public static boolean shouldPreventSplit(int researchLevel, int slimeSize,
                                             boolean killedByPlayer, boolean deadOrDying) {
        return researchLevel > 0 && slimeSize > 1 && killedByPlayer && deadOrDying;
    }

    /**
     * Generates the recursive descendant sizes produced when a slime of {@code parentSize} splits
     * down to size 1 without spawning entities. Each split creates 2..4 children of {@code size / 2}.
     */
    public static List<Integer> descendantSizes(int parentSize, IntUnaryOperator nextIntWithBound) {
        if (parentSize <= 1) return List.of();
        List<Integer> descendants = new ArrayList<>();
        collectDescendants(parentSize, nextIntWithBound, descendants);
        return List.copyOf(descendants);
    }

    private static void collectDescendants(int size, IntUnaryOperator nextIntWithBound,
                                           List<Integer> descendants) {
        int childSize = size / 2;
        if (childSize < 1) return;
        int children = 2 + Math.clamp(nextIntWithBound.applyAsInt(3), 0, 2);
        for (int i = 0; i < children; i++) {
            descendants.add(childSize);
            if (childSize > 1) collectDescendants(childSize, nextIntWithBound, descendants);
        }
    }

    public static int totalDescendantXp(List<Integer> descendantSizes) {
        int total = 0;
        for (int size : descendantSizes) {
            if (size > 0) total += size;
        }
        return total;
    }
}
