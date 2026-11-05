package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.List;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/**
 * D13-03-I01 (Q01-Q10): independent T2 accessory pools added to four vanilla structure chest tables.
 * Each pool is one extra roll gated by a fixed chance; joint pools hold two equal-weight entries so a success
 * yields exactly one of them. Existing pools (vanilla loot, Ancient Heart, Relic Catalyst) are never touched.
 */
public final class T2StructureChestAccessories {
    /** One additive pool: {@code chance} per generated chest, one entry chosen by equal weight on success. */
    public record PoolSpec(String lootTable, double chance, List<AccessoryDefinition> items) {
        public double chancePerItem() { return chance / items.size(); }
    }

    public static final List<PoolSpec> POOLS = List.of(
            new PoolSpec("minecraft:chests/nether_bridge", 0.05D,
                    List.of(AccessoryDefinition.EMBER_RING, AccessoryDefinition.SOUL_CHAIN)),
            new PoolSpec("minecraft:chests/shipwreck_treasure", 0.05D, List.of(AccessoryDefinition.WHISPER_OF_THE_SEA)),
            new PoolSpec("minecraft:chests/ancient_city", 0.05D,
                    List.of(AccessoryDefinition.RING_OF_LIFE, AccessoryDefinition.PHANTOM_MIRROR)),
            new PoolSpec("minecraft:chests/bastion_treasure", 0.15D, List.of(AccessoryDefinition.BLOOD_COVENANT)));

    private T2StructureChestAccessories() {}

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            for (PoolSpec spec : POOLS) {
                if (!key.equals(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(spec.lootTable())))) continue;
                LootPool.Builder pool = LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1)))
                        .when(LootItemRandomChanceCondition.randomChance((float) spec.chance()));
                for (AccessoryDefinition definition : spec.items())
                    pool.add(LootItem.lootTableItem(ModItems.ACCESSORIES.get(definition)).setWeight(1));
                table.withPool(pool);
            }
        });
    }
}
