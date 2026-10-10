package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.network.ProgressionSyncRequestPayload;
import dev.itzgamesgd.moreminecrafts.network.QuestClaimRequestPayload;
import dev.itzgamesgd.moreminecrafts.network.ResearchPurchaseRequestPayload;
import dev.itzgamesgd.moreminecrafts.progression.ProgressionScreenData;
import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressState;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;

/** Read-only screen data and validated request helpers for future progression screens. */
public final class ProgressionClientApi {
    private static int revision = -1;
    private static Map<String,Integer> values = Map.of();
    private static String lastAction = "";
    private static int lastActionResult = -1;
    private ProgressionClientApi() {}

    static synchronized void replace(int nextRevision, Map<String,Integer> next) {
        var updated = dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRules.snapshot(
                revision, values, nextRevision, next);
        revision = updated.revision();
        values = updated.values();
    }
    static synchronized boolean apply(int nextRevision, Map<String,Integer> changes) {
        var updated = dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRules.delta(
                revision, values, nextRevision, changes);
        if (updated.status() == dev.itzgamesgd.moreminecrafts.progression.ProgressionSyncRules.Status.NEED_SNAPSHOT)
            return false;
        values = updated.values();
        revision = updated.revision();
        return true;
    }
    static synchronized void actionResult(String id, int result) { lastAction = id; lastActionResult = result; }
    static synchronized void reset() { revision = -1; values = Map.of(); lastAction = ""; lastActionResult = -1; }
    public static synchronized int revision() { return revision; }
    public static synchronized String lastAction() { return lastAction; }
    public static synchronized int lastActionResult() { return lastActionResult; }
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
    public static void requestSnapshot() {
        if (ClientPlayNetworking.canSend(ProgressionSyncRequestPayload.TYPE))
            ClientPlayNetworking.send(ProgressionSyncRequestPayload.INSTANCE);
    }
    public static void purchase(String id, int level) {
        if (ClientPlayNetworking.canSend(ResearchPurchaseRequestPayload.TYPE))
            ClientPlayNetworking.send(new ResearchPurchaseRequestPayload(id, level));
    }
    public static void claim(String id) {
        if (ClientPlayNetworking.canSend(QuestClaimRequestPayload.TYPE))
            ClientPlayNetworking.send(new QuestClaimRequestPayload(id));
    }
}
