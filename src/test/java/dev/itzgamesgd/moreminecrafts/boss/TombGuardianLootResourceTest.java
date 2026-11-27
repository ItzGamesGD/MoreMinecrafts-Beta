package dev.itzgamesgd.moreminecrafts.boss;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** 2026-11-14 spec P1-BUG-04: Tomb Guardian kill drop is Ancient Heart 100% x1 only (no extra reward pool). */
class TombGuardianLootResourceTest {
    @Test void killDropIsAncientHeartFiftyPercentTimesOne() throws Exception {
        JsonObject table = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/data/moreminecrafts/loot_table/entities/tomb_guardian.json"))).getAsJsonObject();
        assertEquals("minecraft:entity", table.get("type").getAsString());
        JsonArray pools = table.getAsJsonArray("pools");
        assertEquals(1, pools.size(), "a single pool: no additional rare-mineral reward pool");
        JsonObject pool = pools.get(0).getAsJsonObject();
        assertEquals(1, pool.get("rolls").getAsInt());
        assertFalse(pool.has("condition"), "the core drop is 100%: no chance condition");
        JsonArray entries = pool.getAsJsonArray("entries");
        assertEquals(1, entries.size());
        JsonObject entry = entries.get(0).getAsJsonObject();
        assertEquals("moreminecrafts:ancient_heart", entry.get("name").getAsString());
        JsonObject count = entry.getAsJsonArray("modifier").get(0).getAsJsonObject();
        assertEquals("minecraft:set_count", count.get("type").getAsString());
        assertEquals(1, count.get("count").getAsInt());
    }

    @Test void ancientCityThreePercentRouteIsUnchanged() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/progression/T2CoreAcquisition.java"));
        assertTrue(source.contains("if (key.equals(ANCIENT_CITY)) addRareCore(table, ModItems.ANCIENT_HEART, 0.03F);"));
    }
}
