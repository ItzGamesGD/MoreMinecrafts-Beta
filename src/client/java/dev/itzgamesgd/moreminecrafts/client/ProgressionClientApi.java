package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.network.ProgressionSyncRequestPayload;
import dev.itzgamesgd.moreminecrafts.network.QuestClaimRequestPayload;
import dev.itzgamesgd.moreminecrafts.network.ResearchPurchaseRequestPayload;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionPurchaseGate;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionPurchasePolicy;
import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressState;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Read-only screen data and validated request helpers for future progression screens. */
public final class ProgressionClientApi {
    private static int revision = -1;
    private static Map<String,Integer> values = Map.of();
    private static String lastAction = "";
    private static int lastActionResult = -1;
    private static final ProgressionPurchaseGate PURCHASE_GATE = new ProgressionPurchaseGate();
    private ProgressionClientApi() {}

    static synchronized void replace(int nextRevision, Map<String,Integer> next) {
        var updated = dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRules.snapshot(
                revision, values, nextRevision, next);
        revision = updated.revision();
        values = updated.values();
        PURCHASE_GATE.synchronizedAt(revision);
    }
    static synchronized boolean apply(int nextRevision, Map<String,Integer> changes) {
        var updated = dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRules.delta(
                revision, values, nextRevision, changes);
        if (updated.status() == dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRules.Status.NEED_SNAPSHOT)
            return false;
        values = updated.values();
        revision = updated.revision();
        PURCHASE_GATE.synchronizedAt(revision);
        return true;
    }
    static synchronized void actionResult(String id, int result, int resultRevision) {
        lastAction = id;
        lastActionResult = result;
        PURCHASE_GATE.actionResult(id, resultRevision);
    }
    static synchronized void reset() {
        revision = -1; values = Map.of(); lastAction = ""; lastActionResult = -1; PURCHASE_GATE.reset();
    }
    public static synchronized int revision() { return revision; }
    public static synchronized String lastAction() { return lastAction; }
    public static synchronized int lastActionResult() { return lastActionResult; }
    public static synchronized boolean purchasePending(String id) { return PURCHASE_GATE.pending("research:" + id); }
    private static synchronized ResearchProgressState state() { return new ResearchProgressState(values); }

    public static java.util.List<ProgressionScreenData.KnowledgeRow> knowledge(String discipline) {
        return ProgressionScreenData.knowledge(state(), discipline);
    }
    public static java.util.List<ProgressionScreenData.ResearchRow> research(String discipline) {
        var player = Minecraft.getInstance().player;
        if (player == null) return java.util.List.of();
        Map<String,Integer> materials = new HashMap<>();
        for (int slot = 0; slot < Math.min(36, player.getInventory().getContainerSize()); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) materials.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    stack.getCount(), Integer::sum);
        }
        return ProgressionScreenData.research(state(), discipline, materials, RawXp.available(player));
    }
    public static java.util.List<ProgressionScreenData.QuestRow> quests(String category) {
        return ProgressionScreenData.quests(state(), category);
    }
    public static String materialName(String itemId) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        return item == null || item == Items.AIR ? itemId : new ItemStack(item).getHoverName().getString();
    }
    public static int materialCount(String itemId) {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        if (item == null || item == Items.AIR) return 0;
        int count = 0;
        for (int slot = 0; slot < Math.min(36, player.getInventory().getContainerSize()); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }
    public static int availableXp() {
        var player = Minecraft.getInstance().player;
        return player == null ? 0 : RawXp.available(player);
    }
    public static void requestSnapshot() {
        if (ClientPlayNetworking.canSend(ProgressionSyncRequestPayload.TYPE))
            ClientPlayNetworking.send(ProgressionSyncRequestPayload.INSTANCE);
    }
    public static synchronized boolean purchaseIfEligible(ProgressionScreenData.ResearchRow row) {
        if (row == null || !ProgressionPurchasePolicy.canRequest(true, row.prerequisitesDefined(), row.level(),
                row.maximumLevel(), row.nextCost() != null, row.availability())
                || !ClientPlayNetworking.canSend(ResearchPurchaseRequestPayload.TYPE))
            return false;
        String actionId = "research:" + row.id();
        if (!PURCHASE_GATE.begin(actionId, revision)) return false;
        ClientPlayNetworking.send(new ResearchPurchaseRequestPayload(row.id(), row.level() + 1));
        return true;
    }
    public static void claim(String id) {
        if (ClientPlayNetworking.canSend(QuestClaimRequestPayload.TYPE))
            ClientPlayNetworking.send(new QuestClaimRequestPayload(id));
    }
}
