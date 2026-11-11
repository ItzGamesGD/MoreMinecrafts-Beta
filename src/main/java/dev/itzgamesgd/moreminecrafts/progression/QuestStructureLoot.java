package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

/** Player-specific evidence from a container's first loot generation. */
public final class QuestStructureLoot {
    private static final String TABLES = "set.structure_loot_tables";
    private static final Set<String> SHIPWRECK = Set.of("shipwreck_supply", "shipwreck_map", "shipwreck_treasure");
    private static final Set<String> RUINS = Set.of("underwater_ruin_big", "underwater_ruin_small");
    private static final Set<String> BASTION = Set.of("bastion_bridge", "bastion_hoglin_stable", "bastion_other", "bastion_treasure");

    private QuestStructureLoot() {}

    public static void blockGenerated(ServerPlayer player, ResourceKey<LootTable> table, Level level, BlockPos pos) {
        if (player == null || level == null || pos == null) return;
        generated(player, table, level.dimension().identifier() + ":block:" + pos.asLong());
    }

    public static void entityGenerated(ServerPlayer player, ResourceKey<LootTable> table, Level level, java.util.UUID entityId) {
        if (player == null || level == null || entityId == null) return;
        generated(player, table, level.dimension().identifier() + ":entity:" + entityId);
    }

    private static void generated(ServerPlayer player, ResourceKey<LootTable> table, String container) {
        if (table == null || player.level().getServer() == null
                || player.level().getServer().reloadableRegistries().getLootTable(table) == LootTable.EMPTY) return;
        String id = table.identifier().toString();
        String path = table.identifier().getPath();
        if (!path.startsWith("chests/")) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = observe(before, "set.structure_loot_any", container, 1);
        // The approved structure-specific tables are vanilla identities, not just matching paths.
        if (!"minecraft".equals(table.identifier().getNamespace())) {
            if (!next.equals(before)) {
                KnowledgeRuntime.updateState(player, next);
                XpQuestTracking.reconcile(player);
            }
            return;
        }
        String name = path.substring("chests/".length());
        if (name.equals("abandoned_mineshaft")) next = observe(next, "set.structure_loot_mineshaft", container, 1);
        if (name.equals("simple_dungeon")) next = observe(next, "set.structure_loot_dungeon", container, 1);
        if (name.equals("nether_bridge")) next = observe(next, "set.structure_loot_fortress", container, 1);
        if (name.equals("ancient_city") || name.equals("ancient_city_ice_box"))
            next = observe(next, "set.structure_loot_ancient_city", container, 3);
        if (name.equals("end_city_treasure")) next = observe(next, "set.structure_loot_end_city", container, 10);
        if (name.equals("desert_pyramid") || name.equals("jungle_temple")
                || SHIPWRECK.contains(name) || RUINS.contains(name)
                || name.equals("buried_treasure") || BASTION.contains(name))
            next = QuestRules.observeDistinct(next, TABLES, id);
        if (!next.equals(before)) {
            KnowledgeRuntime.updateState(player, next);
            XpQuestTracking.reconcile(player);
        }
    }

    private static ResearchProgressState observe(ResearchProgressState state, String key, String value, int cap) {
        return QuestRules.distinctCount(state, key) >= cap ? state : QuestRules.observeDistinct(state, key, value);
    }
}
