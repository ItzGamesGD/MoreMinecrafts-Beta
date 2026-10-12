package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

final class KnowledgeEventRulesTest {
    @Test void oreSmeltingTypesAreRecognizedAndNotConfusedWithOrdinaryCooking() {
        assertEquals("iron", KnowledgeEventRules.oreTypeFromId(Identifier.parse("minecraft:raw_iron")).orElseThrow());
        assertEquals("iron", KnowledgeEventRules.oreTypeFromId(Identifier.parse("minecraft:iron_ore")).orElseThrow());
        assertEquals("titanium", KnowledgeEventRules.oreTypeFromId(Identifier.parse("moreminecrafts:titanium_ore")).orElseThrow());
        assertTrue(KnowledgeEventRules.oreTypeFromId(Identifier.parse("minecraft:beef")).isEmpty());
    }

    @Test void onlyRegisteredStrongPotionTypesQualifyForKnowledge70() {
        assertTrue(KnowledgeEventRules.isStrengthenedPotionId("minecraft:strong_strength"));
        assertFalse(KnowledgeEventRules.isStrengthenedPotionId("minecraft:strength"));
        assertNotEquals("minecraft:strong_strength", "minecraft:strength");
    }

    @Test void specialAmmunitionExcludesOrdinaryArrowsAndAcceptsApprovedTypes() {
        assertTrue(KnowledgeEventRules.isSpecialAmmunitionId(Identifier.parse("minecraft:tipped_arrow")));
        assertTrue(KnowledgeEventRules.isSpecialAmmunitionId(Identifier.parse("minecraft:firework_rocket")));
        assertFalse(KnowledgeEventRules.isSpecialAmmunitionId(Identifier.parse("minecraft:arrow")));
        assertFalse(KnowledgeEventRules.isSpecialAmmunitionId(Identifier.parse("minecraft:spectral_arrow")));
    }

    @Test void fireworkAmmunitionRequiresCrossbowLaunchOrigin() {
        Identifier firework = Identifier.parse("minecraft:firework_rocket");
        Identifier tippedArrow = Identifier.parse("minecraft:tipped_arrow");
        assertFalse(KnowledgeEventRules.isEligibleSpecialAmmunitionId(firework, false));
        assertTrue(KnowledgeEventRules.isEligibleSpecialAmmunitionId(firework, true));
        assertTrue(KnowledgeEventRules.isEligibleSpecialAmmunitionId(tippedArrow, false));
        assertTrue(KnowledgeEventRules.isEligibleSpecialAmmunitionId(tippedArrow, true));
    }
}
