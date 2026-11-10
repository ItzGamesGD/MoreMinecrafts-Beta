package dev.itzgamesgd.moreminecrafts.pyramid;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01AssemblyPiece;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01FootprintCorrectionPiece;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * P8-C2-B: the natural-worldgen pieces of all eight orientation x route cases, placed chunk by chunk exactly as vanilla
 * {@code StructureStart.placeInChunk} does (pieces in list order, each clipped to the chunk box), in NORMAL, REVERSE and
 * seeded RANDOM chunk order. The three placements of each case must be identical cell for cell, the NORMAL one is dumped
 * for {@code tools/st01-p8c2a/verify_placement.py}, and the D1 metadata must register with the unchanged P8-B2 API.
 * Gated by {@code -Dmm.st01.c2b.pieces=true}; dumps go to {@code -Dmm.st01.c2b.pieces.out}.
 */
public final class St01NaturalPieceGameTests {
    private static final int GROUND_Y = 150;

    private static void run(GameTestHelper ctx, Orientation orientation, AccessRoute route) {
        if (!Boolean.getBoolean("mm.st01.c2b.pieces")) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        int index = orientation.ordinal() * 2 + route.ordinal();
        String[] orders = {"NORMAL", "REVERSE", "RANDOM"};
        String firstHash = null;
        for (int o = 0; o < orders.length; o++) {
            // chunk-aligned ground at a chunk centre, like a natural start
            BlockPos ground = new BlockPos(16 * (2000 + 12 * (index * 3 + o)) + 8, GROUND_Y, 16 * 2000 + 8);
            BoundingBox box = St01PyramidAssembly.completionBox(ground, orientation);
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY() - 1, box.minZ(), box.maxX(), box.maxY() + 1, box.maxZ()))
                if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            // natural terrain stand-in inside the footprint: sand up to the walking layer and a 6-block hill to be cut
            for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int top = (x - box.minX()) < 20 ? GROUND_Y + 6 : GROUND_Y - 1;
                for (int y = GROUND_Y - 33; y <= top; y++) level.setBlock(new BlockPos(x, y, z), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            List<StructurePiece> pieces = pieces(level.getStructureTemplateManager(), ground, orientation, route);
            List<ChunkPos> chunks = new ArrayList<>();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) chunks.add(new ChunkPos(cx, cz));
            if (o == 1) Collections.reverse(chunks);
            if (o == 2) Collections.shuffle(chunks, new Random(index));
            BlockPos reference = pieces.getFirst().getBoundingBox().getCenter();
            for (ChunkPos c : chunks) {
                BoundingBox chunkBox = new BoundingBox(c.getMinBlockX(), level.getMinY(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxY(), c.getMaxBlockZ());
                for (StructurePiece piece : pieces) {
                    if (piece.getBoundingBox().intersects(chunkBox))
                        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunkBox, c, reference);
                }
            }
            String hash = hash(level, box);
            if (firstHash == null) firstHash = hash;
            ctx.assertTrue(firstHash.equals(hash), orientation + "/" + route + " " + orders[o] + " chunk order changed structure blocks");
            var placement = St01PyramidAssembly.describe(ground, orientation, route);
            var registered = St01RoyalChestPlacement.registerPlaced(level, placement.d1PlacementOrigin(), placement.d1Settings());
            ctx.assertTrue(registered.isPresent(), "registerPlaced " + orientation + "/" + route + " " + orders[o]);
            if (o == 0) dump(level, box, ground, orientation, route, placement, registered.orElseThrow());
            RoyalChestRuntime.discardInstance(level, registered.orElseThrow().id());
        }
        System.out.println("ST01_C2B_PIECES " + orientation + "/" + route + " identical across NORMAL/REVERSE/RANDOM hash=" + firstHash);
        ctx.succeed();
    }

    private static List<StructurePiece> pieces(StructureTemplateManager templates, BlockPos ground, Orientation o, AccessRoute r) {
        List<StructurePiece> out = new ArrayList<>();
        out.add(new St01FootprintCorrectionPiece(ground.getX(), ground.getY(), ground.getZ()));
        for (int i = 0; i < 3; i++) out.add(new St01AssemblyPiece(templates, ground, o, r, i));
        return out;
    }

    private static String hash(ServerLevel level, BoundingBox box) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++)
                md.update(((x - box.minX()) + "," + (y - box.minY()) + "," + (z - box.minZ()) + "=" + level.getBlockState(p.set(x, y, z)) + ";").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static void dump(ServerLevel level, BoundingBox box, BlockPos ground, Orientation orientation, AccessRoute route,
                             St01PyramidAssembly.Placement placement, RoyalChestData.Instance instance) {
        JsonObject out = new JsonObject();
        out.addProperty("orientation", orientation.name());
        out.addProperty("access", route.name());
        out.addProperty("rotation", orientation.rotation.name());
        out.addProperty("source", "St01AssemblyPiece chunk-clipped postProcess");
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
            JsonObject s = state(level.getBlockState(p));
            Integer id = lookup.get(s.toString());
            if (id == null) { id = palette.size(); lookup.put(s.toString(), id); palette.add(s); }
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
        reg.addProperty("idempotent", St01RoyalChestPlacement.registerPlaced(level, placement.d1PlacementOrigin(), placement.d1Settings()).orElseThrow().equals(instance));
        out.add("registration", reg);
        JsonObject meta = new JsonObject();
        meta.add("primary_left", vec(placement.primaryLeft()));
        meta.add("primary_right", vec(placement.primaryRight()));
        meta.add("guardian", vec(placement.guardianSpawn()));
        meta.add("treasury_ui_anchor", vec(placement.treasuryUiAnchor()));
        meta.add("d1_placement_origin", vec(placement.d1PlacementOrigin()));
        out.add("metadata", meta);
        String dir = System.getProperty("mm.st01.c2b.pieces.out");
        if (dir == null) return;
        Path file = Path.of(dir, "case-" + orientation.name().toLowerCase() + "-" + route.name().toLowerCase() + ".json.gz");
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(file))) {
                os.write(new Gson().toJson(out).getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static JsonArray vec(BlockPos p) { JsonArray a = new JsonArray(); a.add(p.getX()); a.add(p.getY()); a.add(p.getZ()); return a; }

    private static JsonObject state(BlockState state) {
        JsonObject s = new JsonObject();
        s.addProperty("id", String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        JsonObject props = new JsonObject();
        for (Property<?> property : state.getProperties()) props.addProperty(property.getName(), value(state, property));
        if (!props.isEmpty()) s.add("properties", props);
        return s;
    }

    private static <T extends Comparable<T>> String value(BlockState state, Property<T> property) { return property.getName(state.getValue(property)); }

    @GameTest(maxTicks = 1200) public void northEast(GameTestHelper ctx) { run(ctx, Orientation.NORTH, AccessRoute.EAST); }
    @GameTest(maxTicks = 1200) public void northWest(GameTestHelper ctx) { run(ctx, Orientation.NORTH, AccessRoute.WEST); }
    @GameTest(maxTicks = 1200) public void eastEast(GameTestHelper ctx) { run(ctx, Orientation.EAST, AccessRoute.EAST); }
    @GameTest(maxTicks = 1200) public void eastWest(GameTestHelper ctx) { run(ctx, Orientation.EAST, AccessRoute.WEST); }
    @GameTest(maxTicks = 1200) public void southEast(GameTestHelper ctx) { run(ctx, Orientation.SOUTH, AccessRoute.EAST); }
    @GameTest(maxTicks = 1200) public void southWest(GameTestHelper ctx) { run(ctx, Orientation.SOUTH, AccessRoute.WEST); }
    @GameTest(maxTicks = 1200) public void westEast(GameTestHelper ctx) { run(ctx, Orientation.WEST, AccessRoute.EAST); }
    @GameTest(maxTicks = 1200) public void westWest(GameTestHelper ctx) { run(ctx, Orientation.WEST, AccessRoute.WEST); }
}
