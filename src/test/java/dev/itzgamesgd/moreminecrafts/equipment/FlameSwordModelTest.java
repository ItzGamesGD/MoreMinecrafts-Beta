package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlameSwordModelTest {
    private JsonObject model() {
        var stream = getClass().getClassLoader()
                .getResourceAsStream("assets/moreminecrafts/models/item/flame_sword.json");
        assertNotNull(stream);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void cuboidKeepsItsProductionTextureAndDisplayTransformsWithValidFaceUvs() {
        JsonObject model = model();
        JsonObject textures = model.getAsJsonObject("textures");
        assertEquals("moreminecrafts:item/flame_sword", textures.get("layer0").getAsString());
        assertNotNull(getClass().getClassLoader().getResource("assets/moreminecrafts/textures/item/flame_sword.png"));
        assertEquals("minecraft:item/handheld", model.get("parent").getAsString());
        assertEquals(1, model.getAsJsonArray("elements").size());
        JsonObject element = model.getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals(List.of(0, 0, 7), ints(element.getAsJsonArray("from")));
        assertEquals(List.of(16, 16, 9), ints(element.getAsJsonArray("to")));

        JsonObject faces = element.getAsJsonObject("faces");
        JsonArray front = faces.getAsJsonObject("north").getAsJsonArray("uv");
        JsonArray back = faces.getAsJsonObject("south").getAsJsonArray("uv");
        assertEquals(front, back, "back face must not mirror the approved sword artwork");
        for (String face : List.of("north", "south", "east", "west", "up", "down")) {
            JsonArray uv = faces.getAsJsonObject(face).getAsJsonArray("uv");
            assertEquals(4, uv.size(), face);
            double u0 = uv.get(0).getAsDouble(), v0 = uv.get(1).getAsDouble();
            double u1 = uv.get(2).getAsDouble(), v1 = uv.get(3).getAsDouble();
            assertTrue(u0 >= 0 && v0 >= 0 && u1 <= 16 && v1 <= 16, face);
            assertTrue(u0 < u1 && v0 < v1, "UV rectangle must have area: " + face);
            assertEquals("#layer0", faces.getAsJsonObject(face).get("texture").getAsString());
        }

        JsonObject display = model.getAsJsonObject("display");
        assertArrayEquals(new double[]{0, -90, 25}, doubles(display.getAsJsonObject("firstperson_righthand")
                .getAsJsonArray("rotation")));
        assertArrayEquals(new double[]{0, 90, -25}, doubles(display.getAsJsonObject("firstperson_lefthand")
                .getAsJsonArray("rotation")));
        assertArrayEquals(new double[]{0, -90, 55}, doubles(display.getAsJsonObject("thirdperson_righthand")
                .getAsJsonArray("rotation")));
        assertArrayEquals(new double[]{0, 90, -55}, doubles(display.getAsJsonObject("thirdperson_lefthand")
                .getAsJsonArray("rotation")));
    }

    private static List<Integer> ints(JsonArray values) {
        return java.util.stream.IntStream.range(0, values.size())
                .mapToObj(index -> values.get(index).getAsInt()).toList();
    }

    private static double[] doubles(JsonArray values) {
        return java.util.stream.IntStream.range(0, values.size()).mapToDouble(index -> values.get(index).getAsDouble()).toArray();
    }
}
