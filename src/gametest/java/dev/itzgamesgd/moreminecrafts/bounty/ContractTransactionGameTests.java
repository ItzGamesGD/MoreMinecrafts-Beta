package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** In-process transaction and recovery checks. Process-kill evidence is in ContractCrashGameTests. */
public final class ContractTransactionGameTests implements CustomTestMethodInvoker {
    private static ContractData accepted(String id) {
        return new ContractData(id, ContractData.Tier.BEGINNER, ContractData.Status.ACCEPTED,
                List.of(new ContractData.Objective(ContractData.Category.DELIVERY, "minecraft:wheat", 8, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1));
    }

    private static ServerPlayer holder(GameTestHelper ctx, String id) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.getInventory().setItem(0, ContractStacks.create(accepted(id)));
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT, 8));
        return player;
    }

    /** Iron dropped by one specific transaction, so neighboring tests cannot be counted. */
    private static int droppedIron(GameTestHelper ctx, ServerPlayer player, String tx) {
        int total = 0;
        for (ItemEntity entity : ctx.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(64.0)))
            if (entity.getItem().is(Items.IRON_INGOT) && tx.equals(entity.getAttached(ContractAttachments.DROP_TX)))
                total += entity.getItem().getCount();
        return total;
    }

    @GameTest
    public void completionCommitsLedgerAndLeavesNoReceipt(GameTestHelper ctx) {
        String id = UUID.randomUUID().toString();
        ServerPlayer player = holder(ctx, id);
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.COMPLETE), "completes");
        ContractLedger.Entry entry = ContractLedger.of(ctx.getLevel()).get(id);
        ctx.assertTrue(entry != null && entry.state() == ContractLedger.State.COMMITTED, "ledger committed");
        ctx.assertTrue(player.getAttachedOrCreate(ContractAttachments.RECEIPTS).list().isEmpty(), "receipt cleared");
        ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 5 && player.getInventory().countItem(Items.IRON_INGOT) == 1,
                "rewards granted once");
        ctx.assertTrue(player.getInventory().countItem(Items.WHEAT) == 0, "materials consumed");
        player.getInventory().setItem(3, ContractStacks.create(accepted(id)));
        player.getInventory().setItem(4, new ItemStack(Items.WHEAT, 8));
        ctx.assertTrue(!ContractService.act(player, 3, id, ContractService.Action.COMPLETE), "copy cannot redeem again");
        ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 5, "no second payout");
        ctx.succeed();
    }

    @GameTest
    public void pendingWithoutReceiptRollsBackAndAllowsExactlyOneRedemption(GameTestHelper ctx) {
        String id = UUID.randomUUID().toString();
        ServerPlayer player = holder(ctx, id);
        ContractLedger ledger = ContractLedger.of(ctx.getLevel());
        ctx.assertTrue(ledger.prepare(id, "tx-1", player.getStringUUID()), "write-ahead entry");
        ctx.assertTrue(!ContractService.act(player, 0, id, ContractService.Action.COMPLETE), "pending blocks redemption");
        ctx.assertTrue(!ContractService.act(player, 0, id, ContractService.Action.ABANDON), "pending blocks abandonment");
        ContractTransactions.recover(player);
        ctx.assertTrue(ledger.get(id) == null, "player data predates the transaction: rolled back");
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.COMPLETE), "contract is redeemable again");
        ctx.assertTrue(!ContractService.act(player, 0, id, ContractService.Action.COMPLETE), "but only once");
        ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 5, "single payout");
        ctx.succeed();
    }

    @GameTest
    public void pendingWithReceiptRollsForwardOnceAndRecoveryIsIdempotent(GameTestHelper ctx) {
        ContractTransactions.setEntityFlush(level -> { }); // durability is covered by the real-process matrix
        try {
            pendingWithReceiptRollsForwardOnceAndRecoveryIsIdempotent_body(ctx);
        } finally {
            ContractTransactions.setEntityFlush(null);
        }
    }

    private void pendingWithReceiptRollsForwardOnceAndRecoveryIsIdempotent_body(GameTestHelper ctx) {
        String id = UUID.randomUUID().toString();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.getInventory().setItem(0, new ItemStack(Items.EMERALD, 5));
        ContractLedger ledger = ContractLedger.of(ctx.getLevel());
        ledger.prepare(id, "tx-2", player.getStringUUID());
        player.setAttached(ContractAttachments.RECEIPTS, ContractAttachments.Receipts.empty().with(
                new ContractAttachments.Receipt("tx-2", id, List.of(new ItemStack(Items.IRON_INGOT, 1)), false)));
        ContractTransactions.recover(player);
        ctx.assertTrue(ledger.get(id).state() == ContractLedger.State.COMMITTED, "rolled forward");
        ctx.assertTrue(player.getAttachedOrCreate(ContractAttachments.RECEIPTS).list().isEmpty(), "receipt cleared");
        ctx.assertTrue(droppedIron(ctx, player, "tx-2") == 1, "overflow delivered once: " + droppedIron(ctx, player, "tx-2"));
        ContractTransactions.recover(player);
        ContractTransactions.recover(player);
        ctx.assertTrue(droppedIron(ctx, player, "tx-2") == 1, "repeated recovery never redelivers");
        ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 5, "inventory untouched by recovery");
        ctx.succeed();
    }

    @GameTest
    public void recoveryDeliversOverflowEvenWithAFullInventory(GameTestHelper ctx) {
        ContractTransactions.setEntityFlush(level -> { }); // durability is covered by the real-process matrix
        try {
            recoveryDeliversOverflowEvenWithAFullInventory_body(ctx);
        } finally {
            ContractTransactions.setEntityFlush(null);
        }
    }

    private void recoveryDeliversOverflowEvenWithAFullInventory_body(GameTestHelper ctx) {
        String id = UUID.randomUUID().toString();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        ContractLedger.of(ctx.getLevel()).prepare(id, "tx-3", player.getStringUUID());
        player.setAttached(ContractAttachments.RECEIPTS, ContractAttachments.Receipts.empty().with(
                new ContractAttachments.Receipt("tx-3", id, List.of(new ItemStack(Items.IRON_INGOT, 1)), false)));
        ContractTransactions.recover(player);
        ctx.assertTrue(droppedIron(ctx, player, "tx-3") == 1, "full inventory does not block or lose the overflow: " + droppedIron(ctx, player, "tx-3"));
        ctx.assertTrue(ContractLedger.of(ctx.getLevel()).get(id).state() == ContractLedger.State.COMMITTED, "committed");
        ctx.succeed();
    }

    @GameTest
    public void twoPlayersOnOneIdAreSerializedByThePendingEntry(GameTestHelper ctx) {
        String id = UUID.randomUUID().toString();
        ServerPlayer first = holder(ctx, id);
        ServerPlayer second = holder(ctx, id);
        ContractLedger ledger = ContractLedger.of(ctx.getLevel());
        ledger.prepare(id, "tx-4", first.getStringUUID());
        ctx.assertTrue(!ContractService.act(second, 0, id, ContractService.Action.COMPLETE), "second holder blocked while first is pending");
        ContractTransactions.recover(second);
        ctx.assertTrue(ledger.get(id) != null, "another player's recovery never resolves someone else's entry");
        ContractTransactions.recover(first);
        ctx.assertTrue(ledger.get(id) == null, "owner's recovery rolls it back");
        ctx.assertTrue(ContractService.act(second, 0, id, ContractService.Action.COMPLETE), "then the second holder may redeem");
        ctx.assertTrue(!ContractService.act(first, 0, id, ContractService.Action.COMPLETE), "and the first no longer can");
        ctx.assertTrue(first.getInventory().countItem(Items.EMERALD) + second.getInventory().countItem(Items.EMERALD) == 5,
                "exactly one payout across both players");
        ctx.succeed();
    }

    @GameTest
    public void transferDuringPendingCompletionCannotRedeem(GameTestHelper ctx) {
        String id = UUID.randomUUID().toString();
        ServerPlayer owner = holder(ctx, id);
        ServerPlayer receiver = ctx.makeMockServerPlayerInLevel();
        ContractLedger.of(ctx.getLevel()).prepare(id, "tx-5", owner.getStringUUID());
        receiver.getInventory().setItem(0, owner.getInventory().removeItemNoUpdate(0));
        receiver.getInventory().setItem(1, new ItemStack(Items.WHEAT, 8));
        ctx.assertTrue(!ContractService.act(receiver, 0, id, ContractService.Action.COMPLETE), "transferred copy blocked while pending");
        ContractTransactions.recover(owner);
        ctx.assertTrue(ContractService.act(receiver, 0, id, ContractService.Action.COMPLETE), "allowed once the entry is rolled back");
        ctx.succeed();
    }

    @GameTest
    public void legacyAndCurrentLedgerFormatsDecode(GameTestHelper ctx) {
        ListTag legacy = new ListTag();
        legacy.add(StringTag.valueOf("legacy-id"));
        ContractLedger decoded = ContractLedger.CODEC.parse(NbtOps.INSTANCE, legacy).getOrThrow();
        ctx.assertTrue(decoded.get("legacy-id").state() == ContractLedger.State.COMMITTED, "bare IDs read as committed");
        decoded.prepare("new-id", "tx", "player");
        var encoded = ContractLedger.CODEC.encodeStart(NbtOps.INSTANCE, decoded).getOrThrow();
        ContractLedger again = ContractLedger.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        ctx.assertTrue(again.get("new-id").state() == ContractLedger.State.PENDING && again.size() == 2, "round trip keeps states");
        ctx.assertTrue(!again.abort("legacy-id", ""), "committed entries are never aborted");
        ctx.succeed();
    }

    @GameTest
    public void receiptsSurviveSerialization(GameTestHelper ctx) {
        var receipts = ContractAttachments.Receipts.empty().with(new ContractAttachments.Receipt(
                "tx", "id", List.of(new ItemStack(Items.IRON_INGOT, 3)), false));
        var ops = ctx.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var restored = ContractAttachments.Receipts.CODEC.parse(ops,
                ContractAttachments.Receipts.CODEC.encodeStart(ops, receipts).getOrThrow()).getOrThrow();
        ContractAttachments.Receipt receipt = restored.find("tx");
        ctx.assertTrue(receipt != null && !receipt.delivered() && receipt.overflow().size() == 1
                && receipt.overflow().getFirst().getCount() == 3, "receipt round trip");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
