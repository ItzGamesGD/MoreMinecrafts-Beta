package dev.itzgamesgd.moreminecrafts.bounty;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/** Owner decision D2 (2026-10-25): exploration objectives require 1 / 1–2 / 2–3 distinct structure instances. */
class ContractExplorationTest {
    private static final String ID = "f87002f8-508e-4d35-84a7-4e4834ce0204";
    /** The approved table, written out rather than read from the generator. */
    private static final Map<ContractData.Tier, int[]> APPROVED = Map.of(
            ContractData.Tier.BEGINNER, new int[]{1, 1},
            ContractData.Tier.INTERMEDIATE, new int[]{1, 2},
            ContractData.Tier.ADVANCED, new int[]{2, 3},
            ContractData.Tier.SPECIAL, new int[]{2, 3});

    @Test void generatedExplorationCountsCoverExactlyTheApprovedBand() {
        RandomSource random = RandomSource.create(2026_10_25L);
        for (var row : APPROVED.entrySet()) {
            int min = Integer.MAX_VALUE, max = 0, seen = 0;
            for (int i = 0; i < 40_000; i++) {
                for (ContractData.Objective objective : ContractGenerator.generate(ID, row.getKey(), random).objectives()) {
                    if (objective.category() != ContractData.Category.EXPLORATION) continue;
                    seen++;
                    min = Math.min(min, objective.required());
                    max = Math.max(max, objective.required());
                    assertEquals(0, objective.progress());
                    assertTrue(objective.visited().isEmpty(), "a new objective has no visited instances");
                    assertTrue(ContractCatalog.EXPLORATION_STRUCTURES.contains(objective.target()), objective.target());
                }
            }
            assertTrue(seen > 1000, row.getKey() + " generated exploration objectives: " + seen);
            assertEquals(row.getValue()[0], min, row.getKey() + " minimum locations");
            assertEquals(row.getValue()[1], max, row.getKey() + " maximum locations");
        }
    }

    @Test void multiLocationContractsAreDeterministicForASeed() {
        for (long seed = 0; seed < 500; seed++)
            assertEquals(ContractGenerator.generate(ID, ContractData.Tier.ADVANCED, RandomSource.create(seed)),
                    ContractGenerator.generate(ID, ContractData.Tier.ADVANCED, RandomSource.create(seed)), "seed " + seed);
    }

    @Test void onlyDistinctInstancesAdvanceAndCompletionNeedsAllOfThem() {
        ContractData data = new ContractData(ID, ContractData.Tier.ADVANCED, ContractData.Status.ACCEPTED,
                List.of(new ContractData.Objective(ContractData.Category.EXPLORATION, "minecraft:mineshaft", 3, 0)),
                new ContractData.Reward(25, "minecraft:coal", 16));
        data = data.enterStructure("minecraft:mineshaft", "minecraft:overworld:1");
        data = data.enterStructure("minecraft:mineshaft", "minecraft:overworld:1");
        assertEquals(1, data.objectives().getFirst().progress(), "re-entering one instance counts once");
        data = data.enterStructure("minecraft:mineshaft", "minecraft:the_nether:1");
        assertEquals(2, data.objectives().getFirst().progress(), "the same chunk key in another dimension is a different instance");
        assertFalse(data.ready(Map.of()), "two of three locations is not complete");
        data = data.enterStructure("minecraft:stronghold", "minecraft:overworld:2");
        assertEquals(2, data.objectives().getFirst().progress(), "another structure type never counts");
        data = data.enterStructure("minecraft:mineshaft", "minecraft:overworld:2");
        assertEquals(3, data.objectives().getFirst().progress());
        assertTrue(data.ready(Map.of()), "three distinct instances complete it");
        ContractData full = data.enterStructure("minecraft:mineshaft", "minecraft:overworld:3");
        assertEquals(data, full, "a completed objective records nothing further");
        assertEquals(List.of("minecraft:overworld:1", "minecraft:the_nether:1", "minecraft:overworld:2"),
                full.objectives().getFirst().visited());
    }

    @Test void legacySingleLocationContractsStillDecodeAndComplete() {
        // Shape written by builds before D2: required 1, and objectives without the optional visited list.
        String legacy = """
                {"id":"%s","tier":"INTERMEDIATE","status":"ACCEPTED","objectives":[
                {"category":"EXPLORATION","target":"minecraft:mineshaft","required":1,"progress":0},
                {"category":"COMBAT","target":"minecraft:zombie","required":20,"progress":20}],
                "reward":{"emeralds":12,"item":"minecraft:coal","count":16}}""".formatted(ID);
        ContractData data = ContractData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(legacy)).getOrThrow();
        assertEquals(1, data.objectives().getFirst().required(), "the legacy requirement is not rewritten");
        assertTrue(data.objectives().getFirst().visited().isEmpty());
        assertFalse(data.ready(Map.of()));
        ContractData done = data.enterStructure("minecraft:mineshaft", "minecraft:overworld:9");
        assertTrue(done.ready(Map.of()), "one location still completes a legacy single-location objective");
        assertEquals(done, ContractData.CODEC.parse(JsonOps.INSTANCE,
                ContractData.CODEC.encodeStart(JsonOps.INSTANCE, done).getOrThrow()).getOrThrow(), "round trip");

        String legacyDone = """
                {"id":"%s","tier":"BEGINNER","status":"ACCEPTED","objectives":[
                {"category":"EXPLORATION","target":"minecraft:mineshaft","required":1,"progress":1,"visited":["a"]}],
                "reward":{"emeralds":5,"item":"minecraft:coal","count":16}}""".formatted(ID);
        ContractData finished = ContractData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(legacyDone)).getOrThrow();
        assertTrue(finished.ready(Map.of()), "a legacy objective that was already complete stays complete");
    }
}
