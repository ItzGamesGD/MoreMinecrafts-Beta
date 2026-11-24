package dev.itzgamesgd.moreminecrafts.heaven;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** P3.7: the forest comparison variants and the policy-review data stay out of production worldgen. */
class HeavenP37ResourceTest {
    @Test
    void forestComparisonTemplatesAreIsolatedTestResources() throws Exception {
        Path cmp = Path.of("src/gametest/resources/data/moreminecrafts-gametest/structure/heaven_forest_cmp");
        for (String v : List.of("a_v5", "a_forest", "b_v5", "b_forest"))
            for (String n : List.of("large_a", "plain_medium_d", "ruins", "temple"))
                assertTrue(Files.exists(cmp.resolve(v + "/" + n + ".nbt")), v + "/" + n);
        try (Stream<Path> files = Files.walk(Path.of("src/main/resources"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList())
                assertFalse(Files.readString(f).contains("heaven_forest_cmp"), "production file references the comparison templates: " + f);
        }
        assertFalse(Files.exists(Path.of("src/main/resources/data/moreminecrafts/structure/heaven_p3/forest")), "no production forest templates before owner approval");
    }

    @Test
    void worldgenStructureIdsAndSetsAreUnchanged() throws Exception {
        String set = Files.readString(Path.of("src/main/resources/data/moreminecrafts/worldgen/structure_set/sky_islands_general.json"));
        assertEquals(5, set.split("\"structure\":").length - 1);
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/data/moreminecrafts/worldgen/structure"))) {
            assertEquals(9, files.filter(p -> p.getFileName().toString().startsWith("sky_island_")).count() + 1, "eight sky island structure IDs (+1 pyramid offset)");
        }
    }
}
