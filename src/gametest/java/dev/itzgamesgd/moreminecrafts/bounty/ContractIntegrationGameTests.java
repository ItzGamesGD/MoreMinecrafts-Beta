package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;

/** Loot and villager integration checks. Rates use fixed seeds, so results are deterministic. */
public final class ContractIntegrationGameTests implements CustomTestMethodInvoker {
    private static final Map<String, ContractEconomy.Difficulty> SOURCE_DIFFICULTY = Map.of(
            "minecraft:chests/abandoned_mineshaft", ContractEconomy.Difficulty.EASY,
            "minecraft:chests/desert_pyramid", ContractEconomy.Difficulty.EASY,
            "minecraft:chests/jungle_temple", ContractEconomy.Difficulty.MEDIUM,
            "minecraft:chests/trial_chambers/reward", ContractEconomy.Difficulty.MEDIUM,
            "minecraft:chests/ancient_city", ContractEconomy.Difficulty.HARD,
            "minecraft:chests/bastion_treasure", ContractEconomy.Difficulty.HARD);

    private static int tierIndex(ContractData.Tier tier) {
        return switch (tier) {
            case BEGINNER -> 0;
            case INTERMEDIATE -> 1;
            default -> 2;
        };
    }

    @GameTest
    public void approvedSourcesRollContractsAtApprovedRates(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        int trials = 20000;
        for (var entry : SOURCE_DIFFICULTY.entrySet()) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(
                    ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(entry.getKey())));
            int appeared = 0;
            int[] tiers = new int[3];
            for (int trial = 0; trial < trials; trial++) {
                for (ItemStack stack : table.getRandomItems(params, 1000L + trial)) {
                    ContractData.Tier tier = stack.get(ModDataComponents.PENDING_CONTRACT);
                    if (tier == null) continue;
                    appeared++;
                    tiers[tierIndex(tier)]++;
                }
            }
            double expected = ContractEconomy.appearancePercent(entry.getValue()) / 100.0;
            double rate = appeared / (double) trials;
            ctx.assertTrue(Math.abs(rate - expected) <= 0.02,
                    entry.getKey() + " appearance " + rate + " expected " + expected);
            int[] weights = ContractEconomy.tierWeights(entry.getValue());
            for (int index = 0; index < 3 && appeared > 0; index++) {
                double share = tiers[index] / (double) appeared;
                ctx.assertTrue(Math.abs(share - weights[index] / 100.0) <= 0.07,
                        entry.getKey() + " tier " + index + " share " + share);
            }
        }
        ctx.succeed();
    }

    @GameTest
    public void unapprovedContainerNeverReceivesContracts(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/simple_dungeon")));
        int contracts = 0;
        int vanilla = 0;
        for (int trial = 0; trial < 2000; trial++) {
            for (ItemStack stack : table.getRandomItems(params, 5000L + trial)) {
                if (stack.get(ModDataComponents.PENDING_CONTRACT) != null) contracts++;
                else vanilla++;
            }
        }
        ctx.assertTrue(contracts == 0, "no contracts injected into unrelated chest");
        ctx.assertTrue(vanilla > 0, "vanilla loot still generated");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
