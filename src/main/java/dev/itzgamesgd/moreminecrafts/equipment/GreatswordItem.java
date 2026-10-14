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

    /** Shared entry point for the entity interaction hook and the normal item-use action. */
    public InteractionResult useSweep(Level level, Player user, InteractionHand hand) {
        return use(level, user, hand);
    }

    @Override public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var attacker = (ServerPlayer) user;
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
                var source = level.damageSources().playerAttack(attacker);
                float enchanted = net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(
                        (ServerLevel) level, attacker.getItemInHand(hand), target, source,
                        damage + (float) (attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                                * .035 * dev.itzgamesgd.moreminecrafts.progression.T1Operations.option(attacker.getItemInHand(hand), "sweeping_edge")));
                if (target.hurtServer((ServerLevel) level, source, enchanted)) {
                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(
                            (ServerLevel) level, target, source, attacker.getItemInHand(hand));
                    hit = true;
                }
            }
        }
        if (hit) {
            attacker.getItemInHand(hand).hurtAndBreak(1, attacker,
                    hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            attacker.resetAttackStrengthTicker();
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }
}
