package dev.itzgamesgd.moreminecrafts.enhancement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnhancementEffectsTest {
    private static final double D = 1e-9;
    private static EnhancementDefinition def(EnhancementProfile p) { return new EnhancementDefinition(EnhancementCategory.VANILLA, p); }
    @Test void provisionalSwordCurveIsCentralizedAndClamped() {
        assertEquals(.5, EnhancementEffects.value(def(EnhancementProfile.SWORD), 5, EnhancementEffect.Target.ATTACK_DAMAGE), D);
        assertEquals(1.1, EnhancementEffects.value(def(EnhancementProfile.SWORD), 10, EnhancementEffect.Target.ATTACK_DAMAGE), D);
        assertEquals(2.6, EnhancementEffects.value(def(EnhancementProfile.SWORD), 20, EnhancementEffect.Target.ATTACK_DAMAGE), D);
        assertEquals(2.6, EnhancementEffects.value(def(EnhancementProfile.SWORD), 100, EnhancementEffect.Target.ATTACK_DAMAGE), D);
        assertEquals(0, EnhancementEffects.value(def(EnhancementProfile.SWORD), -1, EnhancementEffect.Target.ATTACK_DAMAGE), D);
    }
    @Test void armorFullSetMeetsProvisionalEightPercentCeiling() {
        double piece = EnhancementEffects.damageReduction(def(EnhancementProfile.ARMOR), 20);
        assertEquals(.02, piece, D); assertTrue(piece * 4 <= .08 + D);
        assertEquals(.02, EnhancementEffects.damageReduction(def(EnhancementProfile.ARMOR), 100), D);
    }
    @Test void toolGrowthIsBoundedAndMaceFormulaRemainsDeferred() {
        assertEquals(2.0, EnhancementEffects.value(def(EnhancementProfile.TOOL), 100, EnhancementEffect.Target.TOOL_EFFICIENCY), D);
        assertEquals(0, EnhancementEffects.value(def(EnhancementProfile.MACE), 20, EnhancementEffect.Target.ATTACK_DAMAGE), D);
        assertEquals(MilestoneEffect.Kind.MACE_SMASH_SCALING_SEAM,
                EnhancementProfile.MACE.milestoneEffects().getFirst().kind());
    }
    @Test void toolSpeedOnlyExtendsNativeEffectiveBlockSpeed() {
        assertEquals(1.0F, EnhancementEffects.toolDestroySpeed(1.0F, 2.0D));
        assertEquals(.5F, EnhancementEffects.toolDestroySpeed(.5F, 2.0D));
        assertEquals(10.0F, EnhancementEffects.toolDestroySpeed(8.0F, 2.0D));
    }
    @Test void rangedAndTridentProjectilePipelineRemainsCompatible() {
        assertEquals(1.6, EnhancementEffects.projectileDamage(def(EnhancementProfile.RANGED), 100), D);
        assertEquals(1.8, EnhancementEffects.projectileDamage(def(EnhancementProfile.SPEAR), 100), D);
        assertEquals(0, EnhancementEffects.projectileDamage(def(EnhancementProfile.SWORD), 20), D);
    }
}
