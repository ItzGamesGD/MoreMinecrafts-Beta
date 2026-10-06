package dev.itzgamesgd.moreminecrafts.mob;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mixin.SpawnPlacementsInvoker;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Registry and natural-spawn catalog for the approved ordinary vanilla-model roster. */
public final class CustomMobCatalog {
    private static final List<SpawnSpec> SPAWNS = new ArrayList<>();
    private static boolean initialized;
    private static final TagKey<Biome> IS_SWAMP = TagKey.create(Registries.BIOME, Identifier.parse("minecraft:is_swamp"));

    private CustomMobCatalog() {}

    public static final EntityType<Zombie> SWAMP_ZOMBIE = type("swamp_zombie", Zombie::new, 0.6f, 1.95f);
    public static final EntityType<Zombie> CAVE_ZOMBIE = type("cave_zombie", Zombie::new, 0.5f, 1.65f);
    public static final EntityType<Zombie> LARGE_ZOMBIE = type("large_zombie", Zombie::new, 0.78f, 2.54f);
    public static final EntityType<Zombie> FROZEN_ZOMBIE = type("frozen_zombie", Zombie::new, 0.6f, 1.95f);
    public static final EntityType<Zombie> MOSSY_ZOMBIE = type("mossy_zombie", Zombie::new, 0.6f, 1.95f);
    public static final EntityType<Zombie> FUNGAL_ZOMBIE = type("fungal_zombie", Zombie::new, 0.6f, 1.95f);
    public static final EntityType<Zombie> MINER_ZOMBIE = type("miner_zombie", (type, level) -> new MinerZombie(type, level), 0.6f, 1.95f);
    public static final EntityType<MeleeSkeleton> ROTTEN_SKELETON = type("rotten_skeleton", (type, level) -> new MeleeSkeleton(type, level, Items.STONE_SWORD), 0.6f, 1.99f);
    public static final EntityType<MeleeSkeleton> CAVE_SKELETON = type("cave_skeleton", (type, level) -> new MeleeSkeleton(type, level, Items.STONE_SWORD), 0.6f, 1.99f);
    public static final EntityType<ArmoredSkeleton> ARMORED_SKELETON = type("armored_skeleton", ArmoredSkeleton::new, 0.6f, 1.99f);
    public static final EntityType<Skeleton> ASHEN_SKELETON = type("ashen_skeleton", Skeleton::new, 0.6f, 1.99f);
    public static final EntityType<WitherSkeleton> LESSER_WITHER_SKELETON = type("lesser_wither_skeleton", WitherSkeleton::new, 0.55f, 1.75f, true);
    public static final EntityType<Stray> MOUNTAIN_SKELETON = type("mountain_skeleton", Stray::new, 0.6f, 1.99f);
    public static final EntityType<MeleeSkeleton> MINER_SKELETON = type("miner_skeleton", (type, level) -> new MeleeSkeleton(type, level, Items.IRON_PICKAXE), 0.6f, 1.99f);
    public static final EntityType<Spider> SMALL_SPIDER = type("small_spider", Spider::new, 0.55f, 0.45f);
    public static final EntityType<Spider> LARGE_SPIDER = type("large_spider", Spider::new, 1.2f, 1.0f);
    public static final EntityType<Spider> FOREST_SPIDER = type("forest_spider", Spider::new, 0.8f, 0.6f);
    public static final EntityType<Spider> DESERT_SPIDER = type("desert_spider", Spider::new, 0.8f, 0.6f);
    public static final EntityType<CaveSpider> SWAMP_SPIDER = type("swamp_spider", CaveSpider::new, 0.7f, 0.5f);
    public static final EntityType<Spider> ICE_SPIDER = type("ice_spider", Spider::new, 0.8f, 0.6f);
    public static final EntityType<Silverfish> CAVE_BUG = type("cave_bug", Silverfish::new, 0.4f, 0.3f);
    public static final EntityType<Silverfish> DESERT_BUG = type("desert_bug", Silverfish::new, 0.4f, 0.3f);
    public static final EntityType<Endermite> END_BUG = type("end_bug", Endermite::new, 0.4f, 0.3f);
    public static final EntityType<Slime> WILD_SLIME = type("wild_slime", Slime::new, 0.52f, 0.52f);
    public static final EntityType<Slime> MUD_SLIME = type("mud_slime", Slime::new, 0.6f, 0.6f);
    public static final EntityType<Slime> SAND_SLIME = type("sand_slime", Slime::new, 0.6f, 0.6f);
    public static final EntityType<MagmaCube> LESSER_MAGMA_CUBE = type("lesser_magma_cube", MagmaCube::new, 0.65f, 0.65f, true);
    public static final EntityType<Drowned> DROWNED_SAILOR = type("drowned_sailor", Drowned::new, 0.6f, 1.95f);
    public static final EntityType<Drowned> DEEP_SEA_DROWNED = type("deep_sea_drowned", Drowned::new, 0.6f, 1.95f);
    public static final EntityType<Drowned> RIVER_DROWNED = type("river_drowned", Drowned::new, 0.6f, 1.95f);
    public static final EntityType<Piglin> WILD_PIGLIN = type("wild_piglin", Piglin::new, 0.6f, 1.95f, true);
    public static final EntityType<Pillager> BANDIT = type("bandit", (type, level) -> new BanditEntity(type, level), 0.6f, 1.95f);
    public static final EntityType<Pillager> BANDIT_ARCHER = type("bandit_archer", (type, level) -> new BanditArcher(type, level), 0.6f, 1.95f);
    public static final EntityType<Vindicator> BANDIT_WARRIOR = type("bandit_warrior", Vindicator::new, 0.6f, 1.95f);
    public static final EntityType<Pillager> WANDERING_RAIDER = type("wandering_raider", Pillager::new, 0.6f, 1.95f);
    public static final EntityType<Wolf> FERAL_WOLF = type("feral_wolf", (type, level) -> new FeralWolf(type, level), 0.6f, 0.85f, MobCategory.CREATURE, false);
    public static final EntityType<Bat> CAVE_BAT = type("cave_bat", (type, level) -> new CaveBatEntity(type, level), 0.9f, 0.9f, MobCategory.AMBIENT, false);
    public static final EntityType<Bee> GIANT_BEE = type("giant_bee", Bee::new, 1.0f, 0.8f, MobCategory.CREATURE, false);
    public static final EntityType<Phantom> LESSER_PHANTOM = type("lesser_phantom", Phantom::new, 0.7f, 0.45f);
    public static final EntityType<Creeper> CAVE_CREEPER = type("cave_creeper", Creeper::new, 0.6f, 1.7f);

