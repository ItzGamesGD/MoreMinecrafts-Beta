package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionRulesTest {
    @Test void contextualTreesDoNotLeakOtherEquipment() {
        assertTrue(EnchantmentTrees.tree(EnchantmentTrees.Equipment.SWORD).stream().anyMatch(n -> n.id().equals("minecraft:sharpness")));
        assertFalse(EnchantmentTrees.tree(EnchantmentTrees.Equipment.PICKAXE).stream().anyMatch(n -> n.id().equals("minecraft:sharpness")));
        assertTrue(EnchantmentTrees.tree(EnchantmentTrees.Equipment.NONE).isEmpty());
    }
    @Test void sequentialCostsAndDurabilityPrerequisite() {
        assertEquals(100, EnchantmentTrees.quote(EnchantmentTrees.Equipment.SWORD, Map.of(), "minecraft:sharpness").cost());
        assertFalse(EnchantmentTrees.quote(EnchantmentTrees.Equipment.SWORD, Map.of(), "minecraft:mending").allowed());
        assertTrue(EnchantmentTrees.quote(EnchantmentTrees.Equipment.SWORD, Map.of("minecraft:unbreaking", 3), "minecraft:mending").allowed());
        assertEquals(400, EnchantmentTrees.quote(EnchantmentTrees.Equipment.SWORD, Map.of("minecraft:sharpness",4), "minecraft:sharpness").cost());
    }
    @Test void protectionPremiumOnlyChargedForFirstLevelOfAdditionalFamily() {
        assertEquals(600, EnchantmentTrees.quote(EnchantmentTrees.Equipment.BOOTS, Map.of("minecraft:protection",1), "minecraft:fire_protection").cost());
        assertEquals(150, EnchantmentTrees.quote(EnchantmentTrees.Equipment.BOOTS, Map.of("minecraft:protection",1,"minecraft:fire_protection",1), "minecraft:fire_protection").cost());
        assertEquals(2100, EnchantmentTrees.quote(EnchantmentTrees.Equipment.BOOTS, Map.of("minecraft:protection",1,"minecraft:fire_protection",1,"minecraft:blast_protection",1), "minecraft:projectile_protection").cost());
    }
    @Test void structuralConflictsAndFreeStacking() {
        assertEquals(500, EnchantmentTrees.quote(EnchantmentTrees.Equipment.MACE, Map.of("minecraft:density",1), "minecraft:breach").premium());
        assertFalse(EnchantmentTrees.quote(EnchantmentTrees.Equipment.BOW, Map.of("minecraft:power",3,"moreminecrafts:multi_shot",1), "moreminecrafts:laser_arrow").allowed());
        assertFalse(EnchantmentTrees.quote(EnchantmentTrees.Equipment.CROSSBOW, Map.of("minecraft:quick_charge",3,"minecraft:multishot",1), "moreminecrafts:crossbow_barrage").allowed());
        assertTrue(EnchantmentTrees.quote(EnchantmentTrees.Equipment.BOW, Map.of("minecraft:unbreaking",3,"minecraft:infinity",1), "minecraft:mending").allowed());
        assertTrue(EnchantmentTrees.quote(EnchantmentTrees.Equipment.CROSSBOW, Map.of("minecraft:piercing",4), "minecraft:multishot").allowed());
    }
    @Test void binomialBoundariesAndSingleLevel() {
        assertEquals(.2, BookOptionRules.probability(1,5), 1e-9);
        assertEquals(.8, BookOptionRules.probability(5,5), 1e-9);
        assertEquals(5, BookOptionRules.roll(5,1,5,()->0));
        assertEquals(1, BookOptionRules.roll(5,5,5,()->.999));
        assertEquals(1, BookOptionRules.roll(1,1,1,()->{throw new AssertionError("single level must not draw RNG");}));
        assertThrows(IllegalArgumentException.class, ()->BookOptionRules.roll(5,6,5,()->0));
    }
    @Test void retainedAllowListAndMatchingBooks() {
        assertTrue(BookOptionRules.retained("minecraft:mending"));
        assertFalse(BookOptionRules.retained("minecraft:looting"));
        assertFalse(BookOptionRules.retained("moreminecrafts:vein_mining"));
        assertEquals(3, BookOptionRules.combine("minecraft:sharpness",2,"minecraft:sharpness",2));
        assertEquals(0, BookOptionRules.combine("minecraft:sharpness",2,"minecraft:power",2));
        assertEquals(0, BookOptionRules.combine("minecraft:sharpness",5,"minecraft:sharpness",5));
    }
    @Test void bonusProtectionPreservesVanillaAndCapsAtNinetyPercent() {
        assertEquals(20, BookOptionRules.protectedDamage(100,20,0), 1e-6);
        assertEquals(10, BookOptionRules.protectedDamage(100,20,.4), 1e-6);
        assertEquals(82.5, BookOptionRules.protectedDamage(100,90,.075), 1e-6);
    }
}
