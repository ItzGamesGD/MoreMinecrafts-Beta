package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ContractGameTests implements CustomTestMethodInvoker {
    private static ContractData contract(String id, ContractData.Category category, String target, int count) {
        return new ContractData(id, ContractData.Tier.BEGINNER, ContractData.Status.OFFERED,
                List.of(new ContractData.Objective(category, target, count, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1));
    }

    private static int slotOf(ServerPlayer player, String id) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ContractData data = ContractStacks.read(player.getInventory().getItem(slot));
            if (data != null && data.id().equals(id)) return slot;
        }
        return -1;
    }

    private static boolean act(ServerPlayer player, String id, ContractService.Action action) {
        return ContractService.act(player, slotOf(player, id), id, action);
    }

    @GameTest
    public void transferPreservesItemBoundProgress(GameTestHelper ctx) {
        ServerPlayer giver = ctx.makeMockServerPlayerInLevel();
        ServerPlayer receiver = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        ItemStack stack = ContractStacks.create(contract(id, ContractData.Category.COMBAT, "minecraft:zombie", 2));
        giver.getInventory().setItem(0, stack);
        ctx.assertTrue(act(giver, id, ContractService.Action.ACCEPT), "accept succeeds");
        ContractService.advance(giver, ContractData.Category.COMBAT, "minecraft:zombie");
        ItemStack moved = giver.getInventory().removeItemNoUpdate(0);
        receiver.getInventory().setItem(0, moved);
        ctx.assertTrue(ContractStacks.read(receiver.getInventory().getItem(0)).objectives().getFirst().progress() == 1,
                "transfer keeps progress");
        ContractService.advance(receiver, ContractData.Category.COMBAT, "minecraft:zombie");
        ctx.assertTrue(act(receiver, id, ContractService.Action.COMPLETE), "recipient redeems");
        ctx.assertTrue(receiver.getInventory().countItem(Items.EMERALD) == 5, "reward granted once");
        ctx.succeed();
    }

    @GameTest
    public void duplicateIdentityAndDeliveryCompetitionDoNotDoublePay(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        ItemStack original = ContractStacks.create(contract(id, ContractData.Category.DELIVERY, "minecraft:wheat", 8));
        player.getInventory().setItem(0, original);
        player.getInventory().setItem(2, new ItemStack(Items.WHEAT, 8));
        ctx.assertTrue(act(player, id, ContractService.Action.ACCEPT), "accept succeeds");
        player.getInventory().setItem(1, player.getInventory().getItem(0).copy());
        ctx.assertTrue(act(player, id, ContractService.Action.COMPLETE), "first copy redeems");
        ctx.assertTrue(!act(player, id, ContractService.Action.COMPLETE), "duplicate cannot redeem");
        ctx.assertTrue(player.getInventory().countItem(Items.WHEAT) == 0
                && player.getInventory().countItem(Items.EMERALD) == 5, "materials and reward used once");
        ctx.succeed();
    }

    @GameTest
    public void competingDeliveryContractsSecondWaitsForMaterialsAgain(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(contract(first, ContractData.Category.DELIVERY, "minecraft:wheat", 8)));
        player.getInventory().setItem(1, ContractStacks.create(contract(second, ContractData.Category.DELIVERY, "minecraft:wheat", 8)));
        player.getInventory().setItem(2, new ItemStack(Items.WHEAT, 8));
        ctx.assertTrue(act(player, first, ContractService.Action.ACCEPT), "first accepted");
        ctx.assertTrue(act(player, second, ContractService.Action.ACCEPT), "second accepted");
        ctx.assertTrue(act(player, first, ContractService.Action.COMPLETE), "first completes with shared stock");
        ctx.assertTrue(!act(player, second, ContractService.Action.COMPLETE), "second refused after stock is consumed");
        ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 5, "exactly one reward paid");
        ctx.succeed();
    }

    @GameTest
    public void offeredAbandonDestroysOnlySelectedSlotWithoutReward(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(contract(id, ContractData.Category.COMBAT, "minecraft:zombie", 2)));
        player.getInventory().setItem(1, ContractStacks.create(contract(id, ContractData.Category.COMBAT, "minecraft:zombie", 2)));
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.ABANDON), "offered abandon succeeds");
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(0)) == null, "selected slot destroyed");
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(1)) != null, "duplicate copy in other slot kept");
        ctx.assertTrue(!ContractService.act(player, 0, id, ContractService.Action.ABANDON), "repeated request refused");
        ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 0, "abandon grants no reward");
        ctx.succeed();
    }

    @GameTest
    public void staleSlotCannotActOnAnotherItem(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(3, ContractStacks.create(contract(id, ContractData.Category.COMBAT, "minecraft:zombie", 2)));
        ctx.assertTrue(!ContractService.act(player, 0, id, ContractService.Action.ABANDON), "wrong slot refused");
        ctx.assertTrue(!ContractService.act(player, -1, id, ContractService.Action.ABANDON), "negative slot refused");
        ctx.assertTrue(!ContractService.act(player, 99, id, ContractService.Action.ABANDON), "out-of-range slot refused");
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(3)) != null, "contract untouched");
        ctx.succeed();
    }

    @GameTest
    public void redeemedCopyCannotBeAbandoned(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(contract(id, ContractData.Category.DELIVERY, "minecraft:wheat", 4)));
        player.getInventory().setItem(2, new ItemStack(Items.WHEAT, 4));
        ctx.assertTrue(act(player, id, ContractService.Action.ACCEPT), "accept succeeds");
        player.getInventory().setItem(1, player.getInventory().getItem(0).copy());
        ctx.assertTrue(act(player, id, ContractService.Action.COMPLETE), "redeem succeeds");
        ctx.assertTrue(!ContractService.act(player, 1, id, ContractService.Action.ABANDON), "redeemed copy cannot be abandoned");
        ctx.succeed();
    }

    @GameTest
    public void pendingPlaceholderMaterializesOnPickup(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ItemStack pending = new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.BOUNTY_CONTRACT);
        pending.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PENDING_CONTRACT, ContractData.Tier.INTERMEDIATE);
        player.getInventory().setItem(0, pending);
        ctx.assertTrue(ContractStacks.read(pending) == null, "placeholder carries no objectives before pickup");
        ContractMaterializer.materialize(player);
        ContractData data = ContractStacks.read(player.getInventory().getItem(0));
        ctx.assertTrue(data != null, "placeholder becomes a contract");
        ctx.assertTrue(data.tier() == ContractData.Tier.INTERMEDIATE, "tier preserved");
        ctx.assertTrue(data.status() == ContractData.Status.OFFERED && !data.objectives().isEmpty(), "fixed objectives rolled");
        ctx.assertTrue(player.getInventory().getItem(0).get(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.PENDING_CONTRACT) == null,
                "pending marker removed");
        ctx.succeed();
    }

    @GameTest
    public void contractSurvivesItemSerializationRoundTrip(GameTestHelper ctx) {
        ContractData original = contract(UUID.randomUUID().toString(), ContractData.Category.COMBAT, "minecraft:zombie", 3);
        ItemStack stack = ContractStacks.create(original);
        var ops = ctx.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        ItemStack restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, stack).getOrThrow()).getOrThrow();
        ctx.assertTrue(original.equals(ContractStacks.read(restored)), "contract identity and progress survive serialization");
        ctx.succeed();
    }

    @GameTest
    public void fullInventoryOverflowPaysRewardExactlyOnce(GameTestHelper ctx) {
        ContractTransactions.setEntityFlush(level -> { }); // durability is covered by the real-process matrix
        try {
            fullInventoryOverflow(ctx);
        } finally {
            ContractTransactions.setEntityFlush(null);
        }
    }

    private void fullInventoryOverflow(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(contract(id, ContractData.Category.DELIVERY, "minecraft:wheat", 8)));
        // 64 wheat stays in its slot after 8 are consumed, so only the contract's own slot frees up and one reward must overflow.
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT, 64));
        for (int slot = 2; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        ctx.assertTrue(act(player, id, ContractService.Action.ACCEPT), "accept succeeds");
        ctx.assertTrue(act(player, id, ContractService.Action.COMPLETE), "complete succeeds with full inventory");
        ctx.assertTrue(!act(player, id, ContractService.Action.COMPLETE), "second completion refused");
        int emeraldsOnHand = player.getInventory().countItem(Items.EMERALD);
        ctx.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 0, "the bonus reward did not fit and is not in the inventory");
        String tx = ContractLedger.of(ctx.getLevel()).get(id).tx();
        int tagged = 0;
        for (var entity : ctx.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(64.0)))
            if (entity.getItem().is(Items.IRON_INGOT) && tx.equals(entity.getAttached(ContractAttachments.DROP_TX))) tagged += entity.getItem().getCount();
        ctx.assertTrue(tagged == 1, "exactly one tagged overflow drop of the bonus item: " + tagged);
        int emeraldsDropped = 0;
        for (var entity : ctx.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(-1.0E7, -1024, -1.0E7, 1.0E7, 1024, 1.0E7))) {
            if (entity.getItem().is(Items.EMERALD)) emeraldsDropped += entity.getItem().getCount();
        }
        ctx.assertTrue(emeraldsOnHand + emeraldsDropped == 5, "reward total equals one payout: " + (emeraldsOnHand + emeraldsDropped));
        ctx.succeed();
    }

    @GameTest
    public void unknownActionNamesResolveToNothing(GameTestHelper ctx) {
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.network.ContractActionPayload.parse("ACCEPT")
                == ContractService.Action.ACCEPT, "known name resolves");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.network.ContractActionPayload.parse("accept") == null, "case mismatch refused");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.network.ContractActionPayload.parse("DROP_TABLE") == null, "unknown refused");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
