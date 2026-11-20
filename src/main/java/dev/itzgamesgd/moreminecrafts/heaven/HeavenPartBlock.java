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
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Heaven sky island P2 vegetation block: a family of reusable model parts described by a generated table
 * ({@code data/moreminecrafts/heaven/flora/<block>.json}, written by {@code tools/heaven-p2/build_p2.py}).
 *
 * <ul>
 *   <li>{@code part}: which model of the family (omitted when the family has a single model);</li>
 *   <li>{@code facing}: optional horizontal rotation (models are authored facing north and rotated by the
 *       blockstate, so one model serves four orientations);</li>
 *   <li>outline = the part's voxel boxes; collision = only the voxels the table marks as colliding
 *       (giant-flower stems). Every other part is passable.</li>
 * </ul>
 * Gameplay behaviour (survival, growth, drops, bone meal, fire) is deliberately absent: P2 only specifies it
 * (docs/design/heaven_sky_island_p2_2026-11-20/README.md, policy table) pending owner approval.
 */
public class HeavenPartBlock extends Block {
    private record Spec(int count, boolean facing, IntegerProperty[] created) {}

    /** Carries the table size into {@link #createBlockStateDefinition}, which runs inside the super constructor. */
    private static final ThreadLocal<Spec> PENDING = new ThreadLocal<>();
    private static final Direction[] ORDER = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private final String family;
    private final int count;
    private final IntegerProperty part;
    private final boolean facing;
    private final VoxelShape[][] outline;
    private final VoxelShape[][] collision;

    public static HeavenPartBlock create(String name, BlockBehaviour.Properties properties) {
        JsonObject table = read("/data/moreminecrafts/heaven/flora/" + name + ".json");
        Spec spec = new Spec(table.get("count").getAsInt(), table.get("facing").getAsBoolean(), new IntegerProperty[1]);
        if (spec.count() < 1) throw new IllegalStateException("empty part table for " + name);
        PENDING.set(spec);
        try {
            return new HeavenPartBlock(name, properties, table, spec);
        } finally {
            PENDING.remove();
        }
    }

    private HeavenPartBlock(String family, BlockBehaviour.Properties properties, JsonObject table, Spec spec) {
        super(properties);
        this.family = family;
        this.count = spec.count();
        this.part = spec.created()[0];
        this.facing = spec.facing();
        JsonArray parts = table.getAsJsonArray("parts");
        if (parts.size() != count) throw new IllegalStateException("part table size mismatch for " + family);
        outline = new VoxelShape[count][4];
        collision = new VoxelShape[count][4];
        for (int i = 0; i < count; i++) {
            JsonObject p = parts.get(i).getAsJsonObject();
            for (int r = 0; r < 4; r++) {
                outline[i][r] = shape(p.getAsJsonArray("outline"), r);
                collision[i][r] = shape(p.getAsJsonArray("collision"), r);
            }
        }
        BlockState state = stateDefinition.any();
        if (part != null) state = state.setValue(part, 0);
        if (facing) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Spec spec = PENDING.get();
        if (spec == null) throw new IllegalStateException("HeavenPartBlock must be built through create()");
        if (spec.count() > 1) {
            spec.created()[0] = IntegerProperty.create("part", 0, spec.count() - 1);
            builder.add(spec.created()[0]);
        }
        if (spec.facing()) builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static JsonObject read(String path) {
        try (InputStream in = HeavenPartBlock.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing heaven flora table " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Boxes in model pixels (authored facing north), rotated clockwise r quarter turns like blockstate "y". */
    private static VoxelShape shape(JsonArray boxes, int r) {
        VoxelShape shape = Shapes.empty();
        for (JsonElement e : boxes) {
            JsonArray b = e.getAsJsonArray();
            double x0 = b.get(0).getAsDouble(), y0 = b.get(1).getAsDouble(), z0 = b.get(2).getAsDouble();
            double x1 = b.get(3).getAsDouble(), y1 = b.get(4).getAsDouble(), z1 = b.get(5).getAsDouble();
            for (int k = 0; k < r; k++) {           // (x, z) -> (16 - z, x)
                double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
                x0 = nx0; x1 = nx1; z0 = nz0; z1 = nz1;
            }
            shape = Shapes.or(shape, Block.box(x0, y0, z0, x1, y1, z1));
        }
        return shape.optimize();
    }

    private int rotation(BlockState state) {
        if (!facing) return 0;
        Direction d = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (int i = 0; i < 4; i++) if (ORDER[i] == d) return i;
        return 0;
    }

    private int partOf(BlockState state) {
        return part == null ? 0 : state.getValue(part);
    }

    public String family() { return family; }
    public int partCount() { return count; }
    public IntegerProperty partProperty() { return part; }
    public boolean hasFacing() { return facing; }
    public VoxelShape collisionOf(int partIndex) { return collision[partIndex][0]; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return outline[partOf(state)][rotation(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collision[partOf(state)][rotation(state)];
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
