package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.skeleton.Skeleton;

/** Summonable visual variants. Encounter placement is deliberately separate. */
public final class PyramidMobs {
    private PyramidMobs() {}
    public static final EntityType<Husk> BURIED_MUMMY = register("buried_mummy", Husk::new, 1.95F);
    public static final EntityType<Skeleton> CRYPT_ARCHER = register("crypt_archer", Skeleton::new, 1.99F);
    public static final EntityType<Zombie> FUNERAL_SERVANT = register("funeral_servant", Zombie::new, 1.95F);
    public static final EntityType<Husk> GILDED_MUMMY = register("gilded_mummy", Husk::new, 1.95F);

    private static <T extends Mob> EntityType<T> register(String name, EntityType.EntityFactory<T> factory, float height) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(0.6F, height).clientTrackingRange(8).notInPeaceful().build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(BURIED_MUMMY, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(CRYPT_ARCHER, Skeleton.createAttributes());
        FabricDefaultAttributeRegistry.register(FUNERAL_SERVANT, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(GILDED_MUMMY, Zombie.createAttributes());
    }
}
