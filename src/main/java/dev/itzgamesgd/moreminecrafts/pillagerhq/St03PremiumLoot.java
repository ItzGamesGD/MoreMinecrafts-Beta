package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryDefinition;
import dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Approved Option A conditional premium loot selection for ST-03:
 * - Manor (65% once per HQ): 55% Equipment, 30% Combat Accessory, 15% Rune (reweighted if runes absent)
 * - Armory (35% once per HQ): 75% Equipment, 15% Combat Accessory, 10% Rune
 * - Commander (10% once on unique death): 50% Combat Accessory, 40% High-Tier Equipment, 10% Rune
 */
public final class St03PremiumLoot {
    private St03PremiumLoot() {}

    public static final double MANOR_CHANCE = 0.65D;
    public static final double ARMORY_CHANCE = 0.35D;
    public static final double COMMANDER_CHANCE = 0.10D;

    public enum Source {
        MANOR(MANOR_CHANCE, 55, 30, 15),
        ARMORY(ARMORY_CHANCE, 75, 15, 10),
        COMMANDER(COMMANDER_CHANCE, 40, 50, 10);

        public final double triggerChance;
        public final int equipmentWeight;
        public final int accessoryWeight;
        public final int runeWeight;

        Source(double triggerChance, int equipmentWeight, int accessoryWeight, int runeWeight) {
            this.triggerChance = triggerChance;
            this.equipmentWeight = equipmentWeight;
            this.accessoryWeight = accessoryWeight;
            this.runeWeight = runeWeight;
        }
    }

    public enum Category {
        EQUIPMENT,
        ACCESSORY,
        RUNE
    }

    public static final List<AccessoryDefinition> COMBAT_ACCESSORIES = List.of(
            AccessoryDefinition.STAR_COVENANT,
            AccessoryDefinition.PHANTOM_MIRROR,
            AccessoryDefinition.RING_OF_TIME,
            AccessoryDefinition.BLOOD_COVENANT,
            AccessoryDefinition.HEART_OF_GRAVITY,
            AccessoryDefinition.SOUL_CHAIN,
            AccessoryDefinition.EMBER_RING,
            AccessoryDefinition.RING_OF_LIFE,
            AccessoryDefinition.T1_COMBO,
            AccessoryDefinition.T1_CRISIS_ESCAPE,
            AccessoryDefinition.T1_HEAVY_BUFFER,
            AccessoryDefinition.T1_STATUS_RESILIENCE
    );

    public static Optional<Category> rollCategory(Source source, boolean runesAvailable,
                                                  DoubleSupplier chanceRoll, IntUnaryOperator weightRoll) {
        if (!(chanceRoll.getAsDouble() < source.triggerChance)) {
            return Optional.empty();
        }
        return Optional.of(pickCategory(source, runesAvailable, weightRoll));
    }

    public static Category pickCategory(Source source, boolean runesAvailable, IntUnaryOperator weightRoll) {
        int eqW = source.equipmentWeight;
        int accW = source.accessoryWeight;
        int runeW = runesAvailable ? source.runeWeight : 0;
        int total = eqW + accW + runeW;
        int r = Math.floorMod(weightRoll.applyAsInt(total), total);
        if (r < eqW) {
            return Category.EQUIPMENT;
        }
        if (r < eqW + accW) {
            return Category.ACCESSORY;
        }
        return Category.RUNE;
    }

    public static ItemStack createPremiumStack(Source source, IntUnaryOperator pickRoll) {
        // Runes are currently unfinalized/not registered in this baseline -> renormalize over Equipment & Accessory.
        Category cat = pickCategory(source, false, pickRoll);
        if (cat == Category.ACCESSORY) {
            AccessoryDefinition def = COMBAT_ACCESSORIES.get(Math.floorMod(pickRoll.applyAsInt(COMBAT_ACCESSORIES.size()), COMBAT_ACCESSORIES.size()));
            Item item = ModItems.ACCESSORIES.get(def);
            if (item != null) {
                return new ItemStack(item);
            }
        }
        return createEquipmentStack(source, pickRoll);
    }

    private static ItemStack createEquipmentStack(Source source, IntUnaryOperator pickRoll) {
        List<Item> pool = switch (source) {
            case COMMANDER -> List.of(
                    ModItems.DAGGERS.getOrDefault(WeaponBalance.Tier.DIAMOND, Items.DIAMOND_SWORD),
                    ModItems.GREATSWORDS.getOrDefault(WeaponBalance.Tier.DIAMOND, Items.DIAMOND_AXE),
                    ModItems.GAUNTLET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_AXE
            );
            case MANOR, ARMORY -> List.of(
                    ModItems.DAGGERS.getOrDefault(WeaponBalance.Tier.DIAMOND, Items.DIAMOND_SWORD),
                    ModItems.GREATSWORDS.getOrDefault(WeaponBalance.Tier.DIAMOND, Items.DIAMOND_AXE),
                    ModItems.DAGGERS.getOrDefault(WeaponBalance.Tier.IRON, Items.IRON_SWORD),
                    ModItems.GREATSWORDS.getOrDefault(WeaponBalance.Tier.IRON, Items.IRON_AXE),
                    ModItems.GAUNTLET,
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_BOOTS
            );
        };
        Item chosen = pool.get(Math.floorMod(pickRoll.applyAsInt(pool.size()), pool.size()));
        return new ItemStack(chosen != null ? chosen : Items.DIAMOND_AXE);
    }
}
