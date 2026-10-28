package dev.itzgamesgd.moreminecrafts.boss;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registration for the unique Royal Tomb Guardian boss and its temporary telegraph display. */
public final class TombGuardianEntities {
    private TombGuardianEntities() {}

    public static final EntityType<TombGuardian> TOMB_GUARDIAN = register("tomb_guardian",
            EntityType.Builder.<TombGuardian>of(TombGuardian::new, MobCategory.MONSTER)
                    .sized(TombGuardianTuning.WIDTH, TombGuardianTuning.HEIGHT)
                    .eyeHeight(TombGuardianTuning.HEIGHT * 0.85F)
                    .clientTrackingRange(10)
                    .notInPeaceful());

    public static final EntityType<TombVfxDisplay> TOMB_VFX = register("tomb_vfx",
            EntityType.Builder.<TombVfxDisplay>of(TombVfxDisplay::new, MobCategory.MISC)
                    .noLootTable()
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1));

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(path));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(TOMB_GUARDIAN, TombGuardian.createAttributes());
        TombGuardianDebugCommand.initialize();
        MoreMinecrafts.LOGGER.info("Registered Royal Tomb Guardian boss entity.");
    }
}
