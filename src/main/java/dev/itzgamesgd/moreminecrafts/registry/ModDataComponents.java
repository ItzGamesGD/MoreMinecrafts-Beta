package dev.itzgamesgd.moreminecrafts.registry;

import com.mojang.serialization.Codec;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import dev.itzgamesgd.moreminecrafts.fishing.RodEquipment;

public final class ModDataComponents {
    public static final DataComponentType<Integer> ENHANCEMENT_LEVEL = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ModIds.id("enhancement_level"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, Integer.MAX_VALUE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    public static final DataComponentType<Integer> FISH_GRADE = integerComponent("fish_grade", 1, 5);
    public static final DataComponentType<Integer> FISH_LENGTH = integerComponent("fish_length", 1, 10000);
    public static final DataComponentType<RodEquipment> ROD_EQUIPMENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, ModIds.id("rod_equipment"),
            DataComponentType.<RodEquipment>builder().persistent(RodEquipment.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(RodEquipment.CODEC)).build());

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
