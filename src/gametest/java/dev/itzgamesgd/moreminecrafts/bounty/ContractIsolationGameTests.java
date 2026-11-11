package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;

/**
 * BC-03 validation hardening: multiplayer isolation, acceptance idempotency, and persistence through a real
 * disconnect and reconnect of a player held by the server's PlayerList. Process-kill durability is covered separately
 * by ContractCrashGameTests and scripts/contract_crash/run_matrix.sh.
 */
public final class ContractIsolationGameTests implements CustomTestMethodInvoker {
    private static ContractData accepted(String id, ContractData.Category category, String target, int required) {
        return new ContractData(id, ContractData.Tier.BEGINNER, ContractData.Status.ACCEPTED,
                List.of(new ContractData.Objective(category, target, required, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1));
    }

    private static ContractData offered(String id) {
        return new ContractData(id, ContractData.Tier.BEGINNER, ContractData.Status.OFFERED,
                List.of(new ContractData.Objective(ContractData.Category.COMBAT, "minecraft:zombie", 3, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1));
    }

    private static int progress(ServerPlayer player, int slot) {
        return ContractStacks.read(player.getInventory().getItem(slot)).objectives().getFirst().progress();
    }

    /** A real zombie, killed through the same damage and death events gameplay uses. */
    private static Zombie zombie(GameTestHelper ctx) {
        Zombie zombie = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        zombie.setNoAi(true);
        return zombie;
    }

    private static void hit(GameTestHelper ctx, Zombie zombie, ServerPlayer attacker, float amount) {
        zombie.damageCooldownTime = 0; // each hit must land in full, not only its excess over the last one
        ctx.assertTrue(zombie.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().playerAttack(attacker), amount),
                "the hit landed");
    }

    @GameTest
    public void killCreditGoesOnlyToTheKillerAndQualifyingAssistants(GameTestHelper ctx) {
        ServerPlayer killer = ctx.makeMockServerPlayerInLevel();
        ServerPlayer assistant = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        for (ServerPlayer player : List.of(killer, assistant, bystander))
            player.getInventory().setItem(0, ContractStacks.create(accepted(UUID.randomUUID().toString(),
                    ContractData.Category.COMBAT, "minecraft:zombie", 5)));

        Zombie solo = zombie(ctx);
        hit(ctx, solo, killer, 1000);
        ctx.assertTrue(solo.isDeadOrDying(), "the first zombie died");
        ctx.assertTrue(progress(killer, 0) == 1, "the killer is credited: " + progress(killer, 0));
        ctx.assertTrue(progress(assistant, 0) == 0 && progress(bystander, 0) == 0,
                "players who did not damage the mob are not credited");

        Zombie shared = zombie(ctx);
        hit(ctx, shared, assistant, 6);  // about 29% of 20 max health after armor: qualifies
        hit(ctx, shared, bystander, 2);  // about 9%: below the 20% threshold
        hit(ctx, shared, killer, 1000);
        ctx.assertTrue(shared.isDeadOrDying(), "the second zombie died");
        ctx.assertTrue(progress(killer, 0) == 2, "final blow credited again: " + progress(killer, 0));
        ctx.assertTrue(progress(assistant, 0) == 1, "a 20%+ assistant is credited: " + progress(assistant, 0));
        ctx.assertTrue(progress(bystander, 0) == 0, "a sub-threshold contributor is not credited: " + progress(bystander, 0));
        ctx.succeed();
    }

    @GameTest
    public void structureEntryAndDeliveryInventoryNeverCrossBetweenPlayers(GameTestHelper ctx) {
        ServerPlayer explorer = ctx.makeMockServerPlayerInLevel();
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        String explorerId = UUID.randomUUID().toString();
        String otherId = UUID.randomUUID().toString();
        explorer.getInventory().setItem(0, ContractStacks.create(accepted(explorerId,
                ContractData.Category.EXPLORATION, "minecraft:village_plains", 1)));
        other.getInventory().setItem(0, ContractStacks.create(accepted(otherId,
                ContractData.Category.EXPLORATION, "minecraft:village_plains", 1)));
        ContractService.enterStructure(explorer, "minecraft:village_plains", "overworld:1");
        ctx.assertTrue(progress(explorer, 0) == 1, "the entering player is credited");
        ctx.assertTrue(progress(other, 0) == 0, "another player's identical contract is untouched");

        String deliveryId = UUID.randomUUID().toString();
        explorer.getInventory().setItem(1, ContractStacks.create(accepted(deliveryId,
                ContractData.Category.DELIVERY, "minecraft:wheat", 8)));
        other.getInventory().setItem(1, new ItemStack(Items.WHEAT, 64));
        ctx.assertTrue(!ContractService.act(explorer, 1, deliveryId, ContractService.Action.COMPLETE),
                "another player's materials never satisfy a delivery");
        ctx.assertTrue(other.getInventory().countItem(Items.WHEAT) == 64, "and are never consumed");
        ctx.assertTrue(!ContractService.act(other, 1, deliveryId, ContractService.Action.COMPLETE),
                "a player cannot act on a contract that is in someone else's inventory");
        explorer.getInventory().setItem(2, new ItemStack(Items.WHEAT, 8));
        ctx.assertTrue(ContractService.act(explorer, 1, deliveryId, ContractService.Action.COMPLETE), "own materials complete it");
        ctx.assertTrue(explorer.getInventory().countItem(Items.EMERALD) == 5 && other.getInventory().countItem(Items.EMERALD) == 0,
                "only the redeeming player is paid");
        ctx.succeed();
    }

    @GameTest
    public void acceptanceIsIdempotentAndNeverResetsOrConsumes(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(offered(id)));
        player.getInventory().setItem(1, ContractStacks.create(offered(id))); // a copy with the same identity

        ContractService.advance(player, ContractData.Category.COMBAT, "minecraft:zombie");
        ctx.assertTrue(progress(player, 0) == 0, "an offered contract records nothing");
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.ACCEPT), "first acceptance succeeds");
        ctx.assertTrue(player.getInventory().getItem(0).getCount() == 1, "acceptance does not consume the item");
        ContractService.advance(player, ContractData.Category.COMBAT, "minecraft:zombie");
        ContractData afterProgress = ContractStacks.read(player.getInventory().getItem(0));
        ctx.assertTrue(afterProgress.objectives().getFirst().progress() == 1, "accepted contract records progress");

        ctx.assertTrue(!ContractService.act(player, 0, id, ContractService.Action.ACCEPT), "second acceptance is refused");
        ctx.assertTrue(!ContractService.act(player, 0, id, ContractService.Action.ACCEPT), "and stays refused");
        ctx.assertTrue(afterProgress.equals(ContractStacks.read(player.getInventory().getItem(0))),
                "repeated acceptance leaves status and progress unchanged");
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(1)).status() == ContractData.Status.OFFERED,
                "accepting one item never accepts another copy");
        ctx.assertTrue(progress(player, 1) == 0, "the unaccepted copy gained nothing");

        ItemStack placeholder = new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.BOUNTY_CONTRACT);
        placeholder.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PENDING_CONTRACT, ContractData.Tier.BEGINNER);
        player.getInventory().setItem(2, placeholder);
        ctx.assertTrue(!ContractService.act(player, 2, id, ContractService.Action.ACCEPT), "an unrolled placeholder cannot be accepted");
        ctx.assertTrue(!ContractService.act(player, 0, UUID.randomUUID().toString(), ContractService.Action.ACCEPT),
                "a mismatched id is refused");

        // A redeemed identity: completion of any later-accepted copy is refused and pays nothing.
        ContractService.advance(player, ContractData.Category.COMBAT, "minecraft:zombie");
        ContractService.advance(player, ContractData.Category.COMBAT, "minecraft:zombie");
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.COMPLETE), "the accepted item completes");
        ContractService.act(player, 1, id, ContractService.Action.ACCEPT);
        for (int kill = 0; kill < 3; kill++) ContractService.advance(player, ContractData.Category.COMBAT, "minecraft:zombie");
        ctx.assertTrue(!ContractService.act(player, 1, id, ContractService.Action.COMPLETE), "the copy can never redeem the same identity");
        ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 5, "exactly one payout");
        ctx.succeed();
    }

    /** A player held by the PlayerList, so disconnect saves it and reconnect loads it from disk and fires the join event. */
    private static ServerPlayer connect(GameTestHelper ctx, UUID id, boolean loadFromDisk) {
        ServerLevel level = ctx.getLevel();
        var profile = new com.mojang.authlib.GameProfile(id, "contract-" + id.toString().substring(0, 8));
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        if (loadFromDisk) {
            var saved = level.getServer().getPlayerList().loadPlayerData(player.nameAndId());
            ctx.assertTrue(saved.isPresent(), "player data was saved on disconnect");
            player.load(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess(), saved.orElseThrow()));
        }
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    private static void disconnect(GameTestHelper ctx, ServerPlayer player) {
        ctx.getLevel().getServer().getPlayerList().remove(player);
        ctx.assertTrue(ctx.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == null, "player is offline");
    }

    private static int slotOf(ServerPlayer player, String id) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ContractData data = ContractStacks.read(player.getInventory().getItem(slot));
            if (data != null && data.id().equals(id)) return slot;
        }
        return -1;
    }

    @GameTest
    public void reconnectKeepsAcceptedProgressAndRedemptionStaysSingle(GameTestHelper ctx) {
        UUID playerId = UUID.randomUUID();
        String id = UUID.randomUUID().toString();
        ServerPlayer player = connect(ctx, playerId, false);
        player.getInventory().setItem(0, ContractStacks.create(new ContractData(id, ContractData.Tier.BEGINNER,
                ContractData.Status.OFFERED, List.of(
                        new ContractData.Objective(ContractData.Category.COMBAT, "minecraft:zombie", 3, 0),
                        new ContractData.Objective(ContractData.Category.EXPLORATION, "minecraft:mineshaft", 2, 0)),
                new ContractData.Reward(7, "minecraft:gold_ingot", 9))));
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.ACCEPT), "accept");
        ContractService.advance(player, ContractData.Category.COMBAT, "minecraft:zombie");
        ContractService.advance(player, ContractData.Category.COMBAT, "minecraft:zombie");
        ContractService.enterStructure(player, "minecraft:mineshaft", "overworld:7");
        ContractData before = ContractStacks.read(player.getInventory().getItem(0));
        disconnect(ctx, player);

        ServerPlayer back = connect(ctx, playerId, true);
        ContractData restored = ContractStacks.read(back.getInventory().getItem(slotOf(back, id)));
        ctx.assertTrue(before.equals(restored), "identity, status, counters, visited instances and reward survive reconnect");
        ContractService.enterStructure(back, "minecraft:mineshaft", "overworld:7");
        ctx.assertTrue(ContractStacks.read(back.getInventory().getItem(slotOf(back, id))).objectives().get(1).progress() == 1,
                "an instance visited before the reconnect is not counted twice");
        ctx.assertTrue(!ContractService.act(back, slotOf(back, id), id, ContractService.Action.ACCEPT), "acceptance is not repeatable after reconnect");
        ContractService.advance(back, ContractData.Category.COMBAT, "minecraft:zombie");
        ContractService.enterStructure(back, "minecraft:mineshaft", "overworld:8");
        ItemStack copy = back.getInventory().getItem(slotOf(back, id)).copy();
        ctx.assertTrue(ContractService.act(back, slotOf(back, id), id, ContractService.Action.COMPLETE), "completes after reconnect");
        disconnect(ctx, back);

        ServerPlayer again = connect(ctx, playerId, true);
        ctx.assertTrue(slotOf(again, id) < 0, "the redeemed contract is gone after another reconnect");
        ctx.assertTrue(again.getInventory().countItem(Items.EMERALD) == 7 && again.getInventory().countItem(Items.GOLD_INGOT) == 9,
                "the reward was saved exactly once");
        ctx.assertTrue(again.getAttachedOrCreate(ContractAttachments.RECEIPTS).list().isEmpty(), "no unresolved receipt");
        again.getInventory().setItem(5, copy);
        ctx.assertTrue(!ContractService.act(again, 5, id, ContractService.Action.COMPLETE), "a pre-redemption copy is refused after reconnect");
        ctx.assertTrue(again.getInventory().countItem(Items.EMERALD) == 7, "no second payout");
        disconnect(ctx, again);
        ctx.succeed();
    }

    @GameTest
    public void interruptedRedemptionRollsForwardWhenTheOwnerReconnects(GameTestHelper ctx) {
        UUID playerId = UUID.randomUUID();
        String id = UUID.randomUUID().toString();
        ServerPlayer player = connect(ctx, playerId, false);
        player.getInventory().setItem(0, ContractStacks.create(accepted(id, ContractData.Category.DELIVERY, "minecraft:wheat", 8)));
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT, 8));
        ContractTransactions.setFaultHook(point -> {
            if (point == ContractTransactions.Point.AFTER_PLAYER_SAVE) throw new IllegalStateException("interrupted after commit point");
        });
        boolean interrupted = false;
        try {
            ContractService.act(player, 0, id, ContractService.Action.COMPLETE);
        } catch (IllegalStateException expected) {
            interrupted = true;
        } finally {
            ContractTransactions.setFaultHook(null);
        }
        ctx.assertTrue(interrupted, "the redemption stopped at the commit point");
        ctx.assertTrue(ContractLedger.of(ctx.getLevel()).get(id).state() == ContractLedger.State.PENDING, "left pending");
        disconnect(ctx, player);

        ServerPlayer back = connect(ctx, playerId, true); // the join event runs recovery
        ContractLedger.Entry entry = ContractLedger.of(ctx.getLevel()).get(id);
        ctx.assertTrue(entry != null && entry.state() == ContractLedger.State.COMMITTED, "join recovery rolled forward: " + entry);
        ctx.assertTrue(back.getAttachedOrCreate(ContractAttachments.RECEIPTS).list().isEmpty(), "receipt cleared");
        ctx.assertTrue(back.getInventory().countItem(Items.EMERALD) == 5 && back.getInventory().countItem(Items.IRON_INGOT) == 1
                && back.getInventory().countItem(Items.WHEAT) == 0 && slotOf(back, id) < 0, "the committed result, exactly once");
        ContractTransactions.recover(back);
        ctx.assertTrue(back.getInventory().countItem(Items.EMERALD) == 5, "repeated recovery adds nothing");
        disconnect(ctx, back);
        ctx.succeed();
    }

    @GameTest
    public void interruptedBeforeTheCommitPointRollsBackWhenTheOwnerReconnects(GameTestHelper ctx) {
        UUID playerId = UUID.randomUUID();
        String id = UUID.randomUUID().toString();
        ServerPlayer player = connect(ctx, playerId, false);
        player.getInventory().setItem(0, ContractStacks.create(accepted(id, ContractData.Category.DELIVERY, "minecraft:wheat", 8)));
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT, 8));
        ContractTransactions.setFaultHook(point -> {
            if (point == ContractTransactions.Point.AFTER_PREPARE) throw new IllegalStateException("interrupted after prepare");
        });
        try {
            ContractService.act(player, 0, id, ContractService.Action.COMPLETE);
        } catch (IllegalStateException expected) {
            // the pending ledger entry is the only change
        } finally {
            ContractTransactions.setFaultHook(null);
        }
        ctx.assertTrue(!ContractService.act(player, 0, id, ContractService.Action.COMPLETE), "pending entry blocks a retry before recovery");
        disconnect(ctx, player);

        ServerPlayer back = connect(ctx, playerId, true);
        ctx.assertTrue(ContractLedger.of(ctx.getLevel()).get(id) == null, "join recovery rolled the unfinished entry back");
        ctx.assertTrue(back.getInventory().countItem(Items.EMERALD) == 0 && back.getInventory().countItem(Items.WHEAT) == 8,
                "inputs intact and nothing paid");
        ctx.assertTrue(ContractService.act(back, slotOf(back, id), id, ContractService.Action.COMPLETE), "redeemable once after recovery");
        ctx.assertTrue(back.getInventory().countItem(Items.EMERALD) == 5, "single payout");
        disconnect(ctx, back);
        ctx.succeed();
    }

    @GameTest
    public void committedLedgerIsWrittenToDiskAndDecodes(GameTestHelper ctx) throws Exception {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(accepted(id, ContractData.Category.DELIVERY, "minecraft:wheat", 4)));
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT, 4));
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.COMPLETE), "completes");
        Path root = ctx.getLevel().getServer().getWorldPath(LevelResource.ROOT);
        Path file;
        try (var files = Files.walk(root)) {
            file = files.filter(path -> path.getFileName().toString().equals("contract_redemptions.dat")).findFirst().orElse(null);
        }
        ctx.assertTrue(file != null, "the ledger file exists under " + root);
        CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        ContractLedger fromDisk = ContractLedger.CODEC.parse(NbtOps.INSTANCE, tag.get("data")).getOrThrow();
        ContractLedger.Entry entry = fromDisk.get(id);
        ctx.assertTrue(entry != null && entry.state() == ContractLedger.State.COMMITTED
                && entry.player().equals(player.getStringUUID()), "the on-disk ledger holds the committed redemption: " + entry);
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
