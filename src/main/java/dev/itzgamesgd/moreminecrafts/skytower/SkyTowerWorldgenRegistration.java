package dev.itzgamesgd.moreminecrafts.skytower;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;

/** Registers a composite island/tower only after its persisted structure start and all anchors are loaded. */
public final class SkyTowerWorldgenRegistration {
    private record Pending(ServerLevel level, SkyTowerLayout.Site site, BoundingBox bounds) {}
    private static final Map<String, Pending> PENDING = new LinkedHashMap<>();

    private SkyTowerWorldgenRegistration() {}

    public static void initialize() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newChunk) -> {
            discover(level, chunk);
            reconcileRooftop(level, chunk);
        });
        ServerTickEvents.END_SERVER_TICK.register(SkyTowerWorldgenRegistration::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    private static void reconcileRooftop(ServerLevel level, ChunkAccess chunk) {
        var data = level.getServer().overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        if (data.quarantined() || data.sites().isEmpty()) return;
        String dim = level.dimension().identifier().toString();
        ChunkPos chunkPos = chunk.getPos();
        for (var site : data.sites().values()) {
            if (!site.dimension().equals(dim)) continue;
            var pt = site.rooftopVault();
            if (new ChunkPos(pt.x() >> 4, pt.z() >> 4).equals(chunkPos)) {
                SkyTowerVaultRewards.placeAndConfigureRooftopVault(level, site);
            }
        }
    }

    private static void discover(ServerLevel level, ChunkAccess chunk) {
        for (var entry : chunk.getAllStarts().entrySet()) {
            var start = entry.getValue();
            if (!start.isValid()) continue;
            for (var raw : start.getPieces()) {
                if (!(raw instanceof PoolElementStructurePiece piece)
                        || !(piece.getElement() instanceof SinglePoolElement single)) continue;
                var template = single.getTemplateLocation();
                if (template == null || !template.getNamespace().equals("moreminecrafts")
                        || !template.getPath().matches("sky_islands/(snowy|meadow|forest|badlands|desert|rocky)/tower")) continue;
                var origin = towerOrigin(piece.getPosition(), piece.getRotation());
                var site = new SkyTowerLayout.Site(level.dimension().identifier().toString(),
                        origin.getX(), origin.getY(), origin.getZ(), rotationIndex(piece.getRotation()));
                PENDING.putIfAbsent(site.id(), new Pending(level, site, piece.getBoundingBox()));
            }
        }
    }

    /** The approved tower's unrotated NBT minimum is (0,0,0) inside the composite at local Y=17. */
    public static BlockPos towerOrigin(BlockPos piecePosition, Rotation rotation) {
        return piecePosition.offset(StructureTemplate.transform(new BlockPos(0, 17, 0), Mirror.NONE, rotation, BlockPos.ZERO));
    }

    public static int rotationIndex(Rotation rotation) {
        return switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
    }

    private static void tick(MinecraftServer server) {
        var data = server.overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        if (data.quarantined()) return;
        var iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Pending pending = entry.getValue();
            if (pending.level().getServer() != server) continue;
            if (data.sites().containsKey(pending.site().id())) {
                iterator.remove();
                continue;
            }
            if (!allBoundsChunksLoaded(pending.level(), pending.bounds()) || !anchorsPresent(pending.level(), pending.site())) continue;
            try {
                boolean added = data.register(pending.site());
                if (added) {
                    MoreMinecrafts.LOGGER.info("Registered generated Sky Tower {}", pending.site().id());
                    SkyTowerVaultRewards.configureSiteVaults(pending.level(), pending.site());
                }
                iterator.remove();
            } catch (IllegalArgumentException | IllegalStateException conflict) {
                MoreMinecrafts.LOGGER.error("Could not register generated Sky Tower {}: {}", pending.site().id(), conflict.getMessage());
                iterator.remove();
            }
        }
    }

    private static boolean allBoundsChunksLoaded(ServerLevel level, BoundingBox bounds) {
        for (int cx = bounds.minX() >> 4; cx <= bounds.maxX() >> 4; cx++) {
            for (int cz = bounds.minZ() >> 4; cz <= bounds.maxZ() >> 4; cz++) {
                if (level.getChunkSource().getChunkNow(cx, cz) == null) return false;
            }
        }
        return true;
    }

    private static boolean anchorsPresent(ServerLevel level, SkyTowerLayout.Site site) {
        for (int i = 0; i < 16; i++) {
            var point = site.spawner(i);
            var pos = new BlockPos(point.x(), point.y(), point.z());
            if (!level.getBlockState(pos).is(Blocks.TRIAL_SPAWNER)) return false;
        }
        for (int i = 0; i < 8; i++) {
            var point = site.vault(i);
            var pos = new BlockPos(point.x(), point.y(), point.z());
            if (!level.getBlockState(pos).is(Blocks.VAULT)) return false;
        }
        for (int floor = 0; floor < 4; floor++) {
            for (int cell = 0; cell < 18; cell++) {
                var point = site.barrier(floor, cell);
                var pos = new BlockPos(point.x(), point.y(), point.z());
                if (!level.getBlockState(pos).isAir()) return false;
            }
        }
        return true;
    }
}
