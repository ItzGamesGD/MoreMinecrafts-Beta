package dev.itzgamesgd.moreminecrafts.pillagerhq;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Opt-in screenshots of actual normal-world starts. Never places templates or fabricates HQ records. */
public final class St03NaturalClientCapture implements FabricClientGameTest {
    private record View(String name,int x,int y,int z,int tx,int ty,int tz) {}
    private static final List<View> VIEWS=List.of(
            new View("exterior",56,46,170,56,8,56),
            // F01 NBT: feet/eye and central sightline are explicit air above the chandelier ring.
            new View("interior",51,8,16,56,9,14));

    @Override public void runTest(ClientGameTestContext context) {
        String planFile=System.getProperty("mm.st03.capture.plan");if(planFile==null)return;
        Path out=Path.of(System.getProperty("mm.st03.capture.out","build/st03-natural/captures")).toAbsolutePath();
        Map<String,Object> manifest=new LinkedHashMap<>();List<Map<String,Object>> captures=new ArrayList<>();manifest.put("captures",captures);
        try {
            Files.createDirectories(out);
            JsonObject plan=JsonParser.parseString(Files.readString(Path.of(planFile))).getAsJsonObject();
            long seed=plan.get("seed").getAsLong();manifest.put("seed",seed);manifest.put("head_sha",System.getenv("GITHUB_SHA"));
            manifest.put("run_id",System.getenv("GITHUB_RUN_ID"));manifest.put("source_survey_head",plan.get("head_sha").getAsString());
            manifest.put("source_survey_run",plan.get("run_id").getAsString());manifest.put("night_vision",true);
            if(plan.getAsJsonArray("targets").size()<3)throw new AssertionError("Three natural HQ targets required");
            Properties props=new Properties();props.setProperty("online-mode","false");props.setProperty("level-seed",Long.toString(seed));
            props.setProperty("level-type","minecraft:normal");props.setProperty("generate-structures","true");props.setProperty("spawn-protection","0");
            props.setProperty("view-distance","10");props.setProperty("simulation-distance","6");props.setProperty("max-tick-time","-1");
            props.setProperty("spawn-monsters","false");props.setProperty("spawn-animals","false");
            // Send the intended view distance in the login client information, before connecting.
            context.runOnClient(c->c.options.renderDistance().set(10));
            try(TestDedicatedServerContext server=context.worldBuilder().setUseConsistentSettings(false)
                    .adjustSettings(settings->settings.setSeed(Long.toString(seed))).createServer(props);
                TestDedicatedServerConnection connection=server.connect()) {
                context.waitFor(c->c.player!=null&&c.level!=null);
                context.runOnClient(c->{if(!c.gui.hud.isHidden())c.gui.hud.toggle();c.gui.toastManager().clear();c.options.fov().set(85);});
                server.runOnServer(s->{
                    if(!(s.overworld().getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator)||s.overworld().getSeed()!=seed)
                        throw new AssertionError("Capture requires the surveyed seed and NoiseBasedChunkGenerator");
                    manifest.put("generator",s.overworld().getChunkSource().getGenerator().getClass().getName());
                    System.out.println("ST03_CAPTURE_GENERATOR seed="+seed+" class="+manifest.get("generator"));
                    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,72000,0,false,false,false));
                    s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),"time set 6000");
                    s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),"weather clear");
                });
                int index=0;
                for(var element:plan.getAsJsonArray("targets")) {
                    if(index==3)break;index++;
                    var target=element.getAsJsonObject();int cx=target.get("candidate_chunk_x").getAsInt(),cz=target.get("candidate_chunk_z").getAsInt();
                    long startKey=new ChunkPos(cx,cz).pack();BlockPos origin=new BlockPos(target.get("origin_x").getAsInt(),target.get("foundation_y").getAsInt(),target.get("origin_z").getAsInt());
                    Rotation rotation=Rotation.valueOf(target.get("rotation").getAsString());
                    server.runOnServer(s->{
                        ServerLevel level=s.overworld();var structure=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(St03Worldgen.STRUCTURE_ID);
                        var start=level.getChunk(cx,cz,ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
                        if(start==null||!start.isValid())throw new AssertionError("Natural structure start absent at "+cx+","+cz);
                        var module=start.getPieces().stream().filter(p->p instanceof St03AssemblyPiece).map(p->(St03AssemblyPiece)p).findFirst().orElseThrow();
                        if(!module.compoundOrigin().equals(origin)||module.compoundRotation()!=rotation)throw new AssertionError("Capture disagrees with production survey");
                        // Only the normal chunk pipeline places this structure. No addPieces/placeInWorld/getOrCreate calls.
                        for(int x=origin.getX()>>4;x<=(origin.getX()+111)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+111)>>4;z++) {
                            level.setChunkForced(x,z,true);level.getChunk(x,z,ChunkStatus.FULL);
                        }
                    });
                    boolean complete=false;
                    for(int n=0;n<80&&!complete;n++) {
                        context.waitTicks(5);complete=server.computeOnServer(s->{
                            var level=s.overworld();var hq=St03HeadquartersData.get(level).instances().get(startKey);if(hq==null||!hq.structureCompleted)return false;
                            for(int x=origin.getX()>>4;x<=(origin.getX()+111)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+111)>>4;z++)
                                if(!level.areEntitiesLoaded(new ChunkPos(x,z).pack())||!level.isPositionEntityTicking(new BlockPos(x*16+8,origin.getY(),z*16+8)))return false;
                            return true;
                        });
                    }
                    if(!complete)throw new AssertionError("Natural completion hook did not finish "+cx+","+cz);
                    server.runOnServer(s->{
                        var hq=St03HeadquartersData.get(s.overworld()).instances().get(startKey);
                        if(hq.garrisonInitialized)throw new AssertionError("Garrison initialized before first non-spectator proximity");
                        var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.onUpdateAbilities();
                        p.teleportTo(s.overworld(),origin.getX()+56.5,origin.getY()+1,origin.getZ()+56.5,Set.of(),0,0,true);
                    });
                    context.waitTicks(2);Map<String,Object> natural=new LinkedHashMap<>();
                    server.runOnServer(s->{
                        var level=s.overworld();var hq=St03HeadquartersData.get(level).instances().get(startKey);
                        int count=level.getEntitiesOfClass(Mob.class,new AABB(hq.box.minX(),hq.box.minY(),hq.box.minZ(),hq.box.maxX()+1,hq.box.maxY()+1,hq.box.maxZ()+1).inflate(32),m->m.isAlive()
                                &&m.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX+hq.id())&&!m.entityTags().contains(St03GarrisonManager.TAG_PRISONER)).size();
                        if(!hq.garrisonInitialized||count!=40)throw new AssertionError("First natural proximity defenders="+count);
                        natural.put("hq_id",hq.id());natural.put("structure_completed",hq.structureCompleted);natural.put("first_proximity_defenders",count);
                        natural.put("all_49_chunks_entity_ticking_before_proximity",true);
                        s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
                        s.tickRateManager().setFrozen(true);
                        System.out.println("ST03_CAPTURE_READY "+hq.id()+" origin="+origin+" defenders="+count);
                    });
                    for(var view:VIEWS) {
                        String name="st03-natural-"+index+"-"+view.name();
                        Map<String,Object> shot=capture(context,server,connection,out,name,origin,rotation,view);
                        shot.put("candidate_chunk",List.of(cx,cz));shot.put("origin",List.of(origin.getX(),origin.getY(),origin.getZ()));
                        shot.put("rotation",rotation.name());shot.put("natural_completion",natural);captures.add(shot);
                    }
                    server.runOnServer(s->{
                        var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),0,200,0,Set.of(),0,0,true);
                        s.tickRateManager().setFrozen(false);
                        for(int x=origin.getX()>>4;x<=(origin.getX()+111)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+111)>>4;z++)s.overworld().setChunkForced(x,z,false);
                    });
                }
                manifest.put("client",context.computeOnClient(c->Map.of("width",c.getWindow().getWidth(),"height",c.getWindow().getHeight(),"fov",c.options.fov().get())));
            }
            manifest.put("status","PASS");Files.writeString(out.resolve("manifest.json"),new GsonBuilder().setPrettyPrinting().create().toJson(manifest));
            System.out.println("ST03_NATURAL_CAPTURE_COMPLETE "+captures.size());
        }catch(Exception e){throw new AssertionError(e);}
    }
    private static Map<String,Object> capture(ClientGameTestContext context,TestDedicatedServerContext server,TestDedicatedServerConnection connection,
            Path out,String name,BlockPos origin,Rotation rotation,View view) throws Exception {
        int[] at=St03PlacementManifest.rotateLocalIn112(view.x(),view.z(),rotation),look=St03PlacementManifest.rotateLocalIn112(view.tx(),view.tz(),rotation);
        double x=origin.getX()+at[0]+0.5,y=origin.getY()+view.y(),z=origin.getZ()+at[1]+0.5;
        double tx=origin.getX()+look[0]+0.5,ty=origin.getY()+view.ty(),tz=origin.getZ()+look[1]+0.5;
        float yaw=(float)Math.toDegrees(Math.atan2(-(tx-x),tz-z));float pitch=(float)-Math.toDegrees(Math.atan2(ty-(y+1.62),Math.hypot(tx-x,tz-z)));
        server.runOnServer(s->{
            s.tickRateManager().setFrozen(false);
            int cx=((int)Math.floor(x))>>4,cz=((int)Math.floor(z))>>4;
            // Prepare the photographic surroundings before freezing, including ordinary terrain outside the HQ.
            for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++)s.overworld().getChunk(cx+dx,cz+dz,ChunkStatus.FULL);
            s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(),x,y,z,Set.of(),yaw,pitch,true);
        });
        context.waitFor(c->c.player.position().distanceTo(new Vec3(x,y,z))<0.6,400);
        // The Fabric helper requests a complete view-distance square, while the server can send a rounded view.
        // Require every actual HQ chunk and the camera's immediate terrain, then wait for received meshes to render.
        context.waitFor(c->{
            for(int cx=origin.getX()>>4;cx<=(origin.getX()+111)>>4;cx++)for(int cz=origin.getZ()>>4;cz<=(origin.getZ()+111)>>4;cz++)
                if(c.level.getChunk(cx,cz,ChunkStatus.FULL,false)==null)return false;
            int cx=((int)Math.floor(x))>>4,cz=((int)Math.floor(z))>>4;
            for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++)
                if(dx*dx+dz*dz<=81&&c.level.getChunk(cx+dx,cz+dz,ChunkStatus.FULL,false)==null)return false;
            return true;
        },2400);
        connection.waitForClientboundPackets();context.waitTicks(100);
        int renderTicks=connection.waitForChunksRender(false,2400);
        server.runOnServer(s->{
            var level=s.overworld();int unsupported=0,wet=0;
            for(int bx=origin.getX();bx<origin.getX()+112;bx++)for(int bz=origin.getZ();bz<origin.getZ()+112;bz++) {
                var support=level.getBlockState(new BlockPos(bx,origin.getY()-1,bz));
                if(!support.isSolidRender())unsupported++;if(!support.getFluidState().isEmpty())wet++;
            }
            if(unsupported!=0||wet!=0)throw new AssertionError("Photographic neighbors changed HQ support: "+unsupported+" unsupported, "+wet+" wet");
            s.tickRateManager().setFrozen(true);
        });
        context.runOnClient(c->c.gui.toastManager().clear());context.waitTicks(10);
        boolean eyeClear=context.computeOnClient(c->c.level.getBlockState(BlockPos.containing(c.player.getEyePosition())).isAir());
        if(!eyeClear)throw new AssertionError("Capture camera inside solid block: "+name);
        int[] size=context.computeOnClient(c->new int[]{c.getWindow().getWidth(),c.getWindow().getHeight()});
        if(size[0]!=1280||size[1]!=720)throw new AssertionError("Native screenshot must be 1280x720, got "+Arrays.toString(size));
        Path screenshot=context.takeScreenshot(name);Files.copy(screenshot,out.resolve(name+".png"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Map<String,Object> row=new LinkedHashMap<>();row.put("name",name);row.put("file",name+".png");row.put("feet",List.of(x,y,z));row.put("yaw",yaw);row.put("pitch",pitch);row.put("eye_air",true);
        row.put("hq_chunks_downloaded",49);row.put("render_wait_ticks",renderTicks);row.put("server_frozen_for_photography",true);
        return row;
    }
}
