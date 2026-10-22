package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class AccessoryEnhancementGameTests implements CustomTestMethodInvoker {
    @GameTest public void anvilEnhancementConsumesExactResourcesAndPreservesComponents(GameTestHelper ctx) {
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.giveExperiencePoints(10_000);
        var position=ctx.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        ctx.getLevel().setBlock(position,net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState(),3);
        player.setPos(position.getX()+.5,position.getY()+1,position.getZ()+.5);
        var menu=new AnvilMenu(20,player.getInventory(),ContainerLevelAccess.create(ctx.getLevel(),position));
        var modes=(T1AnvilModes)menu;
        ItemStack charm=new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM));
        charm.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Kept name"));
        CompoundTag customTag=new CompoundTag();customTag.putString("owner_note","kept component");
        charm.set(DataComponents.CUSTOM_DATA,CustomData.of(customTag));
        menu.getSlot(0).set(charm);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,2));
        int xpBefore=RawXp.available(player);
        player.getRandom().setSeed(noWearSeed(player));
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"accessory +0 to +1 succeeds through the anvil action");
        ctx.assertTrue(AccessoryEnhancementOperation.level(menu.getSlot(0).getItem())==1,"enhancement component advances once");
        ctx.assertTrue(menu.getSlot(1).getItem().getCount()==1,"ordinary accessory step consumes one Enhancement Stone");
        ctx.assertTrue(RawXp.available(player)==xpBefore-40,"accessory step consumes exactly 40 raw XP");
        ctx.assertTrue(menu.getSlot(0).getItem().get(DataComponents.CUSTOM_NAME).getString().equals("Kept name"),"custom name survives enhancement");
        ctx.assertTrue(java.util.Optional.of("kept component").equals(
                menu.getSlot(0).getItem().get(DataComponents.CUSTOM_DATA).copyTag().getString("owner_note")),"custom item data survives enhancement");

        var ops=RegistryOps.create(NbtOps.INSTANCE,ctx.getLevel().registryAccess());
        var encoded=ItemStack.CODEC.encodeStart(ops,menu.getSlot(0).getItem()).getOrThrow();
        ItemStack restored=ItemStack.CODEC.parse(ops,encoded).getOrThrow();
        ctx.assertTrue(AccessoryEnhancementOperation.level(restored)==1,"enhancement level survives stack save/load");
        ctx.assertTrue(restored.get(DataComponents.CUSTOM_NAME).getString().equals("Kept name"),"name survives stack save/load");
        player.getInventory().add(restored);
        boolean transferred=false;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++)
            transferred|=AccessoryEnhancementOperation.level(player.getInventory().getItem(slot))==1;
        ctx.assertTrue(transferred,"enhanced accessory survives inventory transfer");
        ctx.succeed();
    }

    @GameTest public void milestoneAndRejectedAnvilTransactionsAreAtomic(GameTestHelper ctx) {
        var position=ctx.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        ctx.getLevel().setBlock(position,net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState(),3);
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.giveExperiencePoints(2_000);
        player.setPos(position.getX()+.5,position.getY()+1,position.getZ()+.5);
        var menu=new AnvilMenu(21,player.getInventory(),ContainerLevelAccess.create(ctx.getLevel(),position));
        var modes=(T1AnvilModes)menu;
        ItemStack ring=new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.RING_OF_LIFE));
        ring.set(ModDataComponents.ENHANCEMENT_LEVEL,4);
        menu.getSlot(0).set(ring);menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,2));
        menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).set(new ItemStack(ModItems.RELIC_CATALYST));
        int xpBefore=RawXp.available(player);
        player.getRandom().setSeed(noWearSeed(player));
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"+5 milestone succeeds with two stones and one catalyst");
        ctx.assertTrue(AccessoryEnhancementOperation.level(menu.getSlot(0).getItem())==5,"milestone advances exactly to +5");
        ctx.assertTrue(menu.getSlot(1).getItem().isEmpty(),"milestone consumes both stones");
        ctx.assertTrue(menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).getItem().isEmpty(),"milestone consumes its catalyst");
        ctx.assertTrue(RawXp.available(player)==xpBefore-40,"+5 milestone consumes 40 raw XP");

        ring.set(ModDataComponents.ENHANCEMENT_LEVEL,9);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,2));
        menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).set(new ItemStack(ModItems.RELIC_CATALYST));
        int levelBefore=RawXp.available(player);
        player.getRandom().setSeed(noWearSeed(player));
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"+10 milestone succeeds with its complete inputs");
        ctx.assertTrue(AccessoryEnhancementOperation.level(ring)==10,"accessory reaches the approved +10 maximum");
        ctx.assertTrue(levelBefore-RawXp.available(player)==70,"+10 milestone consumes 70 raw XP");

        ring.set(ModDataComponents.ENHANCEMENT_LEVEL,10);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,64));
        menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).set(new ItemStack(ModItems.RELIC_CATALYST));
        int maxXp=RawXp.available(player);
        ctx.assertTrue(!modes.moreminecrafts$button(player,3),"+11 is rejected");
        ctx.assertTrue(AccessoryEnhancementOperation.level(ring)==10
                &&menu.getSlot(1).getItem().getCount()==64
                &&menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).getItem().getCount()==1
                &&RawXp.available(player)==maxXp,"rejected +11 consumes no item or XP");

        ring.set(ModDataComponents.ENHANCEMENT_LEVEL,4);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,2));
        menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).set(ItemStack.EMPTY);
        int missingCatalystXp=RawXp.available(player);
        ctx.assertTrue(!modes.moreminecrafts$button(player,3),"missing milestone catalyst rejects operation");
        ctx.assertTrue(AccessoryEnhancementOperation.level(ring)==4&&menu.getSlot(1).getItem().getCount()==2
                &&RawXp.available(player)==missingCatalystXp,"missing catalyst leaves all transaction state unchanged");

        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE));
        player.getRandom().setSeed(noWearSeed(player));
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"ordinary T1/T2 enhancement still uses its existing path");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation.level(menu.getSlot(0).getItem())==1,
                "ordinary item remains on the ordinary enhancement cap/policy");
        ctx.succeed();
    }

    @GameTest public void postMilestoneCostAndRejectedDuplicateInputsAreAtomic(GameTestHelper ctx) {
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.giveExperiencePoints(1_000);
        var menu=new AnvilMenu(24,player.getInventory());
        var modes=(T1AnvilModes)menu;
        ItemStack accessory=new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.PHANTOM_MIRROR));
        accessory.set(ModDataComponents.ENHANCEMENT_LEVEL,5);
        menu.getSlot(0).set(accessory);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE));
        int xpBefore=RawXp.available(player);
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"+5 to +6 succeeds without a milestone catalyst");
        ctx.assertTrue(AccessoryEnhancementOperation.level(accessory)==6,"post-milestone step advances exactly once");
        ctx.assertTrue(menu.getSlot(1).getItem().isEmpty(),"+6 consumes one Enhancement Stone");
        ctx.assertTrue(RawXp.available(player)==xpBefore-70,"+6 consumes exactly 70 raw XP");

        // A duplicated request without a second stone cannot repeat the successful transition.
        accessory.set(ModDataComponents.ENHANCEMENT_LEVEL,0);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE));
        int duplicateXpBefore=RawXp.available(player);
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"first +1 request succeeds");
        int afterFirst=RawXp.available(player);
        ctx.assertTrue(!modes.moreminecrafts$button(player,3),"repeated request without resources is rejected");
        ctx.assertTrue(AccessoryEnhancementOperation.level(accessory)==1
                &&menu.getSlot(1).getItem().isEmpty()
                &&afterFirst==duplicateXpBefore-40
                &&RawXp.available(player)==afterFirst,"repeated request neither reapplies nor consumes extra XP/material");

        accessory.set(ModDataComponents.ENHANCEMENT_LEVEL,6);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE));
        player.giveExperiencePoints(-RawXp.available(player));
        int noXp=RawXp.available(player);
        ctx.assertTrue(!modes.moreminecrafts$button(player,3),"insufficient raw XP rejects +7");
        ctx.assertTrue(AccessoryEnhancementOperation.level(accessory)==6
                &&menu.getSlot(1).getItem().getCount()==1
                &&RawXp.available(player)==noXp,"insufficient XP leaves enhancement, stone, and XP unchanged");

        accessory.set(ModDataComponents.ENHANCEMENT_LEVEL,4);
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        menu.getSlot(((T1AnvilModes)menu).moreminecrafts$catalystSlotIndex()).set(new ItemStack(ModItems.RELIC_CATALYST));
        ctx.assertTrue(!modes.moreminecrafts$button(player,3),"wrong enhancement material is rejected");
        ctx.assertTrue(AccessoryEnhancementOperation.level(accessory)==4
                &&menu.getSlot(1).getItem().is(Items.DIAMOND)
                &&menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).getItem().is(ModItems.RELIC_CATALYST)
                &&RawXp.available(player)==noXp,"wrong material consumes no input, catalyst, or XP");

        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,2));
        menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).set(new ItemStack(Items.DIAMOND));
        ctx.assertTrue(!modes.moreminecrafts$button(player,3),"wrong catalyst is rejected at +5");
        ctx.assertTrue(AccessoryEnhancementOperation.level(accessory)==4
                &&menu.getSlot(1).getItem().getCount()==2
                &&menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).getItem().is(Items.DIAMOND)
                &&RawXp.available(player)==noXp,"wrong catalyst consumes no input or XP");
        ctx.succeed();
    }

    @GameTest public void catalystQuickMoveAndMenuCloseConserveTheInput(GameTestHelper ctx) {
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new AnvilMenu(22,player.getInventory());
        var modes=(T1AnvilModes)menu;
        player.getInventory().setItem(9,new ItemStack(ModItems.RELIC_CATALYST));
        int inventorySlot=-1;
        for(int slot=3;slot<modes.moreminecrafts$catalystSlotIndex();slot++)
            if(menu.getSlot(slot).getItem().is(ModItems.RELIC_CATALYST)){inventorySlot=slot;break;}
        ctx.assertTrue(inventorySlot>=3,"test catalyst is visible in the anvil menu inventory range");
        menu.quickMoveStack(player,inventorySlot);
        int catalystSlot=modes.moreminecrafts$catalystSlotIndex();
        ctx.assertTrue(menu.getSlot(catalystSlot).getItem().is(ModItems.RELIC_CATALYST),"shift-click moves catalyst into the dedicated input");
        menu.quickMoveStack(player,catalystSlot);
        ctx.assertTrue(menu.getSlot(catalystSlot).getItem().isEmpty(),"shift-click returns catalyst to inventory");
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++)
            if(player.getInventory().getItem(slot).is(ModItems.RELIC_CATALYST))player.getInventory().setItem(slot,ItemStack.EMPTY);
        menu.getSlot(catalystSlot).set(new ItemStack(ModItems.RELIC_CATALYST));
        menu.removed(player);
        int catalystCount=0;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++)
            catalystCount+=player.getInventory().getItem(slot).getCount();
        ctx.assertTrue(catalystCount==1,"closing the anvil returns the catalyst without duplication or loss");

        for(int slot=0;slot<player.getInventory().getContainerSize();slot++)
            player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
        var fullMenu=new AnvilMenu(23,player.getInventory());
        var fullModes=(T1AnvilModes)fullMenu;
        fullMenu.getSlot(fullModes.moreminecrafts$catalystSlotIndex()).set(new ItemStack(ModItems.RELIC_CATALYST));
        fullMenu.removed(player);
        var dropped=ctx.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(2),
                item->item.getItem().is(ModItems.RELIC_CATALYST));
        ctx.assertTrue(dropped.size()==1,"a full inventory receives the catalyst as one world drop");
        ctx.succeed();
    }

    private static long noWearSeed(net.minecraft.world.entity.player.Player player) {
        var random=player.getRandom();
        for(long seed=0;seed<1_000_000;seed++){
            random.setSeed(seed);
            if(random.nextDouble()>=.12)return seed;
        }
        throw new AssertionError("Could not find deterministic no-wear anvil seed");
    }

    @Override public void invokeTestMethod(GameTestHelper context,Method method)throws ReflectiveOperationException {
        method.invoke(this,context);
    }
}
