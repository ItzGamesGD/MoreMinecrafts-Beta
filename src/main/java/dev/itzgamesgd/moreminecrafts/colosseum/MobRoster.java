package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Active Q109 baseline roster (exactly 15). Scores are the owner-provisional threat values; conditional candidates stay inactive. */
public final class MobRoster {
    private MobRoster() {}

    /** Roster id -> threat score. {@code minecraft:zombie#baby} is the Baby Zombie variant of the Zombie EntityType. */
    public static final Map<String, Integer> SCORES = new LinkedHashMap<>();
    static {
        SCORES.put("minecraft:zombie", 3);
        SCORES.put("minecraft:zombie#baby", 2);
        SCORES.put("minecraft:husk", 3);
        SCORES.put("minecraft:skeleton", 4);
        SCORES.put("minecraft:bogged", 4);
        SCORES.put("minecraft:stray", 5);
        SCORES.put("minecraft:breeze", 7);
        SCORES.put("moreminecrafts:cave_zombie", 3);
        SCORES.put("moreminecrafts:cave_skeleton", 3);
        SCORES.put("moreminecrafts:bandit", 4);
        SCORES.put("moreminecrafts:bandit_archer", 4);
        SCORES.put("moreminecrafts:large_zombie", 6);
        SCORES.put("moreminecrafts:lesser_wither_skeleton", 6);
        SCORES.put("moreminecrafts:armored_skeleton", 7);
        SCORES.put("moreminecrafts:bandit_warrior", 7);
    }

    /** R3 role classification (owner-approved 2026-10-29). */
    public static final Set<String> MELEE = Set.of("minecraft:zombie", "minecraft:zombie#baby", "minecraft:husk", "moreminecrafts:cave_zombie",
            "moreminecrafts:cave_skeleton", "moreminecrafts:bandit", "moreminecrafts:large_zombie", "moreminecrafts:lesser_wither_skeleton",
            "moreminecrafts:bandit_warrior");
    public static final Set<String> RANGED = Set.of("minecraft:skeleton", "minecraft:bogged", "minecraft:stray", "moreminecrafts:bandit_archer",
            "moreminecrafts:armored_skeleton", "minecraft:breeze");
    public static final Set<String> SPECIAL = Set.of("minecraft:breeze", "minecraft:bogged", "minecraft:stray", "minecraft:husk",
            "moreminecrafts:lesser_wither_skeleton");
    public static final int ELITE_SCORE = 7;

    public static int score(String id) { return SCORES.get(id); }

    public static List<String> eligible(int round) {
        int cap = RoundPlan.scoreCap(round);
        return SCORES.entrySet().stream().filter(e -> e.getValue() <= cap).map(Map.Entry::getKey).toList();
    }
}
