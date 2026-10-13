package dev.itzgamesgd.moreminecrafts.worldgen.structure;

import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Observes normal generation only; opt-in validation never places or forces the arena. */
public final class ArenaWorldgenProbe {
    private static boolean running;
    private static int attempt;
    private static final HashSet<Long> SEEN = new HashSet<>();
    private static final ArrayDeque<ChunkPos> CHUNKS = new ArrayDeque<>();
    private static PoolElementStructurePiece arena;
    private static final Map<String, Object> RESULT = new LinkedHashMap<>();

    private ArenaWorldgenProbe() {}

    public static void initialize() {
        if (!"1".equals(System.getenv("MOREMINECRAFTS_ARENA_VALIDATION"))) return;
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("mmarena").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("validate").executes(command -> {
                            if (running) return 0;
                            attempt = 0;
                            arena = null;
                            SEEN.clear();
                            CHUNKS.clear();
                            RESULT.clear();
                            running = true;
                            return 1;
                        }))));
        ServerTickEvents.END_SERVER_TICK.register(ArenaWorldgenProbe::tick);
    }

    private static void tick(MinecraftServer server) {
        if (!running) return;
        try {
            var level = server.overworld();
            if (arena == null) {
                if (attempt >= 81) throw new IllegalStateException("No arena after 81 deterministic locate origins");
                int index = attempt++;
                int x = ((index % 9) - 4) * 2048;
                int z = ((index / 9) - 4) * 2048;
                var city = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(
                        ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace("ancient_city")));
                BlockPos located = level.findNearestMapStructure(HolderSet.direct(city), new BlockPos(x, -27, z), 48, false);
                MoreMinecrafts.LOGGER.info("ARENA_WORLDGEN_SEARCH attempt={} origin={},{} located={}", attempt, x, z, located);
                if (located == null) return;
                var chunkPos = ChunkPos.containing(located);
                if (!SEEN.add(chunkPos.pack())) return;
                var chunk = level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.STRUCTURE_STARTS);
                var start = level.structureManager().getStartForStructure(city.value(), chunk);
                if (start == null || !start.isValid()) return;
                for (var piece : start.getPieces()) {
                    if (piece instanceof PoolElementStructurePiece pooled
                            && pooled.getElement().toString().contains("moreminecrafts:ancient_city/arena")) {
                        arena = pooled;
                        break;
                    }
                }
                long vanilla = start.getPieces().stream().filter(piece -> piece instanceof PoolElementStructurePiece pooled
                        && pooled.getElement().toString().contains("minecraft:ancient_city/")).count();
                if (arena == null) {
                    RESULT.put("ordinary_city_without_arena", Map.of("x", chunkPos.x(), "z", chunkPos.z(), "vanilla_pieces", vanilla));
                    return;
                }
                if (vanilla < 2) throw new IllegalStateException("City lacks vanilla components");
                RESULT.put("seed", level.getSeed());
                RESULT.put("production_weight", 1);
                RESULT.put("locate_attempts", attempt);
                RESULT.put("city_chunk", Map.of("x", chunkPos.x(), "z", chunkPos.z()));
                RESULT.put("vanilla_pieces", vanilla);
                RESULT.put("arena_element", arena.getElement().toString());
                RESULT.put("rotation", arena.getRotation().toString());
                var box = arena.getBoundingBox();
                RESULT.put("bounds", Map.of("min_x", box.minX(), "min_y", box.minY(), "min_z", box.minZ(),
                        "max_x", box.maxX(), "max_y", box.maxY(), "max_z", box.maxZ()));
                for (int cx = (box.minX() - 48) >> 4; cx <= (box.maxX() + 48) >> 4; cx++) {
                    for (int cz = (box.minZ() - 48) >> 4; cz <= (box.maxZ() + 48) >> 4; cz++) CHUNKS.add(new ChunkPos(cx, cz));
                }
                MoreMinecrafts.LOGGER.info("ARENA_WORLDGEN_FOUND {}", RESULT);
                return;
            }
            if (!CHUNKS.isEmpty()) {
                var pos = CHUNKS.removeFirst();
                level.getChunk(pos.x(), pos.z());
                return;
            }
            var samples = new LinkedHashMap<String, String>();
            int surviving = 0;
            for (var local : new BlockPos[]{new BlockPos(18, 0, 18), new BlockPos(12, 0, 23),
                    new BlockPos(25, 0, 13), new BlockPos(8, 0, 18), new BlockPos(18, 0, 25)}) {
                BlockPos world = StructureTemplate.transform(local, Mirror.NONE, arena.getRotation(), BlockPos.ZERO).offset(arena.getPosition());
                var state = level.getBlockState(world);
                samples.put(world.toShortString(), state.toString());
                if (state.toString().contains("polished_deepslate") || state.toString().contains("sculk")
                        || state.toString().contains("gray_wool")) surviving++;
            }
            if (surviving < 3) throw new IllegalStateException("Placed arena voxel probes failed: " + samples);
            RESULT.put("physical_voxel_samples", samples);
            var center = StructureTemplate.transform(new BlockPos(18, 0, 18), Mirror.NONE, arena.getRotation(), BlockPos.ZERO).offset(arena.getPosition());
            RESULT.put("cameras", new Object[]{camera(center, 22, 12, 24), camera(center, -17, 8, 20)});
            Files.createDirectories(Path.of("../arena-validation-results"));
            Files.writeString(Path.of("../arena-validation-results/worldgen.json"), new GsonBuilder().setPrettyPrinting().create().toJson(RESULT));
            running = false;
            MoreMinecrafts.LOGGER.info("ARENA_WORLDGEN_VERIFIED {}", RESULT);
        } catch (Exception error) {
            running = false;
            MoreMinecrafts.LOGGER.error("ARENA_WORLDGEN_FAILED", error);
        }
    }

    private static Map<String, Object> camera(BlockPos center, int dx, int dy, int dz) {
        double yaw = Math.toDegrees(Math.atan2(dx, -dz));
        double pitch = Math.toDegrees(Math.atan2(dy - 1, Math.hypot(dx, dz)));
        return Map.of("x", center.getX() + dx + 0.5, "y", center.getY() + dy + 0.5,
                "z", center.getZ() + dz + 0.5, "yaw", yaw, "pitch", pitch);
    }
}
