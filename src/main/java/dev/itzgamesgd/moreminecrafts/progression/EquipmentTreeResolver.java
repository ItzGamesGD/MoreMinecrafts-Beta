package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/** Explicit vanilla item/category and custom-item profile mapping for shared T1/T2 trees. */
public final class EquipmentTreeResolver {
    private static final Map<Item, EnchantmentTrees.Equipment> VANILLA = vanillaProfiles();
    private static final Map<Identifier, EnchantmentTrees.Equipment> CUSTOM_IDENTIFIERS = customProfiles();
    private static final Map<Item, EnchantmentTrees.Equipment> CUSTOM_ITEMS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static volatile boolean BUILT_IN_CUSTOM_PROFILES_REGISTERED;
    private static final Map<Item, EnchantmentTrees.Equipment> EXTENSIONS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private EquipmentTreeResolver() {}

    /** Add an explicitly categorized mod item without relying on registry-name conventions. */
    public static void registerExtension(Item item, EnchantmentTrees.Equipment profile) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(profile);
        if (profile == EnchantmentTrees.Equipment.NONE) EXTENSIONS.remove(item);
        else EXTENSIONS.put(item, profile);
    }

    public static EnchantmentTrees.Equipment resolve(ItemStack stack) {
        if (stack.isEmpty()) return EnchantmentTrees.Equipment.NONE;
        Item item = stack.getItem();
        EnchantmentTrees.Equipment extension = EXTENSIONS.get(item);
        if (extension != null) return extension;
        EnchantmentTrees.Equipment vanilla = vanillaProfile(item);
        if (vanilla != EnchantmentTrees.Equipment.NONE) return vanilla;
        ensureBuiltInCustomProfiles();
        EnchantmentTrees.Equipment customItem = CUSTOM_ITEMS.get(item);
        if (customItem != null) return customItem;
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return customProfile(id);
    }

    static EnchantmentTrees.Equipment customProfile(Identifier id) {
        return CUSTOM_IDENTIFIERS.getOrDefault(id, EnchantmentTrees.Equipment.NONE);
    }

    static EnchantmentTrees.Equipment vanillaProfile(Item item) {
        return VANILLA.getOrDefault(item, EnchantmentTrees.Equipment.NONE);
    }

    private static void ensureBuiltInCustomProfiles() {
        if (BUILT_IN_CUSTOM_PROFILES_REGISTERED) return;
        synchronized (CUSTOM_ITEMS) {
            if (BUILT_IN_CUSTOM_PROFILES_REGISTERED) return;
            for (Item item : ModItems.DAGGERS.values()) CUSTOM_ITEMS.put(item, EnchantmentTrees.Equipment.SWORD);
            for (Item item : ModItems.GREATSWORDS.values()) CUSTOM_ITEMS.put(item, EnchantmentTrees.Equipment.SWORD);
            CUSTOM_ITEMS.put(ModItems.GAUNTLET, EnchantmentTrees.Equipment.GAUNTLET);
            CUSTOM_ITEMS.put(ModItems.FLAME_SWORD, EnchantmentTrees.Equipment.SWORD);
            CUSTOM_ITEMS.put(ModItems.WIND_SWORD, EnchantmentTrees.Equipment.SWORD);
            CUSTOM_ITEMS.put(ModItems.FLOWING_WATER_SWORD, EnchantmentTrees.Equipment.SWORD);
            CUSTOM_ITEMS.put(ModItems.LIGHTNING_SWORD, EnchantmentTrees.Equipment.SWORD);
            CUSTOM_ITEMS.put(ModItems.WIND_ARCHERS_BOW, EnchantmentTrees.Equipment.BOW);
            CUSTOM_ITEMS.put(ModItems.EARTH_MACE, EnchantmentTrees.Equipment.MACE);
            // Dark Energy Staff currently has no approved gameplay/Enhancement profile.
            BUILT_IN_CUSTOM_PROFILES_REGISTERED = true;
        }
    }

    private static Map<Item, EnchantmentTrees.Equipment> vanillaProfiles() {
        Map<Item, EnchantmentTrees.Equipment> profiles = new IdentityHashMap<>();
        add(profiles, EnchantmentTrees.Equipment.SWORD, Items.WOODEN_SWORD, Items.STONE_SWORD,
                Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        add(profiles, EnchantmentTrees.Equipment.AXE, Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
                Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        add(profiles, EnchantmentTrees.Equipment.PICKAXE, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE,
                Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
        add(profiles, EnchantmentTrees.Equipment.SHOVEL, Items.WOODEN_SHOVEL, Items.STONE_SHOVEL,
                Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL);
        add(profiles, EnchantmentTrees.Equipment.HOE, Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE,
                Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        add(profiles, EnchantmentTrees.Equipment.MACE, Items.MACE);
        add(profiles, EnchantmentTrees.Equipment.SPEAR, Items.WOODEN_SPEAR, Items.STONE_SPEAR,
                Items.COPPER_SPEAR, Items.IRON_SPEAR, Items.GOLDEN_SPEAR, Items.DIAMOND_SPEAR, Items.NETHERITE_SPEAR);
        add(profiles, EnchantmentTrees.Equipment.TRIDENT, Items.TRIDENT);
        add(profiles, EnchantmentTrees.Equipment.BOW, Items.BOW);
        add(profiles, EnchantmentTrees.Equipment.CROSSBOW, Items.CROSSBOW);
        add(profiles, EnchantmentTrees.Equipment.HELMET, Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET,
                Items.IRON_HELMET, Items.GOLDEN_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET,
                Items.TURTLE_HELMET);
        add(profiles, EnchantmentTrees.Equipment.CHESTPLATE, Items.LEATHER_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE);
        add(profiles, EnchantmentTrees.Equipment.LEGGINGS, Items.LEATHER_LEGGINGS,
                Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.GOLDEN_LEGGINGS,
                Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS);
        add(profiles, EnchantmentTrees.Equipment.BOOTS, Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS,
                Items.IRON_BOOTS, Items.GOLDEN_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS);
        add(profiles, EnchantmentTrees.Equipment.ELYTRA, Items.ELYTRA);
        add(profiles, EnchantmentTrees.Equipment.SHIELD, Items.SHIELD);
        add(profiles, EnchantmentTrees.Equipment.FISHING_ROD, Items.FISHING_ROD);
        return Collections.unmodifiableMap(profiles);
    }

    private static Map<Identifier, EnchantmentTrees.Equipment> customProfiles() {
        Map<Identifier, EnchantmentTrees.Equipment> profiles = new HashMap<>();
        for (WeaponBalance.Tier tier : WeaponBalance.Tier.values()) {
            String name = tier.name().toLowerCase(Locale.ROOT);
            profiles.put(ModIds.id(name + "_dagger"), EnchantmentTrees.Equipment.SWORD);
            profiles.put(ModIds.id(name + "_greatsword"), EnchantmentTrees.Equipment.SWORD);
        }
        profiles.put(ModIds.id("gauntlet"), EnchantmentTrees.Equipment.GAUNTLET);
        profiles.put(ModIds.id("flame_sword"), EnchantmentTrees.Equipment.SWORD);
        profiles.put(ModIds.id("wind_sword"), EnchantmentTrees.Equipment.SWORD);
        profiles.put(ModIds.id("flowing_water_sword"), EnchantmentTrees.Equipment.SWORD);
        profiles.put(ModIds.id("lightning_sword"), EnchantmentTrees.Equipment.SWORD);
        profiles.put(ModIds.id("wind_archers_bow"), EnchantmentTrees.Equipment.BOW);
        profiles.put(ModIds.id("earth_mace"), EnchantmentTrees.Equipment.MACE);
        // Dark Energy Staff currently has no approved gameplay/Enhancement profile.
        return Map.copyOf(profiles);
    }

    private static void add(Map<Item, EnchantmentTrees.Equipment> profiles,
            EnchantmentTrees.Equipment profile, Item... items) {
        for (Item item : items) profiles.put(item, profile);
    }
}
