package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mixin.SpawnPlacementsInvoker;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * ST-01 pyramid visual variants (PYRAMID_MOB_ROSTER_DESIGN_2026-10-26): vanilla Husk, Skeleton and Zombie behaviour
 * with their vanilla attributes, dimensions, AI, loot and spawn-placement rules; only the texture differs, and the
 * water (Husk to Zombie, Zombie to Drowned) and powder-snow (Skeleton to Stray) conversions are blocked
 * (owner decision 2026-11-14).
 * Natural spawning and pyramid encounter placement are deliberately not part of this registration.
 */
public final class PyramidMobs {
    private PyramidMobs() {}

    public static final EntityType<PyramidHusk> BURIED_MUMMY = register("buried_mummy",
            EntityType.Builder.of(PyramidHusk::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .passengerAttachments(2.075F).ridingOffset(-0.7F));
    public static final EntityType<CryptArcher> CRYPT_ARCHER = register("crypt_archer",
            EntityType.Builder.of(CryptArcher::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F)
                    .ridingOffset(-0.7F));
    public static final EntityType<PyramidZombie> FUNERAL_SERVANT = register("funeral_servant",
            EntityType.Builder.of(PyramidZombie::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .passengerAttachments(2.0125F).ridingOffset(-0.7F));
    public static final EntityType<PyramidHusk> GILDED_MUMMY = register("gilded_mummy",
            EntityType.Builder.of(PyramidHusk::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .passengerAttachments(2.075F).ridingOffset(-0.7F));

    /** The four variants in roster order. */
    public static final List<EntityType<?>> ALL = List.of(BURIED_MUMMY, CRYPT_ARCHER, FUNERAL_SERVANT, GILDED_MUMMY);

    private static <T extends Monster> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                builder.clientTrackingRange(8).notInPeaceful().build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(BURIED_MUMMY, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(CRYPT_ARCHER, AbstractSkeleton.createAttributes());
        FabricDefaultAttributeRegistry.register(FUNERAL_SERVANT, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(GILDED_MUMMY, Zombie.createAttributes());
        // Same placement rules as the vanilla base types (used by spawners, reinforcements and spawn checks).
        SpawnPlacementsInvoker.moreminecrafts$register(BURIED_MUMMY, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkSurfaceMonstersSpawnRules);
        SpawnPlacementsInvoker.moreminecrafts$register(GILDED_MUMMY, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkSurfaceMonstersSpawnRules);
        SpawnPlacementsInvoker.moreminecrafts$register(CRYPT_ARCHER, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        SpawnPlacementsInvoker.moreminecrafts$register(FUNERAL_SERVANT, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
    }
}
