package dev.itzgamesgd.moreminecrafts.test.crystalguardian;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registration of the POC test entity {@code moreminecrafts:ancient_crystal_guardian_test}. MISC category, no spawn placement,
 * no spawn egg, no loot table, not saved: it only exists when an operator summons it.
 */
public final class AncientCrystalGuardianTestEntities {
    private AncientCrystalGuardianTestEntities() {}

    public static final String ID = "ancient_crystal_guardian_test";

    public static final EntityType<AncientCrystalGuardianTest> TYPE = register(
            EntityType.Builder.<AncientCrystalGuardianTest>of(AncientCrystalGuardianTest::new, MobCategory.MISC)
                    .sized(2.4F, 4.1F)
                    .eyeHeight(3.6F)
                    .noSave()
                    .noLootTable()
                    .clientTrackingRange(10));

    private static EntityType<AncientCrystalGuardianTest> register(EntityType.Builder<AncientCrystalGuardianTest> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(ID));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(TYPE, AncientCrystalGuardianTest.createAttributes());
        AncientCrystalGuardianTestCommand.initialize();
    }
}
