package dev.itzgamesgd.moreminecrafts.mob;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Small, one-time vanilla-attribute adjustments applied during ordinary spawn finalization. */
public final class CustomMobStats {
    private static final Set<Mob> APPLIED = Collections.newSetFromMap(new WeakHashMap<>());

    private CustomMobStats() {}

    public static void apply(Mob mob) {
        String id = customId(mob.getType());
        if (id == null || !APPLIED.add(mob)) return;
        double health = 1.0, speed = 1.0, damage = 1.0, scale = 1.0, armor = 1.0;
        switch (id) {
            case "swamp_zombie" -> { health = 1.20; speed = 0.82; }
            case "cave_zombie" -> { health = 0.90; speed = 1.10; scale = 0.88; }
            case "large_zombie" -> { health = 1.55; speed = 0.78; damage = 1.20; scale = 1.30; }
            case "frozen_zombie" -> { health = 1.08; speed = 0.94; }
            case "mossy_zombie" -> { health = 1.10; speed = 0.92; }
            case "fungal_zombie" -> { health = 0.95; speed = 1.02; }
            case "miner_zombie" -> { health = 1.12; speed = 0.92; }
            case "rotten_skeleton" -> { health = 0.72; damage = 0.82; }
            case "cave_skeleton" -> { health = 0.95; speed = 1.04; }
            case "armored_skeleton" -> { health = 1.30; speed = 0.82; armor = 1.5; }
            case "ashen_skeleton" -> { health = 1.05; damage = 1.05; }
            case "lesser_wither_skeleton" -> { health = 0.72; damage = 0.78; scale = 0.86; }
            case "mountain_skeleton" -> { health = 1.05; speed = 0.95; }
            case "miner_skeleton" -> { health = 0.95; speed = 1.02; }
            case "small_spider" -> { health = 0.60; speed = 1.20; scale = 0.72; }
            case "large_spider" -> { health = 1.65; speed = 0.82; damage = 1.12; scale = 1.25; }
            case "forest_spider" -> { speed = 0.94; }
            case "desert_spider" -> { damage = 1.12; }
            case "swamp_spider" -> { health = 0.88; damage = 0.80; }
            case "ice_spider" -> { health = 1.08; speed = 0.92; }
            case "cave_bug" -> { health = 1.15; }
            case "desert_bug" -> { health = 1.12; damage = 1.05; }
            case "wild_slime" -> { health = 0.82; scale = 0.82; }
            case "mud_slime" -> { health = 1.15; speed = 0.78; scale = 0.90; }
            case "sand_slime" -> { speed = 1.08; }
            case "lesser_magma_cube" -> { health = 0.78; damage = 0.80; scale = 0.82; }
            case "drowned_sailor" -> { health = 1.12; }
            case "deep_sea_drowned" -> { health = 1.35; speed = 0.90; }
            case "river_drowned" -> { health = 0.82; damage = 0.88; }
            case "wild_piglin" -> { health = 0.82; damage = 0.82; armor = 0.75; }
            case "bandit" -> { health = 0.90; }
            case "bandit_archer" -> { health = 0.82; damage = 0.92; }
            case "bandit_warrior" -> { health = 0.85; damage = 0.85; }
            case "wandering_raider" -> { health = 0.78; }
            case "feral_wolf" -> { health = 1.15; damage = 1.15; }
            case "cave_bat" -> { health = 0.60; damage = 0.55; speed = 1.08; scale = 1.55; }
            case "giant_bee" -> { health = 1.65; damage = 1.25; scale = 1.45; }
            case "lesser_phantom" -> { health = 0.60; damage = 0.72; scale = 0.72; }
            case "cave_creeper" -> { health = 0.92; scale = 0.90; }
            default -> { return; }
        }
        multiply(mob, Attributes.MAX_HEALTH, health);
        multiply(mob, Attributes.MOVEMENT_SPEED, speed);
        multiply(mob, Attributes.ATTACK_DAMAGE, damage);
        multiply(mob, Attributes.ARMOR, armor);
        multiply(mob, Attributes.SCALE, scale);
        mob.setHealth((float) mob.getAttributeValue(Attributes.MAX_HEALTH));
    }

