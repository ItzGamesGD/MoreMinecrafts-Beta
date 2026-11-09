package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** A successful sword hit advances the five-stage sweep bonus for five seconds. */
public final class ResearchConsecutiveSweep {
    private record Chain(int stage, long lastHitTick) {}
    private static final Map<ServerPlayer, Chain> CHAINS = new WeakHashMap<>();

    private ResearchConsecutiveSweep() {}

    public static void recordHit(ServerPlayer player, Entity target) {
        if (!(target instanceof LivingEntity) || !player.getMainHandItem().is(ItemTags.SWORDS)
                || ResearchBatch2Runtime.level(player, "v3.research.33") == 0) return;
        long now = player.level().getGameTime();
        Chain prior = CHAINS.get(player);
        int stage = ResearchRemainingRules.nextSwordStage(prior == null ? 0 : prior.stage(),
                now, prior == null ? Long.MIN_VALUE : prior.lastHitTick());
        CHAINS.put(player, new Chain(stage, now));
    }

    public static int currentStage(ServerPlayer player) {
        if (!player.getMainHandItem().is(ItemTags.SWORDS)
                || ResearchBatch2Runtime.level(player, "v3.research.33") == 0) return 0;
        Chain chain = CHAINS.get(player);
        if (chain == null) return 0;
        long now = player.level().getGameTime();
        return now >= chain.lastHitTick() && now - chain.lastHitTick() <= 100 ? chain.stage() : 0;
    }

    public static float sweepDamage(ServerPlayer player, float vanillaDamage) {
        return (float) (vanillaDamage * ResearchRemainingRules.sweepMultiplier(currentStage(player)));
    }
}
