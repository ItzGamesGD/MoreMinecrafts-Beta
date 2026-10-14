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
import dev.itzgamesgd.moreminecrafts.equipment.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;

public final class ModItems {
    public static final Item RAW_TITANIUM = material("raw_titanium");
    public static final Item TITANIUM_INGOT = material("titanium_ingot");
    public static final Item RAW_TUNGSTEN = material("raw_tungsten");
    public static final Item TUNGSTEN_INGOT = material("tungsten_ingot");
    public static final Item MYTHRIL = material("mythril");
    public static final Item ETHERIUM = material("etherium");
    public static final Item ADAMANTIUM_INGOT = fireResistantMaterial("adamantium_ingot");
    public static final Item EYE_OF_TYPHOON = core("eye_of_typhoon");
    public static final Item HEART_OF_THUNDER = core("heart_of_thunder");
    public static final Item BURNING_HEART = core("burning_heart");
    public static final Item HEART_OF_CURRENTS = core("heart_of_currents");
    public static final Item ANCIENT_HEART = core("ancient_heart");
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

    public static final Item MOB_CAPTURE_TOOL = register("mob_capture_tool", key -> new Item(new Item.Properties().durability(30).setId(key)));
    public static final Item CAPTURED_MOB_EGG = register("captured_mob_egg", key -> new CapturedMobEggItem(new Item.Properties().stacksTo(1).setId(key)));
    public static final Item PARACHUTE = register("parachute", key -> new ParachuteItem(new Item.Properties().stacksTo(16).setId(key)));
    public static final Item MINER_LANTERN = simpleUtility("miner_lantern");
    public static final Item POTION_INJECTOR = register("potion_injector", key -> new PotionInjectorItem(
            new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER,
                    net.minecraft.world.item.component.ItemContainerContents.EMPTY).setId(key)));
    public static final Item PORTABLE_FURNACE = register("portable_furnace", key ->
            new PortableFurnaceItem(new Item.Properties().stacksTo(1).setId(key)));
    public static final Item MINING_DRILL = register("mining_drill", key -> new MiningDrillItem(
            new Item.Properties().pickaxe(ToolMaterial.NETHERITE, 1.0F, -2.8F)
                    .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                    .component(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY)
                    .component(ModDataComponents.DRILL_FUEL_POINTS, 0).setId(key)));
    public static final Item SMALL_BACKPACK = backpack("small_backpack", 9);
    public static final Item BACKPACK = backpack("backpack", 27);
    public static final Item LARGE_BACKPACK = backpack("large_backpack", 54);
    public static final Map<WeaponBalance.Tier, Item> DAGGERS = new EnumMap<>(WeaponBalance.Tier.class);
    public static final Map<WeaponBalance.Tier, Item> GREATSWORDS = new EnumMap<>(WeaponBalance.Tier.class);
    public static final Item GAUNTLET;


    public static final Map<FishSpecies, Item> FISH = new EnumMap<>(FishSpecies.class);
    public static final Item[][] FISHING_EQUIPMENT = new Item[FishingEquipmentFamily.values().length][FishingEquipmentTier.MAX];
    static {
        for (WeaponBalance.Values value : WeaponBalance.VALUES) {
            String tier = value.tier().name().toLowerCase();
            DAGGERS.put(value.tier(), register(tier + "_dagger", key -> new BasicWeaponItem(BasicWeaponItem.Kind.DAGGER,
                    WeaponProperties.tierProperties(WeaponProperties.create(key, value.daggerDamage(), WeaponBalance.DAGGER_ATTACK_SPEED,
                            LowerEquipmentRules.DAGGER_REACH), value.tier()))));
            GREATSWORDS.put(value.tier(), register(tier + "_greatsword", key -> new GreatswordItem(
                    WeaponProperties.tierProperties(WeaponProperties.create(key, value.greatswordDamage(), WeaponBalance.GREATSWORD_ATTACK_SPEED,
                            LowerEquipmentRules.GREATSWORD_REACH), value.tier()))));
        }
        GAUNTLET = register("gauntlet", key -> new GauntletItem(WeaponProperties.create(key,
                LowerEquipmentRules.GAUNTLET_DIRECT_DAMAGE, WeaponBalance.GAUNTLET_ATTACK_SPEED,
                LowerEquipmentRules.GAUNTLET_REACH)));
        for (FishSpecies species : FishSpecies.values()) FISH.put(species, fish(species));
        for (FishingEquipmentFamily family : FishingEquipmentFamily.values())
            for (int tier=1; tier<=FishingEquipmentTier.MAX; tier++) FISHING_EQUIPMENT[family.ordinal()][tier-1]=equipment(family,tier);

        // Keep old save identifiers decodable without restoring them to active catch or module logic.
        String[] legacyFishIds = {
                "bluegill", "perch", "trout", "catfish", "salmon",
                "pike", "sturgeon", "golden_koi", "abyssal_angler", "ancient_coelacanth"
        };
        for (String id : legacyFishIds) {
            register(id, key -> new Item(new Item.Properties().stacksTo(1).setId(key)));
        }
        for (FishingEquipmentFamily family : FishingEquipmentFamily.values()) {
            for (int tier = FishingEquipmentTier.MAX + 1; tier <= 5; tier++) {
                String id = family.name().toLowerCase() + "_" + tier;
                int maxDamage = 96 + tier * 48;
                register(id, key -> new Item(new Item.Properties()
                        .stacksTo(1).durability(maxDamage).setId(key)));
            }
        }
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

    private static Item simpleUtility(String path) {
        return register(path, key -> new Item(new Item.Properties().stacksTo(1).setId(key)));
    }
    private static Item core(String path) {
        return register(path, key -> new Item(new Item.Properties().stacksTo(1).fireResistant().setId(key)));
    }
    private static Item backpack(String path, int capacity) {
        return register(path, key -> new BackpackItem(capacity, new Item.Properties().stacksTo(1).setId(key)));
    }
    private static Item register(String path, java.util.function.Function<ResourceKey<Item>, Item> factory) {
        ResourceKey<Item> key = key(path);
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
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
            DAGGERS.values().forEach(entries::accept);
            GREATSWORDS.values().forEach(entries::accept);
            entries.accept(GAUNTLET);
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
            entries.accept(MINER_LANTERN);
            entries.accept(PARACHUTE);
            entries.accept(MOB_CAPTURE_TOOL);
            entries.accept(ModBlocks.AUTO_FARM.asItem());
            entries.accept(CAPTURED_MOB_EGG);
            entries.accept(POTION_INJECTOR);
            entries.accept(PORTABLE_FURNACE);
            entries.accept(MINING_DRILL);
            entries.accept(SMALL_BACKPACK);
            entries.accept(BACKPACK);
            entries.accept(LARGE_BACKPACK);
            entries.accept(EYE_OF_TYPHOON);
            entries.accept(HEART_OF_THUNDER);
            entries.accept(BURNING_HEART);
            entries.accept(HEART_OF_CURRENTS);
            entries.accept(ANCIENT_HEART);
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
            entries.accept(ModBlocks.EXPLORATION_SLEEPING_BAG);
            entries.accept(ModBlocks.FISH_TRAP);
        });
        MoreMinecrafts.LOGGER.info("Item registration OK: {}", windSwordId);
    }
}
