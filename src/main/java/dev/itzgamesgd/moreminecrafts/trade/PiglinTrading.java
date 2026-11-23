package dev.itzgamesgd.moreminecrafts.trade;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;

/**
 * Opens the vanilla merchant screen on a Piglin for players who unlocked structured trading,
 * replacing right-click barter for them (villager-style). Dropped-gold attraction stays vanilla.
 */
public final class PiglinTrading {
    /** One active session per Piglin so stock counters cannot diverge between two open screens. */
    private static final java.util.Map<Piglin, PiglinMerchant> SESSIONS = new java.util.WeakHashMap<>();

    private PiglinTrading() {}

    public static void initialize() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                ModIds.id("piglin_trade"), new PiglinTradeCatalog.Listener());
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Piglin piglin) || player.isSpectator()
                    || level.isClientSide() || !(player instanceof ServerPlayer serverPlayer))
                return InteractionResult.PASS;
            return handleUse(serverPlayer, piglin);
        });
    }

    /**
     * Right-click handling. Players who have unlocked structured trading never start vanilla barter by
     * right-click (even holding the barter item): the click is consumed and opens the villager-style
     * trade when possible. Locked players fall through to untouched vanilla. Gold attraction
     * (dropped-item pickup/admiring) is a separate vanilla path and is never touched here.
     */
    static InteractionResult handleUse(ServerPlayer player, Piglin piglin) {
        if (!isUnlocked(player)) return InteractionResult.PASS;
        tryOpen(player, piglin);
        return InteractionResult.SUCCESS_SERVER;
    }

    static boolean isUnlocked(ServerPlayer player) {
        return PiglinTradeCatalog.current().config().unlocked(id -> KnowledgeRuntime.state(player).researchLevel(id));
    }

    static boolean isBarterItem(net.minecraft.world.item.ItemStack stack) {
        return stack.is(PiglinAi.BARTERING_ITEM);
    }

    /** Server-side gate shared by the interaction and tests. */
    public static boolean tryOpen(ServerPlayer player, Piglin piglin) {
        PiglinTradeCatalog.Loaded loaded = PiglinTradeCatalog.current();
        if (loaded.offers().isEmpty() || piglin.isBaby() || !piglin.isAlive()) return false;
        // Never interfere with an in-progress vanilla admire/barter sequence.
        if (isBarterItem(piglin.getOffhandItem())) return false;
        if (!loaded.config().unlocked(id -> KnowledgeRuntime.state(player).researchLevel(id))) return false;
        PiglinMerchant active = SESSIONS.get(piglin);
        if (active != null && active.getTradingPlayer() != null && active.getTradingPlayer() != player
                && !active.getTradingPlayer().isRemoved()) return false;
        PiglinMerchant merchant = new PiglinMerchant(piglin, loaded.offers());
        SESSIONS.put(piglin, merchant);
        merchant.setTradingPlayer(player);
        merchant.openTradingScreen(player, piglin.getDisplayName(), 1);
        return true;
    }
}
