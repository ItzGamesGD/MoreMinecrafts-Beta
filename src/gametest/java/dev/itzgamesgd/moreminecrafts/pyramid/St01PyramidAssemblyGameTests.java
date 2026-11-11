package dev.itzgamesgd.moreminecrafts.pyramid;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;

/**
 * P8-C2-A actual-asset validation: each whole-pyramid orientation x access route is placed from the exported templates
 * into a real 26.3 server level, then every block of the 81x72x81 completion box and every block entity is dumped for
 * the independent verifier (tools/st01-p8c2a/verify_placement.py). Gated by {@code -Dmm.st01.assembly=true}.
 */
public final class St01PyramidAssemblyGameTests {
    private static final int GROUND_Y = 150;

    private static void run(GameTestHelper ctx, Orientation orientation, AccessRoute route) {
        if (!Boolean.getBoolean("mm.st01.assembly")) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        int index = orientation.ordinal() * 2 + route.ordinal();
        BlockPos ground = new BlockPos(24_000 + 192 * index, GROUND_Y, 24_000);
        BoundingBox box = St01PyramidAssembly.completionBox(ground, orientation);
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        int cleared = 0;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (!level.getBlockState(p).isAir()) { level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS); cleared++; }

        int flags = Block.UPDATE_CLIENTS;
        ctx.assertTrue(St01PyramidAssembly.place(level, level.getStructureTemplateManager(), ground, orientation, route,
                level.getRandom(), flags), "assembly placed " + orientation + "/" + route);
        var placement = St01PyramidAssembly.describe(ground, orientation, route);

