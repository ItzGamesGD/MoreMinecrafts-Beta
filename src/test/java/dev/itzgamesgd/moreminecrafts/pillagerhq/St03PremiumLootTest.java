package dev.itzgamesgd.moreminecrafts.pillagerhq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class St03PremiumLootTest {
    @Test
    void triggerChancesRespect65_35_10Thresholds() {
        assertTrue(St03PremiumLoot.rollCategory(St03PremiumLoot.Source.MANOR, false, () -> 0.649, bound -> 0).isPresent());
        assertTrue(St03PremiumLoot.rollCategory(St03PremiumLoot.Source.MANOR, false, () -> 0.650, bound -> 0).isEmpty());

        assertTrue(St03PremiumLoot.rollCategory(St03PremiumLoot.Source.ARMORY, false, () -> 0.349, bound -> 0).isPresent());
        assertTrue(St03PremiumLoot.rollCategory(St03PremiumLoot.Source.ARMORY, false, () -> 0.350, bound -> 0).isEmpty());

        assertTrue(St03PremiumLoot.rollCategory(St03PremiumLoot.Source.COMMANDER, false, () -> 0.099, bound -> 0).isPresent());
        assertTrue(St03PremiumLoot.rollCategory(St03PremiumLoot.Source.COMMANDER, false, () -> 0.100, bound -> 0).isEmpty());
    }

    @Test
    void optionACategoryWeightsRenormalizeWhenRunesUnavailable() {
        // Manor without runes: 55 eq + 30 acc = 85 total
        assertEquals(St03PremiumLoot.Category.EQUIPMENT,
                St03PremiumLoot.pickCategory(St03PremiumLoot.Source.MANOR, false, bound -> 54));
        assertEquals(St03PremiumLoot.Category.ACCESSORY,
                St03PremiumLoot.pickCategory(St03PremiumLoot.Source.MANOR, false, bound -> 55));

        // Manor with runes: 55 eq + 30 acc + 15 rune = 100 total
        assertEquals(St03PremiumLoot.Category.RUNE,
                St03PremiumLoot.pickCategory(St03PremiumLoot.Source.MANOR, true, bound -> 90));
    }
}
