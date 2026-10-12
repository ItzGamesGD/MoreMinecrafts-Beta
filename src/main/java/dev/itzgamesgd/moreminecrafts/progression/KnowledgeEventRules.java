package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;

/** Pure identification rules shared by server gameplay event collectors. */
public final class KnowledgeEventRules {
    private static final Map<String, String> RAW_ORE_TYPES = Map.of(
            "minecraft:raw_iron", "iron",
            "minecraft:raw_copper", "copper",
            "minecraft:raw_gold", "gold",
            "minecraft:ancient_debris", "netherite",
            "moreminecrafts:raw_titanium", "titanium",
            "moreminecrafts:raw_tungsten", "tungsten",
            "moreminecrafts:adamantium_debris", "adamantium"
    );

    private KnowledgeEventRules() {}

    /** Returns the canonical ore family represented by a furnace input, if it is ore-derived. */
    public static Optional<String> oreType(ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        var itemId = BuiltInRegistries.ITEM.getKey(input.getItem());
        Optional<String> namedType = oreTypeFromId(itemId);
        if (namedType.isPresent()) return namedType;
        if (input.is(ItemTags.IRON_ORES)) return Optional.of("iron");
        if (input.is(ItemTags.COPPER_ORES)) return Optional.of("copper");
        if (input.is(ItemTags.GOLD_ORES)) return Optional.of("gold");
        if (input.is(ItemTags.COAL_ORES)) return Optional.of("coal");
        if (input.is(ItemTags.DIAMOND_ORES)) return Optional.of("diamond");
        if (input.is(ItemTags.REDSTONE_ORES)) return Optional.of("redstone");
        if (input.is(ItemTags.LAPIS_ORES)) return Optional.of("lapis");
        if (input.is(ItemTags.EMERALD_ORES)) return Optional.of("emerald");
        return Optional.empty();
    }

    public static Optional<String> oreTypeFromId(Identifier itemId) {
        String id = itemId.toString();
        String rawType = RAW_ORE_TYPES.get(id);
        if (rawType != null) return Optional.of(rawType);
        String path = itemId.getPath();
        if (path.equals("iron_ore") || path.equals("deepslate_iron_ore")) return Optional.of("iron");
        if (path.equals("copper_ore") || path.equals("deepslate_copper_ore")) return Optional.of("copper");
        if (path.equals("gold_ore") || path.equals("deepslate_gold_ore")) return Optional.of("gold");
        if (path.equals("coal_ore") || path.equals("deepslate_coal_ore")) return Optional.of("coal");
        if (path.equals("diamond_ore") || path.equals("deepslate_diamond_ore")) return Optional.of("diamond");
        if (path.equals("redstone_ore") || path.equals("deepslate_redstone_ore")) return Optional.of("redstone");
        if (path.equals("lapis_ore") || path.equals("deepslate_lapis_ore")) return Optional.of("lapis");
        if (path.equals("emerald_ore") || path.equals("deepslate_emerald_ore")) return Optional.of("emerald");
        if (path.equals("titanium_ore") || path.equals("deepslate_titanium_ore")) return Optional.of("titanium");
        if (path.equals("tungsten_ore") || path.equals("deepslate_tungsten_ore")) return Optional.of("tungsten");
        if (path.equals("mythril_ore") || path.equals("deepslate_mythril_ore")) return Optional.of("mythril");
        if (path.equals("etherium_ore") || path.equals("deepslate_etherium_ore")) return Optional.of("etherium");
        return Optional.empty();
    }

    /** Only actual projectile launches from bow/crossbow hooks call this rule. */
    public static boolean isSpecialAmmunition(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow)
            return isSpecialAmmunition(arrow.getPickupItemStackOrigin());
        return projectile instanceof FireworkRocketEntity rocket && isSpecialAmmunition(rocket.getItem());
    }

    /** Approved special ammunition: tipped arrows and crossbow-fired fireworks. */
    public static boolean isSpecialAmmunition(ItemStack ammunition) {
        return isSpecialAmmunitionId(BuiltInRegistries.ITEM.getKey(ammunition.getItem()));
    }

    public static boolean isSpecialAmmunitionId(Identifier itemId) {
        return itemId.equals(Identifier.parse("minecraft:tipped_arrow"))
                || itemId.equals(Identifier.parse("minecraft:firework_rocket"));
    }

    public static Optional<String> potionType(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return Optional.empty();
        return contents.potion().map(holder -> BuiltInRegistries.POTION.getKey(holder.value()).toString());
    }

    /** Vanilla and data-pack potion IDs named strong_* represent approved strengthened brews. */
    public static boolean isStrengthenedPotion(ItemStack stack) {
        return potionType(stack).map(KnowledgeEventRules::isStrengthenedPotionId)
                .orElse(false);
    }

    public static boolean isStrengthenedPotionId(String id) {
        if (id == null) return false;
        int separator = id.indexOf(':');
        return id.substring(separator + 1).startsWith("strong_");
    }
}
