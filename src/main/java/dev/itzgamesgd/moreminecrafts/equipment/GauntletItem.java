package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.special.elemental.ElementalCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GauntletItem extends BasicWeaponItem {
    public GauntletItem(Properties properties) { super(Kind.GAUNTLET, properties); }

    @Override public void hurtEnemy(ItemStack stack, LivingEntity directlyHit, LivingEntity attackerEntity) {
        if (!(attackerEntity instanceof ServerPlayer attacker) || !(attacker.level() instanceof ServerLevel level)) return;
        Vec3 forward = directlyHit.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() < 1.0E-6D) return;
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        AABB broad = directlyHit.getBoundingBox().inflate(LowerEquipmentRules.GAUNTLET_REGION_LENGTH,
                1.0D, LowerEquipmentRules.GAUNTLET_REGION_LENGTH);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, broad,
                candidate -> candidate != directlyHit && ElementalCombat.validTarget(attacker, candidate))) {
            Vec3 offset = target.position().subtract(directlyHit.position()).multiply(1, 0, 1);
            double longitudinal = offset.dot(forward);
            double lateral = Math.abs(offset.dot(right));
            if (longitudinal < 0 || longitudinal > LowerEquipmentRules.GAUNTLET_REGION_LENGTH
                    || lateral > LowerEquipmentRules.GAUNTLET_REGION_WIDTH / 2.0D) continue;
            target.hurtServer(level, level.damageSources().playerAttack(attacker),
                    LowerEquipmentRules.GAUNTLET_SECONDARY_DAMAGE);
            Vec3 push = forward.scale(LowerEquipmentRules.GAUNTLET_HORIZONTAL_PUSH);
            Vec3 velocity = target.getDeltaMovement();
            target.setDeltaMovement(push.x, velocity.y, push.z);
        }
    }
}
