package dev.itzgamesgd.moreminecrafts.accessory;

/**
 * Candidate positions for the anvil's accessory catalyst slot, kept only so the owner can compare them in the
 * real client. {@link #CURRENT} is the shipped placement from the enhancement PR and remains the default;
 * nothing selects another layout outside the comparison tests. No layout is approved yet.
 */
public enum AnvilCatalystLayout {
    /** As shipped by the enhancement PR: right of the 176px panel. */
    CURRENT(180, 47),
    /** Same row as the inputs, between the stone slot and a narrowed Enhance button. */
    INLINE_RIGHT(102, 47),
    /** Same row as the inputs, left of the target slot. */
    LEFT_OF_INPUT(8, 47);

    public final int x;
    public final int y;
    private static volatile AnvilCatalystLayout active = CURRENT;

    AnvilCatalystLayout(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static AnvilCatalystLayout active() {
        return active;
    }

    /** Comparison hook for tests; affects menus created afterwards. */
    public static void useForComparison(AnvilCatalystLayout layout) {
        active = layout;
    }
}