        JsonObject out = new JsonObject();
        out.addProperty("orientation", orientation.name());
        out.addProperty("access", route.name());
        out.addProperty("rotation", orientation.rotation.name());
        out.addProperty("flags", flags);
        out.addProperty("pre_cleared_non_air", cleared);
        out.add("ground", vec(ground));
        out.add("frame_min", vec(new BlockPos(box.minX(), box.minY(), box.minZ())));
        out.add("frame_size", vec(new BlockPos(box.getXSpan(), box.getYSpan(), box.getZSpan())));
        Map<String, Integer> lookup = new HashMap<>();
        JsonArray palette = new JsonArray();
        ByteBuffer grid = ByteBuffer.allocate(box.getXSpan() * box.getYSpan() * box.getZSpan() * 2).order(ByteOrder.LITTLE_ENDIAN);
        JsonArray entities = new JsonArray();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            p.set(x, y, z);
            BlockState state = level.getBlockState(p);
            JsonObject s = state(state);
            String key = s.toString();
            Integer id = lookup.get(key);
            if (id == null) { id = palette.size(); lookup.put(key, id); palette.add(s); }
            grid.putShort((short) (int) id);
            BlockEntity be = level.getBlockEntity(p);
            if (be != null) {
                JsonObject e = new JsonObject();
                e.add("pos", vec(p.subtract(new BlockPos(box.minX(), box.minY(), box.minZ()))));
                e.addProperty("type", String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType())));
                e.addProperty("nbt", be.saveWithFullMetadata(level.registryAccess()).toString());
                if (be instanceof Container c) e.addProperty("empty", c.isEmpty());
                entities.add(e);
            }
        }
        out.add("palette", palette);
        out.addProperty("grid_u16le_xyz_b64", Base64.getEncoder().encodeToString(grid.array()));
        out.add("block_entities", entities);

        // Runtime compatibility: the unchanged P8-B2 API, fed only by the assembly's exposed metadata.
        BlockState left = level.getBlockState(placement.primaryLeft()), right = level.getBlockState(placement.primaryRight());
        ctx.assertTrue(left.is(Blocks.CHEST) && left.getValue(ChestBlock.TYPE) == ChestType.LEFT, "PRIMARY left " + left);
        ctx.assertTrue(right.is(Blocks.CHEST) && right.getValue(ChestBlock.TYPE) == ChestType.RIGHT, "PRIMARY right " + right);
        var registered = St01RoyalChestPlacement.registerPlaced(level, placement.d1PlacementOrigin(), placement.d1Settings());
        ctx.assertTrue(registered.isPresent(), "registerPlaced " + orientation + "/" + route);
        var instance = registered.orElseThrow();
        JsonObject reg = new JsonObject();
        reg.addProperty("id", instance.id());
        reg.add("primary", vec(instance.primary()));
        reg.add("primary_right", vec(instance.primaryRight().orElseThrow()));
        reg.add("spawn", vec(instance.spawn()));
        reg.addProperty("secondary_present", instance.secondary().isPresent());
        reg.addProperty("canonical_id_matches", instance.id().equals(RoyalChestRuntime.canonicalId(level, placement.primaryLeft())));
        reg.addProperty("primary_matches_metadata", instance.primary().equals(placement.primaryLeft())
                && instance.primaryRight().orElseThrow().equals(placement.primaryRight()));
        reg.addProperty("spawn_matches_metadata", instance.spawn().equals(placement.guardianSpawn()));
        reg.addProperty("idempotent", St01RoyalChestPlacement.registerPlaced(level, placement.d1PlacementOrigin(),
                placement.d1Settings()).orElseThrow().equals(instance));
        out.add("registration", reg);
        JsonObject meta = new JsonObject();
        meta.add("primary_left", vec(placement.primaryLeft()));
        meta.add("primary_right", vec(placement.primaryRight()));
        meta.add("guardian", vec(placement.guardianSpawn()));
        meta.add("treasury_ui_anchor", vec(placement.treasuryUiAnchor()));
        meta.add("d1_placement_origin", vec(placement.d1PlacementOrigin()));
        out.add("metadata", meta);
        ctx.assertTrue(instance.secondary().isEmpty(), "no SECONDARY");
        ctx.assertTrue(instance.primary().equals(placement.primaryLeft()) && instance.spawn().equals(placement.guardianSpawn()), "metadata");
        RoyalChestRuntime.discardInstance(level, instance.id());

        String dir = System.getProperty("mm.st01.assembly.out");
        if (dir != null) {
            Path file = Path.of(dir, "case-" + orientation.name().toLowerCase() + "-" + route.name().toLowerCase() + ".json.gz");
            try {
                Files.createDirectories(file.getParent());
                try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(file))) {
                    os.write(new Gson().toJson(out).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            } catch (IOException e) { throw new RuntimeException(e); }
        }
        System.out.println("ST01_ASSEMBLY_CASE_PLACED " + orientation + "/" + route + " ground=" + ground.toShortString());
        ctx.succeed();
    }

    private static JsonArray vec(BlockPos p) {
        JsonArray a = new JsonArray(); a.add(p.getX()); a.add(p.getY()); a.add(p.getZ()); return a;
    }

    private static JsonObject state(BlockState state) {
        JsonObject s = new JsonObject();
        s.addProperty("id", String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        JsonObject props = new JsonObject();
        for (Property<?> property : state.getProperties()) props.addProperty(property.getName(), value(state, property));
        if (!props.isEmpty()) s.add("properties", props);
        return s;
    }

    private static <T extends Comparable<T>> String value(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    @GameTest(maxTicks = 400) public void northEast(GameTestHelper ctx) { run(ctx, Orientation.NORTH, AccessRoute.EAST); }
    @GameTest(maxTicks = 400) public void northWest(GameTestHelper ctx) { run(ctx, Orientation.NORTH, AccessRoute.WEST); }
    @GameTest(maxTicks = 400) public void eastEast(GameTestHelper ctx) { run(ctx, Orientation.EAST, AccessRoute.EAST); }
    @GameTest(maxTicks = 400) public void eastWest(GameTestHelper ctx) { run(ctx, Orientation.EAST, AccessRoute.WEST); }
    @GameTest(maxTicks = 400) public void southEast(GameTestHelper ctx) { run(ctx, Orientation.SOUTH, AccessRoute.EAST); }
    @GameTest(maxTicks = 400) public void southWest(GameTestHelper ctx) { run(ctx, Orientation.SOUTH, AccessRoute.WEST); }
    @GameTest(maxTicks = 400) public void westEast(GameTestHelper ctx) { run(ctx, Orientation.WEST, AccessRoute.EAST); }
    @GameTest(maxTicks = 400) public void westWest(GameTestHelper ctx) { run(ctx, Orientation.WEST, AccessRoute.WEST); }
}
