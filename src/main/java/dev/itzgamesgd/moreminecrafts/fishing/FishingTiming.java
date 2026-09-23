package dev.itzgamesgd.moreminecrafts.fishing;

public final class FishingTiming {
    public static final int HALF_CYCLE_TICKS=20;
    public static final int TIMEOUT_TICKS=160;
    public static final double SUCCESS_MIN=0.42;
    public static final double SUCCESS_MAX=0.58;
    private FishingTiming() {}
    public static double position(long elapsedTicks) {
        long phase=Math.floorMod(elapsedTicks, HALF_CYCLE_TICKS*2L);
        return phase<=HALF_CYCLE_TICKS ? phase/(double)HALF_CYCLE_TICKS : (HALF_CYCLE_TICKS*2L-phase)/(double)HALF_CYCLE_TICKS;
    }
    public static boolean inSuccessWindow(double position){return position>=SUCCESS_MIN&&position<=SUCCESS_MAX;}
    public static boolean successful(long elapsedTicks){return inSuccessWindow(position(elapsedTicks));}
    public static boolean timedOut(long elapsedTicks){return elapsedTicks>=TIMEOUT_TICKS;}
}