    private static String customId(EntityType<?> type) {
        if (type == CustomMobCatalog.SWAMP_ZOMBIE) return "swamp_zombie";
        if (type == CustomMobCatalog.CAVE_ZOMBIE) return "cave_zombie";
        if (type == CustomMobCatalog.LARGE_ZOMBIE) return "large_zombie";
        if (type == CustomMobCatalog.FROZEN_ZOMBIE) return "frozen_zombie";
        if (type == CustomMobCatalog.MOSSY_ZOMBIE) return "mossy_zombie";
        if (type == CustomMobCatalog.FUNGAL_ZOMBIE) return "fungal_zombie";
        if (type == CustomMobCatalog.MINER_ZOMBIE) return "miner_zombie";
        if (type == CustomMobCatalog.ROTTEN_SKELETON) return "rotten_skeleton";
        if (type == CustomMobCatalog.CAVE_SKELETON) return "cave_skeleton";
        if (type == CustomMobCatalog.ARMORED_SKELETON) return "armored_skeleton";
        if (type == CustomMobCatalog.ASHEN_SKELETON) return "ashen_skeleton";
        if (type == CustomMobCatalog.LESSER_WITHER_SKELETON) return "lesser_wither_skeleton";
        if (type == CustomMobCatalog.MOUNTAIN_SKELETON) return "mountain_skeleton";
        if (type == CustomMobCatalog.MINER_SKELETON) return "miner_skeleton";
        if (type == CustomMobCatalog.SMALL_SPIDER) return "small_spider";
        if (type == CustomMobCatalog.LARGE_SPIDER) return "large_spider";
        if (type == CustomMobCatalog.FOREST_SPIDER) return "forest_spider";
        if (type == CustomMobCatalog.DESERT_SPIDER) return "desert_spider";
        if (type == CustomMobCatalog.SWAMP_SPIDER) return "swamp_spider";
        if (type == CustomMobCatalog.ICE_SPIDER) return "ice_spider";
        if (type == CustomMobCatalog.CAVE_BUG) return "cave_bug";
        if (type == CustomMobCatalog.DESERT_BUG) return "desert_bug";
        if (type == CustomMobCatalog.END_BUG) return "end_bug";
        if (type == CustomMobCatalog.WILD_SLIME) return "wild_slime";
        if (type == CustomMobCatalog.MUD_SLIME) return "mud_slime";
        if (type == CustomMobCatalog.SAND_SLIME) return "sand_slime";
        if (type == CustomMobCatalog.LESSER_MAGMA_CUBE) return "lesser_magma_cube";
        if (type == CustomMobCatalog.DROWNED_SAILOR) return "drowned_sailor";
        if (type == CustomMobCatalog.DEEP_SEA_DROWNED) return "deep_sea_drowned";
        if (type == CustomMobCatalog.RIVER_DROWNED) return "river_drowned";
        if (type == CustomMobCatalog.WILD_PIGLIN) return "wild_piglin";
        if (type == CustomMobCatalog.BANDIT) return "bandit";
        if (type == CustomMobCatalog.BANDIT_ARCHER) return "bandit_archer";
        if (type == CustomMobCatalog.BANDIT_WARRIOR) return "bandit_warrior";
        if (type == CustomMobCatalog.WANDERING_RAIDER) return "wandering_raider";
        if (type == CustomMobCatalog.FERAL_WOLF) return "feral_wolf";
        if (type == CustomMobCatalog.CAVE_BAT) return "cave_bat";
        if (type == CustomMobCatalog.GIANT_BEE) return "giant_bee";
        if (type == CustomMobCatalog.LESSER_PHANTOM) return "lesser_phantom";
        if (type == CustomMobCatalog.CAVE_CREEPER) return "cave_creeper";
        return null;
    }

    private static void multiply(Mob mob, net.minecraft.core.Holder<Attribute> attribute, double multiplier) {
        if (multiplier == 1.0) return;
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(instance.getBaseValue() * multiplier);
    }
}
