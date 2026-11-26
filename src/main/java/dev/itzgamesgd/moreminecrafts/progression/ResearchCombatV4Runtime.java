package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.mixin.LivingEntityAttackAccessor;
import dev.itzgamesgd.moreminecrafts.special.elemental.ElementalCombat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server runtime for V4 combat & tool skills:
 * `shield_counter`, `falling_axe_smash`, `weapon_swap_handling`, and `automatic_tool_switch`.
 */
public final class ResearchCombatV4Runtime {
    private static final String FALLING_AXE_COOLDOWN_KEY = "research.cooldown.falling_axe_smash";
    private static final TagKey<Item> DAGGERS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("moreminecrafts", "daggers"));
    private static final TagKey<Item> GREATSWORDS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("moreminecrafts", "greatswords"));

    private static final class PlayerState {
        long shieldCounterWindowUntil;
        long shieldCounterCooldownUntil;
        long weaponSwapCooldownUntil;
        long autoToolSwitchCooldownUntil;
        int lastSelectedSlot = -1;
    }

    private static final Map<UUID, PlayerState> STATE = new HashMap<>();

    private ResearchCombatV4Runtime() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                STATE.remove(handler.player.getUUID()));
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer
                    && serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) {
                tryAutoToolSwitch(serverPlayer, world.getBlockState(pos));
            }
            return InteractionResult.PASS;
        });
    }

    private static void tickPlayer(ServerPlayer player) {
        // Tick down persisted falling_axe_smash cooldown so logout/restart cannot bypass it.
        ResearchProgressState progress = KnowledgeRuntime.state(player);
        int fallingCooldown = progress.get(FALLING_AXE_COOLDOWN_KEY);
        if (fallingCooldown > 0) {
            KnowledgeRuntime.updateState(player, progress.with(FALLING_AXE_COOLDOWN_KEY, fallingCooldown - 1));
        }

        // Track hotbar slot switches for weapon_swap_handling.
        PlayerState state = STATE.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        int currentSlot = player.getInventory().getSelectedSlot();
        if (state.lastSelectedSlot >= 0 && state.lastSelectedSlot != currentSlot) {
            onSlotSwitched(player, state);
        }
        state.lastSelectedSlot = currentSlot;
    }

    private static void onSlotSwitched(ServerPlayer player, PlayerState state) {
        if (!KnowledgeRuntime.researched(player, "weapon_swap_handling")) return;
        long now = player.level().getGameTime();
        if (!ResearchCombatV4Rules.isOffCooldown(now, state.weaponSwapCooldownUntil)) return;
        ItemStack held = player.getMainHandItem();
        if (!isMeleeWeapon(held)) return;

        LivingEntityAttackAccessor accessor = (LivingEntityAttackAccessor) player;
        int nextTicker = ResearchCombatV4Rules.weaponSwapPrefillTicker(
                accessor.moreminecrafts$getAttackStrengthTicker(),
                player.getCurrentItemAttackStrengthDelay());
        accessor.moreminecrafts$setAttackStrengthTicker(nextTicker);
        state.weaponSwapCooldownUntil = now + ResearchCombatV4Rules.WEAPON_SWAP_COOLDOWN_TICKS;
    }

    public static void onShieldBlocked(ServerPlayer player) {
        if (!KnowledgeRuntime.researched(player, "shield_counter")) return;
        long now = player.level().getGameTime();
        PlayerState state = STATE.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        if (!ResearchCombatV4Rules.isOffCooldown(now, state.shieldCounterCooldownUntil)) return;
        state.shieldCounterWindowUntil = now + ResearchCombatV4Rules.SHIELD_COUNTER_WINDOW_TICKS;
    }

    /** Consumes an active shield counter window on melee hit, returning bonus damage fraction (+0.20) and emitting shockwave. */
    public static double consumeShieldCounter(ServerPlayer attacker, LivingEntity primaryTarget) {
        if (!KnowledgeRuntime.researched(attacker, "shield_counter")) return 0.0D;
        PlayerState state = STATE.get(attacker.getUUID());
        if (state == null) return 0.0D;
        long now = attacker.level().getGameTime();
        if (!ResearchCombatV4Rules.isShieldCounterWindowActive(now, state.shieldCounterWindowUntil)) return 0.0D;

        state.shieldCounterWindowUntil = 0L;
        state.shieldCounterCooldownUntil = now + ResearchCombatV4Rules.SHIELD_COUNTER_COOLDOWN_TICKS;
        applyKnockbackShockwave(attacker, primaryTarget,
                ResearchCombatV4Rules.SHIELD_COUNTER_SHOCKWAVE_RADIUS,
                ResearchCombatV4Rules.SHIELD_COUNTER_KNOCKBACK, false);
        return ResearchCombatV4Rules.SHIELD_COUNTER_DAMAGE_BONUS;
    }

    /** Consumes falling_axe_smash on eligible airborne axe strike; persists 60-tick cooldown. */
    public static double consumeFallingAxeSmash(ServerPlayer attacker, LivingEntity primaryTarget) {
        boolean researched = KnowledgeRuntime.researched(attacker, "falling_axe_smash");
        boolean holdingAxe = attacker.getMainHandItem().is(ItemTags.AXES);
        ResearchProgressState progress = KnowledgeRuntime.state(attacker);
        int cooldown = progress.get(FALLING_AXE_COOLDOWN_KEY);
        if (!ResearchCombatV4Rules.isFallingAxeEligible(
                researched, holdingAxe, attacker.onGround(), (float) attacker.fallDistance, cooldown)) {
            return 0.0D;
        }
        KnowledgeRuntime.updateState(attacker,
                progress.with(FALLING_AXE_COOLDOWN_KEY, ResearchCombatV4Rules.FALLING_AXE_COOLDOWN_TICKS));
        disableShieldIfBlocking(primaryTarget);
        applyKnockbackShockwave(attacker, primaryTarget,
                ResearchCombatV4Rules.FALLING_AXE_SHOCKWAVE_RADIUS, 0.55D, true);
        return ResearchCombatV4Rules.FALLING_AXE_DAMAGE_BONUS;
    }

    public static void tryAutoToolSwitch(ServerPlayer player, net.minecraft.world.level.block.state.BlockState blockState) {
        if (!KnowledgeRuntime.researched(player, "automatic_tool_switch")) return;
        long now = player.level().getGameTime();
        PlayerState state = STATE.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
        if (!ResearchCombatV4Rules.isOffCooldown(now, state.autoToolSwitchCooldownUntil)) return;

        int currentSlot = player.getInventory().getSelectedSlot();
        float currentSpeed = player.getMainHandItem().getDestroySpeed(blockState);
        float[] speeds = new float[9];
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            speeds[slot] = stack.isEmpty() ? 1.0F : stack.getDestroySpeed(blockState);
        }
        int best = ResearchCombatV4Rules.selectBestHotbarTool(currentSpeed, speeds, currentSlot);
        if (best >= 0 && best != currentSlot) {
            player.getInventory().setSelectedSlot(best);
            state.lastSelectedSlot = best;
            state.autoToolSwitchCooldownUntil = now + ResearchCombatV4Rules.AUTO_TOOL_SWITCH_COOLDOWN_TICKS;
        }
    }

    private static void applyKnockbackShockwave(ServerPlayer attacker, LivingEntity centerTarget,
                                                double radius, double strength, boolean disableShields) {
        if (!(attacker.level() instanceof ServerLevel level)) return;
        AABB area = centerTarget.getBoundingBox().inflate(radius, 1.5D, radius);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> ElementalCombat.validTarget(attacker, candidate))) {
            Vec3 away = nearby.position().subtract(attacker.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() < 1.0E-6D) away = attacker.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() >= 1.0E-6D) {
                Vec3 dir = away.normalize();
                nearby.knockback(strength, -dir.x, -dir.z, attacker.damageSources().playerAttack(attacker), 0);
            }
            if (disableShields) disableShieldIfBlocking(nearby);
        }
    }

    private static void disableShieldIfBlocking(LivingEntity target) {
        if (target instanceof ServerPlayer targetPlayer && targetPlayer.isBlocking()) {
            targetPlayer.getCooldowns().addCooldown(new ItemStack(Items.SHIELD),
                    ResearchCombatV4Rules.FALLING_AXE_SHIELD_DISABLE_TICKS);
            targetPlayer.stopUsingItem();
        }
    }

    private static boolean isMeleeWeapon(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                || stack.is(DAGGERS) || stack.is(GREATSWORDS));
    }
}
