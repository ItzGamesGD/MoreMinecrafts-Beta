package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Heaven sky island P1 prototype in the real (server) game: registration, template loading through the
 * game's StructureTemplateManager, placement into a level and the placed result. Not a visual check.
 */
public final class HeavenPrototypeGameTests implements CustomTestMethodInvoker {
    private static final Identifier TEMPLATE = Identifier.parse("moreminecrafts:heaven_prototype/plain_medium_d_v5");
    private static final List<String> NATURAL = List.of("heaven_grass_block", "heaven_loam", "heaven_cream_stone", "heaven_cream_stone_pale");

    private static Block block(String path) {
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse("moreminecrafts:" + path));
        if (b == Blocks.AIR) throw new IllegalStateException("block not registered: " + path);
        return b;
    }

    @GameTest
    public void heavenPrototypeBlocksAreRegistered(GameTestHelper ctx) {
        for (String id : NATURAL) {
            Block b = block(id);
            ctx.assertTrue(b.asItem() != net.minecraft.world.item.Items.AIR, "item for " + id);
        }
        Block flora = block("heaven_flora_prototype");
        ctx.assertTrue(flora instanceof HeavenFloraPrototypeBlock, "flora prototype block class");
        ctx.assertTrue(flora.asItem() == net.minecraft.world.item.Items.AIR, "flora prototype parts have no item");
        ctx.assertTrue(flora.getStateDefinition().getPossibleStates().size() == HeavenFloraPrototypeBlock.PART_COUNT,
                "one state per baked part");
        ctx.succeed();
    }

    @GameTest
    public void heavenGrassDropsLoamWithoutSilkTouch(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos pos = ctx.absolutePos(new BlockPos(1, 1, 1));
        List<ItemStack> drops = Block.getDrops(block("heaven_grass_block").defaultBlockState(), level, pos, null);
        ctx.assertTrue(drops.size() == 1 && drops.getFirst().is(block("heaven_loam").asItem()), "PROVISIONAL rule: grass drops loam, got " + drops);
        ctx.succeed();
    }

    @GameTest(maxTicks = 200)
    public void heavenPrototypeTemplatePlacesAsDesigned(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        StructureTemplate template = level.getStructureTemplateManager().get(TEMPLATE)
                .orElseThrow(() -> new AssertionError("template did not load: " + TEMPLATE));
        var size = template.getSize();
        ctx.assertTrue(size.getX() == 45 && size.getY() == 43 && size.getZ() == 37, "template size " + size);
        // High above the test grid so neighbouring tests are not touched; cleared afterwards.
        BlockPos origin = ctx.absolutePos(BlockPos.ZERO).atY(200);
        boolean placed = template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
        ctx.assertTrue(placed, "placeInWorld");
        Map<Block, Integer> counts = new HashMap<>();
        int solidParts = 0, collidingStems = 0;
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            counts.merge(s.getBlock(), 1, Integer::sum);
            if (s.getBlock() instanceof HeavenFloraPrototypeBlock) {
                int part = s.getValue(HeavenFloraPrototypeBlock.PART);
                if (!HeavenFloraPrototypeBlock.collision(part).isEmpty()) {
                    solidParts++;
                    if (!s.getCollisionShape(level, p).isEmpty()) collidingStems++;
                } else {
                    ctx.assertTrue(s.getCollisionShape(level, p).isEmpty(), "passable part " + part + " has no collision");
                }
                ctx.assertTrue(!s.getShape(level, p).isEmpty(), "part " + part + " has an outline");
            }
        }
        MoreMinecrafts.LOGGER.info("HEAVEN_PROTOTYPE_PLACED counts={} solidParts={}", counts, solidParts);
        ctx.assertTrue(counts.getOrDefault(block("heaven_grass_block"), 0) == 807, "807 grass blocks, got " + counts.get(block("heaven_grass_block")));
        ctx.assertTrue(counts.getOrDefault(block("heaven_loam"), 0) == 1080 + 534, "loam = dirt + coarse dirt");
        ctx.assertTrue(counts.getOrDefault(block("heaven_cream_stone"), 0) == 1954, "cream stone = stone");
        ctx.assertTrue(counts.getOrDefault(block("heaven_cream_stone_pale"), 0) == 928 + 969, "pale cream = andesite + tuff");
        ctx.assertTrue(counts.getOrDefault(block("heaven_flora_prototype"), 0) == 1561, "1561 flora cells, got " + counts.get(block("heaven_flora_prototype")));
        ctx.assertTrue(solidParts > 0 && solidParts == collidingStems, "stems and trunks collide in the world");
        for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1)))
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
