package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

/**
 * P3.9 instrumentation (opt-in: {@code -Dmm.habitat.instrument=true}, otherwise a no-op): replays the phases of the ORIGINAL
 * {@code SkyIslandHabitat.solids()} on real templates and times each one, with GC and heap figures, so the bottleneck is measured
 * rather than assumed. Product code is not touched.
 */
public final class HabitatPerfInstrumentation implements CustomTestMethodInvoker {
    private static long gcMs() { long t = 0; for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) t += Math.max(0, b.getCollectionTime()); return t; }
    private static long usedMb() { Runtime r = Runtime.getRuntime(); return (r.totalMemory() - r.freeMemory()) >> 20; }

    @GameTest(maxTicks = 100)
    public void instrumentOriginalSolidsPhases(GameTestHelper ctx) {
        if (!Boolean.getBoolean("mm.habitat.instrument")) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel(); var mgr = level.getStructureTemplateManager();
        for (String id : List.of("sky_islands/meadow/plain_medium_d", "sky_islands/meadow/large_a", "sky_islands/forest/large_c", "sky_islands/heaven/forest/large_c")) {
            var t = mgr.get(Identifier.parse("moreminecrafts:" + id)).orElseThrow();
            System.gc(); long g0 = gcMs(), m0 = usedMb();
            long a = System.nanoTime(); CompoundTag tag = t.save(new CompoundTag()); long tSave = System.nanoTime() - a;
            var palette = tag.getListOrEmpty("palette"); boolean[] solid = new boolean[palette.size()]; boolean[] connector = new boolean[palette.size()];
            a = System.nanoTime();
            for (int i = 0; i < solid.length; i++) {
                var state = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), palette.getCompoundOrEmpty(i));
                solid[i] = state.isSolid(); connector[i] = state.is(Blocks.JIGSAW);
            }
            long tPalette = System.nanoTime() - a;
            a = System.nanoTime(); Set<BlockPos> result = new HashSet<>(); int blocks = 0;
            for (var entry : tag.getListOrEmpty("blocks")) {
                if (!(entry instanceof CompoundTag block)) continue; blocks++;
                int state = block.getIntOr("state", -1); var pos = block.getListOrEmpty("pos");
                if (state < 0 || state >= solid.length || pos.size() != 3 || connector[state]) continue;
                if (solid[state]) result.add(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)));
            }
            long tLoop = System.nanoTime() - a;
            // hash quality: distinct BlockPos hash codes vs elements
            Set<Integer> distinct = new HashSet<>(); for (BlockPos p : result) distinct.add(p.hashCode());
            a = System.nanoTime(); Set<BlockPos> copied = Set.copyOf(result); long tCopy = System.nanoTime() - a;
            // lookups on both structures
            a = System.nanoTime(); int hit = 0; for (BlockPos p : result) if (copied.contains(p)) hit++; long tLookupImmutable = System.nanoTime() - a;
            a = System.nanoTime(); for (BlockPos p : result) if (result.contains(p)) hit++; long tLookupHash = System.nanoTime() - a;
            MoreMinecrafts.LOGGER.info("HABITAT_INSTRUMENT {} size={} blocks={} solid={} distinctBlockPosHash={} save={}ms palette={}ms loop(HashSet.add)={}ms copyOf={}ms "
                    + "lookup(Set.copyOf)={}ms lookup(HashSet)={}ms gc={}ms heapDelta={}MB", id, t.getSize().toShortString(), blocks, result.size(), distinct.size(), tSave / 1_000_000, tPalette / 1_000_000,
                    tLoop / 1_000_000, tCopy / 1_000_000, tLookupImmutable / 1_000_000, tLookupHash / 1_000_000, gcMs() - g0, usedMb() - m0);
        }
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
