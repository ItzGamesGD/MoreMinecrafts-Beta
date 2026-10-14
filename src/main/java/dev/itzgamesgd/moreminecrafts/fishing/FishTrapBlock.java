package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class FishTrapBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

    public FishTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT));
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return context.getLevel().getBlockState(context.getClickedPos().relative(facing)).canBeReplaced()
                ? defaultBlockState().setValue(FACING, facing).setValue(PART, BedPart.FOOT) : null;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.relative(state.getValue(FACING)), state.setValue(PART, BedPart.HEAD), 3);
    }

    private static BlockPos primary(BlockPos pos, BlockState state) {
        return state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(state.getValue(FACING).getOpposite());
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(primary(pos, state)) instanceof FishTrapBlockEntity trap)
            player.openMenu(trap);
        return InteractionResult.SUCCESS;
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos owner = primary(pos, state);
        if (!level.isClientSide() && level.getBlockEntity(owner) instanceof FishTrapBlockEntity trap)
            net.minecraft.world.Containers.dropContents(level, owner, trap);
        if (!level.isClientSide() && !player.isCreative() && state.getValue(PART) == BedPart.FOOT
                && level instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().get(GameRules.BLOCK_DROPS)) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, new ItemStack(this));
        }
        BlockPos other = state.getValue(PART) == BedPart.FOOT ? pos.relative(state.getValue(FACING))
                : pos.relative(state.getValue(FACING).getOpposite());
        if (level.getBlockState(other).is(this)) level.removeBlock(other, false);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == BedPart.FOOT ? new FishTrapBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(PART) == BedPart.HEAD ? null
                : createTickerHelper(type, ModBlockEntities.FISH_TRAP,
                    (world, pos, currentState, trap) -> FishTrapBlockEntity.serverTick((ServerLevel) world, pos, currentState, trap));
    }
}
