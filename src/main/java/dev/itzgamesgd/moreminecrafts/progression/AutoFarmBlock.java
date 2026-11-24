package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AutoFarmBlock extends BaseEntityBlock {
    public AutoFarmBlock(Properties properties) { super(properties); }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoFarmBlockEntity(pos, state);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AutoFarmBlockEntity farm) {
            farm.recordPlaced(serverPlayer.getUUID());
        }
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AutoFarmBlockEntity farm) {
            if (ResearchRewards.level(serverPlayer, "v3.research.24", "auto_farm_operation") <= 0) {
                serverPlayer.sendSystemMessage(Component.translatable("moreminecrafts.research.auto_farm.locked"));
                return InteractionResult.FAIL;
            }
            farm.recordInteracted(serverPlayer.getUUID());
            player.openMenu(farm);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AutoFarmBlockEntity farm)
            net.minecraft.world.Containers.dropContents(level, pos, farm);
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.AUTO_FARM,
            (world, pos, current, farm) -> AutoFarmBlockEntity.serverTick((ServerLevel) world,pos,current,farm));
    }
}
