package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

/** The ported P6/P11 entrance geometry must equal the approved G0-R1 blueprint and follow the assembly transform. */
class EntranceLayoutTest {
    private static final Path BLUEPRINT = Path.of("docs/design/st01-g0-r1/generated/blueprint.json");

    private static Set<BlockPos> blueprintCover(AccessRoute route) throws Exception {
        JsonObject bp = JsonParser.parseString(Files.readString(BLUEPRINT)).getAsJsonObject();
        Set<BlockPos> cells = new HashSet<>();
        for (JsonElement e : bp.getAsJsonObject("variants").getAsJsonObject(route.name()).getAsJsonArray("cover_cells")) {
            JsonArray a = e.getAsJsonArray();
            cells.add(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()));
        }
        return cells;
    }

    @Test
    void authoredCoverIsTheApprovedSharedTwelveCellCoverOfBothRoutes() throws Exception {
        Set<BlockPos> authored = new HashSet<>(EntranceLayout.authoredCover());
        assertEquals(12, authored.size());
        for (AccessRoute route : AccessRoute.values()) assertEquals(blueprintCover(route), authored, "cover of " + route);
    }

    @Test
    void authoredValuesEqualTheP11AnchorRelativeApprovals() {
        BlockPos anchor = St01RoyalChestPlacement.REFERENCE_ANCHOR;
        assertEquals(new BlockPos(-21, -20, 33), anchor);
        assertEquals(new BlockPos(-12, -22, 36), EntranceLayout.AUTHORED_STOP, "O-4 C03 = anchor + (9,-2,3)");
        assertEquals(new BlockPos(-21, -22, 27), EntranceLayout.AUTHORED_GOLEM_SPAWN, "O-6 BOSS_S = anchor + (0,-2,-6)");
        for (BlockPos c : EntranceLayout.authoredCover()) {
            BlockPos d = c.subtract(anchor);
            assertTrue(d.getX() >= 12 && d.getX() <= 15 && d.getY() == -3 && d.getZ() >= 0 && d.getZ() <= 2, "P6 cut dx12..15 dy-3 dz0..2: " + d);
        }
        // The stop point is outside the cover so the golem never stands on the cells it opens.
        assertFalse(EntranceLayout.authoredCover().contains(EntranceLayout.AUTHORED_STOP.below()));
    }

    @Test
    void worldPositionsUseTheAssemblyTransformAndStayInsideTheCompletionBox() {
        BlockPos ground = new BlockPos(1234, 70, -5678);
        for (Orientation o : Orientation.values()) {
            BoundingBox box = St01PyramidAssembly.completionBox(ground, o);
            List<BlockPos> cover = EntranceLayout.coverCells(ground, o);
            assertEquals(12, new HashSet<>(cover).size());
            for (int i = 0; i < cover.size(); i++) {
                assertEquals(St01PyramidAssembly.worldPos(EntranceLayout.authoredCover().get(i), ground, o), cover.get(i));
                assertTrue(box.isInside(cover.get(i)), "cover inside completion box " + o);
                assertEquals(ground.getY() - 23, cover.get(i).getY());
            }
            assertEquals(St01PyramidAssembly.describe(ground, o, AccessRoute.EAST).primaryLeft(), EntranceLayout.tombAnchor(ground, o),
                    "entrance key is the royal tomb anchor " + o);
            assertEquals(St01PyramidAssembly.describe(ground, o, AccessRoute.WEST).primaryLeft(), EntranceLayout.tombAnchor(ground, o));
            assertTrue(box.isInside(EntranceLayout.stopPoint(ground, o)) && box.isInside(EntranceLayout.golemSpawn(ground, o)));
            // Rigid transform: distances between anchor, stop and spawn are rotation invariant.
            assertEquals(EntranceLayout.AUTHORED_STOP.distManhattan(EntranceLayout.AUTHORED_ANCHOR),
                    EntranceLayout.stopPoint(ground, o).distManhattan(EntranceLayout.tombAnchor(ground, o)));
        }
    }

    @Test
    void dataRegistersOncePerTombAndNeverRegresses() {
        SecretEntranceData data = new SecretEntranceData();
        BlockPos ground = new BlockPos(100, 64, 200);
        assertTrue(data.register(ground, Orientation.WEST));
        assertFalse(data.register(ground, Orientation.WEST), "idempotent");
        long key = EntranceLayout.tombAnchor(ground, Orientation.WEST).asLong();
        SecretEntranceData.Instance inst = data.get(key);
        assertNotNull(inst);
        assertEquals(EntranceState.SEALED, inst.state());
        assertTrue(data.put(inst.withSaved(new EntranceCore.Saved(EntranceState.FALLBACK_OPEN, 0, true))));
        assertFalse(data.put(inst.withSaved(new EntranceCore.Saved(EntranceState.GOLEM_GUIDING, 5, false))), "OPEN is permanent");
        assertFalse(data.put(inst.withSaved(new EntranceCore.Saved(EntranceState.SEALED, 0, false))), "never back to SEALED");

        var encoded = SecretEntranceData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        SecretEntranceData restored = SecretEntranceData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(data.get(key), restored.get(key));
        assertEquals(EntranceState.FALLBACK_OPEN, restored.get(key).state());
        assertTrue(restored.discard(key) && restored.isEmpty());
    }
}
