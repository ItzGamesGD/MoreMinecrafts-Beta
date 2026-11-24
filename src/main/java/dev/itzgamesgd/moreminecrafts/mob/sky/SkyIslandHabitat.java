package dev.itzgamesgd.moreminecrafts.mob.sky;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Ground habitat only. Generated starts establish provenance; original solid voxels establish the surface.
 * No altitude threshold, inferred radius, world writes or additional natural spawn registrations.
 */
public final class SkyIslandHabitat {
    private static final Set<String> IDS = Set.of("sky_island_mountain_snowy", "sky_island_mountain_rocky",
            "sky_island_mountain_meadow", "sky_island_snowy", "sky_island_meadow", "sky_island_forest",
            "sky_island_desert", "sky_island_badlands");
    /** Per-template generated-solid mask. P3.9: a dense bit mask over the solid bounds instead of {@code Set.copyOf(HashSet<BlockPos>)}. */
    private static final Map<StructureTemplate, SolidMask> SOLIDS = new WeakHashMap<>();

    /**
     * Same membership as the former immutable BlockPos set, in O(volume / 8) bytes and O(1) lookups. The former {@code Set.copyOf}
     * (open addressing) degraded to ~0.24 ms per lookup and 7-80 s to build on large templates because {@code BlockPos.hashCode()}
     * ({@code (y + z * 31) * 31 + x}) maps ~100k+ clustered positions to a much smaller set of distinct hash codes (measured in P3.9).
     */
    static final class SolidMask {
        private final int minX, minY, minZ, sx, sy, sz; private final BitSet bits;
        private SolidMask(int minX, int minY, int minZ, int sx, int sy, int sz, BitSet bits) { this.minX = minX; this.minY = minY; this.minZ = minZ; this.sx = sx; this.sy = sy; this.sz = sz; this.bits = bits; }
        static final SolidMask EMPTY = new SolidMask(0, 0, 0, 0, 0, 0, new BitSet());
        boolean contains(BlockPos p) {
            int x = p.getX() - minX, y = p.getY() - minY, z = p.getZ() - minZ;
            return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz && bits.get((y * sz + z) * sx + x);
        }
        int cardinality() { return bits.cardinality(); }
        long bytes() { return bits.size() / 8; }
    }
    private SkyIslandHabitat() {}
    public record Habitat(Identifier structure, Identifier template, boolean mountainFamily,
                          int terrainHeight, int relativeHeight) {}

    public static void initialize() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
    }
    private static synchronized void clear() { SOLIDS.clear(); }
    public static boolean isSkyStructure(Identifier id) {
        return id != null && id.getNamespace().equals("moreminecrafts") && IDS.contains(id.getPath());
    }
    public static Optional<Habitat> find(ServerLevel level, BlockPos feet) {
        if (level.dimension() != Level.OVERWORLD || !level.hasChunkAt(feet)
                || !level.hasChunkAt(feet.below())) return Optional.empty();
        var support = level.getBlockState(feet.below());
        if (!support.isSolid() || !level.getFluidState(feet).isEmpty()
                || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty())
            return Optional.empty();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        // Only starts referenced by this loaded chunk; never scan regions or call locate.
        for (var start : level.structureManager().startsForStructure(feet.getX() >> 4, feet.getZ() >> 4,
                structure -> isSkyStructure(registry.getKey(structure)))) {
            if (!start.isValid()) continue;
            for (var raw : start.getPieces()) {
                if (!(raw instanceof PoolElementStructurePiece piece)
                        || !(piece.getElement() instanceof SinglePoolElement single)) continue;
                Identifier id = single.getTemplateLocation();
                if (id == null || !id.getNamespace().equals("moreminecrafts")
                        || !id.getPath().startsWith("sky_islands/")
                        || !piece.getBoundingBox().isInside(feet.below())) continue;
                var template = level.getStructureTemplateManager().get(id);
                if (template.isEmpty()) continue;
                // Invert the actual saved piece rotation about its origin, including negative world coordinates.
                var inverse = switch (piece.getRotation()) {
                    case CLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
                    case COUNTERCLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
                    default -> piece.getRotation();
                };
                BlockPos local = StructureTemplate.transform(feet.below().subtract(piece.getPosition()), Mirror.NONE, inverse, BlockPos.ZERO);
                if (!originalSupport(level, template.get(), local)) continue;
                int terrain = level.getChunkSource().getGenerator().getBaseHeight(feet.getX(), feet.getZ(),
                        Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
                Identifier structureId = registry.getKey(start.getStructure());
                return Optional.of(new Habitat(structureId, id, structureId.getPath().startsWith("sky_island_mountain_"),
                        terrain, feet.getY() - terrain));
            }
        }
        return Optional.empty();
    }
    static boolean originalSupport(ServerLevel level, StructureTemplate template, BlockPos local) {
        return solids(level, template).contains(local);
    }
    static synchronized SolidMask solids(ServerLevel level, StructureTemplate template) {
        return SOLIDS.computeIfAbsent(template, key -> {
            CompoundTag tag = key.save(new CompoundTag());
            var palette = tag.getListOrEmpty("palette");
            boolean[] solid = new boolean[palette.size()];
            boolean[] connector = new boolean[palette.size()];
            for (int i = 0; i < solid.length; i++) {
                var state = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), palette.getCompoundOrEmpty(i));
                solid[i] = state.isSolid();
                connector[i] = state.is(Blocks.JIGSAW);
            }
            int[] xs = new int[1024], ys = new int[1024], zs = new int[1024]; int n = 0;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (var entry : tag.getListOrEmpty("blocks")) {
                if (!(entry instanceof CompoundTag block)) continue;
                int state = block.getIntOr("state", -1);
                var pos = block.getListOrEmpty("pos");
                if (state < 0 || state >= solid.length || pos.size() != 3) continue;
                boolean generatedSolid = solid[state];
                if (connector[state]) {
                    try {
                        generatedSolid = BlockStateParser.parseForBlock(level.registryAccess().lookupOrThrow(Registries.BLOCK),
                                block.getCompoundOrEmpty("nbt").getStringOr("final_state", "minecraft:air"), false).blockState().isSolid();
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException invalid) {
                        generatedSolid = false;
                    }
                }
                if (!generatedSolid) continue;
                int x = pos.getIntOr(0, 0), y = pos.getIntOr(1, 0), z = pos.getIntOr(2, 0);
                if (n == xs.length) { xs = Arrays.copyOf(xs, n * 2); ys = Arrays.copyOf(ys, n * 2); zs = Arrays.copyOf(zs, n * 2); }
                xs[n] = x; ys[n] = y; zs[n] = z; n++;
                minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
            }
            if (n == 0) return SolidMask.EMPTY;
            int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
            BitSet bits = new BitSet(sx * sy * sz);
            for (int i = 0; i < n; i++) bits.set(((ys[i] - minY) * sz + (zs[i] - minZ)) * sx + (xs[i] - minX));
            return new SolidMask(minX, minY, minZ, sx, sy, sz, bits);
        });
    }
}
