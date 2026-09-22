package dev.itzgamesgd.moreminecrafts.enhancement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnhancementEffectsTest {
    private static final double DELTA = 0.000_000_1D;

    @Test
    void resolvesMeleeProfilesLinearly() {
        assertEffect(EnhancementProfile.SWORD, 0, EnhancementEffect.Target.ATTACK_DAMAGE, 0.0D);
        assertEffect(EnhancementProfile.SWORD, 1, EnhancementEffect.Target.ATTACK_DAMAGE, 0.08D);
        assertEffect(EnhancementProfile.SWORD, 10, EnhancementEffect.Target.ATTACK_DAMAGE, 0.80D);
        assertEffect(EnhancementProfile.SWORD, 30, EnhancementEffect.Target.ATTACK_DAMAGE, 2.40D);
        assertEffect(EnhancementProfile.MELEE, 10, EnhancementEffect.Target.ATTACK_DAMAGE, 1.00D);
        assertEffect(EnhancementProfile.SPEAR, 10, EnhancementEffect.Target.ATTACK_DAMAGE, 0.90D);
    }

    @Test
    void resolvesToolAndAxeEffectsLinearly() {
        assertEffect(EnhancementProfile.TOOL, 10, EnhancementEffect.Target.TOOL_EFFICIENCY, 6.50D);
        assertEffect(EnhancementProfile.AXE, 10, EnhancementEffect.Target.ATTACK_DAMAGE, 1.00D);
        assertEffect(EnhancementProfile.AXE, 10, EnhancementEffect.Target.TOOL_EFFICIENCY, 6.50D);
    }

    @Test
    void unrelatedProfilesHaveNoMeleeOrToolRuntimeEffect() {
        for (EnhancementProfile profile : new EnhancementProfile[]{
                EnhancementProfile.ELYTRA,
                EnhancementProfile.RANGED,
                EnhancementProfile.ARMOR
        }) {
            assertEffect(profile, 10, EnhancementEffect.Target.ATTACK_DAMAGE, 0.0D);
            assertEffect(profile, 10, EnhancementEffect.Target.TOOL_EFFICIENCY, 0.0D);
        }
    }

    private static void assertEffect(
            EnhancementProfile profile,
            int level,
            EnhancementEffect.Target target,
            double expected
    ) {
        var definition = new EnhancementDefinition(EnhancementCategory.VANILLA, profile);
        assertEquals(expected, EnhancementEffects.value(definition, level, target), DELTA);
    }
}
