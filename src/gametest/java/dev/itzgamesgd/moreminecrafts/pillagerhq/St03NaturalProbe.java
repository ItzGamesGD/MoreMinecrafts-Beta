package dev.itzgamesgd.moreminecrafts.pillagerhq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedWriter;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import org.slf4j.LoggerFactory;

/** Dedicated-server actual start survey and natural-completion/first-proximity validation. Inert without opt-in. */
public final class St03NaturalProbe implements ModInitializer {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final int RADIUS=25,COMMON_MIN=-1200,COMMON_MAX=1248;
    private static final long SIDE=51L*66*16,AREA=SIDE*SIDE,COMMON_AREA=2448L*16*2448*16;
    private static final double PREDICTED=64000000.0*(1739.0/26010)/(256*66*66);
    private static Session session;

    @Override public void onInitialize() {
        String output=System.getProperty("mm.st03.natural.out");if(output==null)return;
        Identifier after=Identifier.fromNamespaceAndPath("moreminecrafts","st03_natural_validation");
        ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(Event.DEFAULT_PHASE,after);
        ServerTickEvents.END_SERVER_TICK.register(after,server->{
            if(session==null)return;
            try {session.tick();}catch(Throwable t){session.finish(t);}
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            Map<String,Object> result=new LinkedHashMap<>();long start=System.nanoTime();Path dir=Path.of(output);
            result.put("head_sha",System.getenv("GITHUB_SHA"));result.put("run_id",System.getenv("GITHUB_RUN_ID"));result.put("seed",server.overworld().getSeed());
            try {
                scan(server.overworld(),dir,result);
                Files.writeString(dir.resolve("scan.json"),JSON.toJson(result));
                if(Boolean.getBoolean("mm.st03.natural.scanOnly")) {
                    result.put("scope","actual_start_scan_only");result.put("full_generation_status","NOT RUN");
                    result.put("full_generation_reason","Standalone start survey; full terrain/completion validation runs separately");
                    writeCapturePlan(dir,result);writeAndExit(server,dir,result,start,null);return;
                }
                session=new Session(server,dir,result,start);
            } catch(Throwable t) {writeAndExit(server,dir,result,start,t);}
        });
    }
    @SuppressWarnings("unchecked") private static void writeCapturePlan(Path dir,Map<String,Object> result) throws Exception {
        var starts=(List<Map<String,Object>>)result.get("starts");
        var targets=List.copyOf(starts.subList(0,Math.min(3,starts.size())));
        Map<String,Object> plan=new LinkedHashMap<>();plan.put("seed",result.get("seed"));plan.put("head_sha",result.get("head_sha"));
        plan.put("run_id",result.get("run_id"));plan.put("targets",targets);
        Files.writeString(dir.resolve("capture-plan.json"),JSON.toJson(plan));
    }
    private static void writeAndExit(MinecraftServer server,Path dir,Map<String,Object> result,long start,Throwable error) {
        result.put("status",error==null?"PASS":"FAIL");result.put("total_seconds",(System.nanoTime()-start)/1e9);
        if(error!=null) {LoggerFactory.getLogger("st03-natural-v2").error("Natural placement validation failed",error);result.put("error",error.toString());result.put("stack",Arrays.toString(error.getStackTrace()));}
        try {Files.createDirectories(dir);Files.writeString(dir.resolve("result.json"),JSON.toJson(result));}
        catch(Exception e){throw new RuntimeException(e);}
        Runtime.getRuntime().halt(error==null?0:1);
    }
    private static void scan(ServerLevel level,Path dir,Map<String,Object> result) throws Exception {
        Files.createDirectories(dir);
        if(!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator gen))throw new IllegalStateException("Expected normal noise world");
        LoggerFactory.getLogger("st03-natural-v2").info("ST03_NATURAL_GENERATOR seed={} class={}",level.getSeed(),gen.getClass().getName());
        var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,St03Worldgen.STRUCTURE_ID));
        Structure structure=holder.value();
        var set=level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET,St03Worldgen.STRUCTURE_ID)).value();
        var placement=(RandomSpreadStructurePlacement)set.placement();
        if(placement.spacing()!=66||placement.separation()!=22||!placement.spreadType().getSerializedName().equals("triangular"))throw new AssertionError("66/22 TRIANGULAR required");
        var state=level.getChunkSource().getGeneratorState();var rs=level.getChunkSource().randomState();
        var sampler=rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
        result.put("generator",gen.getClass().getName());result.put("spacing",66);result.put("separation",22);result.put("salt",91403);
        result.put("region_radius",RADIUS);result.put("area_blocks_squared",AREA);result.put("min_block_inclusive",-25*66*16);result.put("max_block_exclusive",26*66*16);
        result.put("predicted_per_64km2",PREDICTED);result.put("common_min_chunk",COMMON_MIN);result.put("common_max_chunk_exclusive",COMMON_MAX);result.put("common_area_blocks_squared",COMMON_AREA);
        int potential=0,placementPass=0,valid=0,commonPotential=0,commonValid=0;
        List<Map<String,Object>> starts=new ArrayList<>();long started=System.nanoTime();
        try(BufferedWriter out=Files.newBufferedWriter(dir.resolve("starts.csv"))) {
            out.write("seed,rx,rz,candidate_chunk_x,candidate_chunk_z,valid,origin_x,foundation_y,origin_z,dx_chunks,dz_chunks,pieces,common_rectangle\n");
            for(int rx=-RADIUS;rx<=RADIUS;rx++) {
                for(int rz=-RADIUS;rz<=RADIUS;rz++) {
                    ChunkPos cp=placement.getPotentialStructureChunk(level.getSeed(),rx*66,rz*66);
                    boolean common=cp.x()>=COMMON_MIN&&cp.x()<COMMON_MAX&&cp.z()>=COMMON_MIN&&cp.z()<COMMON_MAX;
                    potential++;if(common)commonPotential++;
                    if(!placement.isStructureChunk(state,cp.x(),cp.z()))continue;placementPass++;
                    StructureStart generated=structure.generate(holder,level.dimension(),level.registryAccess(),gen,gen.getBiomeSource(),sampler,rs,
                            level.getStructureTemplateManager(),level.getSeed(),cp,0,level,structure.biomes()::contains);
                    if(!generated.isValid()) {out.write(level.getSeed()+","+rx+","+rz+","+cp.x()+","+cp.z()+",false,,,,,,,"+common+"\n");continue;}
                    valid++;if(common)commonValid++;
                    var first=generated.getPieces().stream().filter(p->p instanceof St03AssemblyPiece).map(p->(St03AssemblyPiece)p).findFirst().orElseThrow();
                    BlockPos origin=first.compoundOrigin();int dx=(origin.getX()-(cp.getMinBlockX()-48))/16,dz=(origin.getZ()-(cp.getMinBlockZ()-48))/16;
                    if(Math.abs(dx)>2||Math.abs(dz)>2)throw new AssertionError("Selected site outside N=2");
                    var context=new Structure.GenerationContext(level.registryAccess(),gen,gen.getBiomeSource(),sampler,rs,level.getStructureTemplateManager(),level.getSeed(),cp,level,structure.biomes()::contains);
                    var evaluated=St03Eligibility.evaluate(new St03HeadquartersStructure.GeneratorTerrain(context),origin.getX(),origin.getZ());
                    if(!evaluated.eligible()||evaluated.foundationY()!=origin.getY())throw new AssertionError("Actual start does not meet V2");
                    Map<String,Object> row=new LinkedHashMap<>();row.put("candidate_chunk_x",cp.x());row.put("candidate_chunk_z",cp.z());row.put("region_x",rx);row.put("region_z",rz);
                    row.put("origin_x",origin.getX());row.put("origin_z",origin.getZ());row.put("foundation_y",origin.getY());row.put("common_rectangle",common);
                    row.put("dx_chunks",dx);row.put("dz_chunks",dz);row.put("pieces",generated.getPieces().size());row.put("rotation",first.compoundRotation().name());
                    row.put("cut",evaluated.cut());row.put("skirt",evaluated.skirt());row.put("fill",evaluated.footingFill());starts.add(row);
                    out.write(level.getSeed()+","+rx+","+rz+","+cp.x()+","+cp.z()+",true,"+origin.getX()+","+origin.getY()+","+origin.getZ()+","+dx+","+dz+","+generated.getPieces().size()+","+common+"\n");
                }
                out.flush();LoggerFactory.getLogger("st03-natural-v2").info("ST03_NATURAL_PROGRESS seed={} region_x={} potential={} valid={} seconds={}",level.getSeed(),rx,potential,valid,(System.nanoTime()-started)/1e9);
                Files.writeString(dir.resolve("progress.json"),JSON.toJson(Map.of("seed",level.getSeed(),"potential",potential,"valid",valid,"seconds",(System.nanoTime()-started)/1e9)));
            }
        }
        result.put("potential_candidates",potential);result.put("placement_pass",placementPass);result.put("valid_starts",valid);
        result.put("acceptance_pct",100.0*valid/potential);result.put("starts_per_64km2",valid*64000000.0/AREA);
        result.put("delta_vs_prediction_pct",100*(valid*64000000.0/AREA-PREDICTED)/PREDICTED);result.put("starts",starts);
        result.put("common_potential_candidates",commonPotential);result.put("common_valid_starts",commonValid);result.put("common_starts_per_64km2",commonValid*64000000.0/COMMON_AREA);
        result.put("scan_seconds",(System.nanoTime()-started)/1e9);
        if(potential!=2601||starts.isEmpty())throw new AssertionError("Incomplete suite or no actual starts");
    }
    private static final class Session {
        final MinecraftServer server;final ServerLevel level;final Path dir;final Map<String,Object> result;final long started;
        final List<Map<String,Object>> targets,verified=new ArrayList<>();final ServerPlayer player;
        int index,phase,ticks;Map<String,Object> current;St03HeadquartersData.HqInstance hq;List<ChunkPos> forced=new ArrayList<>();
        @SuppressWarnings("unchecked") Session(MinecraftServer server,Path dir,Map<String,Object> result,long started) {
            this.server=server;level=server.overworld();this.dir=dir;this.result=result;this.started=started;
            List<Map<String,Object>> starts=(List<Map<String,Object>>)result.get("starts");int count=level.getSeed()==10079?3:1;
            if(starts.size()<count)throw new AssertionError("Too few natural starts");targets=List.copyOf(starts.subList(0,count));result.put("full_generation",verified);
            var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"st03-probe"),false);
            player=new ServerPlayer(server,level,cookie.gameProfile(),cookie.clientInformation());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);server.getPlayerList().placeNewPlayer(connection,player,cookie);
            player.setGameMode(GameType.SPECTATOR);player.teleportTo(0,200,0);
        }
        void tick() {
            if(++ticks>400)throw new AssertionError("Natural completion/proximity timed out at target "+index+" phase "+phase);
            if(phase==0) {prepare();phase=1;ticks=0;return;}
            if(phase==1) {
                if(hq==null||!hq.structureCompleted)return;
                if(hq.garrisonInitialized||defenders()!=0)throw new AssertionError("Garrison spawned before first proximity");
                current.put("completion_record_created",true);current.put("structure_completed",true);current.put("pre_proximity_defenders",0);
                player.setGameMode(GameType.SURVIVAL);player.getAbilities().invulnerable=true;player.onUpdateAbilities();
                player.teleportTo(hq.origin.getX()+56.5,hq.origin.getY()+1,hq.origin.getZ()+56.5);
                phase=2;ticks=0;return;
            }
            if(!hq.garrisonInitialized)return;
            int defenders=defenders();current.put("garrison_initialized",true);current.put("defenders_first_proximity",defenders);
            current.put("hq_id",hq.id());current.put("committed_chunks",hq.committedChunks.size());
            if(defenders!=40)throw new AssertionError("Natural first proximity must spawn 40 defenders, got "+defenders);
            Map<String,Integer> types=new TreeMap<>();
            for(Mob mob:level.getEntitiesOfClass(Mob.class,new AABB(hq.box.minX(),hq.box.minY(),hq.box.minZ(),hq.box.maxX()+1,hq.box.maxY()+1,hq.box.maxZ()+1).inflate(32),this::ownsDefender))
                types.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),1,Integer::sum);
            current.put("defender_types",types);verified.add(current);
            player.setGameMode(GameType.SPECTATOR);player.teleportTo(0,200,0);
            for(var cp:forced)level.setChunkForced(cp.x(),cp.z(),false);forced.clear();
            if(++index==targets.size()) {finish(null);return;}phase=0;ticks=0;
        }
        boolean ownsDefender(Mob mob) {
            return mob.isAlive()&&mob.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX+hq.id())
                    &&!mob.entityTags().contains(St03GarrisonManager.TAG_PRISONER);
        }
        int defenders(){return level.getEntitiesOfClass(Mob.class,new AABB(hq.box.minX(),hq.box.minY(),hq.box.minZ(),hq.box.maxX()+1,hq.box.maxY()+1,hq.box.maxZ()+1).inflate(32),this::ownsDefender).size();}
        void prepare() {
            var target=targets.get(index);int cx=((Number)target.get("candidate_chunk_x")).intValue(),cz=((Number)target.get("candidate_chunk_z")).intValue();
            var structure=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(St03Worldgen.STRUCTURE_ID);
            var actual=level.getChunk(cx,cz,ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
            if(actual==null||!actual.isValid())throw new AssertionError("Actual chunk pipeline start absent");
            var modules=actual.getPieces().stream().filter(p->p instanceof St03AssemblyPiece).map(p->(St03AssemblyPiece)p).toList();
            if(modules.size()!=34||actual.getPieces().size()!=35)throw new AssertionError("Terrain correction plus 34 unchanged modules required");
            BlockPos origin=modules.getFirst().compoundOrigin();int chunks=0,maxRadius=0;
            for(int x=origin.getX()>>4;x<=(origin.getX()+111)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+111)>>4;z++) {
                level.setChunkForced(x,z,true);forced.add(new ChunkPos(x,z));level.getChunk(x,z,ChunkStatus.FULL);chunks++;
                maxRadius=Math.max(maxRadius,Math.max(Math.abs(x-cx),Math.abs(z-cz)));
            }
            if(maxRadius>8)throw new AssertionError("Structure exceeds vanilla reference radius");
            hq=St03HeadquartersData.get(level).instances().get(new ChunkPos(cx,cz).pack());
            if(hq==null||hq.origin==null||!hq.origin.equals(origin))throw new AssertionError("Completion did not record natural HQ origin");
            int unsupported=0,wet=0,airBelow=0,waterBelow=0,planks=0;
            for(int x=origin.getX();x<origin.getX()+112;x++)for(int z=origin.getZ();z<origin.getZ()+112;z++) {
                var block=level.getBlockState(new BlockPos(x,origin.getY()-1,z));
                if(!block.isSolidRender())unsupported++;if(!block.getFluidState().isEmpty())wet++;
                for(int dy=1;dy<=16;dy++) {
                    var below=level.getBlockState(new BlockPos(x,origin.getY()-dy,z));
                    if(below.isAir())airBelow++;if(!below.getFluidState().isEmpty())waterBelow++;
                }
                for(int dy=1;dy<=26;dy++)if(level.getBlockState(new BlockPos(x,origin.getY()+dy,z)).is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS))planks++;
            }
            current=new LinkedHashMap<>(target);current.put("actual_start_valid",true);current.put("assembly_pieces",modules.size());current.put("total_pieces",actual.getPieces().size());
            current.put("full_chunks_generated",chunks);current.put("max_chunk_radius",maxRadius);current.put("support_plane_columns",112*112);
            current.put("unsupported_columns",unsupported);current.put("wet_columns",wet);current.put("current_air_cells_below_1_16",airBelow);current.put("current_water_cells_below_1_16",waterBelow);
            current.put("placed_spruce_planks_above_foundation",planks);current.put("overlaps",overlaps(actual,hq.box));
            if(unsupported!=0||wet!=0||planks==0)throw new AssertionError("Natural template placement/support validation failed: "+current);
        }
        List<String> overlaps(StructureStart own,net.minecraft.world.level.levelgen.structure.BoundingBox box) {
            Set<String> hits=new TreeSet<>();Set<String> visited=new HashSet<>();var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)
                for(var refs:level.getChunk(x,z).getAllReferences().entrySet()) {
                    if(refs.getKey() instanceof St03HeadquartersStructure)continue;
                    for(long packed:refs.getValue()) {
                        String key=registry.getKey(refs.getKey())+"@"+packed;if(!visited.add(key))continue;
                        var pos=ChunkPos.unpack(packed);var other=level.getChunk(pos.x(),pos.z(),ChunkStatus.STRUCTURE_STARTS).getStartForStructure(refs.getKey());
                        if(other!=null&&other.isValid()&&other.getBoundingBox().intersects(box))hits.add(registry.getKey(refs.getKey())+"@"+pos.x()+","+pos.z());
                    }
                }
            return List.copyOf(hits);
        }
        void finish(Throwable error) {
            if(error==null) {
                try {
                    server.getPlayerList().remove(player);server.saveEverything(true,true,true);result.put("world_saved",true);
                    writeCapturePlan(dir,result);result.put("full_generation_status","PASS");
                }catch(Throwable t){error=t;}
            }
            writeAndExit(server,dir,result,started,error);
        }
    }
}
