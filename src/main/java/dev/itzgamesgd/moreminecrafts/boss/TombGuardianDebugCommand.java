package dev.itzgamesgd.moreminecrafts.boss;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Developer-only verification commands (operator permission) so the guardian and its encounter rules can be
 * exercised before the royal tomb structure exists. {@code start} stands in for the royal chest interaction.
 */
final class TombGuardianDebugCommand {
    private TombGuardianDebugCommand() {}

    static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("mmtomb")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("spawn").executes(TombGuardianDebugCommand::spawnFree))
                        .then(Commands.literal("start")
                                .then(Commands.argument("anchor", BlockPosArgument.blockPos())
                                        .executes(c -> start(c, BlockPosArgument.getBlockPos(c, "anchor").above()))
                                        .then(Commands.argument("spawn", BlockPosArgument.blockPos())
                                                .executes(c -> start(c, BlockPosArgument.getBlockPos(c, "spawn"))))))
                        .then(Commands.literal("status")
                                .then(Commands.argument("anchor", BlockPosArgument.blockPos())
                                        .executes(TombGuardianDebugCommand::status)))
                        .then(Commands.literal("end")
                                .then(Commands.argument("anchor", BlockPosArgument.blockPos())
                                        .executes(TombGuardianDebugCommand::end)))
                        .then(Commands.literal("attack")
                                .then(Commands.literal("melee").executes(c -> attack(c, TombGuardianCombat.Attack.MELEE)))
                                .then(Commands.literal("slam").executes(c -> attack(c, TombGuardianCombat.Attack.SLAM)))
                                .then(Commands.literal("statue").executes(c -> attack(c, TombGuardianCombat.Attack.STATUE))))));
    }

    /** Unbound guardian for combat checks; it belongs to no tomb and completes nothing. */
    private static int spawnFree(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        TombGuardian guardian = TombGuardianEntities.TOMB_GUARDIAN.create(source.getLevel(), EntitySpawnReason.COMMAND);
        if (guardian == null) return 0;
        var position = source.getPosition();
        guardian.snapTo(position.x, position.y, position.z, source.getRotation().y + 180.0F, 0.0F);
        source.getLevel().addFreshEntity(guardian);
        source.sendSuccess(() -> Component.literal("Spawned an unbound Tomb Guardian (no tomb, no completion record)"), true);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> c, BlockPos spawn) {
        CommandSourceStack source = c.getSource();
        BlockPos anchor = BlockPosArgument.getBlockPos(c, "anchor");
        TombGuardianEncounters.StartResult result = TombGuardianEncounters.onRoyalChestInteraction(
                source.getLevel(), anchor, spawn, source.getPlayer());
        source.sendSuccess(() -> Component.literal("Royal chest interaction at " + anchor.toShortString() + ": " + result
                + " (sealed=" + TombGuardianEncounters.isRoyalChestSealed(source.getLevel(), anchor) + ")"), true);
        return result.spawned() ? 1 : 0;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        ServerLevel level = source.getLevel();
        BlockPos anchor = BlockPosArgument.getBlockPos(c, "anchor");
        TombEncounterData data = TombGuardianEncounters.data(level);
        long tomb = TombGuardianEncounters.tombKey(anchor);
        TombEncounterData.Active active = data.active(tomb);
        String state = data.isDefeated(tomb) ? "DEFEATED" : active == null ? "DORMANT"
                : "ACTIVE guardian=" + active.guardian() + " presence=" + TombGuardianEncounters.presence(level, active);
        source.sendSuccess(() -> Component.literal("Tomb " + anchor.toShortString() + ": " + state
                + " sealed=" + TombGuardianEncounters.isRoyalChestSealed(level, anchor)), false);
        return data.isDefeated(tomb) ? 2 : active == null ? 0 : 1;
    }

    private static int end(CommandContext<CommandSourceStack> c) {
        BlockPos anchor = BlockPosArgument.getBlockPos(c, "anchor");
        boolean ended = TombGuardianEncounters.endEncounter(c.getSource().getLevel(), anchor);
        c.getSource().sendSuccess(() -> Component.literal("End encounter at " + anchor.toShortString() + ": " + ended), true);
        return ended ? 1 : 0;
    }

    /** Forces the nearest guardian to start one attack against the command's player (live telegraph checks). */
    private static int attack(CommandContext<CommandSourceStack> c, TombGuardianCombat.Attack attack) {
        CommandSourceStack source = c.getSource();
        ServerLevel level = source.getLevel();
        LivingEntity target = source.getPlayer();
        var position = source.getPosition();
        TombGuardian guardian = level.getEntitiesOfClass(TombGuardian.class, new AABB(position, position).inflate(32.0D),
                        g -> g.isAlive() && !g.isAwakening())
                .stream().min(java.util.Comparator.comparingDouble(g -> g.distanceToSqr(position))).orElse(null);
        if (guardian == null || target == null || !guardian.forceAttack(level, attack, target)) {
            source.sendFailure(Component.literal("No idle Tomb Guardian within 32 blocks (or no player target)"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Tomb Guardian forced attack: " + attack), false);
        return 1;
    }
}
