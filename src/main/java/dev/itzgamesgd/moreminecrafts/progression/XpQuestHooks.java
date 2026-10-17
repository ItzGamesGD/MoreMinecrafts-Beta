package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Q1-B live evidence for XP Quests. Every method runs on the server thread, attributes progress to
 * the acting player, and writes only {@code quest.counter.*}, {@code quest.seen.*} and
 * {@code quest.baseline.*} keys through the single {@link KnowledgeRuntime#updateState} path.
 *
 * <p>Retroactive history uses a <em>baseline</em> captured once per key, before the first live event:
 * progress = baseline + live counter. A live event is never also part of its baseline, so history is
 * recognised without double counting (owner policy 3).</p>
 */
public final class XpQuestHooks {
    /** Knowledge counters whose collectors report "stored + n" per event; their deltas are exact events. */
    static final Set<String> INCREMENT_CONDITIONS = Set.of(
            "archaeology_items_brushed", "shield_blocks", "trident_hostile_kills", "natural_vanilla_logs_mined",
            "pickaxe_durability_used", "crops_harvested", "farmland_tilled", "piglin_barters", "fishing_successes",
            "anvil_repairs", "redstone_components_placed", "potions_brewed");
    static final String BLOCKS_PLACED = "blocks_placed";
    static final String BLOCK_ITEMS_USED_BASELINE = "block_items_used";
    static final String PLACED_BLOCK_SET = "set.blocks_placed";
    static final String NATURAL_ORE_SET = "set.natural_ore_mined";
    static final String TAMED_SET = "set.tamed";
    /** Bound for per-player distinct sets; the largest catalog target is 150. */
    static final int DISTINCT_CAP = 200;

    private XpQuestHooks() {}

    /** Registered before XP Quest reconciliation so baselines exist before the first evaluation. */
    static void initialize() {
        QuestOreProvenance.initialize();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> captureBaselines(handler.player));
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) oreMined(serverPlayer, level, pos, state);
        });
    }

    static String baselineKey(String key) { return "quest.baseline." + key; }

    static boolean hasBaseline(ResearchProgressState state, String key) {
        return state.counters().containsKey(baselineKey(key));
    }

    /** Captures every not-yet-captured baseline from persisted evidence. Idempotent. */
    public static void captureBaselines(ServerPlayer player) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        for (String condition : INCREMENT_CONDITIONS)
            if (!hasBaseline(next, condition)) next = next.with(baselineKey(condition), next.conditionProgress(condition));
        next = capturePlacementBaseline(player, next);
        if (!next.equals(before)) KnowledgeRuntime.updateState(player, next);
    }

    private static ResearchProgressState capturePlacementBaseline(ServerPlayer player, ResearchProgressState state) {
        if (hasBaseline(state, BLOCK_ITEMS_USED_BASELINE)) return state;
        long used = 0;
        ResearchProgressState next = state;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem blockItem)) continue;
            int count = player.getStats().getValue(Stats.ITEM_USED.get(item));
            if (count <= 0) continue;
            used += count;
            next = observeBounded(next, PLACED_BLOCK_SET, BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());
        }
        return next.with(baselineKey(BLOCK_ITEMS_USED_BASELINE), (int) Math.min(Integer.MAX_VALUE, used));
    }

    private static ResearchProgressState observeBounded(ResearchProgressState state, String set, String entry) {
        if (QuestRules.distinctCount(state, set) >= DISTINCT_CAP) return state;
        return QuestRules.observeDistinct(state, set, entry);
    }

    private static void write(ServerPlayer player, ResearchProgressState before, ResearchProgressState after) {
        if (after != before && !after.equals(before)) KnowledgeRuntime.updateState(player, after);
    }

    /**
     * Called by {@link KnowledgeRuntime#recordCondition} before it stores a new total. For the
     * increment-style collectors the difference between the reported total and the stored (capped)
     * value is exactly this event's amount, even after the Knowledge counter reached its cap.
     */
    static void knowledgeConditionReported(ServerPlayer player, String condition, int reportedTotal) {
        if (!INCREMENT_CONDITIONS.contains(condition)) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        if (!hasBaseline(next, condition)) next = next.with(baselineKey(condition), next.conditionProgress(condition));
        long delta = (long) reportedTotal - next.conditionProgress(condition);
        if (delta > 0) next = QuestRules.addCounter(next, condition, delta);
        write(player, before, next);
    }

    /** BlockItem.place returned Success for this player: one confirmed placement. */
    public static void blockPlaced(ServerPlayer player, BlockItem item) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = capturePlacementBaseline(player, before);
        next = QuestRules.addCounter(next, BLOCKS_PLACED, 1);
        next = observeBounded(next, PLACED_BLOCK_SET, BuiltInRegistries.BLOCK.getKey(item.getBlock()).toString());
        write(player, before, next);
    }

    /** Player-mined tracked ore: counts only when provenance is verified natural. */
    static void oreMined(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        if (!QuestOreProvenance.isTrackedOre(state)) return;
        QuestOreProvenance.Verdict verdict = QuestOreProvenance.verdict(level, pos);
        QuestOreProvenance.clear(level, pos);
        if (verdict != QuestOreProvenance.Verdict.NATURAL) return;
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = QuestRules.addCounter(before, "natural_ore_mined", 1);
        next = QuestRules.addCounter(next, "natural_mined." + id, 1);
        next = observeBounded(next, NATURAL_ORE_SET, id);
        write(player, before, next);
    }

    /** Vanilla TAME_ANIMAL fired for this player (TamableAnimal.tame / AbstractHorse.tameWithName). */
    public static void tamed(ServerPlayer player, Entity animal) {
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, observeBounded(before, TAMED_SET, type.toString()));
    }

    /** A movement statistic (centimetres) awarded while the player is in the Nether. */
    public static void netherMovement(ServerPlayer player, int centimetres) {
        if (centimetres <= 0 || player.level().dimension() != Level.NETHER) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.addCounter(before, "nether_travel_cm", centimetres));
    }

    /** One fish sold to a Fisherman for {@code emeralds} emeralds. */
    public static void fishSold(ServerPlayer player, int emeralds) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = QuestRules.addCounter(before, "fish_sold", 1);
        next = QuestRules.addCounter(next, "fish_sale_emeralds", Math.max(0, emeralds));
        write(player, before, next);
    }
}
