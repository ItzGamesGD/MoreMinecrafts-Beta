package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.registry.ModMenuTypes;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Vanilla container transport; the client only requests actions, never XP amounts or rolled values. */
public final class EnchantingProgressionMenu extends AbstractContainerMenu {
    private final SimpleContainer inputs=new SimpleContainer(2);
    private final ContainerLevelAccess access;
    private final DataSlot mode=DataSlot.standalone();
    public EnchantingProgressionMenu(int id,Inventory inventory){this(id,inventory,ContainerLevelAccess.NULL);}
    public EnchantingProgressionMenu(int id,Inventory inventory,ContainerLevelAccess access) {
        super(ModMenuTypes.ENCHANTING_PROGRESSION,id);this.access=access;addDataSlot(mode);
        addSlot(new Slot(inputs,0,12,46){@Override public boolean mayPlace(ItemStack stack){return EquipmentTreeResolver.resolve(stack)!=EnchantmentTrees.Equipment.NONE;}@Override public int getMaxStackSize(){return 1;}});
        addSlot(new Slot(inputs,1,12,74){@Override public boolean mayPlace(ItemStack stack){return T1Operations.book(stack).isPresent();}});
        addStandardInventorySlots(inventory,78,202);
    }
    public int mode(){return mode.get();}
    public java.util.List<EnchantmentTrees.Node> tree(){return EnchantmentTrees.tree(EquipmentTreeResolver.resolve(inputs.getItem(0)));}
    @Override public boolean clickMenuButton(Player player,int action) {
        if(player.level().isClientSide() || !stillValid(player))return false;
        if(action==0||action==1){mode.set(action);broadcastChanges();return true;}
        boolean changed=false;
        if(action==2 && mode()==1)changed=T1Operations.apply(player,inputs.getItem(0),inputs.getItem(1),player.getRandom()::nextDouble);
        else if(action>=100&&action<200&&mode()==0){int index=action-100;var tree=tree();if(index<tree.size())changed=T1Operations.purchase(player,inputs.getItem(0),tree.get(index).id());}
        else if(action>=200&&action<300&&mode()==1){int index=action-200;var tree=tree();if(index<tree.size())changed=T1Operations.reset(player,inputs.getItem(0),tree.get(index).id());}
        if(changed){inputs.setChanged();broadcastChanges();}return changed;
    }
    @Override public boolean stillValid(Player player){return stillValid(access,player,Blocks.ENCHANTING_TABLE);}
    @Override public void removed(Player player){super.removed(player);access.execute((level,pos)->clearContainer(player,inputs));}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<2){if(!moveItemStackTo(stack,2,slots.size(),true))return ItemStack.EMPTY;}
        else {int target=T1Operations.book(stack).isPresent()?1:0;if(!slots.get(target).mayPlace(stack)||!moveItemStackTo(stack,target,target+1,false))return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;
    }
}
