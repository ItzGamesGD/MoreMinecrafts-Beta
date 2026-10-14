package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Map;
import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentMath;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionRulesTest {
    @BeforeAll static void bootstrapMinecraft(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    @Test void contextualTreesDoNotLeakOtherEquipment() {
        assertTrue(EnchantmentTrees.tree(EnchantmentTrees.Equipment.SWORD).stream().anyMatch(n -> n.id().equals("minecraft:sharpness")));
        assertFalse(EnchantmentTrees.tree(EnchantmentTrees.Equipment.PICKAXE).stream().anyMatch(n -> n.id().equals("minecraft:sharpness")));
        assertTrue(EnchantmentTrees.tree(EnchantmentTrees.Equipment.NONE).isEmpty());
        assertEquals(EnchantmentTrees.Equipment.SWORD,EquipmentTreeResolver.vanillaProfile(Items.NETHERITE_SWORD));
        assertEquals(EnchantmentTrees.Equipment.BOW,EquipmentTreeResolver.vanillaProfile(Items.BOW));
        assertEquals(EnchantmentTrees.Equipment.CROSSBOW,EquipmentTreeResolver.vanillaProfile(Items.CROSSBOW));
        assertEquals(EnchantmentTrees.Equipment.HELMET,EquipmentTreeResolver.vanillaProfile(Items.DIAMOND_HELMET));
        assertEquals(EnchantmentTrees.Equipment.SPEAR,EquipmentTreeResolver.vanillaProfile(Items.IRON_SPEAR));
        assertEquals(EnchantmentTrees.Equipment.SWORD,EquipmentTreeResolver.customProfile(Identifier.parse("moreminecrafts:flame_sword")));
        assertEquals(EnchantmentTrees.Equipment.NONE,EquipmentTreeResolver.customProfile(Identifier.parse("othermod:pretend_sword")));
        assertEquals(EnchantmentTrees.Equipment.NONE,EquipmentTreeResolver.customProfile(Identifier.parse("othermod:pretend_bow")));
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
    @Test void multiCatchIsOneLevelAfterLureThree() {
        var withoutLure = EnchantmentTrees.quote(EnchantmentTrees.Equipment.FISHING_ROD, Map.of(), "moreminecrafts:multi_catch");
        var unlocked = EnchantmentTrees.quote(EnchantmentTrees.Equipment.FISHING_ROD,
                Map.of("minecraft:lure", 3), "moreminecrafts:multi_catch");
        assertFalse(withoutLure.allowed());
        assertEquals("prerequisite", withoutLure.reason());
        assertTrue(unlocked.allowed());
        assertEquals(1000, unlocked.cost());
    }
    @Test void binomialBoundariesAndSingleLevel() {
        assertEquals(.2, BookOptionRules.probability(1,5), 1e-9);
        assertEquals(.8, BookOptionRules.probability(5,5), 1e-9);
        assertEquals(5, BookOptionRules.roll(5,1,5,()->0));
        assertEquals(1, BookOptionRules.roll(5,5,5,()->.999));
        assertEquals(1, BookOptionRules.roll(1,1,1,()->{throw new AssertionError("single level must not draw RNG");}));
        assertThrows(IllegalArgumentException.class, ()->BookOptionRules.roll(5,6,5,()->0));
    }
    @Test void confirmedDamageOptionAndAnvilUseValues() {
        assertEquals(.25,T1Balance.DAMAGE_OPTION_PER_LEVEL,1e-12);
        assertEquals(1.25,5*T1Balance.DAMAGE_OPTION_PER_LEVEL,1e-12);
        assertTrue(T1AnvilUse.shouldDamage(false,true,.119999));
        assertFalse(T1AnvilUse.shouldDamage(false,true,.12));
        assertFalse(T1AnvilUse.shouldDamage(true,true,0));
        assertFalse(T1AnvilUse.shouldDamage(false,false,0));
        assertEquals(4,EnchantmentMath.miningOperationDurability(7,2,false,2,0));
        assertEquals(4,EnchantmentMath.miningOperationDurability(7,2,false,2,3));
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
