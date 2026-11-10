package dev.itzgamesgd.moreminecrafts.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;

/** The Java placement contract must equal the exported manifest, and the committed assets must equal its hashes. */
class St01PyramidAssemblyTest {
    private static final Path MANIFEST = Path.of("docs/design/st01-p8c2a/generated/assembly-manifest.json");

    private static JsonObject manifest() throws Exception {
        return JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonObject();
    }

    private static BlockPos pos(JsonArray a) { return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()); }

    @Test
    void piecesOrderCornersAndHashesMatchManifest() throws Exception {
        JsonObject pieces = manifest().getAsJsonObject("pieces");
        for (AccessRoute route : AccessRoute.values()) {
            JsonArray expected = pieces.getAsJsonArray(route.name());
            var actual = St01PyramidAssembly.pieces(route);
            assertEquals(expected.size(), actual.size());
            for (int i = 0; i < actual.size(); i++) {
                JsonObject e = expected.get(i).getAsJsonObject();
                assertEquals(e.get("piece").getAsString(), actual.get(i).name());
                assertEquals(e.get("resource").getAsString(), actual.get(i).template().toString());
                assertEquals(pos(e.getAsJsonArray("authored_min_corner")), actual.get(i).authoredMinCorner());
                assertEquals(pos(e.getAsJsonArray("rotation_pivot")), BlockPos.ZERO.subtract(actual.get(i).authoredMinCorner()));
                byte[] bytes = Files.readAllBytes(Path.of(e.get("path").getAsString()));
                assertEquals(e.get("sha256").getAsString(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            }
        }
    }

    @Test
    void d1RemainsOriginalBytes() throws Exception {
        byte[] d1 = Files.readAllBytes(Path.of("src/main/resources/data/moreminecrafts/structure/pyramid/royal_hall.nbt"));
        assertEquals("7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(d1)));
    }

    @Test
    void runtimeMetadataAndOrientationMappingMatchManifest() throws Exception {
        JsonObject m = manifest();
        JsonObject rm = m.getAsJsonObject("runtime_metadata");
        assertEquals(pos(rm.getAsJsonArray("authored_anchor")), St01RoyalChestPlacement.REFERENCE_ANCHOR);
        assertEquals(pos(rm.getAsJsonArray("authored_primary_right")), St01RoyalChestPlacement.REFERENCE_RIGHT);
        assertEquals(pos(rm.getAsJsonArray("authored_guardian")), St01RoyalChestPlacement.REFERENCE_SPAWN);
        assertEquals(pos(rm.getAsJsonArray("d1_reference_origin")), St01RoyalChestPlacement.REFERENCE_ORIGIN);
        JsonArray b = rm.getAsJsonArray("completion_bounds");
        assertEquals(new BlockPos(b.get(0).getAsInt(), b.get(2).getAsInt(), b.get(4).getAsInt()), St01PyramidAssembly.COMPLETION_MIN);
        assertEquals(new BlockPos(b.get(1).getAsInt(), b.get(3).getAsInt(), b.get(5).getAsInt()), St01PyramidAssembly.COMPLETION_MAX);
        assertTrue(rm.get("secondary").isJsonNull());
        JsonObject mapping = m.getAsJsonObject("coordinate_contract").getAsJsonObject("orientation_to_rotation");
        for (Orientation o : Orientation.values()) assertEquals(Rotation.valueOf(mapping.get(o.name()).getAsString()), o.rotation);
    }

    @Test
    void authoredEntranceFacesTheNamedOrientation() {
        BlockPos ground = new BlockPos(1000, 64, -2000);
        for (Orientation o : Orientation.values()) {
            BlockPos d = St01PyramidAssembly.worldPos(new BlockPos(0, 0, 1), ground, o).subtract(ground);
            String facing = d.getZ() == 1 ? "SOUTH" : d.getZ() == -1 ? "NORTH" : d.getX() == 1 ? "EAST" : "WEST";
            assertEquals(o.name(), facing);
            var box = St01PyramidAssembly.completionBox(ground, o);
            assertEquals(81, box.getXSpan()); assertEquals(72, box.getYSpan()); assertEquals(81, box.getZSpan());
            assertEquals(ground.getY() - 32, box.minY());
        }
    }
}
