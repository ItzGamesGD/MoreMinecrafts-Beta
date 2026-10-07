package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.level.Level;

/** Cave-spider family variant with deliberately weaker poison than vanilla Cave Spider. */
public final class SwampCaveSpider extends CaveSpider {
    private static final int WEAK_POISON_TICKS = 60;

    public SwampCaveSpider(EntityType<? extends CaveSpider> type, Level level) { super(type, level); }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        MobEffectInstance previousPoison = target instanceof LivingEntity living
                ? deepCopy(level, living.getEffect(MobEffects.POISON)) : null;
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.removeEffect(MobEffects.POISON);
            if (previousPoison != null) living.addEffect(previousPoison);
            living.addEffect(new MobEffectInstance(MobEffects.POISON, WEAK_POISON_TICKS, 0), this);
        }
        return hit;
    }

    private static MobEffectInstance deepCopy(ServerLevel level, MobEffectInstance effect) {
        if (effect == null) return null;
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var encoded = MobEffectInstance.CODEC.encodeStart(ops, effect).getOrThrow();
        return MobEffectInstance.CODEC.parse(ops, encoded).getOrThrow();
    }
}
