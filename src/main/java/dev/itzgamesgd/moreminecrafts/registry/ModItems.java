package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
    private static final ResourceKey<Item> FOUNDATION_PROBE_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("foundation_probe"));

    public static final Item FOUNDATION_PROBE = Registry.register(
            BuiltInRegistries.ITEM,
            FOUNDATION_PROBE_KEY,
            new Item(new Item.Properties().setId(FOUNDATION_PROBE_KEY))
    );

    private ModItems() {
    }

    public static void initialize() {
        var registeredId = BuiltInRegistries.ITEM.getKey(FOUNDATION_PROBE);

        if (!ModIds.id("foundation_probe").equals(registeredId)) {
            throw new IllegalStateException("MoreMinecrafts foundation registry probe was not registered correctly.");
        }

        MoreMinecrafts.LOGGER.info("Registry probe OK: {}", registeredId);
    }
}
