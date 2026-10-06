package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.level.Level;

/** Cave-spider family variant whose approved identity explicitly has no poison. */
public final class SmallCaveSpider extends CaveSpider {
    public SmallCaveSpider(EntityType<? extends CaveSpider> type, Level level) { super(type, level); }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        var poison = target instanceof net.minecraft.world.entity.LivingEntity living
                ? living.getEffect(net.minecraft.world.effect.MobEffects.POISON) : null;
        boolean hit = super.doHurtTarget(level, target);
        if (target instanceof net.minecraft.world.entity.LivingEntity living) {
            living.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
            if (poison != null) living.addEffect(new net.minecraft.world.effect.MobEffectInstance(poison));
        }
        return hit;
    }
}
