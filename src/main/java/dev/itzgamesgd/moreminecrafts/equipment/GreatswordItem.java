package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.special.elemental.ElementalCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GreatswordItem extends BasicWeaponItem {
    public GreatswordItem(Properties properties) { super(Kind.GREATSWORD, properties); }

    @Override public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return sweep((ServerPlayer) user);
    }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                            LivingEntity target, InteractionHand hand) {
        // Client success stops startUseItem from also dispatching the air-use packet.
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;
        return sweep((ServerPlayer) user);
    }

    private InteractionResult sweep(ServerPlayer attacker) {
        Level level = attacker.level();
        var origin = attacker.getEyePosition();
        var direction = attacker.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        float strength = attacker.getAttackStrengthScale(0.5F);
        float damage = (float) (attacker.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                * LowerEquipmentRules.greatswordDamageMultiplier(strength));
        double reach = LowerEquipmentRules.GREATSWORD_REACH;
        var area = attacker.getBoundingBox().inflate(reach, 1.0D, reach);
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> ElementalCombat.validTarget(attacker, candidate)
                        && attacker.hasLineOfSight(candidate))) {
            var offset = target.getBoundingBox().getCenter().subtract(origin);
            if (LowerEquipmentRules.isWithinGreatswordSweep(offset, direction)) {
                target.hurtServer((ServerLevel) level, level.damageSources().playerAttack(attacker), damage);
                hit = true;
            }
        }
        if (hit) {
            attacker.resetAttackStrengthTicker();
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }
}
