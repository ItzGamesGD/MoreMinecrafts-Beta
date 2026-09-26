package dev.itzgamesgd.moreminecrafts.special.elemental;

/** Pure transition policy for the server-owned Water Sword landing sequence. */
public record WaterDiveState(Phase phase, int ticksInPhase, boolean becameAirborne) {
    public enum Phase { LAUNCHED, DIVING, WAITING_FOR_GROUND, RESOLVE, CLEANUP }

    public static WaterDiveState launched() { return new WaterDiveState(Phase.LAUNCHED, 0, false); }

    public WaterDiveState tick(boolean onGround) {
        boolean airborne = becameAirborne || !onGround;
        return switch (phase) {
            case LAUNCHED -> ticksInPhase >= 5
                    ? new WaterDiveState(Phase.DIVING, 0, airborne)
                    : new WaterDiveState(phase, ticksInPhase + 1, airborne);
            case DIVING -> new WaterDiveState(Phase.WAITING_FOR_GROUND, 0, airborne);
            case WAITING_FOR_GROUND -> airborne && onGround
                    ? new WaterDiveState(Phase.RESOLVE, 0, true)
                    : new WaterDiveState(phase, ticksInPhase + 1, airborne);
            case RESOLVE -> new WaterDiveState(Phase.CLEANUP, 0, airborne);
            case CLEANUP -> this;
        };
    }
}
