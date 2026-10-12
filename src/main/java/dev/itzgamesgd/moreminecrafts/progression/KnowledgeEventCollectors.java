package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.world.level.block.Blocks;
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
    private static final Map<Projectile, ProjectileWeaponOrigin> KNOWLEDGE_WEAPON_ORIGINS = new WeakHashMap<>();
    private record ProjectileWeaponOrigin(UUID player, String condition) {}

    private KnowledgeEventCollectors() {}

    public static void blastFurnaceProductTaken(ServerPlayer player, AbstractFurnaceBlockEntity furnace, ItemStack taken) {
        if (!taken.isEmpty() && furnace.getBlockState().is(Blocks.BLAST_FURNACE)
                && player.getStats().getValue(net.minecraft.stats.Stats.ITEM_CRAFTED.get(Items.BLAST_FURNACE)) > 0)
            KnowledgeRuntime.recordCondition(player, "furnace_blast_furnace_used", 1);
    }

    public static void blockPlaced(ServerPlayer player, ItemStack placed) {
        if (placed.is(Items.HOPPER)) {
            int current = KnowledgeRuntime.state(player).conditionProgress("hoppers_placed");
            KnowledgeRuntime.recordCondition(player, "hoppers_placed", current + 1);
        }
        if (isEligibleRedstoneComponent(placed)) {
            int current = KnowledgeRuntime.state(player).conditionProgress("redstone_components_placed");
            KnowledgeRuntime.recordCondition(player, "redstone_components_placed", current + 1);
        }
    }

    public static boolean isEligibleRedstoneComponent(ItemStack stack) {
        return stack.is(Items.REDSTONE) || stack.is(Items.REPEATER) || stack.is(Items.COMPARATOR)
                || stack.is(Items.OBSERVER) || stack.is(Items.PISTON) || stack.is(Items.STICKY_PISTON);
    }

    public static void successfulFarmlandCultivation(ServerPlayer player) {
        int current = KnowledgeRuntime.state(player).conditionProgress("farmland_tilled");
        KnowledgeRuntime.recordCondition(player, "farmland_tilled", current + 1);
    }

    public static void successfulFarmlandIrrigation(ServerPlayer player) {
        KnowledgeRuntime.recordCondition(player, "farmland_watered", 1);
    }

    public static void rememberProjectileWeapon(ServerPlayer player, Projectile projectile, boolean crossbow) {
        KNOWLEDGE_WEAPON_ORIGINS.put(projectile, new ProjectileWeaponOrigin(player.getUUID(),
                crossbow ? "crossbow_hostile_kills" : "bow_hostile_kills"));
    }

    public static void projectileKill(Projectile projectile) {
        ProjectileWeaponOrigin origin = KNOWLEDGE_WEAPON_ORIGINS.get(projectile);
        if (origin == null || !(projectile.level() instanceof ServerLevel level)) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(origin.player());
        if (player != null)
            KnowledgeRuntime.recordCondition(player, origin.condition(),
                    KnowledgeRuntime.state(player).conditionProgress(origin.condition()) + 1);
    }

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