    private static <T extends Mob> EntityType<T> type(String path, EntityType.EntityFactory<T> factory,
                                                       float width, float height) {
        return type(path, factory, width, height, MobCategory.MONSTER, false);
    }

    private static <T extends Mob> EntityType<T> type(String path, EntityType.EntityFactory<T> factory,
                                                       float width, float height, boolean fireImmune) {
        return type(path, factory, width, height, MobCategory.MONSTER, fireImmune);
    }

    private static <T extends Mob> EntityType<T> type(String path, EntityType.EntityFactory<T> factory,
                                                       float width, float height, MobCategory category,
                                                       boolean fireImmune) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(path));
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, category)
                .sized(width, height).clientTrackingRange(8);
        if (fireImmune) builder.fireImmune();
        EntityType<T> result = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
        return result;
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        add(SWAMP_ZOMBIE, Zombie.createAttributes(), IS_SWAMP, 4, SpawnKind.GROUND);
        add(CAVE_ZOMBIE, Zombie.createAttributes(), BiomeTags.IS_OVERWORLD, 3, SpawnKind.CAVE);
        add(LARGE_ZOMBIE, Zombie.createAttributes(), BiomeTags.IS_OVERWORLD, 1, SpawnKind.GROUND);
        add(FROZEN_ZOMBIE, Zombie.createAttributes(), BiomeTags.IS_MOUNTAIN, 3, SpawnKind.GROUND);
        add(MOSSY_ZOMBIE, Zombie.createAttributes(), BiomeTags.IS_FOREST, 3, SpawnKind.GROUND);
        add(FUNGAL_ZOMBIE, Zombie.createAttributes(), BiomeTags.IS_JUNGLE, 2, SpawnKind.CAVE);
        add(MINER_ZOMBIE, Zombie.createAttributes(), BiomeTags.HAS_MINESHAFT, 2, SpawnKind.CAVE);
        add(ROTTEN_SKELETON, Skeleton.createAttributes(), BiomeTags.IS_OVERWORLD, 3, SpawnKind.GROUND);
        add(CAVE_SKELETON, Skeleton.createAttributes(), BiomeTags.IS_OVERWORLD, 3, SpawnKind.CAVE);
        add(ARMORED_SKELETON, Skeleton.createAttributes(), BiomeTags.IS_MOUNTAIN, 2, SpawnKind.GROUND);
        add(ASHEN_SKELETON, Skeleton.createAttributes(), BiomeTags.IS_NETHER, 3, SpawnKind.GROUND);
        add(LESSER_WITHER_SKELETON, WitherSkeleton.createAttributes(), BiomeTags.HAS_NETHER_FORTRESS, 2, SpawnKind.NETHER);
        add(MOUNTAIN_SKELETON, Stray.createAttributes(), BiomeTags.IS_MOUNTAIN, 3, SpawnKind.GROUND);
        add(MINER_SKELETON, Skeleton.createAttributes(), BiomeTags.HAS_MINESHAFT, 2, SpawnKind.CAVE);
        add(SMALL_SPIDER, Spider.createAttributes(), BiomeTags.IS_OVERWORLD, 2, SpawnKind.CAVE);
        add(LARGE_SPIDER, Spider.createAttributes(), BiomeTags.IS_OVERWORLD, 1, SpawnKind.GROUND);
        add(FOREST_SPIDER, Spider.createAttributes(), BiomeTags.IS_FOREST, 3, SpawnKind.GROUND);
        add(DESERT_SPIDER, Spider.createAttributes(), BiomeTags.IS_BADLANDS, 3, SpawnKind.GROUND);
        add(SWAMP_SPIDER, CaveSpider.createCaveSpider(), IS_SWAMP, 2, SpawnKind.GROUND);
        add(ICE_SPIDER, Spider.createAttributes(), BiomeTags.IS_MOUNTAIN, 2, SpawnKind.GROUND);
        add(CAVE_BUG, Silverfish.createAttributes(), BiomeTags.IS_OVERWORLD, 3, SpawnKind.CAVE);
        add(DESERT_BUG, Silverfish.createAttributes(), BiomeTags.HAS_DESERT_PYRAMID, 2, SpawnKind.CAVE);
        add(END_BUG, Endermite.createAttributes(), BiomeTags.IS_END, 3, SpawnKind.GROUND);
        add(WILD_SLIME, net.minecraft.world.entity.monster.Monster.createMonsterAttributes(), BiomeTags.IS_FOREST, 2, SpawnKind.GROUND);
        add(MUD_SLIME, net.minecraft.world.entity.monster.Monster.createMonsterAttributes(), IS_SWAMP, 3, SpawnKind.GROUND);
        add(SAND_SLIME, net.minecraft.world.entity.monster.Monster.createMonsterAttributes(), BiomeTags.IS_BADLANDS, 3, SpawnKind.GROUND);
        add(LESSER_MAGMA_CUBE, MagmaCube.createAttributes(), BiomeTags.IS_NETHER, 2, SpawnKind.NETHER);
        add(DROWNED_SAILOR, Drowned.createAttributes(), BiomeTags.HAS_SHIPWRECK, 1, SpawnKind.WATER);
        add(DEEP_SEA_DROWNED, Drowned.createAttributes(), BiomeTags.IS_DEEP_OCEAN, 3, SpawnKind.WATER);
        add(RIVER_DROWNED, Drowned.createAttributes(), BiomeTags.IS_RIVER, 2, SpawnKind.WATER);
        add(WILD_PIGLIN, Piglin.createAttributes(), BiomeTags.IS_NETHER, 2, SpawnKind.NETHER);
        add(BANDIT, Pillager.createAttributes(), BiomeTags.IS_FOREST, 2, SpawnKind.GROUND);
        add(BANDIT_ARCHER, Pillager.createAttributes(), BiomeTags.IS_FOREST, 2, SpawnKind.GROUND);
        add(BANDIT_WARRIOR, Vindicator.createAttributes(), BiomeTags.IS_FOREST, 1, SpawnKind.GROUND);
        add(WANDERING_RAIDER, Pillager.createAttributes(), BiomeTags.IS_OVERWORLD, 1, SpawnKind.GROUND);
        add(FERAL_WOLF, Wolf.createAttributes(), BiomeTags.IS_FOREST, 1, SpawnKind.GROUND);
        add(CAVE_BAT, Bat.createAttributes().add(Attributes.ATTACK_DAMAGE, 1.5), BiomeTags.IS_OVERWORLD, 2, SpawnKind.CAVE);
        add(GIANT_BEE, Bee.createAttributes(), BiomeTags.IS_FOREST, 1, SpawnKind.GROUND);
        add(LESSER_PHANTOM, net.minecraft.world.entity.monster.Monster.createMonsterAttributes(), BiomeTags.IS_OVERWORLD, 1, SpawnKind.SKY);
        add(CAVE_CREEPER, Creeper.createAttributes(), BiomeTags.IS_OVERWORLD, 2, SpawnKind.CAVE);
        for (SpawnSpec spec : SPAWNS) {
            FabricDefaultAttributeRegistry.register(spec.type, spec.attributes.build());
            SpawnPlacementsInvoker.moreminecrafts$register(spec.type,
                    spec.kind == SpawnKind.WATER ? SpawnPlacementTypes.IN_WATER :
                            spec.kind == SpawnKind.SKY ? SpawnPlacementTypes.NO_RESTRICTIONS : SpawnPlacementTypes.ON_GROUND,
                    spec.kind == SpawnKind.SKY ? Heightmap.Types.MOTION_BLOCKING : Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> familySpawnRules(type, level, reason, pos, random)
                            && allowed(spec.kind, level, pos));
            BiomeModifications.addSpawn(BiomeSelectors.tag(spec.biomeTag), spec.type.getCategory(),
                    spec.type, spec.weight, spec.minGroup, spec.maxGroup);
        }
        MoreMinecrafts.LOGGER.info("Registered {} vanilla-model custom mobs and spawn rules.", SPAWNS.size());
    }

    private static void add(EntityType<? extends Mob> type, AttributeSupplier.Builder attributes,
                            net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> biomeTag,
                            int weight, SpawnKind kind) {
        SPAWNS.add(new SpawnSpec(type, attributes, biomeTag, weight, 1, kind == SpawnKind.CAVE ? 2 : 3, kind));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean familySpawnRules(EntityType<? extends Mob> type,
                                             net.minecraft.world.level.ServerLevelAccessor level,
                                             net.minecraft.world.entity.EntitySpawnReason reason,
                                             BlockPos pos, net.minecraft.util.RandomSource random) {
        if (type == WILD_SLIME || type == MUD_SLIME || type == SAND_SLIME)
            return Slime.checkSlimeSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == LESSER_MAGMA_CUBE)
            return MagmaCube.checkMagmaCubeSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == DROWNED_SAILOR || type == DEEP_SEA_DROWNED || type == RIVER_DROWNED)
            return Drowned.checkDrownedSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == WILD_PIGLIN)
            return Piglin.checkPiglinSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == FERAL_WOLF)
            return Wolf.checkWolfSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == CAVE_BAT)
            return Bat.checkBatSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == GIANT_BEE)
            return Animal.checkAnimalSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == CAVE_BUG || type == DESERT_BUG)
            return Silverfish.checkSilverfishSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == END_BUG)
            return Endermite.checkEndermiteSpawnRules((EntityType) type, level, reason, pos, random);
        if (type == MOUNTAIN_SKELETON)
            return Stray.checkStraySpawnRules((EntityType) type, level, reason, pos, random);
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    private static boolean allowed(SpawnKind kind, net.minecraft.world.level.ServerLevelAccessor level, BlockPos pos) {
        return switch (kind) {
            case CAVE -> !level.canSeeSky(pos) && pos.getY() < level.getSeaLevel() &&
                    level.getBlockState(pos.below()).isSolid();
            case NETHER -> level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid();
            case WATER -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
            case SKY -> pos.getY() > level.getSeaLevel() + 12 && level.canSeeSky(pos);
            case GROUND -> level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid() &&
                    !level.getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK);
        };
    }

    public static List<EntityType<? extends Mob>> allTypes() {
        return SPAWNS.stream().map(SpawnSpec::type).toList();
    }

    private enum SpawnKind { GROUND, CAVE, NETHER, WATER, SKY }
    private record SpawnSpec(EntityType<? extends Mob> type, AttributeSupplier.Builder attributes,
                             net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> biomeTag,
                             int weight, int minGroup, int maxGroup, SpawnKind kind) {}
}
