package dev.itzgamesgd.moreminecrafts.worldgen;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Task A sample verification against the real loaded loot tables and worldgen registries. The probabilities are measured by
 * drawing the actual {@link LootTable}s with fixed seeds; they are not derived from the JSON.
 */
public final class SkyIslandsGameTests implements CustomTestMethodInvoker {
    private static final int SAMPLES = 200_000;

    /**
     * Looked up when a test runs, never in a static initializer: Fabric loads this class while collecting GameTests,
     * which can happen before the mod's items are registered, and a registry miss silently yields air (0% drops).
     */
    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        if (item == Items.AIR) throw new IllegalStateException("item not registered: " + id);
        return item;
    }

    private record Tally(int feather, int shard, int both, int maxFeatherPerDraw, int maxShardPerDraw, boolean allSingles) {}

    private static LootTable table(ServerLevel level, String id) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id)));
        if (table == LootTable.EMPTY) throw new IllegalStateException("loot table not loaded: " + id);
        return table;
    }

    private static LootParams params(ServerLevel level, float luck) {
        return new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).withLuck(luck).create(LootContextParamSets.CHEST);
    }

    private static Tally sample(LootTable table, LootParams params, long seedBase, int samples) {
        Item featherItem = item("moreminecrafts:sky_feather");
        Item shardItem = item("moreminecrafts:gale_shard");
        int feather = 0, shard = 0, both = 0, maxFeather = 0, maxShard = 0;
        boolean singles = true;
        for (int i = 0; i < samples; i++) {
            int f = 0, s = 0;
            for (ItemStack stack : table.getRandomItems(params, seedBase + i)) {
                if (stack.is(featherItem)) { f += stack.getCount(); singles &= stack.getCount() == 1; }
                if (stack.is(shardItem)) { s += stack.getCount(); singles &= stack.getCount() == 1; }
            }
            if (f > 0) feather++;
            if (s > 0) shard++;
            if (f > 0 && s > 0) both++;
            maxFeather = Math.max(maxFeather, f);
            maxShard = Math.max(maxShard, s);
        }
        return new Tally(feather, shard, both, maxFeather, maxShard, singles);
    }

    private static void near(GameTestHelper ctx, String label, int hits, int samples, double expected, double tolerance) {
        double rate = hits / (double) samples;
        MoreMinecrafts.LOGGER.info("SKY_LOOT_SAMPLE {} hits={} samples={} rate={} expected={}", label, hits, samples, rate, expected);
        ctx.assertTrue(Math.abs(rate - expected) <= tolerance, label + " measured " + rate + " expected " + expected + " +/- " + tolerance);
    }

    @GameTest
    public void ordinarySkyChestRollsFeatherFivePercentExactlyOne(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Tally t = sample(table(level, "moreminecrafts:chests/sky_islands/chest"), params(level, 0.0F), 7_000_000L, SAMPLES);
        near(ctx, "chest feather", t.feather(), SAMPLES, 0.05, 0.003);
        ctx.assertTrue(t.maxFeatherPerDraw() <= 1 && t.allSingles(), "a chest never holds more than one Sky Feather");
        ctx.assertTrue(t.shard() == 0, "ordinary chests never hold Gale Shards");
        ctx.succeed();
    }

    @GameTest
    public void skyVaultsRollFeatherFifteenAndShardFivePercentIndependently(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        for (String id : List.of("moreminecrafts:chests/sky_islands/vault", "moreminecrafts:chests/sky_islands/vault_tower_top")) {
            Tally t = sample(table(level, id), params(level, 0.0F), 9_000_000L, SAMPLES);
            near(ctx, id + " feather", t.feather(), SAMPLES, 0.15, 0.005);
            near(ctx, id + " gale_shard", t.shard(), SAMPLES, 0.05, 0.003);
            near(ctx, id + " both (independence 0.15*0.05)", t.both(), SAMPLES, 0.0075, 0.0015);
            ctx.assertTrue(t.maxFeatherPerDraw() <= 1 && t.maxShardPerDraw() <= 1 && t.allSingles(), id + " yields at most one of each, quantity 1");
        }
        ctx.succeed();
    }

    @GameTest
    public void luckDoesNotChangeFeatherOrShardChances(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Tally chest = sample(table(level, "moreminecrafts:chests/sky_islands/chest"), params(level, 10.0F), 11_000_000L, 100_000);
        near(ctx, "chest feather luck=10", chest.feather(), 100_000, 0.05, 0.004);
        Tally vault = sample(table(level, "moreminecrafts:chests/sky_islands/vault"), params(level, 10.0F), 12_000_000L, 100_000);
        near(ctx, "vault feather luck=10", vault.feather(), 100_000, 0.15, 0.006);
        near(ctx, "vault gale_shard luck=10", vault.shard(), 100_000, 0.05, 0.004);
        ctx.succeed();
    }

    @GameTest
    public void vanillaTrialAndStructureLootNeverGainsSkyRewards(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        for (String id : List.of("minecraft:chests/trial_chambers/reward", "minecraft:chests/trial_chambers/reward_ominous",
                "minecraft:chests/trial_chambers/reward_rare", "minecraft:chests/trial_chambers/reward_common",
                "minecraft:chests/trial_chambers/reward_unique", "minecraft:chests/simple_dungeon", "minecraft:chests/ancient_city")) {
            LootTable table = table(level, id);
            LootParams params = params(level, 0.0F);
            Item featherItem = item("moreminecrafts:sky_feather");
            Item shardItem = item("moreminecrafts:gale_shard");
            int items = 0;
            for (int i = 0; i < 20_000; i++) {
                for (ItemStack stack : table.getRandomItems(params, 13_000_000L + i)) {
                    items++;
                    ctx.assertTrue(!stack.is(featherItem) && !stack.is(shardItem), id + " must not drop sky rewards");
                }
            }
            ctx.assertTrue(items > 0, id + " still generates its vanilla loot");
        }
        ctx.succeed();
    }

    @GameTest
    public void skyWorldgenRegistriesAndTemplatesLoad(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String id : List.of("sky_island_mountain_snowy", "sky_island_mountain_rocky", "sky_island_mountain_meadow", "sky_island_snowy",
                "sky_island_meadow", "sky_island_forest", "sky_island_desert", "sky_island_badlands"))
            ctx.assertTrue(structures.get(ResourceKey.create(Registries.STRUCTURE, Identifier.parse("moreminecrafts:" + id))).isPresent(), "structure " + id);
        for (String id : List.of("sky_islands_mountain", "sky_islands_general"))
            ctx.assertTrue(sets.get(ResourceKey.create(Registries.STRUCTURE_SET, Identifier.parse("moreminecrafts:" + id))).isPresent(), "structure set " + id);

        var pools = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var manager = level.getStructureTemplateManager();
        int loadedPools = 0, loadedElements = 0;
        for (var key : pools.listElementIds().toList()) {
            if (!key.identifier().getNamespace().equals("moreminecrafts") || !key.identifier().getPath().startsWith("sky_islands/")) continue;
            loadedPools++;
            for (var entry : pools.getOrThrow(key).value().getTemplates()) {
                var element = entry.getFirst();
                var size = element.getSize(manager, Rotation.NONE);
                if (element.toString().contains("Empty")) continue;
                ctx.assertTrue(size.getX() > 0 && size.getY() > 0 && size.getZ() > 0, "template must load: " + element);
                loadedElements++;
            }
        }
        MoreMinecrafts.LOGGER.info("SKY_POOLS_VERIFIED pools={} elements={}", loadedPools, loadedElements);
        ctx.assertTrue(loadedPools == 6 * 2 + 3, "expected 15 sky island pools, loaded " + loadedPools);
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
