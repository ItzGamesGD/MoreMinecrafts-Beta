package dev.itzgamesgd.moreminecrafts.accessory;

/**
 * Geometry of the five accessory slots, in vanilla survival-inventory slot coordinates (origin = top-left of the
 * 176x166 inventory texture). The column sits immediately left of the vanilla armor slots (x = 8, y = 8 + 18n).
 */
public final class AccessoryUiLayout {
    /** Item position of every accessory slot; the 18x18 cell spans {@code SLOT_X - 1 .. SLOT_X + 17}. */
    public static final int SLOT_X = -18;
    public static final int SLOT_FIRST_Y = 8;
    public static final int SLOT_PITCH = 18;
    public static final int CELL_SIZE = 18;
    /** Left edge of the slot cells relative to the inventory texture. */
    public static final int COLUMN_LEFT = SLOT_X - 1;
    /** Distance the open recipe book is moved left of its vanilla anchor to leave the column uncovered. */
    public static final int RECIPE_BOOK_SHIFT = 20;

    private AccessoryUiLayout() {}

    public static int slotY(int slot) {
        return SLOT_FIRST_Y + slot * SLOT_PITCH;
    }
}
