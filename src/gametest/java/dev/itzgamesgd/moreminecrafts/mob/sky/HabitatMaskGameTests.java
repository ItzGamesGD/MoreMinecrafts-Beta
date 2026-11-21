package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
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
 * P3.9: the dense solid mask of {@link SkyIslandHabitat} is membership-identical to the former algorithm (a plain {@code HashSet} of
 * the generated-solid positions, reference implementation kept here) on every sky-island template: the 5 + 5 + 19 themed island sets
 * (all 6 themes x 19 = 114 originals), the shared buildings, and the 38 heaven templates. Reports first-call time, repeat lookup time
 * and mask size per template.
 */
public final class HabitatMaskGameTests implements CustomTestMethodInvoker {
    private static final List<String> THEMES = List.of("meadow", "forest", "snowy", "desert", "badlands", "rocky");
    private static final List<String> NAMES = List.of("large_a", "large_b", "large_c", "large_d", "large_e", "plain_medium_a", "plain_medium_b", "plain_medium_c", "plain_medium_d",
            "plain_medium_e", "plain_small_a", "plain_small_b", "plain_small_c", "plain_small_d", "ruins", "satellite_a", "satellite_b", "temple", "tower");

    /** The pre-P3.9 algorithm, membership only (a HashSet; the immutable copy was only a performance wrapper). */
    static Set<BlockPos> reference(ServerLevel level, StructureTemplate key) {
        CompoundTag tag = key.save(new CompoundTag());
        var palette = tag.getListOrEmpty("palette");
        boolean[] solid = new boolean[palette.size()], connector = new boolean[palette.size()];
        for (int i = 0; i < solid.length; i++) {
            var state = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), palette.getCompoundOrEmpty(i));
            solid[i] = state.isSolid(); connector[i] = state.is(Blocks.JIGSAW);
        }
        Set<BlockPos> result = new HashSet<>();
        for (var entry : tag.getListOrEmpty("blocks")) {
            if (!(entry instanceof CompoundTag block)) continue;
            int state = block.getIntOr("state", -1); var pos = block.getListOrEmpty("pos");
            if (state < 0 || state >= solid.length || pos.size() != 3) continue;
            boolean generatedSolid = solid[state];
            if (connector[state]) {
                try {
                    generatedSolid = BlockStateParser.parseForBlock(level.registryAccess().lookupOrThrow(Registries.BLOCK),
                            block.getCompoundOrEmpty("nbt").getStringOr("final_state", "minecraft:air"), false).blockState().isSolid();
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException invalid) { generatedSolid = false; }
            }
            if (generatedSolid) result.add(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)));
        }
        return result;
    }

    @GameTest(maxTicks = 100)
    public void solidMaskEqualsTheFormerSetOnEveryTemplate(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel(); var mgr = level.getStructureTemplateManager();
        List<String> ids = new java.util.ArrayList<>();
        for (String t : THEMES) for (String n : NAMES) ids.add("sky_islands/" + t + "/" + n);
        for (String t : List.of("meadow", "forest")) for (String n : NAMES) ids.add("sky_islands/heaven/" + t + "/" + n);
        ids.addAll(List.of("sky_islands/ruins_a", "sky_islands/ruins_b", "sky_islands/ruins_c", "sky_islands/ruins_d", "sky_islands/temple", "sky_islands/tower_5", "sky_islands/tower_6", "sky_islands/tower_7"));
        Map<String, Object> rows = new LinkedHashMap<>(); int bad = 0; long maxFirst = 0, totalBytes = 0, totalSolids = 0;
        for (String id : ids) {
            StructureTemplate t = mgr.get(Identifier.parse("moreminecrafts:" + id)).orElseThrow(() -> new AssertionError("missing " + id));
            Set<BlockPos> ref = reference(level, t);
            long t0 = System.nanoTime(); var mask = SkyIslandHabitat.solids(level, t); long first = (System.nanoTime() - t0) / 1_000_000;
            var size = t.getSize(); int diff = 0;
            for (int x = -1; x <= size.getX(); x++) for (int y = -1; y <= size.getY(); y++) for (int z = -1; z <= size.getZ(); z++) {
                BlockPos p = new BlockPos(x, y, z);
                if (mask.contains(p) != ref.contains(p)) diff++;
            }
            for (BlockPos p : new BlockPos[]{new BlockPos(-100000, 5, 5), new BlockPos(5, 100000, 5), new BlockPos(5, 5, -100000)}) if (mask.contains(p)) diff++;
            long r0 = System.nanoTime(); int hits = 0;
            for (int i = 0; i < 200_000; i++) if (SkyIslandHabitat.originalSupport(level, t, new BlockPos(i % size.getX(), (i / 7) % size.getY(), (i / 3) % size.getZ()))) hits++;
            long repeatMicros = (System.nanoTime() - r0) / 1000;
            boolean ok = diff == 0 && mask.cardinality() == ref.size();
            if (!ok) bad++;
            maxFirst = Math.max(maxFirst, first); totalBytes += mask.bytes(); totalSolids += ref.size();
            rows.put(id, "ok=" + ok + " solids=" + ref.size() + " first_call_ms=" + first + " lookups_200k_us=" + repeatMicros + " mask_bytes=" + mask.bytes());
        }
        MoreMinecrafts.LOGGER.info("HEAVEN_P39_TEST habitat_mask templates={} bad={} max_first_call_ms={} total_mask_bytes={} total_solids={} {}", ids.size(), bad, maxFirst, totalBytes, totalSolids, rows);
        ctx.assertTrue(bad == 0, "dense mask == former set on all " + ids.size() + " templates: " + rows);
        ctx.assertTrue(maxFirst < 2000, "first call under 2 s on every template, max " + maxFirst + " ms");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
