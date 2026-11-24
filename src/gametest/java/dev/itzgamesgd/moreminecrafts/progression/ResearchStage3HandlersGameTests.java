package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/** Server integration tests for Stage 3 handlers (.13, .14, .46, .63, combat V4, tool switch). */
public final class ResearchStage3HandlersGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void cropCultivationAndMagicalMaterialBlockDropsArePurchaseGated(GameTestHelper ctx) {
        // makeMockServerPlayerInLevel() always reports isCreative() == true; drop bonuses require genuine survival.
        ServerPlayer unpaid = (ServerPlayer) ctx.makeMockServerPlayer(GameType.SURVIVAL);
        ServerPlayer paid = (ServerPlayer) ctx.makeMockServerPlayer(GameType.SURVIVAL);
        paid.setAttached(KnowledgeRuntime.STATE, ResearchProgressState.empty()
                .with("research.v3.research.14", 3)
                .with("research.v3.research.63", 3));

        var matureWheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        var baseWheat = List.of(new ItemStack(Items.WHEAT, 1));
        ctx.assertTrue(ResearchContentRuntime.applyCropCultivationResearch(unpaid, matureWheat, baseWheat, () -> 0.0D).size() == 1,
                "unpurchased player gets no extra crop yield");
        var paidWheat = ResearchContentRuntime.applyCropCultivationResearch(paid, matureWheat, baseWheat, () -> 0.0D);
        int wheatCount = paidWheat.stream().filter(s -> s.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum();
        ctx.assertTrue(wheatCount == 3, "L3 Crop Cultivation adds +2 wheat (got " + wheatCount + ")");

        var netherWart = Blocks.NETHER_WART.defaultBlockState();
        var baseWart = List.of(new ItemStack(Items.NETHER_WART, 2));
        var paidWart = ResearchContentRuntime.applyMagicalMaterialBlockResearch(paid, netherWart, baseWart, () -> 0.0D);
        int wartCount = paidWart.stream().filter(s -> s.is(Items.NETHER_WART)).mapToInt(ItemStack::getCount).sum();
        ctx.assertTrue(wartCount == 3, "L3 Magical Material Study adds +1 tagged brewing ingredient (got " + wartCount + ")");
        ctx.succeed();
    }

    @GameTest
    public void rareMobResearch46AppliesToTaggedRareMobsOnly(GameTestHelper ctx) {
        ServerPlayer paid = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(paid, ResearchProgressState.empty()
                .with("research.v3.research.46", 3)
                .with("knowledge.51", 1));
        var evoker = ctx.spawn(EntityTypes.EVOKER, new BlockPos(2, 2, 2));
        var wither = ctx.spawn(EntityTypes.WITHER, new BlockPos(4, 2, 2));
        ctx.assertTrue(Math.abs(ResearchEcologyDamage.bonus(paid, evoker) - 0.17D) < 1.0E-8D,
                "L3 rare mob (+12%) + knowledge 51 (+5%) = +17%");
        ctx.assertTrue(Math.abs(ResearchEcologyDamage.bonus(paid, wither) - 0.0D) < 1.0E-8D,
                "bosses are excluded from rare mob research bonus");
        ctx.succeed();
    }

    @GameTest
    public void chestSortConsolidatesAndSortsLosslesslyOnlyWhenResearched(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        chest.setItem(5, new ItemStack(Items.DIAMOND_PICKAXE, 1));
        chest.setItem(10, new ItemStack(Items.COBBLESTONE, 12));
        chest.setItem(15, new ItemStack(Items.BREAD, 5));
        player.containerMenu = ChestMenu.threeRows(91, player.getInventory(), chest);

        ctx.assertTrue(!ChestSortRuntime.sortOpenChest(player, ChestSortRules.MODE_CATEGORY),
                "unpurchased player cannot sort chest");

        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.v3.research.13", 1));
        ctx.assertTrue(ChestSortRuntime.sortOpenChest(player, ChestSortRules.MODE_CATEGORY),
                "purchased v3.research.13 player sorts open chest");
        ctx.assertTrue(chest.getItem(0).is(Items.DIAMOND_PICKAXE), "tools sort first in category mode");
        ctx.assertTrue(chest.getItem(1).is(Items.COBBLESTONE) && chest.getItem(1).getCount() == 22,
                "identical cobblestone stacks merge losslessly to 22");
        ctx.assertTrue(chest.getItem(2).is(Items.BREAD) && chest.getItem(2).getCount() == 5,
                "food sorts after blocks in category mode");
        ctx.succeed();
    }

