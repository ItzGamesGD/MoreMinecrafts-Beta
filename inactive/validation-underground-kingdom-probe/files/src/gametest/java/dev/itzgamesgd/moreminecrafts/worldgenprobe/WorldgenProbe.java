package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EXPERIMENTAL worldgen probe (not production). Inert unless -Dmm.probe.out is set.
 *
 * <p>Runs once the dedicated server has started: picks the first N deterministic start chunks of
 * {@code moreminecrafts:probe_kingdom} around the world origin, forces the surrounding chunks to FULL,
 * then measures the generated blocks of every sample and writes one JSON document. Driven by
 * tools/worldgen-probe/run_matrix.sh; see docs/UNDERGROUND_KINGDOM_WORLDGEN_PROBE.md.
 */
public final class WorldgenProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("worldgen-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] CORE = {"central_chamber", "corridor_a", "corridor_b", "combat_room", "end_cap"};
    private static final int REGION_CHUNKS = 5; // conservative: structure radius <= ~52 blocks + 12 block terrain halo
    private static final int RING = 10;
    private static final int RING_STRIDE = 2;

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.probe.out");
        if (out == null) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                run(server, Path.of(out));
            } catch (Throwable t) {
                LOG.error("worldgen probe failed", t);
                writeFailure(Path.of(out), t);
            }
            // Results are on disk. Skip the (very slow) save of ~1000s of forced chunks; the world is throwaway.
            Runtime.getRuntime().halt(0);
        });
    }

    private static void writeFailure(Path out, Throwable t) {
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, GSON.toJson(Map.of("probe_error", String.valueOf(t))), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private record Piece(String kind, BoundingBox box) {
    }

    private static void run(MinecraftServer server, Path out) throws IOException {
        int wanted = Integer.getInteger("mm.probe.samples", 16);
        int searchRadius = Integer.getInteger("mm.probe.searchRadius", 90);
        ServerLevel level = server.overworld();
        ChunkGeneratorStructureState genState = level.getChunkSource().getGeneratorState();
        Holder<Structure> probe = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
            .getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("moreminecrafts", "probe_kingdom")));
        List<StructurePlacement> placements = genState.getPlacementsForStructure(probe);

        List<ChunkPos> candidates = new ArrayList<>();
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (StructurePlacement p : placements) {
                    if (p.isStructureChunk(genState, x, z)) {
                        candidates.add(new ChunkPos(x, z));
                        break;
                    }
                }
            }
        }
        int minSurface = Integer.getInteger("mm.probe.minSurface", Integer.MIN_VALUE);
        if (minSurface != Integer.MIN_VALUE) {
            // Targeted terrain selection: keep only start chunks whose base (pre-structure) terrain height is high
            // (mountains). getBaseHeight evaluates the density function only; it does not generate chunks.
            candidates.removeIf(c -> level.getChunkSource().getGenerator().getBaseHeight(c.getMiddleBlockX(), c.getMiddleBlockZ(),
                Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState()) < minSurface);
        }
        candidates.sort(Comparator.comparingInt((ChunkPos c) -> Math.max(Math.abs(c.x()), Math.abs(c.z())))
            .thenComparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z));
        if (candidates.size() > wanted) {
            candidates = candidates.subList(0, wanted);
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("seed", level.getSeed());
        doc.put("label", System.getProperty("mm.probe.label", ""));
        doc.put("candidate_start_chunks", candidates.size());
        List<Map<String, Object>> samples = new ArrayList<>();
        List<String> slices = new ArrayList<>();
        long genNanos = 0;
        int chunksTouched = 0;
        for (ChunkPos start : candidates) {
            long t0 = System.nanoTime();
            for (int dx = -REGION_CHUNKS; dx <= REGION_CHUNKS; dx++) {
                for (int dz = -REGION_CHUNKS; dz <= REGION_CHUNKS; dz++) {
                    level.setChunkForced(start.x() + dx, start.z() + dz, true);
                    level.getChunk(start.x() + dx, start.z() + dz, ChunkStatus.FULL, true);
                    chunksTouched++;
                }
            }
            genNanos += System.nanoTime() - t0;
            Map<String, Object> sample = measure(level, probe.value(), start);
            samples.add(sample);
            if (slices.size() < Integer.getInteger("mm.probe.slices", 0) && Boolean.TRUE.equals(sample.get("generated"))) {
                slices.add("=== start chunk " + sample.get("start_chunk") + " box " + sample.get("box") + "\n"
                    + slice(level, probe.value(),
                        level.getChunk(start.x(), start.z(), ChunkStatus.FULL, true).getStartForStructure(probe.value())));
            }
        }
        doc.put("gen_ms", genNanos / 1_000_000);
        doc.put("chunks_requested", chunksTouched);
        doc.put("samples", samples);
        Files.createDirectories(out.getParent());
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            GSON.toJson(doc, w);
        }
        if (!slices.isEmpty()) {
            Files.writeString(Path.of(out.toString().replace(".json", ".slices.txt")),
                "legend: # solid block on a structure box surface (built shell, or natural rock for bare templates) | + passable cell on a box surface (doorway or breach) | . room air | o foreign/decor block in room\n"
                    + "        ~ water | L lava | , open cave air | (space) solid terrain\n\n" + String.join("\n", slices), StandardCharsets.UTF_8);
        }
        LOG.info("worldgen probe wrote {} samples to {}", samples.size(), out);
    }

    private static Map<String, Object> measure(ServerLevel level, Structure probe, ChunkPos startChunk) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("start_chunk", startChunk.x() + "," + startChunk.z());
        ChunkAccess chunk = level.getChunk(startChunk.x(), startChunk.z(), ChunkStatus.FULL, true);
        StructureStart start = chunk.getStartForStructure(probe);
        if (start == null || !start.isValid()) {
            m.put("generated", false);
            m.put("start_state", start == null ? "absent" : "invalid");
            return m;
        }
        List<Piece> pieces = new ArrayList<>();
        List<String> signature = new ArrayList<>();
        for (StructurePiece sp : start.getPieces()) {
            String kind = "other";
            if (sp instanceof PoolElementStructurePiece pp) {
                String element = pp.getElement().toString();
                for (String core : CORE) {
                    if (element.contains("probe/" + core)) {
                        kind = core;
                    }
                }
            }
            pieces.add(new Piece(kind, sp.getBoundingBox()));
            BoundingBox b = sp.getBoundingBox();
            signature.add(kind + ":" + b.minX() + "," + b.minY() + "," + b.minZ() + ":" + b.maxX() + "," + b.maxY() + "," + b.maxZ());
        }
        signature.sort(String::compareTo);
        m.put("generated", true);
        pieces.stream().filter(p -> p.kind().equals("central_chamber")).findFirst()
            .ifPresent(p -> m.put("central_min_y", p.box().minY()));
        m.put("pieces_total", pieces.size());
        m.put("layout_signature", String.join(";", signature));

        List<Piece> core = pieces.stream().filter(p -> !p.kind().equals("other")).toList();
        long coreKinds = core.stream().map(Piece::kind).distinct().count();
        m.put("core_pieces_present", coreKinds);
        // StructureStart#getBoundingBox() is inflated by the terrain-adaptation margin (12) for any adaptation
        // except none, so the union of the real piece boxes is the physical footprint.
        BoundingBox union = pieces.get(0).box();
        for (Piece p : pieces) {
            union = BoundingBox.encapsulating(union, p.box());
        }
        BoundingBox sb = start.getBoundingBox();
        m.put("start_box_reported", sb.minX() + "," + sb.minY() + "," + sb.minZ() + "," + sb.maxX() + "," + sb.maxY() + "," + sb.maxZ());
        m.put("box", union.minX() + "," + union.minY() + "," + union.minZ() + "," + union.maxX() + "," + union.maxY() + "," + union.maxZ());

        Piece central = core.stream().filter(p -> p.kind().equals("central_chamber")).findFirst().orElse(null);
        Map<String, Object> pieceStats = new LinkedHashMap<>();
        long leak = 0;
        Map<String, Long> reachedPerPiece = new LinkedHashMap<>();
        if (central != null) {
            BlockPos seed = findAirSeed(level, central.box());
            if (seed != null) {
                long[] res = flood(level, seed, core, union, reachedPerPiece);
                leak = res[1];
                m.put("flood_reached_cells", res[0]);
            } else {
                m.put("flood_reached_cells", -1);
            }
        }
        m.put("breach_leak_cells", leak);

        long disconnected = 0;
        long intrusionTotal = 0;
        long foreignTotal = 0;
        long fluidInteriorTotal = 0;
        long shellHolesTotal = 0;
        long exposedAir = 0;
        long exposedWater = 0;
        long exposedLava = 0;
        long exposedOther = 0;
        for (Piece p : core) {
            Map<String, Object> ps = new LinkedHashMap<>();
            long interiorCells = 0;
            Map<String, Integer> intrusionBlocks = new LinkedHashMap<>();
            List<String> intrusionAt = new ArrayList<>();
            long intrusion = 0;
            long foreign = 0;
            long lichen = 0;
            List<String> foreignAt = new ArrayList<>();
            Map<String, Integer> foreignIds = new LinkedHashMap<>();
            long fluid = 0;
            long holes = 0;
            BoundingBox b = p.box();
            for (int x = b.minX(); x <= b.maxX(); x++) {
                for (int y = b.minY(); y <= b.maxY(); y++) {
                    for (int z = b.minZ(); z <= b.maxZ(); z++) {
                        boolean surface = x == b.minX() || x == b.maxX() || y == b.minY() || y == b.maxY() || z == b.minZ() || z == b.maxZ();
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState s = level.getBlockState(pos);
                        if (!surface) {
                            interiorCells++;
                            if (isNaturalTerrain(s) && !insideDecoration(pieces, pos)) {
                                intrusion++;
                                if (intrusionAt.size() < 6) {
                                    StringBuilder ctx = new StringBuilder(x + "," + y + "," + z + " [");
                                    for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                                        ctx.append(d.getName()).append('=').append(
                                            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.relative(d)).getBlock()).getPath()).append(' ');
                                    }
                                    intrusionAt.add(ctx.append(']').toString());
                                }
                                intrusionBlocks.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1, Integer::sum);
                            }
                            if (!s.getFluidState().isEmpty()) {
                                fluid++;
                            }
                            if (isDecorationFeature(s)) {
                                lichen++; // vanilla biome decoration (vines, lichen, spore blossom...) growing inside the room
                            } else if (!s.isAir() && s.getFluidState().isEmpty() && !insideDecoration(pieces, pos)) {
                                foreign++;
                                foreignIds.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath(), 1, Integer::sum);
                                if (foreignAt.size() < 6) {
                                    foreignAt.add(x + "," + y + "," + z + " "
                                        + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath());
                                }
                            }
                            continue;
                        }
                        boolean passable = s.getCollisionShape(level, pos).isEmpty();
                        for (BlockPos out : outwardNeighbours(b, x, y, z)) {
                            if (insideAny(core, out, p)) {
                                continue; // junction with another core piece (doorway)
                            }
                            BlockState o = level.getBlockState(out);
                            if (passable) {
                                holes++; // shell cell is open and leads outside the structure
                            } else if (o.getCollisionShape(level, out).isEmpty()) {
                                if (o.getFluidState().is(FluidTags.LAVA)) {
                                    exposedLava++;
                                } else if (o.getFluidState().is(FluidTags.WATER)) {
                                    exposedWater++;
                                } else if (o.isAir()) {
                                    exposedAir++;
                                } else {
                                    exposedOther++;
                                }
                            }
                        }
                    }
                }
            }
            long reached = reachedPerPiece.getOrDefault(p.kind(), 0L);
            ps.put("interior_cells", interiorCells);
            ps.put("interior_foreign_blocks", foreign);
            ps.put("interior_decoration_features", lichen);
            if (foreign > 0) {
                ps.put("interior_foreign_by_id", foreignIds);
                ps.put("interior_foreign_at", foreignAt);
            }
            ps.put("interior_natural_solid", intrusion);
            if (p.kind().equals("combat_room")) {
                int spawners = 0;
                int vaults = 0;
                for (BlockPos q : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                    BlockState cs = level.getBlockState(q);
                    if (cs.is(net.minecraft.world.level.block.Blocks.TRIAL_SPAWNER)) {
                        spawners++;
                    } else if (cs.is(net.minecraft.world.level.block.Blocks.VAULT)) {
                        vaults++;
                    }
                }
                ps.put("trial_spawner_blocks", spawners);
                ps.put("vault_blocks", vaults);
            }
            if (!intrusionBlocks.isEmpty()) {
                ps.put("interior_natural_solid_blocks", intrusionBlocks);
                ps.put("interior_natural_solid_at", intrusionAt);
            }
            ps.put("interior_fluid", fluid);
            ps.put("shell_holes", holes);
            ps.put("interior_reached", reached);
            boolean isConnected = p.kind().equals("central_chamber") ? reached > 0 : reached > 0;
            ps.put("connected", isConnected);
            if (!isConnected) {
                disconnected++;
            }
            intrusionTotal += intrusion;
            foreignTotal += foreign;
            fluidInteriorTotal += fluid;
            shellHolesTotal += holes;
            pieceStats.put(p.kind(), ps);
        }
        m.put("pieces", pieceStats);
        m.put("disconnected_core_pieces", disconnected + (CORE.length - coreKinds));
        m.put("interior_natural_solid_total", intrusionTotal);
        m.put("interior_foreign_total", foreignTotal);
        m.put("interior_fluid_total", fluidInteriorTotal);
        m.put("shell_holes_total", shellHolesTotal);
        m.put("shell_exposed_faces_air", exposedAir);
        m.put("shell_exposed_faces_water", exposedWater);
        m.put("shell_exposed_faces_lava", exposedLava);
        m.put("shell_exposed_faces_other_nonsolid", exposedOther);

        m.put("context", context(level, probe, union, start));
        return m;
    }

    /** Natural overworld terrain a carver/density pass could leave inside a room (not structure blocks). */
    private static boolean isNaturalTerrain(BlockState s) {
        return s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
            || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(net.minecraft.world.level.block.Blocks.GRAVEL);
    }

    /** Biome decoration features (vegetal / underground decoration steps) that legitimately grow into rooms. */
    private static boolean isDecorationFeature(BlockState s) {
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        for (String k : new String[] {"vine", "lichen", "spore_blossom", "moss", "azalea", "hanging_roots", "dripleaf", "dripstone", "sculk", "amethyst", "roots", "fern", "grass", "flower"}) {
            if (path.contains(k)) {
                return true;
            }
        }
        return false;
    }

    /** Vanilla spawner/vault sub-pieces legitimately contain sand, copper, etc. */
    private static boolean insideDecoration(List<Piece> pieces, BlockPos pos) {
        for (Piece q : pieces) {
            if (q.kind().equals("other") && q.box().isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    private static String slice(ServerLevel level, Structure probe, StructureStart start) {
        List<Piece> pieces = new ArrayList<>();
        for (StructurePiece sp : start.getPieces()) {
            pieces.add(new Piece("p", sp.getBoundingBox()));
        }
        BoundingBox u = pieces.get(0).box();
        for (Piece p : pieces) {
            u = BoundingBox.encapsulating(u, p.box());
        }
        BoundingBox view = u.inflatedBy(6);
        StringBuilder sb = new StringBuilder();
        for (int dy : new int[] {2, 7}) {
            int y = u.minY() + dy;
            sb.append("-- horizontal slice y=").append(y).append("  (x ").append(view.minX()).append("..").append(view.maxX())
                .append(" left->right, z ").append(view.minZ()).append("..").append(view.maxZ()).append(" top->bottom)\n");
            for (int z = view.minZ(); z <= view.maxZ(); z++) {
                for (int x = view.minX(); x <= view.maxX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(pos);
                    boolean passable = st.getCollisionShape(level, pos).isEmpty();
                    Piece owner = null;
                    for (Piece p : pieces) {
                        if (p.box().isInside(pos)) {
                            owner = p;
                            break;
                        }
                    }
                    char c;
                    if (owner != null) {
                        BoundingBox b = owner.box();
                        boolean surface = x == b.minX() || x == b.maxX() || y == b.minY() || y == b.maxY() || z == b.minZ() || z == b.maxZ();
                        c = surface ? (passable ? '+' : '#') : (passable ? '.' : 'o');
                    } else if (st.getFluidState().is(FluidTags.LAVA)) {
                        c = 'L';
                    } else if (st.getFluidState().is(FluidTags.WATER)) {
                        c = '~';
                    } else {
                        c = passable ? ',' : ' ';
                    }
                    sb.append(c);
                }
                sb.append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static boolean insideAny(List<Piece> core, BlockPos pos, Piece self) {
        for (Piece q : core) {
            if (q != self && q.box().isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    private static List<BlockPos> outwardNeighbours(BoundingBox b, int x, int y, int z) {
        List<BlockPos> out = new ArrayList<>(3);
        if (x == b.minX()) out.add(new BlockPos(x - 1, y, z));
        if (x == b.maxX()) out.add(new BlockPos(x + 1, y, z));
        if (y == b.minY()) out.add(new BlockPos(x, y - 1, z));
        if (y == b.maxY()) out.add(new BlockPos(x, y + 1, z));
        if (z == b.minZ()) out.add(new BlockPos(x, y, z - 1));
        if (z == b.maxZ()) out.add(new BlockPos(x, y, z + 1));
        return out;
    }

    private static BlockPos findAirSeed(ServerLevel level, BoundingBox b) {
        BlockPos c = b.getCenter();
        for (int dy = 0; dy < b.getYSpan() - 2; dy++) {
            BlockPos p = new BlockPos(c.getX(), b.minY() + 1 + dy, c.getZ());
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return p;
            }
        }
        return null;
    }

    /** BFS over collision-free cells inside the structure box plus a 1-cell halo; halo hits = leaks outside. */
    private static long[] flood(ServerLevel level, BlockPos seed, List<Piece> core, BoundingBox union, Map<String, Long> reachedPerPiece) {
        BoundingBox limit = union.inflatedBy(1);
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(seed);
        seen.add(seed.asLong());
        long reached = 0;
        long leak = 0;
        BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            reached++;
            Piece owner = null;
            for (Piece c : core) {
                BoundingBox b = c.box();
                if (b.isInside(p) && p.getX() > b.minX() && p.getX() < b.maxX() && p.getY() > b.minY() && p.getY() < b.maxY()
                    && p.getZ() > b.minZ() && p.getZ() < b.maxZ()) {
                    owner = c;
                    break;
                }
            }
            if (owner != null) {
                reachedPerPiece.merge(owner.kind(), 1L, Long::sum);
            }
            boolean inCore = false;
            for (Piece c : core) {
                if (c.box().isInside(p)) {
                    inCore = true;
                    break;
                }
            }
            if (!inCore) {
                leak++;
                continue; // do not expand beyond the halo cell
            }
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                n.setWithOffset(p, d);
                if (!limit.isInside(n)) {
                    continue;
                }
                long key = n.asLong();
                if (seen.contains(key)) {
                    continue;
                }
                if (level.getBlockState(n).getCollisionShape(level, n).isEmpty()) {
                    seen.add(key);
                    q.add(n.immutable());
                }
            }
        }
        return new long[] {reached, leak};
    }

    private static Map<String, Object> context(ServerLevel level, Structure probe, BoundingBox union, StructureStart self) {
        Map<String, Object> c = new LinkedHashMap<>();
        BoundingBox ring = union.inflatedBy(RING);
        long total = 0;
        long air = 0;
        long water = 0;
        long lava = 0;
        long solid = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = ring.minX(); x <= ring.maxX(); x += RING_STRIDE) {
            for (int y = Math.max(ring.minY(), level.getMinY()); y <= Math.min(ring.maxY(), level.getMaxY()); y += RING_STRIDE) {
                for (int z = ring.minZ(); z <= ring.maxZ(); z += RING_STRIDE) {
                    if (union.isInside(x, y, z)) {
                        continue;
                    }
                    p.set(x, y, z);
                    BlockState s = level.getBlockState(p);
                    total++;
                    if (s.getFluidState().is(FluidTags.LAVA)) {
                        lava++;
                    } else if (s.getFluidState().is(FluidTags.WATER)) {
                        water++;
                    } else if (s.getCollisionShape(level, p).isEmpty()) {
                        air++;
                    } else {
                        solid++;
                    }
                }
            }
        }
        BlockPos center = union.getCenter();
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, center.getX(), center.getZ());
        c.put("ring_samples", total);
        c.put("ring_air", air);
        c.put("ring_water", water);
        c.put("ring_lava", lava);
        c.put("ring_solid", solid);
        c.put("surface_y", surface);
        c.put("box_max_y", union.maxY());
        c.put("depth_below_surface", surface - union.maxY());
        c.put("mountain_biome", level.getBiome(center).is(BiomeTags.IS_MOUNTAIN));

        // Tell-tale blocks of vanilla features that share the underground_structures step with jigsaw structures
        // (geodes: smooth_basalt/budding_amethyst/calcite, fossils: bone_block, monster rooms: spawner/mossy_cobblestone).
        Map<String, Integer> markers = new LinkedHashMap<>();
        BoundingBox mb = union.inflatedBy(2);
        for (int x = mb.minX(); x <= mb.maxX(); x++) {
            for (int y = Math.max(mb.minY(), level.getMinY()); y <= Math.min(mb.maxY(), level.getMaxY()); y++) {
                for (int z = mb.minZ(); z <= mb.maxZ(); z++) {
                    BlockState ms = level.getBlockState(new BlockPos(x, y, z));
                    if (ms.is(net.minecraft.world.level.block.Blocks.SMOOTH_BASALT) || ms.is(net.minecraft.world.level.block.Blocks.BUDDING_AMETHYST)
                        || ms.is(net.minecraft.world.level.block.Blocks.CALCITE) || ms.is(net.minecraft.world.level.block.Blocks.BONE_BLOCK)
                        || ms.is(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE) || ms.is(net.minecraft.world.level.block.Blocks.SPAWNER)) {
                        markers.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(ms.getBlock()).getPath(), 1, Integer::sum);
                    }
                }
            }
        }
        c.put("feature_markers_near_footprint", markers);

        List<String> overlaps = new ArrayList<>();
        List<String> nearby = new ArrayList<>();
        BoundingBox scan = union.inflatedBy(16);
        java.util.HashSet<String> checked = new java.util.HashSet<>();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (ChunkPos cp : (Iterable<ChunkPos>) scan.intersectingChunks()::iterator) {
            ChunkAccess ca = level.getChunk(cp.x(), cp.z(), ChunkStatus.FULL, true);
            for (Map.Entry<Structure, LongSet> e : ca.getAllReferences().entrySet()) {
                for (long packed : e.getValue()) {
                    ChunkPos sc = ChunkPos.unpack(packed);
                    if (!checked.add(structures.getKey(e.getKey()) + "@" + packed)) {
                        continue;
                    }
                    StructureStart ss = level.getChunk(sc.x(), sc.z(), ChunkStatus.FULL, true).getStartForStructure(e.getKey());
                    if (ss == null || !ss.isValid() || ss == self) {
                        continue; // self is the sample under test; other probe instances DO count as overlaps
                    }
                    String id = structures.getKey(e.getKey()) == null ? "?" : structures.getKey(e.getKey()).toString();
                    boolean overlap = false;
                    boolean near = false;
                    for (StructurePiece sp : ss.getPieces()) {
                        if (sp.getBoundingBox().intersects(union)) {
                            overlap = true;
                        } else if (sp.getBoundingBox().intersects(scan)) {
                            near = true;
                        }
                    }
                    if (overlap) {
                        overlaps.add(id);
                    } else if (near) {
                        nearby.add(id);
                    }
                }
            }
        }
        c.put("overlapping_structures", overlaps.stream().distinct().sorted().toList());
        c.put("nearby_structures_16", nearby.stream().distinct().sorted().toList());
        return c;
    }
}
