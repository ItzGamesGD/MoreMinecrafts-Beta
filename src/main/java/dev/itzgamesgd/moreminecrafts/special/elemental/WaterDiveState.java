package dev.itzgamesgd.moreminecrafts.special.elemental;

import net.minecraft.world.phys.Vec3;

/** Pure transition policy for the server-owned Water Sword landing sequence. */
public record WaterDiveState(Phase phase, int ticksInPhase, boolean becameAirborne, Vec3 impactCenter) {
    public enum Phase { LAUNCHED, DIVING, WAITING_FOR_GROUND, RESOLVE, PILLAR, CLEANUP }

    public static WaterDiveState launched() { return new WaterDiveState(Phase.LAUNCHED, 0, false, null); }

    public WaterDiveState withImpactCenter(Vec3 center) {
        return new WaterDiveState(phase, ticksInPhase, becameAirborne, center);
    }

    public static boolean shouldExpire(boolean playerPresent, boolean playerAlive, boolean sameDimension,
                                      int totalTicks, int timeoutTicks) {
        return !playerPresent || !playerAlive || !sameDimension || totalTicks >= timeoutTicks;
    }

    public WaterDiveState tick(boolean onGround) {
        boolean airborne = becameAirborne || !onGround;
        return switch (phase) {
            case LAUNCHED -> ticksInPhase >= 5
                    ? new WaterDiveState(Phase.DIVING, 0, airborne, impactCenter)
                    : new WaterDiveState(phase, ticksInPhase + 1, airborne, impactCenter);
            case DIVING -> new WaterDiveState(Phase.WAITING_FOR_GROUND, 0, airborne, impactCenter);
            case WAITING_FOR_GROUND -> airborne && onGround
                    ? new WaterDiveState(Phase.RESOLVE, 0, true, impactCenter)
                    : new WaterDiveState(phase, ticksInPhase + 1, airborne, impactCenter);
            case RESOLVE -> new WaterDiveState(Phase.PILLAR, 0, airborne, impactCenter);
            case PILLAR -> new WaterDiveState(Phase.CLEANUP, 0, airborne, impactCenter);
            case CLEANUP -> this;
        };
    }
}
