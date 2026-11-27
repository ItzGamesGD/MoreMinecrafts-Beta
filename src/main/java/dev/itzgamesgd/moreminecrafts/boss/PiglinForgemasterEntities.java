package dev.itzgamesgd.moreminecrafts.boss;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registration for the Lava Forgemaster (piglin fire boss). No natural spawn: the summon trigger is deferred by the design document. */
public final class PiglinForgemasterEntities {
    private PiglinForgemasterEntities() {}

    // About 1.75x the player (document U-01); the model is 3.14 blocks tall.
    public static final EntityType<PiglinForgemaster> PIGLIN_FORGEMASTER = register("piglin_forgemaster",
            EntityType.Builder.<PiglinForgemaster>of(PiglinForgemaster::new, MobCategory.MONSTER)
                    .sized(1.5F, 3.2F)
                    .eyeHeight(2.8F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .notInPeaceful());

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(path));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(PIGLIN_FORGEMASTER, PiglinForgemaster.createAttributes());
        PiglinForgemasterDebugCommand.initialize();
    }
}
