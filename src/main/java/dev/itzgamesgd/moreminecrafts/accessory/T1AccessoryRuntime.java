package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.ability.AbilityAttachments;
import dev.itzgamesgd.moreminecrafts.ability.AbilityCooldowns;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.mixin.ResearchEffectDurationAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Server-only timers. Durable cooldowns use the existing online-tick attachment. */
public final class T1AccessoryRuntime {
    public static final Identifier SPRINT_ID = ModIds.id("t1_sprint_acceleration");
    // Vanilla 26.3 gravity/drag; measured by the runtime physics acceptance test.
    public static final double JUMP_VELOCITY = 0.47D;
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final class State {
        int quietTicks, healTicks, sprintTicks;
    }
    private T1AccessoryRuntime() {}
    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }
    public static double stat(ServerPlayer player, AccessoryDefinition accessory, String name) {
        var stack = AccessoryAbilities.equipped(player, accessory);
        return stack.isEmpty() ? 0 : AccessoryEnhancementCatalog.valueAt(accessory, name,
                AccessoryEnhancementOperation.level(stack)).orElse(0);
    }
    public static void tick(ServerPlayer player) {
        if (!player.isAlive()) { clear(player); return; }
        State state = state(player);
        if (player.onGround()) player.removeAttached(EquipmentAttachments.T1_AIR_JUMP_USED);
        boolean recovery = !AccessoryAbilities.equipped(player, AccessoryDefinition.T1_SUSTAINED_RECOVERY).isEmpty();
        if (state.quietTicks < 200) {
            state.quietTicks++;
            if (state.quietTicks == 200 && recovery) player.heal(1);
        } else if (recovery && ++state.healTicks >= stat(player, AccessoryDefinition.T1_SUSTAINED_RECOVERY, "heal_interval_ticks")) {
            state.healTicks = 0;
            player.heal(1);
        }
        boolean sprint = player.isSprinting() && !AccessoryAbilities.equipped(player, AccessoryDefinition.T1_SPRINT_ACCELERATION).isEmpty();
        state.sprintTicks = sprint ? Math.min(100, state.sprintTicks + 1) : 0;
        double bonus = state.sprintTicks == 100 ? stat(player, AccessoryDefinition.T1_SPRINT_ACCELERATION, "speed_bonus_percent") / 100 : 0;
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            var modifier = speed.getModifier(SPRINT_ID);
            if (modifier != null && modifier.amount() != bonus) speed.removeModifier(SPRINT_ID);
            if (bonus > 0 && speed.getModifier(SPRINT_ID) == null)
                speed.addTransientModifier(new AttributeModifier(SPRINT_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        T1Shockwave.tryActivate(player);
    }
    public static void afterDamage(ServerPlayer player) {
        State state = state(player);
        state.quietTicks = state.healTicks = 0;
        T1Shockwave.tryActivate(player);
    }
    public static void equipmentChanged(ServerPlayer player, ItemStack previous, ItemStack next) {
        if (previous.getItem() == ModItems.ACCESSORIES.get(AccessoryDefinition.T1_COMBO)) T1ComboRuntime.clear(player);
        if (previous.getItem() == ModItems.ACCESSORIES.get(AccessoryDefinition.T1_SPRINT_ACCELERATION)) {
            state(player).sprintTicks = 0;
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(SPRINT_ID);
        }
        if (previous.getItem() == ModItems.ACCESSORIES.get(AccessoryDefinition.T1_SUSTAINED_RECOVERY)
                || next.getItem() == ModItems.ACCESSORIES.get(AccessoryDefinition.T1_SUSTAINED_RECOVERY)) {
            state(player).quietTicks = state(player).healTicks = 0;
        }
        T1Shockwave.tryActivate(player);
    }
    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
        T1ComboRuntime.clear(player);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPRINT_ID);
    }
    public static void onDeath(ServerPlayer player) {
        clear(player);
        player.removeAttached(EquipmentAttachments.T1_AIR_JUMP_USED);
        var cooldowns = player.getAttachedOrElse(AbilityAttachments.COOLDOWNS, AbilityCooldowns.EMPTY);
        for (var ability : new AccessoryDefinition[]{AccessoryDefinition.T1_CRISIS_ESCAPE,
                AccessoryDefinition.T1_HEAVY_BUFFER, AccessoryDefinition.T1_DOUBLE_JUMP})
            cooldowns = cooldowns.startOnline(AccessoryCooldownRuntime.id(ability), 0);
        player.setAttached(AbilityAttachments.COOLDOWNS, cooldowns);
    }
    /** Armor and protection have already run, but vanilla absorption and death have not. */
    public static float bufferDamage(ServerPlayer player, float amount) {
        double reduction = stat(player, AccessoryDefinition.T1_HEAVY_BUFFER, "damage_reduction_percent");
        if (reduction > 0 && player.isAlive() && amount >= player.getMaxHealth() * 0.4F
                && AccessoryCooldownRuntime.start(player, AccessoryDefinition.T1_HEAVY_BUFFER, 1200)) {
            effect(player, ParticleTypes.ENCHANTED_HIT, SoundEvents.SHIELD_BLOCK.value());
            return (float) (amount * (1 - reduction / 100));
        }
        return amount;
    }
    /** Never mutate a shared AoE input. Vanilla then merges this one incoming application. */
    public static MobEffectInstance shortenEffect(ServerPlayer player, MobEffectInstance incoming) {
        double reduction = stat(player, AccessoryDefinition.T1_STATUS_RESILIENCE, "duration_reduction_percent");
        var type = incoming.getEffect().value();
        if (reduction <= 0 || type.isInstantaneous() || type.getCategory() != MobEffectCategory.HARMFUL
                || incoming.getDuration() <= 0) return incoming;
        var copy = new MobEffectInstance(incoming);
        var access = (ResearchEffectDurationAccessor) (Object) copy;
        access.moreminecrafts$setDuration(Math.max(1, (int) Math.round(incoming.getDuration() * (1 - reduction / 100))));
        var hidden = ((ResearchEffectDurationAccessor) (Object) incoming).moreminecrafts$getHiddenEffect();
        access.moreminecrafts$setHiddenEffect(hidden == null ? null : shortenEffect(player, hidden));
        copy.copyBlendState(incoming);
        return copy;
    }
    public static boolean onJump(ServerPlayer player) {
        double cooldown = stat(player, AccessoryDefinition.T1_DOUBLE_JUMP, "cooldown_ticks");
        if (cooldown <= 0 || !player.isAlive() || player.onGround() || player.isSpectator()
                || player.getAbilities().flying || player.isInWater() || player.isInLava() || player.onClimbable()
                || player.isPassenger() || player.isFallFlying() || player.isSwimming() || player.isAutoSpinAttack()
                || player.getAttachedOrElse(EquipmentAttachments.T1_AIR_JUMP_USED, false)) return false;
        if (!AccessoryCooldownRuntime.start(player, AccessoryDefinition.T1_DOUBLE_JUMP, (long) cooldown)) return false;
        player.setAttached(EquipmentAttachments.T1_AIR_JUMP_USED, true);
        var movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, JUMP_VELOCITY, movement.z);
        player.fallDistance = 0;
        synchronizeMotion(player);
        effect(player, ParticleTypes.CLOUD, SoundEvents.PLAYER_SMALL_FALL);
        return true;
    }
    public static void synchronizeMotion(net.minecraft.world.entity.Entity entity) {
        entity.syncVelocity = true;
        if (entity instanceof ServerPlayer player)
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }
    static void effect(ServerPlayer player, net.minecraft.core.particles.SimpleParticleType particles,
                       net.minecraft.sounds.SoundEvent sound) {
        var level = (ServerLevel) player.level();
        level.sendParticles(particles, player.getX(), player.getY() + 0.5, player.getZ(), 10, 0.4, 0.3, 0.4, 0.03);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.6F, 1.0F);
    }
}
