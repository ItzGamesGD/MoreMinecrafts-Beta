package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registration of the gold guide golem. The golem never spawns by itself (no spawn rules, no egg); only the entrance runtime creates it. */
public final class SecretEntranceEntities {
    private SecretEntranceEntities() {}

    public static final EntityType<GoldGuideGolem> GOLD_GUIDE_GOLEM = register("gold_guide_golem",
            EntityType.Builder.<GoldGuideGolem>of(GoldGuideGolem::new, MobCategory.MISC)
                    .sized(GoldGuideGolemTuning.BASE_WIDTH, GoldGuideGolemTuning.BASE_HEIGHT)
                    .eyeHeight(GoldGuideGolemTuning.BASE_HEIGHT * 0.85F)
                    .noSave()
                    .noLootTable()
                    .clientTrackingRange(10));

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(GOLD_GUIDE_GOLEM, GoldGuideGolem.createAttributes());
        MoreMinecrafts.LOGGER.info("Registered gold guide golem entity.");
    }
}
