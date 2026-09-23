package dev.itzgamesgd.moreminecrafts.skeleton;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;

public final class SkeletonKingCommand {
    private SkeletonKingCommand() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("summonskeletonking")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(command -> {
                            var source = command.getSource();
                            Skeleton skeleton = EntityTypes.SKELETON.create(source.getLevel(), EntitySpawnReason.COMMAND);
                            if (skeleton == null) return 0;
                            var position = source.getPosition();
                            skeleton.setPos(position.x, position.y, position.z);
                            ((SkeletonKingState) skeleton).moreminecrafts$makeSkeletonKing();
                            source.getLevel().addFreshEntity(skeleton);
                            source.sendSuccess(() -> Component.literal("Summoned Skeleton King"), true);
                            return 1;
                        })));
    }
}
