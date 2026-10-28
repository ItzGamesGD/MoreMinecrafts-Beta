package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerPlayer;

/**
 * Raid victory credit. The hook is the vanilla award of the raid-win statistic inside {@code Raid.tick}, which
 * runs exactly once per hero when the raid status turns to VICTORY and only for heroes of the village (players
 * who delivered the final blow to a raider) that are present, loaded and not spectating. Failed, stopped and
 * cancelled raids never reach it.
 */
public final class QuestRaidVictory {
    static final String VICTORIES = "raid_victories";
    static final String OMEN_VICTORIES = "omen_raid_wins";
    static final int OMEN_LEVEL_REQUIRED = 3;

    private QuestRaidVictory() {}

    public static void heroRewarded(ServerPlayer hero, int raidOmenLevel) {
        XpQuestTracking.addCounter(hero, VICTORIES, 1);
        if (raidOmenLevel >= OMEN_LEVEL_REQUIRED) XpQuestTracking.addCounter(hero, OMEN_VICTORIES, 1);
        XpQuestTracking.reconcile(hero);
    }
}
