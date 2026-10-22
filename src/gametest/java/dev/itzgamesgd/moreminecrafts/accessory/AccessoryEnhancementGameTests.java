package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.progression.RawXp;
import dev.itzgamesgd.moreminecrafts.progression.T1AnvilModes;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
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

public final class AccessoryEnhancementGameTests {
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
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"+5 milestone succeeds with two stones and one catalyst");
        ctx.assertTrue(AccessoryEnhancementOperation.level(menu.getSlot(0).getItem())==5,"milestone advances exactly to +5");
        ctx.assertTrue(menu.getSlot(1).getItem().isEmpty(),"milestone consumes both stones");
        ctx.assertTrue(menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).getItem().isEmpty(),"milestone consumes its catalyst");
        ctx.assertTrue(RawXp.available(player)==xpBefore-40,"+5 milestone consumes 40 raw XP");

        ring.set(ModDataComponents.ENHANCEMENT_LEVEL,9);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,2));
        menu.getSlot(modes.moreminecrafts$catalystSlotIndex()).set(new ItemStack(ModItems.RELIC_CATALYST));
        int levelBefore=RawXp.available(player);
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
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"ordinary T1/T2 enhancement still uses its existing path");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation.level(menu.getSlot(0).getItem())==1,
                "ordinary item remains on the ordinary enhancement cap/policy");
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
        ctx.succeed();
    }
}
