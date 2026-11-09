package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * C2 vault evidence durability. Without a phase property: player reload plus a real vault-chunk save, Minecraft
 * unload and reload in one process. With {@code moreminecrafts.questvault.restart.phase=write|read}: one JVM saves
 * players and vault chunk, a second JVM reopens them (a clean shutdown and restart, not a JVM kill).
 */
public final class QuestVaultRestartGameTests implements CustomTestMethodInvoker {
    private static final String NORMAL = "set.normal_vault_rewards";
    private static final String OMINOUS = "set.ominous_vault_rewards";
    private static final UUID PLAYER_ID = UUID.fromString("5b0c0d1e-6f1a-4a39-9d71-0b3a5c2f7e01");
    private static final UUID OTHER_ID = UUID.fromString("5b0c0d1e-6f1a-4a39-9d71-0b3a5c2f7e02");
    private static final ChunkPos CHUNK = new ChunkPos(240, 240);
    private static final int MAX_WAIT_TICKS = 600;
    private static final Set<LevelChunk> UNLOADED = Collections.newSetFromMap(new IdentityHashMap<>());

    static {
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            if (chunk.getPos().equals(CHUNK)) synchronized (UNLOADED) { UNLOADED.add(chunk); }
        });
    }

    private static BlockPos vaultPos(int index, boolean ominous) {
        return new BlockPos((CHUNK.x() << 4) + 2 + index * 2, 100, (CHUNK.z() << 4) + (ominous ? 8 : 4));
    }

    private static int seen(ServerPlayer player, String key) {
        return QuestRules.distinctCount(KnowledgeRuntime.state(player), key);
    }

    private static boolean complete(ServerPlayer player, String quest) {
        return KnowledgeRuntime.state(player).questComplete(quest);
    }

    private static void reward(ServerLevel level, ServerPlayer player, int index, boolean ominous) {
        BlockPos pos = vaultPos(index, ominous);
        QuestVaultFixture.place(level, pos, ominous, VaultState.ACTIVE);
        QuestVaultFixture.insert(level, pos, player, QuestVaultFixture.key(ominous));
    }

    /** Re-using an already rewarded vault is refused by vanilla's persisted rewarded-player list: key kept, no evidence. */
    private static void verifyVanillaStillRemembers(GameTestHelper ctx, ServerLevel level, ServerPlayer player,
            int index, boolean ominous) {
        ItemStack key = QuestVaultFixture.key(ominous);
        int before = seen(player, ominous ? OMINOUS : NORMAL);
        QuestVaultFixture.insert(level, vaultPos(index, ominous), player, key);
        ctx.assertTrue(!key.isEmpty(), "reloaded vault still refuses a previously rewarded player (key not consumed)");
        ctx.assertTrue(seen(player, ominous ? OMINOUS : NORMAL) == before, "no extra evidence after reload");
    }

    private static void writeFixture(GameTestHelper ctx, ServerLevel level, ServerPlayer player, ServerPlayer other) {
        for (int i = 0; i < 5; i++) reward(level, player, i, false);
        for (int i = 0; i < 3; i++) reward(level, player, i, true);
        QuestVaultFixture.insert(level, vaultPos(0, false), other, QuestVaultFixture.key(false));
        ctx.assertTrue(seen(player, NORMAL) == 5 && complete(player, "xpq.dim.trial_vaults_5")
                && seen(player, OMINOUS) == 3 && !complete(player, "xpq.dim.ominous_vaults_5"),
                "fixture player holds five normal and three ominous vault rewards");
        ctx.assertTrue(seen(other, NORMAL) == 1 && !complete(other, "xpq.dim.trial_vaults_5"),
                "second player sharing the first vault holds only their own evidence");
    }

    private static void verifyRestored(GameTestHelper ctx, ServerLevel level, ServerPlayer player, ServerPlayer other) {
        ctx.assertTrue(seen(player, NORMAL) == 5 && complete(player, "xpq.dim.trial_vaults_5")
                && seen(player, OMINOUS) == 3 && !complete(player, "xpq.dim.ominous_vaults_5"),
                "player evidence and completion restored");
        ctx.assertTrue(seen(other, NORMAL) == 1 && seen(other, OMINOUS) == 0, "second player's own evidence restored");
        verifyVanillaStillRemembers(ctx, level, player, 0, false);
        verifyVanillaStillRemembers(ctx, level, other, 0, false);
        verifyVanillaStillRemembers(ctx, level, player, 2, true);
        // Continued play after restore: two more ominous vaults finish the ominous quest, once.
        reward(level, player, 3, true);
        ctx.assertTrue(!complete(player, "xpq.dim.ominous_vaults_5"), "four ominous vaults are still insufficient");
        reward(level, player, 4, true);
        ctx.assertTrue(complete(player, "xpq.dim.ominous_vaults_5") && seen(player, OMINOUS) == 5,
                "restored evidence plus two new vaults completes the ominous quest");
        int xp = RawXp.available(player);
        ctx.assertTrue(QuestProgressService.claim(player, "xpq.dim.trial_vaults_5") == QuestProgressService.ClaimResult.CLAIMED
                && QuestProgressService.claim(player, "xpq.dim.trial_vaults_5") == QuestProgressService.ClaimResult.ALREADY_CLAIMED
                && RawXp.available(player) == xp + 500, "restored completion claims exactly once");
    }

    @GameTest(maxTicks = 1300)
    public void vaultEvidenceSurvivesReloadUnloadAndRestart(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        String phase = System.getProperty("moreminecrafts.questvault.restart.phase");
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, CHUNK, 0);
        level.getChunk(CHUNK.x(), CHUNK.z());
        if ("write".equals(phase)) {
            ServerPlayer player = QuestPersistentPlayers.join(ctx, PLAYER_ID, "xpq-vault-restart", false);
            ServerPlayer other = QuestPersistentPlayers.join(ctx, OTHER_ID, "xpq-vault-other", false);
            writeFixture(ctx, level, player, other);
            level.getServer().getPlayerList().saveAll();
            level.getChunkSource().save(true);
            System.out.println("QUEST_VAULT_RESTART_WRITE chunk=" + CHUNK + " player=" + PLAYER_ID);
            ctx.succeed();
            return;
        }
        if ("read".equals(phase)) {
            ServerPlayer player = QuestPersistentPlayers.join(ctx, PLAYER_ID, "xpq-vault-restart", true);
            ServerPlayer other = QuestPersistentPlayers.join(ctx, OTHER_ID, "xpq-vault-other", true);
            verifyRestored(ctx, level, player, other);
            System.out.println("QUEST_VAULT_RESTART_READ chunk=" + CHUNK + " player=" + PLAYER_ID);
            ctx.succeed();
            return;
        }
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        writeFixture(ctx, level, player, other);
        // Player reload: the record round-trips through the same save/load path as a disconnect.
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        player.saveWithoutId(out);
        var otherOut = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        other.saveWithoutId(otherOut);
        ServerPlayer reloaded = ctx.makeMockServerPlayerInLevel();
        reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
        ServerPlayer otherReloaded = ctx.makeMockServerPlayerInLevel();
        otherReloaded.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), otherOut.buildResult()));
        // Real chunk save, then Minecraft unloads and reloads the vault chunk.
        LevelChunk first = level.getChunk(CHUNK.x(), CHUNK.z());
        level.getChunkSource().save(true);
        level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, CHUNK, 0);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                level.getChunkSource().removeTicketWithRadius(TicketType.UNKNOWN, new ChunkPos(CHUNK.x() + dx, CHUNK.z() + dz), 0);
        awaitUnload(ctx, level, first, reloaded, otherReloaded, 0);
    }

    private static void awaitUnload(GameTestHelper ctx, ServerLevel level, LevelChunk first, ServerPlayer reloaded,
            ServerPlayer otherReloaded, int waited) {
        boolean unloaded;
        synchronized (UNLOADED) { unloaded = UNLOADED.contains(first); }
        if (unloaded) {
            level.getChunkSource().addTicketWithRadius(TicketType.FORCED, CHUNK, 0);
            LevelChunk second = level.getChunk(CHUNK.x(), CHUNK.z());
            ctx.assertTrue(second != first, "reload produced a new chunk instance (real unload)");
            verifyRestored(ctx, level, reloaded, otherReloaded);
            level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, CHUNK, 0);
            ctx.succeed();
            return;
        }
        ctx.assertTrue(waited < MAX_WAIT_TICKS, "vault chunk did not unload after its ticket was removed");
        ctx.runAfterDelay(1, () -> awaitUnload(ctx, level, first, reloaded, otherReloaded, waited + 1));
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
