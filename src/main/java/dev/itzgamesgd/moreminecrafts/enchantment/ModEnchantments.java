package dev.itzgamesgd.moreminecrafts.enchantment;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/** Resource keys and safe dynamic-registry access for the Stage 17 enchantments. */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> MULTI_SHOT = key("multi_shot");
    public static final ResourceKey<Enchantment> CROSSBOW_BARRAGE = key("crossbow_barrage");
    public static final ResourceKey<Enchantment> VEIN_MINING = key("vein_mining");
    public static final ResourceKey<Enchantment> AUTO_REPLANT = key("auto_replant");
    public static final ResourceKey<Enchantment> AUTO_SMELT = key("auto_smelt");
    public static final ResourceKey<Enchantment> LUMBERJACK = key("lumberjack");
    public static final ResourceKey<Enchantment> ELYTRA_LAUNCH = key("elytra_launch");
    public static final ResourceKey<Enchantment> EXPLOSIVE_MACE = key("explosive_mace");
    public static final ResourceKey<Enchantment> FEATHER_BOOTS = key("feather_boots");
    public static final ResourceKey<Enchantment> JET_BOOTS = key("jet_boots");

    private ModEnchantments() {}

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ModIds.id(path));
    }

    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        Holder.Reference<Enchantment> enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }
}
