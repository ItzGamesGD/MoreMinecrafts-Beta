package dev.itzgamesgd.moreminecrafts.pillagerhq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedWriter;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.block.Rotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static dev.itzgamesgd.moreminecrafts.pillagerhq.St03SiteV2Rules.*;

/** Opt-in dedicated-server probe. Uses the running level's seed and vanilla noise generator. */
public final class St03SiteV2Probe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st03-site-v2");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().setObjectToNumberStrategy(com.google.gson.ToNumberPolicy.LONG_OR_DOUBLE).create();
    private static final int[] RADII = {0, 2, 4, 8};
    private static final List<String> VERDICTS = List.of("PASS", "CENTER_BIOME", "FOOTPRINT_BIOME", "WATER", "HEIGHT", "CUT_SKIRT", "FOOTING_FILL");
    private record Config(int set, int coverage, int water, boolean edges, boolean robust, Limits limits) {
        String id() { return "S" + set + "_B" + coverage + "_W" + water + "_E" + (edges ? 1 : 0)
                + "_" + (robust ? "P" : "R") + "_" + limits.name(); }
    }
    private static List<Config> configs() {
        List<Config> out = new ArrayList<>();
        for (int set = 0; set < 3; set++) for (int b : new int[]{70, 80})
            for (Limits l : LIMITS) for (int w : new int[]{0, 5}) out.add(new Config(set, b, w, true, true, l));
        // Controlled ablations: edge treatment, raw height spread, and each earthwork parameter separately.
        for (int set = 0; set < 3; set++) for (int b : new int[]{70, 80}) {
            out.add(new Config(set, b, 5, false, true, LIMITS[2]));
            out.add(new Config(set, b, 5, true, false, LIMITS[2]));
            out.add(new Config(set, b, 5, true, true, new Limits("CUT3",20,3,12,16)));
            out.add(new Config(set, b, 5, true, true, new Limits("SKIRT5",20,10,5,16)));
            out.add(new Config(set, b, 5, true, true, new Limits("FILL6",20,10,12,6)));
        }
        return out;
    }
    private static Config searchConfig(int set, int b) { return new Config(set, b, 5, true, true, LIMITS[2]); }
    private static Decision decide(Sample s, Config c) {
        return evaluate(s, c.set, c.coverage, c.water, c.edges, c.robust, c.limits);
    }
    @Override public void onInitialize() {
        String dir = System.getProperty("mm.st03.v2.out");
        if (dir == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String,Object> out = new LinkedHashMap<>();
            try {
                String verificationInput = System.getProperty("mm.st03.v2.verify.input");
                if (verificationInput == null) run(server, Path.of(dir), out);
                else rerunVerification(server, Path.of(verificationInput), out);
                out.put("status", "PASS");
            } catch (Throwable e) {
                LOG.error("ST03 V2 failed", e);
                out.put("status", "FAIL"); out.put("error", e.toString());
                out.put("stack", Arrays.toString(e.getStackTrace()));
            }
            try { Files.createDirectories(Path.of(dir)); Files.writeString(Path.of(dir, "summary.json"), JSON.toJson(out)); }
            catch (Exception e) { throw new RuntimeException(e); }
            Runtime.getRuntime().halt("PASS".equals(out.get("status")) ? 0 : 1);
        });
    }
    /** Rerun only affected chunk checks, retaining the already completed, unchanged numerical experiment. */
    @SuppressWarnings("unchecked")
    private static void rerunVerification(MinecraftServer server, Path input, Map<String,Object> out) throws Exception {
        selfCheck();
        Map<String,Object> measured = JSON.fromJson(Files.readString(input), Map.class);
        if (!"PASS".equals(measured.get("status"))) throw new IllegalStateException("Incomplete source measurement");
        if (((Number)measured.get("seed")).longValue() != server.overworld().getSeed()) throw new IllegalStateException("Source seed mismatch");
        out.putAll(measured);
        out.put("measurement_head_sha", measured.get("head_sha"));
        out.put("measurement_run_id", measured.get("run_id"));
        out.put("head_sha", System.getenv("GITHUB_SHA"));
        out.put("run_id", System.getenv("GITHUB_RUN_ID"));
        out.put("mode", "verify-only");
        List<Map<String,Object>> profiles = (List<Map<String,Object>>)out.get("profiles");
        for (Map<String,Object> p : profiles) p.remove("generated_bare_ground_section");
        long start = System.nanoTime();
        out.put("verification", verify(server.overworld(), new Terrain(server.overworld()),
                (List<Map<String,Object>>)out.get("verification_targets"), profiles));
        out.put("verification_seconds", (System.nanoTime()-start)/1e9);
        out.put("total_seconds", ((Number)out.get("measurement_elapsed_seconds")).doubleValue()
                + ((Number)out.get("baseline_seconds")).doubleValue() + ((Number)out.get("verification_seconds")).doubleValue());
        for (Map<String,Object> p : profiles) {
            if (((List<?>)p.get("generated_bare_solid_section")).size() != 15) throw new AssertionError("Section length");
        }
    }
    private static final class Terrain implements St03Eligibility.Terrain {
        final ServerLevel level;
        final NoiseBasedChunkGenerator gen;
        final RandomState rs;
        final BiomeResolver resolver;
        final Map<Long,Integer> surfaces = new HashMap<>(), floors = new HashMap<>();
        final Map<Long,Integer> biomeSets = new HashMap<>();
        long columns;
        Terrain(ServerLevel level) {
            this.level = level;
            if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator n)) throw new IllegalStateException("not noise generator");
            gen = n; rs = level.getChunkSource().randomState();
            resolver = gen.getBiomeSource().createResolver(rs.createClimateSampler(SamplerContext.builder().enableCaches().build()));
        }
        static long key(int x,int z) { return ((long)x << 32) ^ (z & 0xffffffffL); }
        void clear() { surfaces.clear(); floors.clear(); biomeSets.clear(); }
        @Override public int surfaceHeight(int x,int z) {
            return surfaces.computeIfAbsent(key(x,z), k -> { columns++; return gen.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,rs); });
        }
        @Override public int oceanFloorHeight(int x,int z) {
            return floors.computeIfAbsent(key(x,z), k -> gen.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,level,rs));
        }
        @Override public boolean isEligibleBiomeCell(int qx,int qy,int qz) {
            return St03HeadquartersStructure.isEligibleBiome(resolver.getNoiseBiome(qx,qy,qz));
        }
        String biome(int qx,int qy,int qz) {
            return resolver.getNoiseBiome(qx,qy,qz).unwrapKey().orElseThrow().identifier().getPath();
        }
        int columnSet(int x,int z) {
            return biomeSets.computeIfAbsent(key(x,z), k -> {
                int qy = Math.floorDiv(surfaceHeight(x,z)-3,4), qx = Math.floorDiv(x,4), qz = Math.floorDiv(z,4);
                String a = biome(qx,qy,qz), b = biome(qx,qy+1,qz);
                for (int set=0;set<3;set++) if (allowed(a,set) && allowed(b,set)) return set;
                if (EDGE.contains(a) && EDGE.contains(b)) return 3;
                return 4;
            });
        }
        Sample sample(ChunkPos cp, int step) {
            int ox=cp.getMinBlockX()-48, oz=cp.getMinBlockZ()-48;
            int center = columnSet(ox+56,oz+56);
            if (center>2) return new Sample(center,new int[3],0,225,0,0,0,0,0,0,0,0,new int[0]);
            // Identical biome sample columns to St03Eligibility (15x15, both adjacent vertical quart cells).
            int[] good = new int[3]; int edge=0, count=0;
            for (int x=ox-2;x<=ox+110;x+=8) for (int z=oz-2;z<=oz+110;z+=8) {
                int b=columnSet(x,z); count++;
                if (b==3) edge++;
                for(int set=0;set<3;set++) if(b<=set) good[set]++;
            }
            // Skip height population only when even the most permissive measured coverage cannot pass.
            if (100L*good[2] < 70L*(count-edge)) return new Sample(center,good,edge,count,0,0,0,0,0,0,0,0,new int[0]);
            int[] offsets=offsets(step); int[] heights=new int[offsets.length*offsets.length];
            int i=0, water=0, floorMin=Integer.MAX_VALUE;
            for (int dx:offsets) for(int dz:offsets) {
                int s=surfaceHeight(ox+dx,oz+dz), f=oceanFloorHeight(ox+dx,oz+dz);
                heights[i++]=s; if(s>f+1) water++; floorMin=Math.min(floorMin,f);
            }
            int[] sorted=sorted(heights);
            return new Sample(center,good,edge,count,water,heights.length,sorted[0],percentile(sorted,.05),
                    sorted[sorted.length/2],percentile(sorted,.95),sorted[sorted.length-1],floorMin,heights);
        }
    }
    private static int[] offsets(int step) {
        int[] out=new int[112/step+1]; for(int i=0;i<out.length-1;i++) out[i]=i*step; out[out.length-1]=111; return out;
    }
    private static Map<String,Integer> zeroCounts() {
        Map<String,Integer> out=new LinkedHashMap<>(); for(String v:VERDICTS) out.put(v,0); return out;
    }
    private static void run(MinecraftServer server, Path dir, Map<String,Object> out) throws Exception {
        selfCheck();
        Files.createDirectories(dir); long started=System.nanoTime(); ServerLevel level=server.overworld();
        out.put("rules_self_checks", "PASS");
        Terrain t=new Terrain(level); long seed=level.getSeed();
        out.put("seed",seed); out.put("head_sha",System.getenv("GITHUB_SHA")); out.put("run_id",System.getenv("GITHUB_RUN_ID"));
        out.put("generator",t.gen.getClass().getName()); out.put("height_step",8); out.put("legacy_height_step",4);
        out.put("region_radius",25); out.put("search_radii",RADII); out.put("limits",LIMITS);
        out.put("footprint_offsets",offsets(8)); out.put("percentile_method","nearest rank ceil(n*p)-1");
        out.put("water_definition","WORLD_SURFACE_WG > OCEAN_FLOOR_WG + 1; fraction <= 0 or 5 percent");
        out.put("neutral_edge_definition","eligible/(all-edge) >= threshold; center always strictly eligible; edges never eligible");
        out.put("search_selection","exhaustive integer chunk offsets; eligible first, minimum max(cut,skirt,fill), then raw spread, distance, dx,dz");
        var sets=level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        var placement=(RandomSpreadStructurePlacement)sets.getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET,St03Worldgen.STRUCTURE_ID)).value().placement();
        if(placement.spacing()!=48) throw new IllegalStateException("unexpected base spacing");
        List<Config> configs=configs();
        Map<String,Map<String,Integer>> counts=new LinkedHashMap<>(); for(Config c:configs) counts.put(c.id(),zeroCounts());
        Map<String,Integer> legacy=new TreeMap<>();
        Map<String,Map<String,Integer>> searchCounts=new LinkedHashMap<>();
        for(int set=0;set<3;set++) for(int b:new int[]{70,80}) for(int n:RADII) searchCounts.put(searchConfig(set,b).id()+"_N"+n,zeroCounts());
        List<Map<String,Object>> verifyTargets=new ArrayList<>(); List<Map<String,Object>> profiles=new ArrayList<>();
        List<Map<String,Object>> pairedResolution=new ArrayList<>();
        int legacyBioWater=0, legacyLe12=0, total=0; long searchLocations=0;
        try(BufferedWriter dump=Files.newBufferedWriter(dir.resolve("candidates.csv"));
            BufferedWriter selected=Files.newBufferedWriter(dir.resolve("search.csv"));
            BufferedWriter oldDump=Files.newBufferedWriter(dir.resolve("legacy-height.csv"))) {
            dump.write("seed,rx,rz,cx,cz,center_set,good_s0,good_s1,good_s2,edge,biome_n,water,height_n,min,p05,p50,p95,max,floor_min\n");
            selected.write("seed,rx,rz,set,coverage,N,verdict,cx,cz,foundation,cut,skirt,fill\n");
            oldDump.write("seed,rx,rz,cx,cz,min,p05,p50,p95,max,water,height_n\n");
            for(int rx=-25;rx<=25;rx++) for(int rz=-25;rz<=25;rz++) {
                t.clear(); ChunkPos base=placement.getPotentialStructureChunk(seed,rx*48,rz*48); total++;
                int ox=base.getMinBlockX()-48, oz=base.getMinBlockZ()-48;
                St03Eligibility.Result old=St03Eligibility.evaluate(t,ox,oz); legacy.merge(old.verdict().name(),1,Integer::sum);
                Sample s=t.sample(base,8);
                dump.write(seed+","+rx+","+rz+","+base.x()+","+base.z()+","+s.centerSet()+","+s.good()[0]+","+s.good()[1]+","+s.good()[2]+","+s.edge()+","+s.biomeSamples()+","+s.water()+","+s.heightSamples()+","+s.min()+","+s.p05()+","+s.median()+","+s.p95()+","+s.max()+","+s.floorMin()+"\n");
                for(Config c:configs) counts.get(c.id()).merge(decide(s,c).verdict(),1,Integer::sum);
                if(old.verdict()!=St03Eligibility.Verdict.REJECT_BIOME && old.verdict()!=St03Eligibility.Verdict.REJECT_WATER) {
                    Sample dense=t.sample(base,4); legacyBioWater++;
                    if(dense.rawSpread()<=12) legacyLe12++;
                    oldDump.write(seed+","+rx+","+rz+","+base.x()+","+base.z()+","+dense.min()+","+dense.p05()+","+dense.median()+","+dense.p95()+","+dense.max()+","+dense.water()+","+dense.heightSamples()+"\n");
                    pairedResolution.add(Map.of("chunk_x",base.x(),"chunk_z",base.z(),"raw4",dense.rawSpread(),"raw8",s.rawSpread(),"p4",dense.robustSpread(),"p8",s.robustSpread()));
                }
                if(profiles.size()<3 && s.heightSamples()>0 && s.centerSet()==0 &&
                        Set.of("plains","sunflower_plains").contains(t.biome((ox+56)>>2,(t.surfaceHeight(ox+56,oz+56)-1)>>2,(oz+56)>>2)) &&
                        biomeVerdict(s,0,70,true).equals("PASS") && s.water()*100L<=5L*s.heightSamples()) {
                    profiles.add(new LinkedHashMap<>(Map.of("seed",seed,"chunk_x",base.x(),"chunk_z",base.z(),"origin_x",ox,"origin_z",oz,
                            "offsets",offsets(8),"raw_surface_x_rows_z_columns",matrix(s.surfaceHeights(),15),"raw_spread",s.rawSpread(),"p05_p95_spread",s.robustSpread())));
                }
                // Six independent profiles, nested square radii. Every offset is evaluated; a site can serve several policies.
                Sample[][] samples=new Sample[17][17]; samples[8][8]=s;
                int[][][] bestScore=new int[6][4][1]; // scores stored separately to avoid retaining whole height arrays
                Decision[][] best=new Decision[6][4]; ChunkPos[][] bestPos=new ChunkPos[6][4];
                for(int dx=-8;dx<=8;dx++) for(int dz=-8;dz<=8;dz++) {
                    Sample q=samples[dx+8][dz+8]; ChunkPos cp=new ChunkPos(base.x()+dx,base.z()+dz);
                    if(q==null) q=t.sample(cp,8); searchLocations++;
                    int distance=Math.max(Math.abs(dx),Math.abs(dz));
                    int ci=0;
                    for(int set=0;set<3;set++) for(int b:new int[]{70,80}) {
                        Decision d=decide(q,searchConfig(set,b));
                        int stage=VERDICTS.indexOf(d.verdict());
                        // Lowest score = pass; otherwise deepest successful stage is preferred. No failed policy becomes a pass.
                        int quality=d.pass()? Math.max(d.cut(),Math.max(d.skirt(),d.fill()))*100000+q.rawSpread()*1000+distance*10
                                : 100000000+(VERDICTS.size()-stage)*100000;
                        for(int ni=0;ni<4;ni++) if(distance<=RADII[ni] && (best[ci][ni]==null || quality<bestScore[ci][ni][0])) {
                            best[ci][ni]=d; bestPos[ci][ni]=cp; bestScore[ci][ni][0]=quality;
                        }
                        ci++;
                    }
                }
                int ci=0;
                for(int set=0;set<3;set++) for(int b:new int[]{70,80}) {
                    for(int ni=0;ni<4;ni++) {
                        Decision d=best[ci][ni]; ChunkPos cp=bestPos[ci][ni];
                        searchCounts.get(searchConfig(set,b).id()+"_N"+RADII[ni]).merge(d.verdict(),1,Integer::sum);
                        selected.write(seed+","+rx+","+rz+",S"+set+","+b+","+RADII[ni]+","+d.verdict()+","+cp.x()+","+cp.z()+","+d.foundation()+","+d.cut()+","+d.skirt()+","+d.fill()+"\n");
                        // Preserve an unshifted S0 start for the production-vs-proposed chunk check.
                        if(set==0 && b==80 && ni==0 && d.pass() && verifyTargets.size()<1)
                            verifyTargets.add(Map.of("chunk_x",cp.x(),"chunk_z",cp.z(),"foundation",d.foundation(),"policy",searchConfig(set,b).id()));
                    }
                    ci++;
                }
                if(total%25==0) { Files.writeString(dir.resolve("progress.json"), JSON.toJson(Map.of("seed",seed,"anchors",total,"columns",t.columns,"seconds",(System.nanoTime()-started)/1e9))); dump.flush(); selected.flush(); oldDump.flush(); LOG.info("ST03_V2_PROGRESS seed={} anchors={}/2601 columns={} elapsed_s={}",seed,total,t.columns,(System.nanoTime()-started)/1e9); }
            }
        }
        out.put("candidates",total); out.put("configs",configs); out.put("counts",counts); out.put("search_counts",searchCounts);
        out.put("legacy_counts",legacy); out.put("legacy_biome_water_pass",legacyBioWater); out.put("legacy_raw_le12",legacyLe12);
        out.put("legacy_resolution_pairs",pairedResolution); out.put("search_locations",searchLocations); out.put("height_columns",t.columns);
        out.put("profiles",profiles); out.put("verification_targets",verifyTargets);
        out.put("measurement_elapsed_seconds",(System.nanoTime()-started)/1e9);
        Files.writeString(dir.resolve("measurement.json"),JSON.toJson(out));
        long bt=System.nanoTime(); out.put("baselines",baselines(level,t)); out.put("baseline_seconds",(System.nanoTime()-bt)/1e9);
        Files.writeString(dir.resolve("measurement.json"),JSON.toJson(out));
        long vt=System.nanoTime(); out.put("verification",verify(level,t,verifyTargets,profiles)); out.put("verification_seconds",(System.nanoTime()-vt)/1e9);
        out.put("total_seconds",(System.nanoTime()-started)/1e9);
    }
    private static void selfCheck() {
        Sample s=new Sample(0,new int[]{70,80,90},10,100,5,100,60,62,68,74,76,58,new int[100]);
        if(!biomeVerdict(s,0,70,true).equals("PASS") || !biomeVerdict(s,0,80,true).equals("FOOTPRINT_BIOME")) throw new AssertionError("neutral edge coverage");
        if(!biomeVerdict(s,0,70,false).equals("PASS") || !biomeVerdict(s,0,71,false).equals("FOOTPRINT_BIOME")) throw new AssertionError("raw denominator boundary");
        if(!evaluate(s,0,70,5,true,true,LIMITS[2]).pass()) throw new AssertionError("exact 5% water");
        if(!evaluate(s,0,70,0,true,true,LIMITS[2]).verdict().equals("WATER")) throw new AssertionError("water exclusion");
        Sample slope=new Sample(0,new int[]{100,100,100},0,100,0,100,60,61,61,72,72,60,new int[100]);
        Decision d=evaluate(slope,0,80,5,true,true,new Limits("test",20,4,8,8));
        if(!d.pass() || d.foundation()!=68) throw new AssertionError("feasible nonmedian foundation");
        Sample pit=new Sample(0,new int[]{100,100,100},0,100,0,100,40,65,66,68,100,40,new int[100]);
        if(evaluate(pit,0,80,5,true,true,LIMITS[2]).pass()) throw new AssertionError("percentile must not hide outlier");
        if(evaluate(slope,0,80,5,true,true,new Limits("test",20,4,8,6)).pass()) throw new AssertionError("footing fill independently constrained");
        if(percentile(new int[]{1,2,3,4,5},.05)!=1 || percentile(new int[]{1,2,3,4,5},.95)!=5) throw new AssertionError("nearest rank");
    }
    private static List<List<Integer>> matrix(int[] data,int n) {
        List<List<Integer>> out=new ArrayList<>(); for(int i=0;i<n;i++) { List<Integer> row=new ArrayList<>(); for(int j=0;j<n;j++) row.add(data[i*n+j]); out.add(row); } return out;
    }
    /** Same physical rectangle as all 51x51 ST03 regions: [-1200,1248) chunks, area 2448^2*256 blocks. */
    private static List<Map<String,Object>> baselines(ServerLevel level,Terrain t) {
        List<Map<String,Object>> out=new ArrayList<>(); var state=level.getChunkSource().getGeneratorState();
        var sampler=t.rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
        for(Holder<StructureSet> h:state.possibleStructureSets()) {
            String id=h.unwrapKey().orElseThrow().identifier().toString();
            if(!Set.of("minecraft:villages","moreminecrafts:st01_pyramid").contains(id)) continue;
            StructureSet set=h.value(); var p=(RandomSpreadStructurePlacement)set.placement();
            int potential=0,placementPass=0,valid=0; Map<String,Integer> byType=new TreeMap<>(); List<Map<String,Object>> starts=new ArrayList<>();
            long start=System.nanoTime();
            for(int rx=Math.floorDiv(-1200,p.spacing());rx<=Math.floorDiv(1247,p.spacing());rx++)
                for(int rz=Math.floorDiv(-1200,p.spacing());rz<=Math.floorDiv(1247,p.spacing());rz++) {
                    ChunkPos cp=p.getPotentialStructureChunk(level.getSeed(),rx*p.spacing(),rz*p.spacing());
                    if(cp.x() < -1200 || cp.x()>=1248 || cp.z() < -1200 || cp.z()>=1248) continue;
                    potential++; if(!p.isStructureChunk(state,cp.x(),cp.z())) continue; placementPass++;
                    List<StructureSet.StructureSelectionEntry> options=new ArrayList<>(set.structures());
                    WorldgenRandom random=new WorldgenRandom(new LegacyRandomSource(0L)); random.setLargeFeatureSeed(level.getSeed(),cp.x(),cp.z());
                    int total=options.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();
                    while(!options.isEmpty()) {
                        int index=0;
                        if(options.size()!=1 || set.structures().size()!=1) {
                            int choice=random.nextInt(total); for(var o:options) { choice-=o.weight(); if(choice<0) break; index++; }
                        }
                        var option=options.get(index); Structure structure=option.structure().value();
                        StructureStart generated=structure.generate(option.structure(),level.dimension(),level.registryAccess(),t.gen,t.gen.getBiomeSource(),sampler,t.rs,
                                level.getStructureTemplateManager(),level.getSeed(),cp,0,level,structure.biomes()::contains);
                        if(generated.isValid()) {
                            valid++; String name=option.structure().unwrapKey().orElseThrow().identifier().toString(); byType.merge(name,1,Integer::sum);
                            starts.add(Map.of("chunk_x",cp.x(),"chunk_z",cp.z(),"structure",name,"pieces",generated.getPieces().size())); break;
                        }
                        total-=option.weight(); options.remove(index);
                    }
                }
            out.add(Map.of("set",id,"spacing",p.spacing(),"potential",potential,"placement_pass",placementPass,"valid_starts",valid,
                    "by_type",byType,"starts",starts,"seconds",(System.nanoTime()-start)/1e9));
            LOG.info("ST03_V2_BASELINE {} valid={} potential={}",id,valid,potential);
        }
        if(out.size()!=2) throw new IllegalStateException("baseline structure sets missing"); return out;
    }
    private static List<Map<String,Object>> verify(ServerLevel level,Terrain t,List<Map<String,Object>> targets,List<Map<String,Object>> profiles) {
        List<Map<String,Object>> out=new ArrayList<>();
        Structure st03=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(St03Worldgen.STRUCTURE_ID);
        // Natural starts are checked unchanged. V2 pass does not imply an unchanged production rule will accept it.
        for(Map<String,Object> target:targets) {
            int cx=((Number)target.get("chunk_x")).intValue(),cz=((Number)target.get("chunk_z")).intValue();
            t.clear(); Sample raw=t.sample(new ChunkPos(cx,cz),8); Decision d=decide(raw,searchConfig(0,80));
            if(!d.pass()) throw new IllegalStateException("verification candidate no longer passes");
            var chunk=level.getChunk(cx,cz,ChunkStatus.STRUCTURE_STARTS); StructureStart natural=chunk.getStartForStructure(st03);
            boolean actual=natural!=null && natural.isValid();
            Map<String,Object> rec=new LinkedHashMap<>(target); rec.put("natural_valid_start",actual);
            rec.put("production_verdict",St03Eligibility.evaluate(t,cx*16-48,cz*16-48).verdict().name());
            level.getChunk(cx,cz,ChunkStatus.FULL); rec.put("natural_chunk_status","FULL");
            rec.put("natural_generation",actual?"PASS":"FAIL");
            out.add(rec);
        }
        // Three actual generated plains sections, paired against getBaseHeight; no synthetic flattening or rule override.
        for(Map<String,Object> profile:profiles) {
            int ox=((Number)profile.get("origin_x")).intValue(),oz=((Number)profile.get("origin_z")).intValue();
            List<Integer> surface=new ArrayList<>(),floor=new ArrayList<>(),raw=new ArrayList<>(),bare=new ArrayList<>();
            for(int dx:offsets(8)) {
                int x=ox+dx,z=oz+56; level.getChunk(x>>4,z>>4,ChunkStatus.FULL);
                surface.add(level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z));
                floor.add(level.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z)); raw.add(t.surfaceHeight(x,z));
                int ground=surface.getLast()-1;
                while(ground>level.getMinY()) {
                    var block=level.getBlockState(new BlockPos(x,ground,z));
                    if(block.isSolidRender() && !block.is(net.minecraft.tags.BlockTags.LOGS) && !block.is(net.minecraft.tags.BlockTags.LEAVES)) break;
                    ground--;
                }
                bare.add(ground+1);
            }
            profile.put("generated_section_x_offsets",offsets(8)); profile.put("generated_section_z_offset",56);
            profile.put("generated_bare_solid_section",bare); profile.put("raw_base_section",raw); profile.put("generated_world_surface_section",surface); profile.put("generated_ocean_floor_section",floor);
        }
        return out;
    }
}
