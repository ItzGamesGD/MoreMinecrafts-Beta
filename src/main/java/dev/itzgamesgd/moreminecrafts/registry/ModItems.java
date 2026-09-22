package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.item.FoundationProbeItem;
import dev.itzgamesgd.moreminecrafts.special.flame.FlameSwordItem;
import dev.itzgamesgd.moreminecrafts.special.wind.WindSwordItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ToolMaterial;

public final class ModItems {
    private static final ResourceKey<Item> FOUNDATION_PROBE_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("foundation_probe"));
    public static final ResourceKey<Item> ENHANCEMENT_STONE_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("enhancement_stone"));
    public static final ResourceKey<Item> FLAME_SWORD_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("flame_sword"));
    public static final ResourceKey<Item> WIND_SWORD_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("wind_sword"));

    public static final Item FOUNDATION_PROBE = Registry.register(
            BuiltInRegistries.ITEM,
            FOUNDATION_PROBE_KEY,
            new FoundationProbeItem(new Item.Properties().setId(FOUNDATION_PROBE_KEY))
    );
    public static final Item ENHANCEMENT_STONE = Registry.register(
            BuiltInRegistries.ITEM,
            ENHANCEMENT_STONE_KEY,
            new Item(new Item.Properties().setId(ENHANCEMENT_STONE_KEY))
    );
    public static final Item FLAME_SWORD = Registry.register(
            BuiltInRegistries.ITEM,
            FLAME_SWORD_KEY,
            new FlameSwordItem(new Item.Properties()
                    .sword(ToolMaterial.NETHERITE, 4.0F, -2.6F)
                    .fireResistant()
                    .setId(FLAME_SWORD_KEY))
    );
    public static final Item WIND_SWORD = Registry.register(
            BuiltInRegistries.ITEM,
            WIND_SWORD_KEY,
            new WindSwordItem(new Item.Properties()
                    .sword(ToolMaterial.DIAMOND, 4.0F, -2.2F)
                    .setId(WIND_SWORD_KEY))
    );

    private ModItems() {
    }

    public static void initialize() {
        var registeredId = BuiltInRegistries.ITEM.getKey(FOUNDATION_PROBE);

        if (!ModIds.id("foundation_probe").equals(registeredId)) {
            throw new IllegalStateException("MoreMinecrafts foundation registry probe was not registered correctly.");
        }

        MoreMinecrafts.LOGGER.info("Registry probe OK: {}", registeredId);

        var enhancementStoneId = BuiltInRegistries.ITEM.getKey(ENHANCEMENT_STONE);

        if (!ModIds.id("enhancement_stone").equals(enhancementStoneId)) {
            throw new IllegalStateException("MoreMinecrafts enhancement stone was not registered correctly.");
        }

        MoreMinecrafts.LOGGER.info("Item registration OK: {}", enhancementStoneId);

        var flameSwordId = BuiltInRegistries.ITEM.getKey(FLAME_SWORD);
        if (!ModIds.id("flame_sword").equals(flameSwordId)) {
            throw new IllegalStateException("MoreMinecrafts Flame Sword was not registered correctly.");
        }
        MoreMinecrafts.LOGGER.info("Item registration OK: {}", flameSwordId);

        var windSwordId = BuiltInRegistries.ITEM.getKey(WIND_SWORD);
        if (!ModIds.id("wind_sword").equals(windSwordId)) {
            throw new IllegalStateException("MoreMinecrafts Wind Sword was not registered correctly.");
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> entries.accept(WIND_SWORD));
        MoreMinecrafts.LOGGER.info("Item registration OK: {}", windSwordId);
    }
}
