package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * P3.8: the production heaven templates of meadow and forest (38) are assembled through the vanilla jigsaw placer with the isolated
 * test twin of the option-D pools (gametest namespace; the live pool IDs are untouched), placed chunk by chunk exactly like world
 * generation ({@code StructureStart.placeInChunk} per chunk), checked for heaven-only content and preserved functions, compared
 * with a whole-box placement (chunk boundaries) and round-tripped through the structure-start save tag (saved piece locations).
 */
public final class HeavenP38GameTests implements CustomTestMethodInvoker {
    private static final String NS = "moreminecrafts-gametest";
    private static final List<String> FORBIDDEN = List.of("grass_block", "dirt", "coarse_dirt", "podzol", "stone", "short_grass", "tall_grass", "oak_leaves",
            "birch_leaves", "fern", "poppy", "dandelion", "azure_bluet", "allium", "cornflower", "oxeye_daisy", "birch_log");
    private static final BlockPos SITE = new BlockPos(0, 150, 12000);

    record Assembly(StructureStart start, BoundingBox box, long ms) {}
    record Scan(Map<String, Integer> counts, long hash) { int n(String id) { return counts.getOrDefault(id, 0); } }

    private static void log(String name, Object o) { MoreMinecrafts.LOGGER.info("HEAVEN_P38_TEST {} {}", name, o); }

