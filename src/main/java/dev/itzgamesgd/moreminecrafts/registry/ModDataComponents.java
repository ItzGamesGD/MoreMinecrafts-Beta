package dev.itzgamesgd.moreminecrafts.registry;

import com.mojang.serialization.Codec;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;

public final class ModDataComponents {
    public static final DataComponentType<Integer> ENHANCEMENT_LEVEL = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ModIds.id("enhancement_level"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 1))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

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
