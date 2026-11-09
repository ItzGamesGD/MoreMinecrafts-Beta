package dev.itzgamesgd.moreminecrafts.bounty;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractBalanceTest {
    @Test void onlyApprovedHighValueDeliveryExceptionsExist() {
        assertEquals(2, ContractBalance.DELIVERY_EXCEPTIONS.size());
        assertArrayEquals(new int[]{2, 4}, ContractBalance.DELIVERY_EXCEPTIONS.get("minecraft:diamond"));
        assertArrayEquals(new int[]{1, 1}, ContractBalance.DELIVERY_EXCEPTIONS.get("moreminecrafts:adamantium_ingot"));
        assertFalse(ContractBalance.DELIVERY_EXCEPTIONS.containsKey("moreminecrafts:mythril"));
    }
}
