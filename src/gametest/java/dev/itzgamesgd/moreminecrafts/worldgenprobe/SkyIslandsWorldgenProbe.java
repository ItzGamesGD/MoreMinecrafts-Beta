package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sky Islands natural-generation probe (not production; inert unless {@code -Dmm.sky.out} is set). Scans the real Overworld
 * generator for structure starts, classifies the assembled jigsaw pieces, then forces the chunks of one sample of every
 * building variant to FULL and counts the physically placed trial spawners, vaults and chests with their block entity data.
 */
public final class SkyIslandsWorldgenProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("sky-islands-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> VARIANTS = List.of("ruins_a", "ruins_b", "ruins_c", "ruins_d", "tower_5", "tower_6", "tower_7", "temple");
    /** Expected container counts per building (spawners, vaults, chests), taken from the generator manifest. */
    private static final Map<String, int[]> EXPECTED = Map.of(
            "ruins_a", new int[]{1, 1, 2}, "ruins_b", new int[]{1, 1, 2}, "ruins_c", new int[]{1, 1, 2}, "ruins_d", new int[]{1, 1, 2},
            "tower_5", new int[]{7, 7, 12}, "tower_6", new int[]{8, 8, 14}, "tower_7", new int[]{9, 9, 16}, "temple", new int[]{2, 2, 4});

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.sky.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                run(server, Path.of(out));
            } catch (Throwable t) {
                LOG.error("sky islands probe failed", t);
                try {
                    Files.createDirectories(Path.of(out).getParent());
                    Files.writeString(Path.of(out), GSON.toJson(Map.of("error", String.valueOf(t))), StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // the missing/invalid output file is the failure signal for the driver
                }
            }
            Runtime.getRuntime().halt(0);
        });
    }

    private record Sample(String structure, String variant, BoundingBox buildingBox, int chunkX, int chunkZ, List<String> pieces) {}

    private static String variantOf(String element) {
        for (String variant : VARIANTS) {
            if (element.contains("moreminecrafts:sky_islands/" + variant + "]") || element.contains("moreminecrafts:sky_islands/" + variant + ",")) return variant;
        }
        return null;
    }

    private static void run(MinecraftServer server, Path out) throws IOException {
        int maxRadius = Integer.getInteger("mm.sky.radius", 160);
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        Map<String, Integer> startsByStructure = new TreeMap<>();
        Map<String, Integer> variantCount = new TreeMap<>();
        Map<String, Integer> islandStyleCount = new TreeMap<>();
        Map<String, Integer> islandKindCount = new TreeMap<>();
        Map<String, Sample> samples = new LinkedHashMap<>();
        int totalStarts = 0, scanned = 0, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        scan:
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int cx = -radius; cx <= radius; cx++) {
                for (int cz = -radius; cz <= radius; cz++) {
                    if (Math.abs(cx) != radius && Math.abs(cz) != radius) continue;
                    ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS);
                    scanned++;
                    for (var entry : chunk.getAllStarts().entrySet()) {
                        var id = structureRegistry.getKey(entry.getKey());
                        if (id == null || !id.getNamespace().equals("moreminecrafts") || !id.getPath().startsWith("sky_island_")) continue;
                        StructureStart start = entry.getValue();
                        if (!start.isValid()) continue;
                        totalStarts++;
                        startsByStructure.merge(id.getPath(), 1, Integer::sum);
                        BoundingBox all = start.getBoundingBox();
                        minY = Math.min(minY, all.minY());
                        maxY = Math.max(maxY, all.maxY());
                        List<String> pieces = new ArrayList<>();
                        for (var piece : start.getPieces()) {
                            if (!(piece instanceof PoolElementStructurePiece pooled)) continue;
                            String element = pooled.getElement().toString();
                            pieces.add(element);
                            String variant = variantOf(element);
                            if (variant != null) {
                                variantCount.merge(variant, 1, Integer::sum);
                                samples.putIfAbsent(variant, new Sample(id.getPath(), variant, pooled.getBoundingBox(), cx, cz, pieces));
                            } else if (element.contains("moreminecrafts:sky_islands/")) {
                                String tail = element.substring(element.indexOf("moreminecrafts:sky_islands/") + "moreminecrafts:sky_islands/".length());
                                String[] parts = tail.replace("]", "").replace(",", "").trim().split("/");
                                if (parts.length == 2) {
                                    islandStyleCount.merge(parts[0], 1, Integer::sum);
                                    islandKindCount.merge(parts[1].replaceAll("[^a-z_]+$", ""), 1, Integer::sum);
                                }
                            }
                        }
                    }
                    if (samples.size() == VARIANTS.size() && scanned > 4000) break scan;
                }
            }
        }
        LOG.info("SKY_SCAN scanned={} starts={} variants={}", scanned, totalStarts, variantCount);

        Map<String, Object> physical = new LinkedHashMap<>();
        boolean allPhysicalOk = true;
        for (Sample sample : samples.values()) {
            BoundingBox box = sample.buildingBox();
            for (int cx = (box.minX() - 16) >> 4; cx <= (box.maxX() + 16) >> 4; cx++)
                for (int cz = (box.minZ() - 16) >> 4; cz <= (box.maxZ() + 16) >> 4; cz++) level.getChunk(cx, cz, ChunkStatus.FULL, true);
            int spawners = 0, vaults = 0, chests = 0, chestLoot = 0, vaultLoot = 0, spawnerConfigs = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                var state = level.getBlockState(pos.set(x, y, z));
                if (state.is(Blocks.TRIAL_SPAWNER)) {
                    spawners++;
                    var be = level.getBlockEntity(pos);
                    if (be != null) {
                        String data = be.saveWithFullMetadata(level.registryAccess()).toString();
                        if (data.contains("trial_chamber/") || data.contains("moreminecrafts:sky_islands/shulker")) spawnerConfigs++;
                    }
                } else if (state.is(Blocks.VAULT)) {
                    vaults++;
                    var be = level.getBlockEntity(pos);
                    if (be != null && be.saveWithFullMetadata(level.registryAccess()).toString().contains("moreminecrafts:chests/sky_islands/vault")) vaultLoot++;
                } else if (state.is(Blocks.CHEST)) {
                    chests++;
                    var be = level.getBlockEntity(pos);
                    if (be != null && be.saveWithFullMetadata(level.registryAccess()).toString().contains("moreminecrafts:chests/sky_islands/chest")) chestLoot++;
                }
            }
            BlockPos support = new BlockPos((box.minX() + box.maxX()) / 2, box.minY() - 1, (box.minZ() + box.maxZ()) / 2);
            boolean onIsland = !level.getBlockState(support).isAir() && !level.getBlockState(support).is(Blocks.WATER);
            int[] expected = EXPECTED.get(sample.variant());
            boolean ok = spawners == expected[0] && vaults == expected[1] && chests == expected[2]
                    && vaultLoot == vaults && chestLoot == chests && spawnerConfigs == spawners && onIsland;
            allPhysicalOk &= ok;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("structure", sample.structure());
            row.put("chunk", sample.chunkX() + "," + sample.chunkZ());
            row.put("bounds", box.toString());
            row.put("trial_spawners", spawners);
            row.put("vaults", vaults);
            row.put("chests", chests);
            row.put("vault_loot_table_ok", vaultLoot);
            row.put("chest_loot_table_ok", chestLoot);
            row.put("spawner_config_ok", spawnerConfigs);
            row.put("island_support_block", level.getBlockState(support).toString());
            row.put("expected", List.of(expected[0], expected[1], expected[2]));
            row.put("pass", ok);
            row.put("pieces", sample.pieces());
            physical.put(sample.variant(), row);
            LOG.info("SKY_PHYSICAL {} {}", sample.variant(), row);
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("seed", level.getSeed());
        doc.put("chunks_scanned", scanned);
        doc.put("max_radius_chunks", maxRadius);
        doc.put("structure_starts", totalStarts);
        doc.put("starts_by_structure", startsByStructure);
        doc.put("building_variants_seen", variantCount);
        doc.put("island_pieces_by_style", islandStyleCount);
        doc.put("island_pieces_by_kind", islandKindCount);
        doc.put("start_y_min", minY == Integer.MAX_VALUE ? null : minY);
        doc.put("start_y_max", maxY == Integer.MIN_VALUE ? null : maxY);
        doc.put("physical", physical);
        doc.put("all_physical_checks_pass", allPhysicalOk && samples.size() == VARIANTS.size());
        doc.put("missing_variants", VARIANTS.stream().filter(v -> !samples.containsKey(v)).toList());
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(doc), StandardCharsets.UTF_8);
        LOG.info("SKY_ISLANDS_PROBE_DONE starts={} pass={}", totalStarts, doc.get("all_physical_checks_pass"));
    }
}
