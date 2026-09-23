package dev.itzgamesgd.moreminecrafts.zombie;

/** Pure prototype balance and geometry for the Mining Giant's pickaxe cleave. */
public final class MiningGiantRules {
    public static final double NATURAL_SPAWN_CHANCE = 0.01D;
    public static final int MAX_SPAWN_Y = 32;
    public static final double CLEAVE_MIN_DISTANCE = 2.5D;
    public static final double CLEAVE_MAX_DISTANCE = 7.0D;
    public static final int TELEGRAPH_TICKS = 24;
    public static final int COOLDOWN_TICKS = 200;
    public static final double CLEAVE_HALF_WIDTH = 2.25D;
    public static final float CLEAVE_DAMAGE = 8.0F;

    private MiningGiantRules() {
    }

    public static boolean canSpawn(boolean natural, int blockY, double roll) {
        return natural && blockY <= MAX_SPAWN_Y && roll < NATURAL_SPAWN_CHANCE;
    }

    public static boolean canStartCleave(double distance, long gameTick, long readyTick,
                                         boolean targetAlive, boolean grounded) {
        return targetAlive && grounded && distance >= CLEAVE_MIN_DISTANCE
                && distance <= CLEAVE_MAX_DISTANCE && gameTick >= readyTick;
    }

    /** An interrupted warning cannot turn into an attack after its target or footing is lost. */
    public static boolean canContinueCleave(boolean targetValid, boolean grounded,
                                            boolean inFluid, boolean passenger) {
        return targetValid && grounded && !inFluid && !passenger;
    }

    public static long nextCleaveTick(long attackTick) {
        return attackTick + COOLDOWN_TICKS;
    }

    /** Tests a ground position against a fixed forward corridor, letting players sidestep the telegraph. */
    public static boolean isInsideCleave(double forwardX, double forwardZ, double offsetX, double offsetZ) {
        double length = Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (length < 1.0E-6D) {
            return false;
        }
        double nx = forwardX / length;
        double nz = forwardZ / length;
        double forward = offsetX * nx + offsetZ * nz;
        double sideways = Math.abs(offsetX * -nz + offsetZ * nx);
        return forward >= 0.5D && forward <= CLEAVE_MAX_DISTANCE && sideways <= CLEAVE_HALF_WIDTH;
    }
}
