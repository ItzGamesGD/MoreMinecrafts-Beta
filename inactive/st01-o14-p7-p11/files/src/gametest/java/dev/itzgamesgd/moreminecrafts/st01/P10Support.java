package dev.itzgamesgd.moreminecrafts.st01;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/** Shared helpers of the ST-01 P10 game tests: tick scheduler, fixture slots, CSV output (only when -Dmoreminecrafts.p10.out is set). */
final class P10Support {
    private P10Support() {}

    /** Own tick scheduler (nesting ctx.onEachTick / runAfterDelay inside their own callbacks crashes the 26.3 test ticker). */
    static final class Sched {
        int tick;
        private final List<Runnable> each = new ArrayList<>();
        private final List<Runnable> actions = new ArrayList<>();
        private final List<Integer> due = new ArrayList<>();
        Sched(GameTestHelper ctx) { ctx.onEachTick(this::run); }
        private void run() {
            tick++;
            for (int i = 0; i < each.size(); i++) each.get(i).run();
            for (int i = 0; i < actions.size(); ) {
                if (due.get(i) <= tick) { Runnable r = actions.remove(i); due.remove(i); r.run(); } else i++;
            }
        }
        void each(Runnable r) { each.add(r); }
        void after(int ticks, Runnable r) { due.add(tick + ticks); actions.add(r); }
    }

    /**
     * Heavy measurement mode (-Dmoreminecrafts.p10.measure=true): the full sample sizes and the sweeps used for the P10 reports. Without it the same tests run
     * with small sample sizes as regression checks (a few minutes less, far less heap); the extra sweeps/load tests are inert.
     */
    static boolean measure() { return Boolean.getBoolean("moreminecrafts.p10.measure"); }

    private static final AtomicInteger NEXT_SLOT = new AtomicInteger();

    /** Far-away origin for the next fixture: model (0,0,0) -> world. Model Y -31..-4 lands on world Y 119..146. */
    static BlockPos nextOrigin(GameTestHelper ctx) {
        int slot = NEXT_SLOT.getAndIncrement();
        BlockPos base = ctx.absolutePos(BlockPos.ZERO);
        return new BlockPos(base.getX() + 20480 + (slot % 12) * 192, 150, base.getZ() + 20480 + (slot / 12) * 192);
    }

    static ServerLevel level(GameTestHelper ctx) { return ctx.getLevel(); }

    /** Output directory for measurement CSVs, or null when measurement output is not requested. */
    static Path outDir() {
        String p = System.getProperty("moreminecrafts.p10.out");
        return p == null || p.isBlank() ? null : Path.of(p);
    }

    static synchronized void csv(String file, String header, List<String> rows) {
        Path dir = outDir();
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            Path f = dir.resolve(file);
            boolean fresh = !Files.exists(f);
            List<String> lines = new ArrayList<>();
            if (fresh) lines.add(header);
            lines.addAll(rows);
            Files.write(f, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
}
