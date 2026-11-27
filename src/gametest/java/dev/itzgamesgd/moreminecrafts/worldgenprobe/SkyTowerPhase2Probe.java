package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SKY TOWER mountain-worldgen PHASE 2 validation probe (isolated validation tooling; NOT production).
 * Inert unless {@code -Dmm.p2.out=<dir>} is supplied. Modes ({@code -Dmm.p2.mode}):
 * <ul>
 *  <li>{@code world}: for the server's real world (natural overworld or a flat world), enumerate every placement-selected chunk of
 *      the two Sky Island structure sets, (1) generate the chunks through the real pipeline (NATURAL), (2) re-run the engine's own
 *      {@code Structure.generate} per candidate with the engine's weighted-retry loop (REPLAY) while capturing JigsawPlacement's own
 *      debug log, (3) cross-check against an independent re-derivation of the root-piece arithmetic, (4) evaluate candidate
 *      remediation configurations on the same chunks (paired).</li>
 *  <li>{@code controlled}: same REPLAY/strategy logic but on in-process {@code FlatLevelSource} generators of exactly controlled
 *      surface height and pinned biome (labelled CONTROLLED_REPLAY).</li>
 *  <li>{@code forced}: FORCED template placement at chosen Y to observe physical clipping at the build ceiling.</li>
 * </ul>
 */
