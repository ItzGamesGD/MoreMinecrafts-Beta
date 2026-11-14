package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PyramidMobResourcesTest {
    @Test
    void twoMummyVariantsHaveDifferentTexturesInBothSizes() throws Exception {
        Path root = Path.of("src/main/resources/assets/moreminecrafts/textures/entity/pyramid");
        for (String suffix : new String[]{"", "_baby"}) {
            assertFalse(Arrays.equals(Files.readAllBytes(root.resolve("buried_mummy" + suffix + ".png")),
                    Files.readAllBytes(root.resolve("gilded_mummy" + suffix + ".png"))));
        }
    }

    @Test
    void eachVariantHasItsOwnTexture() {
        for (String id : new String[]{"buried_mummy", "crypt_archer", "funeral_servant", "gilded_mummy"}) {
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/moreminecrafts/textures/entity/pyramid", id + ".png")), id);
        }
    }
}
