package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Operator-only test interface so the ST-05 controller can be exercised without the natural colosseum structure.
 * {@code testfloor} builds a flat lit platform; {@code register} binds an arena at a center; walking into the center
 * then starts R1 exactly as the structure would.
 */
final class ColosseumDebugCommand {
    private ColosseumDebugCommand() {}

    static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("mmcolosseum")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("testfloor").then(center().executes(ColosseumDebugCommand::testFloor)))
                        .then(Commands.literal("register").then(center().executes(ColosseumDebugCommand::register)))
                        .then(Commands.literal("unregister").then(center().executes(c -> report(c, "unregister",
                                ColosseumIntegration.unregisterArena(c.getSource().getLevel(), pos(c))))))
                        .then(Commands.literal("status").then(center().executes(ColosseumDebugCommand::status)))
                        .then(Commands.literal("reset").then(center().executes(c -> report(c, "reset",
                                ColosseumIntegration.forceReset(c.getSource().getLevel(), pos(c))))))
                        .then(Commands.literal("clearcooldown").then(center().executes(c -> report(c, "clearcooldown",
                                ColosseumIntegration.clearCooldown(c.getSource().getLevel(), pos(c))))))
                        .then(Commands.literal("advance").then(center().then(Commands.argument("ticks",
                                        IntegerArgumentType.integer(1, 100_000)).executes(c -> {
                                    ColosseumIntegration.advanceController(c.getSource().getLevel(), pos(c),
                                            IntegerArgumentType.getInteger(c, "ticks"));
                                    return status(c);
                                }))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> center() {
        return Commands.argument("center", BlockPosArgument.blockPos());
    }

    private static BlockPos pos(CommandContext<CommandSourceStack> c) {
        return BlockPosArgument.getBlockPos(c, "center");
    }

    private static int report(CommandContext<CommandSourceStack> c, String action, boolean ok) {
        c.getSource().sendSuccess(() -> Component.literal("ST-05 " + action + " at " + pos(c).toShortString() + ": " + ok), true);
        return ok ? 1 : 0;
    }

    /** A 27-radius smooth-stone floor with 10 blocks of air above and a glowstone grid; destructive by design. */
    private static int testFloor(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos center = pos(c);
        for (int dx = -27; dx <= 27; dx++)
            for (int dz = -27; dz <= 27; dz++) {
                if (dx * dx + dz * dz > 27 * 27) continue;
                boolean light = dx % 6 == 0 && dz % 6 == 0;
                level.setBlockAndUpdate(center.offset(dx, -1, dz), light ? Blocks.GLOWSTONE.defaultBlockState()
                        : Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 0; dy <= 10; dy++) level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        c.getSource().sendSuccess(() -> Component.literal("Built ST-05 test floor at " + center.toShortString()), true);
        return 1;
    }

    private static int register(CommandContext<CommandSourceStack> c) {
        ColosseumArena arena = ColosseumIntegration.registerArena(c.getSource().getLevel(),
                ColosseumArenaGeometry.testFixture(pos(c)));
        c.getSource().sendSuccess(() -> Component.literal("ST-05 test arena registered at "
                + arena.geometry().center().toShortString() + " (phase " + arena.state().phase() + ")"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        ColosseumArena arena = ColosseumIntegration.arena(c.getSource().getLevel(), pos(c));
        if (arena == null) {
            c.getSource().sendFailure(Component.literal("No ST-05 arena registered at " + pos(c).toShortString()));
            return 0;
        }
        ColosseumTrialState s = arena.state();
        String text = "ST-05 " + pos(c).toShortString() + ": phase=" + s.phase() + " round=" + s.round()
                + " trial=" + s.trialId() + " locked=" + s.lockedCount() + " dead=" + s.deadThisRound().size()
                + " allocated=" + s.allocations().size() + " threat=" + s.allocatedThreat()
                + " pending=" + s.count(ColosseumTrialState.AllocationStatus.PENDING)
                + " warning=" + s.count(ColosseumTrialState.AllocationStatus.WARNING)
                + " alive=" + s.count(ColosseumTrialState.AllocationStatus.ALIVE)
                + " killed=" + s.count(ColosseumTrialState.AllocationStatus.KILLED)
                + " cleared=" + s.clearedWaves().size() + " intermission=" + s.intermissionRemaining()
                + " absent=" + s.absenceTicks() + " blocked=" + s.blockedTicks()
                + " cooldown=" + s.cooldownRemaining() + " last=" + s.lastOutcome();
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
