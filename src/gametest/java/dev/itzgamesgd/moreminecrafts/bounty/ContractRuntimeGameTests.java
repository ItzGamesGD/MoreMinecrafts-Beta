package dev.itzgamesgd.moreminecrafts.bounty;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/** Runtime behavior around the transaction: death, concurrency, structures, and the full acquisition loop. */
public final class ContractRuntimeGameTests implements CustomTestMethodInvoker {
    private static ContractData combat(String id, int required, int progress) {
        return new ContractData(id, ContractData.Tier.BEGINNER, ContractData.Status.ACCEPTED,
                List.of(new ContractData.Objective(ContractData.Category.COMBAT, "minecraft:zombie", required, progress)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1));
    }

    @GameTest
    public void deathDropKeepsContractIdentityAndProgress(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer victim = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        ContractData original = combat(id, 3, 2);
        victim.getInventory().setItem(0, ContractStacks.create(original));
        victim.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        victim.die(level.damageSources().genericKill());
        ContractData dropped = null;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(16.0))) {
            ContractData data = ContractStacks.read(entity.getItem());
            if (data != null && data.id().equals(id)) dropped = data;
        }
        ctx.assertTrue(dropped != null, "the contract was dropped on death");
        ctx.assertTrue(dropped.equals(original), "dropped contract keeps identity, status, and progress");
        ServerPlayer finder = ctx.makeMockServerPlayerInLevel();
        finder.getInventory().setItem(0, ContractStacks.create(dropped));
        ContractService.advance(finder, ContractData.Category.COMBAT, "minecraft:zombie");
        ctx.assertTrue(ContractService.act(finder, 0, id, ContractService.Action.COMPLETE), "another player can finish the dropped contract");
        ctx.assertTrue(finder.getInventory().countItem(Items.EMERALD) == 5, "paid once");
        ctx.succeed();
    }

    @GameTest(maxTicks = 200)
    public void concurrentSubmissionsAreSerializedAndPayOnce(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(new ContractData(id, ContractData.Tier.BEGINNER,
                ContractData.Status.ACCEPTED, List.of(new ContractData.Objective(ContractData.Category.DELIVERY, "minecraft:wheat", 8, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1))));
        player.getInventory().setItem(1, new ItemStack(Items.WHEAT, 8));
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        int threads = 8;
        for (int index = 0; index < threads; index++) {
            new Thread(() -> ctx.getLevel().getServer().execute(() -> {
                if (ContractService.act(player, 0, id, ContractService.Action.COMPLETE)) successes.incrementAndGet();
                finished.incrementAndGet();
            })).start();
        }
        ctx.runAfterDelay(40, () -> {
            ctx.assertTrue(finished.get() == threads, "all submitted requests ran: " + finished.get());
            ctx.assertTrue(successes.get() == 1, "exactly one concurrent request succeeded: " + successes.get());
            ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 5, "single payout");
            ctx.succeed();
        });
    }

    @GameTest
    public void malformedAndOversizedRequestsAreRejected(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(combat(id, 1, 1)));
        ctx.assertTrue(!ContractService.act(player, 0, null, ContractService.Action.COMPLETE), "null id");
        ctx.assertTrue(!ContractService.act(player, 0, "x".repeat(500), ContractService.Action.COMPLETE), "oversized id");
        ctx.assertTrue(!ContractService.act(player, 0, id, null), "null action");
        ctx.assertTrue(!ContractService.act(player, 0, UUID.randomUUID().toString(), ContractService.Action.COMPLETE), "unknown id");
        player.getInventory().setItem(1, new ItemStack(Items.DIRT));
        ctx.assertTrue(!ContractService.act(player, 1, id, ContractService.Action.ABANDON), "slot without the contract");
        ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) == 0, "nothing was granted by rejected requests");
        ctx.succeed();
    }

    @GameTest
    public void unacceptedContractDoesNotCreditAndAcceptedOneDoesNotDoubleCountAnInstance(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        ContractData offered = new ContractData(id, ContractData.Tier.BEGINNER, ContractData.Status.OFFERED,
                List.of(new ContractData.Objective(ContractData.Category.EXPLORATION, "minecraft:village_plains", 2, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1));
        player.getInventory().setItem(0, ContractStacks.create(offered));
        ContractService.enterStructure(player, "minecraft:village_plains", "overworld:1");
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(0)).objectives().getFirst().progress() == 0,
                "visits before acceptance do not count");
        ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.ACCEPT), "accept");
        ContractService.enterStructure(player, "minecraft:village_plains", "overworld:1");
        ContractService.enterStructure(player, "minecraft:village_plains", "overworld:1");
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(0)).objectives().getFirst().progress() == 1,
                "the same structure instance counts once however often it is entered");
        ContractService.enterStructure(player, "minecraft:mineshaft", "overworld:2");
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(0)).objectives().getFirst().progress() == 1,
                "an unrelated structure does not count");
        ContractService.enterStructure(player, "minecraft:village_plains", "overworld:3");
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(0)).objectives().getFirst().progress() == 2,
                "a distinct instance counts");
        ctx.succeed();
    }

    @GameTest(maxTicks = 300)
    public void generatedVanillaStructureEntryIsDetectedOnTheServerAfterAcceptance(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        // The test world is flat with no natural structures, so generate a genuine vanilla StructureStart with the
        // game's own Structure.generate and register it in the chunks exactly as world generation records it.
        var structures = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var holder = structures.getOrThrow(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.STRUCTURE, Identifier.parse("minecraft:desert_pyramid")));
        var generator = level.getChunkSource().getGenerator();
        var randomState = level.getChunkSource().randomState();
        net.minecraft.world.level.ChunkPos origin = new net.minecraft.world.level.ChunkPos(5000, 5000);
        var start = holder.value().generate(holder, level.dimension(), level.registryAccess(), generator,
                generator.getBiomeSource(), randomState.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED), randomState, level.getServer().getStructureTemplateManager(),
                level.getSeed(), origin, 0, level, biome -> true);
        ctx.assertTrue(start.isValid(), "vanilla produced a valid desert pyramid start");
        var box = start.getBoundingBox();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                var chunk = level.getChunk(cx, cz);
                chunk.addReferenceForStructure(holder.value(), origin.pack());
                if (cx == origin.x() && cz == origin.z()) chunk.setStartForStructure(holder.value(), start);
            }
        BlockPos inside = start.getPieces().getFirst().getBoundingBox().getCenter();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        String id = UUID.randomUUID().toString();
        player.getInventory().setItem(0, ContractStacks.create(new ContractData(id, ContractData.Tier.BEGINNER,
                ContractData.Status.ACCEPTED, List.of(
                        new ContractData.Objective(ContractData.Category.EXPLORATION, "minecraft:desert_pyramid", 2, 0),
                        new ContractData.Objective(ContractData.Category.EXPLORATION, "minecraft:jungle_pyramid", 1, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1))));
        player.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        ContractObjectives.detectStructure(player);
        ContractData data = ContractStacks.read(player.getInventory().getItem(0));
        ctx.assertTrue(data.objectives().getFirst().progress() == 1, "entering the generated pyramid credits it: " + data.objectives().getFirst().progress());
        ctx.assertTrue(data.objectives().get(1).progress() == 0, "a non-matching structure objective is untouched");
        player.setPos(inside.getX() + 0.5, 300, inside.getZ() + 0.5);
        ContractObjectives.detectStructure(player);
        player.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
        ContractObjectives.detectStructure(player);
        ctx.assertTrue(ContractStacks.read(player.getInventory().getItem(0)).objectives().getFirst().progress() == 1,
                "leaving and re-entering the same instance does not double count");
        ServerPlayer outsider = ctx.makeMockServerPlayerInLevel();
        outsider.getInventory().setItem(0, ContractStacks.create(new ContractData(UUID.randomUUID().toString(), ContractData.Tier.BEGINNER,
                ContractData.Status.ACCEPTED, List.of(new ContractData.Objective(ContractData.Category.EXPLORATION, "minecraft:desert_pyramid", 1, 0)),
                new ContractData.Reward(5, "minecraft:iron_ingot", 1))));
        outsider.setPos(inside.getX() + 500.5, 70, inside.getZ() + 500.5);
        ContractObjectives.detectStructure(outsider);
        ctx.assertTrue(ContractStacks.read(outsider.getInventory().getItem(0)).objectives().getFirst().progress() == 0,
                "proximity without entering a structure piece credits nothing");
        ctx.succeed();
    }

    @GameTest
    public void tradeAcquisitionThroughCompletionWithAGeneratedContract(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        for (ContractData.Tier tier : List.of(ContractData.Tier.BEGINNER, ContractData.Tier.INTERMEDIATE, ContractData.Tier.ADVANCED)) {
            player.getInventory().clearContent();
            MerchantOffer offer = ContractTrades.offer(tier);
            player.getInventory().setItem(8, new ItemStack(Items.EMERALD, 40));
            ctx.assertTrue(offer.take(player.getInventory().getItem(8), ItemStack.EMPTY), "the buyer can pay the listed price");
            offer.increaseUses();
            ctx.assertTrue(offer.isOutOfStock(), "stock exhausted by the purchase");
            player.getInventory().setItem(0, offer.getResult().copy());
            ContractMaterializer.materialize(player);
            ContractData data = ContractStacks.read(player.getInventory().getItem(0));
            ctx.assertTrue(data != null && data.status() == ContractData.Status.OFFERED, tier + " purchase yields a fixed unaccepted contract");
            String id = data.id();
            ctx.assertTrue(ContractService.act(player, 0, id, ContractService.Action.ACCEPT), "accept");
            // Satisfy every generated objective through the same server paths gameplay uses.
            for (ContractData.Objective objective : data.objectives()) {
                switch (objective.category()) {
                    case COMBAT -> {
                        for (int kill = 0; kill < objective.required(); kill++)
                            ContractService.advance(player, ContractData.Category.COMBAT, objective.target());
                    }
                    case EXPLORATION -> {
                        for (int visit = 0; visit < objective.required(); visit++)
                            ContractService.enterStructure(player, objective.target(), "overworld:" + visit + ":" + id);
                    }
                    case DELIVERY -> {
                        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(objective.target())));
                        for (int left = objective.required(); left > 0; ) {
                            ItemStack part = stack.copyWithCount(Math.min(left, stack.getMaxStackSize()));
                            left -= part.getCount();
                            player.getInventory().add(part);
                        }
                    }
                    default -> { }
                }
            }
            int slot = -1;
            for (int s = 0; s < player.getInventory().getContainerSize(); s++) {
                ContractData candidate = ContractStacks.read(player.getInventory().getItem(s));
                if (candidate != null && candidate.id().equals(id)) slot = s;
            }
            ctx.assertTrue(slot >= 0, "the contract stays in the inventory while being worked");
            ctx.assertTrue(ContractService.act(player, slot, id, ContractService.Action.COMPLETE), tier + " completes");
            ctx.assertTrue(player.getInventory().countItem(Items.EMERALD) >= data.reward().emeralds(), "emerald reward granted");
            ctx.assertTrue(ContractLedger.of(ctx.getLevel()).get(id).state() == ContractLedger.State.COMMITTED, "redemption committed");
        }
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
