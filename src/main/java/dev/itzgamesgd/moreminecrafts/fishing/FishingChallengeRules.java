package dev.itzgamesgd.moreminecrafts.fishing;

/** Deterministic lifecycle and award rules shared by the server challenge and regression tests. */
final class FishingChallengeRules {
    private FishingChallengeRules() {}

    static boolean valid(boolean connected, boolean alive, boolean sameLevel, boolean hookPresent,
                         boolean hookOwned, boolean isPlayerHook, boolean holdsRod) {
        return connected && alive && sameLevel && hookPresent && hookOwned && isPlayerHook && holdsRod;
    }

    static boolean shouldAward(boolean minigameSuccess, boolean connected, boolean valid) {
        return minigameSuccess && connected && valid;
    }

    static int catchCount(boolean multiCatchUnlocked) {
        return multiCatchUnlocked ? 2 : 1;
    }
}
