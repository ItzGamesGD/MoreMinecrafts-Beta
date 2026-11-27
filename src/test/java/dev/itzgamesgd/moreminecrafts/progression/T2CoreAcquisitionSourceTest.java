package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Source-level guard for T2 §18.4 vault chances and removal of the superseded Bastion/Monument core grants. */
class T2CoreAcquisitionSourceTest {
    private static String source() throws Exception {
        return Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/progression/T2CoreAcquisition.java"));
    }

    @Test void vaultAndAncientCityChancesMatchSection18() throws Exception {
        String s = source();
        assertTrue(s.contains("ANCIENT_HEART, 0.03F"));
        assertTrue(s.contains("EYE_OF_TYPHOON, 0.05F"));
        assertTrue(s.contains("HEART_OF_THUNDER, 0.05F"));
    }

    @Test void supersededBastionAndMonumentCoreGrantsAreGone() throws Exception {
        String s = source();
        assertFalse(s.contains("bastion_treasure"));
        assertFalse(s.contains("BURNING_HEART"));
        assertFalse(s.contains("HEART_OF_CURRENTS"));
        assertFalse(s.contains("new ItemEntity"));
    }
}
