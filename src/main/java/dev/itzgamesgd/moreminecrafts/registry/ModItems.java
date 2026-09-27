package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.item.FoundationProbeItem;
import dev.itzgamesgd.moreminecrafts.fishing.*;
import java.util.EnumMap;
import java.util.Map;
import dev.itzgamesgd.moreminecrafts.special.flame.FlameSwordItem;
import dev.itzgamesgd.moreminecrafts.special.wind.WindSwordItem;
import dev.itzgamesgd.moreminecrafts.special.elemental.DarkEnergyStaffItem;
import dev.itzgamesgd.moreminecrafts.special.elemental.EarthMaceItem;
import dev.itzgamesgd.moreminecrafts.special.elemental.ElementalSwordItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.MaceItem;

public final class ModItems {
    public static final Item RAW_TITANIUM = material("raw_titanium");
    public static final Item TITANIUM_INGOT = material("titanium_ingot");
    public static final Item RAW_TUNGSTEN = material("raw_tungsten");
    public static final Item TUNGSTEN_INGOT = material("tungsten_ingot");
    public static final Item MYTHRIL = material("mythril");
    public static final Item ETHERIUM = material("etherium");
    public static final Item ADAMANTIUM_INGOT = fireResistantMaterial("adamantium_ingot");
    private static final ResourceKey<Item> FOUNDATION_PROBE_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("foundation_probe"));
    public static final ResourceKey<Item> ENHANCEMENT_STONE_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("enhancement_stone"));
    public static final ResourceKey<Item> FLAME_SWORD_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("flame_sword"));
    public static final ResourceKey<Item> WIND_SWORD_KEY =
            ResourceKey.create(Registries.ITEM, ModIds.id("wind_sword"));
    public static final ResourceKey<Item> FLOWING_WATER_SWORD_KEY = key("flowing_water_sword");
    public static final ResourceKey<Item> WIND_ARCHERS_BOW_KEY = key("wind_archers_bow");
    public static final ResourceKey<Item> EARTH_MACE_KEY = key("earth_mace");
    public static final ResourceKey<Item> LIGHTNING_SWORD_KEY = key("lightning_sword");
    public static final ResourceKey<Item> DARK_ENERGY_STAFF_KEY = key("dark_energy_staff");

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
    public static final Item FLOWING_WATER_SWORD = Registry.register(BuiltInRegistries.ITEM, FLOWING_WATER_SWORD_KEY,
            new ElementalSwordItem(ElementalSwordItem.Element.WATER, new Item.Properties().sword(ToolMaterial.DIAMOND, 4.0F, -2.0F).setId(FLOWING_WATER_SWORD_KEY)));
    public static final Item WIND_ARCHERS_BOW = Registry.register(BuiltInRegistries.ITEM, WIND_ARCHERS_BOW_KEY,
            new BowItem(new Item.Properties().durability(465).enchantable(1).setId(WIND_ARCHERS_BOW_KEY)));
    public static final Item EARTH_MACE = Registry.register(BuiltInRegistries.ITEM, EARTH_MACE_KEY,
            new EarthMaceItem(new Item.Properties().durability(600).attributes(MaceItem.createAttributes()).setId(EARTH_MACE_KEY)));
    public static final Item LIGHTNING_SWORD = Registry.register(BuiltInRegistries.ITEM, LIGHTNING_SWORD_KEY,
            new ElementalSwordItem(ElementalSwordItem.Element.LIGHTNING, new Item.Properties().sword(ToolMaterial.DIAMOND, 4.0F, -2.2F).setId(LIGHTNING_SWORD_KEY)));
    public static final Item DARK_ENERGY_STAFF = Registry.register(BuiltInRegistries.ITEM, DARK_ENERGY_STAFF_KEY,
            new DarkEnergyStaffItem(new Item.Properties().durability(420).setId(DARK_ENERGY_STAFF_KEY)));


    public static final Map<FishSpecies, Item> FISH = new EnumMap<>(FishSpecies.class);
    public static final Item[][] FISHING_EQUIPMENT = new Item[FishingEquipmentFamily.values().length][FishingEquipmentTier.MAX];
    static {
        for (FishSpecies species : FishSpecies.values()) FISH.put(species, fish(species));
        for (FishingEquipmentFamily family : FishingEquipmentFamily.values())
            for (int tier=1; tier<=FishingEquipmentTier.MAX; tier++) FISHING_EQUIPMENT[family.ordinal()][tier-1]=equipment(family,tier);
    }
    public static Item fishItem(FishSpecies species) { return FISH.get(species); }
    public static Item equipmentItem(FishingEquipmentFamily family,int tier){return FISHING_EQUIPMENT[family.ordinal()][FishingEquipmentTier.requireValid(tier)-1];}
    private static Item fish(FishSpecies species){
        ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,ModIds.id(species.id()));
        return Registry.register(BuiltInRegistries.ITEM,key,new FishItem(species,new Item.Properties().stacksTo(1).setId(key)));
    }
    private static Item equipment(FishingEquipmentFamily family,int tier){
        String path=family.name().toLowerCase()+"_"+tier;
        ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,ModIds.id(path));
        int durability = (family == FishingEquipmentFamily.REEL ? 240 : 96) + tier * 48;
        return Registry.register(BuiltInRegistries.ITEM,key,new FishingEquipmentItem(family,tier,new Item.Properties().durability(durability).setId(key)));
    }

    private ModItems() {
    }

    private static ResourceKey<Item> key(String path) { return ResourceKey.create(Registries.ITEM, ModIds.id(path)); }

    private static Item material(String path) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModIds.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
    }

    private static Item fireResistantMaterial(String path) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModIds.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new Item(new Item.Properties().fireResistant().setId(key)));
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
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(FLAME_SWORD);
            entries.accept(FLOWING_WATER_SWORD);
            entries.accept(WIND_SWORD);
            entries.accept(WIND_ARCHERS_BOW);
            entries.accept(EARTH_MACE);
            entries.accept(LIGHTNING_SWORD);
            entries.accept(DARK_ENERGY_STAFF);
            entries.accept(WIND_ARCHERS_BOW); entries.accept(EARTH_MACE); entries.accept(LIGHTNING_SWORD);
            entries.accept(DARK_ENERGY_STAFF);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            FISH.values().forEach(entries::accept);
            for (Item[] family : FISHING_EQUIPMENT) for (Item item : family) entries.accept(item);
            entries.accept(RAW_TITANIUM);
            entries.accept(TITANIUM_INGOT);
            entries.accept(RAW_TUNGSTEN);
            entries.accept(TUNGSTEN_INGOT);
            entries.accept(MYTHRIL);
            entries.accept(ETHERIUM);
            entries.accept(ADAMANTIUM_INGOT);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            entries.accept(ModBlocks.TITANIUM_ORE);
            entries.accept(ModBlocks.DEEPSLATE_TITANIUM_ORE);
            entries.accept(ModBlocks.TUNGSTEN_ORE);
            entries.accept(ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
            entries.accept(ModBlocks.MYTHRIL_ORE);
            entries.accept(ModBlocks.DEEPSLATE_MYTHRIL_ORE);
            entries.accept(ModBlocks.ETHERIUM_ORE);
            entries.accept(ModBlocks.DEEPSLATE_ETHERIUM_ORE);
            entries.accept(ModBlocks.ADAMANTIUM_DEBRIS);
            entries.accept(ModBlocks.TITANIUM_BLOCK);
            entries.accept(ModBlocks.TUNGSTEN_BLOCK);
            entries.accept(ModBlocks.MYTHRIL_BLOCK);
            entries.accept(ModBlocks.ETHERIUM_BLOCK);
            entries.accept(ModBlocks.ADAMANTIUM_BLOCK);
        });
        MoreMinecrafts.LOGGER.info("Item registration OK: {}", windSwordId);
    }
}
