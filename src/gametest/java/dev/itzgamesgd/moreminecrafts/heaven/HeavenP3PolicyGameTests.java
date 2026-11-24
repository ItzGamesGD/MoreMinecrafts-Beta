package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * P3-D: one GameTest per approved policy (F1-F4, F6, F8, F11) on the real server, plus fire, collision / spawn surface
 * and the F7 duplicate-drop guard (independent of any drop amount). Every result is also logged as
 * {@code HEAVEN_P3_TEST <name> <json-ish map>} for the P3 report.
 */
public final class HeavenP3PolicyGameTests implements CustomTestMethodInvoker {
    private static Block b(String id) {
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id.contains(":") ? id : "moreminecrafts:" + id));
        if (block == Blocks.AIR) throw new IllegalStateException("not registered: " + id);
        return block;
    }

    private static String id(BlockState s) { return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(); }

    private static void log(String name, Object result) { MoreMinecrafts.LOGGER.info("HEAVEN_P3_TEST {} {}", name, result); }

    private static final BlockState GRASS = HeavenPrototypeBlocks.HEAVEN_GRASS_BLOCK.defaultBlockState();
    private static final BlockState LOAM = HeavenPrototypeBlocks.HEAVEN_LOAM.defaultBlockState();

    /** A w x w heaven-grass floor (loam under it) with air above, starting at {@code corner}. */
    private static void floor(ServerLevel level, BlockPos corner, int w, int air) {
        for (int x = 0; x < w; x++) for (int z = 0; z < w; z++) {
            BlockPos p = corner.offset(x, 0, z);
            level.setBlock(p.below(), LOAM, Block.UPDATE_CLIENTS);
            level.setBlock(p, GRASS, Block.UPDATE_CLIENTS);
            for (int y = 1; y <= air; y++) level.setBlock(p.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Places one plant from its P2 layout like the P3 generator does: cells below the ground row (y &lt; 0) are skipped. */
    static List<BlockPos> plant(ServerLevel level, BlockPos root, String layout, int rotation) {
        List<BlockPos> out = new ArrayList<>();
        Direction facing = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}[rotation];
        for (HeavenPlants.Cell c : HeavenPlants.layout(layout).cells()) {
            if (c.dy() < 0) continue;
            HeavenPartBlock blk = (HeavenPartBlock) BuiltInRegistries.BLOCK.getValue(c.block());
            BlockState s = blk.defaultBlockState();
            if (blk.partProperty() != null) s = s.setValue(blk.partProperty(), c.part());
            if (blk.hasFacing()) s = s.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            BlockPos p = root.offset(HeavenPlants.rotated(c, rotation));
            level.setBlock(p, s, Block.UPDATE_CLIENTS);
            out.add(p);
        }
        return out;
    }

    /** Block ticks only run in ticking chunks; force-load the area a tick-dependent test uses (released on success). */
    static void forceChunks(ServerLevel level, BlockPos from, BlockPos to, boolean on) {
        for (int cx = Math.min(from.getX(), to.getX()) >> 4; cx <= Math.max(from.getX(), to.getX()) >> 4; cx++)
            for (int cz = Math.min(from.getZ(), to.getZ()) >> 4; cz <= Math.max(from.getZ(), to.getZ()) >> 4; cz++) level.setChunkForced(cx, cz, on);
    }

    private static int countItems(ServerLevel level, BlockPos center, int r) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(r)).size();
    }

    private static BlockPos high(GameTestHelper ctx, int y) { return ctx.absolutePos(new BlockPos(1, 1, 1)).atY(y); }

    // ------------------------------------------------------------------ F1 spread / decay

    @GameTest
    public void f1GrassSpreadsOnlyOntoHeavenLoamAndDecaysToIt(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos g = high(ctx, 144);
        Map<String, String> out = new LinkedHashMap<>();
        // spread: grass with heaven loam east and vanilla dirt west, daylight, many random ticks
        level.setBlock(g, GRASS, Block.UPDATE_ALL);
        level.setBlock(g.east(), LOAM, Block.UPDATE_ALL);
        level.setBlock(g.west(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos p : new BlockPos[]{g, g.east(), g.west()}) for (int y = 1; y < 4; y++) level.setBlock(p.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(g.above(2), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);   // light 9+ even under another test's island
        for (int i = 0; i < 600; i++) level.getBlockState(g).randomTick(level, g, level.getRandom());
        out.put("loam_neighbour", id(level.getBlockState(g.east())));
        out.put("dirt_neighbour", id(level.getBlockState(g.west())));
        // decay: covered by an opaque block
        BlockPos d = g.south(3);
        level.setBlock(d, GRASS, Block.UPDATE_ALL);
        level.setBlock(d.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 600 && level.getBlockState(d).is(GRASS.getBlock()); i++) level.getBlockState(d).randomTick(level, d, level.getRandom());
        out.put("covered", id(level.getBlockState(d)));
        // snowy state exists (inherited) but the block keeps its single model
        level.setBlock(d.above(), Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(d, GRASS, Block.UPDATE_ALL);
        level.setBlock(d.above(), Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
        out.put("snowy_under_snow", String.valueOf(level.getBlockState(d).getValue(BlockStateProperties.SNOWY)));
        log("f1_spread_decay", out);
        ctx.assertTrue(out.get("loam_neighbour").equals("moreminecrafts:heaven_grass_block"), "heaven grass spreads onto heaven loam: " + out);
        ctx.assertTrue(out.get("dirt_neighbour").equals("minecraft:dirt"), "heaven grass never spreads onto vanilla dirt: " + out);
        ctx.assertTrue(out.get("covered").equals("moreminecrafts:heaven_loam"), "covered heaven grass decays to heaven loam: " + out);
        ctx.succeed();
    }

    // ------------------------------------------------------------------ F3 shovel / hoe, farmland reversion

    private static String use(GameTestHelper ctx, BlockPos pos, net.minecraft.world.item.Item tool) {
        var player = ctx.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(tool);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false)));
        return id(ctx.getLevel().getBlockState(pos));
    }

    @GameTest
    public void f3ShovelPathAndHoeFarmland(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos p = high(ctx, 150);
        Map<String, String> out = new LinkedHashMap<>();
        for (BlockState s : new BlockState[]{GRASS, LOAM}) {
            String k = id(s).substring(15);
            for (var tool : new net.minecraft.world.item.Item[]{Items.IRON_SHOVEL, Items.IRON_HOE}) {
                level.setBlock(p, s, Block.UPDATE_ALL);
                level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                out.put(k + (tool == Items.IRON_SHOVEL ? ".shovel" : ".hoe"), use(ctx, p, tool));
            }
        }
        // what tilled heaven ground turns back into (vanilla farmland: trampling / drying -> its base block)
        level.setBlock(p, GRASS, Block.UPDATE_ALL);
        use(ctx, p, Items.IRON_HOE);
        BlockState farmland = level.getBlockState(p);
        ((FarmlandBlock) Blocks.FARMLAND).turnToBaseBlock(null, farmland, level, p);
        out.put("tilled_heaven_grass.trampled", id(level.getBlockState(p)));
        level.setBlock(p, GRASS, Block.UPDATE_ALL);
        use(ctx, p, Items.IRON_HOE);
        for (int i = 0; i < 200 && level.getBlockState(p).is(Blocks.FARMLAND); i++) level.getBlockState(p).randomTick(level, p, level.getRandom());
        out.put("tilled_heaven_grass.dried_without_water", id(level.getBlockState(p)));
        log("f3_tools", out);
        ctx.assertTrue(out.get("heaven_grass_block.shovel").equals("minecraft:dirt_path"), "shovel: heaven grass -> vanilla dirt path " + out);
        ctx.assertTrue(out.get("heaven_grass_block.hoe").equals("minecraft:farmland"), "hoe: heaven grass -> vanilla farmland " + out);
        ctx.assertTrue(out.get("heaven_loam.hoe").equals("minecraft:farmland"), "hoe: heaven loam -> vanilla farmland " + out);
        ctx.assertTrue(out.get("heaven_loam.shovel").equals("minecraft:dirt_path"), "shovel: heaven loam -> vanilla dirt path (A3) " + out);
        ctx.succeed();
    }

    // ------------------------------------------------------------------ F4 bone meal

    @GameTest
    public void f4BoneMealGrowsHeavenVegetationOnly(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos corner = high(ctx, 154);
        floor(level, corner, 9, 3);
        BlockPos center = corner.offset(4, 0, 4);
        boolean applied = false;
        for (int i = 0; i < 4; i++) applied |= BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, center);
        Map<String, Integer> grown = new HashMap<>();
        for (BlockPos q : BlockPos.betweenClosed(corner.above(), corner.offset(8, 2, 8))) {
            BlockState s = level.getBlockState(q);
            if (!s.isAir()) grown.merge(id(s), 1, Integer::sum);
        }
        log("f4_bonemeal", Map.of("applied", applied, "grown", grown));
        ctx.assertTrue(applied && !grown.isEmpty(), "bone meal on heaven grass grows vegetation " + grown);
        ctx.assertTrue(grown.keySet().stream().allMatch(k -> k.equals("moreminecrafts:heaven_short_grass") || k.equals("moreminecrafts:heaven_tall_grass")
                || k.startsWith("moreminecrafts:heaven_flower_")), "only heaven vegetation is grown " + grown);
        ctx.succeed();
    }

    // ------------------------------------------------------------------ F2 vanilla plants

    @GameTest
    public void f2VanillaPlantSurvivalAndGrowth(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos base = high(ctx, 160);
        Map<String, Object> out = new LinkedHashMap<>();
        String[] tagOnly = {"dandelion", "poppy", "short_grass", "fern", "oak_sapling", "cherry_sapling", "pink_petals", "sweet_berry_bush", "azalea", "dead_bush"};
        for (BlockState ground : new BlockState[]{GRASS, LOAM, HeavenPrototypeBlocks.HEAVEN_CREAM_STONE.defaultBlockState()}) {
            level.setBlock(base, ground, Block.UPDATE_ALL);
            Map<String, Boolean> row = new LinkedHashMap<>();
            for (String plant : tagOnly) row.put(plant, b("minecraft:" + plant).defaultBlockState().canSurvive(level, base.above()));
            row.put("wheat", Blocks.WHEAT.defaultBlockState().canSurvive(level, base.above()));
            row.put("sugar_cane_dry", Blocks.SUGAR_CANE.defaultBlockState().canSurvive(level, base.above()));
            level.setBlock(base.east(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            row.put("sugar_cane_by_water", Blocks.SUGAR_CANE.defaultBlockState().canSurvive(level, base.above()));
            level.setBlock(base.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            row.put("bamboo", Blocks.BAMBOO_SAPLING.defaultBlockState().canSurvive(level, base.above()));
            out.put(id(ground).substring(15), row);
        }
        // crops need vanilla farmland (made with a hoe from heaven grass); light-dependent checks run after the light engine
        level.setBlock(base, GRASS, Block.UPDATE_ALL);
        level.setBlock(base.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.above().north(), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);   // crops need light 8+
        use(ctx, base, Items.IRON_HOE);
        // mushrooms: vanilla light rule (dark + solid) unchanged; one bright cell, one enclosed cell
        BlockPos m = base.south(3), md = base.south(6);
        level.setBlock(m, GRASS, Block.UPDATE_ALL);
        level.setBlock(m.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(m.above().north(), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(md, GRASS, Block.UPDATE_ALL);
        level.setBlock(md.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (Direction d : Direction.Plane.HORIZONTAL) level.setBlock(md.above().relative(d), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(md.above(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        // tree growth: oak sapling on heaven grass, the block under the trunk must stay heaven grass
        BlockPos t = base.west(8).atY(162);
        floor(level, t.offset(-3, 0, -3), 7, 10);
        level.setBlock(t.above(), Blocks.OAK_SAPLING.defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 40 && level.getBlockState(t.above()).is(Blocks.OAK_SAPLING); i++)
            ((SaplingBlock) Blocks.OAK_SAPLING).advanceTree(level, t.above(), level.getBlockState(t.above()), level.getRandom());
        out.put("oak_grown", id(level.getBlockState(t.above())));
        out.put("under_trunk", id(level.getBlockState(t)));
        ctx.runAfterDelay(20, () -> {
            out.put("wheat_on_tilled_heaven_grass", Blocks.WHEAT.defaultBlockState().canSurvive(level, base.above()));
            out.put("brown_mushroom_bright", Blocks.BROWN_MUSHROOM.defaultBlockState().canSurvive(level, m.above()));
            out.put("brown_mushroom_dark", Blocks.BROWN_MUSHROOM.defaultBlockState().canSurvive(level, md.above()));
            log("f2_vanilla_plants", out);
            @SuppressWarnings("unchecked") Map<String, Boolean> grass = (Map<String, Boolean>) out.get("heaven_grass_block");
            @SuppressWarnings("unchecked") Map<String, Boolean> loam = (Map<String, Boolean>) out.get("heaven_loam");
            @SuppressWarnings("unchecked") Map<String, Boolean> stone = (Map<String, Boolean>) out.get("heaven_cream_stone");
            for (String plant : tagOnly) {
                ctx.assertTrue(grass.get(plant) && loam.get(plant), plant + " survives on heaven grass and loam " + out);
                ctx.assertFalse(stone.get(plant), plant + " does not survive on heaven stone");
            }
            ctx.assertFalse(grass.get("wheat"), "crops still need farmland");
            ctx.assertTrue((Boolean) out.get("wheat_on_tilled_heaven_grass"), "wheat on farmland tilled from heaven grass " + out);
            ctx.assertFalse(grass.get("sugar_cane_dry"), "sugar cane still needs water");
            ctx.assertTrue(grass.get("sugar_cane_by_water") && loam.get("sugar_cane_by_water"), "sugar cane next to water on heaven ground");
            ctx.assertFalse((Boolean) out.get("brown_mushroom_bright"), "mushrooms keep the vanilla light rule (bright) " + out);
            ctx.assertTrue((Boolean) out.get("brown_mushroom_dark"), "mushrooms grow on heaven grass in the dark (vanilla rule) " + out);
            ctx.assertTrue(out.get("oak_grown").equals("minecraft:oak_log"), "oak grows on heaven grass " + out);
            ctx.assertTrue(out.get("under_trunk").equals("moreminecrafts:heaven_grass_block"), "the trunk does not turn heaven grass into vanilla dirt " + out);
            ctx.succeed();
        });
    }

    // ------------------------------------------------------------------ F8 single-cell heaven vegetation

    @GameTest
    public void f8HeavenVegetationNeedsSupport(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos g = high(ctx, 176);
        forceChunks(level, g.offset(-2, 0, -2), g.offset(4, 0, 12), true);
        Map<String, Object> out = new LinkedHashMap<>();
        for (String f : new String[]{"heaven_flower_red", "heaven_short_grass", "heaven_tall_grass", "heaven_petals"}) {
            BlockState s = b(f).defaultBlockState();
            level.setBlock(g, GRASS, Block.UPDATE_ALL);
            out.put(f + ".on_heaven_grass", s.canSurvive(level, g.above()));
            level.setBlock(g, LOAM, Block.UPDATE_ALL);
            out.put(f + ".on_heaven_loam", s.canSurvive(level, g.above()));
            level.setBlock(g, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            out.put(f + ".on_vanilla_grass", s.canSurvive(level, g.above()));
            level.setBlock(g, HeavenPrototypeBlocks.HEAVEN_CREAM_STONE.defaultBlockState(), Block.UPDATE_ALL);
            out.put(f + ".on_heaven_stone", s.canSurvive(level, g.above()));
            level.setBlock(g, GRASS, Block.UPDATE_ALL);
            level.setBlock(g.above(), s, Block.UPDATE_ALL);
            level.setBlock(g, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            out.put(f + ".after_support_removed", id(level.getBlockState(g.above())));
            out.put(f + ".items_dropped", countItems(level, g, 3));
            level.setBlock(g.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        // vine: needs the face behind it (facing) or the same vine above
        BlockPos v = g.south(3);
        level.setBlock(v.north(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        BlockState vine = b("heaven_vine").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        out.put("vine.on_cliff", vine.canSurvive(level, v));
        level.setBlock(v, vine, Block.UPDATE_ALL);
        level.setBlock(v.below(), vine, Block.UPDATE_ALL);
        level.setBlock(v.below().north(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        out.put("vine.hanging_from_vine", id(level.getBlockState(v.below())));
        level.setBlock(v.north(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // wisteria: hangs from leaves
        BlockPos w = g.south(6);
        level.setBlock(w.above(), Blocks.CHERRY_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true), Block.UPDATE_ALL);
        level.setBlock(w, b("heaven_wisteria").defaultBlockState(), Block.UPDATE_ALL);
        out.put("wisteria.under_leaves", id(level.getBlockState(w)));
        level.setBlock(w.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        BlockState wis = b("heaven_wisteria").defaultBlockState();
        BlockPos w2 = w.east(2);
        level.setBlock(w2.above(), HeavenP2Blocks.HEAVEN_WHITE_BLOSSOM_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true), Block.UPDATE_ALL);
        out.put("wisteria.survives_under_white_blossom", wis.canSurvive(level, w2));
        out.put("wisteria.survives_under_air", wis.canSurvive(level, w.west(2)));
        // water: flowing water onto a small flower
        BlockPos f = g.south(9);
        level.setBlock(f, GRASS, Block.UPDATE_ALL);
        level.setBlock(f.above(), b("heaven_flower_blue").defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(f.above().east(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        ctx.runAfterDelay(20, () -> {
            out.put("flower.after_water_flow", id(level.getBlockState(f.above())));
            out.put("vine.after_cliff_removed", id(level.getBlockState(v)) + "," + id(level.getBlockState(v.below())));
            out.put("wisteria.after_leaves_removed", id(level.getBlockState(w)));
            log("f8_support", out);
            for (String k : new String[]{"heaven_flower_red", "heaven_short_grass", "heaven_tall_grass", "heaven_petals"}) {
                ctx.assertTrue((Boolean) out.get(k + ".on_heaven_grass") && (Boolean) out.get(k + ".on_heaven_loam"), k + " on heaven ground " + out);
                ctx.assertFalse((Boolean) out.get(k + ".on_heaven_stone"), k + " not on heaven stone");
                ctx.assertTrue(out.get(k + ".after_support_removed").equals("minecraft:air"), k + " is removed with its support");
            }
            ctx.assertTrue((Boolean) out.get("vine.on_cliff"), "vine on a cliff face");
            ctx.assertTrue(out.get("vine.after_cliff_removed").equals("minecraft:air,minecraft:air"), "vines go with their cliff (next ticks) " + out);
            ctx.assertTrue(out.get("wisteria.under_leaves").equals("moreminecrafts:heaven_wisteria"), "wisteria hangs from leaves");
            ctx.assertTrue(out.get("wisteria.after_leaves_removed").equals("minecraft:air"), "wisteria goes with its canopy (next tick) " + out);
            ctx.assertTrue((Boolean) out.get("wisteria.survives_under_white_blossom"), "wisteria hangs from white blossom leaves " + out);
            ctx.assertFalse((Boolean) out.get("wisteria.survives_under_air"), "wisteria never floats " + out);
            forceChunks(level, g.offset(-2, 0, -2), g.offset(4, 0, 12), false);
            ctx.succeed();
        });
    }

    // ------------------------------------------------------------------ F6 giant flowers, F7 duplicate-drop guard

    @GameTest(maxTicks = 100)
    public void f6StemRemovesOnlyItsOwnFlower(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos corner = high(ctx, 118);
        // put a chunk border through the starlotus root (chunk-boundary case)
        int dx = Math.floorMod(-(corner.getX() + 6), 16);
        corner = corner.offset(dx, 0, 0);
        floor(level, corner, 20, 14);
        BlockPos rootA = corner.offset(6, 1, 6), rootB = corner.offset(12, 1, 6);    // starlotus and lily touch / overlap
        List<BlockPos> a = plant(level, rootA, "giant_starlotus", 0);
        List<BlockPos> bCells = plant(level, rootB, "giant_lily", 0);
        Map<BlockPos, BlockState> lily = new HashMap<>();
        for (BlockPos p : bCells) lily.put(p.immutable(), level.getBlockState(p));
        int aPresent = (int) a.stream().filter(p -> !lily.containsKey(p)).count();
        boolean crossesChunk = a.stream().map(p -> p.getX() >> 4).distinct().count() > 1;
        var player = ctx.makeMockPlayer(GameType.SURVIVAL);
        // a leaf alone: only that block
        BlockPos leaf = a.stream().filter(p -> level.getBlockState(p).is(b("heaven_giant_leaf")) && !lily.containsKey(p)).findFirst().orElseThrow();
        level.destroyBlock(leaf, true, player);
        int afterLeaf = (int) a.stream().filter(p -> level.getBlockState(p).getBlock() instanceof HeavenPartBlock && !lily.containsKey(p)).count();
        int hooks = HeavenPlants.dropHookCalls();
        BlockPos stem = a.stream().filter(p -> !lily.containsKey(p) && p.getY() > rootA.getY() && level.getBlockState(p).is(b("heaven_giant_stem")))
                .findFirst().orElseThrow(() -> new AssertionError("no starlotus stem above the root"));
        level.destroyBlock(stem, true, player);
        HeavenPlants.Removal r = HeavenPlants.lastRemoval();
        int aLeft = (int) a.stream().filter(p -> !lily.containsKey(p) && level.getBlockState(p).getBlock() instanceof HeavenPartBlock).count();
        int lilyKept = (int) lily.entrySet().stream().filter(e -> level.getBlockState(e.getKey()).equals(e.getValue())).count();
        int floorKept = 0;
        for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) if (level.getBlockState(corner.offset(x, 0, z)).is(GRASS.getBlock())) floorKept++;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("starlotus_cells", aPresent); out.put("after_leaf_break", afterLeaf); out.put("starlotus_left", aLeft);
        out.put("lily_cells", lily.size()); out.put("lily_kept", lilyKept); out.put("floor_kept", floorKept + "/400");
        out.put("removed", r.removed()); out.put("unloaded_skipped", r.skippedUnloaded()); out.put("micros", r.nanos() / 1000);
        out.put("drop_hook_calls", HeavenPlants.dropHookCalls() - hooks); out.put("items", countItems(level, rootA, 12));
        out.put("crosses_chunk_border", crossesChunk);
        log("f6_giant_cascade", out);
        ctx.assertTrue(afterLeaf == aPresent - 1, "breaking a leaf removes only that leaf " + out);
        ctx.assertTrue(aLeft == 0, "the stem removes every cell of its flower " + out);
        ctx.assertTrue(lilyKept == lily.size(), "the touching lily is untouched " + out);
        ctx.assertTrue(floorKept == 400, "terrain is untouched " + out);
        ctx.assertTrue(HeavenPlants.dropHookCalls() - hooks == 1, "one drop hook call per flower, not per block " + out);
        ctx.assertTrue(countItems(level, rootA, 12) == 0, "no item drops while F7 is pending " + out);
        ctx.assertTrue(crossesChunk, "case covers a chunk border");
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void f6EveryGiantSpeciesAndRemovalCause(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos corner = high(ctx, 100);
        forceChunks(level, corner, corner.offset(40, 0, 40), true);
        floor(level, corner, 40, 14);
        Map<String, Object> out = new LinkedHashMap<>();
        String[] species = {"giant_starlotus", "giant_lily", "giant_daffodil"};
        List<List<BlockPos>> plants = new ArrayList<>();
        for (int i = 0; i < 3; i++) plants.add(plant(level, corner.offset(6 + 12 * i, 1, 8), species[i], 0));
        for (int i = 0; i < 3; i++) plants.add(plant(level, corner.offset(6 + 12 * i, 1, 26), species[i], 0));
        // row 1: /setblock-like replacement of a stem; row 2: the root support removed (F8 for giants)
        for (int i = 0; i < 3; i++) {
            List<BlockPos> cells = plants.get(i);
            BlockPos stem = cells.stream().filter(p -> level.getBlockState(p).is(b("heaven_giant_stem"))).findFirst().orElseThrow();
            level.setBlock(stem, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            out.put(species[i] + ".stem_replaced.left", cells.stream().filter(p -> level.getBlockState(p).getBlock() instanceof HeavenPartBlock).count());
            out.put(species[i] + ".removed", HeavenPlants.lastRemoval().removed());
            out.put(species[i] + ".micros", HeavenPlants.lastRemoval().nanos() / 1000);
        }
        for (int i = 0; i < 3; i++) {
            BlockPos root = corner.offset(6 + 12 * i, 1, 26);
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) level.setBlock(root.offset(x, -1, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        ctx.runAfterDelay(5, () -> {
            for (int i = 0; i < 3; i++) {
                List<BlockPos> cells = plants.get(3 + i);
                out.put(species[i] + ".support_removed.left", cells.stream().filter(p -> level.getBlockState(p).getBlock() instanceof HeavenPartBlock).count());
            }
            log("f6_species", out);
            for (String s : species) {
                ctx.assertTrue(((Long) out.get(s + ".stem_replaced.left")) == 0, s + " removed when a stem goes " + out);
                ctx.assertTrue(((Long) out.get(s + ".support_removed.left")) == 0, s + " removed when its ground goes " + out);
            }
            forceChunks(level, corner, corner.offset(40, 0, 40), false);
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void f8MediumFlowerIsOnePlant(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos corner = high(ctx, 136);
        floor(level, corner, 24, 5);
        Map<String, Object> out = new LinkedHashMap<>();
        String[] ids = {"medium_lavender_0", "medium_hydrangea_1", "medium_phlox_0"};
        for (int i = 0; i < 3; i++) {
            for (int r = 0; r < 4; r++) {
                BlockPos root = corner.offset(2 + 6 * i, 1, 2 + 5 * r);
                List<BlockPos> cells = plant(level, root, ids[i], r);
                BlockPos top = cells.stream().max(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
                level.destroyBlock(top, true, ctx.makeMockPlayer(GameType.SURVIVAL));
                long left = cells.stream().filter(p -> level.getBlockState(p).getBlock() instanceof HeavenPartBlock).count();
                out.put(ids[i] + ".r" + r + ".cells", cells.size());
                out.put(ids[i] + ".r" + r + ".left_after_break", left);
            }
        }
        log("f8_medium", out);
        for (var e : out.entrySet()) if (e.getKey().endsWith("left_after_break")) ctx.assertTrue((Long) e.getValue() == 0, e.getKey() + " " + out);
        ctx.succeed();
    }

    // ------------------------------------------------------------------ F11, fire, collision / spawn surface

    @GameTest
    public void f11StoneDropsItselfFireAndSpawnSurfaces(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos p = high(ctx, 184);
        Map<String, Object> out = new LinkedHashMap<>();
        for (BlockState s : new BlockState[]{HeavenPrototypeBlocks.HEAVEN_CREAM_STONE.defaultBlockState(), HeavenPrototypeBlocks.HEAVEN_CREAM_STONE_PALE.defaultBlockState()}) {
            out.put(id(s) + ".drops", Block.getDrops(s, level, p, null, null, new ItemStack(Items.IRON_PICKAXE)).stream()
                    .map(st -> BuiltInRegistries.ITEM.getKey(st.getItem()).toString()).toList());
            out.put(id(s) + ".drops_by_hand", Block.getDrops(s, level, p, null, null, ItemStack.EMPTY).size());
        }
        // fire: no heaven vegetation is in the flammable registry; a fire next to them burns nothing
        List<String> flammable = new ArrayList<>();
        List<Block> veg = new ArrayList<>(HeavenP2Blocks.partBlocks());
        veg.add(HeavenP2Blocks.HEAVEN_WHITE_BLOSSOM_LEAVES);
        for (Block blk : veg) {
            var e = FlammableBlockRegistry.getDefaultInstance().get(blk);       // the registry answers 0/0 for unregistered blocks
            if (e != null && (e.getBurnOdds() > 0 || e.getIgniteOdds() > 0)) flammable.add(id(blk.defaultBlockState()) + "=" + e.getIgniteOdds() + "/" + e.getBurnOdds());
        }
        out.put("flammable_heaven_vegetation", flammable);
        BlockPos f = p.south(3);
        level.setBlock(f, Blocks.NETHERRACK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(f.east(), HeavenP2Blocks.HEAVEN_WHITE_BLOSSOM_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true), Block.UPDATE_ALL);
        level.setBlock(f.west(), b("heaven_giant_stem").defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(f.above(), Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 300; i++) if (level.getBlockState(f.above()).is(Blocks.FIRE)) level.getBlockState(f.above()).tick(level, f.above(), level.getRandom());
        out.put("after_fire.white_leaves", id(level.getBlockState(f.east())));
        out.put("after_fire.giant_stem", id(level.getBlockState(f.west())));
        out.put("white_leaves.ignitedByLava", HeavenP2Blocks.HEAVEN_WHITE_BLOSSOM_LEAVES.defaultBlockState().ignitedByLava());
        // collision and spawn surfaces (Sky mobs use ON_GROUND placement; the block below must accept the spawn)
        Map<String, String> spawn = new LinkedHashMap<>();
        BlockPos s0 = p.south(6);
        for (BlockState ground : new BlockState[]{GRASS, b("heaven_giant_stem").defaultBlockState(), b("heaven_giant_bloom").defaultBlockState(),
                b("heaven_giant_leaf").defaultBlockState(), HeavenP2Blocks.HEAVEN_WHITE_BLOSSOM_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true)}) {
            level.setBlock(s0, ground, Block.UPDATE_CLIENTS);
            level.setBlock(s0.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(s0.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            boolean slime = SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, s0.above(), SkyNormalMobs.CLOUD_SLIME);
            boolean zombie = SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, s0.above(), EntityTypes.ZOMBIE);
            spawn.put(id(ground), "solid=" + ground.isSolid() + ",collision=" + !ground.getCollisionShape(level, s0).isEmpty()
                    + ",cloud_slime=" + slime + ",zombie=" + zombie);
        }
        out.put("spawn_surface", spawn);
        log("f11_fire_spawn", out);
        ctx.assertTrue(out.get("moreminecrafts:heaven_cream_stone.drops").equals(List.of("moreminecrafts:heaven_cream_stone")), "cream stone drops itself " + out);
        ctx.assertTrue(out.get("moreminecrafts:heaven_cream_stone_pale.drops").equals(List.of("moreminecrafts:heaven_cream_stone_pale")), "pale stone drops itself " + out);
        ctx.assertTrue(flammable.isEmpty(), "heaven vegetation is not flammable " + flammable);
        ctx.assertTrue(out.get("after_fire.white_leaves").equals("moreminecrafts:heaven_white_blossom_leaves")
                && out.get("after_fire.giant_stem").equals("moreminecrafts:heaven_giant_stem"), "fire burns no heaven vegetation " + out);
        ctx.assertTrue(spawn.get("moreminecrafts:heaven_grass_block").contains("cloud_slime=true"), "heaven grass stays a spawn surface " + spawn);
        for (String k : new String[]{"moreminecrafts:heaven_giant_stem", "moreminecrafts:heaven_giant_bloom", "moreminecrafts:heaven_giant_leaf", "moreminecrafts:heaven_white_blossom_leaves"})
            ctx.assertTrue(spawn.get(k).contains("cloud_slime=false") && spawn.get(k).contains("zombie=false"), k + " is never a spawn surface " + spawn);
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
