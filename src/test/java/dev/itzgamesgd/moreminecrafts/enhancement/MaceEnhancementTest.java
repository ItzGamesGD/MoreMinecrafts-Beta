package dev.itzgamesgd.moreminecrafts.enhancement;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MaceEnhancementTest {
 @Test void zeroIsVanillaAndEnhancementRaisesSameHeightDamage() {
  assertEquals(10, MaceEnhancement.effectiveFallDistance(10,0));
  assertTrue(MaceEnhancement.vanillaSmashBonus(MaceEnhancement.effectiveFallDistance(10,20))
    > MaceEnhancement.vanillaSmashBonus(MaceEnhancement.effectiveFallDistance(10,0)));
 }
 @Test void milestoneAreaAndCapAreMonotonic() {
  double lastR=0; int lastC=0;
  for(int level:new int[]{0,5,10,15,20}) { assertTrue(MaceEnhancement.radius(level)>=lastR); assertTrue(MaceEnhancement.secondaryTargetCap(level)>=lastC); lastR=MaceEnhancement.radius(level); lastC=MaceEnhancement.secondaryTargetCap(level); }
  assertEquals(4, MaceEnhancement.secondaryTargetCap(20));
 }
 @Test void profileInjectsNoGenericMeleeDamage() {
  var mace=new EnhancementDefinition(EnhancementCategory.VANILLA,EnhancementProfile.MACE);
  assertEquals(0, EnhancementEffects.value(mace,20,EnhancementEffect.Target.ATTACK_DAMAGE));
 }
}
