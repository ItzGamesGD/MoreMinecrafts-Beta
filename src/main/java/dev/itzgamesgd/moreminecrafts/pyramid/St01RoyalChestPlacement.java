package dev.itzgamesgd.moreminecrafts.pyramid;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Explicit registration after controlled D1 placement; does not hook natural world generation. */
public final class St01RoyalChestPlacement {
    public static final Identifier TEMPLATE = Identifier.parse("moreminecrafts:pyramid/royal_hall");
    public static final BlockPos REFERENCE_ORIGIN = new BlockPos(-40, -24, 2);
    public static final BlockPos REFERENCE_ANCHOR = new BlockPos(-21, -20, 33);
    public static final BlockPos REFERENCE_RIGHT = new BlockPos(-20, -20, 33);
    public static final BlockPos REFERENCE_SPAWN = new BlockPos(-21, -22, 26);

    private St01RoyalChestPlacement() {}

    /** Reference coordinates first become template-local, then use Minecraft's placement transform and pivot. */
    public static BlockPos worldPos(BlockPos reference, BlockPos placementOrigin, StructurePlaceSettings settings) {
        return StructureTemplate.transform(reference.subtract(REFERENCE_ORIGIN), settings.getMirror(),
                settings.getRotation(), settings.getRotationPivot()).offset(placementOrigin);
    }

    /**
     * Vanilla ChestBlock.mirror changes facing but not handedness. Correct only the approved PRIMARY pair before
     * vanilla applies state transforms/neighbor updates, otherwise a mirrored placement becomes two single chests.
     * This in-memory processor is controlled-placement infrastructure, never a worldgen processor registration.
     */
    public static StructurePlaceSettings placementSettings(StructurePlaceSettings requested) {
        var settings = requested.copy();
        settings.popProcessor(PrimaryMirrorProcessor.INSTANCE);
        if (settings.getMirror() != net.minecraft.world.level.block.Mirror.NONE)
            settings.addProcessor(PrimaryMirrorProcessor.INSTANCE);
        return settings;
    }

    private enum PrimaryMirrorProcessor implements net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor {
        INSTANCE;
        private static final com.mojang.serialization.MapCodec<PrimaryMirrorProcessor> CODEC = com.mojang.serialization.MapCodec.unit(INSTANCE);

        @Override public com.mojang.serialization.MapCodec<PrimaryMirrorProcessor> codec() { return CODEC; }

        @Override public StructureTemplate.StructureBlockInfo processBlock(net.minecraft.world.level.LevelReader level,
                BlockPos origin, BlockPos pivot, BlockPos local, StructureTemplate.StructureBlockInfo current,
                StructurePlaceSettings settings) {
            if ((!local.equals(REFERENCE_ANCHOR.subtract(REFERENCE_ORIGIN))
                    && !local.equals(REFERENCE_RIGHT.subtract(REFERENCE_ORIGIN)))
                    || !current.state().is(net.minecraft.world.level.block.Blocks.CHEST)) return current;
            var type = current.state().getValue(net.minecraft.world.level.block.ChestBlock.TYPE);
            if (type == net.minecraft.world.level.block.state.properties.ChestType.SINGLE) return current;
            return new StructureTemplate.StructureBlockInfo(current.pos(),
                    current.state().setValue(net.minecraft.world.level.block.ChestBlock.TYPE, type.getOpposite()), current.nbt());
        }
    }

    /** Call only after successful placement with the very same origin and settings. Validates the physical pair. */
    public static Optional<RoyalChestData.Instance> registerPlaced(ServerLevel level, BlockPos placementOrigin,
                                                                 StructurePlaceSettings settings) {
        BlockPos anchor = worldPos(REFERENCE_ANCHOR, placementOrigin, settings);
        return RoyalChestRuntime.register(level, anchor, worldPos(REFERENCE_SPAWN, placementOrigin, settings), anchor,
                worldPos(REFERENCE_RIGHT, placementOrigin, settings));
    }
}
