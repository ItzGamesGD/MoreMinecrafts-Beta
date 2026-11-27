package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The only gameplay purchase path. Call it on the authoritative server thread. */
public final class ResearchPurchaseService {
    private static final int PLAYER_STORAGE_SLOTS = 36;
    public enum Result {
        PURCHASED, INVALID_ID, INVALID_LEVEL, MISSING_KNOWLEDGE, MISSING_PREREQUISITE,
        MISSING_MATERIALS, INSUFFICIENT_XP, ALREADY_PURCHASED, MAX_LEVEL, INVALID_STATE
    }
    private record Debit(int slot, int count) {}
    private ResearchPurchaseService() {}

    public static Result purchase(ServerPlayer player, String id, int requestedLevel) {
        if (player == null || player.level().getServer() == null
                || !player.level().getServer().isSameThread())
            return Result.INVALID_STATE;
        ResearchDefinition definition = ResearchCatalog.byId(id);
        if (definition == null) return Result.INVALID_ID;
        ResearchProgressState state = KnowledgeRuntime.state(player);
        int storedLevel = state.get("research." + id);
        if (storedLevel < 0 || storedLevel > definition.maximumLevel()) return Result.INVALID_STATE;
        int current = storedLevel;
        Set<Integer> knowledge = new HashSet<>();
        for (int i = 1; i <= 70; i++) if (state.hasKnowledge(i)) knowledge.add(i);
        Set<String> purchased = purchasedResearch(state);
        Map<String, Integer> inventoryTotals = materialTotals(player);
        int xp = RawXp.available(player);
        ResearchPurchaseRules.Result validation = ResearchPurchaseRules.validate(
                definition, requestedLevel, current, knowledge, purchased, inventoryTotals, xp);
        if (validation != ResearchPurchaseRules.Result.PURCHASED) return map(validation);

        ResearchDefinition.LevelCost cost = definition.cost(requestedLevel);
        List<Debit> debits = planDebits(player, cost.materials());
        if (debits == null) return Result.MISSING_MATERIALS;

        var inventory = player.getInventory();
        List<ItemStack> inventorySnapshot = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            inventorySnapshot.add(inventory.getItem(slot).copy());
        int oldLevel = player.experienceLevel;
        float oldProgress = player.experienceProgress;
        int oldTotalXp = player.totalExperience;
        ResearchProgressState oldState = state;

        try {
            for (Debit debit : debits) {
                ItemStack removed = inventory.removeItem(debit.slot(), debit.count());
                if (removed.getCount() != debit.count()) throw new IllegalStateException("Inventory changed during purchase");
            }
            player.giveExperiencePoints(-cost.rawXp());
            KnowledgeRuntime.updateState(player, state.with("research." + id, requestedLevel));
            return Result.PURCHASED;
        } catch (RuntimeException failure) {
            for (int slot = 0; slot < inventorySnapshot.size(); slot++)
                inventory.setItem(slot, inventorySnapshot.get(slot).copy());
            inventory.setChanged();
            player.experienceLevel = oldLevel;
            player.experienceProgress = oldProgress;
            player.totalExperience = oldTotalXp;
            KnowledgeRuntime.updateState(player, oldState);
            return Result.INVALID_STATE;
        }
    }

    static Set<String> purchasedResearch(ResearchProgressState state) {
        Set<String> purchased = new HashSet<>();
        for (ResearchDefinition research : ResearchCatalog.allCatalogEntries())
            if (state.researchLevel(research.id()) > 0) purchased.add(research.id());
        return purchased;
    }

    private static Result map(ResearchPurchaseRules.Result result) {
        return switch (result) {
            case PURCHASED -> Result.PURCHASED;
            case INVALID_LEVEL -> Result.INVALID_LEVEL;
            case MISSING_KNOWLEDGE -> Result.MISSING_KNOWLEDGE;
            case MISSING_PREREQUISITE -> Result.MISSING_PREREQUISITE;
            case MISSING_MATERIALS -> Result.MISSING_MATERIALS;
            case INSUFFICIENT_XP -> Result.INSUFFICIENT_XP;
            case ALREADY_PURCHASED -> Result.ALREADY_PURCHASED;
            case MAX_LEVEL -> Result.MAX_LEVEL;
            case INVALID_STATE -> Result.INVALID_STATE;
        };
    }

    private static Map<String, Integer> materialTotals(ServerPlayer player) {
        Map<String, Integer> totals = new HashMap<>();
        var inventory = player.getInventory();
        for (int slot = 0; slot < PLAYER_STORAGE_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    stack.getCount(), Integer::sum);
        }
        return totals;
    }

    private static List<Debit> planDebits(ServerPlayer player, List<ResearchDefinition.MaterialCost> materials) {
        Map<String, Integer> remaining = new HashMap<>();
        for (var material : materials) remaining.merge(material.itemId(), material.count(), Integer::sum);
        List<Debit> result = new ArrayList<>();
        var inventory = player.getInventory();
        for (int slot = 0; slot < PLAYER_STORAGE_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            int need = remaining.getOrDefault(itemId, 0);
            if (need <= 0) continue;
            int take = Math.min(need, stack.getCount());
            result.add(new Debit(slot, take));
            if (take == need) remaining.remove(itemId);
            else remaining.put(itemId, need - take);
        }
        return remaining.isEmpty() ? result : null;
    }
}
