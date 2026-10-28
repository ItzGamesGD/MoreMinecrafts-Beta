package dev.itzgamesgd.moreminecrafts.bounty;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

/** Child-JVM entry point: touches the contract codecs in one chosen order, then round-trips a golden contract. */
public final class InitOrderProbe {
    public static final String GOLDEN = """
            {"id":"00000000-0000-4000-8000-000000000001","tier":"ADVANCED","status":"ACCEPTED",
            "objectives":[{"category":"COMBAT","target":"minecraft:zombie","required":40,"progress":3},
            {"category":"EXPLORATION","target":"minecraft:mineshaft","required":1,"progress":1,"visited":["a"]}],
            "reward":{"emeralds":30,"item":"minecraft:diamond","count":2}}""";

    private InitOrderProbe() {}

    public static void main(String[] args) {
        switch (args[0]) {
            case "contract-first" -> ContractData.CODEC.hashCode();
            case "objective-first" -> ContractData.Objective.CODEC.hashCode();
            case "category-first" -> ContractData.Category.valueOf("COMBAT");
            case "generator-first" -> ContractGenerator.tryObjectives(ContractData.Tier.BEGINNER,
                    net.minecraft.util.RandomSource.create(1));
            default -> throw new IllegalArgumentException(args[0]);
        }
        JsonElement golden = JsonParser.parseString(GOLDEN);
        ContractData decoded = ContractData.CODEC.parse(JsonOps.INSTANCE, golden).getOrThrow();
        JsonElement encoded = ContractData.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow();
        if (!encoded.equals(golden)) throw new IllegalStateException("format drift: " + encoded);
        System.out.println("OK " + encoded);
    }
}
