package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 풍화된 좀비 / Weathered Zombie (group B, §5 #1): vanilla Zombie AI, worn look, some leather/chain gear.
 *
 * <p>Equipment keeps the vanilla armor <em>probability</em> roll and only remaps the rolled material (PROVISIONAL):
 * leather and gold rolls become leather, every heavier roll becomes chainmail. Weapons stay vanilla. The drowning
 * conversion to a Drowned is disabled (required policy).
 */
public class WeatheredZombie extends Zombie {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET};

    public WeatheredZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    /** Required policy: never starts the underwater Drowned conversion. */
    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        for (EquipmentSlot slot : ARMOR) {
            ItemStack rolled = getItemBySlot(slot);
            if (rolled.isEmpty()) continue;
            Item weathered = weatheredArmor(slot, rolled.getItem());
            if (weathered != rolled.getItem()) setItemSlot(slot, new ItemStack(weathered));
        }
    }

    /** PROVISIONAL material remap; pure so it can be unit-checked. */
    public static Item weatheredArmor(EquipmentSlot slot, Item rolled) {
        boolean light = rolled == Items.LEATHER_HELMET || rolled == Items.LEATHER_CHESTPLATE
                || rolled == Items.LEATHER_LEGGINGS || rolled == Items.LEATHER_BOOTS
                || rolled == Items.GOLDEN_HELMET || rolled == Items.GOLDEN_CHESTPLATE
                || rolled == Items.GOLDEN_LEGGINGS || rolled == Items.GOLDEN_BOOTS;
        return switch (slot) {
            case HEAD -> light ? Items.LEATHER_HELMET : Items.CHAINMAIL_HELMET;
            case CHEST -> light ? Items.LEATHER_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE;
            case LEGS -> light ? Items.LEATHER_LEGGINGS : Items.CHAINMAIL_LEGGINGS;
            case FEET -> light ? Items.LEATHER_BOOTS : Items.CHAINMAIL_BOOTS;
            default -> rolled;
        };
    }
}
