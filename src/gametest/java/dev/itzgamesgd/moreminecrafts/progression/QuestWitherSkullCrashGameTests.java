package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 2-JVM forced-halt crash matrix for wither skeleton skull P3 transactions.
 * Tests 13 boundary scenarios matching scripts/contract_crash/run_matrix.sh pattern.
 */
public final class QuestWitherSkullCrashGameTests implements CustomTestMethodInvoker {
    private static final UUID PLAYER_A = UUID.fromString("b16cb186-70ce-4dcc-9bfa-d2728c3d4b01");
    private static final UUID PLAYER_B = UUID.fromString("b16cb186-70ce-4dcc-9bfa-d2728c3d4b02");
    private static final BlockPos CRASH_POS = new BlockPos(6408, 100, 6408);
    private static final java.nio.file.Path CRASH_FIXTURE = java.nio.file.Path.of("quest-skull-crash-fixture.txt");

    private static final ResourceKey<LootTable> ALWAYS_SKULL = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.parse("moreminecrafts-gametest:wither_skull_always"));

    private static int credited(ServerPlayer player) {
        return QuestRules.counter(KnowledgeRuntime.state(player), QuestWitherSkullProvenance.DROPS);
    }

    private static int skulls(ServerPlayer player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.WITHER_SKELETON_SKULL)) total += stack.getCount();
        }
        return total;
    }

    private static void touch(ItemEntity item, ServerPlayer player) {
        item.setNoPickUpDelay();
        item.playerTouch(player);
    }

    private static ItemEntity spawnGenuineSkull(GameTestHelper ctx, ServerPlayer killer) {
        ServerLevel level = ctx.getLevel();
        LivingEntity victim = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.COMMAND);
        victim.setPos(CRASH_POS.getX() + 0.5D, CRASH_POS.getY(), CRASH_POS.getZ() + 0.5D);
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        victim.saveWithoutId(out);
        var tag = out.buildResult();
        tag.putString("DeathLootTable", ALWAYS_SKULL.identifier().toString());
        victim.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        level.addFreshEntity(victim);
        victim.hurtServer(level, killer.damageSources().playerAttack(killer), 100_000F);

        var box = new net.minecraft.world.phys.AABB(CRASH_POS).inflate(4.0D);
        var items = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(Items.WITHER_SKELETON_SKULL));
        ctx.assertTrue(!items.isEmpty(), "genuine skull drop spawned");
        ItemEntity drop = items.getFirst();
        drop.setPos(CRASH_POS.getX() + 0.5D, CRASH_POS.getY(), CRASH_POS.getZ() + 0.5D);
        drop.setNoGravity(true);
        drop.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        return drop;
    }

    @GameTest(maxTicks = 600)
    public void crashRestartScenario(GameTestHelper ctx) throws Exception {
        String phase = System.getProperty("moreminecrafts.questskull.crash.phase");
        if (phase == null) {
            ctx.succeed();
            return;
        }
        String point = System.getProperty("moreminecrafts.questskull.crash.point", "NONE");
        boolean fullInventory = Boolean.getBoolean("moreminecrafts.questskull.crash.full_inv");

        ServerLevel level = ctx.getLevel();
        var chunk = new net.minecraft.world.level.ChunkPos(CRASH_POS.getX() >> 4, CRASH_POS.getZ() >> 4);
        level.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.FORCED, chunk, 0);
        level.getChunk(chunk.x(), chunk.z());

        if ("PERSIST".equals(point)) {
            if ("write".equals(phase)) persistWrite(ctx);
            else persistRead(ctx);
            return;
        }

        if ("write".equals(phase)) write(ctx, point, fullInventory);
        else read(ctx, point, fullInventory);
    }

    private void persistWrite(GameTestHelper ctx) throws Exception {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = QuestPersistentPlayers.join(ctx, PLAYER_A, "xpq-skull-crash-A", false);
        player.getInventory().clearContent();
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty());
        ItemEntity drop = spawnGenuineSkull(ctx, player);
        String token = QuestWitherSkullProvenance.getToken(drop);
        ctx.assertTrue(token != null, "spawned genuine drop carries a token");
        java.nio.file.Files.writeString(CRASH_FIXTURE, drop.getUUID().toString() + "," + token);

        touch(drop, player);
        ctx.assertTrue(credited(player) == 1 && skulls(player) == 1, "A picked up skull");
        level.getServer().getPlayerList().saveAll();
        QuestWitherSkullLedger.flush(level);
        System.out.println("SKULL_PERSIST_WRITE token=" + token);
        ctx.succeed();
    }

    private void persistRead(GameTestHelper ctx) throws Exception {
        ServerLevel level = ctx.getLevel();
        String[] fixture = java.nio.file.Files.readString(CRASH_FIXTURE).trim().split(",");
        String token = fixture[1];

        ServerPlayer playerA = QuestPersistentPlayers.join(ctx, PLAYER_A, "xpq-skull-crash-A", true);
        ServerPlayer playerB = QuestPersistentPlayers.join(ctx, PLAYER_B, "xpq-skull-crash-B", false);

        ctx.assertTrue(credited(playerA) == 1 && skulls(playerA) == 1, "A survived with credit");
        ctx.assertTrue(QuestWitherSkullLedger.of(level).get(token).state() == QuestWitherSkullLedger.State.COMMITTED, "committed");

        // Duplicate pickup attempt by B with an artificial entity carrying the same token
        ItemEntity fake = new ItemEntity(level, CRASH_POS.getX() + 0.5D, CRASH_POS.getY(), CRASH_POS.getZ() + 0.5D,
                new ItemStack(Items.WITHER_SKELETON_SKULL));
        fake.setAttached(QuestWitherSkullProvenance.ORIGIN_TOKEN, token);
        level.addFreshEntity(fake);
        touch(fake, playerB);

        ctx.assertTrue(credited(playerB) == 0, "B refused on duplicate token");
        System.out.println("SKULL_CRASH_READ point=PERSIST full_inv=false A=1 B=0 result=PASS");
        ctx.succeed();
    }

    private void write(GameTestHelper ctx, String point, boolean fullInventory) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = QuestPersistentPlayers.join(ctx, PLAYER_A, "xpq-skull-crash-A", false);
        player.getInventory().clearContent();
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty());

        if (fullInventory) {
            for (int i = 0; i < 36; i++) {
                player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            }
        }

        ItemEntity drop = spawnGenuineSkull(ctx, player);
        String token = QuestWitherSkullProvenance.getToken(drop);
        ctx.assertTrue(token != null, "genuine drop token present");

        try {
            java.nio.file.Files.writeString(CRASH_FIXTURE, drop.getUUID().toString() + "," + token);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }

        // Save world/chunk entities BEFORE pickup so the ground entity is durable
        level.save(null, true, false);

        ctx.runAfterDelay(10, () -> {
            QuestWitherSkullTransactions.setFaultHook(boundary -> {
                System.out.println("SKULL_CRASH_BOUNDARY reached=" + boundary + " target=" + point);
                if (boundary.name().equals(point)) {
                    System.out.println("SKULL_CRASH_HALT point=" + boundary);
                    System.out.flush();
                    Runtime.getRuntime().halt(77);
                }
            });

            touch(drop, player);

            if ("PLAYER_ONLY_SAVE".equals(point)) {
                // Scenario from PR #130: player file saved, ground entity NOT saved
                level.getServer().getPlayerList().saveAll();
                System.out.println("SKULL_CRASH_HALT point=PLAYER_ONLY_SAVE");
                System.out.flush();
                Runtime.getRuntime().halt(77);
            }

            if ("CHUNK_BEFORE_PLAYER".equals(point)) {
                // Chunk entity saved (removal saved), then halt BEFORE player save
                level.save(null, true, false);
                System.out.println("SKULL_CRASH_HALT point=CHUNK_BEFORE_PLAYER");
                System.out.flush();
                Runtime.getRuntime().halt(77);
            }

            QuestWitherSkullTransactions.setFaultHook(null);
            level.getServer().getPlayerList().saveAll();
            QuestWitherSkullLedger.flush(level);
            System.out.println("SKULL_CRASH_WRITE_COMPLETED point=" + point);
            ctx.succeed();
        });
    }

    private void read(GameTestHelper ctx, String point, boolean fullInventory) throws Exception {
        ServerLevel level = ctx.getLevel();
        String[] fixture = java.nio.file.Files.readString(CRASH_FIXTURE).trim().split(",");
        UUID expectedEntityUuid = UUID.fromString(fixture[0]);
        String token = fixture[1];

        QuestWitherSkullLedger.Entry durableBefore = QuestWitherSkullLedger.of(level).get(token);
        String before = durableBefore == null ? "ABSENT" : durableBefore.state().name();

        ServerPlayer playerA = QuestPersistentPlayers.join(ctx, PLAYER_A, "xpq-skull-crash-A", true);
        ServerPlayer playerB = QuestPersistentPlayers.join(ctx, PLAYER_B, "xpq-skull-crash-B", false);
        playerB.getInventory().clearContent();

        ctx.runAfterDelay(30, () -> {
            QuestWitherSkullLedger ledger = QuestWitherSkullLedger.of(level);
            int creditA = credited(playerA);
            int heldA = skulls(playerA);

            // Invariant 2 & 3: holds <=> credited
            ctx.assertTrue(creditA == heldA, "credit matches held skulls for A: credit=" + creditA + " held=" + heldA);

            // Look for restored ground entity
            var groundEntities = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(CRASH_POS).inflate(3),
                    e -> e.getUUID().equals(expectedEntityUuid));

            if (!groundEntities.isEmpty()) {
                ItemEntity survivingGround = groundEntities.getFirst();
                touch(survivingGround, playerB);
            }

            int creditB = credited(playerB);
            int heldB = skulls(playerB);

            // Invariant 1: credit(A) + credit(B) <= 1
            ctx.assertTrue(creditA + creditB <= 1, "double credit forbidden: A=" + creditA + " B=" + creditB);

            // Invariant 2 & 3 for B: holds <=> credited only if genuine
            // If B picked up duplicate uncredited skull, creditB must be 0
            if (creditA == 1) {
                ctx.assertTrue(creditB == 0, "B cannot receive credit when A was credited");
            }

            System.out.println("SKULL_CRASH_READ point=" + point + " full_inv=" + fullInventory
                    + " ledgerBefore=" + before + " ledgerAfter=" + (ledger.get(token) != null ? ledger.get(token).state() : "ABSENT")
                    + " A=" + creditA + " B=" + creditB + " heldA=" + heldA + " heldB=" + heldB + " result=PASS");
            ctx.succeed();
        });
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
