package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class PotionInjectorItem extends Item {
    public PotionInjectorItem(Properties properties) { super(properties); }

    public static boolean isUsablePotion(ItemStack stack) {
        return !stack.isEmpty() && isUsablePotionItem(stack.getItem(), stack.has(DataComponents.POTION_CONTENTS));
    }

    static boolean isUsablePotionItem(Item item, boolean hasPotionContents) {
        return item == Items.POTION && hasPotionContents;
    }

    public static void open(ServerPlayer player, ItemStack injector) {
        var inventory = new ItemComponentContainer(injector, LowerEquipmentRules.INJECTOR_SLOTS);
        player.openMenu(new SimpleMenuProvider((id, playerInventory, ignored) ->
                new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, playerInventory,
                        new PotionInjectorContainer(player, injector, inventory), 1),
                Component.translatable("container.moreminecrafts.potion_injector")));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack injector = player.getItemInHand(hand);
        var inventory = new ItemComponentContainer(injector, LowerEquipmentRules.INJECTOR_SLOTS);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack potion = inventory.getItem(slot);
            if (!isUsablePotion(potion)) continue;
            potion.get(DataComponents.POTION_CONTENTS).applyToLivingEntity(player, 1.0F);
            potion.shrink(1);
            inventory.setItem(slot, potion);
            if (!player.addItem(new ItemStack(Items.GLASS_BOTTLE)))
                player.spawnAtLocation((net.minecraft.server.level.ServerLevel) level, new ItemStack(Items.GLASS_BOTTLE));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    private static final class PotionInjectorContainer extends net.minecraft.world.SimpleContainer {
        private final ItemComponentContainer backing;
        private final ServerPlayer player;
        private final ItemStack owner;
        PotionInjectorContainer(ServerPlayer player, ItemStack owner, ItemComponentContainer backing) {
            super(9); this.player = player; this.owner = owner; this.backing = backing;
        }
        private boolean ownsSource() {
            if (player.getMainHandItem() == owner || player.getOffhandItem() == owner) return true;
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (inventory.getItem(slot) == owner) return true;
            }
            return false;
        }
        @Override public ItemStack getItem(int slot) {
            return slot < 9 && ownsSource() ? backing.getItem(slot) : ItemStack.EMPTY;
        }
        @Override public ItemStack removeItem(int slot, int count) {
            return slot < 9 && ownsSource() ? backing.removeItem(slot, count) : ItemStack.EMPTY;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            return slot < 9 && ownsSource() ? backing.removeItemNoUpdate(slot) : ItemStack.EMPTY;
        }
        @Override public void setItem(int slot, ItemStack stack) {
            if (slot < 9 && ownsSource()) backing.setItem(slot, stack);
        }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < 9 && ownsSource() && isUsablePotion(stack);
        }
        @Override public int getMaxStackSize() { return 1; }
    }
}
