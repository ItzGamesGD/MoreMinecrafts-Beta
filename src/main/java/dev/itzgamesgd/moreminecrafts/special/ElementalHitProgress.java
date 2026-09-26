package dev.itzgamesgd.moreminecrafts.special;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-runtime, player-owned counters. Only primary item hit callbacks may record here. */
public final class ElementalHitProgress {
    public enum Weapon { FIRE, WIND, LIGHTNING }

    public static final int FIRE_THRESHOLD = 10;
    public static final int WIND_THRESHOLD = 10;
    public static final int LIGHTNING_THRESHOLD = 5;

    private static final Map<UUID, EnumMap<Weapon, SuccessfulHitCounter>> PROGRESS = new HashMap<>();

    private ElementalHitProgress() {}

    public static boolean record(UUID playerId, Weapon weapon) {
        int threshold = threshold(weapon);
        EnumMap<Weapon, SuccessfulHitCounter> playerProgress =
                PROGRESS.computeIfAbsent(playerId, ignored -> new EnumMap<>(Weapon.class));
        SuccessfulHitCounter.Advance advance = playerProgress
                .getOrDefault(weapon, SuccessfulHitCounter.EMPTY)
                .recordSuccessfulHit(threshold);
        playerProgress.put(weapon, advance.counter());
        return advance.triggered();
    }

    static int threshold(Weapon weapon) {
        return switch (weapon) {
            case FIRE -> FIRE_THRESHOLD;
            case WIND -> WIND_THRESHOLD;
            case LIGHTNING -> LIGHTNING_THRESHOLD;
        };
    }
}
