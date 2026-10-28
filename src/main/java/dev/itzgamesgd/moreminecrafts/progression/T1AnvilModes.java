package dev.itzgamesgd.moreminecrafts.progression;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public interface T1AnvilModes {
    int moreminecrafts$mode();
    int moreminecrafts$catalystSlotIndex();
    ItemStack moreminecrafts$quickMoveCatalyst(Player player,int slotIndex);
    void moreminecrafts$returnCatalyst(Player player);
    boolean moreminecrafts$button(Player player,int action);
}
