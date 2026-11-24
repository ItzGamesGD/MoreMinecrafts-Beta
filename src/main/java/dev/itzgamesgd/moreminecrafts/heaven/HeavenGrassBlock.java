package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Heaven grass (P3, owner-approved F1 / F4).
 *
 * <ul>
 *   <li>F1: vanilla {@link SpreadingSnowyBlock} rules with {@code heaven_loam} as base block: spreads ONLY onto heaven
 *       loam (light 9+ above), and turns into heaven loam when covered / too dark (vanilla grass light rule). The
 *       inherited {@code snowy} state uses the same model (no snow texture; heaven has no snow).</li>
 *   <li>F4: bone meal grows heaven vegetation. Algorithm and constants mirror vanilla {@code GrassBlock} (128 random-walk
 *       attempts, 12.5% flower, 10% short-to-tall grass); the composition (6 heaven small flowers, heaven short / tall
 *       grass) is the P3 provisional choice.</li>
 * </ul>
 * Shovel path / hoe farmland (F3) and vanilla plant survival (F2) are data tags, not code.
 */
public class HeavenGrassBlock extends SpreadingSnowyBlock implements BonemealableBlock {
    public static final ResourceKey<Block> LOAM = ResourceKey.create(Registries.BLOCK, ModIds.id("heaven_loam"));
    static final int ATTEMPTS = 128;
    static final float TALL_GRASS_CHANCE = 0.1F;
    static final float FLOWER_CHANCE = 0.125F;
    static final List<String> FLOWERS = List.of("heaven_flower_red", "heaven_flower_yellow", "heaven_flower_white",
            "heaven_flower_pink", "heaven_flower_blue", "heaven_flower_violet");

    public HeavenGrassBlock(Properties properties) {
        super(properties, LOAM);
    }

    private static Block block(String name) {
        return BuiltInRegistries.BLOCK.getValue(ModIds.id(name));
    }

    /** A random part / facing of a heaven part family (deterministic per random source). */
    private static BlockState variant(Block b, RandomSource random) {
        BlockState s = b.defaultBlockState();
        if (b instanceof HeavenPartBlock p) {
            IntegerProperty part = p.partProperty();
            if (part != null) s = s.setValue(part, random.nextInt(p.partCount()));
            if (p.hasFacing()) s = s.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }
        return s;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        Block shortGrass = block("heaven_short_grass"), tallGrass = block("heaven_tall_grass");
        BlockPos above = pos.above();
        attempt:
        for (int i = 0; i < ATTEMPTS; i++) {
            BlockPos p = above;
            for (int j = 0; j < i / 16; j++) {
                p = p.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                if (!level.getBlockState(p.below()).is(this) || level.getBlockState(p).isCollisionShapeFullBlock(level, p)) continue attempt;
            }
            BlockState current = level.getBlockState(p);
            if (current.is(shortGrass) && random.nextFloat() < TALL_GRASS_CHANCE) {
                BlockState tall = variant(tallGrass, random);
                if (tall.canSurvive(level, p)) level.setBlock(p, tall, Block.UPDATE_ALL);
                continue;
            }
            if (!current.isAir()) continue;
            BlockState place = random.nextFloat() < FLOWER_CHANCE
                    ? variant(block(FLOWERS.get(random.nextInt(FLOWERS.size()))), random) : variant(shortGrass, random);
            if (place.canSurvive(level, p)) level.setBlock(p, place, Block.UPDATE_ALL);
        }
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}
