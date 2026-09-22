package dev.itzgamesgd.moreminecrafts.enhancement;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/** Explicit Item-identity associations for the equipment supported by the final Paper behavior. */
public final class EnhancementCatalog {
    private static final Map<Item, EnhancementDefinition> DEFINITIONS = createDefinitions();

    private EnhancementCatalog() {
    }

    public static Optional<EnhancementDefinition> definition(Item item) {
        return Optional.ofNullable(DEFINITIONS.get(item));
    }

    private static Map<Item, EnhancementDefinition> createDefinitions() {
        Map<Item, EnhancementDefinition> definitions = new IdentityHashMap<>();
        add(definitions, EnhancementProfile.SWORD, Items.WOODEN_SWORD, Items.STONE_SWORD,
                Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
        add(definitions, EnhancementProfile.MELEE, Items.MACE);
        add(definitions, EnhancementProfile.RANGED, Items.BOW, Items.CROSSBOW);
        add(definitions, EnhancementProfile.SPEAR, Items.TRIDENT);
        add(definitions, EnhancementProfile.ARMOR,
                Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
                Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
                Items.TURTLE_HELMET);
        add(definitions, EnhancementProfile.TOOL,
                Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE,
                Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL,
                Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
        add(definitions, EnhancementProfile.AXE,
                Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        add(definitions, EnhancementProfile.ELYTRA, Items.ELYTRA);
        return Map.copyOf(definitions);
    }

    private static void add(Map<Item, EnhancementDefinition> definitions, EnhancementProfile profile, Item... items) {
        EnhancementDefinition definition = new EnhancementDefinition(EnhancementCategory.VANILLA, profile);
        for (Item item : items) {
            definitions.put(item, definition);
        }
    }
}
