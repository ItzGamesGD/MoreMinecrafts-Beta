package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.fishing.FishTrapBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<FishTrapBlockEntity> FISH_TRAP = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, ModIds.id("fish_trap"),
            new BlockEntityType<>(FishTrapBlockEntity::new, java.util.Set.of(ModBlocks.FISH_TRAP)));
    public static final BlockEntityType<dev.itzgamesgd.moreminecrafts.progression.AutoFarmBlockEntity> AUTO_FARM =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModIds.id("auto_farm"),
                new BlockEntityType<>(dev.itzgamesgd.moreminecrafts.progression.AutoFarmBlockEntity::new,
                        java.util.Set.of(ModBlocks.AUTO_FARM)));
    private ModBlockEntities() {}
    public static void initialize() {}
}
