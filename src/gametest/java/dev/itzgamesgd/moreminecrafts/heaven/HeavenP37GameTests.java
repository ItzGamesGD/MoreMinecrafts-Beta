package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** P3.7: the four forest comparison variants (isolated gametest templates) place cleanly; placement time and block counts are logged. */
public final class HeavenP37GameTests implements CustomTestMethodInvoker {
    static final String[] VARIANTS = {"a_v5", "a_forest", "b_v5", "b_forest"};
    static final String[] NAMES = {"large_a", "plain_medium_d", "ruins", "temple"};

    @GameTest(maxTicks = 300)
    public void forestComparisonVariantsPlaceCleanly(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos start = ctx.absolutePos(BlockPos.ZERO).atY(205).offset(0, 0, 6000);
        Map<String, Object> out = new LinkedHashMap<>();
        int x = 0, bad = 0;
        for (String name : NAMES) for (String v : VARIANTS) {
            StructureTemplate t = level.getStructureTemplateManager().get(Identifier.parse("moreminecrafts-gametest:heaven_forest_cmp/" + v + "/" + name))
                    .orElseThrow(() -> new AssertionError("missing " + v + "/" + name));
            BlockPos o = start.offset(x, 0, 0); x += t.getSize().getX() + 8;
            long best = Long.MAX_VALUE;
            for (int rep = 0; rep < 3; rep++) {                      // best of 3 (JIT warm-up)
                long t0 = System.nanoTime();
                if (!t.placeInWorld(level, o, o, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS)) throw new AssertionError("place");
                best = Math.min(best, System.nanoTime() - t0);
            }
            int blocks = 0, veg = 0, trees = 0, vanillaTerrain = 0;
            for (BlockPos p : BlockPos.betweenClosed(o, o.offset(t.getSize().getX() - 1, t.getSize().getY() - 1, t.getSize().getZ() - 1))) {
                BlockState s = level.getBlockState(p);
                if (s.isAir()) continue;
                blocks++;
                if (s.getBlock() instanceof HeavenPartBlock || BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().endsWith("_leaves")) veg++;
                if (BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().equals("oak_log")) trees++;
                String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
                if (id.equals("grass_block") || id.equals("dirt") || id.equals("podzol") || id.equals("mossy_cobblestone")) vanillaTerrain++;
            }
            out.put(name + "/" + v, "blocks=" + blocks + " vegetation=" + veg + " log_blocks=" + trees + " vanilla_terrain=" + vanillaTerrain + " place_ms=" + best / 1_000_000.0);
            boolean paletteB = v.startsWith("b");
            if (!name.equals("ruins") && !name.equals("temple") && (paletteB ? vanillaTerrain == 0 : vanillaTerrain != 0)) bad++;   // A: no vanilla terrain; B keeps podzol / mossy
            if (name.equals("ruins") || name.equals("temple")) { if (!paletteB && vanillaTerrain != 0) bad++; }
        }
        MoreMinecrafts.LOGGER.info("HEAVEN_P37_TEST forest_variants {}", out);
        ctx.assertTrue(bad == 0, "palette A leaves no vanilla terrain, palette B keeps podzol / mossy cobblestone: " + out);
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
