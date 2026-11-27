package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Atomically detaches the equipped stacks before the vanilla death drop/respawn copy path. */
public final class AccessoryDeathDrops {
    private AccessoryDeathDrops() {}

    public static List<ItemStack> detachForDeath(ServerPlayer player, boolean keepInventory) {
        if (keepInventory) return List.of();
        AccessoryLoadout current = player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
        if (current.isEmpty()) return List.of();
        player.setAttached(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty());
        return current.stacks().stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
    }
}
