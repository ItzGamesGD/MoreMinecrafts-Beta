package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class CaveBatEntity extends Bat {
    private int attackCooldown;

    public CaveBatEntity(EntityType<? extends Bat> type, Level level) { super(type, level); }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || getTarget() == null || !getTarget().isAlive()) return;
        if (attackCooldown > 0) attackCooldown--;
        Vec3 offset = getTarget().position().subtract(position());
        if (offset.lengthSqr() < 2.25) {
            if (attackCooldown == 0) {
                doHurtTarget((ServerLevel) level(), getTarget());
                attackCooldown = 30;
            }
        } else {
            setDeltaMovement(getDeltaMovement().add(offset.normalize().scale(0.035)));
        }
    }
}
