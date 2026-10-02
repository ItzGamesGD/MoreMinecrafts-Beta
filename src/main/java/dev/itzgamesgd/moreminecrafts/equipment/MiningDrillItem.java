package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

public final class MiningDrillItem extends Item {
    public static final float MINING_SPEED = 1000.0F;
    public MiningDrillItem(Properties properties) { super(properties); }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack drill = player.getItemInHand(hand);
        var fuel = new DrillFuelContainer(drill);
        ((ServerPlayer) player).openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                new ChestMenu(MenuType.GENERIC_9x1, id, inventory, fuel, 1),
                Component.translatable("container.moreminecrafts.mining_drill")));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override public float getDestroySpeed(ItemStack drill, BlockState state) {
        return LowerEquipmentRules.isPickaxeMineable(state) && hasFuel(drill) ? MINING_SPEED : 1.0F;
    }

    @Override public boolean mineBlock(ItemStack drill, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) consumeOneFuelPoint(drill);
        return true; // deliberately avoids vanilla durability damage while retaining normal break/drop pipeline
    }

    public static boolean hasFuel(ItemStack drill) {
        if (drill.getOrDefault(ModDataComponents.DRILL_FUEL_POINTS, 0) > 0) return true;
        var fuel = new ItemComponentContainer(drill, 1).getItem(0);
        return fuel.is(Items.COAL) || fuel.is(Items.COAL_BLOCK);
    }

    public static boolean consumeOneFuelPoint(ItemStack drill) {
        int points = drill.getOrDefault(ModDataComponents.DRILL_FUEL_POINTS, 0);
        if (points == 0) {
            var fuel = new ItemComponentContainer(drill, 1);
            ItemStack stored = fuel.getItem(0);
            if (stored.is(Items.COAL)) points = LowerEquipmentRules.DRILL_COAL_FP;
            else if (stored.is(Items.COAL_BLOCK)) points = LowerEquipmentRules.DRILL_COAL_BLOCK_FP;
            else return false;
            stored.shrink(1);
            fuel.setItem(0, stored);
        }
        drill.set(ModDataComponents.DRILL_FUEL_POINTS, points - 1);
        return true;
    }

    private static final class DrillFuelContainer extends net.minecraft.world.SimpleContainer {
        private final ItemComponentContainer backing;
        DrillFuelContainer(ItemStack drill) { super(9); backing = new ItemComponentContainer(drill, 1); }
        @Override public ItemStack getItem(int slot) { return slot == 0 ? backing.getItem(0) : ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int count) { return slot == 0 ? backing.removeItem(0, count) : ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 0 ? backing.removeItemNoUpdate(0) : ItemStack.EMPTY; }
        @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) backing.setItem(0, stack); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot == 0 && (stack.is(Items.COAL) || stack.is(Items.COAL_BLOCK));
        }
        @Override public int getMaxStackSize() { return LowerEquipmentRules.DRILL_FUEL_SLOT_LIMIT; }
    }
}
