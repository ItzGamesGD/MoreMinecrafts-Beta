package dev.itzgamesgd.moreminecrafts.colosseum;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Rendered network client connected to a separate dedicated 26.3 server; R7 fixture, not manual gameplay-feel acceptance. */
public final class GladiatorClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if(!Boolean.getBoolean("moreminecrafts.st05b.client"))return;
        Properties properties=new Properties();properties.setProperty("online-mode","false");properties.setProperty("view-distance","5");properties.setProperty("simulation-distance","5");
        try(TestDedicatedServerContext server=context.worldBuilder().createServer(properties);TestDedicatedServerConnection connection=server.connect()) {
            context.waitFor(c->c.player!=null && c.level!=null);
            server.runOnServer(s->{
                var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);p.setPermanentlyInvulnerable(true);
                for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++) {
                    l.setBlock(new BlockPos(x,149,z),Blocks.DEEPSLATE_BRICKS.defaultBlockState(),18);
                    for(int y=150;y<157;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),18);
                }
                ColosseumRuntime.registerArena(s,"client-r7",0,0,150);
                p.teleportTo(l,8.5,150,8.5,Set.of(),135,0,true);
            });
            context.waitTicks(10);
            server.runOnServer(s->{
                var c=ColosseumRuntime.data(s).controller("client-r7");var a=c.attempt();
                if(a==null)throw new AssertionError("real client entry starts arena");
                for(var e:s.overworld().getAllEntities())if(e.entityTags().contains(ColosseumRuntime.TAG_ATTEMPT+a.attemptId))e.discard();
                a.allocs.clear();a.round=7;a.phase=ArenaAttempt.Phase.ROUND;
                a.snap=new ArenaAttempt.Snapshot(7,1,1000,0,1,new ArrayList<>(a.rosterIds()));
            });
            context.waitFor(c->c.level!=null && c.level.entitiesForRendering().iterator().hasNext());
            context.waitTicks(100);connection.waitForClientboundPackets();
            context.runOnClient(c->{
                boolean found=false;for(var e:c.level.entitiesForRendering())if(e instanceof ImmortalGladiator)found=true;
                if(!found)throw new AssertionError("custom boss reached rendered client");
                if(bossBars(c).isEmpty())throw new AssertionError("R7 health bar not received");
            });
            context.takeScreenshot("st05b-gladiator-phase1");
            server.runOnServer(s->{var a=ColosseumRuntime.data(s).controller("client-r7").attempt();((ImmortalGladiator)s.overworld().getEntity(a.bossEntity)).setHealth(100);});
            context.waitTicks(50);connection.waitForClientboundPackets();
            context.runOnClient(c->{for(var e:c.level.entitiesForRendering())if(e instanceof ImmortalGladiator b && !b.phaseTwo())throw new AssertionError("phase two not synchronized");});
            context.takeScreenshot("st05b-gladiator-phase2");
            server.runOnServer(s->{var a=ColosseumRuntime.data(s).controller("client-r7").attempt();s.overworld().getEntity(a.bossEntity).kill(s.overworld());});
            context.waitTicks(30);connection.waitForClientboundPackets();
            server.runOnServer(s->{if(ColosseumRuntime.data(s).controller("client-r7").attempt().outcome()!=ArenaAttempt.Outcome.SUCCESS)throw new AssertionError("client encounter did not complete");});
            context.runOnClient(c->{if(!bossBars(c).isEmpty())throw new AssertionError("boss bar remains after encounter end");});
            context.takeScreenshot("st05b-gladiator-complete");System.out.println("ST05B_RENDERED_CLIENT_PASS phase1 phase2 success");
        }
    }
    private static Map<?,?> bossBars(net.minecraft.client.Minecraft c) {
        try {var overlay=c.gui.hud.getBossOverlay();var f=overlay.getClass().getDeclaredField("events");f.setAccessible(true);return (Map<?,?>)f.get(overlay);}
        catch(ReflectiveOperationException e) {throw new AssertionError(e);}
    }

}
