package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.registry.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Real server container: a 5x5 planting stock group and 5x5 collected crop output group. */
public final class AutoFarmMenu extends AbstractContainerMenu {
    private final Container farm;
    public AutoFarmMenu(int id, Inventory inv) { this(id, inv, new SimpleContainer(50)); }
    public AutoFarmMenu(int id, Inventory inv, Container farm) {
        super(ModMenuTypes.AUTO_FARM, id);
        this.farm = farm;
        for (int i = 0; i < 50; i++) {
            int local=i%25, x=(i<25?10:122)+(local%5)*18, y=32+(local/5)*18;
            addSlot(new Slot(farm,i,x,y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return index < 25 && super.mayPlace(stack);
                }
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inv,9+row*9+col,30+col*18,130+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,30+col*18,188));
    }
    @Override public boolean stillValid(Player player){return farm.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int slotIndex) {
        if(slotIndex<0 || slotIndex>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(slotIndex);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack input=slot.getItem(),old=input.copy();
        boolean moved;
        if(slotIndex<50)moved=moveItemStackTo(input,50,86,true);
        else moved=moveItemStackTo(input,0,25,false);
        if(!moved)return ItemStack.EMPTY;
        if(input.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        return old;
    }
}
