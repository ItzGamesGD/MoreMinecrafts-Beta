package dev.itzgamesgd.moreminecrafts.skytower;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Operator-only registration of an already placed, verified approved template.
 * Natural generation, battle-completion observation and barriers remain separate integration stages.
 */
public final class SkyTowerIntegration {
    private SkyTowerIntegration() {}
    public static void initialize(){
        CommandRegistrationCallback.EVENT.register((dispatcher,context,environment)->dispatcher.register(
            Commands.literal("mmskytower").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("register")
                    .then(Commands.argument("x",IntegerArgumentType.integer(-30000000,30000000))
                        .then(Commands.argument("y",IntegerArgumentType.integer(-2048,2048))
                            .then(Commands.argument("z",IntegerArgumentType.integer(-30000000,30000000))
                                .then(Commands.argument("rotation",IntegerArgumentType.integer(0,3)).executes(c->{
                                    ServerLevel level=c.getSource().getLevel();
                                    var site=new SkyTowerLayout.Site(level.dimension().identifier().toString(),
                                        IntegerArgumentType.getInteger(c,"x"),IntegerArgumentType.getInteger(c,"y"),
                                        IntegerArgumentType.getInteger(c,"z"),IntegerArgumentType.getInteger(c,"rotation"));
                                    if(!anchorsPresent(level,site)){
                                        c.getSource().sendFailure(Component.literal("Tower anchors missing/unloaded; nothing registered."));return 0;
                                    }
                                    var data=level.getServer().overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
                                    try{
                                        boolean added=data.register(site);
                                        c.getSource().sendSuccess(()->Component.literal((added?"Registered ":"Already registered ")+site.id()),false);
                                        return added?1:0;
                                    }catch(IllegalArgumentException | IllegalStateException e){c.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}
                                }))))))
                .then(Commands.literal("status").executes(c->{
                    var d=c.getSource().getServer().overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
                    c.getSource().sendSuccess(()->Component.literal("registry quarantined="+d.quarantined()),false);
                    d.sites().forEach((id,site)->c.getSource().sendSuccess(()->Component.literal(id+" completedSpawnerMask="+d.completedMask(id)),false));
                    return d.sites().size();
                }))));
    }
    private static boolean anchorsPresent(ServerLevel level,SkyTowerLayout.Site site){
        for(int i=0;i<16;i++){
            var a=site.spawner(i);var p=new BlockPos(a.x(),a.y(),a.z());
            if(!level.hasChunkAt(p) || !level.getBlockState(p).is(Blocks.TRIAL_SPAWNER))return false;
        }
        for(int i=0;i<8;i++){
            var a=site.vault(i);var p=new BlockPos(a.x(),a.y(),a.z());
            if(!level.hasChunkAt(p) || !level.getBlockState(p).is(Blocks.VAULT))return false;
        }
        return true;
    }
}
