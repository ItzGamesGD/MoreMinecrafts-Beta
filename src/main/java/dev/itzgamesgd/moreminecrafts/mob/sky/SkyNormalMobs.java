package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.mixin.SpawnPlacementsInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

import java.util.List;

/**
 * Registry for the five Sky Realm normal mobs. Natural spawning is data-driven through the sky-island structures'
 * {@code spawn_overrides} (SKY-MOB §15.6 P1/P2) and gated here by spawn placements: Q29 light rule, Q79/Q80 vanilla
 * surface rule, and for {@link EntitySpawnReason#NATURAL} the R1 sky-island region (P3). No biome modifier is added (N1).
 */
public final class SkyNormalMobs {
    /** Data-only marker tag for the five types; it is not read by any vanilla AI or targeting code. */
    public static final TagKey<EntityType<?>> TAG =
            TagKey.create(Registries.ENTITY_TYPE, ModIds.id("sky_normal_mobs"));

    private static boolean initialized;

    private SkyNormalMobs() {}

    public static final EntityType<CloudWraith> CLOUD_WRAITH = register("cloud_wraith", CloudWraith::new,
            0.6F, 1.95F, 8, 1.0F);
    public static final EntityType<SkyguardArcher> SKYGUARD_ARCHER = register("skyguard_archer", SkyguardArcher::new,
            0.6F, 1.99F, 8, 1.0F);
    /** Base box is the vanilla Silverfish box; the SCALE attribute (2.5) enlarges collision and rendering. */
    public static final EntityType<CrystalBug> CRYSTAL_BUG = register("crystal_bug", CrystalBug::new,
            0.4F, 0.3F, 8, (float) SkyMobTraits.CRYSTAL_BUG_SCALE);
    public static final EntityType<CloudSlime> CLOUD_SLIME = register("cloud_slime", CloudSlime::new,
            0.52F, 0.52F, 10, 4.0F);
    public static final EntityType<FallenSentinel> FALLEN_SENTINEL = register("fallen_sentinel", FallenSentinel::new,
            0.6F, 1.95F, 8, 1.0F);

    private static <T extends Mob> EntityType<T> register(String path, EntityType.EntityFactory<T> factory,
                                                           float width, float height, int trackingRange,
                                                           float spawnDimensionsScale) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(path));
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height)
                .clientTrackingRange(trackingRange)
                .fireImmune()      // Q36 sun / Q49 fire+lava / Q65 no burning state
                .notInPeaceful();  // Q74 peaceful removal
        if (spawnDimensionsScale != 1.0F) builder.spawnDimensionsScale(spawnDimensionsScale);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static List<EntityType<? extends Mob>> allTypes() {
        return List.of(CLOUD_WRAITH, SKYGUARD_ARCHER, CRYSTAL_BUG, CLOUD_SLIME, FALLEN_SENTINEL);
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        FabricDefaultAttributeRegistry.register(CLOUD_WRAITH, Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.ATTACK_DAMAGE, 5.0D));
        FabricDefaultAttributeRegistry.register(SKYGUARD_ARCHER, AbstractSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D));
        FabricDefaultAttributeRegistry.register(CRYSTAL_BUG, Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D).add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.SCALE, SkyMobTraits.CRYSTAL_BUG_SCALE));
        FabricDefaultAttributeRegistry.register(CLOUD_SLIME, Monster.createMonsterAttributes());
        FabricDefaultAttributeRegistry.register(FALLEN_SENTINEL, Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D).add(Attributes.ATTACK_DAMAGE, 6.0D));
        // Q29: wraith / archer / sentinel only in the dark; crystal bug / cloud slime at any light (D5-a: no slime
        // chunk or altitude rule). Trial-spawner placement keeps the vanilla light exemption (Q29 does not apply).
        placement(CLOUD_WRAITH, SkyNormalMobs::darkRule);
        placement(SKYGUARD_ARCHER, SkyNormalMobs::darkRule);
        placement(FALLEN_SENTINEL, SkyNormalMobs::darkRule);
        placement(CRYSTAL_BUG, SkyNormalMobs::anyLightRule);
        placement(CLOUD_SLIME, SkyNormalMobs::anyLightRule);
        MoreMinecrafts.LOGGER.info("Registered {} Sky Realm normal mobs with sky-island spawn placements.",
                allTypes().size());
    }

    private static <T extends Mob> void placement(EntityType<T> type, SpawnPlacements.SpawnPredicate<T> rule) {
        SpawnPlacementsInvoker.moreminecrafts$register(type, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rule);
    }

    static boolean darkRule(EntityType<? extends Mob> type, ServerLevelAccessor level, EntitySpawnReason reason,
                            BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && regionGate(level, reason, pos);
    }

    static boolean anyLightRule(EntityType<? extends Mob> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && regionGate(level, reason, pos);
    }

    /** N1/P3/Q33: natural spawns only inside the sky-island region and never inside a Sky Tower. */
    static boolean regionGate(ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos) {
        return reason != EntitySpawnReason.NATURAL || SkyIslandRegion.isIsland(level.getLevel(), pos);
    }
}
