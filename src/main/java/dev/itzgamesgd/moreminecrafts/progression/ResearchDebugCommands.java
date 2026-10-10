package dev.itzgamesgd.moreminecrafts.progression;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** OP-only temporary test route; not the final paid research purchase UI. */
public final class ResearchDebugCommands {
    private ResearchDebugCommands() {}

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) ->
            dispatcher.register(Commands.literal("mmresearch")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("test")
                    .then(Commands.literal("crafting").executes(ctx -> {
                        ServerPlayer p=ctx.getSource().getPlayerOrException();
                        for (int id : new int[]{4,14,15,17,18,19,22,25,30,42,44,57,60})
                            KnowledgeRuntime.grantKnowledge(p,id);
                        p.sendSystemMessage(Component.literal("Granted 13 knowledge recipes for live validation only."));
                        return 1;
                    })))));
    }
}
