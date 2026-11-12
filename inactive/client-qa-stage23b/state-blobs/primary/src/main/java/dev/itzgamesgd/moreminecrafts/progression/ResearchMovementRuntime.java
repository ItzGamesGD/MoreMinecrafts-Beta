package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.Vec3;

/** Three user-approved movement skills, sharing vanilla W/Space/Shift controls. */
public final class ResearchMovementRuntime {
    private static final Map<UUID, State> STATE = new HashMap<>();
    private static final int JUMP_GRACE_TICKS = 15;
    private static final int ROLL_GRACE_TICKS = 6;
    private static final int HANG_TICKS = 100;
    private static final net.minecraft.resources.Identifier ROOT_SPEED_ID =
            dev.itzgamesgd.moreminecrafts.core.ModIds.id("research_world_root_speed");

    private static final class State {
        long wallUntil, rollUntil;
        BlockPos lastWallJump;
        boolean previousShift;
        int hangingTicks;
    }

    private ResearchMovementRuntime() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(ResearchMovementRuntime::tick));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATE.remove(handler.player.getUUID()));
    }

    private static void tick(ServerPlayer player) {
        var movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            boolean speedActive = KnowledgeRuntime.researched(player, "world_root")
                    && player.onGround() && !player.isPassenger() && !player.isFallFlying();
            if (speedActive && movement.getModifier(ROOT_SPEED_ID) == null)
                movement.addTransientModifier(new AttributeModifier(ROOT_SPEED_ID, 0.01D,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            else if (!speedActive && movement.getModifier(ROOT_SPEED_ID) != null)
                movement.removeModifier(ROOT_SPEED_ID);
        }
        State state = STATE.computeIfAbsent(player.getUUID(), ignored -> new State());
        long now = player.level().getGameTime();
        boolean shift = player.getLastClientInput().shift();
        if (player.onGround()) {
            state.wallUntil = 0;
            state.rollUntil = 0;
            state.lastWallJump = null;
            state.hangingTicks = 0;
        } else {
            observeWallContact(player, player.horizontalCollision);
            observeFallRollInput(player, shift);
            if (KnowledgeRuntime.researched(player, "climbing")
                    && player.horizontalCollision && player.getLastClientInput().forward()
                    && player.getMainHandItem().is(ItemTags.PICKAXES)) {
                BlockPos wall = player.blockPosition().relative(player.getDirection());
                if (player.level().getBlockState(wall).is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                    var velocity = player.getDeltaMovement();
                    player.setDeltaMovement(velocity.x, 0.15D, velocity.z);
                    player.fallDistance = 0;
                    state.hangingTicks = 0;
                }
            } else if (KnowledgeRuntime.researched(player, "climbing")
                    && player.horizontalCollision && shift && state.hangingTicks < HANG_TICKS) {
                BlockPos edge = player.blockPosition().relative(player.getDirection()).above();
                if (player.level().getBlockState(edge).isSolidRender()) {
                    var velocity = player.getDeltaMovement();
                    player.setDeltaMovement(velocity.x * 0.25D, 0.0D, velocity.z * 0.25D);
                    player.fallDistance = 0;
                    state.hangingTicks++;
                }
            } else state.hangingTicks = 0;
        }
        state.previousShift = shift;
    }

    static void observeWallContact(ServerPlayer player, boolean touchingWall) {
        if (!touchingWall || player.onGround()) return;
        State state = STATE.computeIfAbsent(player.getUUID(), ignored -> new State());
        state.wallUntil = player.level().getGameTime() + JUMP_GRACE_TICKS;
    }

    static void observeFallRollInput(ServerPlayer player, boolean shiftPressed) {
        State state = STATE.computeIfAbsent(player.getUUID(), ignored -> new State());
        if (KnowledgeRuntime.researched(player, "fall_roll")
                && shiftPressed && !state.previousShift
                && player.getDeltaMovement().y < -0.1
                && player.fallDistance > 2.5f) {
            // The short landing window is validated when the damage event fires.
            state.rollUntil = player.level().getGameTime() + ROLL_GRACE_TICKS;
        }
        state.previousShift = shiftPressed;
    }

    /** Returns true when this jump-edge was consumed, so enchantments do not double-trigger. */
    public static boolean onJump(ServerPlayer player) {
        if (!KnowledgeRuntime.researched(player, "wall_jump") || player.onGround()) return false;
        State state = STATE.computeIfAbsent(player.getUUID(), ignored -> new State());
        long now = player.level().getGameTime();
        if (!ResearchMovementRules.inGraceWindow(now, state.wallUntil)) return false;
        BlockPos wall = player.blockPosition().relative(player.getDirection());
        if (wall.equals(state.lastWallJump)) return false;
        Vec3 horizontal = new Vec3(-player.getLookAngle().x, 0, -player.getLookAngle().z);
        if (horizontal.lengthSqr() < 0.01D) return false;
        Vec3 push = horizontal.normalize().scale(0.55);
        player.setDeltaMovement(push.x, 0.55D, push.z);
        player.fallDistance = 0;
        state.lastWallJump = wall.immutable();
        state.wallUntil = 0;
        return true;
    }

    /** Returns a single grace-window roll, no animation or added HUD. */
    public static boolean consumeFallRoll(ServerPlayer player) {
        if (!KnowledgeRuntime.researched(player, "fall_roll")) return false;
        State state = STATE.get(player.getUUID());
        if (state == null || !ResearchMovementRules.inGraceWindow(player.level().getGameTime(), state.rollUntil)) return false;
        state.rollUntil = 0;
        return true;
    }
}
