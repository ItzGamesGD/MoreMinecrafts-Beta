package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Matched-sample Overworld ore abundance probe for Etherium vs Diamond (not production; inert unless
 * {@code -Dmm.ore.out} is set). Forces a square of Overworld chunks to FULL with the real generator and counts diamond
 * and etherium ore blocks (stone and deepslate variants) in the same interior chunks, so the T2 §18.1 "one quarter of
 * diamond ore volume" rule is measured from generated terrain, not derived from vein size x attempts.
 */
public final class OverworldOreSamplingProbe implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("overworld-ore-sampling-probe");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MARGIN = 2;

    @Override
    public void onInitialize() {
        String out = System.getProperty("mm.ore.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                run(server, Path.of(out));
            } catch (Throwable t) {
                LOG.error("overworld ore sampling failed", t);
                try {
                    Files.createDirectories(Path.of(out).getParent());
                    Files.writeString(Path.of(out), GSON.toJson(Map.of("error", String.valueOf(t))), StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // the missing/invalid output file is the failure signal for the driver script
                }
            }
            Runtime.getRuntime().halt(0); // results are on disk; do not save thousands of forced chunks
        });
    }

    private static Block block(String id) {
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        if (block == null || BuiltInRegistries.BLOCK.getKey(block).getPath().equals("air")) {
            throw new IllegalStateException("block not registered: " + id);
        }
        return block;
    }

    private static void run(MinecraftServer server, Path out) throws IOException {
        int radius = Integer.getInteger("mm.ore.radius", 20);
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        Block diamond = block("minecraft:diamond_ore");
        Block deepDiamond = block("minecraft:deepslate_diamond_ore");
        Block etherium = block("moreminecrafts:etherium_ore");
        Block deepEtherium = block("moreminecrafts:deepslate_etherium_ore");

        for (int cx = -radius; cx < radius; cx++)
            for (int cz = -radius; cz < radius; cz++) level.getChunk(cx, cz, ChunkStatus.FULL, true);

        Map<Integer, Integer> diamondY = new TreeMap<>();
        Map<Integer, Integer> etheriumY = new TreeMap<>();
        long diamondTotal = 0, etheriumTotal = 0;
        int counted = 0, chunksWithMoreEtherium = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = -radius + MARGIN; cx < radius - MARGIN; cx++) {
            for (int cz = -radius + MARGIN; cz < radius - MARGIN; cz++) {
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
                int d = 0, e = 0;
                for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
                    pos.set(chunk.getPos().getMinBlockX() + dx, y, chunk.getPos().getMinBlockZ() + dz);
                    Block b = chunk.getBlockState(pos).getBlock();
                    if (b == diamond || b == deepDiamond) { d++; diamondY.merge(y, 1, Integer::sum); }
                    else if (b == etherium || b == deepEtherium) { e++; etheriumY.merge(y, 1, Integer::sum); }
                }
                diamondTotal += d;
                etheriumTotal += e;
                if (e > d) chunksWithMoreEtherium++;
                counted++;
            }
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("label", System.getProperty("mm.ore.label", ""));
        doc.put("seed", level.getSeed());
        doc.put("method", "force FULL generation of a (2R)x(2R) Overworld chunk square, count interior chunks only (margin "
                + MARGIN + "), scan every block from minY to maxY, same chunks for both ores");
        doc.put("radius_chunks", radius);
        doc.put("chunks_counted", counted);
        doc.put("diamond_blocks", diamondTotal);
        doc.put("etherium_blocks", etheriumTotal);
        doc.put("etherium_to_diamond_ratio", diamondTotal == 0 ? null : (double) etheriumTotal / diamondTotal);
        doc.put("diamond_per_chunk", (double) diamondTotal / counted);
        doc.put("etherium_per_chunk", (double) etheriumTotal / counted);
        doc.put("chunks_where_etherium_exceeds_diamond", chunksWithMoreEtherium);
        doc.put("diamond_by_y", diamondY);
        doc.put("etherium_by_y", etheriumY);
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(doc), StandardCharsets.UTF_8);
        LOG.info("OVERWORLD_ORE_SAMPLING_DONE diamond={} etherium={} chunks={}", diamondTotal, etheriumTotal, counted);
    }
}
