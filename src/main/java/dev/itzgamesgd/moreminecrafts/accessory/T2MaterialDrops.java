package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingState;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

/**
 * T2 accessory ingredient acquisition that already has a drop source (2026-11-13 T2 record §2.2 plus the owner's
 * 2026-11-16 task decisions). Every pool is an additive extra pool; no existing pool is replaced.
 *
 * <ul>
 *   <li>Skeleton King's Chain: 100% x1, only from the Skeleton King boss (never ordinary or elite skeletons).</li>
 *   <li>Gladiator's Bloodied Crest: 100% x1 from the Immortal Gladiator, see {@code ImmortalGladiator#dropCustomDeathLoot}.</li>
 *   <li>Ancient Clock Spring: 12% x1 per generated ancient city chest, plus 100% x1 from the Warden.</li>
 *   <li>Ancient World Tree Sap: 100% x1 from the Tomb Guardian (royal PRIMARY chest rolls live in RoyalChestRewardModel).</li>
 * </ul>
 */
public final class T2MaterialDrops {
    /** One additive loot pool: independent chance, fixed count. */
    public record PoolSpec(String lootTable, double chance, Item item, int count) {}

    public static final double ANCIENT_CITY_CLOCK_SPRING_CHANCE = 0.12D;

    public static List<PoolSpec> pools() {
        return List.of(
                new PoolSpec("minecraft:chests/ancient_city", ANCIENT_CITY_CLOCK_SPRING_CHANCE, ModItems.ANCIENT_CLOCK_SPRING, 1),
                new PoolSpec("minecraft:entities/warden", 1.0D, ModItems.ANCIENT_CLOCK_SPRING, 1),
                new PoolSpec(TombGuardianGravityHeart.LOOT_TABLE, 1.0D, ModItems.ANCIENT_WORLD_TREE_SAP, 1));
    }

    private T2MaterialDrops() {}

    public static boolean isSkeletonKing(net.minecraft.world.entity.Entity entity) {
        return entity instanceof SkeletonKingState state && state.moreminecrafts$isSkeletonKing();
    }

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            for (PoolSpec spec : pools()) {
                if (!key.equals(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(spec.lootTable())))) continue;
                LootPool.Builder pool = LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1)));
                if (spec.chance() < 1.0D) pool.when(LootItemRandomChanceCondition.randomChance((float) spec.chance()));
                pool.add(LootItem.lootTableItem(spec.item())
                        .apply(SetItemCountFunction.setCount(Holder.direct(new ConstantValue(spec.count())))));
                table.withPool(pool);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level() instanceof ServerLevel level && isSkeletonKing(entity))
                entity.spawnAtLocation(level, new ItemStack(ModItems.SKELETON_KING_CHAIN, 1));
        });
    }
}
