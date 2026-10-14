package dev.itzgamesgd.moreminecrafts.fishing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaterBodyValidatorTest {
    @Test void tinyOrShallowWaterFails() {
        assertFalse(WaterBodyValidator.isEligible((x,d,z) -> Math.abs(x)<=2 && Math.abs(z)<=2 && d<2));
        assertFalse(WaterBodyValidator.isEligible((x,d,z) -> Math.abs(x)<=16 && Math.abs(z)<=16 && d<3));
    }

    @Test void largeDeepIrregularWaterPassesDeterministically() {
        WaterBodyValidator.WaterSample lake = (x,d,z) -> x*x + z*z <= 16*16 && d < (x*x+z*z<12*12 ? 10 : 4);
        assertTrue(WaterBodyValidator.isEligible(lake));
        assertEquals(WaterBodyValidator.isEligible(lake), WaterBodyValidator.isEligible(lake));
    }
}
