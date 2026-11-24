package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementCatalog;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementProfile;
import dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentFamily;
import dev.itzgamesgd.moreminecrafts.fishing.RodEquipment;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Scans only the player's direct inventory (36 main, 4 armor, 1 offhand) for equipment-possession
 * quests. Nested containers (shulker boxes, bundles, backpacks), ender chests, and transient menus
 * are intentionally excluded.
 */
final class QuestInventoryScanner {
    private static final String T2_CORES_SET = "set.t2_cores";
    private static final String NETHERITE_TOOLS_SET = "set.netherite_tools";

    private QuestInventoryScanner() {}

    static ResearchProgressState scan(ServerPlayer player, ResearchProgressState state) {
        Inventory inventory = player.getInventory();
        int maxEnhance = 0;
        boolean weapon10 = false;
        boolean tool10 = false;
        boolean armor10 = false;
        EnumSet<EquipmentSlot> armor20Slots = EnumSet.noneOf(EquipmentSlot.class);
        boolean customEnchant = false;
        boolean moduleT2 = false;
        boolean tier3Rig = false;

        int size = Math.min(41, inventory.getContainerSize());
        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            int enhanceLevel = stack.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL, 0);
            if (enhanceLevel > maxEnhance) maxEnhance = enhanceLevel;
            if (enhanceLevel >= 10) {
                var def = EnhancementCatalog.definition(stack.getItem());
                if (def.isPresent()) {
                    EnhancementProfile profile = def.get().profile();
                    switch (profile) {
                        case SWORD, MACE, RANGED, SPEAR -> weapon10 = true;
                        case TOOL, AXE -> tool10 = true;
                        case ARMOR -> {
                            armor10 = true;
                            if (enhanceLevel >= 20) {
                                Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
                                if (equippable != null && isArmorSlot(equippable.slot())) {
                                    armor20Slots.add(equippable.slot());
                                }
                            }
                        }
                        default -> {}
                    }
                }
            }

            if (!customEnchant && T1Operations.enchantments(stack).keySet().stream()
                    .anyMatch(id -> id.startsWith("moreminecrafts:"))) {
                customEnchant = true;
            }

            Item item = stack.getItem();
            if (isT2Core(item)) {
                state = QuestRules.observeDistinct(state, T2_CORES_SET, BuiltInRegistries.ITEM.getKey(item).toString());
            }
            if (isNetheriteTool(item)) {
                state = QuestRules.observeDistinct(state, NETHERITE_TOOLS_SET, BuiltInRegistries.ITEM.getKey(item).toString());
            }

            RodEquipment rod = stack.get(ModDataComponents.ROD_EQUIPMENT);
            if (rod != null) {
                if (rod.tier(FishingEquipmentFamily.REEL) >= 2
                        || rod.tier(FishingEquipmentFamily.MINNOW) >= 2
                        || rod.tier(FishingEquipmentFamily.CRANKBAIT) >= 2) {
                    moduleT2 = true;
                }
                if (rod.tier(FishingEquipmentFamily.REEL) >= 3
                        && rod.tier(FishingEquipmentFamily.MINNOW) >= 3
                        && rod.tier(FishingEquipmentFamily.CRANKBAIT) >= 3) {
                    tier3Rig = true;
                }
            }
        }

        if (maxEnhance > 0) state = QuestRules.seedCounter(state, "max_enhance_level", maxEnhance);
        int trio = (weapon10 ? 1 : 0) + (tool10 ? 1 : 0) + (armor10 ? 1 : 0);
        if (trio > 0) state = QuestRules.seedCounter(state, "enhance_trio_simultaneous", trio);
        if (!armor20Slots.isEmpty()) {
            state = QuestRules.seedCounter(state, "enhance_20_armor_simultaneous", armor20Slots.size());
        }
        if (customEnchant) state = QuestRules.seedCounter(state, "custom_enchant_owned", 1);
        if (moduleT2) state = QuestRules.seedCounter(state, "fishing_module_t2", 1);
        if (tier3Rig) state = QuestRules.seedCounter(state, "fishing_tier3_rig", 1);
        return state;
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private static boolean isT2Core(Item item) {
        return item == ModItems.EYE_OF_TYPHOON
                || item == ModItems.HEART_OF_THUNDER
                || item == ModItems.BURNING_HEART
                || item == ModItems.HEART_OF_CURRENTS
                || item == ModItems.ANCIENT_HEART;
    }

    private static boolean isNetheriteTool(Item item) {
        return item == Items.NETHERITE_SWORD
                || item == Items.NETHERITE_PICKAXE
                || item == Items.NETHERITE_AXE
                || item == Items.NETHERITE_SHOVEL
                || item == Items.NETHERITE_HOE;
    }
}
