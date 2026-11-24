package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * P3.10 before/after table (opt-in: {@code -Dmm.habitat.compare=true}; the legacy path takes minutes and blocks the server thread, so it is never part of the
 * regular suite). For the five large originals and five large heaven templates of meadow and forest: the FORMER first lookup (HashSet build + Set.copyOf + one
 * contains, the exact pre-P3.9 cost), 100k former repeat lookups and the heap it retained, against the dense mask's first call, 100k repeat lookups and bytes.
 */
public final class HabitatBeforeAfterGameTests implements CustomTestMethodInvoker {
    private static long usedBytes() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    @GameTest(maxTicks = 100)
    public void legacyVersusMaskOnLargeIslands(GameTestHelper ctx) {
        if (!Boolean.getBoolean("mm.habitat.compare")) { ctx.succeed(); return; }
        var level = ctx.getLevel(); var mgr = level.getStructureTemplateManager(); Map<String, Object> rows = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>();
        for (String t : List.of("meadow", "forest")) for (String n : List.of("large_a", "large_b", "large_c", "large_d", "large_e")) { ids.add("sky_islands/" + t + "/" + n); ids.add("sky_islands/heaven/" + t + "/" + n); }
        for (String id : ids) {
            var tpl = mgr.get(Identifier.parse("moreminecrafts:" + id)).orElseThrow();
            var size = tpl.getSize(); BlockPos[] probes = new BlockPos[1000];
            for (int i = 0; i < probes.length; i++) probes[i] = new BlockPos((i * 7) % size.getX(), (i * 13) % size.getY(), (i * 11) % size.getZ());
            System.gc(); long m0 = usedBytes();
            long a = System.nanoTime(); Set<BlockPos> legacy = Set.copyOf(HabitatMaskGameTests.reference(level, tpl)); boolean first = legacy.contains(probes[0]); long legacyFirstMs = (System.nanoTime() - a) / 1_000_000;
            System.gc(); long legacyBytes = usedBytes() - m0;
            a = System.nanoTime(); int hits = 0; for (int r = 0; r < 100; r++) for (BlockPos p : probes) if (legacy.contains(p)) hits++; long legacyRepeatUs = (System.nanoTime() - a) / 1000;
            legacy = null; System.gc();
            a = System.nanoTime(); var mask = SkyIslandHabitat.solids(level, tpl); boolean f2 = mask.contains(probes[0]); long maskFirstMs = (System.nanoTime() - a) / 1_000_000;
            a = System.nanoTime(); int h2 = 0; for (int r = 0; r < 100; r++) for (BlockPos p : probes) if (mask.contains(p)) h2++; long maskRepeatUs = (System.nanoTime() - a) / 1000;
            rows.put(id, "legacy_first_ms=" + legacyFirstMs + " legacy_100k_lookups_us=" + legacyRepeatUs + " legacy_heap_MB=" + (legacyBytes >> 20)
                    + " | mask_first_ms=" + maskFirstMs + " mask_100k_lookups_us=" + maskRepeatUs + " mask_KB=" + (mask.bytes() >> 10) + " | same_answers=" + (hits == h2 && first == f2));
            MoreMinecrafts.LOGGER.info("HEAVEN_P310_PERF {} {}", id, rows.get(id));
        }
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException { method.invoke(this, context); }
}
