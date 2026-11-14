package dev.itzgamesgd.moreminecrafts.spidercolony;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ceiling-attached hanging cocoon. Provisional 50:50 hazard (1-2 vanilla spiders) vs reward (1-2 string).
 * Worldgen is deliberately not connected.
 */
public final class HangingCocoonBlock extends Block {
    public static final float HARDNESS_PROVISIONAL = 0.3F;
    public static final BooleanProperty REWARD = BooleanProperty.create("reward");
    private static final VoxelShape SHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 16.0, 12.0);

    public HangingCocoonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(REWARD, true));
    }

    public BlockState naturalState(RandomSource random) {
        return defaultBlockState().setValue(REWARD, random.nextBoolean());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REWARD);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        return level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess scheduledTickAccess,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !player.isCreative()) {
            int count = 1 + server.getRandom().nextInt(2);
            if (state.getValue(REWARD)) {
                Block.popResource(server, pos, new ItemStack(Items.STRING, count));
            } else {
                for (int i = 0; i < count; i++) {
                    for (int attempt = 0; attempt < 8; attempt++) {
                        var spider = EntityType.SPIDER.create(server, EntitySpawnReason.TRIGGERED);
                        if (spider == null) break;
                        spider.setPos(
                                pos.getX() + 0.5 + (server.getRandom().nextDouble() - 0.5) * 2.0,
                                pos.getY() - 0.5,
                                pos.getZ() + 0.5 + (server.getRandom().nextDouble() - 0.5) * 2.0
                        );
                        if (!server.noCollision(spider)) continue;
                        if (server.addFreshEntity(spider)) break;
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
