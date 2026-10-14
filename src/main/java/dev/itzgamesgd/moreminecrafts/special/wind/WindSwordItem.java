package dev.itzgamesgd.moreminecrafts.special.wind;

import dev.itzgamesgd.moreminecrafts.ability.AbilityActivation;
import dev.itzgamesgd.moreminecrafts.special.ElementalHitProgress;
import dev.itzgamesgd.moreminecrafts.special.vfx.T2ElementalVfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class WindSwordItem extends Item {
    public WindSwordItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity primary, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player) {
            T2ElementalVfx.windHit(player.level(), primary.position().add(0.0D, 0.8D, 0.0D),
                    player.getLookAngle());
            if (ElementalHitProgress.record(player.getUUID(), ElementalHitProgress.Weapon.WIND)) {
                sweep(player, primary);
            }
        }
        super.hurtEnemy(stack, primary, attacker);
    }

    private static void sweep(ServerPlayer player, LivingEntity primary) {
        ServerLevel level = player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, primary.getBoundingBox().inflate(3),
                entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player)
                        && (!(entity instanceof ServerPlayer other) || player.canHarmPlayer(other)))) {
            if (target.hurtServer(level, player.damageSources().playerAttack(player), 3.0F)) {
                T2ElementalVfx.windHit(level, target.position().add(0.0D, 0.8D, 0.0D), player.getLookAngle());
            }
        }
        T2ElementalVfx.windSweep(level, primary.position().add(0.0D, 0.8D, 0.0D), player.getLookAngle());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return player instanceof ServerPlayer server
                && AbilityActivation.tryActivate(server, player.getItemInHand(hand), WindBladeAbility.INSTANCE)
                == AbilityActivation.Result.ACTIVATED
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }
}
