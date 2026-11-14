package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PyramidMobResourcesTest {
    @Test
    void eachVariantHasItsOwnTexture() {
        for (String id : new String[]{"buried_mummy", "crypt_archer", "funeral_servant", "gilded_mummy"}) {
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/moreminecrafts/textures/entity/pyramid", id + ".png")), id);
        }
    }
}
