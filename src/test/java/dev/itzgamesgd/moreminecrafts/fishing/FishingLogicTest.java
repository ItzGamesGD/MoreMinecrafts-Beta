package dev.itzgamesgd.moreminecrafts.fishing;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class FishingLogicTest {
 @Test void reelOnlyControlsDepthAccess(){assertEquals(1,FishingLogic.accessibleDepth(5,1));assertEquals(4,FishingLogic.accessibleDepth(5,4));assertFalse(FishingLogic.eligible(5,1,5).contains(FishSpecies.STURGEON));assertEquals(FishingLogic.weight(FishSpecies.PERCH,3,1,0),FishingLogic.weight(FishSpecies.PERCH,3,1,0));}
 @Test void minnowIsHardGateNotWeightModifier(){assertFalse(FishingLogic.eligible(5,5,3).contains(FishSpecies.STURGEON));assertTrue(FishingLogic.eligible(5,5,4).contains(FishSpecies.STURGEON));assertEquals(FishingLogic.weight(FishSpecies.PERCH,2,2,0),FishingLogic.weight(FishSpecies.PERCH,2,2,0));}
 @Test void crankbaitBiasesOnlyValidPool(){List<FishSpecies> pool=FishingLogic.eligible(2,2,1);assertTrue(FishingLogic.weight(FishSpecies.PERCH,5,5,0)>FishingLogic.weight(FishSpecies.PERCH,5,1,0));assertFalse(pool.contains(FishSpecies.STURGEON));}
 @Test void fishLengthsAndQualityAreBoundedAndDeterministic(){for(FishSpecies s:FishSpecies.values()){int a=s.lengthFromRoll(Integer.MIN_VALUE),b=s.lengthFromRoll(Integer.MAX_VALUE);assertTrue(a>=s.minLengthCm()&&a<=s.maxLengthCm());assertTrue(b>=s.minLengthCm()&&b<=s.maxLengthCm());assertEquals(s.quality(3,a),s.quality(3,a));}}
 @Test void exactlyFiveGrades(){for(int i=1;i<=5;i++)assertEquals(i,FishingGrade.requireValid(i));assertThrows(IllegalArgumentException.class,()->FishingGrade.requireValid(0));assertThrows(IllegalArgumentException.class,()->FishingGrade.requireValid(6));}
}
