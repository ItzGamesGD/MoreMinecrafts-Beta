package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;

public final class SkyHabitatGameTests {
    @GameTest public void matchingHighGroundWithoutStructureIsOutside(GameTestHelper ctx) {
        BlockPos floor = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.getLevel().setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        ctx.assertTrue(SkyIslandHabitat.find(ctx.getLevel(), floor.above()).isEmpty(),
                "A solid floor without a generated island start is never a habitat");
        ctx.succeed();
    }
    @GameTest public void nonNaturalSourcesNeverEnterNaturalSpawnGate(GameTestHelper ctx) {
        BlockPos pos = ctx.absolutePos(BlockPos.ZERO);
        for (EntitySpawnReason reason : EntitySpawnReason.values()) {
            if (reason != EntitySpawnReason.NATURAL)
                ctx.assertTrue(!SkySpawnRules.allows(ctx.getLevel(), pos, reason),
                        "Source must be natural: " + reason);
        }
        ctx.succeed();
    }
    @GameTest public void airConnectorIsNotOriginalSolidSupport(GameTestHelper ctx) {
        BlockPos origin = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.getLevel().setBlock(origin, Blocks.JIGSAW.defaultBlockState(), 3);
        ((net.minecraft.world.level.block.entity.JigsawBlockEntity) ctx.getLevel().getBlockEntity(origin))
                .setFinalState("minecraft:air");
        var template = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
        template.fillFromWorld(ctx.getLevel(), origin, new net.minecraft.core.Vec3i(1,1,1), false, java.util.List.of());
        ctx.assertTrue(!SkyIslandHabitat.originalSupport(ctx.getLevel(), template, BlockPos.ZERO),
                "Connector replaced by air must not become ground habitat when later filled by a player");
        ctx.succeed();
    }
}
