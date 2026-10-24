package dev.itzgamesgd.moreminecrafts.progression;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Player-attributed structure entry evidence. A player has entered a structure only while standing inside
 * one of its generated pieces (the same {@code getStructureWithPieceAt} test vanilla location predicates
 * use); chunk-level structure references, whole-structure bounding boxes, biomes and mere proximity do not
 * count. Each structure type is recorded once, and end cities are additionally keyed by dimension and the
 * structure's start chunk so three genuinely different cities are required.
 */
public final class QuestStructureEntry {
    static final String TYPES = "set.structure_types_entered";
    static final String END_CITIES = "set.end_city_starts";
    private static final Identifier END_CITY = Identifier.withDefaultNamespace("end_city");
    private static final int END_CITY_TARGET = 3;
    private static final int INTERVAL_TICKS = 10;

    private QuestStructureEntry() {}

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % INTERVAL_TICKS != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) observe(player);
        });
    }

    static void observe(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.hasDisconnected()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos pos = player.blockPosition();
        var structures = level.structureManager();
        var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        for (var structure : structures.getAllStructuresAt(pos).keySet()) {
            StructureStart start = structures.getStructureWithPieceAt(pos.getX(), pos.getY(), pos.getZ(), structure);
            if (!start.isValid()) continue;
            Identifier id = registry.getKey(structure);
            if (id == null) continue;
            next = QuestRules.observeDistinct(next, TYPES, id.toString());
            if (id.equals(END_CITY) && QuestRules.distinctCount(next, END_CITIES) < END_CITY_TARGET)
                next = QuestRules.observeDistinct(next, END_CITIES,
                        level.dimension().identifier() + ":" + start.getChunkPos().pack());
        }
        if (!next.equals(before)) {
            KnowledgeRuntime.updateState(player, next);
            XpQuestTracking.reconcile(player);
        }
    }
}
