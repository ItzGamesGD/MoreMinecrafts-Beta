package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mixin.SpawnPlacementsInvoker;
import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Group B — the five sky-island vanilla hostile variants (SKY_ISLANDS_T2_CONTEXT §5 / §20).
 *
 * <p>Built the same way as the desert-pyramid catalog variants: vanilla entity classes and AI, a texture swap, vanilla
 * loot aliasing and {@link dev.itzgamesgd.moreminecrafts.mob.CustomMobStats} multipliers. Identifiers are deliberately
 * separate from the group C Sky Realm normal mobs ({@link SkyNormalMobs}, e.g. {@code skyguard_archer} vs
 * {@code sky_archer}) and from the approved forty-entry {@link CustomMobCatalog} roster.
 *
 * <p>Natural spawning is never added to biome lists. The five types appear only through the sky-island structures'
 * isolated {@code spawn_overrides} list (§15.6 P2), and their placement predicate adds the
 * {@link CustomMobCatalog#skyIslandSurface sky-island surface} gate and the {@link SkyIslandRegion} gate (P3, Q33).
 * The list weights (normal island 3/2/2/1/3, mountain-family island 10/8/5/3/8, PROVISIONAL group size 1-2) live in
 * the {@code sky_island_*} structure JSON and are pinned by {@code SkyMobSpawnResourceTest}.
 */
public final class SkyVariantMobs {
    /** Data-only marker tag for the five group B types; nothing in vanilla reads it. */
    public static final TagKey<EntityType<?>> TAG =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("sky_variant_mobs"));

    // PROVISIONAL (owner task 2026-11-14): non-health/speed/damage gameplay knobs. Stat multipliers live in CustomMobStats.
    /** PROVISIONAL: Rock Creeper explosion radius multiplier (narrow floating islands). */
    public static final float ROCK_CREEPER_EXPLOSION_MULTIPLIER = 0.85F;
    /** PROVISIONAL: Azure Slime jump strength multiplier over the vanilla 0.42 base. */
    public static final double AZURE_SLIME_JUMP_MULTIPLIER = 1.25D;

    /** Vanilla {@code LivingEntity.createLivingAttributes()} jump strength. */
    public static final double VANILLA_JUMP_STRENGTH = 0.42D;

    private static boolean initialized;

    private SkyVariantMobs() {}

    public static final EntityType<WeatheredZombie> WEATHERED_ZOMBIE = register("weathered_zombie",
            WeatheredZombie::new, 0.6F, 1.95F, 8, 1.0F);
    public static final EntityType<SkyArcher> SKY_ARCHER = register("sky_archer",
            SkyArcher::new, 0.6F, 1.99F, 8, 1.0F);
    public static final EntityType<RockCreeper> ROCK_CREEPER = register("rock_creeper",
            RockCreeper::new, 0.6F, 1.7F, 8, 1.0F);
    public static final EntityType<PaleEnderman> PALE_ENDERMAN = register("pale_enderman",
            PaleEnderman::new, 0.6F, 2.9F, 8, 1.0F);
    public static final EntityType<AzureSlime> AZURE_SLIME = register("azure_slime",
            AzureSlime::new, 0.52F, 0.52F, 10, 4.0F);

    private static <T extends Mob> EntityType<T> register(String path, EntityType.EntityFactory<T> factory,
                                                           float width, float height, int trackingRange,
                                                           float spawnDimensionsScale) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(path));
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(trackingRange)
                .notInPeaceful();
        // Vanilla counterpart eye heights (Zombie / Skeleton 1.74, Enderman 2.55, Slime 0.325).
        switch (path) {
            case "weathered_zombie", "sky_archer" -> builder.eyeHeight(1.74F);
            case "pale_enderman" -> builder.eyeHeight(2.55F);
            case "azure_slime" -> builder.eyeHeight(0.325F);
            default -> { }
        }
        if (spawnDimensionsScale != 1.0F) builder.spawnDimensionsScale(spawnDimensionsScale);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static List<EntityType<? extends Mob>> allTypes() {
        return List.of(WEATHERED_ZOMBIE, SKY_ARCHER, ROCK_CREEPER, PALE_ENDERMAN, AZURE_SLIME);
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        // Vanilla default attributes; health/speed/damage multipliers are applied once at finalizeSpawn by
        // CustomMobStats (the catalog variant path). Only the slime jump has no finalizeSpawn hook that survives
        // vanilla size rewrites, so it is a default attribute.
        FabricDefaultAttributeRegistry.register(WEATHERED_ZOMBIE, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(SKY_ARCHER, Skeleton.createAttributes());
        FabricDefaultAttributeRegistry.register(ROCK_CREEPER, Creeper.createAttributes());
        FabricDefaultAttributeRegistry.register(PALE_ENDERMAN, Enderman.createAttributes());
        FabricDefaultAttributeRegistry.register(AZURE_SLIME, Monster.createMonsterAttributes()
                .add(Attributes.JUMP_STRENGTH, VANILLA_JUMP_STRENGTH * AZURE_SLIME_JUMP_MULTIPLIER));
        for (EntityType<? extends Mob> type : allTypes()) placement(type);
        MoreMinecrafts.LOGGER.info("Registered {} sky-island vanilla variants (group B) with sky-island-only spawning.",
                allTypes().size());
    }

    private static <T extends Mob> void placement(EntityType<T> type) {
        SpawnPlacementsInvoker.moreminecrafts$register(type, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SkyVariantMobs::spawnRule);
    }

    /**
     * Vanilla monster rule (darkness, difficulty) for every reason; for natural spawns additionally the
     * {@code SKY_ISLAND} surface kind and the saved-piece sky-island region, which also excludes Sky Tower interiors.
     */
    public static boolean spawnRule(EntityType<? extends Mob> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                    BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != EntitySpawnReason.NATURAL) return true;
        return CustomMobCatalog.skyIslandSurface(level, pos) && SkyIslandRegion.isIsland(level.getLevel(), pos);
    }
}
