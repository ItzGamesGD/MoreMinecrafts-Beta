package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/**
 * D13-03-I03 (Q21-Q24): Heart of Gravity, 20% x1, as a separate additive pool on the Tomb Guardian's entity loot
 * table. Vanilla evaluates an entity's table once per death into shared world drops (no per-player roll), and the
 * pool has its own random-chance condition, so it is independent of the Ancient Heart 50% pool (both can drop).
 * The Ancient Heart pool JSON, encounter and combat code are untouched.
 */
public final class TombGuardianGravityHeart {
    public static final String LOOT_TABLE = "moreminecrafts:entities/tomb_guardian";
    public static final float CHANCE = 0.20F;
    public static final int COUNT = 1;

    private TombGuardianGravityHeart() {}

    public static void initialize() {
        ResourceKey<LootTable> target = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(LOOT_TABLE));
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (!key.equals(target)) return;
            table.withPool(LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1)))
                    .when(LootItemRandomChanceCondition.randomChance(CHANCE))
                    .add(LootItem.lootTableItem(ModItems.ACCESSORIES.get(AccessoryDefinition.HEART_OF_GRAVITY))
                            .apply(SetItemCountFunction.setCount(Holder.direct(new ConstantValue(COUNT))))));
        });
    }
}
