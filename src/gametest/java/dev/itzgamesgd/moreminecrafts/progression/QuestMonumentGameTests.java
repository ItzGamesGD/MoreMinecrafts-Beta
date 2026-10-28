package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;

/** C6: Ocean Monument participant attribution on top of the existing T2 completion path. */
public final class QuestMonumentGameTests implements CustomTestMethodInvoker {
    private static final String CLEARED = QuestMonumentParticipation.CLEARED;
    private static final java.util.List<BlockPos> HEARTS = new java.util.concurrent.CopyOnWriteArrayList<>();

    static {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item
                    && item.getItem().getItem() == dev.itzgamesgd.moreminecrafts.registry.ModItems.HEART_OF_CURRENTS)
                HEARTS.add(item.blockPosition());
        });
    }

    /** Hearts of Currents created inside this monument's bounds (tests run concurrently, so count locally). */
    private static int hearts(QuestStructureFixture.Registered monument) {
        return (int) HEARTS.stream().filter(p -> monument.start().getBoundingBox().isInside(p)).count();
    }

    private static int cleared(ServerPlayer player) {
        return QuestRules.distinctCount(KnowledgeRuntime.state(player), CLEARED);
    }

    /**
     * Fixture slots used by this class; each slot is a separate monument start chunk. Slot 9 stays unused because the
     * deferred-completion test's 140-block monument from slot 8 reaches into it and unloads that chunk.
     */
    private static final int SLOTS = 11;
    private static final int SLOT_SPACING = 8;
    private static net.minecraft.server.MinecraftServer baseServer;
    private static int baseX;

    /**
     * Monument start chunk for {@code slot}. Monument state is world-persistent, so a reused test world still holds
     * the previous run's claims and evidence at the old chunks; the fixture moves past any slot range that does.
     */
    private static ChunkPos chunk(ServerLevel level, int slot) {
        if (baseServer != level.getServer()) {
            baseServer = level.getServer();
            baseX = firstUnusedBaseX(data(level), 520);
        }
        return new ChunkPos(baseX + slot * SLOT_SPACING, 520);
    }

    static int firstUnusedBaseX(MonumentRewardData data, int startX) {
        int base = startX;
        while (!slotsUnused(data, base)) base += SLOTS * SLOT_SPACING;
        return base;
    }

    private static boolean slotsUnused(MonumentRewardData data, int base) {
        for (int slot = 0; slot < SLOTS; slot++) {
            long key = new ChunkPos(base + slot * SLOT_SPACING, 520).pack();
            if (data.isClaimed(key) || data.pending(key) != null
                    || !data.qualifiedParticipants(key, 0L, Long.MAX_VALUE).isEmpty()) return false;
        }
        return true;
    }

    private static ElderGuardian elder(ServerLevel level, BlockPos at) {
        ElderGuardian elder = EntityTypes.ELDER_GUARDIAN.create(level, EntitySpawnReason.COMMAND);
        elder.setNoAi(true);
        elder.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        level.addFreshEntity(elder);
        return elder;
    }

    private static Guardian guardian(ServerLevel level, BlockPos at) {
        Guardian guardian = EntityTypes.GUARDIAN.create(level, EntitySpawnReason.COMMAND);
        guardian.setNoAi(true);
        guardian.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        level.addFreshEntity(guardian);
        return guardian;
    }

    /** A survival player whose client has "loaded", so vanilla lets damage reach them. */
    private static ServerPlayer vulnerable(ServerLevel level) {
        ServerPlayer player = QuestPersistentPlayers.mock(level, GameType.SURVIVAL);
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        return player;
    }

    private static void place(ServerPlayer player, BlockPos at) {
        player.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
    }

    private static void hit(ServerLevel level, ServerPlayer attacker, net.minecraft.world.entity.LivingEntity victim, float damage) {
        victim.damageCooldownTime = 0;
        victim.hurtServer(level, attacker.damageSources().playerAttack(attacker), damage);
    }

    private static MonumentRewardData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MonumentRewardData.TYPE);
    }

    private static long key(QuestStructureFixture.Registered monument) { return monument.start().getChunkPos().pack(); }

    /** Runs {@code body} at least 25 ticks after registration, once the monuments' chunks tick entities. */
    private static void later(GameTestHelper ctx, Runnable body, QuestStructureFixture.Registered... monuments) {
        QuestStructureFixture.loadEntityTickingNeighbourhood(ctx.getLevel(), monuments);
        ctx.runAfterDelay(25, () -> QuestStructureFixture.whenEntitiesTicking(ctx, body, monuments));
    }

    @GameTest(maxTicks = 120)
    public void everyRealCombatParticipantIsCreditedAndNobodyElse(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var monument = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 0));
        later(ctx, () -> {
            ServerPlayer melee = ctx.makeMockServerPlayerInLevel();
            ServerPlayer archer = ctx.makeMockServerPlayerInLevel();
            ServerPlayer hurt = vulnerable(level);
            ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
            ServerPlayer faraway = ctx.makeMockServerPlayerInLevel();
            BlockPos inside = monument.inside();
            place(bystander, inside);
            place(faraway, inside.offset(500, 0, 0));
            ElderGuardian boss = elder(level, inside);
            Guardian minion = guardian(level, inside.offset(1, 0, 1));
            hit(level, melee, boss, 1F);
            ctx.assertTrue(data(level).participant(key(monument), melee.getUUID()) != null, "melee elder damage is recorded");
            place(archer, inside.offset(60, 0, 0)); // shoots from far outside the bounds
            Arrow arrow = new Arrow(level, archer, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            boss.damageCooldownTime = 0;
            boss.hurtServer(level, level.damageSources().arrow(arrow, archer), 1F);
            place(hurt, inside);
            ctx.assertTrue(hurt.hurtServer(level, level.damageSources().indirectMagic(minion, minion), 1F),
                    "the guardian's beam damaged the survival player");
            ServerPlayer outsideCombat = ctx.makeMockServerPlayerInLevel();
            place(outsideCombat, inside.offset(200, 0, 0));
            Guardian wandering = guardian(level, inside.offset(200, 0, 3));
            hit(level, outsideCombat, wandering, 1F);
            hit(level, melee, boss, 100_000F);
            ctx.assertTrue(hearts(monument) == 0, "the superseded Monument Heart of Currents reward is no longer produced");
            ctx.assertTrue(cleared(melee) == 1 && cleared(archer) == 1 && cleared(hurt) == 1,
                    "melee, projectile and guardian-damaged participants are all credited: "
                    + cleared(melee) + "/" + cleared(archer) + "/" + cleared(hurt));
            ctx.assertTrue(cleared(bystander) == 0 && cleared(faraway) == 0 && cleared(outsideCombat) == 0,
                    "presence alone, distance and combat outside the bounds are rejected");
            ctx.succeed();
        }, monument);
    }

    @GameTest(maxTicks = 120)
    public void mobProjectilesAndWrongMonumentEvidenceAreRejected(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var first = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 1));
        var second = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 2));
        later(ctx, () -> {
            ServerPlayer player = ctx.makeMockServerPlayerInLevel();
            ServerPlayer other = ctx.makeMockServerPlayerInLevel();
            ElderGuardian inFirst = elder(level, first.inside());
            ElderGuardian inSecond = elder(level, second.inside());
            var skeleton = EntityTypes.SKELETON.create(level, EntitySpawnReason.COMMAND);
            skeleton.setPos(first.inside().getX(), first.inside().getY(), first.inside().getZ());
            level.addFreshEntity(skeleton);
            Arrow mobArrow = new Arrow(level, skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            inFirst.damageCooldownTime = 0;
            inFirst.hurtServer(level, level.damageSources().arrow(mobArrow, skeleton), 1F);
            ctx.assertTrue(data(level).participant(key(first), player.getUUID()) == null
                    && data(level).qualifiedParticipants(key(first), level.getGameTime(), 12_000L).isEmpty(),
                    "a mob-owned arrow creates no participant");
            hit(level, player, inFirst, 1F); // elder damage in the first monument
            place(other, second.inside()); // stands in the second monument, hits a guardian of the first
            Guardian foreign = guardian(level, first.inside().offset(1, 0, 1));
            hit(level, other, foreign, 1F);
            ctx.assertTrue(data(level).participant(key(first), player.getUUID()) != null
                    && data(level).participant(key(second), player.getUUID()) == null,
                    "evidence is stored for the monument that contains the elder only");
            ctx.assertTrue(data(level).participant(key(second), other.getUUID()) == null,
                    "combat against a guardian inside another monument is not evidence for the monument the player stands in");
            hit(level, other, inSecond, 100_000F); // another player completes the second monument
            ctx.assertTrue(cleared(player) == 0 && cleared(other) == 1,
                    "the first monument's participant is not credited for the second monument");
            ctx.succeed();
        }, first, second);
    }

    @GameTest
    public void evidenceWindowAndElderSpecialCaseAreExact(GameTestHelper ctx) {
        MonumentRewardData data = new MonumentRewardData();
        java.util.UUID inWindow = new java.util.UUID(0, 1), tooOld = new java.util.UUID(0, 2), elder = new java.util.UUID(0, 3);
        long now = 1_000_000L;
        data.recordParticipation(77L, inWindow, now - 12_000L, false);
        data.recordParticipation(77L, tooOld, now - 12_001L, false);
        data.recordParticipation(77L, elder, 1L, true);
        var qualified = data.qualifiedParticipants(77L, now, 12_000L);
        ctx.assertTrue(qualified.contains(inWindow) && !qualified.contains(tooOld) && qualified.contains(elder),
                "12,000 ticks is inside, 12,001 outside, and Elder Guardian damage never expires");
        data.recordParticipation(77L, tooOld, now - 5L, false);
        ctx.assertTrue(data.qualifiedParticipants(77L, now, 12_000L).contains(tooOld), "newer combat refreshes the window");
        data.recordParticipation(77L, elder, now, false);
        ctx.assertTrue(data.participant(77L, elder).elderDamage(), "a later combat tick never erases Elder Guardian evidence");
        ctx.succeed();
    }

    @GameTest(maxTicks = 120)
    public void evidenceAroundTheWindowDecidesRealCompletion(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var monument = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 3));
        later(ctx, () -> {
            ServerPlayer recent = ctx.makeMockServerPlayerInLevel();
            ServerPlayer stale = ctx.makeMockServerPlayerInLevel();
            ServerPlayer elderHistory = ctx.makeMockServerPlayerInLevel();
            ServerPlayer finisher = ctx.makeMockServerPlayerInLevel();
            long now = level.getGameTime();
            data(level).recordParticipation(key(monument), recent.getUUID(), now - 12_000L, false);
            data(level).recordParticipation(key(monument), stale.getUUID(), now - 12_001L, false);
            data(level).recordParticipation(key(monument), elderHistory.getUUID(), 5L, true);
            ElderGuardian boss = elder(level, monument.inside());
            hit(level, finisher, boss, 100_000F);
            ctx.assertTrue(cleared(recent) == 1 && cleared(stale) == 0 && cleared(elderHistory) == 1 && cleared(finisher) == 1,
                    "window edge, expired combat, ancient elder damage and the finisher are decided exactly");
            ctx.succeed();
        }, monument);
    }

    @GameTest(maxTicks = 160)
    public void offlineParticipantsAreCreditedOnceAtLogin(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var monument = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 4));
        later(ctx, () -> {
            java.util.UUID id = java.util.UUID.fromString("5d6e7f80-9a1b-4c2d-8e3f-405162738401");
            ServerPlayer leaver = QuestPersistentPlayers.join(ctx, id, "xpq-monument-leaver", false);
            ServerPlayer finisher = ctx.makeMockServerPlayerInLevel();
            ElderGuardian boss = elder(level, monument.inside());
            hit(level, leaver, boss, 1F);
            level.getServer().getPlayerList().saveAll();
            level.getServer().getPlayerList().remove(leaver);
            hit(level, finisher, boss, 100_000F);
            ctx.assertTrue(cleared(finisher) == 1 && data(level).hasPendingCredit(id, key(monument)),
                    "the online finisher is credited and the departed participant has a pending credit");
            ServerPlayer back = QuestPersistentPlayers.join(ctx, id, "xpq-monument-leaver", true);
            ctx.assertTrue(cleared(back) == 1 && !data(level).hasPendingCredit(id, key(monument)),
                    "login applies the pending credit and clears it");
            level.getServer().getPlayerList().saveAll();
            level.getServer().getPlayerList().remove(back);
            ServerPlayer again = QuestPersistentPlayers.join(ctx, id, "xpq-monument-leaver", true);
            ctx.assertTrue(cleared(again) == 1, "repeated logins add nothing");
            ctx.succeed();
        }, monument);
    }

    @GameTest(maxTicks = 160)
    public void sameMonumentReplayAndThreeDistinctMonumentsAreCountedCorrectly(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var a = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 5));
        var b = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 6));
        var c = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 7));
        later(ctx, () -> {
            ServerPlayer player = ctx.makeMockServerPlayerInLevel();
            ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
            hit(level, player, elder(level, a.inside()), 100_000F);
            ctx.assertTrue(cleared(player) == 1, "first monument counts");
            hit(level, player, elder(level, a.inside()), 100_000F); // same monument cleared again
            QuestMonumentParticipation.completed(level, data(level), key(a)); // duplicate completion event
            QuestMonumentParticipation.credit(player, key(a));
            ctx.assertTrue(cleared(player) == 1 && hearts(a) == 0,
                    "replaying the same monument adds no credit and no second Heart of Currents: cleared=" + cleared(player)
                    + " hearts=" + hearts(a));
            hit(level, player, elder(level, b.inside()), 100_000F);
            ctx.assertTrue(cleared(player) == 2 && !KnowledgeRuntime.state(player).questComplete("xpq.boss.monuments_3"),
                    "two monuments are two");
            hit(level, player, elder(level, c.inside()), 100_000F);
            ctx.assertTrue(cleared(player) == 3 && cleared(bystander) == 0,
                    "three genuinely different monuments are three");
            ctx.assertTrue(QuestCatalog.byId("xpq.boss.monument_clear") == null && QuestCatalog.byId("xpq.boss.monuments_3") == null,
                    "the VERIFY monument quests remain unregistered");
            ctx.succeed();
        }, a, b, c);
    }

    @GameTest(maxTicks = 900)
    public void deferredCompletionAfterARealChunkUnloadKeepsParticipantsUntilTheChunksReload(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var monument = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 8));
        var piece = monument.firstPiece();
        var box = new net.minecraft.world.level.levelgen.structure.BoundingBox(
                piece.minX(), piece.minY(), piece.minZ(), piece.minX() + 140, piece.maxY(), piece.maxZ());
        ChunkPos remote = new ChunkPos(box.maxX() >> 4, box.minZ() >> 4);
        // Load the monument's remote chunk first, then let Minecraft genuinely unload it again.
        level.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.FORCED, remote, 0);
        level.getChunk(remote.x(), remote.z());
        level.getChunkSource().removeTicketWithRadius(net.minecraft.server.level.TicketType.FORCED, remote, 0);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                level.getChunkSource().removeTicketWithRadius(net.minecraft.server.level.TicketType.UNKNOWN,
                        new ChunkPos(remote.x() + dx, remote.z() + dz), 0);
        awaitUnloaded(ctx, level, monument, box, remote, 0);
    }

    private void awaitUnloaded(GameTestHelper ctx, ServerLevel level, QuestStructureFixture.Registered monument,
            net.minecraft.world.level.levelgen.structure.BoundingBox box, ChunkPos remote, int waited) {
        if (level.hasChunk(remote.x(), remote.z())) {
            ctx.assertTrue(waited < 600, "the remote monument chunk did not unload");
            ctx.runAfterDelay(1, () -> awaitUnloaded(ctx, level, monument, box, remote, waited + 1));
            return;
        }
        ServerPlayer participant = ctx.makeMockServerPlayerInLevel();
        long key = key(monument);
        data(level).recordParticipation(key, participant.getUUID(), level.getGameTime(), true);
        data(level).defer(key, box, net.minecraft.world.phys.Vec3.atCenterOf(monument.inside()));
        ctx.runAfterDelay(10, () -> {
            ctx.assertTrue(data(level).pending(key) != null && cleared(participant) == 0 && !data(level).isClaimed(key),
                    "completion is deferred while a chunk of the monument is unloaded");
            for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
                level.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.FORCED, new ChunkPos(x, box.minZ() >> 4), 0);
                level.getChunk(x, box.minZ() >> 4);
            }
            ctx.runAfterDelay(40, () -> {
                ctx.assertTrue(data(level).pending(key) == null && data(level).isClaimed(key) && cleared(participant) == 1,
                        "once its chunks reload the deferred completion finishes and credits the stored participant");
                ctx.succeed();
            });
        });
    }

    /** QM-01 regression: no fixed delay; the reward is checked the moment the fixture chunks tick entities. */
    @GameTest(maxTicks = 120)
    public void rewardIsObservableAsSoonAsTheMonumentChunksTickEntities(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var monument = QuestStructureFixture.register(level, "minecraft:monument", chunk(level, 10));
        QuestStructureFixture.loadEntityTickingNeighbourhood(level, monument);
        QuestStructureFixture.whenEntitiesTicking(ctx, () -> {
            ServerPlayer finisher = ctx.makeMockServerPlayerInLevel();
            hit(level, finisher, elder(level, monument.inside()), 100_000F);
            ctx.assertTrue(data(level).isClaimed(key(monument)) && cleared(finisher) == 1, "the monument completes");
            ctx.assertTrue(hearts(monument) == 0, "no Heart of Currents is created by Monument completion: hearts="
                    + hearts(monument));
            ctx.succeed();
        }, monument);
    }

    /** QM-01 regression: saved claims, pending completions and evidence from an earlier run move the fixture slots. */
    @GameTest
    public void reusedWorldMonumentStateMovesTheFixtureToUnusedSlots(GameTestHelper ctx) {
        MonumentRewardData data = new MonumentRewardData();
        int stride = SLOTS * SLOT_SPACING;
        ctx.assertTrue(firstUnusedBaseX(data, 520) == 520, "a fresh world keeps the original fixture chunks");
        data.claim(new ChunkPos(520 + 3 * SLOT_SPACING, 520).pack());
        ctx.assertTrue(firstUnusedBaseX(data, 520) == 520 + stride, "a claim left by a previous run moves the slots");
        data.defer(new ChunkPos(520 + stride, 520).pack(), new net.minecraft.world.level.levelgen.structure.BoundingBox(0, 0, 0, 1, 1, 1),
                net.minecraft.world.phys.Vec3.ZERO);
        data.recordParticipation(new ChunkPos(520 + 2 * stride + SLOT_SPACING, 520).pack(), new java.util.UUID(0, 9), 7L, false);
        ctx.assertTrue(firstUnusedBaseX(data, 520) == 520 + 3 * stride,
                "pending completions and stored evidence also mark a slot range as used");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
