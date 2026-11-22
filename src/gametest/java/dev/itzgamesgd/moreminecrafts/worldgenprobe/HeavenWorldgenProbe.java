package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyIslandHabitat;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkySpawnRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;

/**
 * P3.9 real-worldgen probe (dedicated server, opt-in: {@code -Dmm.heaven.out=<json>}; never part of check / build / CI). One server start =
 * one stage of a scenario driven by tools/heaven-p39/run_worldgen.sh. Real chunk generation of naturally located sky-island starts:
 * <ul>
 *   <li>{@code mm.heaven.targets}: sky_island_meadow, sky_island_forest, sky_island_mountain_meadow (comma list of the suffixes);</li>
 *   <li>{@code mm.heaven.mode}: all | west | east | none - which chunks of the start's box are generated to FULL (west = chunk x <= the box's middle);</li>
 *   <li>{@code mm.heaven.sitefile}: located sites are written there on first use and reused by later stages of the same seed;</li>
 *   <li>{@code mm.heaven.spawn}: natural-spawner attempts per island chunk at midnight (0 = skip);</li>
 *   <li>{@code mm.heaven.compare}: a previous output; block fingerprints of complete islands are compared with it.</li>
 * </ul>
 * Reports piece template locations, vanilla / heaven block counts, a block fingerprint, Habitat support on standable cells and natural spawns.
 */
public final class HeavenWorldgenProbe implements ModInitializer {
    private static final List<String> FORBIDDEN = List.of("grass_block", "dirt", "coarse_dirt", "podzol", "short_grass", "tall_grass", "oak_leaves", "birch_leaves", "fern");

