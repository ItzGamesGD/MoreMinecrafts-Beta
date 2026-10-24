package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Real process interruption. The write phase terminates the JVM with {@link Runtime#halt} (no shutdown hooks, no saves)
 * at one injected transaction boundary; the read phase is a fresh JVM that reloads the same world and player from disk.
 * Inert unless {@code moreminecrafts.contract.crash.phase} is set, so the normal suite is unaffected.
 * Driven by scripts/contract_crash/run_matrix.sh.
 */
public final class ContractCrashGameTests implements CustomTestMethodInvoker {
    private static final UUID PLAYER_ID = UUID.fromString("7c0f6e0a-3f5b-4f55-9a53-0c0ffee00bc3");
    private static final String CONTRACT_ID = "11111111-2222-4333-8444-555555555555";
    private static final Set<String> ROLLED_BACK = Set.of("BEFORE_PREPARE", "AFTER_PREPARE", "AFTER_CONSUME", "AFTER_REWARD");

    private static ContractData contract() {
        return new ContractData(CONTRACT_ID, ContractData.Tier.BEGINNER, ContractData.Status.ACCEPTED,
                List.of(new ContractData.Objective(ContractData.Category.DELIVERY, "minecraft:wheat", 8, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1));
    }

    private static ServerPlayer player(GameTestHelper ctx, boolean loadFromDisk) {
        ServerLevel level = ctx.getLevel();
        var profile = new com.mojang.authlib.GameProfile(PLAYER_ID, "contract-crash-player");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        if (loadFromDisk) {
            var saved = level.getServer().getPlayerList().loadPlayerData(player.nameAndId());
            ctx.assertTrue(saved.isPresent(), "player data was written by the previous process");
            player.load(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess(), saved.orElseThrow()));
        }
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    @GameTest(maxTicks = 400)
    public void crashRestartScenario(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.contract.crash.phase");
        if (phase == null) {
            ctx.succeed();
            return;
        }
        String point = System.getProperty("moreminecrafts.contract.crash.point", "NONE");
        boolean overflow = Boolean.getBoolean("moreminecrafts.contract.crash.overflow");
        if ("PERSIST".equals(point)) {
            if ("write".equals(phase)) persistWrite(ctx);
            else persistRead(ctx);
            return;
        }
        if ("write".equals(phase)) write(ctx, point, overflow);
        else read(ctx, point, overflow);
    }

    private static final String PERSIST_ID = "22222222-3333-4444-8555-666666666666";
    private static final java.nio.file.Path PERSIST_MANIFEST = java.nio.file.Path.of("contract-persist-fixture.txt");

    /** Accepted contract with progress plus an unrolled trade placeholder, saved by a process that then exits. */
    private void persistWrite(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = player(ctx, false);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, ContractStacks.create(new ContractData(PERSIST_ID, ContractData.Tier.BEGINNER,
                ContractData.Status.ACCEPTED, List.of(new ContractData.Objective(ContractData.Category.COMBAT, "minecraft:zombie", 3, 2)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1))));
        player.getInventory().setItem(1, ContractTrades.offer(ContractData.Tier.INTERMEDIATE).getResult().copy());
        ContractMaterializer.materialize(player);
        ContractData rolled = ContractStacks.read(player.getInventory().getItem(1));
        ctx.assertTrue(rolled != null, "placeholder rolled before saving");
        try {
            java.nio.file.Files.writeString(PERSIST_MANIFEST, rolled.id() + "," + rolled.objectives().size() + "," + rolled.reward().item()
                    + "," + rolled.reward().count() + "," + rolled.reward().emeralds());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        level.getServer().getPlayerList().saveAll();
        ContractLedger.flush(level);
        System.out.println("CONTRACT_PERSIST_WRITE rolled=" + rolled.id());
        ctx.succeed();
    }

    private void persistRead(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        String[] manifest;
        try {
            manifest = java.nio.file.Files.readString(PERSIST_MANIFEST).trim().split(",");
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        ServerPlayer player = player(ctx, true);
        ContractData progressed = ContractStacks.read(player.getInventory().getItem(0));
        ctx.assertTrue(progressed != null && progressed.id().equals(PERSIST_ID) && progressed.status() == ContractData.Status.ACCEPTED
                && progressed.objectives().getFirst().progress() == 2, "accepted contract and its progress survived the restart");
        ContractData rolled = ContractStacks.read(player.getInventory().getItem(1));
        ctx.assertTrue(rolled != null && rolled.id().equals(manifest[0]) && rolled.status() == ContractData.Status.OFFERED
                && rolled.objectives().size() == Integer.parseInt(manifest[1]) && rolled.reward().item().equals(manifest[2])
                && rolled.reward().count() == Integer.parseInt(manifest[3]) && rolled.reward().emeralds() == Integer.parseInt(manifest[4]),
                "the rolled objectives and reward are fixed, not re-rolled by the restart");
        ContractMaterializer.materialize(player);
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(1)).id().equals(manifest[0]), "materialization is idempotent after restart");
        // Transfer after the restart and finish on the receiving player.
        ServerPlayer receiver = ctx.makeMockServerPlayerInLevel();
        receiver.getInventory().setItem(0, player.getInventory().removeItemNoUpdate(0));
        ContractService.advance(receiver, ContractData.Category.COMBAT, "minecraft:zombie");
        ctx.assertTrue(ContractService.act(receiver, 0, PERSIST_ID, ContractService.Action.COMPLETE), "receiver completes the transferred contract");
        ctx.assertTrue(receiver.getInventory().countItem(Items.EMERALD) == 5, "paid once");
        ctx.assertTrue(ContractLedger.of(level).get(PERSIST_ID).state() == ContractLedger.State.COMMITTED, "committed");
        System.out.println("CONTRACT_CRASH_READ point=PERSIST overflow=false ledgerBefore=ABSENT ledgerAfter=COMMITTED emeralds=5 iron=1 result=PASS");
        ctx.succeed();
    }

    private void write(GameTestHelper ctx, String point, boolean overflow) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = player(ctx, false);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, ContractStacks.create(contract()));
        // 64 wheat keeps its slot occupied after 8 are consumed, so only the contract's slot frees up and one reward overflows.
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT, wheatBefore(overflow)));
        if (overflow) for (int slot = 2; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        // Baseline: the durable state a real autosave would have left before the redemption began.
        level.getServer().getPlayerList().saveAll();
        ContractLedger.flush(level);
        System.out.println("CONTRACT_CRASH_BASELINE point=" + point + " overflow=" + overflow);
        // A joined player's chunk is entity-ticking; the harness's fake connection needs the chunk forced for the same effect.
        level.setChunkForced(player.chunkPosition().x(), player.chunkPosition().z(), true);
        ctx.runAfterDelay(60, () -> {
            ContractTransactions.setFaultHook(boundary -> {
                System.out.println("CONTRACT_CRASH_BOUNDARY reached=" + boundary + " target=" + point); // evidence of the path taken
                if (boundary.name().equals(point)) {
                    System.out.println("CONTRACT_CRASH_HALT point=" + boundary);
                    System.out.flush();
                    Runtime.getRuntime().halt(23);
                }
            });
            boolean completed = ContractService.act(player, 0, CONTRACT_ID, ContractService.Action.COMPLETE);
            ctx.assertTrue(completed, "redemption ran to the end when no boundary halts it");
            ContractTransactions.setFaultHook(null);
            level.getServer().getPlayerList().saveAll();
            ContractLedger.flush(level);
            System.out.println("CONTRACT_CRASH_WRITE_COMPLETED point=" + point);
            ctx.succeed();
        });
    }

    private static int wheatBefore(boolean overflow) { return overflow ? 64 : 8; }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().countItem(item);
    }

    private static int droppedIron(GameTestHelper ctx, ServerPlayer player) {
        int total = 0;
        for (ItemEntity entity : ctx.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(64.0)))
            if (entity.getItem().is(Items.IRON_INGOT)) total += entity.getItem().getCount();
        return total;
    }

    private void read(GameTestHelper ctx, String point, boolean overflow) {
        ServerLevel level = ctx.getLevel();
        ContractLedger.Entry durableBefore = ContractLedger.of(level).get(CONTRACT_ID);
        String before = durableBefore == null ? "ABSENT" : durableBefore.state().name();
        ServerPlayer player = player(ctx, true); // the join event runs recovery
        level.setChunkForced(player.chunkPosition().x(), player.chunkPosition().z(), true);
        ctx.runAfterDelay(140, () -> {
            ContractLedger ledger = ContractLedger.of(level);
            boolean rolledBack = ROLLED_BACK.contains(point);
            String expectedBefore = switch (point) {
                case "BEFORE_PREPARE" -> "ABSENT";
                case "AFTER_PREPARE", "AFTER_CONSUME", "AFTER_REWARD", "AFTER_PLAYER_SAVE", "AFTER_OVERFLOW_SPAWN", "AFTER_OVERFLOW_FLUSH",
                 "AFTER_OVERFLOW_DROP" -> "PENDING";
                default -> "COMMITTED";
            };
            ctx.assertTrue(before.equals(expectedBefore), "durable ledger before recovery was " + before + ", expected " + expectedBefore);

            ContractLedger.Entry after = ledger.get(CONTRACT_ID);
            boolean contractPresent = ContractStacks.read(player.getInventory().getItem(0)) != null;
            int emeralds = count(player, Items.EMERALD);
            int iron = count(player, Items.IRON_INGOT) + droppedIron(ctx, player);
            int wheat = count(player, Items.WHEAT);
            if (rolledBack) {
                ctx.assertTrue(after == null, "unfinished transaction was rolled back");
                ctx.assertTrue(contractPresent && wheat == wheatBefore(overflow) && emeralds == 0 && iron == 0, "pre-transaction state intact: contract=" + contractPresent
                        + " wheat=" + wheat + " emeralds=" + emeralds + " iron=" + iron);
                ctx.assertTrue(ContractService.act(player, 0, CONTRACT_ID, ContractService.Action.COMPLETE), "contract is redeemable after recovery");
                ctx.assertTrue(ledger.get(CONTRACT_ID).state() == ContractLedger.State.COMMITTED, "committed after the retry");
                emeralds = count(player, Items.EMERALD);
                iron = count(player, Items.IRON_INGOT) + droppedIron(ctx, player);
            } else {
                ctx.assertTrue(after != null && after.state() == ContractLedger.State.COMMITTED, "transaction rolled forward to committed: " + after);
                ctx.assertTrue(!contractPresent && wheat == wheatBefore(overflow) - 8, "inputs stay consumed: contract=" + contractPresent + " wheat=" + wheat);
            }
            ctx.assertTrue(emeralds == 5, "emeralds granted exactly once: " + emeralds);
            ctx.assertTrue(iron == 1, "bonus reward granted exactly once (inventory plus dropped): " + iron);
            ctx.assertTrue(player.getAttachedOrCreate(ContractAttachments.RECEIPTS).list().isEmpty(), "no receipt left behind");
            if (overflow) ctx.assertTrue(count(player, Items.IRON_INGOT) == 0 && droppedIron(ctx, player) == 1,
                    "the bonus reward really overflowed and was dropped exactly once: inventory=" + count(player, Items.IRON_INGOT)
                            + " dropped=" + droppedIron(ctx, player));

            // A surviving copy of the contract can never pay again.
            player.getInventory().setItem(2, ContractStacks.create(contract()));
            player.getInventory().setItem(3, new ItemStack(Items.WHEAT, 8));
            ctx.assertTrue(!ContractService.act(player, 2, CONTRACT_ID, ContractService.Action.COMPLETE), "duplicate copy refused");
            ContractTransactions.recover(player);
            ContractTransactions.recover(player);
            ctx.assertTrue(count(player, Items.EMERALD) == emeralds && count(player, Items.IRON_INGOT) + droppedIron(ctx, player) == iron,
                    "repeated recovery changed nothing");
            System.out.println("CONTRACT_CRASH_READ point=" + point + " overflow=" + overflow + " ledgerBefore=" + before
                    + " ledgerAfter=" + ledger.get(CONTRACT_ID).state() + " emeralds=" + emeralds + " iron=" + iron + " result=PASS");
            ctx.succeed();
        });
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
