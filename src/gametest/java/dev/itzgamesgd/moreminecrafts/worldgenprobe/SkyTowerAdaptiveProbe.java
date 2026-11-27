package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.worldgen.structure.AdaptiveJigsawStructure;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validation probe for the adaptive mountain Sky Island placement (not production; inert unless -Dmm.ad.out is set).
 * Compares, on identical placement-selected chunks, the PRODUCTION structures (adaptive) with the baseline legacy minecraft:jigsaw
 * definitions (tools/sky-tower-adaptive/legacy/*.json, -Dmm.ad.legacy=dir). Modes: world (real world generator + natural pipeline
 * cross-check), controlled (in-process flat generator with an exact fixed top block, counting heightmap queries).
 */
public final class SkyTowerAdaptiveProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("sky-tower-adaptive-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String NS = "moreminecrafts";
    static boolean KEYS_LOGGED;
    static final com.sun.management.ThreadMXBean ALLOC = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
    static final AtomicLong HEIGHT_QUERIES = new AtomicLong();

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.ad.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path dir = Path.of(out);
            try {
                Files.createDirectories(dir);
                switch (System.getProperty("mm.ad.mode", "world")) {
                    case "world" -> run(server, dir, false);
                    case "controlled" -> run(server, dir, true);
                    default -> throw new IllegalArgumentException("mode");
                }
            } catch (Throwable t) {
                LOG.error("adaptive probe failed", t);
                try { Files.writeString(dir.resolve("ERROR.txt"), t + "\n"); } catch (IOException ignored) { }
            }
            Runtime.getRuntime().halt(0);
        });
    }

    private static final class Env {
        final String label, mode, world; final ServerLevel level; final ChunkGenerator gen; final BiomeSource bs; final Climate.Sampler climate;
        final RandomState rs; final StructureTemplateManager tm; final RegistryAccess ra; final long seed; final ResourceKey<Level> dim;
        Env(String label, String mode, String world, MinecraftServer server, ChunkGenerator gen) {
            this.label = label; this.mode = mode; this.world = world; this.level = server.overworld(); this.gen = gen; this.bs = gen.getBiomeSource();
            this.rs = level.getChunkSource().randomState(); this.climate = rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
            this.tm = level.getStructureTemplateManager(); this.ra = level.registryAccess(); this.seed = level.getSeed(); this.dim = level.dimension();
        }
        int top(int x, int z) { return gen.getFirstOccupiedHeight(x, z, Types.WORLD_SURFACE_WG, level, rs); }
        String biomeAt(int x, int y, int z) {
            return bs.createResolver(climate).getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)).unwrapKey().map(k -> k.identifier().toString()).orElse("?");
        }
    }

    private static WorldgenRandom rng(long seed, ChunkPos cp) {
        WorldgenRandom r = new WorldgenRandom(new LegacyRandomSource(0L)); r.setLargeFeatureSeed(seed, cp.x(), cp.z()); return r;
    }
    private static String idOf(Holder<Structure> h) { return h.unwrapKey().map(k -> k.identifier().getPath()).orElse("?"); }
    private static String elementName(StructurePoolElement e) {
        if (e instanceof SinglePoolElement s && s.getTemplateLocation() != null) return s.getTemplateLocation().getPath().replace("sky_islands/", "");
        return e == EmptyPoolElement.INSTANCE ? "EMPTY" : "?";
    }

    /** Independent re-derivation of the draws/geometry (same for both configurations: draw 1 uses the 18..70 range). */
    private record Info(boolean empty, int roll, String rotation, String element, int sizeY, int cx, int cz, boolean columnOk, int top, int feasibleMax) {}

    private static Info info(Env e, Structure s, ChunkPos cp) {
        Holder<StructureTemplatePool> pool; int lo = 18, hi = 70; int padTop = 4;
        if (s instanceof JigsawStructure js) pool = (Holder<StructureTemplatePool>) get(js, JigsawStructure.class, "startPool");
        else pool = (Holder<StructureTemplatePool>) get(s, AdaptiveJigsawStructure.class, "startPool");
        GenerationContext ctx = new GenerationContext(e.ra, e.gen, e.bs, e.climate, e.rs, e.tm, e.seed, cp, e.level, s.biomes()::contains);
        WorldgenRandom r = ctx.random();
        int roll = r.nextInt(hi - lo + 1) + lo;
        Rotation rot = Rotation.getRandom(r);
        StructurePoolElement el = pool.value().getRandomTemplate(r);
        if (el == EmptyPoolElement.INSTANCE) return new Info(true, roll, "", "EMPTY", 0, 0, 0, false, 0, 0);
        BoundingBox box = el.getBoundingBox(e.tm, new BlockPos(cp.getMinBlockX(), roll, cp.getMinBlockZ()), rot);
        int cx = (box.maxX() + box.minX()) / 2, cz = (box.maxZ() + box.minZ()) / 2;
        boolean col = ctx.couldStructureExistInColumn(cx, cz, e.level.getMinY(), e.level.getMaxY());
        int top = e.top(cx, cz);
        int fm = AdaptiveJigsawStructure.feasibleMaxOffset(e.level.getMaxY(), padTop, top, box.getYSpan());
        return new Info(false, roll, rot.name(), elementName(el), box.getYSpan(), cx, cz, col, top, fm);
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Object o, Class<?> c, String n) {
        try { var f = c.getDeclaredField(n); f.setAccessible(true); return (T) f.get(o); } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }

    private record Candidate(String setId, StructureSet set, int cx, int cz) {}

    private static List<Candidate> enumerate(MinecraftServer server, int radius) {
        ServerLevel level = server.overworld(); ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState(); long seed = level.getSeed();
        List<Candidate> out = new ArrayList<>();
        for (Holder<StructureSet> h : state.possibleStructureSets()) {
            String id = h.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
            if (!id.startsWith(NS + ":sky_islands") || !(h.value().placement() instanceof RandomSpreadStructurePlacement rsp)) continue;
            int sp = rsp.spacing(), cells = Math.floorDiv(radius, sp) + 1;
            for (int gx = -cells; gx <= cells; gx++) for (int gz = -cells; gz <= cells; gz++) {
                ChunkPos p = rsp.getPotentialStructureChunk(seed, gx * sp, gz * sp);
                if (Math.abs(p.x()) <= radius && Math.abs(p.z()) <= radius && rsp.isStructureChunk(state, p.x(), p.z())) out.add(new Candidate(id.substring(id.indexOf(':') + 1), h.value(), p.x(), p.z()));
            }
        }
        return out;
    }

    private record Result(String structure, StructureStart start) {}

    /** Copy of ChunkGenerator.createStructures' weighted loop, calling the engine's Structure.generate. */
    private static Result loop(Env e, Candidate c, Function<String, Structure> structureOf) {
        List<StructureSelectionEntry> options = new ArrayList<>(c.set().structures());
        ChunkPos cp = new ChunkPos(c.cx(), c.cz());
        WorldgenRandom random = rng(e.seed, cp);
        int total = 0; for (StructureSelectionEntry o : options) total += o.weight();
        while (!options.isEmpty()) {
            int choice = options.size() == 1 && c.set().structures().size() == 1 ? 0 : random.nextInt(total);
            int index = 0;
            for (StructureSelectionEntry o : options) { choice -= o.weight(); if (choice < 0) break; index++; }
            StructureSelectionEntry sel = options.get(index);
            Structure s = structureOf.apply(idOf(sel.structure()));
            StructureStart start = s.generate(sel.structure(), e.dim, e.ra, e.gen, e.bs, e.climate, e.rs, e.tm, e.seed, cp, 0, e.level, s.biomes()::contains);
            if (start.isValid()) return new Result(idOf(sel.structure()), start);
            options.remove(index); total -= sel.weight();
        }
        return new Result("", null);
    }

    private static Structure legacy(Env e, String id, Path dir) throws IOException {
        JsonElement json = JsonParser.parseString(Files.readString(dir.resolve(id + ".json")));
        return Structure.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, e.ra), json).getOrThrow(IllegalStateException::new);
    }

    private static final String HEADER = "label,mode,seed,world,set,chunk_x,chunk_z,config,first_eligible_structure,element,rotation,center_x,center_z,terrain_top_T_at_center,element_size_y,draw1_roll,"
            + "h_allowed_max,h_effective_max,outcome,reason,gen_structure,gen_element,gen_min_y,gen_max_y,gen_offset,gen_top_T,rotation_of_piece,time_us";

    private static void run(MinecraftServer server, Path dir, boolean controlled) throws IOException {
        int radius = Integer.getInteger("mm.ad.radius", 400);
        String label = System.getProperty("mm.ad.label", "run");
        Path legacyDir = Path.of(System.getProperty("mm.ad.legacy", "tools/sky-tower-adaptive/legacy"));
        ServerLevel level = server.overworld();
        List<Candidate> candidates = enumerate(server, radius);
        List<Object[]> cases = new ArrayList<>(); // {Env, ChunkGenerator}
        if (controlled) {
            for (String biome : System.getProperty("mm.ad.biomes", "minecraft:jagged_peaks").split(",")) for (String t : System.getProperty("mm.ad.elevations", "80,120,140,160,180,200,201,202,218,219,220,240,260").split(",")) {
                int top = Integer.parseInt(t.trim()); final int firstFree = top + 1;
                String json = "{\"biome\":\"" + biome + "\",\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},{\"block\":\"minecraft:stone\",\"height\":" + (top + 64) + "}],\"structure_overrides\":[]}";
                FlatLevelGeneratorSettings st = FlatLevelGeneratorSettings.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), JsonParser.parseString(json)).getOrThrow(IllegalStateException::new);
                FlatLevelSource flat = new FlatLevelSource(st) {
                    @Override public int getBaseHeight(int x, int z, Types type, net.minecraft.world.level.LevelHeightAccessor h, RandomState r) { HEIGHT_QUERIES.incrementAndGet(); return firstFree; }
                };
                cases.add(new Object[]{new Env("controlled/" + biome.replace("minecraft:", "") + "/T" + top, "CONTROLLED", "FLAT_IN_PROCESS", server, flat)});
            }
        } else cases.add(new Object[]{new Env(label, "WORLD", level.getChunkSource().getGenerator() instanceof FlatLevelSource ? "FLAT" : "OVERWORLD", server, level.getChunkSource().getGenerator())});

        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("label", label); summary.put("radius", radius); summary.put("candidates", candidates.size());
        long[] alloc = new long[2]; long[] timeNs = new long[2]; long[] heightQ = new long[2]; long[] evals = new long[2]; int[] identical = new int[3];
        List<Map<String, Object>> caseSummaries = new ArrayList<>();
        for (Object[] c : cases) {
            Env e = (Env) c[0];
            Map<String, Structure> prod = new LinkedHashMap<>(), leg = new LinkedHashMap<>();
            var reg = e.ra.lookupOrThrow(Registries.STRUCTURE);
            for (var en : reg.listElements().toList()) { String id = en.key().identifier().getPath(); if (en.key().identifier().getNamespace().equals(NS) && id.startsWith("sky_island_")) { prod.put(id, en.value()); leg.put(id, id.startsWith("sky_island_mountain_") ? legacy(e, id, legacyDir) : en.value()); } }
            int cand = 0;
            for (Candidate cd : candidates) {
                ChunkPos cp = new ChunkPos(cd.cx(), cd.cz());
                // eligibility + shared draws come from the first structure of the set whose biome column accepts the drawn element
                Info ei = null; String eStruct = "";
                for (StructureSelectionEntry o : cd.set().structures()) { Info i = info(e, prod.get(idOf(o.structure())), cp); if (!i.empty() && i.columnOk()) { ei = i; eStruct = idOf(o.structure()); break; } }
                if (ei == null) continue;
                cand++;
                Result[] res = new Result[2]; long[] us = new long[2];
                for (int k = 0; k < 2; k++) {
                    Map<String, Structure> m = k == 0 ? prod : leg; long q0 = HEIGHT_QUERIES.get(); long a0 = ALLOC.getCurrentThreadAllocatedBytes(); long t0 = System.nanoTime();
                    res[k] = loop(e, cd, m::get); us[k] = (System.nanoTime() - t0) / 1000; timeNs[k] += System.nanoTime() - t0 - 0; heightQ[k] += HEIGHT_QUERIES.get() - q0; alloc[k] += ALLOC.getCurrentThreadAllocatedBytes() - a0; evals[k]++;
                }
                boolean mountain = cd.setId().equals("sky_islands_mountain");
                int hEff = Math.min(70, ei.feasibleMax());
                for (int k = 0; k < 2; k++) {
                    Result r = res[k]; String outcome = r.start() != null ? "GENERATED" : "REJECTED";
                    String reason = r.start() != null ? "" : (k == 0 && mountain ? (hEff < 18 ? "ADAPTIVE_INFEASIBLE" : "OTHER_BIOME_OR_STUB") : (ei.roll() > ei.feasibleMax() ? "PADDING" : "STUB_BIOME"));
                    int gmin = 0, gmax = 0, gtop = 0, off = 0; String gel = "", grot = "";
                    if (r.start() != null) {
                        var piece = r.start().getPieces().getFirst(); BoundingBox b = piece.getBoundingBox(); gmin = b.minY(); gmax = b.maxY();
                        gtop = e.top((b.maxX() + b.minX()) / 2, (b.maxZ() + b.minZ()) / 2); off = gmin - gtop;
                        if (piece instanceof PoolElementStructurePiece pp) { gel = elementName(pp.getElement()); grot = pp.getRotation().name(); }
                    }
                    csv.append(e.label).append(',').append(e.mode).append(',').append(e.seed).append(',').append(e.world).append(',').append(cd.setId()).append(',').append(cd.cx()).append(',').append(cd.cz()).append(',')
                       .append(k == 0 ? "PRODUCTION" : "LEGACY").append(',').append(eStruct).append(',').append(ei.element()).append(',').append(ei.rotation()).append(',').append(ei.cx()).append(',').append(ei.cz()).append(',')
                       .append(ei.top()).append(',').append(ei.sizeY()).append(',').append(ei.roll()).append(',').append(ei.feasibleMax()).append(',').append(hEff).append(',').append(outcome).append(',').append(reason).append(',')
                       .append(r.structure()).append(',').append(gel).append(',').append(gmin).append(',').append(gmax).append(',').append(r.start() != null ? off : "").append(',').append(r.start() != null ? gtop : "").append(',').append(grot).append(',').append(us[k]).append('\n');
                }
                // bit-identity claim: wherever the legacy start exists and draw1 <= feasible max, production must be identical
                if (mountain && res[1].start() != null) {
                    boolean same = res[0].start() != null && res[0].start().getPieces().getFirst().getBoundingBox().equals(res[1].start().getPieces().getFirst().getBoundingBox()) && res[0].structure().equals(res[1].structure());
                    identical[same ? 0 : 1]++;
                } else if (mountain && res[1].start() == null && res[0].start() != null) identical[2]++;
            }
            Map<String, Object> cs = new LinkedHashMap<>(); cs.put("label", e.label); cs.put("eligible_candidates", cand);
            caseSummaries.add(cs);
        }
        summary.put("cases", caseSummaries);
        summary.put("evaluations_prod_legacy", List.of(evals[0], evals[1]));
        summary.put("total_time_ms_prod_legacy", List.of(timeNs[0] / 1_000_000, timeNs[1] / 1_000_000));
        summary.put("allocated_bytes_prod_legacy", List.of(alloc[0], alloc[1]));
        summary.put("heightmap_base_height_queries_prod_legacy(controlled only)", List.of(heightQ[0], heightQ[1]));
        summary.put("mountain_legacy_generated__prod_identical_vs_different_vs(prod_only_generated)", List.of(identical[0], identical[1], identical[2]));
        Files.writeString(dir.resolve("rows.csv"), csv.toString());

        if (!controlled && Integer.getInteger("mm.ad.naturalMax", 0) > 0) natural(server, level, candidates, dir, summary, prod(level));
        Files.writeString(dir.resolve("summary.json"), GSON.toJson(summary));
    }

    private static Map<String, Structure> prod(ServerLevel level) {
        Map<String, Structure> m = new LinkedHashMap<>();
        for (var en : level.registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements().toList()) if (en.key().identifier().getNamespace().equals(NS) && en.key().identifier().getPath().startsWith("sky_island_")) m.put(en.key().identifier().getPath(), en.value());
        return m;
    }

    /** NATURAL pipeline cross-check + physical footprint/terrain measurement for accepted towers/islands. */
    private static void natural(MinecraftServer server, ServerLevel level, List<Candidate> candidates, Path dir, Map<String, Object> summary, Map<String, Structure> prod) throws IOException {
        Env e = new Env("natural", "NATURAL", "WORLD", server, level.getChunkSource().getGenerator());
        var reg = e.ra.lookupOrThrow(Registries.STRUCTURE);
        int max = Integer.getInteger("mm.ad.naturalMax", 0), agree = 0, dis = 0, checked = 0, foot = Integer.getInteger("mm.ad.footprintSamples", 200);
        StringBuilder fp = new StringBuilder("seed,chunk_x,chunk_z,structure,element,rotation,piece_min_y,piece_max_y,ref_top_T,ref_offset,footprint_max_terrain,footprint_min_terrain,bbox_min_y_minus_footprint_max_terrain,columns,block_min_separation,block_intersections\n");
        List<StructureStart> towers = new ArrayList<>();
        for (Candidate c : candidates) {
            if (checked >= max) break;
            Info ei = null;
            for (StructureSelectionEntry o : c.set().structures()) { Info i = info(e, prod.get(idOf(o.structure())), new ChunkPos(c.cx(), c.cz())); if (!i.empty() && i.columnOk()) { ei = i; break; } }
            if (ei == null) continue;
            checked++;
            ChunkAccess ch = level.getChunk(c.cx(), c.cz(), ChunkStatus.STRUCTURE_STARTS);
            String nat = ""; StructureStart ns = null;
            for (var en : ch.getAllStarts().entrySet()) { var k = reg.getKey(en.getKey()); if (k != null && k.getNamespace().equals(NS) && k.getPath().startsWith("sky_island_") && en.getValue().isValid()) { nat = k.getPath(); ns = en.getValue(); } }
            Result rp = loop(e, c, prod::get);
            boolean same = rp.structure().equals(nat) && (ns == null || ns.getBoundingBox().equals(rp.start().getBoundingBox()));
            if (same) agree++; else dis++;
            if (ns != null && nat.equals(nat) && ((PoolElementStructurePiece) ns.getPieces().getFirst()).getElement().toString().contains("/tower")) towers.add(ns);
            if (ns != null && c.setId().equals("sky_islands_mountain") && foot-- > 0) fp.append(footprint(e, level, c, nat, ns));
        }
        summary.put("natural_checked", checked); summary.put("natural_vs_replay_agree", agree); summary.put("natural_vs_replay_disagree", dis);
        Files.writeString(dir.resolve("footprint.csv"), fp.toString());
        towers.sort((x, y) -> Integer.compare(y.getBoundingBox().maxY(), x.getBoundingBox().maxY()));
        StringBuilder tb = new StringBuilder("seed,chunk_x,chunk_z,structure_chunk,piece_min_y,piece_max_y,element,rooftop_layer_non_air,rooftop_layer_smooth_stone,rooftop_intact\n");
        for (int i = 0; i < Math.min(Integer.getInteger("mm.ad.topSamples", 6), towers.size()); i++) {
            StructureStart ts = towers.get(i); var piece = (PoolElementStructurePiece) ts.getPieces().getFirst(); BoundingBox b = piece.getBoundingBox();
            for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) level.getChunk(cx, cz, ChunkStatus.FULL, true);
            int na = 0, sm = 0; BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = b.minX(); x <= b.maxX(); x++) for (int z = b.minZ(); z <= b.maxZ(); z++) { var bs = level.getBlockState(pos.set(x, b.maxY(), z)); if (!bs.isAir()) na++; if (bs.is(Blocks.SMOOTH_STONE)) sm++; }
            tb.append(e.seed).append(',').append(ts.getChunkPos().x()).append(',').append(ts.getChunkPos().z()).append(',').append("").append(',').append(b.minY()).append(',').append(b.maxY()).append(',').append(elementName(piece.getElement())).append(',').append(na).append(',').append(sm).append(',').append(sm == 64).append('\n');
        }
        Files.writeString(dir.resolve("rooftop.csv"), tb.toString());
    }

    /** Terrain under the island footprint vs the lowest actual template block per column (sampled every 4 blocks). */
    private static String footprint(Env e, ServerLevel level, Candidate c, String structure, StructureStart start) {
        var piece = (PoolElementStructurePiece) start.getPieces().getFirst(); BoundingBox b = piece.getBoundingBox();
        Identifier id = ((SinglePoolElement) piece.getElement()).getTemplateLocation();
        StructureTemplate t = e.tm.get(id).orElseThrow();
        CompoundTag tag = t.save(new CompoundTag()); if (!KEYS_LOGGED) { KEYS_LOGGED = true; LOG.info("TEMPLATE_KEYS {} blocks={} palette={} first={}", tag.keySet(), tag.getListOrEmpty("blocks").size(), tag.getListOrEmpty("palette").size(), tag.getListOrEmpty("blocks").size() > 0 ? tag.getListOrEmpty("blocks").get(0).toString() + " pal1=" + tag.getListOrEmpty("palette").get(1) : "-"); }
        ListTag palette = tag.getListOrEmpty("palette"), blocks = tag.getListOrEmpty("blocks");
        Map<Long, Integer> lowest = new LinkedHashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag bl = blocks.getCompoundOrEmpty(i); String name = palette.getCompoundOrEmpty(bl.getIntOr("state", 0)).getStringOr("id", "minecraft:air");
            if (name.equals("minecraft:air") || name.equals("minecraft:structure_void") || name.equals("minecraft:jigsaw")) continue;
            ListTag pl = bl.getListOrEmpty("pos"); int[] p = bl.getIntArray("pos").orElse(new int[]{pl.getIntOr(0, 0), pl.getIntOr(1, 0), pl.getIntOr(2, 0)}); BlockPos w = piece.getPosition().offset(StructureTemplate.transform(new BlockPos(p[0], p[1], p[2]), Mirror.NONE, piece.getRotation(), BlockPos.ZERO));
            lowest.merge(((long) w.getX() << 32) ^ (w.getZ() & 0xffffffffL), w.getY(), Math::min);
        }
        int maxT = Integer.MIN_VALUE, minT = Integer.MAX_VALUE, cols = 0, minSep = Integer.MAX_VALUE, inter = 0;
        for (var en : lowest.entrySet()) {
            int x = (int) (en.getKey() >> 32), z = (int) (long) en.getKey();
            if (Math.floorMod(x, 4) != 0 || Math.floorMod(z, 4) != 0) continue;
            int top = e.top(x, z); cols++; maxT = Math.max(maxT, top); minT = Math.min(minT, top);
            int sep = en.getValue() - top - 1; // free blocks between terrain top and lowest island block
            minSep = Math.min(minSep, sep); if (sep < 0) inter++;
        }
        int cx = (b.maxX() + b.minX()) / 2, cz = (b.maxZ() + b.minZ()) / 2, refT = e.top(cx, cz);
        return e.seed + "," + c.cx() + "," + c.cz() + "," + structure + "," + elementName(piece.getElement()) + "," + piece.getRotation() + "," + b.minY() + "," + b.maxY() + "," + refT + "," + (b.minY() - refT) + "," + maxT + "," + minT + ","
                + (b.minY() - maxT) + "," + cols + "," + (cols == 0 ? "" : minSep) + "," + inter + "\n";
    }
}
