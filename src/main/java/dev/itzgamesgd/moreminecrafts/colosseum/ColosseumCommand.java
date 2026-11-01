package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Operator tools: register a verified arena, show its state, release a quarantined arena. */
final class ColosseumCommand {
    private ColosseumCommand() {}

    static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("mmcolosseum").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("register")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .then(Commands.argument("floorY", IntegerArgumentType.integer())
                                                        .executes(c -> {
                                                            int x = IntegerArgumentType.getInteger(c, "x"), z = IntegerArgumentType.getInteger(c, "z"), y = IntegerArgumentType.getInteger(c, "floorY");
                                                            boolean added = ColosseumRuntime.registerArena(c.getSource().getServer(), x + "_" + z, x, z, y);
                                                            c.getSource().sendSuccess(() -> Component.literal(added ? "registered arena " + x + "_" + z : "already registered"), true);
                                                            return added ? 1 : 0;
                                                        })))))
                        .then(Commands.literal("status").executes(c -> {
                            ColosseumData d = ColosseumRuntime.data(c.getSource().getServer());
                            d.sites().forEach((id, site) -> {
                                ArenaController k = d.controller(id);
                                String s = id + " cooldown=" + k.cooldownLeft() + " quarantined=" + k.quarantined() + (k.quarantined() ? "(" + k.quarantineReason() + ")" : "")
                                        + " last=" + k.lastOutcome() + (k.attempt() == null ? "" : " attempt=" + k.attempt().attemptId + " round=" + k.attempt().round()
                                        + " phase=" + k.attempt().phase() + " roster=" + k.attempt().rosterIds().size() + " pending=" + k.attempt().pendingIds().size());
                                c.getSource().sendSuccess(() -> Component.literal(s), false);
                            });
                            return d.sites().size();
                        }))
                        .then(Commands.literal("release").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
                            ColosseumData d = ColosseumRuntime.data(c.getSource().getServer());
                            ArenaController k = d.controller(StringArgumentType.getString(c, "id"));
                            if (k == null || !k.quarantined()) return 0;
                            k.release();
                            d.setDirty();
                            return 1;
                        })))));
    }
}
