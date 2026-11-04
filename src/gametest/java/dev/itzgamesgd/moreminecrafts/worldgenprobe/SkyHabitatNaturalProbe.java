package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyIslandHabitat;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkySpawnRules;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Opt-in integration probe. Uses generated starts and FULL chunks; never places templates or entities. */
public final class SkyHabitatNaturalProbe implements ModInitializer {
    @Override public void onInitialize() {
        if (System.getProperty("mm.habitat.out") == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            boolean delayed = false;
            try {
                Files.deleteIfExists(Path.of(System.getProperty("mm.habitat.out")));
                var level = server.getLevel(Level.OVERWORLD);
                int cx = Integer.getInteger("mm.habitat.cx", -22), cz = Integer.getInteger("mm.habitat.cz", -14);
                level.getChunk(cx, cz, ChunkStatus.FULL, true);
                var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
                var starts = level.structureManager().startsForStructure(cx, cz,
                        s -> SkyIslandHabitat.isSkyStructure(registry.getKey(s)));
                List<Map<String,Object>> rows = new ArrayList<>();
                for (var start : starts) for (var piece : start.getPieces()) {
                    var box = piece.getBoundingBox();
                    for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++)
                        for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) level.getChunk(x,z,ChunkStatus.FULL,true);
                    search: for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++)
                        for (int y = box.minY()+1; y <= box.maxY()+1; y++) {
                            BlockPos p = new BlockPos(x,y,z);
                            var found = SkyIslandHabitat.find(level,p);
                            if (found.isEmpty()) continue;
                            var h=found.get();
                            if (!SkySpawnRules.allows(level,p,EntitySpawnReason.NATURAL)
                                    || SkySpawnRules.allows(level,p,EntitySpawnReason.COMMAND)) throw new AssertionError("spawn source isolation");
                            if (SkyIslandHabitat.find(level,new BlockPos(x,level.getMinY()+1,z)).isPresent()) throw new AssertionError("ground contamination");
                            if (SkyIslandHabitat.find(level,p.above(100)).isPresent()) throw new AssertionError("air contamination");
                            rows.add(Map.of("x",x,"y",y,"z",z,"structure",h.structure().toString(),"template",h.template().toString(),
                                    "relativeHeight",h.relativeHeight(),"biome",level.getBiome(p).unwrapKey().orElseThrow().identifier().toString()));
                            break search;
                        }
                }
                if (rows.isEmpty()) throw new AssertionError("No positive natural surface");
                Path out=Path.of(System.getProperty("mm.habitat.out"));Files.createDirectories(out.getParent());
                Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("seed",level.getSeed(),"surfaces",rows,"pass",true)));
                server.saveEverything(false,true,true);
                if (Boolean.getBoolean("mm.habitat.unload")) {
                    delayed = true;
                    int[] ticks = {0};
                    net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(current -> {
                        if (current != server || ++ticks[0] != 500) return;
                        try {
                            for (var row : rows) {
                                BlockPos pos = new BlockPos((int) row.get("x"), (int) row.get("y"), (int) row.get("z"));
                                if (level.hasChunkAt(pos)) throw new AssertionError("test chunk did not unload");
                                if (SkyIslandHabitat.find(level, pos).isPresent() || level.hasChunkAt(pos))
                                    throw new AssertionError("unloaded lookup must not load a chunk");
                            }
                            for (var row : rows) {
                                BlockPos pos = new BlockPos((int) row.get("x"), (int) row.get("y"), (int) row.get("z"));
                                level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
                                if (SkyIslandHabitat.find(level, pos).isEmpty()) throw new AssertionError("reload lost habitat");
                            }
                            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(
                                    Map.of("seed", level.getSeed(), "surfaces", rows, "chunkUnload", true, "chunkReload", true, "pass", true)));
                        } catch (Throwable error) {
                            error.printStackTrace();
                            try { Files.writeString(out, "{\"pass\":false,\"error\":\"unload/reload check failed; see log\"}"); }
                            catch (Exception ignored) {}
                        } finally { server.halt(false); }
                    });
                }
            } catch (Throwable e) {
                e.printStackTrace();
                try {
                    Path out = Path.of(System.getProperty("mm.habitat.out"));
                    Files.createDirectories(out.getParent());
                    Files.writeString(out, new GsonBuilder().create().toJson(Map.of("pass", false, "error", e.toString())));
                } catch (Exception ignored) {}
            } finally { if (!delayed) server.halt(false); }
        });
    }
}
