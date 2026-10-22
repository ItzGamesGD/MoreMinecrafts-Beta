package dev.itzgamesgd.moreminecrafts.bounty;

import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractDataTest {
    private static ContractData sample() {
        return new ContractData("f87002f8-508e-4d35-84a7-4e4834ce0204", ContractData.Tier.BEGINNER,
                ContractData.Status.OFFERED, List.of(
                        new ContractData.Objective(ContractData.Category.COMBAT, "minecraft:zombie", 8, 0),
                        new ContractData.Objective(ContractData.Category.COMBAT, "minecraft:zombie", 16, 0),
                        new ContractData.Objective(ContractData.Category.DELIVERY, "minecraft:wheat", 16, 0)),
                new ContractData.Reward(7, "minecraft:iron_ingot", 20));
    }

    @Test void codecPreservesIdentityAndProgress() {
        var accepted = sample().accept().advance(ContractData.Category.COMBAT, "minecraft:zombie");
        var json = ContractData.CODEC.encodeStart(JsonOps.INSTANCE, accepted).getOrThrow();
        assertEquals(accepted, ContractData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test void duplicateEntriesAdvanceIndependently() {
        var accepted = sample().accept();
        for (int i = 0; i < 10; i++) accepted = accepted.advance(ContractData.Category.COMBAT, "minecraft:zombie");
        assertEquals(8, accepted.objectives().get(0).progress());
        assertEquals(10, accepted.objectives().get(1).progress());
        assertFalse(accepted.ready(Map.of("minecraft:wheat", 16)));
    }

    @Test void deliveryIsLiveAndDuplicateRequirementsAggregate() {
        var offered = sample();
        assertFalse(offered.ready(Map.of("minecraft:wheat", 64)));
        var ready = offered.accept();
        for (int i = 0; i < 16; i++) ready = ready.advance(ContractData.Category.COMBAT, "minecraft:zombie");
        assertTrue(ready.ready(Map.of("minecraft:wheat", 16)));
        assertFalse(ready.ready(Map.of("minecraft:wheat", 15)));
        assertEquals(Map.of("minecraft:wheat", 32), new ContractData(ready.id(), ready.tier(), ready.status(),
                List.of(ready.objectives().get(2), ready.objectives().get(2)), ready.reward()).deliveryNeeds());
    }

    @Test void explorationCountsDistinctStructureEntriesAfterAcceptance() {
        ContractData offered = new ContractData(sample().id(), ContractData.Tier.INTERMEDIATE,
                ContractData.Status.OFFERED, List.of(new ContractData.Objective(
                        ContractData.Category.EXPLORATION, "minecraft:mineshaft", 2, 0)), sample().reward());
        assertEquals(offered, offered.enterStructure("minecraft:mineshaft", "overworld:12:34"));
        ContractData accepted = offered.accept().enterStructure("minecraft:mineshaft", "overworld:12:34");
        assertEquals(1, accepted.objectives().getFirst().progress());
        assertEquals(accepted, accepted.enterStructure("minecraft:mineshaft", "overworld:12:34"));
        assertEquals(accepted, accepted.enterStructure("minecraft:stronghold", "overworld:15:40"));
        assertEquals(2, accepted.enterStructure("minecraft:mineshaft", "overworld:19:42").objectives().getFirst().progress());
    }

    @Test void oneKillCanAdvanceTwoAcceptedContracts() {
        ContractData first = sample().accept();
        ContractData second = new ContractData("e196d849-6127-4b67-9baa-4b5110ae8f51", first.tier(), first.status(),
                first.objectives(), first.reward());
        assertEquals(1, first.advance(ContractData.Category.COMBAT, "minecraft:zombie").objectives().getFirst().progress());
        assertEquals(1, second.advance(ContractData.Category.COMBAT, "minecraft:zombie").objectives().getFirst().progress());
        assertEquals(0, first.advance(ContractData.Category.COMBAT, "minecraft:skeleton").objectives().getFirst().progress());
    }
}
