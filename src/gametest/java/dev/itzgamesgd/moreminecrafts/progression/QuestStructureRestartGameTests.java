package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * Structure-entry evidence across a clean two-JVM restart. Run with
 * {@code moreminecrafts.questcity.restart.phase=write|read}; without the property it is a no-op.
 */
public final class QuestStructureRestartGameTests implements CustomTestMethodInvoker {
    private static final UUID PLAYER_ID = UUID.fromString("7c2e4b90-1d55-4c0e-8d0a-4f6e2b9a1101");
    private static final String[] VILLAGES = {"village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga"};

    private static ChunkPos cityChunk(int i) { return new ChunkPos(420 + i * 12, 420); }
    private static ChunkPos villageChunk(int i) { return new ChunkPos(420 + i * 12, 440); }

    private static BlockPos enterCity(ServerLevel level, ServerPlayer player, int i) {
        BlockPos at = QuestStructureFixture.register(level, "minecraft:end_city", cityChunk(i)).inside();
        player.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        QuestStructureEntry.observe(player);
        return at;
    }

    private static void enterVillage(ServerLevel level, ServerPlayer player, int i) {
        BlockPos at = QuestStructureFixture.register(level, "minecraft:" + VILLAGES[i], villageChunk(i)).inside();
        player.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        QuestStructureEntry.observe(player);
    }

    @GameTest(maxTicks = 400)
    public void structureEntryPersistsAcrossCleanJvms(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.questcity.restart.phase");
        if (phase == null) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        boolean read = phase.equals("read");
        if (!read && !phase.equals("write")) throw new IllegalArgumentException("Unknown phase " + phase);
        ServerPlayer player = QuestPersistentPlayers.join(ctx, PLAYER_ID, "xpq-structure-restart", read);
        ResearchProgressState state = KnowledgeRuntime.state(player);
        if (!read) {
            enterCity(level, player, 0);
            enterCity(level, player, 1);
            for (int i = 0; i < 4; i++) enterVillage(level, player, i);
            state = KnowledgeRuntime.state(player);
            ctx.assertTrue(QuestRules.distinctCount(state, QuestStructureEntry.END_CITIES) == 2
                    && QuestRules.distinctCount(state, QuestStructureEntry.TYPES) == 5
                    && !state.questComplete("xpq.dim.end_cities_3") && !state.questComplete("xpq.exp.villages_5"),
                    "write JVM holds two cities and four village types plus end_city");
            level.getServer().getPlayerList().saveAll();
            level.getChunkSource().save(true);
            System.out.println("QUEST_CITY_RESTART_WRITE player=" + PLAYER_ID);
            ctx.succeed();
            return;
        }
        ctx.assertTrue(QuestRules.distinctCount(state, QuestStructureEntry.END_CITIES) == 2
                && QuestRules.distinctCount(state, QuestStructureEntry.TYPES) == 5,
                "read JVM restored both evidence sets from disk");
        enterCity(level, player, 1); // a city entered before the restart adds nothing
        ctx.assertTrue(QuestRules.distinctCount(KnowledgeRuntime.state(player), QuestStructureEntry.END_CITIES) == 2,
                "re-entering a pre-restart city after restart is not new");
        enterCity(level, player, 2);
        enterVillage(level, player, 4);
        state = KnowledgeRuntime.state(player);
        ctx.assertTrue(state.questComplete("xpq.dim.end_cities_3") && state.questComplete("xpq.exp.villages_5"),
                "restored evidence plus one new city and the fifth village type completes both quests");
        System.out.println("QUEST_CITY_RESTART_READ player=" + PLAYER_ID);
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
