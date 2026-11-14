package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The committed worldgen JSON must equal the locked owner-approved constants, with no second structure set for ST-01. */
class St01WorldgenResourceTest {
    private static JsonObject json(String path) throws IOException {
        try (InputStream in = St01WorldgenResourceTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void structureSetMatchesLockedPlacement() throws IOException {
        JsonObject set = json("data/moreminecrafts/worldgen/structure_set/st01_pyramid.json");
        JsonObject placement = set.getAsJsonObject("placement");
        assertEquals("minecraft:random_spread", placement.get("type").getAsString());
        assertEquals(St01PlacementConfig.SPACING_CHUNKS, placement.get("spacing").getAsInt());
        assertEquals(St01PlacementConfig.SEPARATION_CHUNKS, placement.get("separation").getAsInt());
        assertEquals(St01PlacementConfig.SPREAD_TYPE, placement.get("spread_type").getAsString());
        assertEquals(St01PlacementConfig.SALT, placement.get("salt").getAsInt());
        assertFalse(placement.has("frequency"), "no frequency reduction: candidate selection must stay exactly spacing/separation/triangular/salt");
        assertFalse(placement.has("exclusion_zone"));
        assertEquals(1, set.getAsJsonArray("structures").size());
        assertEquals("moreminecrafts:st01_pyramid", set.getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString());
    }

    @Test void structureIsDesertOnlyCustomTypeWithoutVanillaTerrainAdaptation() throws IOException {
        JsonObject s = json("data/moreminecrafts/worldgen/structure/st01_pyramid.json");
        assertEquals("moreminecrafts:st01_pyramid", s.get("type").getAsString());
        assertEquals(1, s.getAsJsonArray("biomes").size());
        assertEquals("minecraft:desert", s.getAsJsonArray("biomes").get(0).getAsString());
        assertEquals("none", s.get("terrain_adaptation").getAsString(), "MODERATE correction is applied by St01FootprintCorrectionPiece");
        assertEquals("surface_structures", s.get("step").getAsString());
    }

    @Test void saltIsNotReusedByAnyOtherStructureSet() throws IOException {
        for (String other : new String[]{"sky_islands_general", "sky_islands_mountain"}) {
            JsonObject set = json("data/moreminecrafts/worldgen/structure_set/" + other + ".json");
            assertNotEquals(St01PlacementConfig.SALT, set.getAsJsonObject("placement").get("salt").getAsInt());
        }
    }

    @Test void fixtureTemplateIsThe39x20x39RoyalHall() throws IOException {
        try (InputStream in = St01WorldgenResourceTest.class.getClassLoader().getResourceAsStream("data/moreminecrafts/structure/pyramid/royal_hall.nbt")) {
            assertNotNull(in);
            var root = net.minecraft.nbt.NbtIo.readCompressed(in, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            var size = root.getListOrEmpty("size");
            assertEquals(39, size.getIntOr(0, 0));
            assertEquals(20, size.getIntOr(1, 0));
            assertEquals(39, size.getIntOr(2, 0));
        }
    }
}
