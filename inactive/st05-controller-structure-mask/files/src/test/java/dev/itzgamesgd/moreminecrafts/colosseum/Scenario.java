package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.UUID;

/** One arena's controller state plus a fake world, with the common test moves. */
final class Scenario {
    final FakeColosseumWorld world = new FakeColosseumWorld();
    final ColosseumTrialState state = new ColosseumTrialState();

    /** Players enter, the first reaches the center, and R1 starts. Returns the first player. */
    UUID start(int players) {
        UUID first = world.join();
        for (int i = 1; i < players; i++) world.join();
        world.atActivation = true;
        world.tick(state, 1);
        world.atActivation = false;
        return first;
    }

    /** Clears rounds until {@code lastRound} has been cleared (waits out each intermission). */
    void clearThrough(int lastRound) {
        while (state.phase() == ColosseumTrialState.Phase.ROUND && state.round() <= lastRound) {
            world.clearRound(state, 6000);
            if (state.round() == lastRound) return;
            world.tick(state, state.intermissionRemaining());
        }
    }

    /** Plays R1-R6 and reaches the point where the boss has been started (a boss integration is available). */
    void toBoss() {
        world.bossAvailable = true;
        clearThrough(ColosseumRules.LAST_BUDGET_ROUND);
        world.tick(state, state.intermissionRemaining());
        world.tick(state, 2);
    }

    /** Spawns the mobs of the current round (warnings finish, mobs appear). */
    void spawnMobs() {
        world.tick(state, ColosseumRules.SPAWN_WARNING_TICKS + 5);
    }
}
