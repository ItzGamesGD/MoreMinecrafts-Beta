package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/** Explicit item/category/profile associations. Custom IDs avoid a ModItems initialization cycle. */
public final class EnhancementCatalog {
    private static final Map<Item, EnhancementDefinition> VANILLA = createVanillaDefinitions();
    private static final Map<Identifier, EnhancementDefinition> CUSTOM = Map.of(
            ModIds.id("flame_sword"), definition(EnhancementCategory.ELEMENTAL, EnhancementProfile.SWORD),
            ModIds.id("flowing_water_sword"), definition(EnhancementCategory.ELEMENTAL, EnhancementProfile.SWORD),
            ModIds.id("wind_sword"), definition(EnhancementCategory.ELEMENTAL, EnhancementProfile.SWORD),
            ModIds.id("wind_archers_bow"), definition(EnhancementCategory.ELEMENTAL, EnhancementProfile.RANGED),
            ModIds.id("earth_mace"), definition(EnhancementCategory.ELEMENTAL, EnhancementProfile.MACE),
            ModIds.id("lightning_sword"), definition(EnhancementCategory.ELEMENTAL, EnhancementProfile.SWORD)
    );

    private EnhancementCatalog() {}

    public static Optional<EnhancementDefinition> definition(Item item) {
        EnhancementDefinition vanilla = VANILLA.get(item);
        return vanilla != null ? Optional.of(vanilla)
                : customDefinition(BuiltInRegistries.ITEM.getKey(item));
    }

    static Optional<EnhancementDefinition> customDefinition(Identifier id) {
        EnhancementDefinition existing = CUSTOM.get(id);
        if (existing != null) return Optional.of(existing);
        if (id.getNamespace().equals("moreminecrafts") && java.util.Arrays.stream(
                dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.values()).anyMatch(tier ->
                id.getPath().equals(tier.name().toLowerCase(java.util.Locale.ROOT) + "_dagger")
                || id.getPath().equals(tier.name().toLowerCase(java.util.Locale.ROOT) + "_greatsword")))
            return Optional.of(definition(EnhancementCategory.VANILLA, EnhancementProfile.SWORD));
        return Optional.empty();
    }

    private static Map<Item, EnhancementDefinition> createVanillaDefinitions() {
        Map<Item, EnhancementDefinition> result = new IdentityHashMap<>();
        add(result, EnhancementProfile.SWORD, Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
                Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        add(result, EnhancementProfile.MACE, Items.MACE);
        add(result, EnhancementProfile.RANGED, Items.BOW, Items.CROSSBOW);
        add(result, EnhancementProfile.SPEAR, Items.TRIDENT);
        add(result, EnhancementProfile.ARMOR,
                Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
                Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                Items.TURTLE_HELMET);
        add(result, EnhancementProfile.TOOL,
                Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE,
                Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL,
                Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        add(result, EnhancementProfile.AXE, Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
                Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        add(result, EnhancementProfile.ELYTRA, Items.ELYTRA);
        return Map.copyOf(result);
    }

    private static void add(Map<Item, EnhancementDefinition> result, EnhancementProfile profile, Item... items) {
        EnhancementDefinition definition = definition(EnhancementCategory.VANILLA, profile);
        for (Item item : items) result.put(item, definition);
    }

    private static EnhancementDefinition definition(EnhancementCategory category, EnhancementProfile profile) {
        return new EnhancementDefinition(category, profile);
    }
}
