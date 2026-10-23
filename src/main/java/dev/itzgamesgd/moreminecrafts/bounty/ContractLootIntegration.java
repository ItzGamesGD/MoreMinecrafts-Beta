package dev.itzgamesgd.moreminecrafts.bounty;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

/**
 * Adds one contract pool to each approved structure loot table. Existing vanilla pools are untouched. Only the six
 * approved tables are modified; no other container receives contracts.
 */
public final class ContractLootIntegration {
    private static final Map<String, ContractEconomy.Difficulty> SOURCES = Map.of(
            "minecraft:chests/abandoned_mineshaft", ContractEconomy.Difficulty.EASY,
            "minecraft:chests/desert_pyramid", ContractEconomy.Difficulty.EASY,
            "minecraft:chests/jungle_temple", ContractEconomy.Difficulty.MEDIUM,
            "minecraft:chests/trial_chambers/reward", ContractEconomy.Difficulty.MEDIUM,
            "minecraft:chests/ancient_city", ContractEconomy.Difficulty.HARD,
            "minecraft:chests/bastion_treasure", ContractEconomy.Difficulty.HARD);

    private ContractLootIntegration() {}

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            ContractEconomy.Difficulty difficulty = SOURCES.get(key.identifier().toString());
            if (difficulty != null) builder.pool(contractPool(difficulty).build());
        });
    }

    /** Ids of the loot tables this integration modifies, used by tests to confirm the approved scope. */
    public static Set<String> sourceIds() {
        return SOURCES.keySet();
    }

    static LootPool.Builder contractPool(ContractEconomy.Difficulty difficulty) {
        int[] weights = ContractEconomy.tierWeights(difficulty);
        ContractData.Tier[] tiers = {ContractData.Tier.BEGINNER, ContractData.Tier.INTERMEDIATE, ContractData.Tier.ADVANCED};
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(net.minecraft.core.Holder.direct(new net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue(1)))
                .when(LootItemRandomChanceCondition.randomChance(ContractEconomy.appearancePercent(difficulty) / 100.0F));
        for (int index = 0; index < tiers.length; index++) {
            pool.add(LootItem.lootTableItem(ModItems.BOUNTY_CONTRACT)
                    .setWeight(weights[index])
                    .apply(SetComponentsFunction.setComponent(ModDataComponents.PENDING_CONTRACT, tiers[index])));
        }
        return pool;
    }
}
