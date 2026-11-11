package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Primary melee only; a scope snapshots charge before vanilla resets its attack ticker. */
public final class T1ComboRuntime {
    private record Counter(int hits, long lastTick) {}
    private static final Map<UUID, Counter> COUNTERS = new HashMap<>();
    private static final ThreadLocal<Hit> ACTIVE_HIT = new ThreadLocal<>();
    public static final class Hit implements AutoCloseable {
        private final LivingEntity target;
        private final Hit previous;
        private boolean lostHp;
        private Hit(LivingEntity target) { this.target = target; previous = ACTIVE_HIT.get(); ACTIVE_HIT.set(this); }
        public boolean lostHp() { return lostHp; }
        @Override public void close() { if (previous == null) ACTIVE_HIT.remove(); else ACTIVE_HIT.set(previous); }
    }
    public static Hit observe(LivingEntity target) { return new Hit(target); }
    public static void healthChanging(LivingEntity target, float next) {
        Hit hit = ACTIVE_HIT.get();
        if (hit != null && hit.target == target && next < target.getHealth()) hit.lostHp = true;
    }
    private T1ComboRuntime() {}
    public static void clear(ServerPlayer player) { COUNTERS.remove(player.getUUID()); }

    public static Attack capture(ServerPlayer player) {
        if (AccessoryAbilities.equipped(player, AccessoryDefinition.T1_COMBO).isEmpty()) return new Attack(false, 0);
        var attack = new AttributeInstance(Attributes.ATTACK_DAMAGE, ignored -> {});
        attack.setBaseValue(player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        // Keep player/weapon attributes, remove all potion damage buffs/debuffs from the extra-hit base.
        var effectIds = new HashSet<net.minecraft.resources.Identifier>();
        player.getActiveEffects().forEach(effect -> effect.getEffect().value().createModifiers(effect.getAmplifier(),
                (attribute, modifier) -> { if (attribute.equals(Attributes.ATTACK_DAMAGE)) effectIds.add(modifier.id()); }));
        var current = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (current != null) current.getModifiers().stream().filter(modifier -> !effectIds.contains(modifier.id()))
                .forEach(attack::addTransientModifier);
        return new Attack(player.getAttackStrengthScale(0.5F) >= 0.9F, (float) attack.getValue());
    }
    public record Attack(boolean charged, float baseDamage) {}

    public static boolean eligible(ServerPlayer player, LivingEntity target, Attack attack) {
        return attack.charged() && !AccessoryAbilities.equipped(player, AccessoryDefinition.T1_COMBO).isEmpty()
                && T1CombatTargets.eligible(player, target, false);
    }
    private static int hits(ServerPlayer player) {
        Counter counter = COUNTERS.get(player.getUUID());
        long now = player.level().getGameTime();
        return counter == null || now - counter.lastTick() >= 80 ? 0 : counter.hits();
    }
    /** Side-effect-free vanilla mitigation helpers prevent a pending bonus from being spent on absorption. */
    public static boolean reachesHealth(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float damage) {
        if (target.getInvulnerableTime() > 10 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN)
                && damage <= ((dev.itzgamesgd.moreminecrafts.mixin.T1DamageStateAccessor) target).moreminecrafts$lastHurt()) return false;
        if (target.getAbsorptionAmount() <= 0) return true;
        float preview = damage;
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR))
            preview = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(target, preview, source,
                    target.getArmorValue(), (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS)) {
            var resistance = target.getEffect(net.minecraft.world.effect.MobEffects.RESISTANCE);
            if (resistance != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_RESISTANCE))
                preview = Math.max(0, preview * (25 - (resistance.getAmplifier() + 1) * 5) / 25);
            if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS))
                preview = net.minecraft.world.damagesource.CombatRules.getDamageAfterMagicAbsorb(preview,
                        net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageProtection(
                                (net.minecraft.server.level.ServerLevel) target.level(), target, source));
        }
        return preview > target.getAbsorptionAmount();
    }
    public static float extraDamage(ServerPlayer player, Attack attack) {
        return hits(player) == 3 ? (float) (attack.baseDamage()
                * T1AccessoryRuntime.stat(player, AccessoryDefinition.T1_COMBO, "extra_damage_percent") / 100) : 0;
    }
    public static void successfulHit(ServerPlayer player, boolean bonus) {
        int next = hits(player) + 1;
        COUNTERS.put(player.getUUID(), new Counter(next == 4 ? 0 : next, player.level().getGameTime()));
        if (bonus) T1AccessoryRuntime.effect(player, net.minecraft.core.particles.ParticleTypes.CRIT,
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT);
    }
}
