package dev.itzgamesgd.moreminecrafts.heaven;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * PROTOTYPE vegetation block for the heaven sky island P1 prototype.
 *
 * <p>Each {@code part} state is one block cell of the V5 design voxel grid (quarter-block voxels =
 * 4 model units), baked by {@code tools/heaven-prototype/build_prototype.py}. Outline shapes are the
 * cell's voxel boxes; only giant-flower stems and tree trunks collide (the design's lenient rule),
 * every other part (petals, leaves, flowers, grass, vines) is passable. Parts have no survival
 * rules, no item and no drops. This is a visual reproduction mechanism, not the production
 * vegetation architecture.
 */
public final class HeavenFloraPrototypeBlock extends Block {
    private static final String TABLE = "/data/moreminecrafts/heaven_prototype/flora_parts.json";
    public static final int PART_COUNT;
    public static final IntegerProperty PART;
    private static final VoxelShape[] OUTLINE;
    private static final VoxelShape[] COLLISION;

    static {
        JsonObject table = readTable();
        JsonArray parts = table.getAsJsonArray("parts");
        PART_COUNT = parts.size();
        if (PART_COUNT != table.get("count").getAsInt() || PART_COUNT == 0) {
            throw new IllegalStateException("Corrupt heaven flora part table: " + TABLE);
        }
        PART = IntegerProperty.create("part", 0, PART_COUNT - 1);
        OUTLINE = new VoxelShape[PART_COUNT];
        COLLISION = new VoxelShape[PART_COUNT];
        for (int i = 0; i < PART_COUNT; i++) {
            JsonObject part = parts.get(i).getAsJsonObject();
            OUTLINE[i] = shape(part.getAsJsonArray("outline"));
            COLLISION[i] = shape(part.getAsJsonArray("collision"));
        }
    }

    public HeavenFloraPrototypeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 0));
    }

    private static JsonObject readTable() {
        try (InputStream in = HeavenFloraPrototypeBlock.class.getResourceAsStream(TABLE)) {
            if (in == null) throw new IllegalStateException("Missing heaven flora part table: " + TABLE);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static VoxelShape shape(JsonArray boxes) {
        VoxelShape shape = Shapes.empty();
        for (JsonElement e : boxes) {
            JsonArray b = e.getAsJsonArray();
            shape = Shapes.or(shape, Block.box(b.get(0).getAsDouble(), b.get(1).getAsDouble(), b.get(2).getAsDouble(),
                    b.get(3).getAsDouble(), b.get(4).getAsDouble(), b.get(5).getAsDouble()));
        }
        return shape.optimize();
    }

    public static VoxelShape outline(int part) { return OUTLINE[part]; }
    public static VoxelShape collision(int part) { return COLLISION[part]; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE[state.getValue(PART)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION[state.getValue(PART)];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
