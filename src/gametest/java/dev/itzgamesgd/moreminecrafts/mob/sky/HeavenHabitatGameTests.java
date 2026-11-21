package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * P3.8: every grass surface block of the original meadow / forest island is still a Sky habitat support cell in the heaven template at
 * the production path (template path prefix {@code sky_islands/}, solid palette), so the unchanged Sky Mob spawn rules keep working.
 */
public final class HeavenHabitatGameTests implements CustomTestMethodInvoker {
    private static final List<String> NAMES = List.of("large_a", "large_b", "large_c", "large_d", "large_e", "plain_medium_a", "plain_medium_b", "plain_medium_c", "plain_medium_d",
            "plain_medium_e", "plain_small_a", "plain_small_b", "plain_small_c", "plain_small_d", "ruins", "satellite_a", "satellite_b", "temple", "tower");

    /**
     * All templates except large_a/b/c/e: the first {@code SkyIslandHabitat} support query per large template builds its solid set on the
     * server thread and takes 7-75 s (measured for the ORIGINAL templates as well; see the P3.8 report), which would dominate the suite.
     * large_d (Sky Tower island) stands for the large family; the one-off P3.8 run covered all 19 forest templates.
     */
    private static final List<String> CHECKED = NAMES.stream().filter(n -> !List.of("large_a", "large_b", "large_c", "large_e").contains(n)).toList();

    private static List<BlockPos> grass(ServerLevel level, StructureTemplate t) {
        CompoundTag tag = t.save(new CompoundTag());
        var pal = tag.getListOrEmpty("palette"); var reg = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        boolean[] grass = new boolean[pal.size()];
        for (int i = 0; i < grass.length; i++) {
            var s = NbtUtils.readBlockState(reg, pal.getCompoundOrEmpty(i));
            grass[i] = s.is(Blocks.GRASS_BLOCK) || reg.getKey(s.getBlock()).getPath().equals("heaven_grass_block");
        }
        List<BlockPos> out = new ArrayList<>();
        for (var e : tag.getListOrEmpty("blocks")) {
            CompoundTag b = (CompoundTag) e; int s = b.getIntOr("state", -1); var p = b.getListOrEmpty("pos");
            if (s >= 0 && s < grass.length && grass[s]) out.add(new BlockPos(p.getIntOr(0, 0), p.getIntOr(1, 0), p.getIntOr(2, 0)));
        }
        return out;
    }

    @GameTest(maxTicks = 100)
    public void heavenTemplatesKeepTheSkyHabitatSurface(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel(); var mgr = level.getStructureTemplateManager();
        Map<String, Object> out = new LinkedHashMap<>(); int bad = 0;
        for (String theme : new String[]{"meadow", "forest"}) for (String name : CHECKED) {
            StructureTemplate orig = mgr.get(Identifier.parse("moreminecrafts:sky_islands/" + theme + "/" + name)).orElseThrow();
            StructureTemplate heaven = mgr.get(Identifier.parse("moreminecrafts:sky_islands/heaven/" + theme + "/" + name)).orElseThrow();
            List<BlockPos> cells = grass(level, orig);
            int origOk = 0, heavenOk = 0;
            long t0 = System.nanoTime(); SkyIslandHabitat.originalSupport(level, orig, cells.get(0)); long tOrig = (System.nanoTime() - t0) / 1_000_000;
            t0 = System.nanoTime(); SkyIslandHabitat.originalSupport(level, heaven, cells.get(0)); long tHeaven = (System.nanoTime() - t0) / 1_000_000;
            MoreMinecrafts.LOGGER.info("HEAVEN_P38_TEST habitat_first_call {}/{} original={}ms heaven={}ms", theme, name, tOrig, tHeaven);
            for (BlockPos p : cells) { if (SkyIslandHabitat.originalSupport(level, orig, p)) origOk++; if (SkyIslandHabitat.originalSupport(level, heaven, p)) heavenOk++; }
            if (origOk != cells.size() || heavenOk != cells.size()) bad++;
            out.put(theme + "/" + name, "grass_cells=" + cells.size() + " original=" + origOk + " heaven=" + heavenOk + " first_call_ms=" + tOrig + "/" + tHeaven);
        }
        MoreMinecrafts.LOGGER.info("HEAVEN_P38_TEST habitat {}", out);
        ctx.assertTrue(bad == 0, "every original grass surface cell is still habitat support in the heaven template: " + out);
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
