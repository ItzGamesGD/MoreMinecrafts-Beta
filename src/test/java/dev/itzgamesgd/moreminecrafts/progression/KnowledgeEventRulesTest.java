package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class KnowledgeEventRulesTest {
    @BeforeAll static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void oreSmeltingTypesAreRecognizedAndNotConfusedWithOrdinaryCooking() {
        assertEquals("iron", KnowledgeEventRules.oreType(new ItemStack(Items.RAW_IRON)).orElseThrow());
        assertEquals("iron", KnowledgeEventRules.oreType(new ItemStack(Items.IRON_ORE)).orElseThrow());
        assertEquals("titanium", KnowledgeEventRules.oreType(new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.RAW_TITANIUM)).orElseThrow());
        assertTrue(KnowledgeEventRules.oreType(new ItemStack(Items.BEEF)).isEmpty());
    }

    @Test void onlyRegisteredStrongPotionTypesQualifyForKnowledge70() {
        ItemStack strong = new ItemStack(Items.POTION);
        strong.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(Potions.STRONG_STRENGTH));
        ItemStack ordinary = new ItemStack(Items.POTION);
        ordinary.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(Potions.STRENGTH));
        assertTrue(KnowledgeEventRules.isStrengthenedPotion(strong));
        assertFalse(KnowledgeEventRules.isStrengthenedPotion(ordinary));
        assertNotEquals(KnowledgeEventRules.potionType(strong), KnowledgeEventRules.potionType(ordinary));
    }

    @Test void specialAmmunitionExcludesOrdinaryArrowsAndAcceptsApprovedTypes() {
        assertTrue(KnowledgeEventRules.isSpecialAmmunition(new ItemStack(Items.TIPPED_ARROW)));
        assertTrue(KnowledgeEventRules.isSpecialAmmunition(new ItemStack(Items.FIREWORK_ROCKET)));
        assertFalse(KnowledgeEventRules.isSpecialAmmunition(new ItemStack(Items.ARROW)));
        assertFalse(KnowledgeEventRules.isSpecialAmmunition(new ItemStack(Items.SPECTRAL_ARROW)));
    }
}
