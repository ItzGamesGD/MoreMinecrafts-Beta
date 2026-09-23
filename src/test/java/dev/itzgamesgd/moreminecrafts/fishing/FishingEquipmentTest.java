package dev.itzgamesgd.moreminecrafts.fishing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FishingEquipmentTest {
 @Test void threeFamiliesAndFiveGrades(){assertEquals(3,FishingEquipmentFamily.values().length);for(var family:FishingEquipmentFamily.values())for(int grade=1;grade<=5;grade++)assertEquals(grade,FishingGrade.requireValid(grade));}
}
