package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

public final class FishingEquipmentMenu {
    private FishingEquipmentMenu() {}

    public static void open(ServerPlayer player, ItemStack rod) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, owner) -> new FishingRodMenu(id, inventory, rod),
                Component.translatable("menu.moreminecrafts.fishing_equipment")));
    }
}
