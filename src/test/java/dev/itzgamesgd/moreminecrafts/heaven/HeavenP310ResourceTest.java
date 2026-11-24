package dev.itzgamesgd.moreminecrafts.heaven;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** P3.10: the gametest data packs used for real-worldgen checks are byte-identical copies of the (inactive) pending pools; nothing live changed. */
class HeavenP310ResourceTest {
    private static final Path PENDING = Path.of("src/main/resources/heaven_worldgen_pending/data/moreminecrafts/worldgen/template_pool/sky_islands");
    private static final Path LIVE = Path.of("src/main/resources/data/moreminecrafts/worldgen/template_pool/sky_islands");
    private static final Path PACKS = Path.of("src/gametest/resources/resourcepacks");

    @Test
    void gametestPacksAreByteIdenticalToPendingPools() throws IOException {
        for (String theme : List.of("meadow", "forest")) for (String pool : List.of("start", "satellite", "tower_top")) {
            Path pack = PACKS.resolve("heaven_pending_" + theme + "/data/moreminecrafts/worldgen/template_pool/sky_islands/" + theme + "/" + pool + ".json");
            assertArrayEquals(Files.readAllBytes(PENDING.resolve(theme + "/" + pool + ".json")), Files.readAllBytes(pack), pack.toString());
        }
    }

    @Test
    void livePoolsAreSwitchedExactlyForTheApprovedThemes() throws IOException {
        for (String theme : List.of("meadow", "forest")) for (String pool : List.of("start", "satellite", "tower_top"))
            assertEquals(HeavenP38ResourceTest.TRANSITIONED.contains(theme), Files.readString(LIVE.resolve(theme + "/" + pool + ".json")).contains("sky_islands/heaven/"), theme + "/" + pool + " switched exactly when approved");
    }
}
