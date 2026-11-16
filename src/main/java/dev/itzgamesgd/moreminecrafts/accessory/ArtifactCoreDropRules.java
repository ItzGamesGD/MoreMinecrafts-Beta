package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.random.RandomGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Owner-approved exact rates within BIO §2.2 tier ranges. */
public final class ArtifactCoreDropRules {
    public enum Tier {
        GENERAL_STRONG(10, 1, 1),
        HIGH_ELITE(40, 1, 1),
        BOSS(100, 1, 2);

        private final int chancePercent;
        private final int minCount;
        private final int maxCount;

        Tier(int chancePercent, int minCount, int maxCount) {
            this.chancePercent = chancePercent;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }

        public int chancePercent() { return chancePercent; }
        public int minCount() { return minCount; }
        public int maxCount() { return maxCount; }
    }

    private ArtifactCoreDropRules() {}

    /** Returns the number of cores awarded, using the caller's existing deterministic random stream. */
    public static int rollCount(Tier tier, RandomGenerator random) {
        if (random.nextInt(100) >= tier.chancePercent()) return 0;
        if (tier.minCount() == tier.maxCount()) return tier.minCount();
        return tier.minCount() + random.nextInt(tier.maxCount() - tier.minCount() + 1);
    }

    /** BIO §2.2 tiers: boss entries take precedence, then explicitly named high elites, then documented ordinary examples or HP threshold. */
    public static Tier tier(LivingEntity entity) {
        if (!(entity instanceof Mob)) return null;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (matches(id, "minecraft", "elder_guardian", "warden")
                || matches(id, "moreminecrafts", "mother_spider", "tomb_guardian", "immortal_gladiator")) return Tier.BOSS;
        if (matches(id, "minecraft", "ravager", "ghast")) return Tier.HIGH_ELITE;
        if (matches(id, "minecraft", "enderman", "blaze", "wither_skeleton", "vindicator", "witch", "breeze")
                || matches(id, "moreminecrafts", "large_zombie", "armored_skeleton", "bandit_warrior",
                "lesser_wither_skeleton", "desert_spider", "forest_spider", "ice_spider")
                || (entity instanceof net.minecraft.world.entity.monster.illager.Pillager pillager && pillager.isPatrolLeader()))
            return Tier.GENERAL_STRONG;
        return entity.getMaxHealth() >= 25.0F ? Tier.GENERAL_STRONG : null;
    }

    private static boolean matches(Identifier id, String namespace, String... paths) {
        if (!id.getNamespace().equals(namespace)) return false;
        for (String path : paths) if (id.getPath().equals(path)) return true;
        return false;
    }
}
