package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01c2b;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestData;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRewardModel;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRole;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RouteSelection;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01AssemblyPiece;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PlacementConfig;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidCompletion;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidStructure;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01WorldgenGuard;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 P8-C2-B real-server validation probe for ONE seed (inert unless {@code -Dmm.st01.c2b.out} is set; not part of
 * build/check). Natural worldgen of the complete pyramid in a dedicated Minecraft 26.3 server.
 *
 * <p>Phases: {@code scan} (fresh world, chunks generated in NORMAL order), {@code reverse} and {@code random} (fresh worlds,
 * same seed, anchors and chunks in reversed / seeded-shuffled order), {@code reload} (the saved scan world restarted). Every
 * phase records, per start, structure-owned and whole-box hashes, the exact count of structure-owned cells that differ from
 * the approved templates, terrain/fluid/entrance metrics, vanilla structure overlaps, the completion handoff (including the
 * generation step at which registration happened) and, in scan/reload, the Guardian and reward lifecycle. It writes one
 * P8-C2-A-compatible dump per start for {@code tools/st01-p8c2a/verify_placement.py}'s per-case checks.
 */
public final class St01C2BProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-c2b-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.c2b.out");
        if (out == null) return;
        String phase = System.getProperty("mm.st01.c2b.phase", "scan");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("phase", phase);
            try {
                run(server, phase, result);
            } catch (Throwable t) {
                LOG.error("ST01 C2B probe failed", t);
                result.put("error", t.toString());
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement e : t.getStackTrace()) sb.append(e).append('\n');
                result.put("error_stack", sb.toString());
            }
            try {
                server.saveEverything(true, true, true);
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
            } catch (Throwable t) {
                LOG.error("could not write/save", t);
            }
            Runtime.getRuntime().halt(0);
        });
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Terrain view over the live ServerLevel (independent of St01PyramidStructure.GeneratorTerrain). */
    static final class ProbeTerrain implements St01Eligibility.Terrain {
        final ServerLevel level;
        final ChunkGenerator generator;
        final RandomState random;
        final Map<Long, Integer> heights = new HashMap<>();

        ProbeTerrain(ServerLevel level) {
            this.level = level;
            this.generator = level.getChunkSource().getGenerator();
            this.random = level.getChunkSource().randomState();
        }

        static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

        @Override public int surfaceHeight(int x, int z) {
            return heights.computeIfAbsent(key(x, z), k -> generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random));
        }

        @Override public boolean isDesertCell(int qx, int qy, int qz) {
            return level.getUncachedNoiseBiome(qx, qy, qz).is(Biomes.DESERT);
        }
    }

    /**
     * Diagnostic only (Policy D keeps no entrance criterion in production): the 25 columns of the row directly outside the
     * authored main-entrance opening (authored X-12..12, Z=+41), rotated about the anchor column.
     */
    static List<int[]> entranceApproach(Orientation orientation) {
        List<int[]> row = new ArrayList<>();
        for (int x = -12; x <= 12; x++) {
            BlockPos q = St01PyramidAssembly.worldPos(new BlockPos(x, 0, 41), BlockPos.ZERO, orientation);
            row.add(new int[]{q.getX(), q.getZ()});
        }
        return row;
    }

    /** Independent re-derivation of the start's seeded rotation (vanilla Structure.GenerationContext.makeRandom). */
    static Rotation rotationFor(long seed, ChunkPos pos) {
        WorldgenRandom r = new WorldgenRandom(new LegacyRandomSource(0L));
        r.setLargeFeatureSeed(seed, pos.x(), pos.z());
        return Rotation.getRandom(r);
    }

    // ------------------------------------------------------------------------------------------------------------------
    private static void run(MinecraftServer server, String phase, Map<String, Object> result) throws Exception {
        ServerLevel level = server.overworld();
        long seed = level.getSeed();
        int radius = Integer.getInteger("mm.st01.c2b.radius", 10240);
        boolean reload = phase.equals("reload");
        result.put("seed", seed);
        result.put("radius_blocks", radius);
        Path dumpDir = Path.of(System.getProperty("mm.st01.c2b.dump", "build/st01-p8c2b/dumps")).resolve(phase);

        // ---- 1. registration audit -------------------------------------------------------------------------------
        Map<String, Object> reg = new LinkedHashMap<>();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        Holder<Structure> holder = structures.get(St01Worldgen.STRUCTURE_ID).orElse(null);
        reg.put("structure_registered", holder != null);
        reg.put("structure_class_ok", holder != null && holder.value() instanceof St01PyramidStructure);
        reg.put("piece_types_registered", BuiltInRegistries.STRUCTURE_PIECE.getKey(St01Worldgen.ASSEMBLY_PIECE) != null
                && BuiltInRegistries.STRUCTURE_PIECE.getKey(St01Worldgen.FOOTPRINT_CORRECTION_PIECE) != null);
        RandomSpreadStructurePlacement placement = null;
        List<String> setsWith = new ArrayList<>();
        for (var entry : sets.entrySet()) {
            for (var sel : entry.getValue().structures()) {
                if (holder != null && sel.structure().value() == holder.value()) {
                    setsWith.add(entry.getKey().identifier().toString());
                    if (entry.getValue().placement() instanceof RandomSpreadStructurePlacement rs) placement = rs;
                }
            }
        }
        reg.put("structure_sets_containing", setsWith);
        if (placement != null) {
            reg.put("spacing", placement.spacing());
            reg.put("separation", placement.separation());
            reg.put("spread_type", placement.spreadType().getSerializedName());
            reg.put("placement_matches_locked_values", placement.spacing() == St01PlacementConfig.SPACING_CHUNKS
                    && placement.separation() == St01PlacementConfig.SEPARATION_CHUNKS
                    && placement.spreadType().getSerializedName().equals(St01PlacementConfig.SPREAD_TYPE));
        }
        result.put("registration", reg);
        if (holder == null || placement == null) throw new IllegalStateException("ST-01 structure/structure-set not registered");
        Structure structure = holder.value();

        // ---- 2. staged prediction for every potential anchor + actual starts ---------------------------------------
        List<ChunkPos> startChunks = new ArrayList<>();
        if (!reload) {
            int spacing = placement.spacing(), radiusChunks = radius / 16;
            int r0 = Math.floorDiv(-radiusChunks, spacing), r1 = Math.floorDiv(radiusChunks - 1, spacing);
            List<int[]> regions = new ArrayList<>();
            for (int rx = r0; rx <= r1; rx++) for (int rz = r0; rz <= r1; rz++) regions.add(new int[]{rx, rz});
            if (phase.equals("reverse")) Collections.reverse(regions);
            if (phase.equals("random")) Collections.shuffle(regions, new Random(seed ^ 0x5EEDL));
            ProbeTerrain terrain = new ProbeTerrain(level);
            int potential = 0, centerDesert = 0, desert = 0, terrainQ = 0, eligible = 0, actual = 0, mismatches = 0;
            Map<String, Integer> rejection = new TreeMap<>();
            Map<String, Integer> eastWestPredicted = new TreeMap<>();
            List<String> mismatchSamples = new ArrayList<>();
            List<Map<String, Object>> rejectedForAudit = new ArrayList<>();
            long t0 = System.currentTimeMillis();
            for (int[] region : regions) {
                ChunkPos pc = placement.getPotentialStructureChunk(seed, region[0] * spacing, region[1] * spacing);
                if (Math.abs(pc.getMiddleBlockX()) > radius || Math.abs(pc.getMiddleBlockZ()) > radius) continue;
                potential++;
                int cx = pc.getMinBlockX() + 8, cz = pc.getMinBlockZ() + 8;
                if (level.getUncachedNoiseBiome(Math.floorDiv(cx, 4), Math.floorDiv(terrain.surfaceHeight(cx, cz) - 1, 4), Math.floorDiv(cz, 4)).is(Biomes.DESERT)) centerDesert++;
                St01Candidate.Decision d = St01Candidate.decide(terrain, pc, rotationFor(seed, pc), seed);
                St01Eligibility.Result res = d.result();
                if (res.biomeQualified()) desert++;
                if (res.terrainQualified()) terrainQ++;
                if (res.eligible()) { eligible++; eastWestPredicted.merge(d.route().orElseThrow().name(), 1, Integer::sum); }
                else rejection.merge(res.verdict().name(), 1, Integer::sum);
                if (res.biomeQualified() && !res.eligible() && rejectedForAudit.size() < Integer.getInteger("mm.st01.c2b.rejectaudit", 2)) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("chunk", pc.x() + "," + pc.z());
                    a.put("verdict", res.verdict().name());
                    a.put("cx", cx); a.put("cz", cz); a.put("yf", res.foundationY());
                    rejectedForAudit.add(a);
                }
                terrain.heights.clear();
                StructureStart start = level.getChunk(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
                boolean valid = start != null && start.isValid();
                if (valid) { actual++; startChunks.add(pc); }
                boolean same = valid == res.eligible();
                if (valid && res.eligible()) {
                    var meta = St01PyramidCompletion.metaOf(start).orElseThrow();
                    same = meta.ground().equals(d.ground().orElseThrow()) && meta.orientation() == d.orientation() && meta.route() == d.route().orElseThrow();
                }
                if (!same) {
                    mismatches++;
                    if (mismatchSamples.size() < 20) mismatchSamples.add("chunk " + pc.x() + "," + pc.z() + " predicted=" + res.verdict() + " actualValid=" + valid);
                }
            }
            double areaKm2 = Math.pow(2.0 * radius / 1000.0, 2);
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("potential_anchors", potential);
            stats.put("center_only_desert_anchors", centerDesert);
            stats.put("full_desert_qualified", desert);
            stats.put("terrain_qualified", terrainQ);
            stats.put("eligible_predicted", eligible);
            stats.put("actual_valid_starts", actual);
            stats.put("predicted_routes", eastWestPredicted);
            stats.put("prediction_vs_actual_mismatches", mismatches);
            stats.put("mismatch_samples", mismatchSamples);
            stats.put("rejection_counts", rejection);
            Map<String, Long> prod = new TreeMap<>();
            for (var e : St01PyramidStructure.VERDICT_COUNTS.entrySet()) prod.put(e.getKey().name(), e.getValue().get());
            stats.put("production_verdict_counts_this_process", prod);
            stats.put("area_km2", areaKm2);
            stats.put("density_per_100km2", actual * 100.0 / areaKm2);
            stats.put("scan_ms", System.currentTimeMillis() - t0);
            result.put("anchor_statistics", stats);
            result.put("rejected_candidate_audit_targets", rejectedForAudit);
        } else {
            JsonObject scan = JsonParser.parseString(Files.readString(Path.of(System.getProperty("mm.st01.c2b.scanResult")))).getAsJsonObject();
            for (String key : scan.getAsJsonObject("starts").keySet()) {
                String[] p = key.split(",");
                startChunks.add(new ChunkPos(Integer.parseInt(p[0]), Integer.parseInt(p[1])));
            }
        }

        // ---- 3. FULL generation, completion handoff and verification of every start --------------------------------
        int maxStarts = Integer.getInteger("mm.st01.c2b.maxStarts", 1000);
        List<ChunkPos> order = new ArrayList<>(startChunks);
        if (phase.equals("reverse")) Collections.reverse(order);
        if (phase.equals("random")) Collections.shuffle(order, new Random(seed ^ 0xC0FFEEL));
        Map<String, Object> starts = new TreeMap<>();
        int completed = 0, done = 0;
        for (ChunkPos pc : order) {
            if (done++ >= maxStarts) break;
            StructureStart start = level.getChunk(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
            Map<String, Object> rec = verifyStart(server, level, structure, start, pc, phase, dumpDir);
            starts.put(pc.x() + "," + pc.z(), rec);
            if (Boolean.TRUE.equals(rec.get("all_checks_pass"))) completed++;
        }
        result.put("starts", starts);
        result.put("completed_pyramids", completed);
        result.put("guard_refused_writes_total", St01WorldgenGuard.REFUSED_WRITES.get());

        // ---- 4. rejected candidates leave nothing ------------------------------------------------------------------
        if (!reload) {
            @SuppressWarnings("unchecked") List<Map<String, Object>> targets = (List<Map<String, Object>>) result.get("rejected_candidate_audit_targets");
            for (Map<String, Object> t : targets) auditRejected(level, structure, t);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    private static List<ChunkPos> generationOrder(BoundingBox box, String phase, long salt) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = (box.minX() >> 4) - 1; x <= (box.maxX() >> 4) + 1; x++)
            for (int z = (box.minZ() >> 4) - 1; z <= (box.maxZ() >> 4) + 1; z++) chunks.add(new ChunkPos(x, z));
        if (phase.equals("reverse")) Collections.reverse(chunks);
        if (phase.equals("random")) Collections.shuffle(chunks, new Random(salt));
        return chunks;
    }

    private static boolean inBoxChunks(BoundingBox box, ChunkPos c) {
        return c.x() >= box.minX() >> 4 && c.x() <= box.maxX() >> 4 && c.z() >= box.minZ() >> 4 && c.z() <= box.maxZ() >> 4;
    }

    private static Map<String, Object> verifyStart(MinecraftServer server, ServerLevel level, Structure structure, StructureStart start,
            ChunkPos pc, String phase, Path dumpDir) throws Exception {
        Map<String, Object> rec = new LinkedHashMap<>();
        var meta = St01PyramidCompletion.metaOf(start).orElseThrow();
        BlockPos ground = meta.ground();
        Orientation o = meta.orientation();
        AccessRoute route = meta.route();
        St01PyramidAssembly.Placement p = St01PyramidAssembly.describe(ground, o, route);
        BoundingBox box = p.completionBounds();
        rec.put("anchor", Map.of("x", ground.getX(), "y", ground.getY(), "z", ground.getZ()));
        rec.put("orientation", o.name());
        rec.put("rotation", o.rotation.name());
        rec.put("route", route.name());
        rec.put("route_rederived", St01RouteSelection.select(level.getSeed(), p.primaryLeft()).name());
        rec.put("start_box_equals_completion_box", start.getBoundingBox().equals(box));
        rec.put("start_piece_count", start.getPieces().size());
        String id = RoyalChestRuntime.canonicalId(level, p.primaryLeft());
        rec.put("canonical_id", id);

        // generation with premature-registration checks
        long refusedBefore = St01WorldgenGuard.REFUSED_WRITES.get();
        List<ChunkPos> order = generationOrder(box, phase, level.getSeed() ^ pc.pack());
        int lastBoxIndex = -1, registeredAt = -1;
        for (int i = 0; i < order.size(); i++) if (inBoxChunks(box, order.get(i))) lastBoxIndex = i;
        boolean preExisting = RoyalChestRuntime.data(level).instance(id).isPresent();
        for (int i = 0; i < order.size(); i++) {
            level.getChunk(order.get(i).x(), order.get(i).z(), ChunkStatus.FULL);
            St01PyramidCompletion.processPending(server);
            if (registeredAt < 0 && RoyalChestRuntime.data(level).instance(id).isPresent()) registeredAt = i;
        }
        rec.put("instance_present_before_generation", preExisting);
        rec.put("generation_chunks", order.size());
        rec.put("last_box_chunk_index", lastBoxIndex);
        rec.put("registered_at_index", registeredAt);
        rec.put("no_premature_registration", preExisting || registeredAt == lastBoxIndex);
        rec.put("guard_refused_writes", St01WorldgenGuard.REFUSED_WRITES.get() - refusedBefore);
        var entry = St01PyramidCompletion.data(level).entry(pc.pack()).orElse(null);
        rec.put("completion_state", entry == null ? null : entry.state().name());
        rec.put("completion_committed_chunks", entry == null ? 0 : entry.committedCount());
        rec.put("completion_detail", entry == null ? null : entry.detail().orElse(null));
        Optional<RoyalChestData.Instance> instance = RoyalChestRuntime.data(level).instance(id);
        rec.put("registered", instance.isPresent());
        // repeated callbacks are idempotent
        var again = St01PyramidCompletion.tryComplete(level, pc.pack());
        rec.put("repeat_completion_idempotent", instance.isPresent() && again.equals(instance));
        rec.put("royal_instance_count_for_anchor", RoyalChestRuntime.data(level).instance(id).isPresent() ? 1 : 0);

        // structure-owned cells vs approved templates; hashes
        Map<Long, BlockState> expected = expectedStates(level, ground, o, route);
        MessageDigest owned = MessageDigest.getInstance("SHA-256"), whole = MessageDigest.getInstance("SHA-256");
        int ownedMismatch = 0, voidNonAir = 0, voidFluid = 0, fluidInBox = 0, nonDesert = 0;
        List<String> mismatchSamples = new ArrayList<>();
        Map<String, Integer> voidLeftovers = new TreeMap<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            m.set(x, y, z);
            BlockState actual = level.getBlockState(m);
            String rel = (x - box.minX()) + "," + (y - box.minY()) + "," + (z - box.minZ()) + "=" + actual + ";";
            whole.update(rel.getBytes(StandardCharsets.UTF_8));
            if (!actual.getFluidState().isEmpty()) fluidInBox++;
            BlockState want = expected.get(m.asLong());
            if (want != null) {
                owned.update(rel.getBytes(StandardCharsets.UTF_8));
                if (!want.equals(actual)) {
                    ownedMismatch++;
                    if (mismatchSamples.size() < 12) mismatchSamples.add(m.toShortString() + " want " + want + " got " + actual);
                }
            } else if (!actual.getFluidState().isEmpty()) {
                voidFluid++; // Policy D: natural fluid next to/inside the structure is an accepted observation, not terrain left behind
            } else if (!actual.isAir()) {
                voidNonAir++;
                voidLeftovers.merge(BuiltInRegistries.BLOCK.getKey(actual.getBlock()).toString(), 1, Integer::sum);
            }
        }
        rec.put("owned_cells", expected.size());
        rec.put("owned_cell_mismatches", ownedMismatch);
        rec.put("owned_mismatch_samples", mismatchSamples);
        rec.put("owned_hash", HexFormat.of().formatHex(owned.digest()));
        rec.put("box_hash", HexFormat.of().formatHex(whole.digest()));
        rec.put("void_cells_non_air", voidNonAir);
        rec.put("void_leftover_blocks", voidLeftovers);
        rec.put("fluid_blocks_in_completion_box", fluidInBox);
        rec.put("fluid_blocks_in_structure_void_cells", voidFluid);

        // fluid in the 1-block shell around the underground block (outside the structure; reported, not sealed)
        int shellFluid = 0;
        BoundingBox under = new BoundingBox(box.minX(), ground.getY() - 32, box.minZ(), box.maxX(), ground.getY() - 1, box.maxZ());
        BoundingBox shell = under.inflatedBy(1);
        for (int x = shell.minX(); x <= shell.maxX(); x++) for (int y = shell.minY(); y <= shell.maxY(); y++) for (int z = shell.minZ(); z <= shell.maxZ(); z++) {
            if (under.isInside(x, y, z)) continue;
            if (!level.getBlockState(m.set(x, y, z)).getFluidState().isEmpty()) shellFluid++;
        }
        rec.put("fluid_blocks_in_1_block_shell_outside_underground", shellFluid);

        // terrain: limits from the generator and the actual neighbouring terrain at the footprint edge
        ProbeTerrain t = new ProbeTerrain(level);
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            int h = t.surfaceHeight(ground.getX() + dx, ground.getZ() + dz);
            min = Math.min(min, h); max = Math.max(max, h);
            var biome = level.getBiome(new BlockPos(ground.getX() + dx, h - 1, ground.getZ() + dz));
            if (!biome.is(Biomes.DESERT)) nonDesert++;
        }
        rec.put("terrain", Map.of("yf", ground.getY(), "min", min, "max", max, "height_difference", max - min,
                "cut", Math.max(0, max - ground.getY()), "skirt", Math.max(0, ground.getY() - min)));
        rec.put("moderate_limits_respected", max - min <= 16 && max - ground.getY() <= 10 && ground.getY() - min <= 8);
        rec.put("non_desert_footprint_columns", nonDesert);
        int edgeUp = 0, edgeDown = 0;
        for (int d = -half - 1; d <= half + 1; d++) {
            for (int[] c : new int[][]{{d, -half - 1}, {d, half + 1}, {-half - 1, d}, {half + 1, d}}) {
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ground.getX() + c[0], ground.getZ() + c[1]);
                edgeUp = Math.max(edgeUp, h - ground.getY());
                edgeDown = Math.max(edgeDown, ground.getY() - h);
            }
        }
        rec.put("edge_ring_max_rise_above_walk_level", edgeUp);
        rec.put("edge_ring_max_drop_below_walk_level", edgeDown);
        // entrance: approach row and opening
        int approachMax = Integer.MIN_VALUE;
        for (int[] d : entranceApproach(o)) {
            approachMax = Math.max(approachMax, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ground.getX() + d[0], ground.getZ() + d[1]) - ground.getY());
        }
        boolean openingClear = true;
        for (int x = -12; x <= 12; x++) for (int y = 0; y <= 11; y++) {
            BlockState b = level.getBlockState(St01PyramidAssembly.worldPos(new BlockPos(x, y, 40), ground, o));
            if (!b.isAir() && b.getFluidState().isEmpty()) openingClear = false; // authored opening: air (natural fluid is an accepted observation)
        }
        rec.put("entrance_approach_max_rise", approachMax);
        rec.put("entrance_opening_clear", openingClear);
        // Policy D: a buried entrance is an accepted outcome, recorded only
        rec.put("entrance_buried", approachMax > 1);
        // no excavation outside the structure: every approach column that the base terrain made solid at its surface is still non-air
        int approachExcavated = 0;
        for (int[] d : entranceApproach(o)) {
            int bx = ground.getX() + d[0], bz = ground.getZ() + d[1];
            int h = t.surfaceHeight(bx, bz);
            if (h - 1 > ground.getY() && level.getBlockState(new BlockPos(bx, h - 1, bz)).isAir()) approachExcavated++;
        }
        rec.put("entrance_approach_columns_excavated", approachExcavated);

        // vanilla structure overlap
        rec.put("vanilla_structure_overlaps", overlaps(level, structure, box));

        // P8-C2-A-compatible dump
        Files.createDirectories(dumpDir);
        writeDump(level, dumpDir.resolve("start-" + pc.x() + "_" + pc.z() + ".json.gz"), box, ground, o, route, p, instance);

        // Guardian and reward lifecycle
        if (instance.isPresent()) rec.put("lifecycle", phase.equals("reload") ? verifyPersisted(level, instance.get(), p) : lifecycle(level, instance.get(), p));

        boolean pass = instance.isPresent() && Boolean.TRUE.equals(rec.get("no_premature_registration")) && ownedMismatch == 0 && voidNonAir == 0
                && nonDesert == 0 && Boolean.TRUE.equals(rec.get("moderate_limits_respected"))
                && openingClear && approachExcavated == 0 && Boolean.TRUE.equals(rec.get("start_box_equals_completion_box"))
                && route.name().equals(rec.get("route_rederived")) && Boolean.TRUE.equals(rec.get("repeat_completion_idempotent"));
        @SuppressWarnings("unchecked") Map<String, Object> lc = (Map<String, Object>) rec.get("lifecycle");
        if (lc != null) pass &= Boolean.TRUE.equals(lc.get("pass"));
        rec.put("all_checks_pass", pass);
        LOG.info("ST01_C2B_START {} {} {}/{} pass={} owned_mismatch={} void_non_air={} registered={}", phase, pc, o, route, pass, ownedMismatch, voidNonAir, instance.isPresent());
        return rec;
    }

    private static List<Map<String, Object>> overlaps(ServerLevel level, Structure st01, BoundingBox box) {
        Map<String, Map<String, Object>> found = new TreeMap<>();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            LevelChunk chunk = level.getChunk(x, z);
            for (var e : chunk.getAllReferences().entrySet()) {
                if (e.getKey() == st01) continue;
                for (long ref : e.getValue()) {
                    ChunkPos sc = ChunkPos.unpack(ref);
                    StructureStart s = level.getChunk(sc.x(), sc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(e.getKey());
                    if (s == null || !s.isValid() || !s.getBoundingBox().intersects(box)) continue;
                    String key = registry.getKey(e.getKey()) + "@" + sc.x() + "," + sc.z();
                    if (found.containsKey(key)) continue;
                    int pieces = 0;
                    for (var piece : s.getPieces()) if (piece.getBoundingBox().intersects(box)) pieces++;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("structure", String.valueOf(registry.getKey(e.getKey())));
                    row.put("start_chunk", sc.x() + "," + sc.z());
                    row.put("step", e.getKey().step().name());
                    row.put("pieces_intersecting_pyramid_box", pieces);
                    found.put(key, row);
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    // ------------------------------------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    static Map<Long, BlockState> expectedStates(ServerLevel level, BlockPos ground, Orientation o, AccessRoute route) throws Exception {
        Map<Long, BlockState> out = new HashMap<>();
        Field palettes = StructureTemplate.class.getDeclaredField("palettes");
        palettes.setAccessible(true);
        var pieces = St01PyramidAssembly.pieces(route);
        for (int i = 0; i < pieces.size(); i++) {
            var piece = pieces.get(i);
            StructureTemplate template = level.getStructureTemplateManager().get(piece.template()).orElseThrow();
            StructurePlaceSettings settings = St01AssemblyPiece.settings(o, route, i);
            BlockPos origin = piece.placementOrigin(ground);
            var list = (List<StructureTemplate.Palette>) palettes.get(template);
            for (StructureTemplate.StructureBlockInfo info : settings.getRandomPalette(list, origin).blocks()) {
                BlockPos pos = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(origin);
                out.put(pos.asLong(), info.state().mirror(Mirror.NONE).rotate(settings.getRotation()));
            }
        }
        return out;
    }

    private static void writeDump(ServerLevel level, Path file, BoundingBox box, BlockPos ground, Orientation o, AccessRoute route,
            St01PyramidAssembly.Placement p, Optional<RoyalChestData.Instance> instance) throws Exception {
        JsonObject out = new JsonObject();
        out.addProperty("orientation", o.name());
        out.addProperty("access", route.name());
        out.addProperty("rotation", o.rotation.name());
        out.addProperty("source", "natural worldgen");
        out.add("ground", vec(ground));
        out.add("frame_min", vec(new BlockPos(box.minX(), box.minY(), box.minZ())));
        out.add("frame_size", vec(new BlockPos(box.getXSpan(), box.getYSpan(), box.getZSpan())));
        Map<String, Integer> lookup = new HashMap<>();
        JsonArray palette = new JsonArray();
        ByteBuffer grid = ByteBuffer.allocate(box.getXSpan() * box.getYSpan() * box.getZSpan() * 2).order(ByteOrder.LITTLE_ENDIAN);
        JsonArray entities = new JsonArray();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            m.set(x, y, z);
            JsonObject s = state(level.getBlockState(m));
            String key = s.toString();
            Integer idx = lookup.get(key);
            if (idx == null) { idx = palette.size(); lookup.put(key, idx); palette.add(s); }
            grid.putShort((short) (int) idx);
            BlockEntity be = level.getBlockEntity(m);
            if (be != null) {
                JsonObject e = new JsonObject();
                e.add("pos", vec(m.subtract(new BlockPos(box.minX(), box.minY(), box.minZ()))));
                e.addProperty("type", String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType())));
                e.addProperty("nbt", be.saveWithFullMetadata(level.registryAccess()).toString());
                if (be instanceof Container c) e.addProperty("empty", c.isEmpty());
                entities.add(e);
            }
        }
        out.add("palette", palette);
        out.addProperty("grid_u16le_xyz_b64", Base64.getEncoder().encodeToString(grid.array()));
        out.add("block_entities", entities);
        JsonObject reg = new JsonObject();
        if (instance.isPresent()) {
            var i = instance.get();
            reg.addProperty("id", i.id());
            reg.add("primary", vec(i.primary()));
            reg.add("primary_right", vec(i.primaryRight().orElseThrow()));
            reg.add("spawn", vec(i.spawn()));
            reg.addProperty("secondary_present", i.secondary().isPresent());
            reg.addProperty("canonical_id_matches", i.id().equals(RoyalChestRuntime.canonicalId(level, p.primaryLeft())));
            reg.addProperty("primary_matches_metadata", i.primary().equals(p.primaryLeft()) && i.primaryRight().orElseThrow().equals(p.primaryRight()));
            reg.addProperty("spawn_matches_metadata", i.spawn().equals(p.guardianSpawn()));
            reg.addProperty("idempotent", St01RoyalChestPlacement.registerPlaced(level, p.d1PlacementOrigin(), p.d1Settings()).equals(instance));
        } else {
            for (String k : new String[]{"canonical_id_matches", "primary_matches_metadata", "spawn_matches_metadata", "idempotent"}) reg.addProperty(k, false);
            reg.addProperty("secondary_present", false);
        }
        out.add("registration", reg);
        JsonObject meta = new JsonObject();
        meta.add("primary_left", vec(p.primaryLeft()));
        meta.add("primary_right", vec(p.primaryRight()));
        meta.add("guardian", vec(p.guardianSpawn()));
        meta.add("treasury_ui_anchor", vec(p.treasuryUiAnchor()));
        meta.add("d1_placement_origin", vec(p.d1PlacementOrigin()));
        out.add("metadata", meta);
        try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(file))) {
            os.write(new Gson().toJson(out).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static JsonArray vec(BlockPos p) {
        JsonArray a = new JsonArray(); a.add(p.getX()); a.add(p.getY()); a.add(p.getZ()); return a;
    }

    private static JsonObject state(BlockState state) {
        JsonObject s = new JsonObject();
        s.addProperty("id", String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        JsonObject props = new JsonObject();
        for (Property<?> property : state.getProperties()) props.addProperty(property.getName(), value(state, property));
        if (!props.isEmpty()) s.add("properties", props);
        return s;
    }

    private static <T extends Comparable<T>> String value(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    // ------------------------------------------------------------------------------------------------------------------
    private static ServerPlayer mockPlayer(ServerLevel level) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.nameUUIDFromBytes("st01-c2b".getBytes()), "st01-c2b-probe"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override public GameType gameMode() { return GameType.SURVIVAL; }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    private static void dropPlayer(ServerLevel level, ServerPlayer player) {
        player.closeContainer();
        level.getServer().getPlayerList().remove(player);
    }

    private static int countCatalysts(Container container) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(RoyalChestRewardModel.CATALYST_ID)) count += stack.getCount();
        }
        return count;
    }

    private static int guardians(ServerLevel level, BlockPos anchor) {
        return level.getEntities(dev.itzgamesgd.moreminecrafts.boss.TombGuardianEntities.TOMB_GUARDIAN, g -> anchor.equals(g.tombAnchor())).size();
    }

    /** Scan: awakening, defeat, permanent unsealing, one-time generation, withdraw-only, no duplicate Guardian or rewards. */
    private static Map<String, Object> lifecycle(ServerLevel level, RoyalChestData.Instance instance, St01PyramidAssembly.Placement p) {
        Map<String, Object> r = new LinkedHashMap<>();
        ServerPlayer player = mockPlayer(level);
        try {
            BlockPos left = instance.primary(), right = instance.primaryRight().orElseThrow(), anchor = instance.tombAnchor();
            var data = RoyalChestRuntime.data(level);
            r.put("left_and_right_route_to_one_primary", data.linkAt(left).orElseThrow().instance().id().equals(data.linkAt(right).orElseThrow().instance().id())
                    && data.linkAt(right).orElseThrow().role() == RoyalChestRole.PRIMARY && data.linkAt(left).orElseThrow().role() == RoyalChestRole.PRIMARY);
            r.put("secondary_absent", instance.secondary().isEmpty());
            r.put("sealed_before", TombGuardianEncounters.isRoyalChestSealed(level, anchor));
            r.put("first_open", RoyalChestRuntime.tryOpen(level, player, right).name());
            r.put("guardians_after_awakening", guardians(level, anchor));
            r.put("second_sealed_open", RoyalChestRuntime.tryOpen(level, player, left).name());
            r.put("guardians_after_second_open", guardians(level, anchor));
            r.put("spawn_matches_metadata", instance.spawn().equals(p.guardianSpawn()));
            var guardian = level.getEntities(dev.itzgamesgd.moreminecrafts.boss.TombGuardianEntities.TOMB_GUARDIAN, g -> anchor.equals(g.tombAnchor())).getFirst();
            guardian.skipAwakening();
            guardian.hurtServer(level, level.damageSources().playerAttack(player), 10000.0F);
            r.put("defeated", TombGuardianEncounters.isDefeated(level, anchor));
            r.put("sealed_after_defeat", TombGuardianEncounters.isRoyalChestSealed(level, anchor));
            r.put("open_after_defeat", RoyalChestRuntime.tryOpen(level, player, left).name());
            player.closeContainer();
            var chest = data.chest(instance.id(), RoyalChestRole.PRIMARY).orElseThrow().container();
            List<String> contents = new ArrayList<>();
            for (int i = 0; i < chest.getContainerSize(); i++) if (!chest.getItem(i).isEmpty()) contents.add(i + ":" + BuiltInRegistries.ITEM.getKey(chest.getItem(i).getItem()) + "x" + chest.getItem(i).getCount());
            r.put("contents_after_generation", contents);
            r.put("container_slots", chest.getContainerSize());
            r.put("catalysts_in_primary", countCatalysts(chest));
            r.put("catalyst_assignment", instance.catalyst().map(Enum::name).orElse("NONE"));
            r.put("reopen_right", RoyalChestRuntime.tryOpen(level, player, right).name());
            player.closeContainer();
            r.put("reopen_same_container", data.chest(instance.id(), RoyalChestRole.PRIMARY).orElseThrow().container() == chest);
            boolean insertRefused = !chest.canPlaceItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT));
            r.put("withdraw_only_insert_refused", insertRefused);
            String withdrawn = chest.getItem(0).isEmpty() ? null : BuiltInRegistries.ITEM.getKey(chest.getItem(0).getItem()) + "x" + chest.getItem(0).getCount();
            chest.removeItemNoUpdate(0);
            r.put("withdrawn_slot0", withdrawn);
            r.put("guardian_restart_after_defeat", TombGuardianEncounters.onRoyalChestInteraction(level, anchor, instance.spawn(), player).name());
            r.put("guardians_final", guardians(level, anchor));
            r.put("registration_idempotent_after_rewards", St01RoyalChestPlacement.registerPlaced(level, p.d1PlacementOrigin(), p.d1Settings()).orElseThrow().equals(instance)
                    && data.chest(instance.id(), RoyalChestRole.PRIMARY).orElseThrow().container() == chest);
            r.put("royal_data_json", RoyalChestData.TYPE.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data).getOrThrow().toString());
            boolean pass = Boolean.TRUE.equals(r.get("left_and_right_route_to_one_primary")) && instance.secondary().isEmpty()
                    && Boolean.TRUE.equals(r.get("sealed_before")) && "SEALED_ENCOUNTER_STARTED".equals(r.get("first_open"))
                    && (int) r.get("guardians_after_awakening") == 1 && "SEALED".equals(r.get("second_sealed_open"))
                    && (int) r.get("guardians_after_second_open") == 1 && Boolean.TRUE.equals(r.get("defeated"))
                    && Boolean.FALSE.equals(r.get("sealed_after_defeat")) && "OPENED_AND_GENERATED".equals(r.get("open_after_defeat"))
                    && "OPENED".equals(r.get("reopen_right")) && Boolean.TRUE.equals(r.get("reopen_same_container"))
                    && (int) r.get("container_slots") == 54 && "ALREADY_DEFEATED".equals(r.get("guardian_restart_after_defeat"))
                    && Boolean.TRUE.equals(r.get("registration_idempotent_after_rewards")) && insertRefused
                    && (int) r.get("catalysts_in_primary") == (instance.catalyst().filter(RoyalChestRole.PRIMARY::equals).isPresent() ? 1 : 0);
            r.put("pass", pass);
        } catch (RuntimeException e) {
            r.put("error", e.toString());
            r.put("pass", false);
        } finally {
            dropPlayer(level, player);
        }
        return r;
    }

    /** Reload: state from the scan phase survived a normal server restart, unchanged. */
    private static Map<String, Object> verifyPersisted(ServerLevel level, RoyalChestData.Instance instance, St01PyramidAssembly.Placement p) {
        Map<String, Object> r = new LinkedHashMap<>();
        ServerPlayer player = mockPlayer(level);
        try {
            BlockPos anchor = instance.tombAnchor();
            var data = RoyalChestRuntime.data(level);
            r.put("defeat_persisted", TombGuardianEncounters.isDefeated(level, anchor) && !TombGuardianEncounters.isRoyalChestSealed(level, anchor));
            r.put("generated_persisted", data.isGenerated(instance.id(), RoyalChestRole.PRIMARY));
            r.put("royal_data_json", RoyalChestData.TYPE.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data).getOrThrow().toString());
            r.put("open_after_restart", RoyalChestRuntime.tryOpen(level, player, instance.primaryRight().orElseThrow()).name());
            player.closeContainer();
            r.put("guardian_restart_after_reload", TombGuardianEncounters.onRoyalChestInteraction(level, anchor, instance.spawn(), player).name());
            r.put("guardians", guardians(level, anchor));
            r.put("secondary_absent", instance.secondary().isEmpty());
            r.put("pass", Boolean.TRUE.equals(r.get("defeat_persisted")) && Boolean.TRUE.equals(r.get("generated_persisted"))
                    && "OPENED".equals(r.get("open_after_restart")) && "ALREADY_DEFEATED".equals(r.get("guardian_restart_after_reload"))
                    && (int) r.get("guardians") == 0 && instance.secondary().isEmpty());
        } catch (RuntimeException e) {
            r.put("error", e.toString());
            r.put("pass", false);
        } finally {
            dropPlayer(level, player);
        }
        return r;
    }

    /** A rejected candidate: its real chunks hold no ST-01 start, no reference and no completion record. */
    private static void auditRejected(ServerLevel level, Structure structure, Map<String, Object> t) {
        int cx = (int) t.get("cx"), cz = (int) t.get("cz"), half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        int refs = 0, gold = 0;
        for (int x = (cx - half) >> 4; x <= (cx + half) >> 4; x++) for (int z = (cz - half) >> 4; z <= (cz + half) >> 4; z++) {
            ChunkAccess c = level.getChunk(x, z, ChunkStatus.FULL);
            refs += c.getReferencesForStructure(structure).size();
        }
        int yf = (int) t.get("yf"), unattributed = 0;
        List<String> markers = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = cx - half; x <= cx + half; x++) for (int z = cz - half; z <= cz + half; z++) for (int y = yf - 32; y <= yf + 39; y++) {
            BlockState b = level.getBlockState(m.set(x, y, z));
            if (b.is(Blocks.GOLD_BLOCK) || b.is(Blocks.DIAMOND_BLOCK) || b.is(Blocks.LAPIS_BLOCK)) {
                gold++;
                // attribute the block: a vanilla structure covering it (e.g. a ruined portal's gold blocks) is not ST-01
                var owners = level.structureManager().getAllStructuresAt(m.immutable());
                List<String> names = new ArrayList<>();
                var reg = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
                for (var st : owners.keySet()) names.add(String.valueOf(reg.getKey(st)));
                boolean st01 = owners.keySet().stream().anyMatch(st -> st instanceof St01PyramidStructure);
                if (names.isEmpty() || st01) unattributed++;
                if (markers.size() < 10) markers.add(m.toShortString() + " " + BuiltInRegistries.BLOCK.getKey(b.getBlock()) + " in " + names);
            }
        }
        ChunkPos pc = new ChunkPos(cx >> 4, cz >> 4);
        t.put("start_present", level.getChunk(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure) != null);
        t.put("st01_references_in_footprint_chunks", refs);
        t.put("pyramid_marker_blocks_in_would_be_box", gold);
        t.put("marker_blocks_not_attributable_to_a_vanilla_structure", unattributed);
        t.put("marker_samples", markers);
        t.put("completion_record_present", St01PyramidCompletion.data(level).entry(pc.pack()).isPresent());
        t.put("nothing_left", refs == 0 && unattributed == 0 && !(boolean) t.get("start_present") && !(boolean) t.get("completion_record_present"));
    }
}
