package dev.itzgamesgd.moreminecrafts.colosseum;

import net.minecraft.server.level.ServerLevel;

/**
 * Round 7 connection interface, fulfilled by GladiatorBossProvider and ImmortalGladiator. The controller requests one attempt-bound boss,
 * locks its HP from the round-7 participant count and completes only through the verified normal-death callback.
 */
public final class ColosseumBossHook {
    private ColosseumBossHook() {}

    public interface Provider {
        /** Spawns the boss at the arena centre; returns false if it could not be started (it will be requested again next tick). */
        boolean start(ServerLevel level, ColosseumData.Site site, ArenaAttempt attempt, int maxHp);
        /** Time-out: the boss vanishes without a kill, key or loot (Q117). */
        void vanish(ServerLevel level, ColosseumData.Site site, ArenaAttempt attempt);
    }

    private static volatile Provider provider;

    public static void register(Provider p) { provider = p; }
    public static Provider provider() { return provider; }
}
