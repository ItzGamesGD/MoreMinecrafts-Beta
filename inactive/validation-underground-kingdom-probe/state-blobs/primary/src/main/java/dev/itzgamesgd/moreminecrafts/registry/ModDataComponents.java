package dev.itzgamesgd.moreminecrafts.registry;

import com.mojang.serialization.Codec;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchState;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import dev.itzgamesgd.moreminecrafts.fishing.RodEquipment;
import net.minecraft.world.item.ItemStack;

public final class ModDataComponents {
    public static final DataComponentType<Integer> ENHANCEMENT_LEVEL = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ModIds.id("enhancement_level"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DataComponentType<java.util.Map<String,Integer>> BOOK_OPTIONS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, ModIds.id("book_options"),
            DataComponentType.<java.util.Map<String,Integer>>builder()
                    .persistent(Codec.unboundedMap(Codec.STRING, Codec.intRange(1,5)))
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(Codec.unboundedMap(Codec.STRING, Codec.intRange(1,5))))
                    .build());

    public static final DataComponentType<Integer> FISH_GRADE = integerComponent("fish_grade", 1, 5);
    // Legacy saved stacks use whole centimeters. Keep separate from the active tenths-cm component.
    public static final DataComponentType<Integer> LEGACY_FISH_LENGTH = integerComponent("fish_length", 1, 10000);
    public static final DataComponentType<Integer> FISH_LENGTH_TENTHS_CM = integerComponent("fish_length_tenths_cm", 1, 100000);
    public static final DataComponentType<Integer> FISH_WEIGHT_TENTHS_KG = integerComponent("fish_weight_tenths_kg", 1, 100000);
    public static final DataComponentType<Integer> DRILL_FUEL_POINTS = integerComponent("drill_fuel_points", 0, 1_000_000);
    public static final DataComponentType<ItemStack> ATTACHED_BACKPACK = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, ModIds.id("attached_backpack"),
            DataComponentType.<ItemStack>builder().persistent(ItemStack.OPTIONAL_CODEC)
                    .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC).build());
    public static final DataComponentType<RodEquipment> ROD_EQUIPMENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, ModIds.id("rod_equipment"),
            DataComponentType.<RodEquipment>builder().persistent(RodEquipment.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(RodEquipment.CODEC)).build());
    public static final DataComponentType<FurnaceResearchState> PORTABLE_FURNACE_RESEARCH_STATE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, ModIds.id("portable_furnace_research_state"),
            DataComponentType.<FurnaceResearchState>builder().persistent(FurnaceResearchState.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(FurnaceResearchState.CODEC)).build());

    private static DataComponentType<Integer> integerComponent(String id, int min, int max) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ModIds.id(id),
                DataComponentType.<Integer>builder().persistent(Codec.intRange(min,max)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    }

    private ModDataComponents() {
    }

    public static void initialize() {
        var registeredId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(ENHANCEMENT_LEVEL);
        if (!ModIds.id("enhancement_level").equals(registeredId)) {
            throw new IllegalStateException("Enhancement level data component was not registered correctly.");
        }
        MoreMinecrafts.LOGGER.info("Data component registration OK: {}", registeredId);
    }
}
