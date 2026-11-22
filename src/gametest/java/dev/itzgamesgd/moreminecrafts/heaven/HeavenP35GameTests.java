package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * P3.5 GameTests: giant-flower rotation (3 species x 4 rotations through real structure rotation), heaven-only jigsaw
 * pools assembled by the vanilla jigsaw placer (isolated test pools in the gametest namespace), G4 water and the G5
 * flammability scope. Results are logged as {@code HEAVEN_P35_TEST <name> <map>}.
 */
public final class HeavenP35GameTests implements CustomTestMethodInvoker {
    private static final String[] SPECIES = {"giant_starlotus", "giant_lily", "giant_daffodil"};
    /** Species-structure pivots (heaven/manifest_p2.json p2.structures.*.pivot). */
    private static final Map<String, BlockPos> PIVOT = Map.of("giant_starlotus", new BlockPos(3, 1, 3), "giant_lily", new BlockPos(3, 1, 2),
            "giant_daffodil", new BlockPos(4, 1, 4));
    private static final Rotation[] ROT = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
    private static final Direction[] FACING = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static void log(String name, Object o) { MoreMinecrafts.LOGGER.info("HEAVEN_P35_TEST {} {}", name, o); }

    private static Block b(String id) {
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id.contains(":") ? id : "moreminecrafts:" + id));
        if (block == Blocks.AIR) throw new IllegalStateException("not registered: " + id);
        return block;
    }

    private static String id(BlockState s) { return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(); }

    /** Places a species structure rotated about its own root (pivot), the way a rotated island carries it. */
    private static void placeSpecies(ServerLevel level, String species, BlockPos root, int r) {
        StructureTemplate t = level.getStructureTemplateManager().get(Identifier.parse("moreminecrafts:heaven_flora/" + species)).orElseThrow();
        BlockPos pivot = PIVOT.get(species);
        StructurePlaceSettings s = new StructurePlaceSettings().setRotation(ROT[r]).setRotationPivot(pivot);
        BlockPos origin = root.subtract(pivot);
        if (!t.placeInWorld(level, origin, origin, s, level.getRandom(), Block.UPDATE_CLIENTS)) throw new AssertionError("place " + species);
    }

    /**
     * Outline of a flower as 1/16-block voxels, rotated back by -r about the root block centre: equal for every r iff the
     * silhouette is preserved (voxels, not boxes: a rotated VoxelShape may split into different boxes).
     */
    private static Set<Long> silhouette(ServerLevel level, BlockPos root, List<BlockPos> cells, int r) {
        Set<Long> out = new HashSet<>();
        for (BlockPos p : cells) {
            BlockState s = level.getBlockState(p);
            for (AABB a : s.getShape(level, p).toAabbs()) {
                int x0 = (int) Math.round(a.minX * 16), x1 = (int) Math.round(a.maxX * 16), y0 = (int) Math.round(a.minY * 16), y1 = (int) Math.round(a.maxY * 16);
                int z0 = (int) Math.round(a.minZ * 16), z1 = (int) Math.round(a.maxZ * 16);
                for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) for (int z = z0; z < z1; z++) {
                    // voxel centre relative to the root block centre, in 1/32 units (exact integers)
                    long u = ((p.getX() - root.getX()) * 16L + x) * 2 + 1 - 16, w = ((p.getZ() - root.getZ()) * 16L + z) * 2 + 1 - 16;
                    for (int k = 0; k < r; k++) { long t = u; u = w; w = -t; }          // inverse of (x,z) -> (-z,x)
                    long v = (p.getY() - root.getY()) * 16L + y;
                    out.add(((u + 4096) << 40) | ((v + 4096) << 20) | (w + 4096));
                }
            }
        }
        return out;
    }

    @GameTest(maxTicks = 200)
    public void giantFlowersKeepShapeAndBreakInEveryRotation(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos base = ctx.absolutePos(new BlockPos(1, 1, 1)).atY(150);
        Map<String, Object> out = new LinkedHashMap<>();
        int failures = 0;
        for (int si = 0; si < 3; si++) {
            String sp = SPECIES[si];
            Set<Long> reference = null;
            int refTop = 0;
            for (int r = 0; r < 4; r++) {
                BlockPos root = base.offset(si * 32, 0, r * 16);
                for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {   // support floor two below the root (the structure has y = -1 cells)
                    level.setBlock(root.offset(x, -2, z), HeavenPrototypeBlocks.HEAVEN_GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                placeSpecies(level, sp, root, r);
                // every layout cell where the rotation puts it, with the rotated facing
                List<BlockPos> cells = new ArrayList<>();
                int wrong = 0, top = Integer.MIN_VALUE;
                for (HeavenPlants.Cell c : HeavenPlants.layout(sp).cells()) {
                    BlockPos p = root.offset(HeavenPlants.rotated(c, r));
                    BlockState s = level.getBlockState(p);
                    boolean ok = s.getBlock() instanceof HeavenPartBlock hp && hp.id().equals(c.block()) && s.getValue(hp.partProperty()) == c.part()
                            && s.getValue(BlockStateProperties.HORIZONTAL_FACING) == FACING[r];
                    if (!ok) wrong++;
                    cells.add(p); top = Math.max(top, p.getY());
                }
                Set<Long> sil = silhouette(level, root, cells, r);
                if (reference == null) { reference = sil; refTop = top; }
                boolean same = sil.equals(reference), height = top == refTop;
                // a second, unrotated starlotus touching the rotated flower must survive the break
                BlockPos nb = root.offset(7, 0, 0);
                List<BlockPos> neighbour = new ArrayList<>();
                placeSpecies(level, "giant_starlotus", nb, 0);
                Map<BlockPos, BlockState> before = new HashMap<>();
                for (HeavenPlants.Cell c : HeavenPlants.layout("giant_starlotus").cells()) {
                    BlockPos p = nb.offset(HeavenPlants.rotated(c, 0));
                    neighbour.add(p); before.put(p, level.getBlockState(p));
                }
                int hooks = HeavenPlants.dropHookCalls();
                BlockPos stem = cells.stream().filter(p -> p.getY() > root.getY() && level.getBlockState(p).is(b("heaven_giant_stem"))
                        && !before.containsKey(p)).findFirst().orElseThrow();
                level.destroyBlock(stem, true, ctx.makeMockPlayer(GameType.SURVIVAL));
                long left = cells.stream().filter(p -> !before.containsKey(p) && level.getBlockState(p).getBlock() instanceof HeavenPartBlock).count();
                long neighbourKept = neighbour.stream().filter(p -> level.getBlockState(p).equals(before.get(p))).count();
                int items = level.getEntitiesOfClass(ItemEntity.class, new AABB(root).inflate(12)).size();
                int dh = HeavenPlants.dropHookCalls() - hooks;
                String row = "cells=" + cells.size() + " wrong=" + wrong + " silhouette_equal=" + same + " top=" + (top - root.getY())
                        + " left_after_stem=" + left + " neighbour_kept=" + neighbourKept + "/" + neighbour.size() + " drop_hooks=" + dh + " items=" + items
                        + " removed=" + HeavenPlants.lastRemoval().removed() + " voxels=" + sil.size();
                out.put(sp + ".r" + (90 * r), row);
                if (wrong != 0 || !same || !height || left != 0 || neighbourKept != neighbour.size() || dh != 1 || items != 0) failures++;
            }
        }
        log("giant_rotation", out);
        ctx.assertTrue(failures == 0, "3 species x 4 rotations keep shape, height and break rules: " + out);
        ctx.succeed();
    }

    // ------------------------------------------------------------------ heaven-only jigsaw assembly

    private record Scan(Map<String, Integer> counts) {
        int n(String id) { return counts.getOrDefault(id, 0); }
    }

    private static Scan scan(ServerLevel level, BlockPos c, int r, int down, int up) {
        Map<String, Integer> m = new TreeMap<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -down, -r), c.offset(r, up, r))) {
            BlockState s = level.getBlockState(p);
            if (!s.isAir()) m.merge(id(s), 1, Integer::sum);
        }
        return new Scan(m);
    }

    private static boolean assemble(ServerLevel level, String pool, String target, BlockPos at, int depth) {
        var reg = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var holder = reg.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, Identifier.parse(pool)));
        return JigsawPlacement.generateJigsaw(level, holder, Identifier.parse(target), depth, at, false);
    }

    private static final List<String> VANILLA_TERRAIN = List.of("minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:stone",
            "minecraft:tuff", "minecraft:short_grass", "minecraft:oak_leaves");

    @GameTest(maxTicks = 400)
    public void heavenPoolsAssembleOnlyHeavenPiecesAndKeepFunctionalStructures(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos base = ctx.absolutePos(BlockPos.ZERO).atY(150).offset(0, 0, 4000);   // far from every other test
        Map<String, Object> out = new LinkedHashMap<>();
        // 1) large_d + Sky Tower island through the heaven tower_top pool
        BlockPos a = base;
        boolean okA = assemble(level, "moreminecrafts-gametest:heaven_link/meadow/test/large_d", "moreminecrafts:sky_top", a, 3);
        Scan sa = scan(level, a, 110, 30, 110);
        out.put("large_d+tower", "placed=" + okA + " vanilla_terrain=" + VANILLA_TERRAIN.stream().mapToInt(sa::n).sum() + " andesite=" + sa.n("minecraft:andesite")
                + " vault=" + sa.n("minecraft:vault") + " trial_spawner=" + sa.n("minecraft:trial_spawner") + " jigsaw=" + sa.n("minecraft:jigsaw")
                + " heaven_grass=" + sa.n("moreminecrafts:heaven_grass_block") + " giant_stems=" + sa.n("moreminecrafts:heaven_giant_stem"));
        ctx.assertTrue(okA && VANILLA_TERRAIN.stream().mapToInt(sa::n).sum() == 0 && sa.n("minecraft:andesite") == 3312 && sa.n("minecraft:vault") == 8
                && sa.n("minecraft:trial_spawner") == 16 && sa.n("minecraft:jigsaw") == 0, "heaven large_d + heaven Sky Tower: " + out);
        // 2) medium island, satellites through the heaven satellite pool (random: several tries)
        int satellites = 0, vanilla = 0, jig = 0, tries = 0;
        for (int i = 0; i < 6; i++) {
            BlockPos m = base.offset(260 + i * 130, 0, 0);
            if (!assemble(level, "moreminecrafts-gametest:heaven_link/meadow/test/plain_medium_a", "moreminecrafts:sky_link", m, 3)) continue;
            tries++;
            Scan sm = scan(level, m, 64, 30, 40);
            int grass = sm.n("moreminecrafts:heaven_grass_block");
            if (grass > 802) satellites++;                     // plain_medium_a alone has 802 heaven grass
            vanilla += VANILLA_TERRAIN.stream().mapToInt(sm::n).sum(); jig += sm.n("minecraft:jigsaw");
        }
        out.put("medium+satellites", "assembled=" + tries + " with_heaven_satellites=" + satellites + " vanilla_terrain=" + vanilla + " jigsaw=" + jig);
        ctx.assertTrue(tries > 0 && satellites > 0 && vanilla == 0 && jig == 0, "heaven satellites only: " + out);
        // 3) contrast: the P3 template as-is still points at the vanilla meadow satellite pool
        int mixed = 0;
        for (int i = 0; i < 6; i++) {
            BlockPos m = base.offset(260 + i * 130, 0, 400);
            if (!assemble(level, "moreminecrafts-gametest:heaven_link/meadow/test/unlinked_plain_medium_a", "moreminecrafts:sky_link", m, 3)) continue;
            mixed += scan(level, m, 64, 30, 40).n("minecraft:grass_block");
        }
        out.put("unlinked_contrast_vanilla_grass", mixed);
        // P3.12: the approved transition repoints the live meadow satellite pool itself to the heaven templates, so even the unlinked P3 template (which still names
        // moreminecrafts:sky_islands/meadow/satellite) now links heaven satellites. Before the transition this contrast was > 0 (the problem G7 solved in the test twin).
        ctx.assertTrue(mixed == 0, "after the production transition the unlinked P3 template no longer mixes in vanilla satellites: " + out);
        // 4) ruins and temple keep their shared functional buildings
        BlockPos ru = base.offset(0, 0, 400);
        boolean okR = assemble(level, "moreminecrafts-gametest:heaven_link/meadow/test/ruins", "moreminecrafts:sky_top", ru, 3);
        Scan sr = scan(level, ru, 64, 30, 40);
        BlockPos te = base.offset(0, 0, 800);
        boolean okT = assemble(level, "moreminecrafts-gametest:heaven_link/meadow/test/temple", "moreminecrafts:sky_top", te, 3);
        Scan st = scan(level, te, 80, 30, 50);
        out.put("ruins", "placed=" + okR + " stone_bricks=" + (sr.n("minecraft:stone_bricks") + sr.n("minecraft:mossy_stone_bricks") + sr.n("minecraft:cracked_stone_bricks"))
                + " chest=" + sr.n("minecraft:chest") + " vanilla_terrain=" + VANILLA_TERRAIN.stream().mapToInt(sr::n).sum() + " jigsaw=" + sr.n("minecraft:jigsaw"));
        out.put("temple", "placed=" + okT + " smooth_quartz=" + st.n("minecraft:smooth_quartz") + " vanilla_terrain=" + VANILLA_TERRAIN.stream().mapToInt(st::n).sum()
                + " jigsaw=" + st.n("minecraft:jigsaw"));
        log("jigsaw_pools", out);
        ctx.assertTrue(okR && sr.n("minecraft:chest") > 0 && VANILLA_TERRAIN.stream().mapToInt(sr::n).sum() == 0 && sr.n("minecraft:jigsaw") == 0, "ruins: " + out);
        ctx.assertTrue(okT && st.n("minecraft:smooth_quartz") > 0 && VANILLA_TERRAIN.stream().mapToInt(st::n).sum() == 0 && st.n("minecraft:jigsaw") == 0, "temple: " + out);
        ctx.succeed();
    }

    // ------------------------------------------------------------------ G4 water, G5 flammability scope

    @GameTest(maxTicks = 200)
    public void g4SmallPlantsWashAwayAndG5FlammabilityScope(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos base = ctx.absolutePos(new BlockPos(1, 1, 1)).atY(190);
        Map<String, Object> out = new LinkedHashMap<>();
        String[] plants = {"minecraft:dandelion", "moreminecrafts:heaven_flower_red", "moreminecrafts:heaven_short_grass", "moreminecrafts:heaven_tall_grass",
                "moreminecrafts:heaven_petals"};
        HeavenP3PolicyGameTests.forceChunks(level, base.offset(-11, 0, -11), base.offset(11, 0, plants.length * 24 + 11), true);   // real fluid ticks
        List<BlockPos> plantAt = new ArrayList<>();
        for (int i = 0; i < plants.length; i++) {
            BlockPos g = base.offset(0, 0, i * 24);
            // a closed 21 x 21 floor: no drop within 4 blocks, so the source spreads evenly (into the plant) instead of towards a hole
            for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
                level.setBlock(g.offset(x, 0, z), HeavenPrototypeBlocks.HEAVEN_GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(g.offset(x, 1, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            BlockPos plant = g.above();
            level.setBlock(plant, b(plants[i]).defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(plant.west(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            plantAt.add(plant);
            out.put(plants[i] + ".can_be_replaced_by_water", b(plants[i]).defaultBlockState().canBeReplaced(Fluids.WATER));
        }
        // G5: heaven vegetation incl. white blossom leaves not flammable and not lit by lava; reused vanilla log / cherry leaves keep vanilla values
        var reg = FlammableBlockRegistry.getDefaultInstance();
        BlockState white = HeavenP2Blocks.HEAVEN_WHITE_BLOSSOM_LEAVES.defaultBlockState();
        out.put("white_leaves.fire", reg.get(white.getBlock()).getIgniteOdds() + "/" + reg.get(white.getBlock()).getBurnOdds());
        out.put("white_leaves.ignitedByLava", white.ignitedByLava());
        out.put("cherry_leaves.fire", reg.get(Blocks.CHERRY_LEAVES).getIgniteOdds() + "/" + reg.get(Blocks.CHERRY_LEAVES).getBurnOdds());
        out.put("cherry_leaves.ignitedByLava", Blocks.CHERRY_LEAVES.defaultBlockState().ignitedByLava());
        out.put("oak_log.fire", reg.get(Blocks.OAK_LOG).getIgniteOdds() + "/" + reg.get(Blocks.OAK_LOG).getBurnOdds());
        // white leaves otherwise behave as the cherry leaves they were copied from
        BlockState cherry = Blocks.CHERRY_LEAVES.defaultBlockState();
        BlockPos q = base.offset(10, 0, 0);
        out.put("white_leaves.same_as_cherry", white.getDestroySpeed(level, q) == cherry.getDestroySpeed(level, q) && white.isRandomlyTicking() == cherry.isRandomlyTicking()
                && white.getSoundType() == cherry.getSoundType() && white.getPistonPushReaction() == cherry.getPistonPushReaction()
                && white.isValidSpawn(level, q, EntityTypes.PARROT) && !white.isValidSpawn(level, q, EntityTypes.ZOMBIE)
                && white.getBlock().getExplosionResistance() == cherry.getBlock().getExplosionResistance());
        int heavenFlammable = 0;
        for (Block blk : HeavenP2Blocks.partBlocks()) {
            var e = reg.get(blk);
            if (e.getBurnOdds() > 0 || e.getIgniteOdds() > 0 || blk.defaultBlockState().ignitedByLava()) heavenFlammable++;
        }
        out.put("heaven_part_families_flammable_or_lava_lit", heavenFlammable);
        final int heavenFlammableFinal = heavenFlammable;
        ctx.runAfterDelay(40, () -> {
            for (int i = 0; i < plants.length; i++) out.put(plants[i] + ".after_water", id(level.getBlockState(plantAt.get(i))));
            log("g4_g5", out);
            for (int i = 0; i < plants.length; i++)
                ctx.assertTrue(out.get(plants[i] + ".after_water").equals("minecraft:water"), plants[i] + " is washed away like a vanilla flower " + out);
            ctx.assertTrue(out.get("white_leaves.fire").equals("0/0") && !(Boolean) out.get("white_leaves.ignitedByLava"), "white leaves never burn, lava never lights them " + out);
            ctx.assertTrue(!out.get("cherry_leaves.fire").equals("0/0") && (Boolean) out.get("cherry_leaves.ignitedByLava") && !out.get("oak_log.fire").equals("0/0"),
                    "reused vanilla cherry leaves / oak log keep vanilla flammability " + out);
            ctx.assertTrue((Boolean) out.get("white_leaves.same_as_cherry"), "white leaves keep the cherry leaves behaviour " + out);
            ctx.assertTrue(heavenFlammableFinal == 0, "heaven part families are not flammable " + out);
            HeavenP3PolicyGameTests.forceChunks(level, base.offset(-11, 0, -11), base.offset(11, 0, plants.length * 24 + 11), false);
            ctx.succeed();
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