public final class SkyTowerPhase2Probe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("sky-tower-phase2-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String NS = "moreminecrafts";
    private static final String JIGSAW_LOGGER = "net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement";

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.p2.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path dir = Path.of(out);
            try {
                Files.createDirectories(dir);
                String mode = System.getProperty("mm.p2.mode", "world");
                switch (mode) {
                    case "world" -> runWorld(server, dir);
                    case "controlled" -> runControlled(server, dir);
                    case "forced" -> runForced(server, dir);
                    default -> throw new IllegalArgumentException("unknown mode " + mode);
                }
                LOG.info("SKY_TOWER_PHASE2_DONE mode={}", mode);
            } catch (Throwable t) {
                LOG.error("phase2 probe failed", t);
                try {
                    Files.writeString(dir.resolve("ERROR.txt"), String.valueOf(t) + "\n", StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // missing summary + ERROR.txt is the failure signal for the driver
                }
            }
            Runtime.getRuntime().halt(0);
        });
    }

    // ------------------------------------------------------------------------------------------------ log capture

    /** Captures JigsawPlacement's DEBUG "does not fit dimension padding" messages with the emitting thread. */
    private static final class Capture extends AbstractAppender {
        record Msg(String thread, String text) {}
        final CopyOnWriteArrayList<Msg> messages = new CopyOnWriteArrayList<>();
        Capture() { super("p2cap", null, null, true, Property.EMPTY_ARRAY); }
        @Override public void append(LogEvent event) {
            messages.add(new Msg(event.getThreadName(), event.getMessage().getFormattedMessage()));
        }
        static Capture install() {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration cfg = context.getConfiguration();
            Capture capture = new Capture();
            capture.start();
            cfg.addAppender(capture);
            LoggerConfig lc = new LoggerConfig(JIGSAW_LOGGER, org.apache.logging.log4j.Level.DEBUG, false);
            lc.addAppender(capture, org.apache.logging.log4j.Level.DEBUG, null);
            cfg.addLogger(JIGSAW_LOGGER, lc);
            context.updateLoggers();
            return capture;
        }
        List<Msg> since(int index) { return new ArrayList<>(messages.subList(index, messages.size())); }
    }

    // ------------------------------------------------------------------------------------------------ environment

    private static final class Env {
        final String label, mode, world;
        final ServerLevel level;
        final ChunkGenerator gen;
        final BiomeSource biomeSource;
        final Climate.Sampler climate;
        final RandomState rs;
        final StructureTemplateManager tm;
        final RegistryAccess ra;
        final long seed;
        final ResourceKey<Level> dim;
        Env(String label, String mode, String world, MinecraftServer server, ChunkGenerator gen) {
            this.label = label; this.mode = mode; this.world = world;
            this.level = server.overworld();
            this.gen = gen;
            this.biomeSource = gen.getBiomeSource();
            this.rs = level.getChunkSource().randomState();
            this.climate = rs.createClimateSampler(SamplerContext.builder().enableCaches().build());
            this.tm = level.getStructureTemplateManager();
            this.ra = level.registryAccess();
            this.seed = level.getSeed();
            this.dim = level.dimension();
        }
        String biomeAt(int x, int y, int z) {
            return biomeName(biomeSource.createResolver(climate).getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)));
        }
    }

    private static String biomeName(Holder<Biome> holder) {
        return holder.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object owner, Class<?> declaring, String name) {
        try {
            Field f = declaring.getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("reflection " + name, e);
        }
    }

    private static WorldgenRandom makeRandom(long seed, ChunkPos cp) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(seed, cp.x(), cp.z());
        return random;
    }

    private static String idOf(Holder<Structure> holder) {
        return holder.unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
    }

    private static String elementName(StructurePoolElement element) {
        if (element instanceof SinglePoolElement single) {
            Identifier id = single.getTemplateLocation();
            if (id != null) return id.getPath().replace("sky_islands/", "");
        }
        return element == EmptyPoolElement.INSTANCE ? "EMPTY" : element.toString().replace(',', ';');
    }

    private static boolean isTowerName(String element) { return element.endsWith("/tower"); }

    // ------------------------------------------------------------------------------------------------ replica of the root-piece arithmetic

    /** Independent re-derivation of JigsawPlacement.addPieces up to the dimension-padding and stub-biome checks. */
    private record Rep(String predicted, int startHeight, String rotation, String element, int sizeY, int firstFree, int topY,
                       int minY, int maxY, int limMinY, int limMaxY, int stubY, String surfaceBiome, String stubBiome, int centerX, int centerZ,
                       boolean columnOk) {}

    private static Rep replicate(Env e, JigsawStructure js, ChunkPos cp) {
        Holder<StructureTemplatePool> startPool = field(js, JigsawStructure.class, "startPool");
        HeightProvider hp = field(js, JigsawStructure.class, "startHeight");
        Optional<Types> hm = field(js, JigsawStructure.class, "projectStartToHeightmap");
        DimensionPadding pad = field(js, JigsawStructure.class, "dimensionPadding");
        GenerationContext ctx = new GenerationContext(e.ra, e.gen, e.biomeSource, e.climate, e.rs, e.tm, e.seed, cp, e.level, js.biomes()::contains);
        WorldgenRandom r = ctx.random();
        int sh = hp.sample(r, new WorldGenerationContext(e.gen, e.level));
        BlockPos startPos = new BlockPos(cp.getMinBlockX(), sh, cp.getMinBlockZ());
        Rotation rot = Rotation.getRandom(r);
        StructurePoolElement el = startPool.value().getRandomTemplate(r);
        if (el == EmptyPoolElement.INSTANCE) return new Rep("EMPTY_POOL", sh, rot.name(), "EMPTY", 0, 0, 0, 0, 0, 0, 0, 0, "", "", 0, 0, false);
        BoundingBox box = el.getBoundingBox(e.tm, startPos, rot);
        int cx = (box.maxX() + box.minX()) / 2, cz = (box.maxZ() + box.minZ()) / 2;
        boolean col = ctx.couldStructureExistInColumn(cx, cz, e.level.getMinY(), e.level.getMaxY());
        int ff = e.gen.getFirstFreeHeight(cx, cz, hm.orElseThrow(), e.level, e.rs);
        int bottomY = sh + ff;
        int minY = bottomY - el.getGroundLevelDelta();
        int maxY = minY + (box.maxY() - box.minY());
        int limMin = e.level.getMinY() + pad.bottom(), limMax = e.level.getMaxY() - pad.top();
        String surfaceBiome = e.biomeAt(cx, ff - 1, cz);
        String stubBiome = e.biomeAt(cx, bottomY, cz);
        String predicted;
        if (!col) predicted = "COLUMN_BIOME_NONE";
        else if (pad != DimensionPadding.ZERO && (minY < limMin || maxY > limMax)) predicted = "PADDING_REJECT";
        else {
            boolean ok = js.biomes().contains(biomeSource(e).createResolver(e.climate).getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(bottomY), QuartPos.fromBlock(cz)));
            predicted = ok ? "ACCEPT" : "STUB_BIOME_REJECT";
        }
        return new Rep(predicted, sh, rot.name(), elementName(el), box.maxY() - box.minY() + 1, ff, ff - 1, minY, maxY, limMin, limMax, bottomY,
                surfaceBiome, stubBiome, cx, cz, col);
    }

    private static BiomeSource biomeSource(Env e) { return e.biomeSource; }

    // ------------------------------------------------------------------------------------------------ engine generate

    private record Engine(boolean valid, String element, int pieceMinY, int pieceMaxY, int pieceCount, String paddingLog, StructureStart start) {}

    private static Engine engine(Env e, Capture cap, Holder<Structure> holder, Structure s, ChunkPos cp) {
        int before = cap.messages.size();
        StructureStart start = s.generate(holder, e.dim, e.ra, e.gen, e.biomeSource, e.climate, e.rs, e.tm, e.seed, cp, 0, e.level, s.biomes()::contains);
        String log = "";
        int logCount = 0;
        String thread = Thread.currentThread().getName();
        for (Capture.Msg m : cap.since(before)) {
            if (m.thread().equals(thread) && m.text().contains("does not fit dimension padding")) { if (logCount++ == 0) log = m.text().replace(',', ';'); }
        }
        if (logCount > 1) log = "[x" + logCount + "] " + log;
        if (!start.isValid()) return new Engine(false, "", 0, 0, 0, log, start);
        StructurePoolElementInfo root = rootInfo(start);
        return new Engine(true, root.element, root.minY, root.maxY, start.getPieces().size(), log, start);
    }

    private record StructurePoolElementInfo(String element, int minY, int maxY) {}

    private static StructurePoolElementInfo rootInfo(StructureStart start) {
        var piece = start.getPieces().getFirst();
        String element = piece instanceof PoolElementStructurePiece p ? elementName(p.getElement()) : "?";
        return new StructurePoolElementInfo(element, piece.getBoundingBox().minY(), piece.getBoundingBox().maxY());
    }

    // ------------------------------------------------------------------------------------------------ candidate enumeration

    private record Candidate(String setId, StructureSet set, int cx, int cz) {}

    private static List<Candidate> enumerate(MinecraftServer server, int radius) {
        ServerLevel level = server.overworld();
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        long seed = level.getSeed();
        List<Candidate> out = new ArrayList<>();
        for (Holder<StructureSet> holder : state.possibleStructureSets()) {
            String id = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
            if (!id.startsWith(NS + ":sky_islands")) continue;
            StructureSet set = holder.value();
            if (!(set.placement() instanceof RandomSpreadStructurePlacement rsp)) continue;
            int sp = rsp.spacing();
            int cells = Math.floorDiv(radius, sp) + 1;
            for (int gx = -cells; gx <= cells; gx++) {
                for (int gz = -cells; gz <= cells; gz++) {
                    ChunkPos p = rsp.getPotentialStructureChunk(seed, gx * sp, gz * sp);
                    if (Math.abs(p.x()) > radius || Math.abs(p.z()) > radius) continue;
                    if (rsp.isStructureChunk(state, p.x(), p.z())) out.add(new Candidate(id.substring(id.indexOf(':') + 1), set, p.x(), p.z()));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ the engine's weighted-retry loop (copy of ChunkGenerator.createStructures)

    private record LoopResult(String generatedStructure, String generatedElement, List<String> rows) {}

    /** structureOf maps a structure path (e.g. sky_island_mountain_snowy) to the Structure instance to run. */
    private static LoopResult replayLoop(Env e, Capture cap, Candidate c, String config, Function<String, Structure> structureOf, boolean traceRows,
                                         Map<String, int[]> engineVsReplica) {
        List<StructureSelectionEntry> options = new ArrayList<>(c.set().structures());
        List<String> rows = new ArrayList<>();
        ChunkPos cp = new ChunkPos(c.cx(), c.cz());
        WorldgenRandom random = makeRandom(e.seed, cp);
        int total = 0;
        for (StructureSelectionEntry o : options) total += o.weight();
        int order = 0;
        while (!options.isEmpty()) {
            int choice = options.size() == 1 && c.set().structures().size() == 1 ? 0 : random.nextInt(total);
            int index = 0;
            for (StructureSelectionEntry o : options) {
                choice -= o.weight();
                if (choice < 0) break;
                index++;
            }
            StructureSelectionEntry selected = options.get(index);
            String sid = idOf(selected.structure());
            Structure s = structureOf.apply(sid);
            Engine en = engine(e, cap, selected.structure(), s, cp);
            Rep rep = null;
            if (s instanceof JigsawStructure js) rep = replicate(e, js, cp);
            if (traceRows && !(rep != null && rep.predicted().equals("COLUMN_BIOME_NONE"))) {
                StringBuilder sb = new StringBuilder();
                sb.append(e.label).append(',').append(e.mode).append(',').append(e.seed).append(',').append(e.world).append(',').append(config).append(',')
                        .append(c.setId()).append(',').append(c.cx()).append(',').append(c.cz()).append(',').append(order).append(',').append(sid).append(',')
                        .append(cp.getMinBlockX()).append(',').append(cp.getMinBlockZ()).append(',');
                if (rep != null) {
                    sb.append(rep.surfaceBiome()).append(',').append(rep.firstFree()).append(',').append(rep.topY()).append(',').append(rep.startHeight()).append(',')
                            .append(rep.rotation()).append(',').append(rep.element()).append(',').append(rep.sizeY()).append(',').append(rep.minY()).append(',')
                            .append(rep.maxY()).append(',').append(rep.limMaxY()).append(',').append(rep.stubY()).append(',').append(rep.stubBiome()).append(',')
                            .append(rep.predicted());
                } else sb.append(",,,,,,,,,,,,");
                boolean agree = rep == null || (rep.predicted().equals("ACCEPT") == en.valid());
                sb.append(',').append(en.valid()).append(',').append(en.paddingLog().isEmpty() ? "" : "PADDING_LOG:" + en.paddingLog()).append(',')
                        .append(en.valid() ? en.pieceMinY() : "").append(',').append(en.valid() ? en.pieceMaxY() : "").append(',')
                        .append(agree);
                rows.add(sb.toString());
            }
            if (engineVsReplica != null && rep != null) {
                int[] counts = engineVsReplica.computeIfAbsent(rep.predicted(), k -> new int[2]);
                counts[en.valid() ? 1 : 0]++;
                if (rep.predicted().equals("PADDING_REJECT")) engineVsReplica.computeIfAbsent("PADDING_REJECT_LOGGED", k -> new int[2])[en.paddingLog().isEmpty() ? 0 : 1]++;
                if (en.valid()) {
                    int[] geo = engineVsReplica.computeIfAbsent("GEOMETRY_MATCH", k -> new int[2]);
                    geo[(en.pieceMinY() == rep.minY() && en.pieceMaxY() == rep.maxY() && en.element().equals(rep.element())) ? 1 : 0]++;
                }
            }
            order++;
            if (en.valid()) return new LoopResult(sid, en.element(), rows);
            options.remove(index);
            total -= selected.weight();
        }
        return new LoopResult("", "", rows);
    }

    private static final String TRACE_HEADER = "run_label,mode,seed,world,config,set,chunk_x,chunk_z,order,structure,block_x,block_z,surface_biome,terrain_first_free_y,terrain_top_y,"
            + "start_height_sampled,rotation,pool_element,element_size_y,computed_min_y,computed_max_y,limit_max_y,stub_y,stub_biome,replica_prediction,"
            + "engine_valid,engine_log_reason,engine_piece_min_y,engine_piece_max_y,replica_agrees_with_engine";

    // ------------------------------------------------------------------------------------------------ strategies

    /** Candidate remediation configuration, evaluated on the same chunks. */
    private record Strategy(String id, String kind, int lo, int hi, int n) {}

    private static List<Strategy> strategies() {
        List<Strategy> out = new ArrayList<>();
        String ranges = System.getProperty("mm.p2.ranges", "15-35,18-35,10-30,5-20,1-18,1-10");
        for (String r : ranges.split(",")) {
            String[] p = r.trim().split("-");
            out.add(new Strategy("A_RANGE_" + p[0] + "_" + p[1], "RANGE", Integer.parseInt(p[0]), Integer.parseInt(p[1]), 0));
        }
        out.add(new Strategy("B_DYN_18_70", "DYN", 18, 70, 0));
        out.add(new Strategy("B_DYN_1_70", "DYN", 1, 70, 0));
        out.add(new Strategy("C_RETRY3_18_70", "RETRY", 18, 70, 3));
        out.add(new Strategy("C_RETRY8_18_70", "RETRY", 18, 70, 8));
        out.add(new Strategy("C_SUBST_ELEMENT", "SUBST", 18, 70, 0));
        out.add(new Strategy("C_RELOC_R2", "RELOC", 18, 70, 2));
        out.add(new Strategy("D_TOWER_ONLY_18_70", "TOWER", 18, 70, 0));
        out.add(new Strategy("D_TOWER_ONLY_1_20", "TOWER", 1, 20, 0));
        return out;
    }

    private static JsonElement readStructureJson(String path) throws IOException {
        String resource = "/data/" + NS + "/worldgen/structure/" + path + ".json";
        try (Reader r = new InputStreamReader(SkyTowerPhase2Probe.class.getResourceAsStream(resource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r);
        }
    }

    private static Structure decodeRange(Env e, String path, int lo, int hi) throws IOException {
        JsonElement json = readStructureJson(path);
        var h = json.getAsJsonObject().getAsJsonObject("start_height");
        h.getAsJsonObject("min_inclusive").addProperty("absolute", lo);
        h.getAsJsonObject("max_inclusive").addProperty("absolute", hi);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, e.ra);
        return Structure.DIRECT_CODEC.parse(ops, json).getOrThrow(IllegalStateException::new);
    }

    private static Structure make(Env e, String path, Strategy st, Structure production) throws IOException {
        if (st.kind().equals("RANGE")) return decodeRange(e, path, st.lo(), st.hi());
        JigsawStructure base = (JigsawStructure) production;
        StructureSettings settings = field(base, Structure.class, "settings");
        return new Proto(settings, base, st);
    }

    /** Prototype (NOT production) structure used to test strategies not expressible as JSON; delegates to the real JigsawPlacement. */
    private static final class Proto extends Structure {
        final JigsawStructure base;
        final Strategy st;
        final Holder<StructureTemplatePool> pool;
        final HeightProvider baseHp;
        final DimensionPadding pad;
        final JigsawStructure.MaxDistance maxDist;
        final int size;
        final LiquidSettings liquid;
        Proto(StructureSettings settings, JigsawStructure base, Strategy st) {
            super(settings);
            this.base = base; this.st = st;
            this.pool = field(base, JigsawStructure.class, "startPool");
            this.baseHp = field(base, JigsawStructure.class, "startHeight");
            this.pad = field(base, JigsawStructure.class, "dimensionPadding");
            this.maxDist = field(base, JigsawStructure.class, "maxDistanceFromCenter");
            this.size = field(base, JigsawStructure.class, "maxDepth");
            this.liquid = field(base, JigsawStructure.class, "liquidSettings");
        }

        private Optional<GenerationStub> place(GenerationContext ctx, Holder<StructureTemplatePool> p, int startH, int dx, int dz) {
            ChunkPos cp = ctx.chunkPos();
            return JigsawPlacement.addPieces(ctx, p, Optional.empty(), size, new BlockPos(cp.getMinBlockX() + 16 * dx, startH, cp.getMinBlockZ() + 16 * dz), false,
                    Optional.of(Types.WORLD_SURFACE_WG), maxDist, PoolAliasLookup.EMPTY, pad, liquid);
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
            ChunkPos cp = ctx.chunkPos();
            WorldGenerationContext wgc = new WorldGenerationContext(ctx.chunkGenerator(), ctx.heightAccessor());
            if (st.kind().equals("TOWER")) {
                int h = st.lo() + ctx.random().nextInt(st.hi() - st.lo() + 1);
                StructureTemplatePool original = pool.value();
                StructurePoolElement tower = null;
                for (Pair<StructurePoolElement, Integer> t : original.getTemplates()) if (isTowerName(elementName(t.getFirst()))) { tower = t.getFirst(); break; }
                if (tower == null) return Optional.empty();
                StructureTemplatePool single = new StructureTemplatePool(original.getFallback(), List.of(Pair.of(tower, 1)));
                return place(ctx, Holder.direct(single), h, 0, 0);
            }
            int h0 = baseHp.sample(ctx.random(), wgc);
            WorldgenRandom look = makeRandom(ctx.seed(), cp);
            baseHp.sample(look, wgc);
            Rotation rot = Rotation.getRandom(look);
            StructurePoolElement el = pool.value().getRandomTemplate(look);
            if (el == EmptyPoolElement.INSTANCE) return Optional.empty();
            BoundingBox box = el.getBoundingBox(ctx.structureTemplateManager(), new BlockPos(cp.getMinBlockX(), h0, cp.getMinBlockZ()), rot);
            int sizeY = box.maxY() - box.minY() + 1;
            int limMax = ctx.heightAccessor().getMaxY() - pad.top();
            int cx = (box.maxX() + box.minX()) / 2, cz = (box.maxZ() + box.minZ()) / 2;
            int top = ctx.chunkGenerator().getFirstOccupiedHeight(cx, cz, Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            int maxStart = limMax - top - (sizeY - 1); // largest start_height whose root-piece maxY <= limMax
            switch (st.kind()) {
                case "DYN" -> {
                    int hiEff = Math.min(st.hi(), maxStart);
                    if (hiEff < st.lo()) return Optional.empty(); // infeasible for this terrain at the required minimum clearance
                    return place(ctx, pool, st.lo() + look.nextInt(hiEff - st.lo() + 1), 0, 0);
                }
                case "RETRY" -> {
                    int h = h0, tries = 0;
                    while (top + h + sizeY - 1 > limMax && tries < st.n()) { h = st.lo() + look.nextInt(st.hi() - st.lo() + 1); tries++; }
                    return place(ctx, pool, h, 0, 0);
                }
                case "SUBST" -> {
                    if (top + h0 + sizeY - 1 <= limMax) return place(ctx, pool, h0, 0, 0);
                    List<StructurePoolElement> fits = new ArrayList<>();
                    for (Pair<StructurePoolElement, Integer> t : pool.value().getTemplates()) {
                        BoundingBox b = t.getFirst().getBoundingBox(ctx.structureTemplateManager(), new BlockPos(cp.getMinBlockX(), h0, cp.getMinBlockZ()), rot);
                        int bsz = b.maxY() - b.minY() + 1;
                        int bcx = (b.maxX() + b.minX()) / 2, bcz = (b.maxZ() + b.minZ()) / 2;
                        int btop = ctx.chunkGenerator().getFirstOccupiedHeight(bcx, bcz, Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
                        if (btop + h0 + bsz - 1 <= limMax) for (int i = 0; i < t.getSecond(); i++) fits.add(t.getFirst());
                    }
                    if (fits.isEmpty()) return Optional.empty();
                    StructurePoolElement pick = fits.get(look.nextInt(fits.size()));
                    return place(ctx, Holder.direct(new StructureTemplatePool(pool.value().getFallback(), List.of(Pair.of(pick, 1)))), h0, 0, 0);
                }
                case "RELOC" -> {
                    Optional<GenerationStub> original = top + h0 + sizeY - 1 <= limMax ? place(ctx, pool, h0, 0, 0) : Optional.empty();
                    if (original.isPresent()) return original;
                    Holder<StructureTemplatePool> fixedPool = Holder.direct(new StructureTemplatePool(pool.value().getFallback(), List.of(Pair.of(el, 1))));
                    for (int d = 1; d <= st.n(); d++) {
                        for (int dx = -d; dx <= d; dx++) for (int dz = -d; dz <= d; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
                            Optional<GenerationStub> moved = place(ctx, fixedPool, h0, dx, dz);
                            if (moved.isPresent() && ctx.isValidBiome(moved.get())) return moved;
                        }
                    }
                    return Optional.empty();
                }
                default -> throw new IllegalStateException(st.kind());
            }
        }

        @Override public StructureType<?> type() { return StructureType.JIGSAW; }
    }

    // ------------------------------------------------------------------------------------------------ shared run logic

    private static final String STRAT_HEADER = "run_label,mode,seed,world,config,set,chunk_x,chunk_z,prod_structure,prod_surface_biome,prod_top_y,prod_element,prod_is_tower,"
            + "prod_outcome,prod_reason,cand_generated,cand_structure,cand_element,cand_is_tower,cand_min_y,cand_max_y,cand_clearance,cand_top_y,cand_surface_biome,cand_stub_y,cand_stub_biome";

    private static void writeElements(Env e, Path dir) throws IOException {
        StringBuilder sb = new StringBuilder("pool,element,weight,size_x,size_y,size_z\n");
        var pools = e.ra.lookupOrThrow(Registries.TEMPLATE_POOL);
        for (var entry : pools.listElements().toList()) {
            String key = entry.key().identifier().toString();
            if (!key.startsWith(NS + ":sky_islands/") || !key.endsWith("/start")) continue;
            for (Pair<StructurePoolElement, Integer> t : entry.value().getTemplates()) {
                var size = t.getFirst().getSize(e.tm, Rotation.NONE);
                sb.append(key).append(',').append(elementName(t.getFirst())).append(',').append(t.getSecond()).append(',')
                        .append(size.getX()).append(',').append(size.getY()).append(',').append(size.getZ()).append('\n');
            }
        }
        Files.writeString(dir.resolve("elements.csv"), sb.toString());
    }

    private static Map<String, Structure> productionStructures(Env e) {
        Map<String, Structure> out = new LinkedHashMap<>();
        var reg = e.ra.lookupOrThrow(Registries.STRUCTURE);
        for (var entry : reg.listElements().toList()) {
            String id = entry.key().identifier().toString();
            if (id.startsWith(NS + ":sky_island_")) out.put(entry.key().identifier().getPath(), entry.value());
        }
        return out;
    }

    /** Runs production replay + all strategies on the supplied candidates. Returns summary map. */
    private static Map<String, Object> evaluate(Env e, Capture cap, List<Candidate> candidates, Path dir, boolean traceProduction, Map<String, Structure> production,
                                                boolean withStrategies, StringBuilder trace, StringBuilder strat, List<Candidate> eligibleOut) throws IOException {
        Map<String, int[]> engineVsReplica = new TreeMap<>();
        List<Strategy> strategies = withStrategies ? strategies() : List.of();
        Map<String, Map<String, Structure>> altByStrategy = new LinkedHashMap<>();
        for (Strategy st : strategies) {
            Map<String, Structure> alts = new LinkedHashMap<>();
            for (var p : production.entrySet()) alts.put(p.getKey(), p.getKey().startsWith("sky_island_mountain_") ? make(e, p.getKey(), st, p.getValue()) : p.getValue());
            altByStrategy.put(st.id(), alts);
        }
        int eligible = 0;
        int[] drawIdentity = new int[2];
        for (Candidate c : candidates) {
            LoopResult prod = replayLoop(e, cap, c, "PRODUCTION", production::get, traceProduction, engineVsReplica);
            if (traceProduction) for (String row : prod.rows()) trace.append(row).append('\n');
            // eligibility: at least one attempt whose biome column accepted the structure (height config cannot change this)
            ChunkPos cp = new ChunkPos(c.cx(), c.cz());
            Rep firstEligible = null;
            for (StructureSelectionEntry o : c.set().structures()) {
                String sid = idOf(o.structure());
                Rep r = replicate(e, (JigsawStructure) production.get(sid), cp);
                if (r.columnOk()) { firstEligible = r; break; }
            }
            if (firstEligible == null) continue;
            if (c.setId().equals("sky_islands_mountain")) {
                java.util.Set<String> draws = new java.util.HashSet<>();
                for (StructureSelectionEntry o : c.set().structures()) {
                    Rep r = replicate(e, (JigsawStructure) production.get(idOf(o.structure())), cp);
                    draws.add(r.startHeight() + "|" + r.rotation() + "|" + r.element().substring(r.element().lastIndexOf('/') + 1));
                }
                drawIdentity[draws.size() == 1 ? 0 : 1]++;
            }
            eligible++;
            if (eligibleOut != null) eligibleOut.add(c);
            appendStrat(e, strat, c, "PRODUCTION", firstEligible, prod, cap, production);
            if (!c.setId().equals("sky_islands_mountain")) continue;
            for (Strategy st : strategies) {
                Map<String, Structure> alts = altByStrategy.get(st.id());
                LoopResult r = replayLoop(e, cap, c, st.id(), alts::get, false, null);
                appendStrat(e, strat, c, st.id(), firstEligible, r, cap, alts);
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("candidates", candidates.size());
        summary.put("eligible_candidates", eligible);
        summary.put("variant_draws_identical_vs_different_mountain_candidates", List.of(drawIdentity[0], drawIdentity[1]));
        Map<String, Object> cross = new LinkedHashMap<>();
        for (var entry : engineVsReplica.entrySet()) cross.put(entry.getKey(), List.of(entry.getValue()[0], entry.getValue()[1]));
        summary.put("replica_prediction__engine_[invalid,valid]", cross);
        return summary;
    }

    private static void appendStrat(Env e, StringBuilder strat, Candidate c, String config, Rep prod, LoopResult r, Capture cap, Map<String, Structure> structs) {
        boolean gen = !r.generatedStructure().isEmpty();
        String candEl = r.generatedElement();
        int minY = 0, maxY = 0, stubY = 0, candTop = 0;
        String stubBiome = "", candSurfaceBiome = "";
        if (gen) {
            // re-run to read engine piece box and the stub biome (deterministic)
            ChunkPos cp = new ChunkPos(c.cx(), c.cz());
            Structure s = structs.get(r.generatedStructure());
            Holder<Structure> holder = Holder.direct(s);
            Engine en = engine(e, cap, holder, s, cp);
            minY = en.pieceMinY(); maxY = en.pieceMaxY();
            StructurePoolElementInfo info = rootInfo(en.start());
            candEl = info.element();
            int cx = (en.start().getBoundingBox().maxX() + en.start().getBoundingBox().minX()) / 2;
            int cz = (en.start().getBoundingBox().maxZ() + en.start().getBoundingBox().minZ()) / 2;
            candTop = e.gen.getFirstOccupiedHeight(cx, cz, Types.WORLD_SURFACE_WG, e.level, e.rs);
            candSurfaceBiome = e.biomeAt(cx, candTop, cz);
            stubY = minY + 1;
            stubBiome = e.biomeAt(cx, stubY, cz);
        }
        String prodOutcome = prod.predicted();
        strat.append(e.label).append(',').append(e.mode).append(',').append(e.seed).append(',').append(e.world).append(',').append(config).append(',')
                .append(c.setId()).append(',').append(c.cx()).append(',').append(c.cz()).append(',').append("").append(',').append(prod.surfaceBiome()).append(',')
                .append(prod.topY()).append(',').append(prod.element()).append(',').append(isTowerName(prod.element())).append(',')
                .append(prodOutcome).append(',').append(prodOutcome).append(',')
                .append(gen).append(',').append(r.generatedStructure()).append(',').append(candEl).append(',').append(isTowerName(candEl)).append(',')
                .append(gen ? minY : "").append(',').append(gen ? maxY : "").append(',').append(gen ? minY - candTop : "").append(',').append(gen ? candTop : "").append(',').append(candSurfaceBiome).append(',')
                .append(gen ? stubY : "").append(',').append(stubBiome).append('\n');
    }

    // ------------------------------------------------------------------------------------------------ mode: world

    private static void runWorld(MinecraftServer server, Path dir) throws IOException {
        int radius = Integer.getInteger("mm.p2.radius", 400);
        int naturalMax = Integer.getInteger("mm.p2.naturalMax", 4000);
        boolean withStrategies = !Boolean.getBoolean("mm.p2.noStrategies");
        String label = System.getProperty("mm.p2.label", "world");
        ServerLevel level = server.overworld();
        String world = level.getChunkSource().getGenerator() instanceof FlatLevelSource ? "FLAT" : "OVERWORLD";
        Capture cap = Capture.install();
        Env e = new Env(label, "REPLAY", world, server, level.getChunkSource().getGenerator());
        Map<String, Structure> production = productionStructures(e);
        writeElements(e, dir);
        List<Candidate> candidates = enumerate(server, radius);
        LOG.info("P2 candidates={} radius={} seed={} world={}", candidates.size(), radius, e.seed, world);

        StringBuilder trace = new StringBuilder(TRACE_HEADER).append('\n');
        StringBuilder strat = new StringBuilder(STRAT_HEADER).append('\n');
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("label", label); summary.put("seed", e.seed); summary.put("world", world); summary.put("radius_chunks", radius);
        summary.put("level_min_y", level.getMinY()); summary.put("level_max_y_inclusive", level.getMaxY());
        long t0 = System.currentTimeMillis();
        List<Candidate> eligibleList = new ArrayList<>();
        summary.putAll(evaluate(e, cap, candidates, dir, true, production, withStrategies, trace, strat, eligibleList));
        summary.put("replay_ms", System.currentTimeMillis() - t0);
        summary.put("active_start_height_mountain_snowy", String.valueOf((Object) field(production.get("sky_island_mountain_snowy"), JigsawStructure.class, "startHeight")));
        Files.writeString(dir.resolve("trace.csv"), trace.toString());
        Files.writeString(dir.resolve("strategies.csv"), strat.toString());

        // ---- NATURAL: the real chunk pipeline, compared per candidate with the replay's prediction
        int capBefore = cap.messages.size();
        StringBuilder nat = new StringBuilder("run_label,seed,world,set,chunk_x,chunk_z,natural_structure,natural_root_element,natural_min_y,natural_max_y,replay_structure,replay_element,agrees\n");
        var structureRegistry = e.ra.lookupOrThrow(Registries.STRUCTURE);
        int agree = 0, disagree = 0, naturalChecked = 0, naturalGenerated = 0;
        List<StructureStart> towerStarts = new ArrayList<>();
        long t1 = System.currentTimeMillis();
        Map<String, Structure> prodView = production;
        List<Candidate> naturalOrder = new ArrayList<>(eligibleList);
        int negatives = Integer.getInteger("mm.p2.naturalNegatives", 300);
        for (Candidate c : candidates) { if (negatives <= 0) break; if (!eligibleList.contains(c)) { naturalOrder.add(c); negatives--; } }
        summary.put("natural_eligible_in_scope", eligibleList.size());
        for (Candidate c : naturalOrder) {
            if (naturalChecked >= naturalMax) break;
            naturalChecked++;
            ChunkAccess chunk = level.getChunk(c.cx(), c.cz(), ChunkStatus.STRUCTURE_STARTS);
            String natStructure = "", natEl = "";
            int natMin = 0, natMax = 0;
            for (var entry : chunk.getAllStarts().entrySet()) {
                var key = structureRegistry.getKey(entry.getKey());
                if (key == null || !key.getNamespace().equals(NS) || !key.getPath().startsWith("sky_island_") || !entry.getValue().isValid()) continue;
                StructurePoolElementInfo info = rootInfo(entry.getValue());
                natStructure = key.getPath(); natEl = info.element(); natMin = info.minY(); natMax = info.maxY();
                if (isTowerName(natEl)) towerStarts.add(entry.getValue());
            }
            LoopResult prod = replayLoop(e, cap, c, "PRODUCTION", prodView::get, false, null);
            boolean ok = natStructure.equals(prod.generatedStructure()) && natEl.equals(prod.generatedElement());
            if (ok) agree++; else disagree++;
            if (!natStructure.isEmpty()) naturalGenerated++;
            nat.append(label).append(',').append(e.seed).append(',').append(world).append(',').append(c.setId()).append(',').append(c.cx()).append(',').append(c.cz()).append(',')
                    .append(natStructure).append(',').append(natEl).append(',').append(natStructure.isEmpty() ? "" : natMin).append(',').append(natStructure.isEmpty() ? "" : natMax).append(',')
                    .append(prod.generatedStructure()).append(',').append(prod.generatedElement()).append(',').append(ok).append('\n');
        }
        summary.put("natural_candidates_checked", naturalChecked);
        summary.put("natural_generated_starts", naturalGenerated);
        summary.put("natural_vs_replay_agree", agree);
        summary.put("natural_vs_replay_disagree", disagree);
        summary.put("natural_ms", System.currentTimeMillis() - t1);
        // padding-rejection log lines emitted by the engine on worker threads during the natural pass
        List<String> naturalPadding = new ArrayList<>();
        for (Capture.Msg m : cap.since(capBefore)) if (m.text().contains("does not fit dimension padding")) naturalPadding.add((m.thread().equals(Thread.currentThread().getName()) ? "SERVER_THREAD_REPLAY" : "WORKER:" + m.thread()) + "\t" + m.text());
        summary.put("natural_padding_log_lines", naturalPadding.size());
        Files.writeString(dir.resolve("natural.csv"), nat.toString());
        Files.writeString(dir.resolve("natural_padding_log.txt"), String.join("\n", naturalPadding));

        // ---- top blocks of accepted natural towers (FULL chunk generation)
        towerStarts.sort((a, b) -> Integer.compare(b.getBoundingBox().maxY(), a.getBoundingBox().maxY()));
        int limit = Integer.getInteger("mm.p2.topBlockSamples", 4);
        StringBuilder top = new StringBuilder("run_label,seed,chunk_x,chunk_z,piece_min_y,piece_max_y,element,top_layer_non_air_blocks,top_layer_smooth_stone,top_layer_blocks,top_blocks_present\n");
        for (int i = 0; i < Math.min(limit, towerStarts.size()); i++) {
            StructureStart start = towerStarts.get(i * Math.max(1, towerStarts.size() / limit) % towerStarts.size());
            top.append(topBlocks(e, label, start));
        }
        Files.writeString(dir.resolve("topblocks.csv"), top.toString());
        summary.put("tower_starts_natural", towerStarts.size());
        Files.writeString(dir.resolve("summary.json"), GSON.toJson(summary));
    }

    private static String topBlocks(Env e, String label, StructureStart start) {
        var piece = start.getPieces().getFirst();
        BoundingBox box = piece.getBoundingBox();
        ServerLevel level = e.level;
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz, ChunkStatus.FULL, true);
        int nonAir = 0, smooth = 0;
        Map<String, Integer> hist = new TreeMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            BlockState state = level.getBlockState(pos.set(x, box.maxY(), z));
            if (!state.isAir()) { nonAir++; hist.merge(state.getBlock().builtInRegistryHolder().key().identifier().getPath(), 1, Integer::sum); }
            if (state.is(Blocks.SMOOTH_STONE)) smooth++;
        }
        return label + "," + e.seed + "," + start.getChunkPos().x() + "," + start.getChunkPos().z() + "," + box.minY() + "," + box.maxY() + "," + (piece instanceof PoolElementStructurePiece pp ? elementName(pp.getElement()) : "?") + "," + nonAir + "," + smooth + "," + hist.toString().replace(',', ';') + ","
                + (nonAir > 0) + "\n";
    }

    // ------------------------------------------------------------------------------------------------ mode: controlled

    private static void runControlled(MinecraftServer server, Path dir) throws IOException {
        int radius = Integer.getInteger("mm.p2.radius", 300);
        String[] elevations = System.getProperty("mm.p2.elevations", "80,120,140,160,180,200,220,240,260").split(",");
        String[] biomes = System.getProperty("mm.p2.biomes", "minecraft:jagged_peaks,minecraft:stony_peaks,minecraft:meadow,minecraft:desert").split(",");
        boolean withStrategies = !Boolean.getBoolean("mm.p2.noStrategies");
        ServerLevel level = server.overworld();
        Capture cap = Capture.install();
        Env base = new Env("controlled", "CONTROLLED_REPLAY", "FLAT_IN_PROCESS", server, level.getChunkSource().getGenerator());
        Map<String, Structure> production = productionStructures(base);
        List<Candidate> candidates = enumerate(server, radius);
        LOG.info("P2 controlled candidates={} seed={}", candidates.size(), base.seed);
        StringBuilder trace = new StringBuilder(TRACE_HEADER).append('\n');
        StringBuilder strat = new StringBuilder(STRAT_HEADER).append('\n');
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("seed", base.seed); summary.put("radius_chunks", radius); summary.put("candidates_per_terrain", candidates.size());
        Map<String, Object> perCase = new LinkedHashMap<>();
        for (String biome : biomes) {
            for (String elevation : elevations) {
                int top = Integer.parseInt(elevation.trim());
                int layers = top + 64; // bedrock at -64; top block Y == top
                String json = "{\"biome\":\"" + biome + "\",\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},{\"block\":\"minecraft:stone\",\"height\":" + (layers - 1) + "}],\"structure_overrides\":[]}";
                RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, base.ra);
                FlatLevelGeneratorSettings settings = FlatLevelGeneratorSettings.CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow(IllegalStateException::new);
                final int firstFree = top + 1;
                FlatLevelSource flat = new FlatLevelSource(settings) {
                    @Override public int getBaseHeight(int x, int z, Types type, net.minecraft.world.level.LevelHeightAccessor h, RandomState r) { return firstFree; }
                };
                String lbl = "controlled/" + biome.replace("minecraft:", "") + "/T" + top;
                Env e = new Env(lbl, "CONTROLLED_REPLAY", "FLAT_IN_PROCESS", server, flat);
                int probe = flat.getFirstOccupiedHeight(0, 0, Types.WORLD_SURFACE_WG, level, e.rs);
                Map<String, Object> s = evaluate(e, cap, candidates, dir, true, production, withStrategies, trace, strat, null);
                s.put("measured_top_block_y", probe);
                perCase.put(lbl, s);
                LOG.info("P2 controlled {} done measured_top={}", lbl, probe);
            }
        }
        summary.put("cases", perCase);
        Files.writeString(dir.resolve("trace.csv"), trace.toString());
        Files.writeString(dir.resolve("strategies.csv"), strat.toString());
        Files.writeString(dir.resolve("summary.json"), GSON.toJson(summary));
        writeElements(base, dir);
    }

    // ------------------------------------------------------------------------------------------------ mode: forced

    private static void runForced(MinecraftServer server, Path dir) throws IOException {
        ServerLevel level = server.overworld();
        StructureTemplateManager tm = level.getStructureTemplateManager();
        Identifier id = Identifier.fromNamespaceAndPath(NS, "sky_islands/snowy/tower");
        StructureTemplate template = tm.get(id).orElseThrow();
        int sizeY = template.getSize().getY();
        StringBuilder sb = new StringBuilder("test,label,origin_min_y,expected_max_y,layers_expected,layers_world_readable,top_layer_world_y,top_layer_non_air,reference_top_layer_non_air,highest_non_air_world_y,clipped_layers\n");
        int[] targetMaxY = {80, 315, 316, 319, 320, 324, 330};
        int x0 = 40000;
        // reference placement fully inside the world, to count the non-air blocks of every local layer
        int[] reference = place(level, template, new BlockPos(x0, 0, 0), sizeY);
        StringBuilder ref = new StringBuilder("style,top_layer_non_air_blocks_reference\n");
        String[] styles = {"snowy", "meadow", "forest", "badlands", "desert", "rocky"};
        for (int k = 0; k < styles.length; k++) {
            StructureTemplate t = tm.get(Identifier.fromNamespaceAndPath(NS, "sky_islands/" + styles[k] + "/tower")).orElseThrow();
            int[] counts = place(level, t, new BlockPos(x0 - 200 * (k + 1), 0, 0), t.getSize().getY());
            ref.append(styles[k]).append(',').append(counts[t.getSize().getY() - 1]).append('\n');
        }
        Files.writeString(dir.resolve("forced_reference_top_layer.csv"), ref.toString());
        for (int i = 0; i < targetMaxY.length; i++) {
            int minY = targetMaxY[i] - (sizeY - 1);
            int[] layers = place(level, template, new BlockPos(x0 + 200 * (i + 1), minY, 0), sizeY);
            int readable = 0, clipped = 0, highest = Integer.MIN_VALUE;
            for (int l = 0; l < sizeY; l++) {
                int y = minY + l;
                if (y <= level.getMaxY()) { readable++; if (layers[l] > 0) highest = Math.max(highest, y); }
                else if (reference[l] > 0) clipped++;
            }
            int topLocal = Math.min(sizeY - 1, level.getMaxY() - minY);
            sb.append("FORCED_PLACE,tower_maxY_").append(targetMaxY[i]).append(',').append(minY).append(',').append(targetMaxY[i]).append(',').append(sizeY).append(',')
                    .append(readable).append(',').append(minY + topLocal).append(',').append(layers[topLocal]).append(',').append(reference[topLocal]).append(',')
                    .append(highest).append(',').append(clipped).append('\n');
        }
        Files.writeString(dir.resolve("forced.csv"), sb.toString());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("level_min_y", level.getMinY()); summary.put("level_max_y_inclusive", level.getMaxY()); summary.put("template_size_y", sizeY);
        Files.writeString(dir.resolve("summary.json"), GSON.toJson(summary));
    }

    private static int[] place(ServerLevel level, StructureTemplate template, BlockPos origin, int sizeY) {
        var size = template.getSize();
        for (int cx = origin.getX() >> 4; cx <= (origin.getX() + size.getX()) >> 4; cx++)
            for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + size.getZ()) >> 4; cz++) level.getChunk(cx, cz, ChunkStatus.FULL, true);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
        int[] counts = new int[sizeY];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int l = 0; l < sizeY; l++) {
            int y = origin.getY() + l;
            if (y > level.getMaxY() || y < level.getMinY()) continue;
            for (int x = 0; x < size.getX(); x++) for (int z = 0; z < size.getZ(); z++)
                if (!level.getBlockState(pos.set(origin.getX() + x, y, origin.getZ() + z)).isAir()) counts[l]++;
        }
        return counts;
    }
}
