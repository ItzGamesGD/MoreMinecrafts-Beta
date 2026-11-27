package dev.itzgamesgd.moreminecrafts.spidercolony;

/** Selection and harvest rules; actual natural provenance must be maintained by the caller. */
public final class EggSacPolicy {
    public static final int REACTIVE_PERCENT = 35;
    private EggSacPolicy() {}
    public static boolean naturalReactive(int roll) {
        if (roll < 0 || roll >= 100) throw new IllegalArgumentException("roll must be in [0,100)");
        return roll < REACTIVE_PERCENT;
    }
    public static boolean hatches(boolean natural, boolean reactive, boolean silkTouch) {
        return natural && reactive && !silkTouch;
    }
}
