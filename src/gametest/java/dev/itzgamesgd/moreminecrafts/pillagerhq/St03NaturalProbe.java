package dev.itzgamesgd.moreminecrafts.pillagerhq;

import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedWriter;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.LoggerFactory;

/** Opt-in real overworld, actual engine starts at 66/22, same physical area and seed suite as V2. */
public final class St03NaturalProbe implements ModInitializer {
    private static final int MIN_CHUNK=-1200,MAX_CHUNK=1248;
    private static final long AREA=2448L*16*2448*16;
    private static final double PREDICTED=64000000.0*(1739.0/26010)/(256*66*66);
    @Override public void onInitialize() {
        String output=System.getProperty("mm.st03.natural.out");if(output==null)return;
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            Map<String,Object> result=new LinkedHashMap<>();long start=System.nanoTime();
            result.put("head_sha",System.getenv("GITHUB_SHA"));result.put("run_id",System.getenv("GITHUB_RUN_ID"));
            result.put("seed",server.overworld().getSeed());
            try { run(server.overworld(),Path.of(output),result);result.put("status","PASS"); }
            catch(Throwable t) { LoggerFactory.getLogger("st03-natural-v2").error("Natural placement probe failed",t);
                result.put("status","FAIL");result.put("error",t.toString());result.put("stack",Arrays.toString(t.getStackTrace())); }
            result.put("total_seconds",(System.nanoTime()-start)/1e9);
            try { Files.createDirectories(Path.of(output));Files.writeString(Path.of(output,"result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)); }
            catch(Exception e) { throw new RuntimeException(e); }
            Runtime.getRuntime().halt("PASS".equals(result.get("status"))?0:1);
        });
    }
    private static void run(ServerLevel level,Path dir,Map<String,Object> result) throws Exception {
        Files.createDirectories(dir);
        if(!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator gen))throw new IllegalStateException("Expected normal noise world");
        var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var holder=registry.getOrThrow(ResourceKey.create(Registries.STRUCTURE,St03Worldgen.STRUCTURE_ID));
        Structure structure=holder.value();
        var set=level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET,St03Worldgen.STRUCTURE_ID)).value();
        var placement=(RandomSpreadStructurePlacement)set.placement();
        if(placement.spacing()!=66 || placement.separation()!=22 || !placement.spreadType().getSerializedName().equals("triangular"))throw new AssertionError("66/22 TRIANGULAR required");
        var state=level.getChunkSource().getGeneratorState();var rs=level.getChunkSource().randomState();
        var sampler=rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
        result.put("generator",gen.getClass().getName());result.put("spacing",66);result.put("separation",22);result.put("salt",91403);
        result.put("min_chunk_inclusive",MIN_CHUNK);result.put("max_chunk_exclusive",MAX_CHUNK);result.put("area_blocks_squared",AREA);
        result.put("predicted_per_64km2",PREDICTED);
        int potential=0,placementPass=0,valid=0;
        List<Map<String,Object>> starts=new ArrayList<>();long started=System.nanoTime();
        try(BufferedWriter out=Files.newBufferedWriter(dir.resolve("starts.csv"))) {
            out.write("seed,candidate_chunk_x,candidate_chunk_z,valid,origin_x,foundation_y,origin_z,dx_chunks,dz_chunks,pieces\n");
            for(int rx=Math.floorDiv(MIN_CHUNK,66);rx<=Math.floorDiv(MAX_CHUNK-1,66);rx++) {
                for(int rz=Math.floorDiv(MIN_CHUNK,66);rz<=Math.floorDiv(MAX_CHUNK-1,66);rz++) {
                    ChunkPos cp=placement.getPotentialStructureChunk(level.getSeed(),rx*66,rz*66);
                    if(cp.x()<MIN_CHUNK||cp.x()>=MAX_CHUNK||cp.z()<MIN_CHUNK||cp.z()>=MAX_CHUNK)continue;
                    potential++;if(!placement.isStructureChunk(state,cp.x(),cp.z()))continue;placementPass++;
                    StructureStart generated=structure.generate(holder,level.dimension(),level.registryAccess(),gen,gen.getBiomeSource(),sampler,rs,
                            level.getStructureTemplateManager(),level.getSeed(),cp,0,level,structure.biomes()::contains);
                    if(!generated.isValid()) { out.write(level.getSeed()+","+cp.x()+","+cp.z()+",false,,,,,,\n");continue; }
                    valid++;
                    var first=generated.getPieces().stream().filter(p->p instanceof St03AssemblyPiece).map(p->(St03AssemblyPiece)p).findFirst().orElseThrow();
                    BlockPos origin=first.compoundOrigin();int dx=(origin.getX()-(cp.getMinBlockX()-48))/16,dz=(origin.getZ()-(cp.getMinBlockZ()-48))/16;
                    if(Math.abs(dx)>2||Math.abs(dz)>2)throw new AssertionError("Selected site outside N=2");
                    var context=new Structure.GenerationContext(level.registryAccess(),gen,gen.getBiomeSource(),sampler,rs,level.getStructureTemplateManager(),level.getSeed(),cp,level,structure.biomes()::contains);
                    var evaluated=St03Eligibility.evaluate(new St03HeadquartersStructure.GeneratorTerrain(context),origin.getX(),origin.getZ());
                    if(!evaluated.eligible()||evaluated.foundationY()!=origin.getY())throw new AssertionError("Actual start does not meet V2");
                    Map<String,Object> row=new LinkedHashMap<>();row.put("candidate_chunk_x",cp.x());row.put("candidate_chunk_z",cp.z());
                    row.put("origin_x",origin.getX());row.put("origin_z",origin.getZ());row.put("foundation_y",origin.getY());
                    row.put("dx_chunks",dx);row.put("dz_chunks",dz);row.put("pieces",generated.getPieces().size());row.put("rotation",first.compoundRotation().name());
                    row.put("cut",evaluated.cut());row.put("skirt",evaluated.skirt());row.put("fill",evaluated.footingFill());starts.add(row);
                    out.write(level.getSeed()+","+cp.x()+","+cp.z()+",true,"+origin.getX()+","+origin.getY()+","+origin.getZ()+","+dx+","+dz+","+generated.getPieces().size()+"\n");
                }
                out.flush();LoggerFactory.getLogger("st03-natural-v2").info("ST03_NATURAL_PROGRESS seed={} region_x={} potential={} valid={} seconds={}",level.getSeed(),rx,potential,valid,(System.nanoTime()-started)/1e9);
                Files.writeString(dir.resolve("progress.json"),new GsonBuilder().create().toJson(Map.of("seed",level.getSeed(),"potential",potential,"valid",valid,"seconds",(System.nanoTime()-started)/1e9)));
            }
        }
        result.put("potential_candidates",potential);result.put("placement_pass",placementPass);result.put("valid_starts",valid);
        result.put("acceptance_pct",100.0*valid/potential);result.put("starts_per_64km2",valid*64000000.0/AREA);
        result.put("delta_vs_prediction_pct",100*(valid*64000000.0/AREA-PREDICTED)/PREDICTED);result.put("starts",starts);
        result.put("scan_seconds",(System.nanoTime()-started)/1e9);
        if(starts.isEmpty())throw new AssertionError("No actual starts in seed");
        Map<String,Object> target=starts.getFirst();int cx=(int)target.get("candidate_chunk_x"),cz=(int)target.get("candidate_chunk_z");
        var chunk=level.getChunk(cx,cz,ChunkStatus.STRUCTURE_STARTS);var actual=chunk.getStartForStructure(structure);
        if(actual==null||!actual.isValid())throw new AssertionError("Engine start missing in actual chunk pipeline");
        long assembly=actual.getPieces().stream().filter(p->p instanceof St03AssemblyPiece).count();
        if(assembly!=34)throw new AssertionError("34 approved assembly modules required");
        var first=actual.getPieces().stream().filter(p->p instanceof St03AssemblyPiece).map(p->(St03AssemblyPiece)p).findFirst().orElseThrow();
        BlockPos origin=first.compoundOrigin();int chunks=0;
        for(int x=origin.getX()>>4;x<=(origin.getX()+111)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+111)>>4;z++) {level.getChunk(x,z,ChunkStatus.FULL);chunks++;}
        // The terrain support plane lies below every unchanged template, so architectural floors/roofs don't confound it.
        int unsupported=0,wet=0;
        for(int x=origin.getX();x<origin.getX()+112;x++)for(int z=origin.getZ();z<origin.getZ()+112;z++) {
            var block=level.getBlockState(new BlockPos(x,origin.getY()-1,z));
            if(!block.isSolidRender())unsupported++;if(!block.getFluidState().isEmpty())wet++;
        }
        Map<String,Object> verification=new LinkedHashMap<>();verification.put("candidate_chunk_x",cx);verification.put("candidate_chunk_z",cz);
        verification.put("actual_start_valid",true);verification.put("assembly_pieces",assembly);verification.put("total_pieces",actual.getPieces().size());
        verification.put("full_chunks_generated",chunks);verification.put("support_plane_columns",112*112);verification.put("unsupported_columns",unsupported);verification.put("wet_columns",wet);
        result.put("full_generation",verification);
        if(unsupported!=0||wet!=0)throw new AssertionError("Support plane has missing ground or water");
    }
}
