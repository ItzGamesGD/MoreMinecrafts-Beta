package dev.itzgamesgd.moreminecrafts.boss;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Comparator;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Developer commands (operator permission) to film and test the Lava Forgemaster before its summon trigger exists.
 * <ul>
 *   <li>{@code /mmforge spawn} - spawn one in front of you (it fights normally).</li>
 *   <li>{@code /mmforge scripted <true|false>} - nearest forgemaster only does what you command (no chasing or auto skills).</li>
 *   <li>{@code /mmforge skill <name>} - nearest forgemaster performs the skill toward you.</li>
 *   <li>{@code /mmforge phase2} - trigger the overheat transition.</li>
 * </ul>
 */
final class PiglinForgemasterDebugCommand {
    private PiglinForgemasterDebugCommand() {}

    static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("mmforge")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("spawn").executes(PiglinForgemasterDebugCommand::spawn))
                        .then(Commands.literal("phase2").executes(PiglinForgemasterDebugCommand::phaseTwo))
                        .then(Commands.literal("scripted")
                                .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                        .executes(PiglinForgemasterDebugCommand::scripted)))
                        .then(Commands.literal("skill")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            for (PiglinForgemaster.Skill s : PiglinForgemaster.Skill.values()) {
                                                if (s != PiglinForgemaster.Skill.NONE) b.suggest(s.name().toLowerCase(Locale.ROOT));
                                            }
                                            return b.buildFuture();
                                        })
                                        .executes(PiglinForgemasterDebugCommand::skill)))));
    }

    private static PiglinForgemaster nearest(CommandContext<CommandSourceStack> c) {
        var source = c.getSource();
        return source.getLevel().getEntitiesOfClass(PiglinForgemaster.class, net.minecraft.world.phys.AABB.ofSize(source.getPosition(), 128.0, 128.0, 128.0)).stream()
                .min(Comparator.comparingDouble(f -> f.distanceToSqr(source.getPosition())))
                .orElse(null);
    }

    private static int spawn(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        PiglinForgemaster boss = PiglinForgemasterEntities.PIGLIN_FORGEMASTER.create(source.getLevel(), EntitySpawnReason.COMMAND);
        if (boss == null) return 0;
        var look = source.getRotation();
        double yaw = Math.toRadians(look.y);
        double x = source.getPosition().x - Math.sin(yaw) * 8.0;
        double z = source.getPosition().z + Math.cos(yaw) * 8.0;
        boss.snapTo(x, source.getPosition().y, z, look.y + 180.0F, 0.0F);
        source.getLevel().addFreshEntity(boss);
        source.sendSuccess(() -> Component.literal("Spawned a Lava Forgemaster 8 blocks ahead"), true);
        return 1;
    }

    private static int scripted(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PiglinForgemaster boss = nearest(c);
        if (boss == null) {
            c.getSource().sendFailure(Component.literal("No forgemaster within 64 blocks"));
            return 0;
        }
        boolean value = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "value");
        boss.setScripted(value);
        c.getSource().sendSuccess(() -> Component.literal("Forgemaster scripted=" + value), true);
        return 1;
    }

    private static int skill(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PiglinForgemaster boss = nearest(c);
        if (boss == null) {
            c.getSource().sendFailure(Component.literal("No forgemaster within 64 blocks"));
            return 0;
        }
        String name = StringArgumentType.getString(c, "name").toUpperCase(Locale.ROOT);
        PiglinForgemaster.Skill skill;
        try {
            skill = PiglinForgemaster.Skill.valueOf(name);
        } catch (IllegalArgumentException e) {
            c.getSource().sendFailure(Component.literal("Unknown skill " + name));
            return 0;
        }
        boolean ok = boss.force(skill, c.getSource().getPlayerOrException().position());
        c.getSource().sendSuccess(() -> Component.literal("Forgemaster " + skill.name().toLowerCase(Locale.ROOT) + (ok ? "" : " (refused)")), true);
        return ok ? 1 : 0;
    }

    private static int phaseTwo(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PiglinForgemaster boss = nearest(c);
        if (boss == null) {
            c.getSource().sendFailure(Component.literal("No forgemaster within 64 blocks"));
            return 0;
        }
        boss.enterPhaseTwo();
        c.getSource().sendSuccess(() -> Component.literal("Forgemaster entering phase 2"), true);
        return 1;
    }
}
