package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

/** Adds the five locked T2 core identities to vanilla acquisition routes. */
public final class T2CoreAcquisition {
    private static final ResourceKey<LootTable> BASTION_TREASURE = lootTable("minecraft:chests/bastion_treasure");
    private static final ResourceKey<LootTable> ANCIENT_CITY = lootTable("minecraft:chests/ancient_city");
    private static final ResourceKey<LootTable> TRIAL_REWARD = lootTable("minecraft:chests/trial_chambers/reward");
    private static final ResourceKey<LootTable> OMINOUS_TRIAL_REWARD = lootTable("minecraft:chests/trial_chambers/reward_ominous");
    private static final Identifier OCEAN_MONUMENT = Identifier.withDefaultNamespace("ocean_monument");

    private T2CoreAcquisition() {}

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (key.equals(BASTION_TREASURE)) addRareCore(table, ModItems.BURNING_HEART, 0.04F);
            else if (key.equals(ANCIENT_CITY)) addRareCore(table, ModItems.ANCIENT_HEART, 0.03F);
            else if (key.equals(TRIAL_REWARD)) addRareCore(table, ModItems.EYE_OF_TYPHOON, 0.02F);
            else if (key.equals(OMINOUS_TRIAL_REWARD)) addRareCore(table, ModItems.HEART_OF_THUNDER, 0.03F);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ElderGuardian guardian && guardian.level() instanceof ServerLevel level)
                rewardCompletedMonument(level, guardian);
        });
        ServerTickEvents.END_SERVER_TICK.register(T2CoreAcquisition::retryPendingMonuments);
    }

    private static void addRareCore(LootTable.Builder table, Item item, float chance) {
        table.withPool(LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1)))
                .add(LootItem.lootTableItem(item)).when(LootItemRandomChanceCondition.randomChance(chance)));
    }

    private static void rewardCompletedMonument(ServerLevel level, ElderGuardian killed) {
        if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return;
        StructureStart monument = level.structureManager().getStructureWithPieceAt(killed.blockPosition(), holder ->
                holder.unwrapKey().map(key -> key.identifier().equals(OCEAN_MONUMENT)).orElse(false));
        if (monument == StructureStart.INVALID_START || !monument.isValid()) return;

        long monumentKey = monument.getChunkPos().pack();
        MonumentRewardData rewards = level.getDataStorage().computeIfAbsent(MonumentRewardData.TYPE);
        rewards.defer(monumentKey, monument.getBoundingBox(), killed.position());
        tryCompletePendingMonument(level, rewards, monumentKey);
    }

    private static void retryPendingMonuments(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        MonumentRewardData rewards = overworld.getDataStorage().get(MonumentRewardData.TYPE);
        if (rewards == null) return;
        for (MonumentRewardData.PendingMonument pending : rewards.pendingMonuments())
            tryCompletePendingMonument(overworld, rewards, pending.key());
    }

    private static void tryCompletePendingMonument(ServerLevel level, MonumentRewardData rewards, long monumentKey) {
        MonumentRewardData.PendingMonument pending = rewards.pending(monumentKey);
        if (pending == null) return;
        var bounds = pending.bounds();
        boolean allChunksLoaded = true;
        for (int chunkX = bounds.minX() >> 4; chunkX <= bounds.maxX() >> 4; chunkX++) {
            for (int chunkZ = bounds.minZ() >> 4; chunkZ <= bounds.maxZ() >> 4; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    allChunksLoaded = false;
                    break;
                }
            }
            if (!allChunksLoaded) break;
        }
        if (!allChunksLoaded) {
            rewards.finishIfReady(monumentKey, false, false);
            return;
        }

        AABB area = new AABB(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1.0D, bounds.maxY() + 1.0D, bounds.maxZ() + 1.0D);
        boolean livingGuardianRemains = !level.getEntitiesOfClass(ElderGuardian.class, area, ElderGuardian::isAlive).isEmpty();
        if (rewards.finishIfReady(monumentKey, true, livingGuardianRemains)
                != MonumentRewardData.CompletionResult.CLAIMED) return;

        ItemStack reward = new ItemStack(ModItems.HEART_OF_CURRENTS);
        level.addFreshEntity(new ItemEntity(level, pending.rewardX(), pending.rewardY(), pending.rewardZ(), reward));
    }

    private static ResourceKey<LootTable> lootTable(String id) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id));
    }
}
