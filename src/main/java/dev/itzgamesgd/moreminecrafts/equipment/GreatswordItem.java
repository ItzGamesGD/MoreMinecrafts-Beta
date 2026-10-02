package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.special.elemental.ElementalCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class GreatswordItem extends BasicWeaponItem {
    public GreatswordItem(Properties properties) { super(Kind.GREATSWORD, properties); }

    @Override public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var attacker = (ServerPlayer) user;
        var origin = attacker.getEyePosition();
        var direction = attacker.getLookAngle().normalize();
        float strength = attacker.getAttackStrengthScale(0.5F);
        float strengthSquared = strength * strength;
        float damage = (float) (attacker.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                * (0.2F + 0.8F * strengthSquared));
        double reach = LowerEquipmentRules.GREATSWORD_REACH;
        double lateralCoverage = LowerEquipmentRules.GREATSWORD_SWEEP_HALF_WIDTH;
        var area = attacker.getBoundingBox().inflate(reach, 1.0D, reach);
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> ElementalCombat.validTarget(attacker, candidate)
                        && attacker.hasLineOfSight(candidate))) {
            var offset = target.getBoundingBox().getCenter().subtract(origin);
            double along = offset.dot(direction);
            double lateral = offset.subtract(direction.scale(along)).length();
            if (along >= 0 && along <= reach && lateral <= lateralCoverage) {
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
