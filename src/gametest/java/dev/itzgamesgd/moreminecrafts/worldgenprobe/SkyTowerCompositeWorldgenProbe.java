package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerData;
import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerLayout;
import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerWorldgenRegistration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in natural-worldgen integration probe for C-3; inert unless mm.sky.tower.out is supplied. */
public final class SkyTowerCompositeWorldgenProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("sky-tower-composite-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> STYLES = List.of("snowy", "meadow", "forest", "badlands", "desert", "rocky");
    private record Sample(String style, String structure, SkyTowerLayout.Site site,
                          PoolElementStructurePiece piece, StructureStart start, int seenChunkX, int seenChunkZ) {}
    private record RestartSnapshot(int sitesBefore, Map<String, Integer> progressBefore,
                                   Map<String, Boolean> structureStartsFound) {}

    @Override
    public void onInitialize() {
        String output = System.getProperty("mm.sky.tower.out");
        if (output == null) return;
        boolean resume = Boolean.getBoolean("mm.sky.tower.resume");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int[] stage = {0};
            @SuppressWarnings("unchecked") Map<String, Sample>[] samples = new Map[]{null};
            RestartSnapshot[] restart = {null};
            int[] waited = {0};
            boolean[] forced = {false};
            @SuppressWarnings("unchecked") Map<String, Object>[] pending = new Map[]{null};
            ServerTickEvents.END_SERVER_TICK.register(ticking -> {
                if (ticking != server) return;
                try {
                    if (stage[0] == 0) {
                        if (resume) restart[0] = loadRegisteredStarts(server);
                        else samples[0] = discoverAndLoad(server);
                        stage[0] = 1;
                        return;
                    }
                    boolean floorClear = Boolean.getBoolean("mm.sky.tower.persistFloorClear");
                    if (floorClear && !forced[0]) { forceTowerChunks(server); forced[0] = true; waited[0] = 0; return; } // after auto-registration
                    if (resume && floorClear && stage[0] == 1 && waited[0]++ < 100) return; // let the barrier tick reconcile after load
                    if (!resume && floorClear && stage[0] >= 2) {
                        waited[0]++;
                        if (stage[0] == 2 && waited[0] >= 60) { // barriers installed by the real 20-tick barrier tick
                            Map<String, Object> before = floorState(server);
                            ServerLevel lv = server.getLevel(Level.OVERWORLD);
                            SkyTowerData d = server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
                            var site = d.sites().values().iterator().next();
                            for (int i = 0; i < 4; i++) { var a = site.spawner(i); lv.destroyBlock(new BlockPos(a.x(), a.y(), a.z()), false); }
                            pending[0].put("floor_clear_fixture_before_removal", before);
                            stage[0] = 3; waited[0] = 0; return;
                        }
                        if (stage[0] == 2) return;
                        if (waited[0] < 60) return;
                        pending[0].put("floor_clear_fixture_after_removal", floorState(server));
                        Path path = Path.of(output);
                        Files.createDirectories(path.getParent());
                        Files.writeString(path, GSON.toJson(pending[0]), StandardCharsets.UTF_8);
                        LOG.info("SKY_TOWER_COMPOSITE_PROBE_DONE pass={}", pending[0].get("pass"));
                        server.halt(false);
                        return;
                    }
                    Map<String, Object> report = resume ? verifyRestart(server, restart[0]) : verify(server, samples[0]);
                    if (resume && floorClear) report.put("floor_state_after_restart", floorState(server));
                    if (!resume && floorClear && stage[0] == 1) {
                        pending[0] = report; stage[0] = 2; waited[0] = 0; return;
                    }
                    if (!resume && !floorClear && Boolean.getBoolean("mm.sky.tower.persistTestProgress")) {
                        SkyTowerData data = server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
                        for (Sample sample : samples[0].values()) {
                            if (!data.sites().containsKey(sample.site().id())) continue;
                            boolean recorded = data.recordVanillaCompletion(sample.site().id(), sample.site().spawner(0));
                            report.put("restart_progress_fixture", Map.of("site", sample.site().id(),
                                    "synthetic_valid_anchor_completion", recorded,
                                    "completed_mask_at_save", data.completedMask(sample.site().id())));
                            break;
                        }
                    }
                    Path path = Path.of(output);
                    Files.createDirectories(path.getParent());
                    Files.writeString(path, GSON.toJson(report), StandardCharsets.UTF_8);
                    LOG.info("SKY_TOWER_COMPOSITE_PROBE_DONE pass={}", report.get("pass"));
                    server.halt(false);
                    return;
                } catch (Throwable failure) {
                    LOG.error("Sky Tower composite probe failed", failure);
                    try {
                        Path path = Path.of(output);
                        Files.createDirectories(path.getParent());
                        Files.writeString(path, GSON.toJson(Map.of("error", String.valueOf(failure))), StandardCharsets.UTF_8);
                    } catch (Exception ignored) {
                        // A missing output file is the failure signal.
                    }
                    server.halt(false);
                }
            });
        });
    }

    /** Fixture-only: keep the registered/sampled tower chunks loaded so the real 20-tick barrier tick can act. */
    private static void forceTowerChunks(MinecraftServer server) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        SkyTowerData data = server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        for (var site : data.sites().values()) {
            int[] b = site.bounds();
            for (int cx = (b[0] >> 4) - 2; cx <= (b[3] >> 4) + 2; cx++)
                for (int cz = (b[2] >> 4) - 2; cz <= (b[5] >> 4) + 2; cz++) {
                    level.getChunk(cx, cz, ChunkStatus.FULL, true);
                    level.setChunkForced(cx, cz, true);
                }
        }
    }

    /** Counts the floor-0/floor-1 wall cells of the first registered tower: barrier blocks vs air, plus saved masks. */
    private static Map<String, Object> floorState(MinecraftServer server) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        SkyTowerData data = server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        Map<String, Object> out = new LinkedHashMap<>();
        if (data.sites().isEmpty()) { out.put("error", "no registered site"); return out; }
        var site = data.sites().values().iterator().next();
        out.put("site", site.id());
        out.put("completed_mask", data.completedMask(site.id()));
        out.put("destroyed_mask", data.destroyedMask(site.id()));
        int noBlockChunk = 0, noEntities = 0, noChunkSource = 0;
        for (int cell = 0; cell < 18; cell++) {
            var a = site.barrier(0, cell);
            if (!level.hasChunkAt(new BlockPos(a.x(), a.y(), a.z()))) noBlockChunk++;
            for (int x = Math.floorDiv(a.x() - 2, 16); x <= Math.floorDiv(a.x() + 3, 16); x++)
                for (int z = Math.floorDiv(a.z() - 2, 16); z <= Math.floorDiv(a.z() + 3, 16); z++) {
                    if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(x, z))) noEntities++;
                    if (!level.getChunkSource().hasChunk(x, z)) noChunkSource++;
                }
        }
        out.put("diag_floor0_cells_without_block_chunk", noBlockChunk);
        out.put("diag_floor0_entity_chunk_checks_not_loaded", noEntities);
        out.put("diag_floor0_chunk_source_checks_missing", noChunkSource);
        out.put("diag_server_tick_count", server.getTickCount());
        for (int floor = 0; floor < 2; floor++) {
            int barrier = 0, air = 0, owned = 0;
            for (int cell = 0; cell < 18; cell++) {
                var a = site.barrier(floor, cell);
                var state = level.getBlockState(new BlockPos(a.x(), a.y(), a.z()));
                if (state.is(Blocks.BARRIER)) barrier++; else if (state.isAir()) air++;
                if (data.ownsBarrier(site.id(), floor, cell)) owned++;
            }
            out.put("floor" + floor + "_barrier_cells", barrier);
            out.put("floor" + floor + "_air_cells", air);
            out.put("floor" + floor + "_owned_cells", owned);
        }
        return out;
    }

    private static Map<String, Sample> discoverAndLoad(MinecraftServer server) {
        int maxRadius = Integer.getInteger("mm.sky.tower.radius", 96);
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        Map<String, Sample> samples = new LinkedHashMap<>();
        int scanned = 0;
        outer:
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int cx = -radius; cx <= radius; cx++) for (int cz = -radius; cz <= radius; cz++) {
                if (Math.abs(cx) != radius && Math.abs(cz) != radius) continue;
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS);
                scanned++;
                for (var entry : chunk.getAllStarts().entrySet()) {
                    var structureId = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(entry.getKey());
                    StructureStart start = entry.getValue();
                    if (structureId == null || !start.isValid()) continue;
                    for (var raw : start.getPieces()) {
                        if (!(raw instanceof PoolElementStructurePiece piece)
                                || !(piece.getElement() instanceof SinglePoolElement single)) continue;
                        var id = single.getTemplateLocation();
                        if (id == null || !id.getNamespace().equals("moreminecrafts")) continue;
                        for (String style : STYLES) {
                            if (!id.getPath().equals("sky_islands/" + style + "/tower")) continue;
                            Rotation rotation = piece.getRotation();
                            BlockPos towerOrigin = piece.getPosition().offset(StructureTemplate.transform(
                                    new BlockPos(0, 17, 0), Mirror.NONE, rotation, BlockPos.ZERO));
                            int quarter = switch (rotation) {
                                case NONE -> 0;
                                case CLOCKWISE_90 -> 1;
                                case CLOCKWISE_180 -> 2;
                                case COUNTERCLOCKWISE_90 -> 3;
                            };
                            var site = new SkyTowerLayout.Site(level.dimension().identifier().toString(),
                                    towerOrigin.getX(), towerOrigin.getY(), towerOrigin.getZ(), quarter);
                            samples.putIfAbsent(style, new Sample(style, structureId.toString(), site, piece, start, cx, cz));
                        }
                    }
                }
                if (samples.size() == STYLES.size() && scanned >= 512) break outer;
            }
        }
        for (Sample sample : samples.values()) {
            var box = sample.piece().getBoundingBox();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++)
                    level.getChunk(cx, cz, ChunkStatus.FULL, true);
        }
        LOG.info("SKY_TOWER_COMPOSITE_SCAN chunks={} styles={}", scanned, samples.keySet());
        return samples;
    }

    private static RestartSnapshot loadRegisteredStarts(MinecraftServer server) {
        SkyTowerData data = server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        Map<String, Integer> progress = new LinkedHashMap<>();
        Map<String, Boolean> found = new LinkedHashMap<>();
        for (var site : new ArrayList<>(data.sites().values())) {
            progress.put(site.id(), data.completedMask(site.id()));
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            int cx = Math.floorDiv(site.x(), 16), cz = Math.floorDiv(site.z(), 16);
            ChunkAccess startChunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
            boolean match = false;
            for (StructureStart start : startChunk.getAllStarts().values()) {
                if (!start.isValid()) continue;
                for (var raw : start.getPieces()) {
                    if (!(raw instanceof PoolElementStructurePiece piece)
                            || !(piece.getElement() instanceof SinglePoolElement single)
                            || single.getTemplateLocation() == null
                            || !single.getTemplateLocation().getPath().matches("sky_islands/(snowy|meadow|forest|badlands|desert|rocky)/tower")) continue;
                    BlockPos origin = SkyTowerWorldgenRegistration.towerOrigin(piece.getPosition(), piece.getRotation());
                    match |= origin.getX() == site.x() && origin.getY() == site.y() && origin.getZ() == site.z()
                            && SkyTowerWorldgenRegistration.rotationIndex(piece.getRotation()) == site.rotation();
                    var box = piece.getBoundingBox();
                    for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++)
                        for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
                            level.getChunk(x, z, ChunkStatus.FULL, true);
                }
            }
            found.put(site.id(), match);
        }
        return new RestartSnapshot(data.sites().size(), progress, found);
    }

    private static Map<String, Object> verifyRestart(MinecraftServer server, RestartSnapshot snapshot) {
        SkyTowerData data = server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        Map<String, Integer> progressAfter = new LinkedHashMap<>();
        for (var site : data.sites().values()) progressAfter.put(site.id(), data.completedMask(site.id()));
        boolean allStartsFound = snapshot.structureStartsFound().values().stream().allMatch(Boolean::booleanValue);
        boolean sameProgress = snapshot.progressBefore().equals(progressAfter);
        boolean sameSites = snapshot.sitesBefore() == data.sites().size()
                && data.sites().keySet().containsAll(snapshot.progressBefore().keySet());
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("seed", server.getLevel(Level.OVERWORLD).getSeed());
        report.put("restart", true);
        report.put("sites_before", snapshot.sitesBefore());
        report.put("sites_after", data.sites().size());
        report.put("structure_start_reloaded_for_site", snapshot.structureStartsFound());
        report.put("progress_before", snapshot.progressBefore());
        report.put("progress_after", progressAfter);
        report.put("same_sites_without_duplicates", sameSites);
        report.put("same_progress", sameProgress);
        report.put("pass", !data.quarantined() && sameSites && sameProgress && allStartsFound);
        return report;
    }

    private static Map<String, Object> verify(MinecraftServer server, Map<String, Sample> samples) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        SkyTowerData data = server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        Map<String, Object> styles = new LinkedHashMap<>();
        boolean allPassed = samples.size() == STYLES.size() && !data.quarantined();
        for (String style : STYLES) {
            Sample sample = samples.get(style);
            if (sample == null) {
                styles.put(style, Map.of("pass", false, "reason", "no natural start found in scan radius"));
                allPassed = false;
                continue;
            }
            var site = sample.site();
            int spawners = 0, vaults = 0, barriers = 0, badBarrierCells = 0, comboConfigs = 0, towerExcluded = 0;
            for (int i = 0; i < 16; i++) {
                var a = site.spawner(i);
                if (level.getBlockState(new BlockPos(a.x(), a.y(), a.z())).is(Blocks.TRIAL_SPAWNER)) spawners++;
                // SKY-MOB §15.6 P4/P5/C2: registered spawners carry the approved group C combination configs.
                if (level.getBlockEntity(new BlockPos(a.x(), a.y(), a.z()))
                        instanceof net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity be) {
                    var cfg = ((dev.itzgamesgd.moreminecrafts.mixin.TrialSpawnerConfigAccessor) (Object) be.getTrialSpawner()).moreminecrafts$config();
                    String combo = dev.itzgamesgd.moreminecrafts.skytower.SkyTowerTrialSpawners.combo(i);
                    if (cfg.normal().is(dev.itzgamesgd.moreminecrafts.skytower.SkyTowerTrialSpawners.key(combo, false))
                            && cfg.ominous().is(dev.itzgamesgd.moreminecrafts.skytower.SkyTowerTrialSpawners.key(combo, true)))
                        comboConfigs++;
                }
                // Q33 B: every spawner altar lies in the Sky Tower natural-spawn exclusion.
                if (dev.itzgamesgd.moreminecrafts.mob.sky.SkyIslandRegion.inSkyTower(level, new BlockPos(a.x(), a.y() + 1, a.z())))
                    towerExcluded++;
            }
            for (int i = 0; i < 8; i++) {
                var a = site.vault(i);
                if (level.getBlockState(new BlockPos(a.x(), a.y(), a.z())).is(Blocks.VAULT)) vaults++;
            }
            for (int floor = 0; floor < 4; floor++) for (int cell = 0; cell < 18; cell++) {
                var a = site.barrier(floor, cell);
                if (level.getBlockState(new BlockPos(a.x(), a.y(), a.z())).isAir()) barriers++;
                else badBarrierCells++;
            }
            var box = sample.piece().getBoundingBox();
            boolean chunksLoaded = true;
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++)
                    chunksLoaded &= level.getChunkSource().getChunkNow(cx, cz) != null;
            long compositePieces = sample.start().getPieces().stream().filter(p -> p instanceof PoolElementStructurePiece pooled
                    && pooled.getElement() instanceof SinglePoolElement single
                    && single.getTemplateLocation() != null
                    && single.getTemplateLocation().getPath().matches("sky_islands/(snowy|meadow|forest|badlands|desert|rocky)/tower")).count();
            long legacyTowerChildren = sample.start().getPieces().stream().filter(p -> p instanceof PoolElementStructurePiece pooled
                    && pooled.getElement().toString().matches(".*moreminecrafts:sky_islands/tower_[567].*")).count();
            boolean registered = data.sites().containsKey(site.id());
            boolean pass = spawners == 16 && vaults == 8 && barriers == 72 && badBarrierCells == 0
                    && chunksLoaded && compositePieces == 1 && legacyTowerChildren == 0 && registered
                    && comboConfigs == 16 && towerExcluded == 16;
            allPassed &= pass;
            styles.put(style, Map.ofEntries(Map.entry("pass", pass), Map.entry("structure", sample.structure()),
                    Map.entry("site", site.id()), Map.entry("rotation", site.rotation()),
                    Map.entry("seen_chunk", sample.seenChunkX() + "," + sample.seenChunkZ()),
                    Map.entry("piece_bounds", box.toString()), Map.entry("chunks_loaded", chunksLoaded),
                    Map.entry("composite_piece_count", compositePieces), Map.entry("legacy_tower_child_count", legacyTowerChildren),
                    Map.entry("trial_spawners", spawners), Map.entry("vaults", vaults),
                    Map.entry("runtime_barrier_air_cells", barriers), Map.entry("blocked_barrier_cells", badBarrierCells),
                    Map.entry("automatically_registered", registered),
                    Map.entry("group_c_combo_configs", comboConfigs), Map.entry("sky_tower_spawn_exclusion", towerExcluded)));
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("seed", level.getSeed());
        report.put("styles_found", samples.keySet());
        report.put("automatic_registration_site_count", data.sites().size());
        report.put("styles", styles);
        report.put("pass", allPassed);
        report.put("client_movement_visual_and_restart", "not tested by this fresh-world server probe");
        return report;
    }
}
