package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.LingeringPotionItem;

/** Server event adapters for the existing persistent Knowledge condition pipeline. */
public final class KnowledgeEventCollectors {
    private static final Map<Piglin, UUID> PIGLIN_OFFERERS = new WeakHashMap<>();

    private KnowledgeEventCollectors() {}

    public static void smeltingCollected(ServerPlayer player, AbstractFurnaceBlockEntity furnace, ItemStack taken) {
        for (String oreType : SmeltingOutputProvenance.collect(furnace, taken))
            KnowledgeRuntime.recordDistinctCondition(player, "ore_types_smelted", oreType);
    }

    public static void shieldBlocked(ServerPlayer player) {
        KnowledgeRuntime.recordCondition(player, "shield_blocks",
                KnowledgeRuntime.state(player).get("condition.shield_blocks") + 1);
    }

    public static void specialAmmunitionUsed(ServerPlayer player, Projectile projectile) {
        if (KnowledgeEventRules.isSpecialAmmunition(projectile))
            KnowledgeRuntime.recordCondition(player, "special_ammunition_used", 1);
    }

    public static void rememberPiglinOffer(Piglin piglin, ServerPlayer player) {
        PIGLIN_OFFERERS.put(piglin, player.getUUID());
    }

    public static void completePiglinBarter(Piglin piglin) {
        UUID offerer = PIGLIN_OFFERERS.remove(piglin);
        ServerPlayer player = offerer == null || !(piglin.level() instanceof ServerLevel level)
                ? null : level.getServer().getPlayerList().getPlayer(offerer);
        if (player != null)
            KnowledgeRuntime.recordCondition(player, "piglin_barters",
                    KnowledgeRuntime.state(player).get("condition.piglin_barters") + 1);
    }

    public static void clearPiglinOffer(Piglin piglin) {
        PIGLIN_OFFERERS.remove(piglin);
    }

    public static void archaeologyItemProduced(ServerPlayer player, ItemStack result) {
        if (!result.isEmpty())
            KnowledgeRuntime.recordCondition(player, "archaeology_items_brushed",
                    KnowledgeRuntime.state(player).get("condition.archaeology_items_brushed") + result.getCount());
    }

    public static void potionCollected(ServerPlayer player, BrewingStandBlockEntity stand, int slot, ItemStack result) {
        if (result.isEmpty() || !isPotionItem(result)) return;
        int brewedCount = BrewingOutputProvenance.collect(stand, slot, result);
        if (brewedCount <= 0) return;

        KnowledgeRuntime.recordCondition(player, "potions_brewed",
                KnowledgeRuntime.state(player).get("condition.potions_brewed") + brewedCount);
        KnowledgeEventRules.potionType(result).ifPresent(type ->
                KnowledgeRuntime.recordDistinctCondition(player, "potion_types_brewed", type));
        if (result.getItem() instanceof SplashPotionItem)
            KnowledgeRuntime.recordCondition(player, "splash_potions_brewed", 1);
        if (result.getItem() instanceof LingeringPotionItem)
            KnowledgeRuntime.recordCondition(player, "lingering_potions_brewed", 1);
        if (KnowledgeEventRules.isStrengthenedPotion(result))
            KnowledgeRuntime.recordCondition(player, "strengthened_potions_brewed",
                    KnowledgeRuntime.state(player).get("condition.strengthened_potions_brewed") + brewedCount);
    }

    public static boolean isPotionItem(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }
}
