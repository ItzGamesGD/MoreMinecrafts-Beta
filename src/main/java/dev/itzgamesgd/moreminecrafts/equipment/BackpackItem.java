package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;

public final class BackpackItem extends Item {
    private final int capacity;
    public BackpackItem(int capacity, Properties properties) { super(properties); this.capacity = capacity; }
    public int capacity() { return capacity; }

    @Override public boolean overrideStackedOnOther(ItemStack backpack, Slot slot, ClickAction action, Player player) {
        ItemStack armor = slot.getItem();
        var equippable = armor.get(DataComponents.EQUIPPABLE);
        if (action != ClickAction.SECONDARY || equippable == null || equippable.slot() != EquipmentSlot.CHEST
                || armor.has(ModDataComponents.ATTACHED_BACKPACK)) return false;
        armor.set(ModDataComponents.ATTACHED_BACKPACK, backpack.copyWithCount(1));
        backpack.shrink(1);
        slot.setChanged();
        return true;
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        open((ServerPlayer) player);
        return InteractionResult.SUCCESS_SERVER;
    }

    public void open(ServerPlayer player) {
        var container = new PlayerBackpackContainer(player, capacity);
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> switch (capacity) {
            case 9 -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inventory, container, 1);
            case 27 -> ChestMenu.threeRows(id, inventory, container);
            case 54 -> ChestMenu.sixRows(id, inventory, container);
            default -> throw new IllegalStateException("Unsupported backpack capacity " + capacity);
        }, Component.translatable("container.moreminecrafts.backpack")));
    }

    private static final class PlayerBackpackContainer extends net.minecraft.world.SimpleContainer {
        private final ServerPlayer player;
        private BackpackStorage storage;
        PlayerBackpackContainer(ServerPlayer player, int visibleSize) {
            super(visibleSize);
            this.player = player;
            this.storage = player.getAttachedOrCreate(EquipmentAttachments.BACKPACK);
        }
        @Override public ItemStack getItem(int slot) { return storage.items().get(slot); }
        @Override public ItemStack removeItem(int slot, int count) {
            ItemStack current = storage.items().get(slot);
            ItemStack removed = current.split(count);
            setChanged(); return removed;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack result = storage.items().get(slot);
            storage.items().set(slot, ItemStack.EMPTY); setChanged(); return result;
        }
        @Override public void setItem(int slot, ItemStack stack) {
            storage.items().set(slot, stack); setChanged();
        }
        @Override public void setChanged() {
            super.setChanged();
            player.setAttached(EquipmentAttachments.BACKPACK, storage);
        }
    }
}
