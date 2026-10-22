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

    @GameTest
    public void transferPreservesItemBoundProgress(GameTestHelper ctx) {
        ServerPlayer giver = ctx.makeMockServerPlayerInLevel();
        ServerPlayer receiver = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        ItemStack stack = ContractStacks.create(contract(id, ContractData.Category.COMBAT, "minecraft:zombie", 2));
        giver.getInventory().setItem(0, stack);
        ctx.assertTrue(ContractService.act(giver, id, ContractService.Action.ACCEPT), "accept succeeds");
        ContractService.advance(giver, ContractData.Category.COMBAT, "minecraft:zombie");
        ItemStack moved = giver.getInventory().removeItemNoUpdate(0);
        receiver.getInventory().setItem(0, moved);
        ctx.assertTrue(ContractStacks.read(receiver.getInventory().getItem(0)).objectives().getFirst().progress() == 1,
                "transfer keeps progress");
        ContractService.advance(receiver, ContractData.Category.COMBAT, "minecraft:zombie");
        ctx.assertTrue(ContractService.act(receiver, id, ContractService.Action.COMPLETE), "recipient redeems");
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
        ctx.assertTrue(ContractService.act(player, id, ContractService.Action.ACCEPT), "accept succeeds");
        player.getInventory().setItem(1, player.getInventory().getItem(0).copy());
        ctx.assertTrue(ContractService.act(player, id, ContractService.Action.COMPLETE), "first copy redeems");
        ctx.assertTrue(!ContractService.act(player, id, ContractService.Action.COMPLETE), "duplicate cannot redeem");
        ctx.assertTrue(player.getInventory().countItem(Items.WHEAT) == 0
                && player.getInventory().countItem(Items.EMERALD) == 5, "materials and reward used once");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
