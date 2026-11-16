package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03AssemblyPiece;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03HeadquartersStructure;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Tracks chunk completion of generated ST-03 Pillager Headquarters, initializes one-time loot,
 * and ticks proximity garrison spawning and Option D post-clear village logic.
 */
public final class St03HeadquartersCompletion {
    private St03HeadquartersCompletion() {}

    private static boolean initialized;

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        St03GarrisonManager.initialize();

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) -> {
            if (level instanceof ServerLevel serverLevel) {
                onChunkLoad(serverLevel, chunk);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(St03HeadquartersCompletion::onServerTick);
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        St03HeadquartersData data = null;
        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            if (entry.getKey() instanceof St03HeadquartersStructure) {
                StructureStart ownStart = entry.getValue();
                if (ownStart != null && ownStart.isValid()) {
                    data = St03HeadquartersData.get(level);
                    long startKey = chunk.getPos().pack();
                    St03HeadquartersData.HqInstance hq = data.getOrCreate(startKey);
                    hq.committedChunks.add(startKey);
                    if (hq.origin == null) {
                        extractMetadata(ownStart, hq);
                    }
                    data.setDirty();
                }
            }
        }

        for (var refEntry : chunk.getAllReferences().entrySet()) {
            if (refEntry.getKey() instanceof St03HeadquartersStructure && !refEntry.getValue().isEmpty()) {
                if (data == null) {
                    data = St03HeadquartersData.get(level);
                }
                long chunkKey = chunk.getPos().pack();
                for (var it = refEntry.getValue().longIterator(); it.hasNext(); ) {
                    long startKey = it.nextLong();
                    St03HeadquartersData.HqInstance hq = data.getOrCreate(startKey);
                    hq.committedChunks.add(chunkKey);
                }
                data.setDirty();
            }
        }
    }

    private static void extractMetadata(StructureStart start, St03HeadquartersData.HqInstance hq) {
        hq.box = start.getBoundingBox();
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof St03AssemblyPiece assembly) {
                hq.origin = assembly.compoundOrigin();
                hq.rotation = assembly.compoundRotation();
                break;
            }
        }
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            St03HeadquartersData data = St03HeadquartersData.get(level);
            if (data.instances().isEmpty()) {
                continue;
            }
            for (St03HeadquartersData.HqInstance hq : data.instances().values()) {
                if (!hq.structureCompleted && hq.box != null && hq.origin != null && allChunksCommitted(hq)) {
                    hq.structureCompleted = true;
                    St03LootManager.initializeStructureLoot(level, hq);
                    data.setDirty();
                }
            }
            St03GarrisonManager.tick(level, data);
            St03PostClearHandler.tick(level, data);
        }
    }

    private static boolean allChunksCommitted(St03HeadquartersData.HqInstance hq) {
        BoundingBox box = hq.box;
        int minCx = box.minX() >> 4;
        int maxCx = box.maxX() >> 4;
        int minCz = box.minZ() >> 4;
        int maxCz = box.maxZ() >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!hq.committedChunks.contains(ChunkPos.pack(cx, cz))) {
                    return false;
                }
            }
        }
        return true;
    }
}
