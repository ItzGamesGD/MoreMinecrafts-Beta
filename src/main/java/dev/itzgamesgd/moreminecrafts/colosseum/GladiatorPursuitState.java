package dev.itzgamesgd.moreminecrafts.colosseum;

/** Persisted movement-sampling auxiliaries; MAX_VALUE denotes the initial no-sample baseline. */
record GladiatorPursuitState(int sample, double lastDistance) {
    static final int WINDOW_TICKS = 20;
    static final double INITIAL_DISTANCE = Double.MAX_VALUE;

    boolean valid() {
        return sample >= 0 && sample < WINDOW_TICKS && Double.isFinite(lastDistance)
                && lastDistance >= 0.0 && (lastDistance != Double.MAX_VALUE || sample == 0);
    }
}
