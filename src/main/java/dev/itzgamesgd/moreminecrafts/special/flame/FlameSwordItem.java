package dev.itzgamesgd.moreminecrafts.special.flame;

import dev.itzgamesgd.moreminecrafts.ability.AbilityActivation;
import dev.itzgamesgd.moreminecrafts.special.ElementalHitProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.itzgamesgd.moreminecrafts.special.vfx.T2ElementalVfx;

public final class FlameSwordItem extends Item {
    public FlameSwordItem(Properties properties) { super(properties); }

    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player) {
            T2ElementalVfx.firePrimary(player.level(), player.getEyePosition(),
                    target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D));
            if (ElementalHitProgress.record(player, ElementalHitProgress.Weapon.FIRE)) {
                FlameSwordEffects.sweep(player, target);
            }
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
