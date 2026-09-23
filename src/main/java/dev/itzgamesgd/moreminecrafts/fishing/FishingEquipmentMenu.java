package dev.itzgamesgd.moreminecrafts.fishing;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
public final class FishingEquipmentMenu {
 private FishingEquipmentMenu(){}
 public static void open(ServerPlayer player,ItemStack rod){RodEquipmentContainer c=new RodEquipmentContainer(rod);player.openMenu(new SimpleMenuProvider((id,inventory,p)->ChestMenu.oneRow(id,inventory,c),Component.translatable("menu.moreminecrafts.fishing_equipment")));}
}
