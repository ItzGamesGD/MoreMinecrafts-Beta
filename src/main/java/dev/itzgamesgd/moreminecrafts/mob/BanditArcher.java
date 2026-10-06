package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class BanditArcher extends Pillager {
    public BanditArcher(EntityType<? extends Pillager> type, Level level) { super(type, level); }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> goal instanceof RangedCrossbowAttackGoal<?>);
        goalSelector.addGoal(3, new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        ItemStack bow = getMainHandItem();
        Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), bow);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getY(0.3333333333333333) - arrow.getY() + Math.sqrt(dx * dx + dz * dz) * 0.2;
        arrow.shoot(dx, dy, dz, 1.6F, 1.0F);
        serverLevel.addFreshEntity(arrow);
    }
}
