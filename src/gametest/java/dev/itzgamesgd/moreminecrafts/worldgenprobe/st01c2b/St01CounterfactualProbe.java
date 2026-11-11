package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01c2b;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RouteSelection;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Candidate;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Eligibility;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PlacementConfig;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ST-01 P8-C2-B counterfactual rarity audit probe (DIAGNOSTIC ONLY; inert unless {@code -Dmm.st01.cf.out} is set; not part
 * of build/check; never part of the shipped mod).
 *
 * <p>For ONE seed it enumerates exactly the anchors of {@code St01C2BProbe} (same placement object, same radius, same
 * order) and, for every candidate whose full 81x81 footprint is Desert, records an immutable observation of the raw
 * measurements: the 81x81 height grid, the entrance approach row, every fluid block of the inspection volume (run-length
 * encoded), the hypothetical route and vanilla-structure overlaps. Entrance and fluid conditions are observed
 * independently of each other, including for candidates that production rejects at an earlier or later stage.
 *
 * <p>Nothing here changes generation: the observations read the same generator state production reads
 * ({@code getBaseHeight(WORLD_SURFACE_WG)} and {@code getBaseColumn}, i.e. the noise terrain before carvers, features and
 * structures). The production verdict is obtained from the unmodified production {@link St01Candidate#decide}. The
 * scenario flags written here are a convenience; {@code tools/st01-p8c2b-audit/analyze.py} recomputes everything from the
 * raw observations without using them.
 */
public final class St01CounterfactualProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("st01-cf-probe");
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final int KIND_WATER = 1, KIND_LAVA = 2, KIND_OTHER = 3;

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.st01.cf.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> result = new LinkedHashMap<>();
            try {
                run(server, result);
            } catch (Throwable t) {
                LOG.error("ST01 CF probe failed", t);
                result.put("error", t.toString());
            }
            try {
                Path p = Path.of(out);
                if (p.getParent() != null) Files.createDirectories(p.getParent());
                Files.writeString(p, GSON.toJson(result), StandardCharsets.UTF_8);
            } catch (Throwable t) {
                LOG.error("could not write result", t);
            }
            Runtime.getRuntime().halt(0);
        });
    }

    private static void run(MinecraftServer server, Map<String, Object> result) throws Exception {
        ServerLevel level = server.overworld();
        long seed = level.getSeed();
        int radius = Integer.getInteger("mm.st01.c2b.radius", 10240);
        result.put("schema", "st01-cf-observations/1");
        result.put("seed", seed);
        result.put("dimension", level.dimension().identifier().toString());
        result.put("radius_blocks", radius);

        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        Holder<Structure> holder = structures.get(St01Worldgen.STRUCTURE_ID).orElseThrow();
        Structure structure = holder.value();
        RandomSpreadStructurePlacement placement = null;
        for (var entry : sets.entrySet()) {
            for (var sel : entry.getValue().structures()) {
                if (sel.structure().value() == structure && entry.getValue().placement() instanceof RandomSpreadStructurePlacement rs) placement = rs;
            }
        }
        if (placement == null) throw new IllegalStateException("ST-01 structure set not registered");
        result.put("placement", Map.of("spacing", placement.spacing(), "separation", placement.separation(),
                "spread_type", placement.spreadType().getSerializedName(), "salt", St01PlacementConfig.SALT));

        int spacing = placement.spacing(), radiusChunks = radius / 16;
        int r0 = Math.floorDiv(-radiusChunks, spacing), r1 = Math.floorDiv(radiusChunks - 1, spacing);
        St01C2BProbe.ProbeTerrain terrain = new St01C2BProbe.ProbeTerrain(level);
        int half = St01PlacementConfig.FOOTPRINT_HALF_WIDTH;
        int margin = St01PlacementConfig.FLUID_MARGIN;
        St01Eligibility.Volume vol = St01PlacementConfig.UNDERGROUND_VOLUME;
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        int potential = 0, actualStarts = 0;
        Map<String, Integer> productionVerdicts = new TreeMap<>();
        List<Map<String, Object>> candidates = new ArrayList<>();
        Map<String, String> heightGrids = new LinkedHashMap<>();
        List<String> actualStartChunks = new ArrayList<>();
        long t0 = System.currentTimeMillis();

        for (int rx = r0; rx <= r1; rx++) {
            for (int rz = r0; rz <= r1; rz++) {
                ChunkPos pc = placement.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                if (Math.abs(pc.getMiddleBlockX()) > radius || Math.abs(pc.getMiddleBlockZ()) > radius) continue;
                potential++;
                terrain.heights.clear();
                Rotation rotation = St01C2BProbe.rotationFor(seed, pc);
                St01Candidate.Decision decision = St01Candidate.decide(terrain, pc, rotation, seed);  // UNMODIFIED production decision
                productionVerdicts.merge(decision.result().verdict().name(), 1, Integer::sum);

                StructureStart start = level.getChunk(pc.x(), pc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
                boolean actualValid = start != null && start.isValid();
                if (actualValid) { actualStarts++; actualStartChunks.add(pc.x() + "," + pc.z()); }

                int cx = pc.getMinBlockX() + 8, cz = pc.getMinBlockZ() + 8;
                boolean footprintDesert = St01Eligibility.fullFootprintDesert(terrain, cx, cz);
                if (!footprintDesert) continue;   // only the 221-type population is observed in detail

                Orientation orientation = St01Candidate.orientationOf(rotation);
                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("seed", seed);
                rec.put("dimension", "minecraft:overworld");
                rec.put("chunk_x", pc.x());
                rec.put("chunk_z", pc.z());
                rec.put("anchor_x", cx);
                rec.put("anchor_z", cz);
                rec.put("rotation", rotation.name());
                rec.put("orientation", orientation.name());
                rec.put("footprint_desert", true);
                rec.put("actual_valid_start", actualValid);
                rec.put("production_verdict", decision.result().verdict().name());

                // ---- terrain: the full 81x81 grid (row-major, dx outer, dz inner), then derived values -----------------
                int n = 2 * half + 1;
                short[] grid = new short[n * n];
                int[] sorted = new int[n * n];
                int k = 0;
                for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
                    int h = terrain.surfaceHeight(cx + dx, cz + dz);
                    grid[k] = (short) h; sorted[k++] = h;
                }
                java.util.Arrays.sort(sorted);
                int min = sorted[0], max = sorted[sorted.length - 1], yf = sorted[sorted.length / 2];
                rec.put("min_height", min);
                rec.put("max_height", max);
                rec.put("foundation_y", yf);
                rec.put("height_variation", max - min);
                rec.put("required_cut", Math.max(0, max - yf));
                rec.put("required_skirt", Math.max(0, yf - min));
                ByteBuffer bb = ByteBuffer.allocate(grid.length * 2).order(ByteOrder.LITTLE_ENDIAN);
                for (short s : grid) bb.putShort(s);
                heightGrids.put(pc.x() + "," + pc.z(), Base64.getEncoder().encodeToString(bb.array()));
                boolean stubDesert = terrain.isDesertCell(Math.floorDiv(cx, 4), Math.floorDiv(yf, 4), Math.floorDiv(cz, 4));
                rec.put("stub_cell_desert", stubDesert);

                // ---- entrance: the production approach row (raw heights), and the inside edge row -------------------
                List<int[]> approach = St01Candidate.entranceApproach(orientation);
                int[] approachHeights = new int[approach.size()];
                for (int i = 0; i < approach.size(); i++) approachHeights[i] = terrain.surfaceHeight(cx + approach.get(i)[0], cz + approach.get(i)[1]);
                rec.put("entrance_approach_offsets", approach.stream().map(a -> new int[]{a[0], a[1]}).toList());
                rec.put("entrance_approach_heights", approachHeights);
                int insideEdgeMax = Integer.MIN_VALUE;
                for (int x = -St01PlacementConfig.ENTRANCE_HALF_WIDTH; x <= St01PlacementConfig.ENTRANCE_HALF_WIDTH; x++) {
                    BlockPos p = St01PyramidAssembly.worldPos(new BlockPos(x, 0, half), BlockPos.ZERO, orientation);
                    insideEdgeMax = Math.max(insideEdgeMax, terrain.surfaceHeight(cx + p.getX(), cz + p.getZ()));
                }
                rec.put("entrance_inside_edge_row_max_height", insideEdgeMax);

                // ---- geometry vectors (authored -> world offsets) so the analysis rotates fluid cells exactly ----------
                BlockPos ux = St01PyramidAssembly.worldPos(new BlockPos(1, 0, 0), BlockPos.ZERO, orientation);
                BlockPos uz = St01PyramidAssembly.worldPos(new BlockPos(0, 0, 1), BlockPos.ZERO, orientation);
                rec.put("authored_unit_x_to_world", new int[]{ux.getX(), ux.getZ()});
                rec.put("authored_unit_z_to_world", new int[]{uz.getX(), uz.getZ()});

                // ---- route (production function; hypothetical unless the candidate is production-eligible) ---------
                BlockPos ground = new BlockPos(cx, yf, cz);
                BlockPos tombAnchor = St01PyramidAssembly.worldPos(St01RoyalChestPlacement.REFERENCE_ANCHOR, ground, orientation);
                AccessRoute route = St01RouteSelection.select(seed, tombAnchor);
                rec.put("route", route.name());
                rec.put("tomb_anchor", new int[]{tombAnchor.getX(), tombAnchor.getY(), tombAnchor.getZ()});

                // ---- fluid: every fluid cell of the production inspection region, run-length per column ---------------
                int minX = cx + vol.minDx() - margin, maxX = cx + vol.maxDx() + margin;
                int minZ = cz + vol.minDz() - margin, maxZ = cz + vol.maxDz() + margin;
                int minY = Math.max(yf + vol.minDy() - margin, level.getMinY()), maxY = Math.min(yf + vol.maxDy() + margin, level.getMaxY());
                rec.put("fluid_inspection_bounds", new int[]{minX, minY, minZ, maxX, maxY, maxZ});
                List<int[]> runs = new ArrayList<>();
                long water = 0, lava = 0, other = 0;
                for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
                    NoiseColumn column = terrain.generator.getBaseColumn(x, z, level, terrain.random);
                    int runStart = 0, runKind = 0;
                    for (int y = minY; y <= maxY + 1; y++) {
                        int kind = 0;
                        if (y <= maxY) {
                            BlockState bs = column.getBlock(y);
                            var fs = bs.getFluidState();
                            if (!fs.isEmpty()) kind = fs.is(FluidTags.WATER) ? KIND_WATER : fs.is(FluidTags.LAVA) ? KIND_LAVA : KIND_OTHER;
                        }
                        if (kind != runKind) {
                            if (runKind != 0) {
                                int len = y - runStart;
                                runs.add(new int[]{x - cx, z - cz, runStart - yf, y - 1 - yf, runKind});
                                if (runKind == KIND_WATER) water += len; else if (runKind == KIND_LAVA) lava += len; else other += len;
                            }
                            runStart = y; runKind = kind;
                        }
                    }
                }
                rec.put("fluid_runs_dx_dz_dy0_dy1_kind", runs);
                rec.put("fluid_blocks_total_water_lava_other", new long[]{water, lava, other});

                // ---- vanilla overlaps (pyramid box, only observable via structure references; terrain-qualified only) --
                boolean terrainQualified = (max - min) <= St01PlacementConfig.MAX_HEIGHT_DIFFERENCE
                        && Math.max(0, max - yf) <= St01PlacementConfig.MAX_CUT && Math.max(0, yf - min) <= St01PlacementConfig.MAX_SKIRT && stubDesert;
                if (terrainQualified) {
                    BoundingBox box = new BoundingBox(cx - half, yf - 32, cz - half, cx + half, yf + 39, cz + half);
                    rec.put("vanilla_overlaps", overlaps(level, registry, structure, box, approach, cx, cz, yf, minX, minY, minZ, maxX, maxY, maxZ));
                }
                candidates.add(rec);
            }
        }
        result.put("potential_anchors", potential);
        result.put("production_verdict_counts", productionVerdicts);
        result.put("actual_valid_starts", actualValid(actualStarts, actualStartChunks));
        result.put("candidates", candidates);
        result.put("height_grids_int16le_b64", heightGrids);
        result.put("scan_ms", System.currentTimeMillis() - t0);
        LOG.info("ST01_CF seed={} potential={} desertCandidates={} actualStarts={}", seed, potential, candidates.size(), actualStarts);
    }

    private static Map<String, Object> actualValid(int count, List<String> chunks) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", count);
        m.put("chunks", chunks);
        return m;
    }

    /** Structure references of the pyramid box's chunks (STRUCTURE_REFERENCES status only: no full generation). */
    private static List<Map<String, Object>> overlaps(ServerLevel level, net.minecraft.core.Registry<Structure> registry, Structure st01,
            BoundingBox box, List<int[]> approach, int cx, int cz, int yf, int fMinX, int fMinY, int fMinZ, int fMaxX, int fMaxY, int fMaxZ) {
        Map<String, Map<String, Object>> found = new TreeMap<>();
        BoundingBox fluidRegion = new BoundingBox(fMinX, fMinY, fMinZ, fMaxX, fMaxY, fMaxZ);
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            ChunkAccess chunk = level.getChunk(x, z, ChunkStatus.STRUCTURE_REFERENCES);
            for (var e : chunk.getAllReferences().entrySet()) {
                if (e.getKey() == st01) continue;
                for (long ref : e.getValue()) {
                    ChunkPos sc = ChunkPos.unpack(ref);
                    StructureStart s = level.getChunk(sc.x(), sc.z(), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(e.getKey());
                    if (s == null || !s.isValid() || !s.getBoundingBox().intersects(box)) continue;
                    String key = registry.getKey(e.getKey()) + "@" + sc.x() + "," + sc.z();
                    if (found.containsKey(key)) continue;
                    int piecesBox = 0, piecesFluid = 0, piecesApproach = 0;
                    for (var piece : s.getPieces()) {
                        BoundingBox pb = piece.getBoundingBox();
                        if (pb.intersects(box)) piecesBox++;
                        if (pb.intersects(fluidRegion)) piecesFluid++;
                        for (int[] a : approach) {
                            if (pb.minX() <= cx + a[0] && cx + a[0] <= pb.maxX() && pb.minZ() <= cz + a[1] && cz + a[1] <= pb.maxZ()) { piecesApproach++; break; }
                        }
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("structure", String.valueOf(registry.getKey(e.getKey())));
                    row.put("start_chunk", sc.x() + "," + sc.z());
                    row.put("pieces_intersecting_pyramid_box", piecesBox);
                    row.put("pieces_intersecting_fluid_region", piecesFluid);
                    row.put("pieces_over_entrance_approach_columns_any_y", piecesApproach);
                    found.put(key, row);
                }
            }
        }
        return new ArrayList<>(found.values());
    }
}
