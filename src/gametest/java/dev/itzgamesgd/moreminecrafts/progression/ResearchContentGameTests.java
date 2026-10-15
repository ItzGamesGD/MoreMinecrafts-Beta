package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Event-level coverage for Stage 23-A research effects and purchase gates. */
public final class ResearchContentGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void dropResearchIsPurchaseGatedAndUsesTheProcessedDrop(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var ore = List.of(new ItemStack(Items.DIAMOND));
        var unowned = ResearchContentRuntime.applyBlockDropResearch(
                player, Blocks.DIAMOND_ORE.defaultBlockState(), ore, () -> 0.0D);
        ctx.assertTrue(unowned.size() == 1 && unowned.getFirst().getCount() == 1,
                "an unowned player receives only the post-enchantment base ore result");

        KnowledgeRuntime.updateState(player, KnowledgeRuntime.state(player)
                .with("v3.research.01", 5)
                .with("v3.research.02", 1)
                .with("v3.research.15", 3));
        var oreDrops = ResearchContentRuntime.applyBlockDropResearch(player,
                Blocks.DIAMOND_ORE.defaultBlockState(), List.of(new ItemStack(Items.DIAMOND)), () -> 0.0D);
        ctx.assertTrue(oreDrops.size() == 2 && oreDrops.get(1).is(Items.DIAMOND)
                        && oreDrops.get(1).getCount() == 3,
                "Ore Mining copies the final Fortune/Silk Touch/Auto Smelt product with the approved max count");

        var logDrops = ResearchContentRuntime.applyBlockDropResearch(player,
                Blocks.OAK_LOG.defaultBlockState(), List.of(new ItemStack(Items.OAK_LOG)), () -> 0.0D);
        ctx.assertTrue(logDrops.size() == 2 && logDrops.get(1).is(Items.OAK_LOG)
                        && logDrops.get(1).getCount() == 1,
                "Lumbering adds log items without breaking another log block");

        var seedDrops = ResearchContentRuntime.applyBlockDropResearch(player,
                Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7),
                List.of(new ItemStack(Items.WHEAT_SEEDS)), () -> 0.0D);
        ctx.assertTrue(seedDrops.size() == 2 && seedDrops.get(1).is(Items.WHEAT_SEEDS),
                "Seed Study adds one eligible seed and does not touch crop yield logic");
        ctx.succeed();
    }

    @GameTest
    public void woodProcessingChangesTheAuthoritativeRecipeResultOnce(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(player,
                KnowledgeRuntime.state(player).with("v3.research.09", 2));
        CraftingMenu menu = new CraftingMenu(1, player.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(ctx.getLevel(),
                        ctx.absolutePos(new BlockPos(1, 1, 1))));
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
        menu.slotsChanged(menu.getSlot(1).container);
        ctx.assertTrue(menu.getSlot(0).getItem().is(Items.OAK_PLANKS)
                        && menu.getSlot(0).getItem().getCount() == 6,
                "level two adds two to the vanilla four-plank recipe result");
        menu.quickMoveStack(player, 0);
        ctx.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS) == 6,
                "shift-click transfers the augmented result exactly once");
        ctx.assertTrue(menu.getSlot(1).getItem().isEmpty(), "one log is consumed once");
        ctx.succeed();
    }

    @GameTest
    public void miningConservationRunsOnlyInsideTheEffectiveBlockBreakScope(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(player,
                KnowledgeRuntime.state(player).with("v3.research.03", 5));
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickaxe);
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        player.getRandom().setSeed(314159L);
        int breaks = 32;
        for (int i = 0; i < breaks; i++) {
            ctx.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            ctx.assertTrue(player.gameMode.destroyBlock(pos), "effective pickaxe break succeeds");
        }
        ctx.assertTrue(pickaxe.getDamageValue() > 0 && pickaxe.getDamageValue() < breaks,
                "mining durability is independently prevented without double-consuming damage");
        ctx.succeed();
    }

    @GameTest
    public void equipmentMaintenanceImprovesOnlyAnActualMaterialRepair(GameTestHelper ctx) {
        ServerPlayer baseline = ctx.makeMockServerPlayerInLevel();
        baseline.setGameMode(GameType.SURVIVAL);
        AnvilMenu baselineMenu = new AnvilMenu(1, baseline.getInventory());
        ((T1AnvilModes) baselineMenu).moreminecrafts$button(baseline, 1);
        ItemStack baselineSword = new ItemStack(Items.DIAMOND_SWORD);
        baselineSword.setDamageValue(1000);
        baselineMenu.getSlot(0).set(baselineSword);
        baselineMenu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        baselineMenu.createResult();
        ctx.assertTrue(baselineMenu.getSlot(2).hasItem(), "vanilla material repair has a result");
        int baselineDamage = baselineMenu.getSlot(2).getItem().getDamageValue();

        ServerPlayer researcher = ctx.makeMockServerPlayerInLevel();
        researcher.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(researcher,
                KnowledgeRuntime.state(researcher).with("v3.research.10", 1));
        AnvilMenu researchMenu = new AnvilMenu(1, researcher.getInventory());
        ((T1AnvilModes) researchMenu).moreminecrafts$button(researcher, 1);
        ItemStack researchSword = new ItemStack(Items.DIAMOND_SWORD);
        researchSword.setDamageValue(1000);
        researchMenu.getSlot(0).set(researchSword);
        researchMenu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        researchMenu.createResult();
        ctx.assertTrue(researchMenu.getSlot(2).hasItem()
                        && researchMenu.getSlot(2).getItem().getDamageValue() < baselineDamage,
                "Equipment Maintenance adds durability only to a successful material repair");
        ctx.succeed();
    }

    @GameTest
    public void beekeepingAddsOneCombOnlyAfterAFullHiveShearsHarvest(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(player,
                KnowledgeRuntime.state(player).with("v3.research.18", 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        long seed = 0;
        do {
            player.getRandom().setSeed(seed++);
        } while (player.getRandom().nextDouble() >= 0.60D);
        player.getRandom().setSeed(seed - 1);

        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        var before = Blocks.BEEHIVE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 5);
        ctx.getLevel().setBlock(pos, before, 3);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = Blocks.BEEHIVE.useItemOn(player.getMainHandItem(), before,
                ctx.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        ctx.assertTrue(result.consumesAction(), "vanilla handles the full-hive shears action");
        int honeycombCount = ctx.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                        pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2)).stream()
                .filter(item -> item.getItem().is(Items.HONEYCOMB))
                .mapToInt(item -> item.getItem().getCount()).sum();
        ctx.assertTrue(honeycombCount >= 4,
                "the full-hive activation yields vanilla combs plus one purchased bonus");
        ctx.assertTrue(ctx.getLevel().getBlockState(pos)
                        .getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) == 0,
                "bonus grant does not re-run or preserve the vanilla harvest");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
