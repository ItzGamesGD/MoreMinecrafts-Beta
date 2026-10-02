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
        var center = origin.add(direction.scale(LowerEquipmentRules.GREATSWORD_SWEEP_RANGE * 0.5D));
        var area = attacker.getBoundingBox().move(center.subtract(attacker.position()))
                .inflate(LowerEquipmentRules.GREATSWORD_SWEEP_RANGE * 0.5D, 1.0D,
                        LowerEquipmentRules.GREATSWORD_SWEEP_RANGE * 0.5D);
        LivingEntity primary = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> ElementalCombat.validTarget(attacker, candidate))) {
            var offset = target.getBoundingBox().getCenter().subtract(origin);
            double along = offset.dot(direction);
            double lateral = offset.subtract(direction.scale(along)).length();
            if (along >= 0 && along <= LowerEquipmentRules.GREATSWORD_SWEEP_RANGE && lateral <= 1.5D) {
                target.hurtServer((ServerLevel) level, level.damageSources().playerAttack(attacker),
                        (float) attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
                if (along < nearest) { nearest = along; primary = target; }
            }
        }
        if (primary != null) {
            attacker.resetAttackStrengthTicker();
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }
}
