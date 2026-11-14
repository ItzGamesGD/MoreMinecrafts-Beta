package dev.itzgamesgd.moreminecrafts.test.crystalguardian;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * Operator-only POC test controls. Visual/animation verification only: nothing here deals damage, searches for targets or
 * changes any gameplay system.
 *
 * <pre>
 * /mmcrystalguardian spawn [pos]            summon one test guardian (same as /summon moreminecrafts:ancient_crystal_guardian_test)
 * /mmcrystalguardian walk [ticks] [speed]   forced straight walk (default 100 ticks, speed multiplier 1.0)
 * /mmcrystalguardian stop                   stop walking
 * /mmcrystalguardian swing                  one right-arm swing (ignored while one is already running)
 * /mmcrystalguardian loop &lt;count&gt; [gap]     repeated swings, gap ticks between swings (default 10)
 * /mmcrystalguardian reset                  stop walking and cancel pending loop swings
 * /mmcrystalguardian status                 report state of all test guardians in this dimension
 * </pre>
 * Commands act on every test guardian in the executing dimension.
 */
public final class AncientCrystalGuardianTestCommand {
    private AncientCrystalGuardianTestCommand() {}

    static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("mmcrystalguardian")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("spawn")
                                .executes(c -> spawn(c, c.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(c -> spawn(c, Vec3Argument.getVec3(c, "pos")))))
                        .then(Commands.literal("walk")
                                .executes(c -> walk(c, 100, 1.0D))
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 20 * 600))
                                        .executes(c -> walk(c, IntegerArgumentType.getInteger(c, "ticks"), 1.0D))
                                        .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.1D, 3.0D))
                                                .executes(c -> walk(c, IntegerArgumentType.getInteger(c, "ticks"),
                                                        DoubleArgumentType.getDouble(c, "speed"))))))
                        .then(Commands.literal("stop").executes(c -> each(c, "Stopped walking", AncientCrystalGuardianTest::stopWalk)))
                        .then(Commands.literal("swing").executes(AncientCrystalGuardianTestCommand::swing))
                        .then(Commands.literal("loop")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 1000))
                                        .executes(c -> loop(c, IntegerArgumentType.getInteger(c, "count"), 10))
                                        .then(Commands.argument("gap", IntegerArgumentType.integer(0, 400))
                                                .executes(c -> loop(c, IntegerArgumentType.getInteger(c, "count"),
                                                        IntegerArgumentType.getInteger(c, "gap"))))))
                        .then(Commands.literal("reset").executes(c -> each(c, "Reset", AncientCrystalGuardianTest::resetTest)))
                        .then(Commands.literal("status").executes(AncientCrystalGuardianTestCommand::status))));
    }

    private static List<? extends AncientCrystalGuardianTest> guardians(CommandContext<CommandSourceStack> c) {
        return c.getSource().getLevel().getEntities(AncientCrystalGuardianTestEntities.TYPE, e -> e.isAlive());
    }

    private static int spawn(CommandContext<CommandSourceStack> c, Vec3 at) {
        CommandSourceStack source = c.getSource();
        AncientCrystalGuardianTest g = AncientCrystalGuardianTestEntities.TYPE.create(source.getLevel(), EntitySpawnReason.COMMAND);
        if (g == null) return 0;
        g.snapTo(at.x, at.y, at.z, source.getRotation().y + 180.0F, 0.0F);
        source.getLevel().addFreshEntity(g);
        source.sendSuccess(() -> Component.literal("Spawned Ancient Crystal Guardian test entity (no AI, no drops)"), true);
        return 1;
    }

    private static int walk(CommandContext<CommandSourceStack> c, int ticks, double speed) {
        return each(c, "Walking " + ticks + " ticks at x" + speed, g -> g.startWalk(ticks, speed));
    }

    private static int swing(CommandContext<CommandSourceStack> c) {
        int started = 0;
        for (AncientCrystalGuardianTest g : guardians(c)) if (g.requestSwing()) started++;
        int n = started;
        c.getSource().sendSuccess(() -> Component.literal("Swing started on " + n + " guardian(s)"), false);
        return n;
    }

    private static int loop(CommandContext<CommandSourceStack> c, int count, int gap) {
        return each(c, "Swing loop x" + count + " gap " + gap, g -> g.startSwingLoop(count, gap));
    }

    private static int each(CommandContext<CommandSourceStack> c, String message, java.util.function.Consumer<AncientCrystalGuardianTest> action) {
        List<? extends AncientCrystalGuardianTest> list = guardians(c);
        list.forEach(action);
        int n = list.size();
        c.getSource().sendSuccess(() -> Component.literal(message + " (" + n + " guardian(s))"), false);
        return n;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        List<? extends AncientCrystalGuardianTest> list = guardians(c);
        for (AncientCrystalGuardianTest g : list) {
            c.getSource().sendSuccess(() -> Component.literal(g.getStringUUID() + " walking=" + g.isForcedWalking()
                    + " swinging=" + g.isSwinging() + " loopLeft=" + g.swingRepeatsLeft()), false);
        }
        return list.size();
    }
}
