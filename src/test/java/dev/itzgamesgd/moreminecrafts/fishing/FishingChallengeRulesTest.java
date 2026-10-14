package dev.itzgamesgd.moreminecrafts.fishing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FishingChallengeRulesTest {
    private static final boolean CONNECTED = true;
    private static final boolean ALIVE = true;
    private static final boolean SAME_LEVEL = true;
    private static final boolean HOOK_PRESENT = true;
    private static final boolean HOOK_OWNED = true;
    private static final boolean PLAYER_HOOK = true;
    private static final boolean HOLDS_ROD = true;

    @Test
    void onlyConnectedValidSuccessfulChallengesAwardFish() {
        assertTrue(FishingChallengeRules.shouldAward(true, CONNECTED, validState()));
        assertFalse(FishingChallengeRules.shouldAward(false, CONNECTED, validState()), "minigame failure loses both fish rolls");
        assertFalse(FishingChallengeRules.shouldAward(true, false, validState()), "logout cancels without a reward");
        assertFalse(FishingChallengeRules.shouldAward(true, CONNECTED, false), "invalid hook or rod state cancels");
    }

    @Test
    void lifecycleInvalidationCoversDeathTravelHookRemovalAndRodRemoval() {
        assertTrue(validState());
        assertFalse(valid(false, ALIVE, SAME_LEVEL, HOOK_PRESENT, HOOK_OWNED, PLAYER_HOOK, HOLDS_ROD));
        assertFalse(valid(CONNECTED, false, SAME_LEVEL, HOOK_PRESENT, HOOK_OWNED, PLAYER_HOOK, HOLDS_ROD));
        assertFalse(valid(CONNECTED, ALIVE, false, HOOK_PRESENT, HOOK_OWNED, PLAYER_HOOK, HOLDS_ROD),
                "dimension travel invalidates the active challenge");
        assertFalse(valid(CONNECTED, ALIVE, SAME_LEVEL, false, HOOK_OWNED, PLAYER_HOOK, HOLDS_ROD),
                "removed hooks invalidate the active challenge");
        assertFalse(valid(CONNECTED, ALIVE, SAME_LEVEL, HOOK_PRESENT, HOOK_OWNED, PLAYER_HOOK, false),
                "moving the rod out of hand invalidates the active challenge");
    }

    @Test
    void multiCatchAddsOneIndependentResultOnlyAfterUnlock() {
        assertEquals(1, FishingChallengeRules.catchCount(false));
        assertEquals(2, FishingChallengeRules.catchCount(true));
    }

    private static boolean validState() {
        return valid(CONNECTED, ALIVE, SAME_LEVEL, HOOK_PRESENT, HOOK_OWNED, PLAYER_HOOK, HOLDS_ROD);
    }

    private static boolean valid(boolean connected, boolean alive, boolean sameLevel, boolean hookPresent,
                                 boolean hookOwned, boolean playerHook, boolean holdsRod) {
        return FishingChallengeRules.valid(connected, alive, sameLevel, hookPresent,
                hookOwned, playerHook, holdsRod);
    }
}
