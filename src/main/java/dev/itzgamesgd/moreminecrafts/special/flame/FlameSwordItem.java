package dev.itzgamesgd.moreminecrafts.special.flame;

import dev.itzgamesgd.moreminecrafts.ability.AbilityActivation;
import dev.itzgamesgd.moreminecrafts.special.SuccessfulHitCounter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FlameSwordItem extends Item {
    private static final int PASSIVE_HITS = 10;
    private static final Map<UUID, SuccessfulHitCounter> HITS = new HashMap<>();
    public FlameSwordItem(Properties properties) { super(properties); }

    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player) {
            player.level().sendParticles(ParticleTypes.SMALL_FLAME, target.getX(), target.getY()+.8, target.getZ(), 2,.1,.2,.1,.01);
            var advance=HITS.getOrDefault(player.getUUID(), SuccessfulHitCounter.EMPTY).recordSuccessfulHit(PASSIVE_HITS);
            HITS.put(player.getUUID(), advance.counter());
            if (advance.triggered()) FlameSwordEffects.splash(player,target);
        }
        super.hurtEnemy(stack,target,attacker);
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return player instanceof ServerPlayer server
                && AbilityActivation.tryActivate(server, player.getItemInHand(hand), FlameCleaveAbility.INSTANCE)==AbilityActivation.Result.ACTIVATED
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
}