    @Override
    public void onInitialize() {
        if (System.getProperty("mm.heaven.out") == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Object> doc = new LinkedHashMap<>();
            try { run(server, doc); } catch (Throwable t) { doc.put("error", t.toString()); t.printStackTrace(); }
            try {
                Path out = Path.of(System.getProperty("mm.heaven.out")); Files.createDirectories(out.toAbsolutePath().getParent());
                Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(doc));
                server.saveEverything(false, true, true);
            } catch (Exception e) { e.printStackTrace(); }
            server.halt(false);
        });
    }

    private static void run(MinecraftServer server, Map<String, Object> doc) throws Exception {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        String mode = System.getProperty("mm.heaven.mode", "all");
        int spawn = Integer.getInteger("mm.heaven.spawn", 0), radius = Integer.getInteger("mm.heaven.radius", 120);
        doc.put("label", System.getProperty("mm.heaven.label", "")); doc.put("seed", level.getSeed()); doc.put("mode", mode);
        Path siteFile = Path.of(System.getProperty("mm.heaven.sitefile", "sites.json"));
        JsonObject sites = Files.exists(siteFile) ? JsonParser.parseString(Files.readString(siteFile)).getAsJsonObject() : new JsonObject();
        JsonObject compare = System.getProperty("mm.heaven.compare") == null ? null : JsonParser.parseString(Files.readString(Path.of(System.getProperty("mm.heaven.compare")))).getAsJsonObject().getAsJsonObject("islands");
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var gen = level.getChunkSource().getGenerator();
        Map<String, Object> islands = new LinkedHashMap<>(); doc.put("islands", islands);
        int siteCount = Integer.getInteger("mm.heaven.sites", 1);
        List<String[]> work = new ArrayList<>();
        for (String t : System.getProperty("mm.heaven.targets", "meadow,forest").split(",")) for (int k = 0; k < siteCount; k++) work.add(new String[]{t, String.valueOf(k)});
        for (String[] w : work) {
            String base = w[0]; int k = Integer.parseInt(w[1]); String target = k == 0 ? base : base + "#" + k;
            Identifier sid = Identifier.parse("moreminecrafts:sky_island_" + base);
            long t0 = System.currentTimeMillis();
            if (!sites.has(target)) {
                var holder = registry.get(sid).orElseThrow();
                double ang = k * 2.399963, rr = 900.0 * Math.sqrt(k);   // golden-angle spiral of search origins -> distinct nearby starts
                var found = gen.findNearestMapStructure(level, HolderSet.direct(holder), new BlockPos((int) (Math.cos(ang) * rr), 0, (int) (Math.sin(ang) * rr)), radius, false);
                if (found == null) { islands.put(target, "no start within " + radius + " chunks"); continue; }
                String key = found.getFirst().getX() + "," + found.getFirst().getZ(); boolean dup = false;
                for (var e : sites.entrySet()) if (e.getKey().equals(base) || e.getKey().startsWith(base + "#")) dup |= e.getValue().getAsString().equals(key);
                if (dup) { islands.put(target, "duplicate of an earlier site"); continue; }
                sites.addProperty(target, key);
            }
            String[] xz = sites.get(target).getAsString().split(","); ChunkPos sc = new ChunkPos(Integer.parseInt(xz[0]) >> 4, Integer.parseInt(xz[1]) >> 4);
            Map<String, Object> row = new LinkedHashMap<>(); islands.put(target, row);
            row.put("start_chunk", sc.x() + "," + sc.z()); row.put("locate_ms", System.currentTimeMillis() - t0);
            // the start chunk first (generates the start), then the box of the start
            level.getChunk(sc.x(), sc.z(), ChunkStatus.FULL, true);
            StructureStart start = null;
            for (var s : level.structureManager().startsForStructure(sc.x(), sc.z(), st -> registry.getKey(st) != null && registry.getKey(st).equals(sid))) start = s;
            if (start == null) { row.put("error", "no start in the start chunk"); continue; }
            BoundingBox box = start.getBoundingBox(); int mid = (box.minX() >> 4) + ((box.maxX() >> 4) - (box.minX() >> 4)) / 2;
            List<ChunkPos> generated = new ArrayList<>(); long g0 = System.currentTimeMillis();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                boolean want = switch (mode) { case "all" -> true; case "west" -> cx <= mid; case "east" -> cx > mid; default -> false; };
                if (want) { level.getChunk(cx, cz, ChunkStatus.FULL, true); generated.add(new ChunkPos(cx, cz)); }
            }
            row.put("generate_ms", System.currentTimeMillis() - g0); row.put("generated_chunks", generated.size());
            row.put("box", box.getXSpan() + "x" + box.getYSpan() + "x" + box.getZSpan()); row.put("mid_chunk_x", mid);
            List<String> templates = new ArrayList<>(); boolean vanillaPiece = false, heavenPiece = false;
            for (var piece : start.getPieces()) if (piece instanceof PoolElementStructurePiece p && p.getElement() instanceof SinglePoolElement single) {
                String loc = String.valueOf(single.getTemplateLocation()); templates.add(loc.replace("moreminecrafts:", ""));
                if (loc.matches("moreminecrafts:sky_islands/(meadow|forest|snowy|desert|badlands|rocky)/.*")) vanillaPiece = true;
                if (loc.contains("sky_islands/heaven/")) heavenPiece = true;
            }
            row.put("templates", templates); row.put("any_vanilla_theme_template", vanillaPiece); row.put("any_heaven_template", heavenPiece);
            boolean complete = !mode.equals("west") && !mode.equals("none");
            if (complete || mode.equals("none")) {
                Map<String, Integer> counts = new TreeMap<>(); long h = 1125899906842597L;
                // only cells inside this start's own piece boxes (the start box can also contain natural terrain), each cell once
                java.util.Set<Long> seen = new java.util.HashSet<>(); java.util.List<Long> order = new ArrayList<>();
                for (var piece : start.getPieces()) { BoundingBox pb = piece.getBoundingBox();
                    for (BlockPos p : BlockPos.betweenClosed(pb.minX(), pb.minY(), pb.minZ(), pb.maxX(), pb.maxY(), pb.maxZ())) if (seen.add(p.asLong())) order.add(p.asLong()); }
                java.util.Collections.sort(order);
                for (long pl : order) {
                    BlockPos p = BlockPos.of(pl); var s = level.getBlockState(p); if (s.isAir()) continue;
                    counts.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath(), 1, Integer::sum); h = 31 * h + p.asLong() * 17L + s.toString().hashCode();
                }
                if (Boolean.getBoolean("mm.heaven.trace")) row.put("vanilla_trace", traceVanilla(level, start, order, gen));
                int vanilla = 0; for (String f : FORBIDDEN) vanilla += counts.getOrDefault(f, 0);
                int heaven = 0; for (var e : counts.entrySet()) if (e.getKey().startsWith("heaven_")) heaven += e.getValue();
                row.put("fingerprint", Long.toHexString(h)); row.put("blocks", counts.values().stream().mapToInt(Integer::intValue).sum());
                row.put("vanilla_theme_blocks", vanilla); Map<String, Integer> vd = new TreeMap<>(); for (String f : FORBIDDEN) if (counts.containsKey(f)) vd.put(f, counts.get(f)); row.put("vanilla_theme_detail", vd); row.put("heaven_blocks", heaven); row.put("jigsaw_left", counts.getOrDefault("jigsaw", 0));
                row.put("vault", counts.getOrDefault("vault", 0)); row.put("trial_spawner", counts.getOrDefault("trial_spawner", 0));
                if (compare != null && compare.has(target)) {
                    var c = compare.getAsJsonObject(target);
                    row.put("fingerprint_equal_to_" + System.getProperty("mm.heaven.compareName", "compare"), c.has("fingerprint") && c.get("fingerprint").getAsString().equals(Long.toHexString(h)));
                }
                // Habitat on standable cells (below solid, feet and head free)
                Map<String, Integer> be = new TreeMap<>(); int lootContainers = 0;
                for (ChunkPos cp : generated) for (var e : level.getChunk(cp.x(), cp.z()).getBlockEntities().entrySet()) {
                    BlockPos bp = e.getKey(); if (!box.isInside(bp)) continue;
                    String id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(e.getValue().getType()).getPath(); be.merge(id, 1, Integer::sum);
                    if (e.getValue() instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity rc && rc.getLootTable() != null) lootContainers++;
                }
                row.put("block_entities", be); row.put("loot_table_containers", lootContainers);
                int standable = 0, support = 0, regionOk = 0; long habFirstNs = -1, habMaxNs = 0;
                for (int x = box.minX(); x <= box.maxX(); x += 2) for (int z = box.minZ(); z <= box.maxZ(); z += 2) {
                    for (int y = box.maxY(); y >= box.minY() + 1; y--) {
                        BlockPos feet = new BlockPos(x, y, z);
                        var below = level.getBlockState(feet.below());
                        if (!below.isSolid() || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() || !level.getFluidState(feet).isEmpty()
                                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
                        standable++;
                        long hn = System.nanoTime(); boolean hab = SkyIslandHabitat.find(level, feet).isPresent(); hn = System.nanoTime() - hn;
                        if (habFirstNs < 0) habFirstNs = hn; habMaxNs = Math.max(habMaxNs, hn);
                        if (hab) { support++; if (SkySpawnRules.allows(level, feet, EntitySpawnReason.NATURAL)) regionOk++; }
                        break;
                    }
                }
                row.put("standable_columns", standable); row.put("habitat_support", support); row.put("habitat_first_call_ms", habFirstNs / 1e6); row.put("habitat_max_call_ms", habMaxNs / 1e6); row.put("natural_spawn_gate_open", regionOk);
            }
            if (spawn > 0 && complete) {
                // No player exists on this dedicated probe, so NaturalSpawner's player-distance gate can never pass; the per-position natural
                // spawn rule (SpawnPlacements.checkSpawnRules with NATURAL reason, the check the spawner applies to every candidate) is run directly.
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set midnight");
                var types = List.of(net.minecraft.world.entity.EntityTypes.ZOMBIE, net.minecraft.world.entity.EntityTypes.SKELETON, net.minecraft.world.entity.EntityTypes.CREEPER, net.minecraft.world.entity.EntityTypes.SPIDER);
                Map<String, Integer> pass = new TreeMap<>(); int cells = 0;
                for (int x = box.minX(); x <= box.maxX(); x += 2) for (int z = box.minZ(); z <= box.maxZ(); z += 2)
                    for (int y = box.maxY(); y >= box.minY() + 1; y--) {
                        BlockPos feet = new BlockPos(x, y, z);
                        if (!level.getBlockState(feet.below()).isSolid() || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
                        cells++;
                        for (var t : types) if (net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(t, level, EntitySpawnReason.NATURAL, feet, level.getRandom()))
                            pass.merge(BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath(), 1, Integer::sum);
                        break;
                    }
                row.put("spawn_cells", cells); row.put("natural_spawn_rule_pass_by_type", pass);
            }
        }
        Files.writeString(siteFile, new GsonBuilder().create().toJson(sites));
    }

    /** P3.11: where do the remaining vanilla-theme blocks come from? Each is classified by (a) is it a block of one of the start's own templates (transformed
     *  with the piece rotation/position) and (b) is it at or below the natural (noise) terrain surface of its column. */
    private static Map<String, Object> traceVanilla(ServerLevel level, StructureStart start, List<Long> order, net.minecraft.world.level.chunk.ChunkGenerator gen) {
        java.util.Set<Long> templateCells = new java.util.HashSet<>(); List<String> pieces = new ArrayList<>();
        for (var piece : start.getPieces()) if (piece instanceof PoolElementStructurePiece p && p.getElement() instanceof SinglePoolElement single) {
            var tpl = level.getStructureTemplateManager().getOrCreate(single.getTemplateLocation()); var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(p.getRotation());
            var tag = tpl.save(new net.minecraft.nbt.CompoundTag()); var palette = tag.getListOrEmpty("palette"); BoundingBox pb = p.getBoundingBox();
            pieces.add(single.getTemplateLocation() + " box=" + pb.minX() + "," + pb.minY() + "," + pb.minZ() + ".." + pb.maxX() + "," + pb.maxY() + "," + pb.maxZ());
            for (var e : tag.getListOrEmpty("blocks")) if (e instanceof net.minecraft.nbt.CompoundTag b) {
                int st = b.getIntOr("state", -1); var pos = b.getListOrEmpty("pos"); if (st < 0 || pos.size() != 3) continue;
                if (palette.getCompoundOrEmpty(st).getStringOr("Name", "").equals("minecraft:air")) continue;
                templateCells.add(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings, new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0))).offset(p.getPosition()).asLong());
            }
        }
        var rs = level.getChunkSource().randomState(); Map<String, Integer> classes = new TreeMap<>(); Map<String, Integer> byBlock = new TreeMap<>(); List<String> samples = new ArrayList<>();
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE; Map<Long, Integer> surf = new java.util.HashMap<>();
        for (long pl : order) {
            BlockPos p = BlockPos.of(pl); String n = BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).getPath(); if (!FORBIDDEN.contains(n)) continue;
            int sh = surf.computeIfAbsent(((long) p.getX() << 32) ^ (p.getZ() & 0xffffffffL), k -> gen.getBaseHeight(p.getX(), p.getZ(), net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level, rs));
            String cls = (templateCells.contains(pl) ? "in_template" : "not_template") + "/" + (p.getY() < sh ? "below_natural_surface" : "at_or_above_natural_surface");
            classes.merge(cls, 1, Integer::sum); byBlock.merge(n, 1, Integer::sum); minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
            if (samples.size() < 6 && classes.get(cls) <= 2) samples.add(n + "@" + p.getX() + "," + p.getY() + "," + p.getZ() + " " + cls + " surface=" + sh);
        }
        Map<String, Object> out = new LinkedHashMap<>(); out.put("pieces", pieces); out.put("classes", classes); out.put("by_block", byBlock); out.put("y_range", minY + ".." + maxY); out.put("samples", samples);
        return out;
    }
}
