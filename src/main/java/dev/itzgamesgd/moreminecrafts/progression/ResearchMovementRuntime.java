package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.Vec3;

/** Three user-approved movement skills, sharing vanilla W/Space/Shift controls. */
public final class ResearchMovementRuntime {
    private static final Map<UUID, State> STATE = new HashMap<>();
    private static final int JUMP_GRACE_TICKS = 15;
    private static final int ROLL_GRACE_TICKS = 6;
    private static final int HANG_TICKS = 100;

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
        State state = STATE.computeIfAbsent(player.getUUID(), ignored -> new State());
        long now = player.level().getGameTime();
        boolean shift = player.getLastClientInput().shift();
        if (player.onGround()) {
            state.wallUntil = 0;
            state.rollUntil = 0;
            state.lastWallJump = null;
            state.hangingTicks = 0;
        } else {
            if (player.horizontalCollision) state.wallUntil = now + JUMP_GRACE_TICKS;
            if (KnowledgeRuntime.researched(player, "fall_roll")
                    && shift && !state.previousShift
                    && player.getDeltaMovement().y < -0.1
                    && player.fallDistance > 2.5f) {
                // The short landing window is validated when the damage event fires.
                state.rollUntil = now + ROLL_GRACE_TICKS;
            }
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

    /** Returns true when this jump-edge was consumed, so enchantments do not double-trigger. */
    public static boolean onJump(ServerPlayer player) {
        if (!KnowledgeRuntime.researched(player, "wall_jump") || player.onGround()) return false;
        State state = STATE.computeIfAbsent(player.getUUID(), ignored -> new State());
        long now = player.level().getGameTime();
        if (state.wallUntil < now) return false;
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
        if (state == null || state.rollUntil < player.level().getGameTime()) return false;
        state.rollUntil = 0;
        return true;
    }
}
