package dev.itzgamesgd.moreminecrafts.trade;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Opens the vanilla merchant screen on a Piglin for players who unlocked structured trading. For those
 * players the Piglin's right-click barter is replaced (both hands are consumed). Dropped-gold attraction
 * stays vanilla.
 */
public final class PiglinTrading {
    /**
     * Currently open structured trades. Only trades still held by a player are kept: vanilla clears the
     * trading player when the menu closes, and {@link #prune()} then drops the entry. No map keys are
     * held, so retired Piglins are never retained.
     */
    private static final List<PiglinMerchant> OPEN = new ArrayList<>();

    private PiglinTrading() {}

    public static void initialize() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                ModIds.id("piglin_trade"), new PiglinTradeCatalog.Listener());
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(entity instanceof Piglin piglin) || player.isSpectator() || level.isClientSide()
                    || !(player instanceof ServerPlayer serverPlayer))
                return InteractionResult.PASS;
            return interact(serverPlayer, piglin, hand);
        });
    }

    /**
     * Unlocked players consume both hands, so vanilla barter cannot start from either hand. Only the
     * main-hand click opens the trade, so one right-click (which the client sends for both hands) cannot
     * open two menus. Locked players fall through to vanilla.
     */
    static InteractionResult interact(ServerPlayer player, Piglin piglin, InteractionHand hand) {
        if (!isUnlocked(player)) return InteractionResult.PASS;
        if (hand == InteractionHand.MAIN_HAND) tryOpen(player, piglin);
        return InteractionResult.SUCCESS_SERVER;
    }

    static InteractionResult handleUse(ServerPlayer player, Piglin piglin) {
        return interact(player, piglin, InteractionHand.MAIN_HAND);
    }

    static boolean isUnlocked(ServerPlayer player) {
        return PiglinTradeCatalog.config().unlocked(id -> KnowledgeRuntime.state(player).researchLevel(id));
    }

    static boolean isBarterItem(ItemStack stack) {
        return stack.is(PiglinAi.BARTERING_ITEM);
    }

    /** Server-side gate shared with the interaction and tests. */
    public static boolean tryOpen(ServerPlayer player, Piglin piglin) {
        PiglinTradeCatalog.Loaded loaded = PiglinTradeCatalog.current(player.level().registryAccess());
        if (loaded.offers().isEmpty() || piglin.isBaby() || !piglin.isAlive()) return false;
        // Never interfere with an in-progress vanilla admire/barter sequence.
        if (isBarterItem(piglin.getOffhandItem())) return false;
        if (!isUnlocked(player)) return false;

        prune();
        for (PiglinMerchant open : OPEN) {
            if (open.piglin() == piglin && open.getTradingPlayer() != null && open.getTradingPlayer() != player)
                return false;
        }
        OPEN.removeIf(open -> open.piglin() == piglin && open.getTradingPlayer() == player);

        var overworld = player.level().getServer().overworld();
        PiglinTradeStock stock = PiglinTradeStock.get(overworld);
        Map<String, Integer> maxUses = new HashMap<>();
        loaded.offers().forEach((id, entry) -> maxUses.put(id.toString(), entry.offer().getMaxUses()));
        if (stock.restockIfDue(overworld.getGameTime(), maxUses)) syncOpenTrades(null);

        PiglinMerchant merchant = new PiglinMerchant(piglin, loaded.offers(),
                id -> KnowledgeRuntime.state(player).researchLevel(id), stock);
        OPEN.add(merchant);
        merchant.setTradingPlayer(player);
        merchant.openTradingScreen(player, piglin.getDisplayName(), 1);
        return true;
    }

    /**
     * After a shared-stock change, re-sync every other open trade and refresh its result slot, so a stale
     * menu cannot hand out an item that another player has just sold out.
     */
    static void syncOpenTrades(PiglinMerchant except) {
        prune();
        for (PiglinMerchant open : OPEN) {
            if (open == except) continue;
            open.syncFromStock();
            if (open.getTradingPlayer() instanceof ServerPlayer player
                    && player.containerMenu instanceof MerchantMenu menu) {
                menu.updateSellItem();
            }
        }
    }

    private static void prune() {
        OPEN.removeIf(open -> open.getTradingPlayer() == null
                || open.piglin().isRemoved() || !open.piglin().isAlive());
    }

    /** Test hook: number of open structured trades. */
    static int openCount() {
        prune();
        return OPEN.size();
    }
}
