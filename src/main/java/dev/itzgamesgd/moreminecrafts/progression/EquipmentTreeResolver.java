package dev.itzgamesgd.moreminecrafts.progression;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class EquipmentTreeResolver {
    public static EnchantmentTrees.Equipment resolve(ItemStack stack) {
        if(stack.isEmpty() || !stack.isDamageableItem() && !(stack.getItem() instanceof dev.itzgamesgd.moreminecrafts.equipment.BasicWeaponItem))return EnchantmentTrees.Equipment.NONE;
        String path=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if(path.endsWith("greatsword")||path.endsWith("dagger")||path.endsWith("sword"))return EnchantmentTrees.Equipment.SWORD;
        if(path.endsWith("pickaxe"))return EnchantmentTrees.Equipment.PICKAXE;
        if(path.endsWith("axe"))return EnchantmentTrees.Equipment.AXE;
        if(path.endsWith("shovel"))return EnchantmentTrees.Equipment.SHOVEL;
        if(path.endsWith("hoe"))return EnchantmentTrees.Equipment.HOE;
        if(path.endsWith("spear"))return EnchantmentTrees.Equipment.SPEAR;
        if(path.endsWith("crossbow"))return EnchantmentTrees.Equipment.CROSSBOW;
        if(path.endsWith("bow"))return EnchantmentTrees.Equipment.BOW;
        if(path.endsWith("helmet"))return EnchantmentTrees.Equipment.HELMET;
        if(path.endsWith("chestplate"))return EnchantmentTrees.Equipment.CHESTPLATE;
        if(path.endsWith("leggings"))return EnchantmentTrees.Equipment.LEGGINGS;
        if(path.endsWith("boots"))return EnchantmentTrees.Equipment.BOOTS;
        return switch(path){case "mace"->EnchantmentTrees.Equipment.MACE;case "trident"->EnchantmentTrees.Equipment.TRIDENT;
            case "elytra"->EnchantmentTrees.Equipment.ELYTRA;case "shield"->EnchantmentTrees.Equipment.SHIELD;
            case "fishing_rod"->EnchantmentTrees.Equipment.FISHING_ROD;case "gauntlet"->EnchantmentTrees.Equipment.GAUNTLET;default->EnchantmentTrees.Equipment.NONE;};
    }
    private EquipmentTreeResolver(){}
}