    @GameTest
    public void automaticToolSwitchSelectsBestHotbarPickaxeOnStone(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.STICK));
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND_PICKAXE));
        player.getInventory().setSelectedSlot(0);

        ResearchCombatV4Runtime.tryAutoToolSwitch(player, Blocks.STONE.defaultBlockState());
        ctx.assertTrue(player.getInventory().getSelectedSlot() == 0,
                "unpurchased player does not auto-switch tools");

        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.automatic_tool_switch", 1));
        ResearchCombatV4Runtime.tryAutoToolSwitch(player, Blocks.STONE.defaultBlockState());
        ctx.assertTrue(player.getInventory().getSelectedSlot() == 3,
                "purchased automatic_tool_switch switches to highest-efficiency hotbar pickaxe");
        ctx.succeed();
    }

    @GameTest
    public void hopperFilterGatesIntakeWhenConfiguredByResearchedOwner(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.hopper_filter", 1));

        BlockPos hopperRel = new BlockPos(2, 2, 2);
        BlockPos hopperPos = ctx.absolutePos(hopperRel);
        ctx.setBlock(hopperRel, Blocks.HOPPER);
        var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) ctx.getLevel().getBlockEntity(hopperPos);
        ctx.assertTrue(hopper instanceof HopperFilterAccess, "HopperBlockEntity implements HopperFilterAccess");
        HopperFilterAccess access = (HopperFilterAccess) hopper;
        access.moreminecrafts$setResearchAttribution(BlockEntityResearchAttribution.State.empty().interactedBy(player.getUUID()));
        access.moreminecrafts$setFilterState(new HopperFilterState(true,
                List.of(new HopperFilterState.Entry("minecraft:diamond", false, 0))));

        ItemStack rejected = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(
                null, hopper, new ItemStack(Items.COBBLESTONE, 4), null);
        ctx.assertTrue(rejected.getCount() == 4, "whitelist hopper rejects non-matching cobblestone");
        ItemStack accepted = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(
                null, hopper, new ItemStack(Items.DIAMOND, 2), null);
        ctx.assertTrue(accepted.isEmpty() && hopper.getItem(0).is(Items.DIAMOND) && hopper.getItem(0).getCount() == 2,
                "whitelist hopper accepts matching diamond");
        ctx.succeed();
    }

    @GameTest
    public void brewingMixConcentrateAndAutoProcess9Plus9ArePurchaseGated(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        BlockPos standRel = new BlockPos(3, 2, 3);
        BlockPos standPos = ctx.absolutePos(standRel);
        ctx.setBlock(standRel, Blocks.BREWING_STAND);
        var stand = (net.minecraft.world.level.block.entity.BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(standPos);
        ctx.assertTrue(stand != null, "BrewingStandBlockEntity exists");

        stand.setItem(0, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                Items.POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        stand.setItem(1, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                Items.POTION, net.minecraft.world.item.alchemy.Potions.STRENGTH));
        ctx.assertTrue(!BrewingResearchRuntime.mixOrConcentrate(player, stand),
                "unpurchased player cannot mix potions");

        KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research.v3.research.68", 1)
                .with("research.v3.research.69", 1)
                .with("research.v3.research.70", 1));
        ctx.assertTrue(BrewingResearchRuntime.mixOrConcentrate(player, stand),
                "purchased v3.research.68 mixes Swiftness + Strength");
        var mixedContents = stand.getItem(0).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        ctx.assertTrue(mixedContents != null && mixedContents.customEffects().size() == 2,
                "mixed potion contains both effects");

        // Concentration test (.70)
        stand.setItem(0, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                Items.POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        stand.setItem(1, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                Items.POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        ctx.assertTrue(BrewingResearchRuntime.mixOrConcentrate(player, stand),
                "purchased v3.research.70 concentrates matching potions");
        var concContents = stand.getItem(0).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        ctx.assertTrue(concContents != null && concContents.customEffects().getFirst().getAmplifier() == 1,
                "concentrated potion upgrades amplifier to II (1)");

        // 9+9 Auto-process test (.69)
        BrewingResearchStateAccess access = (BrewingResearchStateAccess) stand;
        access.moreminecrafts$setAutoProcessEnabled(true);
        access.moreminecrafts$getInputQueue().set(0, new ItemStack(Items.NETHER_WART, 1));
        BrewingResearchRuntime.tickAutoProcess(ctx.getLevel(), stand, 0);
        ctx.assertTrue(stand.getItem(3).is(Items.NETHER_WART),
                ".69 auto-process pulls queued ingredient into stand");
        ctx.assertTrue(!access.moreminecrafts$getOutputBuffer().get(0).isEmpty(),
                ".69 auto-process moves finished potion to output buffer");
        ctx.succeed();
    }

    @GameTest
    public void nearbyChestIngredientsSummarizesAndPullsItems(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.nearby_chest_ingredients", 1));

        BlockPos tableRel = new BlockPos(4, 2, 4);
        BlockPos chestRel = new BlockPos(5, 2, 4);
        ctx.setBlock(tableRel, Blocks.CRAFTING_TABLE);
        ctx.setBlock(chestRel, Blocks.CHEST);
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) ctx.getLevel().getBlockEntity(ctx.absolutePos(chestRel));
        ctx.assertTrue(chest != null, "ChestBlockEntity exists");
        chest.setItem(0, new ItemStack(Items.IRON_INGOT, 12));

        var summary = NearbyChestIngredientsRuntime.summarize(ctx.getLevel(), ctx.absolutePos(tableRel));
        ctx.assertTrue(summary.stream().anyMatch(e -> e.itemId().equals("minecraft:iron_ingot") && e.totalCount() == 12),
                "nearby chest summary includes 12 iron ingots");

        int pulled = NearbyChestIngredientsRuntime.pullItem(player, ctx.absolutePos(tableRel),
                new dev.itzgamesgd.moreminecrafts.network.NearbyChestPullPayload("minecraft:iron_ingot", 5));
        ctx.assertTrue(pulled == 5 && chest.getItem(0).getCount() == 7,
                "pulling 5 iron ingots transfers them from chest to player inventory");
        ctx.succeed();
    }

    @GameTest
    public void archaeologyBrushTimeReducesAtLevel3(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.v3.research.52", 3));
        BlockPos rel = new BlockPos(6, 2, 6);
        BlockPos pos = ctx.absolutePos(rel);
        ctx.setBlock(rel, Blocks.SUSPICIOUS_SAND);
        var brushable = (net.minecraft.world.level.block.entity.BrushableBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ctx.assertTrue(brushable != null, "BrushableBlockEntity exists");

        // 7 brushes (L3 = -30% of 10) must complete brushing when called at 10-tick intervals.
        boolean completed = false;
        for (int i = 0; i < 7; i++) {
            completed = brushable.brush(100L + i * 10L, ctx.getLevel(), player,
                    net.minecraft.core.Direction.UP, new ItemStack(Items.BRUSH));
        }
        ctx.assertTrue(completed, "v3.research.52 L3 completes brushing in 7 brushes (-30%)");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