    static StructureStart makeStart(ServerLevel level, String poolId, String theme, BlockPos at) {
        var pools = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var holder = pools.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, Identifier.parse(poolId)));
        var gen = level.getChunkSource().getGenerator(); var rs = level.getChunkSource().randomState();
        var ctx = new Structure.GenerationContext(level.registryAccess(), gen, gen.getBiomeSource(), (net.minecraft.world.level.biome.Climate.Sampler) null, rs, level.getStructureTemplateManager(),
                level.getSeed(), ChunkPos.containing(at), level, b -> true);
        var stub = JigsawPlacement.addPieces(ctx, holder, Optional.empty(), 3, at, false, Optional.empty(), new JigsawStructure.MaxDistance(200),
                PoolAliasLookup.EMPTY, DimensionPadding.ZERO, LiquidSettings.APPLY_WATERLOGGING).orElseThrow(() -> new AssertionError("no pieces for " + poolId));
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Identifier.parse("moreminecrafts:sky_island_" + theme));
        return new StructureStart(structure, ChunkPos.containing(at), 0, stub.getPiecesBuilder().build());
    }

    static Assembly assemble(ServerLevel level, String theme, String name, BlockPos at) {
        long t0 = System.nanoTime();
        StructureStart start = makeStart(level, NS + ":heaven_d/" + theme + "/single/" + name, theme, at);
        placeChunkwise(level, start);
        return new Assembly(start, start.getBoundingBox(), (System.nanoTime() - t0) / 1_000_000);
    }

    /** What world generation does: every chunk of the start's box places the pieces with that chunk's box. */
    static void placeChunkwise(ServerLevel level, StructureStart start) { placeChunks(level, start, c -> true); }

    static void placeChunks(ServerLevel level, StructureStart start, java.util.function.Predicate<ChunkPos> which) {
        BoundingBox b = start.getBoundingBox();
        for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
            if (!which.test(new ChunkPos(cx, cz))) continue;
            BoundingBox chunk = new BoundingBox(cx << 4, level.getMinY(), cz << 4, (cx << 4) + 15, level.getMaxY(), (cz << 4) + 15);
            start.placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunk, new ChunkPos(cx, cz));
        }
    }

    static Scan scan(ServerLevel level, BoundingBox box) {
        Map<String, Integer> m = new TreeMap<>(); long h = 1125899906842597L;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            m.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath(), 1, Integer::sum);
            h = 31 * h + p.asLong() * 17L + s.hashCode();
        }
        return new Scan(m, h);
    }

    static void clear(ServerLevel level, BoundingBox box) {
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
    }

    /** Heaven vegetation blocks of one template, from its own palette. */
    private static int templateVegetation(ServerLevel level, String id) {
        CompoundTag tag = level.getStructureTemplateManager().get(Identifier.parse(id)).orElseThrow().save(new CompoundTag());
        var pal = tag.getListOrEmpty("palette"); var blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        boolean[] veg = new boolean[pal.size()];
        for (int i = 0; i < veg.length; i++) veg[i] = NbtUtils.readBlockState(blocks, pal.getCompoundOrEmpty(i)).getBlock() instanceof HeavenPartBlock;
        int n = 0;
        for (var e : tag.getListOrEmpty("blocks")) { int s = ((CompoundTag) e).getIntOr("state", -1); if (s >= 0 && s < veg.length && veg[s]) n++; }
        return n;
    }

    private static void runGroup(GameTestHelper ctx, String theme, List<String> names) {
        ServerLevel level = ctx.getLevel();
        forceSite(level, true);
        Map<String, Object> out = new LinkedHashMap<>(); int bad = 0;
        for (String name : names) {
            Assembly a = assemble(level, theme, name, SITE);
            Scan s = scan(level, a.box());
            int vanilla = 0; List<String> found = new ArrayList<>();
            for (String f : FORBIDDEN) if (s.n(f) > 0) { vanilla += s.n(f); found.add(f + "=" + s.n(f)); }
            boolean tower = name.equals("large_d") || name.equals("large_e");
            int ownVeg = templateVegetation(level, "moreminecrafts:sky_islands/heaven/" + theme + "/" + name);
            int partBlocks = 0;
            for (var e : s.counts().entrySet()) if (e.getKey().startsWith("heaven_") && !e.getKey().startsWith("heaven_grass") && !e.getKey().startsWith("heaven_loam")
                    && !e.getKey().startsWith("heaven_cream") && !e.getKey().equals("heaven_white_blossom_leaves")) partBlocks += e.getValue();
            List<String> pieceIds = new ArrayList<>(); boolean vanillaPiece = false;
            for (var piece : a.start().getPieces()) if (piece instanceof PoolElementStructurePiece p && p.getElement() instanceof SinglePoolElement single) {
                String loc = String.valueOf(single.getTemplateLocation()); pieceIds.add(loc.replace("moreminecrafts:", "mm:").replace("moreminecrafts-gametest:", "gt:"));
                if (loc.matches("moreminecrafts:sky_islands/(meadow|forest)/.*")) vanillaPiece = true;
            }
            boolean ok = s.n("jigsaw") == 0 && vanilla == 0 && !vanillaPiece && s.n("heaven_grass_block") > 0 && partBlocks >= ownVeg * 0.99
                    && s.n("andesite") == (tower ? 3312 : 0) && (!tower || (s.n("vault") == 8 && s.n("trial_spawner") == 16))
                    && (!name.equals("ruins") || s.n("chest") >= 2) && (!name.equals("temple") || s.n("smooth_quartz") > 0);
            if (!ok) bad++;
            out.put(theme + "/" + name, "ok=" + ok + " pieces=" + pieceIds + " box=" + a.box().getXSpan() + "x" + a.box().getYSpan() + "x" + a.box().getZSpan()
                    + " jigsaw_left=" + s.n("jigsaw") + " vanilla_found=" + found + " heaven_parts=" + partBlocks + "/own " + ownVeg + " andesite=" + s.n("andesite")
                    + " vault=" + s.n("vault") + " trial=" + s.n("trial_spawner") + " chest=" + s.n("chest") + " ms=" + a.ms());
            clear(level, a.box());
        }
        forceSite(level, false);
        log("assemble_" + theme + "_" + names.get(0), out);
        ctx.assertTrue(bad == 0, theme + " islands assemble heaven-only with functions preserved: " + out);
        ctx.succeed();
    }

    private static void forceSite(ServerLevel level, boolean on) {
        for (int cx = -20; cx <= 20; cx++) for (int cz = (SITE.getZ() >> 4) - 20; cz <= (SITE.getZ() >> 4) + 20; cz++) level.setChunkForced(cx, cz, on);
    }

    private static final List<String> SMALL_GROUP = List.of("plain_small_a", "plain_small_b", "plain_small_c", "plain_small_d", "plain_medium_a", "plain_medium_b", "plain_medium_c",
            "plain_medium_d", "plain_medium_e", "satellite_a", "satellite_b", "ruins", "temple");
    private static final List<String> LARGE_GROUP = List.of("large_a", "large_b", "large_c", "large_d", "large_e");

    @GameTest(maxTicks = 100) public void meadowSmallMediumRuinsTemple(GameTestHelper ctx) { runGroup(ctx, "meadow", SMALL_GROUP); }
    @GameTest(maxTicks = 100) public void forestSmallMediumRuinsTemple(GameTestHelper ctx) { runGroup(ctx, "forest", SMALL_GROUP); }
    @GameTest(maxTicks = 100) public void meadowLargeWithSkyTower(GameTestHelper ctx) { runGroup(ctx, "meadow", LARGE_GROUP); }
    @GameTest(maxTicks = 100) public void forestLargeWithSkyTower(GameTestHelper ctx) { runGroup(ctx, "forest", LARGE_GROUP); }

    @GameTest(maxTicks = 100)
    public void chunkBoundariesAndSavedStartsAreConsistent(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel(); forceSite(level, true);
        Map<String, Object> out = new LinkedHashMap<>(); int bad = 0;
        for (String theme : new String[]{"meadow", "forest"}) for (String name : new String[]{"plain_medium_a", "large_d", "temple"}) {
            Assembly a = assemble(level, theme, name, SITE.offset(5, 0, 7));      // odd offset: islands straddle chunk borders
            Scan chunkwise = scan(level, a.box());
            clear(level, a.box());
            // whole-box placement of the same pieces (no chunk borders involved)
            a.start().placeInChunk(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), a.box(), a.start().getChunkPos());
            Scan whole = scan(level, a.box());
            clear(level, a.box());
            // save tag round trip, then place the LOADED start chunk by chunk
            StructurePieceSerializationContext sctx = StructurePieceSerializationContext.fromLevel(level);
            CompoundTag tag = a.start().createTag(sctx, a.start().getChunkPos());
            StructureStart loaded = StructureStart.loadStaticStart(sctx, tag, level.getSeed());
            String saved = tag.toString();
            boolean locationsSaved = saved.contains("heaven_d/" + theme) || saved.contains("sky_islands/heaven/" + theme);
            boolean noVanillaTheme = !saved.contains("sky_islands/" + theme + "/");
            placeChunkwise(level, loaded);
            Scan reloaded = scan(level, loaded.getBoundingBox());
            clear(level, a.box());
            boolean ok = chunkwise.hash() == whole.hash() && chunkwise.hash() == reloaded.hash() && locationsSaved && noVanillaTheme
                    && loaded.getPieces().size() == a.start().getPieces().size() && loaded.getBoundingBox().equals(a.box());
            if (!ok) bad++;
            out.put(theme + "/" + name, "ok=" + ok + " chunks=" + (((a.box().maxX() >> 4) - (a.box().minX() >> 4) + 1) * ((a.box().maxZ() >> 4) - (a.box().minZ() >> 4) + 1))
                    + " hash_equal(chunkwise,whole,reloaded)=" + (chunkwise.hash() == whole.hash()) + "," + (chunkwise.hash() == reloaded.hash())
                    + " pieces=" + a.start().getPieces().size() + "/" + loaded.getPieces().size() + " saved_heaven_locations=" + locationsSaved + " saved_vanilla_theme_locations=" + !noVanillaTheme);
        }
        forceSite(level, false);
        log("chunk_boundaries_saved_starts", out);
        ctx.assertTrue(bad == 0, "chunk-wise, whole-box and reloaded-start placements are identical; saved pieces point at heaven templates: " + out);
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
