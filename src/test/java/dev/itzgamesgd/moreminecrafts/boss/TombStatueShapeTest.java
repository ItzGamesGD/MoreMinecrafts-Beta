package dev.itzgamesgd.moreminecrafts.boss;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** The statue's collision height follows the display's rise and sink; the full box is the approved obelisk. */
class TombStatueShapeTest {
    private static final int LIFETIME = TombGuardianTuning.STATUE_VISIBLE_TICKS;

    @Test void fullBoxMatchesTheApprovedObeliskSize() {
        AABB box = TombStatueShape.fullBox(new Vec3(10.0D, 4.0D, -3.0D));
        assertEquals(0.75D, box.getXsize(), 1.0E-9D, "6/16 model width scaled by height/2");
        assertEquals(0.75D, box.getZsize(), 1.0E-9D);
        assertEquals(TombGuardianTuning.STATUE_HEIGHT, box.getYsize(), 1.0E-9D);
        assertEquals(4.0D, box.minY, 1.0E-9D, "base on the floor");
    }

    @Test void heightRisesMonotonicallyFromZeroToFull() {
        assertEquals(0.0D, TombStatueShape.height(0, LIFETIME));
        assertEquals(0.0D, TombStatueShape.height(1, LIFETIME), "still buried the tick it spawns");
        double previous = 0.0D;
        for (int age = 1; age <= 1 + TombStatueShape.RISE_TICKS; age++) {
            double height = TombStatueShape.height(age, LIFETIME);
            assertTrue(height >= previous, "rise never shrinks");
            previous = height;
        }
        assertEquals(TombGuardianTuning.STATUE_HEIGHT, previous, 1.0E-9D);
        double mid = TombStatueShape.height(3, LIFETIME);
        assertTrue(mid > 0.0D && mid < TombGuardianTuning.STATUE_HEIGHT, "partial height mid-rise, got " + mid);
    }

    @Test void heightStaysFullThenSinksToZeroAtRemoval() {
        int sinkStart = LIFETIME - TombStatueShape.RISE_TICKS;
        for (int age = 2 + TombStatueShape.RISE_TICKS; age < sinkStart; age++)
            assertEquals(TombGuardianTuning.STATUE_HEIGHT, TombStatueShape.height(age, LIFETIME), 1.0E-9D);
        double previous = TombGuardianTuning.STATUE_HEIGHT;
        for (int age = sinkStart; age <= LIFETIME; age++) {
            double height = TombStatueShape.height(age, LIFETIME);
            assertTrue(height <= previous, "sink never grows");
            previous = height;
        }
        assertEquals(0.0D, previous, "no collision height left when the statue is removed");
    }
}
