package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Heaven sky island P2 in the real server: registration, the P2 island and the reusable species structures. */
public final class HeavenP2GameTests implements CustomTestMethodInvoker {
    private static final Identifier P2 = Identifier.parse("moreminecrafts:heaven_prototype/plain_medium_d_v5_p2");
    /** Vegetation blocks in the P2 template (manifest_p2.json p2.vegetation_blocks). */
    private static final int TEMPLATE_VEGETATION = 1232;
    /**
     * P3 (owner-approved F8): unsupported heaven vegetation pops on placement. The unchanged P2 template carries 30
     * such cells (20 petals and 8 wisteria with nothing under / above / behind them, 2 cliff vines without a cliff),
     * so 1202 remain (equal to a static fixpoint replay of the rules). Everything else (giants, trees, medium flowers, small flowers, grass) survives.
     */
    private static final int EXPECTED_VEGETATION = TEMPLATE_VEGETATION - 20 - 8 - 2;

    private static Block block(String id) {
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        if (b == Blocks.AIR) throw new IllegalStateException("block not registered: " + id);
        return b;
    }

    @GameTest
    public void heavenP2FamiliesAreRegistered(GameTestHelper ctx) {
        int models = 0;
        for (String f : HeavenP2Blocks.families()) {
            Block b = block("moreminecrafts:" + f);
            ctx.assertTrue(b instanceof HeavenPartBlock, f + " is a part family");
            HeavenPartBlock p = (HeavenPartBlock) b;
            int expected = p.partCount() * (p.hasFacing() ? 4 : 1);
            ctx.assertTrue(b.getStateDefinition().getPossibleStates().size() == expected, f + " states = parts x facings");
            ctx.assertTrue(b.asItem() == net.minecraft.world.item.Items.AIR, f + " has no item yet (policy pending)");
            models += p.partCount();
        }
        ctx.assertTrue(block("moreminecrafts:heaven_white_blossom_leaves") instanceof LeavesBlock, "white blossom leaves behave as leaves");
        MoreMinecrafts.LOGGER.info("HEAVEN_P2_FAMILIES {} models={}", HeavenP2Blocks.families().size(), models);
        ctx.succeed();
    }

    @GameTest(maxTicks = 200)
    public void heavenP2IslandPlacesWithReusedVanillaBlocks(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        StructureTemplate template = level.getStructureTemplateManager().get(P2).orElseThrow(() -> new AssertionError("missing " + P2));
        var size = template.getSize();
        BlockPos origin = ctx.absolutePos(BlockPos.ZERO).atY(200);
        ctx.assertTrue(template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS), "placeInWorld");
        // P3: vine / wisteria support is checked one tick after placement (scheduled), so count after the ticks ran
        ctx.runAfterDelay(5, () -> {
            Map<String, Integer> counts = new HashMap<>();
            int[] collidingStems = {0}, nonPersistentLeaves = {0};
            for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
                BlockState s = level.getBlockState(p);
                if (s.isAir()) continue;
                counts.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(), 1, Integer::sum);
                if (s.getBlock() instanceof HeavenPartBlock hp && hp.family().startsWith("heaven_giant_") && !s.getCollisionShape(level, p).isEmpty()) collidingStems[0]++;
                if (s.getBlock() instanceof LeavesBlock && !s.getValue(LeavesBlock.PERSISTENT)) nonPersistentLeaves[0]++;
            }
            MoreMinecrafts.LOGGER.info("HEAVEN_P2_PLACED {}", counts);
            int natural = counts.getOrDefault("moreminecrafts:heaven_grass_block", 0) + counts.getOrDefault("moreminecrafts:heaven_loam", 0)
                    + counts.getOrDefault("moreminecrafts:heaven_cream_stone", 0) + counts.getOrDefault("moreminecrafts:heaven_cream_stone_pale", 0);
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            ctx.assertTrue(natural == 6272, "6272 terrain blocks, got " + natural);
            ctx.assertTrue(counts.getOrDefault("minecraft:water", 0) == 104, "pond kept");
            ctx.assertTrue(total - natural - 104 == EXPECTED_VEGETATION, EXPECTED_VEGETATION + " vegetation blocks (template minus the 30 unsupported P2 cells), got " + (total - natural - 104));
            ctx.assertTrue(counts.getOrDefault("minecraft:oak_log", 0) > 0 && counts.getOrDefault("minecraft:cherry_leaves", 0) > 0
                    && counts.getOrDefault("moreminecrafts:heaven_white_blossom_leaves", 0) > 0, "converted trees present");
            ctx.assertTrue(nonPersistentLeaves[0] == 0, "tree leaves are persistent (no decay)");
            ctx.assertTrue(collidingStems[0] > 0, "giant-flower stems collide");
            // Characterisation for the P2-D policy table: vanilla petals and leaves on heaven grass.
            BlockPos grass = ctx.absolutePos(new BlockPos(1, 1, 1));
            level.setBlock(grass, block("moreminecrafts:heaven_grass_block").defaultBlockState(), Block.UPDATE_ALL);
            boolean petalsSurvive = Blocks.PINK_PETALS.defaultBlockState().canSurvive(level, grass.above());
            MoreMinecrafts.LOGGER.info("HEAVEN_P2_POLICY pink_petals_can_survive_on_heaven_grass={}", petalsSurvive);
            for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1)))
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            ctx.succeed();
        });
    }

    @GameTest
    public void heavenP2SpeciesStructuresLoad(GameTestHelper ctx) {
        var manager = ctx.getLevel().getStructureTemplateManager();
        for (String name : new String[]{"giant_starlotus", "giant_lily", "giant_daffodil", "tree_blossom", "tree_white",
                "medium_lavender_0", "medium_lavender_1", "medium_hydrangea_0", "medium_hydrangea_1", "medium_phlox_0", "medium_phlox_1"}) {
            var t = manager.get(Identifier.parse("moreminecrafts:heaven_flora/" + name));
            ctx.assertTrue(t.isPresent() && t.get().getSize().getX() > 0, "structure " + name + " loads");
        }
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
