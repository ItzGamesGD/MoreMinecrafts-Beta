package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Disposable four-block API fixture, NOT arena construction or a production journal. */
final class St05Stage0CStorageProbe {
    private static final Gson GSON = new Gson();
    // Four chunks, away from spawn; fixed positions, fixed fixture version.
    private static BlockPos pos(int i) { return new BlockPos(1600 + i * 16, 200, 1600); }

    static void run(MinecraftServer server, ServerLevel level, String mode, Path out,
                    Map<String, Object> doc) throws Exception {
        Path journal = out.getParent().resolve("fixture-journal.json");
        doc.put("evidence_kind", "LIVE_26.3_DISPOSABLE_BLOCK_FIXTURE");
        doc.put("arena_atomicity_proven", false);
        if (mode.equals("c-prepare")) {
            JsonObject j = new JsonObject();
            j.addProperty("version", 1);
            j.addProperty("seed", level.getSeed());
            j.addProperty("planned", 4);
            j.addProperty("written", 2);
            j.addProperty("budgetUsed", 2);
            j.addProperty("reservation", true);
            j.addProperty("rollDenominator", 1);
            j.addProperty("rollWins", 1);
            j.addProperty("placed", 0);
            Files.writeString(journal, GSON.toJson(j)); // plan before effects
            for (int i = 0; i < 4; i++) {
                level.getChunkAt(pos(i));
                level.setBlock(pos(i), (i < 2 ? Blocks.GOLD_BLOCK : Blocks.AIR).defaultBlockState(), 3);
            }
            server.saveAllChunks(true, true, true);
            doc.put("partial_blocks_saved", 2);
            doc.put("operational", false);
        } else {
            String status = "QUARANTINED";
            int writes = 0;
            try {
                JsonObject j = GSON.fromJson(Files.readString(journal), JsonObject.class);
                boolean valid = j.get("version").getAsInt() == 1
                    && j.get("seed").getAsLong() == level.getSeed()
                    && j.get("planned").getAsInt() == 4 && j.get("written").getAsInt() == 2
                    && j.get("budgetUsed").getAsInt() == 2 && j.get("reservation").getAsBoolean()
                    && j.get("rollDenominator").getAsInt() == 1 && j.get("rollWins").getAsInt() == 1
                    && j.get("placed").getAsInt() == 0;
                for (int i = 0; i < 4; i++)
                    valid &= level.getBlockState(pos(i)).is(i < 2 ? Blocks.GOLD_BLOCK : Blocks.AIR);
                if (valid) {
                    for (int i = 2; i < 4; i++) {
                        level.setBlock(pos(i), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                        writes++;
                    }
                    server.saveAllChunks(true, true, true);
                    boolean complete = true;
                    for (int i = 0; i < 4; i++) complete &= level.getBlockState(pos(i)).is(Blocks.GOLD_BLOCK);
                    if (!complete) throw new IllegalStateException("completion mismatch");
                    status = "VERIFIED_FIXTURE";
                }
            } catch (RuntimeException | java.io.IOException invalid) {
                doc.put("quarantine_reason", invalid.getClass().getSimpleName());
            }
            doc.put("status", status);
            doc.put("new_block_writes", writes);
            doc.put("reward_grants", 0);
            if (mode.equals("c-recover") && !status.equals("VERIFIED_FIXTURE"))
                throw new IllegalStateException("valid restart fixture failed");
            if (mode.equals("c-corrupt") && (!status.equals("QUARANTINED") || writes != 0))
                throw new IllegalStateException("corrupt checkpoint was resumed");
        }
        // Demonstrate final-state reads see fluid hazards; no arena seal/path claim.
        BlockPos hazard = new BlockPos(1680, 200, 1600);
        level.setBlock(hazard, Blocks.LAVA.defaultBlockState(), 3);
        if (level.getFluidState(hazard).isEmpty()) throw new IllegalStateException("lava missed");
        doc.put("lava_observable", true);
        level.setBlock(hazard, Blocks.WATER.defaultBlockState(), 3);
        if (level.getFluidState(hazard).isEmpty()) throw new IllegalStateException("water missed");
        doc.put("water_observable", true);
        level.setBlock(hazard, Blocks.AIR.defaultBlockState(), 3);
    }
}
