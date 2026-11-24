package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * P3-D: every heaven meadow template ({@code heaven_p3/meadow/*}) is placed in the real server; after the P3 survival
 * rules had their ticks, every block of the template must still be exactly where the template put it (no vegetation
 * popped, no giant / medium plant removed, terrain and structure untouched).
 */
public final class HeavenP3MeadowGameTests implements CustomTestMethodInvoker {
    static final List<String> NAMES = List.of("large_a", "large_b", "large_c", "large_d", "large_e", "plain_medium_a", "plain_medium_b",
            "plain_medium_c", "plain_medium_d", "plain_medium_e", "plain_small_a", "plain_small_b", "plain_small_c", "plain_small_d",
            "ruins", "satellite_a", "satellite_b", "temple", "tower");

    private record Expected(BlockPos pos, BlockState state) {}

    @GameTest(maxTicks = 400)
    public void heavenMeadowTemplatesSurvivePlacement(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos start = ctx.absolutePos(BlockPos.ZERO).atY(205).offset(0, 0, 3000);     // far away: never shades other tests
        Map<String, List<Expected>> expected = new LinkedHashMap<>();
        Map<String, Long> placeMicros = new LinkedHashMap<>();
        Map<String, BlockPos> origins = new LinkedHashMap<>();
        int x = 0;
        var blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (String name : NAMES) {
            Identifier id = Identifier.parse("moreminecrafts:heaven_p3/meadow/" + name);
            StructureTemplate t = level.getStructureTemplateManager().get(id).orElseThrow(() -> new AssertionError("missing " + id));
            BlockPos origin = start.offset(x, 0, 0);
            x += t.getSize().getX() + 8;
            CompoundTag tag = t.save(new CompoundTag());
            var palette = tag.getListOrEmpty("palette");
            List<BlockState> states = new ArrayList<>();
            for (int i = 0; i < palette.size(); i++) states.add(NbtUtils.readBlockState(blocks, palette.getCompoundOrEmpty(i)));
            List<Expected> exp = new ArrayList<>();
            for (var e : tag.getListOrEmpty("blocks")) {
                CompoundTag b = (CompoundTag) e;
                var pos = b.getListOrEmpty("pos");
                exp.add(new Expected(origin.offset(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), states.get(b.getIntOr("state", 0))));
            }
            long t0 = System.nanoTime();
            ctx.assertTrue(t.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS), "place " + name);
            placeMicros.put(name, (System.nanoTime() - t0) / 1000);
            expected.put(name, exp);
            origins.put(name, origin);
        }
        ctx.runAfterDelay(5, () -> {
            Map<String, Object> out = new LinkedHashMap<>();
            List<String> detail = new ArrayList<>();
            int bad = 0;
            for (var e : expected.entrySet()) {
                Map<String, Integer> missing = new TreeMap<>();
                int veg = 0;
                for (Expected ex : e.getValue()) {
                    String key = BuiltInRegistries.BLOCK.getKey(ex.state().getBlock()).getPath();
                    if (ex.state().getBlock() instanceof HeavenPartBlock || key.endsWith("_leaves") || key.equals("oak_log")) veg++;
                    BlockState now = level.getBlockState(ex.pos());
                    // heaven blocks and leaves: exact state; vanilla building blocks: same block (walls re-connect on placement)
                    boolean exact = ex.state().getBlock() instanceof HeavenPartBlock || key.endsWith("_leaves") || key.startsWith("heaven_");
                    if (exact ? !now.equals(ex.state()) : !now.is(ex.state().getBlock())) {
                        missing.merge(key, 1, Integer::sum);
                        if (detail.size() < 12) {
                            BlockPos rel = ex.pos().subtract(origins.get(e.getKey()));
                            String around = "";
                            for (var d : net.minecraft.core.Direction.values())
                                around += d.getName().charAt(0) + "=" + BuiltInRegistries.BLOCK.getKey(level.getBlockState(ex.pos().relative(d)).getBlock()).getPath() + " ";
                            detail.add(e.getKey() + "@" + rel.toShortString() + " " + ex.state() + " now=" + now + " " + around);
                        }
                    }
                }
                // the P3 rules evaluated directly (block ticks may not run in these far chunks): every vine / wisteria / ground plant
                // can survive where it stands and every multi-cell plant is identified and supported
                int unsupported = 0;
                for (Expected ex : e.getValue()) {
                    if (!(ex.state().getBlock() instanceof HeavenPartBlock hp)) continue;
                    BlockState now = level.getBlockState(ex.pos());
                    if (!now.equals(ex.state())) continue;
                    boolean ok = switch (hp.rule()) {
                        case GROUND, VINE, WISTERIA -> now.canSurvive(level, ex.pos());
                        case MEDIUM, GIANT -> {
                            var inst = HeavenPlants.find(level, ex.pos(), now, true, level::isLoaded);
                            yield inst != null && HeavenPlants.supported(level, inst);
                        }
                    };
                    if (!ok) { unsupported++; missing.merge("unsupported_" + BuiltInRegistries.BLOCK.getKey(hp).getPath(), 1, Integer::sum); }
                }
                bad += missing.values().stream().mapToInt(Integer::intValue).sum();
                out.put(e.getKey(), "blocks=" + e.getValue().size() + " vegetation=" + veg + " missing=" + missing + " place_us=" + placeMicros.get(e.getKey()));
            }
            MoreMinecrafts.LOGGER.info("HEAVEN_P3_TEST meadow_placement {}", out);
            if (!detail.isEmpty()) MoreMinecrafts.LOGGER.info("HEAVEN_P3_TEST meadow_missing_detail {}", detail);
            ctx.assertTrue(bad == 0, "every heaven meadow template block survives placement: " + out);
            ctx.succeed();
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
