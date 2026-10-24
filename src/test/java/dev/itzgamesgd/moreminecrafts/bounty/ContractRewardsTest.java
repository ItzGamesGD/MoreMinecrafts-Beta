package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractRewardsTest {
    @Test void poolsMatchApprovedRarityLists() {
        assertEquals(List.of("minecraft:coal", "minecraft:copper_ingot"), ContractRewards.pool(ContractEconomy.Rarity.COMMON));
        assertEquals(List.of("minecraft:iron_ingot", "minecraft:gold_ingot"), ContractRewards.pool(ContractEconomy.Rarity.HIGH_GRADE));
        assertEquals(List.of("minecraft:diamond", "moreminecrafts:titanium_ingot"), ContractRewards.pool(ContractEconomy.Rarity.RARE));
        assertEquals(List.of("moreminecrafts:adamantium_ingot"), ContractRewards.pool(ContractEconomy.Rarity.LEGENDARY));
    }

    @Test void itemCapsClampQuantityWithoutInvertingBands() {
        assertArrayEquals(new int[]{2, 2}, ContractRewards.quantityRange("minecraft:diamond", ContractEconomy.Rarity.RARE));
        assertArrayEquals(new int[]{2, 4}, ContractRewards.quantityRange("moreminecrafts:titanium_ingot", ContractEconomy.Rarity.RARE));
        assertArrayEquals(new int[]{1, 1}, ContractRewards.quantityRange("moreminecrafts:adamantium_ingot", ContractEconomy.Rarity.LEGENDARY));
        assertArrayEquals(new int[]{16, 48}, ContractRewards.quantityRange("minecraft:coal", ContractEconomy.Rarity.COMMON));
        assertArrayEquals(new int[]{8, 24}, ContractRewards.quantityRange("minecraft:gold_ingot", ContractEconomy.Rarity.HIGH_GRADE));
    }

    @Test void selectRejectsCountsOutsideCapOrBand() {
        assertEquals(new ContractRewards.Pick("minecraft:diamond", 2), ContractRewards.select(ContractEconomy.Rarity.RARE, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> ContractRewards.select(ContractEconomy.Rarity.RARE, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> ContractRewards.select(ContractEconomy.Rarity.LEGENDARY, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> ContractRewards.select(ContractEconomy.Rarity.COMMON, 2, 16));
    }

    @Test void specialConversionKeepsAdvancedBandsAndSpecialDistribution() {
        assertArrayEquals(new int[]{12, 20, 25, 50}, ContractEconomy.band(ContractData.Tier.SPECIAL));
        assertEquals(ContractEconomy.Rarity.COMMON, ContractEconomy.rarity(ContractData.Tier.SPECIAL, 199));
        assertEquals(ContractEconomy.Rarity.HIGH_GRADE, ContractEconomy.rarity(ContractData.Tier.SPECIAL, 200));
        assertEquals(ContractEconomy.Rarity.RARE, ContractEconomy.rarity(ContractData.Tier.SPECIAL, 650));
        assertEquals(ContractEconomy.Rarity.LEGENDARY, ContractEconomy.rarity(ContractData.Tier.SPECIAL, 950));
    }
}
