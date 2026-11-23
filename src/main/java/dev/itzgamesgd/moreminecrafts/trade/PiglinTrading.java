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
 * Opens the vanilla merchant screen on a Piglin. Vanilla gold-ingot barter is left untouched
 * (interaction with a gold ingot is never intercepted).
 */
public final class PiglinTrading {
    /** One active session per Piglin so stock counters cannot diverge between two open screens. */
    private static final java.util.Map<Piglin, PiglinMerchant> SESSIONS = new java.util.WeakHashMap<>();

    private PiglinTrading() {}

    public static void initialize() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                ModIds.id("piglin_trade"), new PiglinTradeCatalog.Listener());
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Piglin piglin)
                    || player.isSpectator() || isBarterInteraction(player.getItemInHand(hand)))
                return InteractionResult.PASS;
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            return tryOpen(serverPlayer, piglin) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
        });
    }

    /** Safeguard: an interaction carrying the vanilla barter item is never routed to structured trading. */
    static boolean isBarterInteraction(net.minecraft.world.item.ItemStack held) {
        return held.is(PiglinAi.BARTERING_ITEM);
    }

    /** Server-side gate shared by the interaction and tests. */
    public static boolean tryOpen(ServerPlayer player, Piglin piglin) {
        PiglinTradeCatalog.Loaded loaded = PiglinTradeCatalog.current();
        if (loaded.offers().isEmpty() || piglin.isBaby() || !piglin.isAlive()) return false;
        // Never interfere with an in-progress vanilla admire/barter sequence.
        if (isBarterInteraction(piglin.getOffhandItem())) return false;
        if (isBarterInteraction(player.getMainHandItem())) return false;
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
