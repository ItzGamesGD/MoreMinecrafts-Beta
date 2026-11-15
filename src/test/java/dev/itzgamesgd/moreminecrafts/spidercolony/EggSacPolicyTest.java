package dev.itzgamesgd.moreminecrafts.spidercolony;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EggSacPolicyTest {
    @Test void naturalReactiveRateHas35OutOf100PossibleRolls() {
        int count=0;
        for(int roll=0;roll<100;roll++) if(EggSacPolicy.naturalReactive(roll)) count++;
        assertEquals(35,count);
    }
    @Test void onlyNaturallyReactiveAndNonSilkBreaksHatch() {
        assertTrue(EggSacPolicy.hatches(true,true,false));
        assertFalse(EggSacPolicy.hatches(false,true,false));
        assertFalse(EggSacPolicy.hatches(true,false,false));
        assertFalse(EggSacPolicy.hatches(true,true,true));
    }
}
