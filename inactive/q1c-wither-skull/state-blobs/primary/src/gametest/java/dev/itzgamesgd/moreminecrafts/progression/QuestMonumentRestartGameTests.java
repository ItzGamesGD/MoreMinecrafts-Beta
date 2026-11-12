package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;

/**
 * Monument participant evidence, offline credit and completion across a clean two-JVM restart
 * ({@code moreminecrafts.questmonument.restart.phase=write|read}; a no-op without the property).
 */
public final class QuestMonumentRestartGameTests implements CustomTestMethodInvoker {
    private static final UUID ELDER_HITTER = UUID.fromString("e1d3a001-0000-4000-8000-000000000001");
    private static final UUID BEAM_VICTIM = UUID.fromString("e1d3a001-0000-4000-8000-000000000002");
    private static final UUID LEFT_BEFORE = UUID.fromString("e1d3a001-0000-4000-8000-000000000003");
    private static final ChunkPos CHUNK = new ChunkPos(700, 700);

    private static int cleared(ServerPlayer player) {
        return QuestRules.distinctCount(KnowledgeRuntime.state(player), QuestMonumentParticipation.CLEARED);
    }

    private static MonumentRewardData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MonumentRewardData.TYPE);
    }

    private static ElderGuardian elder(ServerLevel level, BlockPos at) {
        ElderGuardian elder = EntityTypes.ELDER_GUARDIAN.create(level, EntitySpawnReason.COMMAND);
        elder.setNoAi(true);
        elder.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        level.addFreshEntity(elder);
        return elder;
    }

    private static ServerPlayer survivor(GameTestHelper ctx, UUID id, String name, boolean fromDisk) {
        ServerPlayer player = QuestPersistentPlayers.join(ctx, id, name, fromDisk);
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        return player;
    }

    @GameTest(maxTicks = 300)
    public void participantsAndCompletionSurviveACleanRestart(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.questmonument.restart.phase");
        if (phase == null) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        var monument = QuestStructureFixture.register(level, "minecraft:monument", CHUNK);
        long key = monument.start().getChunkPos().pack();
        BlockPos inside = monument.inside();
        ctx.runAfterDelay(25, () -> {
            if (phase.equals("write")) {
                ServerPlayer hitter = survivor(ctx, ELDER_HITTER, "xpq-monument-hitter", false);
                ServerPlayer victim = survivor(ctx, BEAM_VICTIM, "xpq-monument-victim", false);
                victim.setPos(inside.getX() + 0.5D, inside.getY(), inside.getZ() + 0.5D);
                ElderGuardian first = elder(level, inside);
                ElderGuardian second = elder(level, inside.offset(1, 0, 1));
                Guardian beam = EntityTypes.GUARDIAN.create(level, EntitySpawnReason.COMMAND);
                beam.setNoAi(true);
                beam.setPos(inside.getX(), inside.getY(), inside.getZ());
                level.addFreshEntity(beam);
                first.hurtServer(level, hitter.damageSources().playerAttack(hitter), 1F);
                ctx.assertTrue(victim.hurtServer(level, level.damageSources().indirectMagic(beam, beam), 1F),
                        "the guardian damaged the survival player");
                finishWriteWhenEldersVisible(ctx, level, monument, key, hitter, victim, first, second, 0);
                return;
            }
            ctx.assertTrue(data(level).participant(key, ELDER_HITTER) != null && data(level).participant(key, ELDER_HITTER).elderDamage()
                    && data(level).participant(key, BEAM_VICTIM) != null && !data(level).participant(key, BEAM_VICTIM).elderDamage()
                    && data(level).hasPendingCredit(LEFT_BEFORE, key) && !data(level).isClaimed(key),
                    "participant evidence and the pending offline credit were restored from disk");
            ServerPlayer hitter = survivor(ctx, ELDER_HITTER, "xpq-monument-hitter", true);
            ServerPlayer victim = survivor(ctx, BEAM_VICTIM, "xpq-monument-victim", true);
            ServerPlayer finisher = ctx.makeMockServerPlayerInLevel();
            finisher.setPos(inside.getX() + 0.5D, inside.getY(), inside.getZ() + 0.5D);
            // The monument's surviving elder was saved with its chunk and must still be there.
            var area = new net.minecraft.world.phys.AABB(monument.start().getBoundingBox().minX(), monument.start().getBoundingBox().minY(),
                    monument.start().getBoundingBox().minZ(), monument.start().getBoundingBox().maxX() + 1,
                    monument.start().getBoundingBox().maxY() + 1, monument.start().getBoundingBox().maxZ() + 1);
            var living = level.getEntitiesOfClass(ElderGuardian.class, area.inflate(4), ElderGuardian::isAlive);
            ctx.assertTrue(living.size() == 1, "the surviving elder guardian was restored: " + living.size());
            living.getFirst().hurtServer(level, finisher.damageSources().playerAttack(finisher), 100_000F);
            ctx.assertTrue(data(level).isClaimed(key), "the last elder completes the monument after the restart");
            ctx.assertTrue(cleared(hitter) == 1 && cleared(victim) == 1 && cleared(finisher) == 1,
                    "stored participants and the finisher are credited once");
            ServerPlayer returning = survivor(ctx, LEFT_BEFORE, "xpq-monument-left", false);
            ctx.assertTrue(cleared(returning) == 1 && !data(level).hasPendingCredit(LEFT_BEFORE, key),
                    "the offline participant's stored credit applies at login");
            ElderGuardian replay = elder(level, inside);
            replay.hurtServer(level, hitter.damageSources().playerAttack(hitter), 100_000F);
            ctx.assertTrue(cleared(hitter) == 1 && cleared(returning) == 1, "replaying the monument adds nothing");
            System.out.println("QUEST_MONUMENT_RESTART_READ monument=" + key);
            ctx.succeed();
        });
    }


    /** Wait for the entity manager to register freshly spawned guardians before testing completion. */
    private static void finishWriteWhenEldersVisible(GameTestHelper ctx, ServerLevel level,
            QuestStructureFixture.Registered monument, long key, ServerPlayer hitter, ServerPlayer victim,
            ElderGuardian first, ElderGuardian second, int waited) {
        var box = monument.start().getBoundingBox();
        var area = new net.minecraft.world.phys.AABB(box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        var visible = level.getEntitiesOfClass(ElderGuardian.class, area, ElderGuardian::isAlive);
        if (!visible.contains(first) || !visible.contains(second)) {
            ctx.assertTrue(first.isAlive() && second.isAlive(), "an elder died before entity registration");
            ctx.assertTrue(waited < 60, "elders were not query-visible within 60 ticks: count=" + visible.size()
                    + " first=" + visible.contains(first) + " second=" + visible.contains(second));
            ctx.runAfterDelay(1, () -> finishWriteWhenEldersVisible(
                    ctx, level, monument, key, hitter, victim, first, second, waited + 1));
            return;
        }
        ctx.assertTrue(!data(level).isClaimed(key), "monument must be unclaimed before first elder kill");
        second.hurtServer(level, hitter.damageSources().playerAttack(hitter), 100_000F);
        var after = level.getEntitiesOfClass(ElderGuardian.class, area, ElderGuardian::isAlive);
        ctx.assertTrue(first.isAlive() && after.contains(first) && !data(level).isClaimed(key),
                "tracked living elder blocks completion: firstAlive=" + first.isAlive()
                + " firstVisible=" + after.contains(first) + " claimed=" + data(level).isClaimed(key));
        data(level).addPendingCredit(LEFT_BEFORE, key);
        ctx.assertTrue(data(level).participant(key, hitter.getUUID()).elderDamage()
                && data(level).participant(key, victim.getUUID()) != null, "evidence recorded before saving");
        level.getServer().getPlayerList().saveAll();
        level.getChunkSource().save(true);
        level.getServer().overworld().getDataStorage().saveAndJoin();
        System.out.println("QUEST_MONUMENT_RESTART_WRITE monument=" + key
                + " entityRegistrationWaitTicks=" + waited);
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
