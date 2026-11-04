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
    private static final Map<StructureTemplate, Set<BlockPos>> SOLIDS = new WeakHashMap<>();
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
    private static synchronized Set<BlockPos> solids(ServerLevel level, StructureTemplate template) {
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
            Set<BlockPos> result = new HashSet<>();
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
                if (generatedSolid) result.add(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)));
            }
            return Set.copyOf(result);
        });
    }
}
