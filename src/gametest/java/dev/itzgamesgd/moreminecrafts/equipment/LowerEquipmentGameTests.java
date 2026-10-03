package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityTypes;

public final class LowerEquipmentGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void drillRejectsGhostSlotWithoutLosingCursor(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MINING_DRILL));
        ModItems.MINING_DRILL.use(context.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = player.containerMenu;
        for (int slot = 1; slot < 9; slot++) {
            menu.setCarried(new ItemStack(Items.COAL, 64));
            menu.clicked(slot, 0, ContainerInput.PICKUP, player);
            context.assertTrue(menu.getCarried().getCount() == 64, "Ghost slot must leave all 64 coal on cursor");
            context.assertTrue(menu.getSlot(slot).getItem().isEmpty(), "Ghost slot must remain empty");
        }
        context.succeed();
    }

    @GameTest
    public void drillShiftClickKeepsExcessWithPlayer(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var drill = new ItemStack(ModItems.MINING_DRILL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        ModItems.MINING_DRILL.use(context.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = player.containerMenu;
        menu.getSlot(0).set(new ItemStack(Items.COAL, 60));
        player.getInventory().setItem(9, new ItemStack(Items.COAL, 64));
        menu.quickMoveStack(player, 9);
        context.assertTrue(menu.getSlot(0).getItem().getCount() == 64, "Drill fuel slot must fill to 64");
        context.assertTrue(player.getInventory().getItem(9).getCount() == 60,
                "Unaccepted coal must stay in the player inventory");
        context.assertTrue(new ItemComponentContainer(drill, 1).getItem(0).getCount() == 64,
                "Merged fuel must persist in the drill component");
        for (int slot = 1; slot < 9; slot++)
            context.assertTrue(menu.getSlot(slot).getItem().isEmpty(), "Shift-click must not populate ghost slots");
        context.succeed();
    }

    @GameTest
    public void drillOnlyAcceptsCoalAndCoalBlocks(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MINING_DRILL));
        ModItems.MINING_DRILL.use(context.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = player.containerMenu;
        menu.setCarried(new ItemStack(Items.CHARCOAL, 64));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().getCount() == 64 && menu.getSlot(0).getItem().isEmpty(),
                "Charcoal must stay on cursor");
        player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 64));
        menu.quickMoveStack(player, 9);
        context.assertTrue(menu.getSlot(0).getItem().isEmpty(), "Shift-click must reject non-fuel");
        int remaining = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(Items.IRON_INGOT)) remaining += stack.getCount();
        }
        context.assertTrue(remaining == 64, "Rejected shift-click must conserve all items");
        menu.setCarried(new ItemStack(Items.COAL_BLOCK, 64));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().getCount() == 64,
                "Real fuel slot must accept 64 coal blocks");
        context.succeed();
    }

    @GameTest
    public void portableFurnaceManualClickKeepsExcessOnCursor(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var contents = new PortableFurnaceMenu.PortableContainer();
        var menu = new PortableFurnaceMenu(1, player.getInventory(), contents);
        menu.setCarried(new ItemStack(Items.COAL, 64));
        menu.clicked(1, 0, ContainerInput.PICKUP, player);
        context.assertTrue(contents.getItem(1).getCount() == 16, "Furnace fuel slot must accept 16");
        context.assertTrue(menu.getCarried().getCount() == 48, "Remaining 48 coal must stay on cursor");
        context.succeed();
    }

    @GameTest
    public void portableFurnaceShiftClickKeepsExcessWithPlayer(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var contents = new PortableFurnaceMenu.PortableContainer();
        var menu = new PortableFurnaceMenu(1, player.getInventory(), contents);
        player.getInventory().setItem(9, new ItemStack(Items.COAL, 64));
        menu.quickMoveStack(player, 3);
        context.assertTrue(contents.getItem(1).getCount() == 16, "Shift-click fuel slot must accept 16");
        context.assertTrue(player.getInventory().getItem(9).getCount() == 48, "48 coal must remain at source");
        context.succeed();
    }

    @GameTest
    public void portableFurnaceSmeltsWithVanillaCoal(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var contents = new PortableFurnaceMenu.PortableContainer();
        var menu = new PortableFurnaceMenu(1, player.getInventory(), contents);
        contents.setItem(0, new ItemStack(Items.RAW_IRON));
        contents.setItem(1, new ItemStack(Items.COAL));
        for (int tick = 0; tick < 70; tick++) menu.serverTick(player);
        context.assertTrue(contents.getItem(0).isEmpty(), "One raw iron must be consumed");
        context.assertTrue(contents.getItem(1).isEmpty(), "One coal must be consumed");
        context.assertTrue(contents.getItem(2).is(Items.IRON_INGOT)
                && contents.getItem(2).getCount() == 1, "One iron ingot must be produced");
        context.succeed();
    }

    @GameTest
    public void greatswordRightClickHitsPrimaryAndNearbySecondary(GameTestHelper context) {
        var level = context.getLevel();
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.GREATSWORDS.get(WeaponBalance.Tier.IRON)));
        var primary = context.spawnMob(EntityTypes.COW, 1, 1, 1).withNoFreeWill().spawn();
        var secondary = context.spawnMob(EntityTypes.COW, 1, 1, 1).withNoFreeWill().spawn();
        var outside = context.spawnMob(EntityTypes.COW, 1, 1, 1).withNoFreeWill().spawn();
        primary.setPos(primary.getX(), primary.getY() + 5.0D, primary.getZ());
        player.setPos(primary.getX() - 1.0D, primary.getY(), primary.getZ());
        player.setYRot(-90.0F);
        player.setXRot(0.0F);
        secondary.setPos(primary.getX(), primary.getY(), primary.getZ() + 1.0D);
        outside.setPos(primary.getX(), primary.getY(), primary.getZ() + 2.5D);
        float primaryBefore = primary.getHealth();
        float secondaryBefore = secondary.getHealth();
        float outsideBefore = outside.getHealth();
        context.assertTrue(player.hasLineOfSight(primary), "Test targets must be unobstructed");
        player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
        context.assertTrue(primary.getHealth() < primaryBefore, "Primary target must take right-click damage");
        context.assertTrue(secondary.getHealth() < secondaryBefore, "Secondary sweep target must take damage");
        context.assertTrue(outside.getHealth() == outsideBefore, "Outside target must not take sweep damage");
        context.succeed();
    }

    @GameTest
    public void greatswordRightClickOnEntityInvokesSweep(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.GREATSWORDS.get(WeaponBalance.Tier.IRON)));
        var primary = context.spawnMob(EntityTypes.COW, 1, 1, 1).withNoFreeWill().spawn();
        var secondary = context.spawnMob(EntityTypes.COW, 1, 1, 1).withNoFreeWill().spawn();
        primary.setPos(primary.getX(), primary.getY() + 5.0D, primary.getZ());
        secondary.setPos(primary.getX(), primary.getY(), primary.getZ() + 1.0D);
        player.setPos(primary.getX() - 1.0D, primary.getY(), primary.getZ());
        player.setYRot(-90.0F);
        player.setXRot(0.0F);
        context.assertTrue(player.hasLineOfSight(primary), "Test target must be unobstructed");
        float primaryBefore = primary.getHealth();
        float secondaryBefore = secondary.getHealth();
        var result = player.interactOn(primary, InteractionHand.MAIN_HAND, primary.position());
        context.assertTrue(result.consumesAction(), "Successful entity sweep must consume the interaction");
        context.assertTrue(primary.getHealth() < primaryBefore, "Entity right-click must hit primary target");
        context.assertTrue(secondary.getHealth() < secondaryBefore, "Entity right-click must sweep nearby target");
        context.succeed();
    }

    @GameTest
    public void injectorRejectsNonPotionsThroughManualAndShiftClick(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var injector = new ItemStack(ModItems.POTION_INJECTOR);
        player.setItemInHand(InteractionHand.MAIN_HAND, injector);
        PotionInjectorItem.open(player, injector);
        var menu = player.containerMenu;
        menu.setCarried(new ItemStack(Items.COAL, 64));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().getCount() == 64 && menu.getSlot(0).getItem().isEmpty(),
                "Non-potions must remain on cursor");
        player.getInventory().setItem(9, new ItemStack(Items.SPLASH_POTION));
        menu.quickMoveStack(player, 9);
        for (int slot = 0; slot < 9; slot++)
            context.assertTrue(menu.getSlot(slot).getItem().isEmpty(), "Injector must reject splash potions");
        var potion = new ItemStack(Items.POTION);
        menu.setCarried(potion);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().is(Items.POTION),
                "Ordinary drinkable potion must be accepted");
        context.assertTrue(new ItemComponentContainer(injector, 9).getItem(0).is(Items.POTION),
                "Accepted potion must persist in the Injector component");
        context.succeed();
    }

    @GameTest
    public void lanternSocketIsImmediatelyLeftOfHelmetAndAcceptsLantern(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var menu = player.inventoryMenu;
        var helmet = menu.getSlot(5);
        var socket = menu.getSlot(46);
        context.assertTrue(socket.x == helmet.x - 18 && socket.y == helmet.y,
                "Socket interaction target must be immediately LEFT of vanilla helmet slot");
        menu.setCarried(new ItemStack(ModItems.MINER_LANTERN));
        menu.clicked(46, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().isEmpty()
                && player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY).is(ModItems.MINER_LANTERN),
                "Socket click must equip the lantern attachment");
        menu.clicked(46, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().is(ModItems.MINER_LANTERN)
                && player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY).isEmpty(),
                "Removing lantern must clear the light attachment");
        context.succeed();
    }

    @GameTest
    public void drillManualMergePersistsAndNeverPullsInventoryFuel(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var drill = new ItemStack(ModItems.MINING_DRILL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.getInventory().setItem(9, new ItemStack(Items.COAL, 64));
        context.assertTrue(!MiningDrillItem.consumeOneFuelPoint(drill), "Empty drill must not pull player coal");
        context.assertTrue(player.getInventory().getItem(9).getCount() == 64, "Inventory coal must remain untouched");
        ModItems.MINING_DRILL.use(context.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = player.containerMenu;
        menu.getSlot(0).set(new ItemStack(Items.COAL, 60));
        menu.setCarried(new ItemStack(Items.COAL, 64));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().getCount() == 60, "Manual merge must leave 60 excess coal on cursor");
        context.assertTrue(new ItemComponentContainer(drill, 1).getItem(0).getCount() == 64,
                "Manual merge must persist all 64 stored coal");
        for (int block = 0; block < 8; block++)
            context.assertTrue(MiningDrillItem.consumeOneFuelPoint(drill), "Coal must pay for eight blocks");
        context.assertTrue(new ItemComponentContainer(drill, 1).getItem(0).getCount() == 63,
                "Eight fuel points must consume exactly one coal");
        context.assertTrue(!drill.isDamageableItem(), "Drill must not acquire durability");
        context.succeed();
    }

    @GameTest
    public void portableFurnaceResolvesNormalFuelsAndReturnsLavaBucket(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        context.assertTrue(PortableFurnaceMenu.vanillaFuelTicks(context.getLevel(), player, new ItemStack(Items.COAL)) == 1600,
                "Coal provider must resolve through Minecraft");
        context.assertTrue(PortableFurnaceMenu.vanillaFuelTicks(context.getLevel(), player, new ItemStack(Items.CHARCOAL)) == 1600,
                "Charcoal provider must resolve through Minecraft");
        context.assertTrue(PortableFurnaceMenu.vanillaFuelTicks(context.getLevel(), player, new ItemStack(Items.OAK_LOG)) == 300,
                "Wood provider must resolve through Minecraft");
        var contents = new PortableFurnaceMenu.PortableContainer();
        var menu = new PortableFurnaceMenu(1, player.getInventory(), contents);
        contents.setItem(0, new ItemStack(Items.RAW_IRON));
        contents.setItem(1, new ItemStack(Items.LAVA_BUCKET));
        for (int tick = 0; tick < 66; tick++) menu.serverTick(player);
        context.assertTrue(contents.getItem(2).isEmpty(), "Smelt must not finish before tick 67");
        menu.serverTick(player);
        context.assertTrue(contents.getItem(2).is(Items.IRON_INGOT), "Smelt must finish on tick 67");
        context.assertTrue(contents.getItem(1).is(Items.BUCKET) && contents.getItem(1).getCount() == 1,
                "Lava fuel must return exactly one empty bucket");
        context.succeed();
    }

    @GameTest
    public void portableFurnaceCloseDropsContentsOnlyOnce(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var pos = context.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        player.setPos(pos.getX(), pos.getY() + 5, pos.getZ());
        var contents = new PortableFurnaceMenu.PortableContainer();
        var menu = new PortableFurnaceMenu(1, player.getInventory(), contents);
        contents.setItem(0, new ItemStack(Items.RAW_IRON, 4));
        contents.setItem(1, new ItemStack(Items.COAL, 5));
        contents.setItem(2, new ItemStack(Items.IRON_INGOT, 6));
        menu.removed(player);
        menu.removed(player);
        context.assertTrue(contents.isEmpty(), "Closed furnace must not retain hidden contents");
        int input = 0, fuel = 0, output = 0;
        for (var entity : context.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                player.getBoundingBox().inflate(2))) {
            var stack = entity.getItem();
            if (stack.is(Items.RAW_IRON)) input += stack.getCount();
            if (stack.is(Items.COAL)) fuel += stack.getCount();
            if (stack.is(Items.IRON_INGOT)) output += stack.getCount();
        }
        context.assertTrue(input == 4 && fuel == 5 && output == 6, "Close must drop contents without loss or duplication");
        context.succeed();
    }

    @GameTest
    public void greatswordLeftClickRemainsSingleTarget(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.GREATSWORDS.get(WeaponBalance.Tier.IRON)));
        var primary = context.spawnMob(EntityTypes.COW, 1, 1, 1).withNoFreeWill().spawn();
        var secondary = context.spawnMob(EntityTypes.COW, 1, 1, 1).withNoFreeWill().spawn();
        primary.setPos(primary.getX(), primary.getY() + 5.0D, primary.getZ());
        secondary.setPos(primary.getX(), primary.getY(), primary.getZ() + 1.0D);
        player.setPos(primary.getX() - 1.0D, primary.getY(), primary.getZ());
        float primaryBefore = primary.getHealth();
        float secondaryBefore = secondary.getHealth();
        player.attack(primary);
        context.assertTrue(primary.getHealth() < primaryBefore, "Left-click must damage the direct target");
        context.assertTrue(secondary.getHealth() == secondaryBefore, "Left-click must not sweep nearby target");
        context.succeed();
    }

    @GameTest
    public void drillMovingOpenerCannotConsumeFuelIntoDetachedStack(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MINING_DRILL));
        ModItems.MINING_DRILL.use(context.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = player.containerMenu;
        menu.clicked(36, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().is(ModItems.MINING_DRILL), "Test must pick up the opener drill");
        menu.clicked(10, 0, ContainerInput.PICKUP, player);
        context.assertTrue(player.getInventory().getItem(10).is(ModItems.MINING_DRILL), "Test must move opener to a new slot");
        menu.setCarried(new ItemStack(Items.COAL, 64));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().getCount() == 64 && menu.getSlot(0).getItem().isEmpty(),
                "Detached opener must reject fuel without losing cursor items");
        context.assertTrue(new ItemComponentContainer(player.getInventory().getItem(10), 1).getItem(0).isEmpty(),
                "Moved drill component must remain consistent with its disabled fuel session");
        context.succeed();
    }

    @GameTest
    public void drillMovingStockedOpenerCannotDuplicateFuel(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        var drill = new ItemStack(ModItems.MINING_DRILL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        ModItems.MINING_DRILL.use(context.getLevel(), player, InteractionHand.MAIN_HAND);
        var menu = player.containerMenu;
        menu.getSlot(0).set(new ItemStack(Items.COAL, 64));
        menu.clicked(36, 0, ContainerInput.PICKUP, player);
        menu.clicked(37, 0, ContainerInput.PICKUP, player);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        context.assertTrue(menu.getCarried().isEmpty(), "Detached fuel view must not duplicate stored coal onto cursor");
        context.assertTrue(new ItemComponentContainer(player.getInventory().getItem(1), 1).getItem(0).getCount() == 64,
                "Moved stocked drill must retain exactly its original 64 coal");
        context.succeed();
    }

    @GameTest
    public void greatswordEntitySweepPrecedesVillagerTrading(GameTestHelper context) {
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.GREATSWORDS.get(WeaponBalance.Tier.IRON)));
        var primary = context.spawnMob(EntityTypes.VILLAGER, 1, 1, 1).withNoFreeWill().spawn();
        primary.setPos(primary.getX(), primary.getY() + 5.0D, primary.getZ());
        player.setPos(primary.getX() - 1.0D, primary.getY(), primary.getZ());
        player.setYRot(-90.0F);
        player.setXRot(0.0F);
        context.assertTrue(player.hasLineOfSight(primary), "Test villager must be unobstructed");
        float before = primary.getHealth();
        player.interactOn(primary, InteractionHand.MAIN_HAND, primary.position());
        context.assertTrue(primary.getHealth() < before, "Villager interaction must not consume the Greatsword attack");
        context.assertTrue(player.containerMenu == player.inventoryMenu, "Attack must not open a trade menu");
        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
