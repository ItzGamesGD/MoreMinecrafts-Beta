package dev.itzgamesgd.moreminecrafts.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit acceptance tests for ST-01 single PRIMARY royal chest architecture (2026-11-12 override).
 * Validates unified 5% catalyst, 20% accessory, single PRIMARY registration, and NBT backward compatibility.
 */
class RoyalChestRuntimeTest {

    @BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void catalystUnifiedOntoPrimaryAtFivePercentRate() {
        SplittableRandom random = new SplittableRandom(12345L);
        int n = 500_000;
        int primaryCount = 0;
        int secondaryCount = 0;
        int emptyCount = 0;

        for (int i = 0; i < n; i++) {
            Optional<RoyalChestRole> role = RoyalChestRewardModel.assignCatalyst(random);
            if (role.isEmpty()) {
                emptyCount++;
            } else if (role.get() == RoyalChestRole.PRIMARY) {
                primaryCount++;
            } else {
                secondaryCount++;
            }
        }

        assertEquals(0, secondaryCount, "SECONDARY should never receive catalyst assignment in single primary model");
        double rate = (double) primaryCount / n;
        assertTrue(Math.abs(rate - 0.05D) < 0.002D, "Catalyst assignment rate should be 5.0% on PRIMARY, got " + rate);
        assertEquals(n, emptyCount + primaryCount);
    }

    @Test
    void accessoryUnifiedOntoPrimaryAtTwentyPercentRate() {
        SplittableRandom random = new SplittableRandom(54321L);
        int n = 500_000;
        int hitCount = 0;

        for (int i = 0; i < n; i++) {
            Optional<String> accessory = RoyalChestRewardModel.rollAccessory(random);
            if (accessory.isPresent()) {
                hitCount++;
                String id = accessory.get();
                assertTrue(id.equals("moreminecrafts:seed_of_life") || id.equals("moreminecrafts:ring_of_time"),
                        "Accessory must be Seed of Life or Ring of Time: " + id);
            }
        }

        double rate = (double) hitCount / n;
        assertTrue(Math.abs(rate - 0.20D) < 0.003D, "Accessory rate should be 20.0%, got " + rate);
    }

    @Test
    void singlePrimaryRegistrationHasNoSecondaryByDefault() {
        RoyalChestData data = new RoyalChestData();
        BlockPos anchor = new BlockPos(10, 64, 20);
        BlockPos right = anchor.east();
        BlockPos spawn = anchor.north(7);

        Optional<RoyalChestData.Instance> reg = data.register("test-instance", anchor, spawn, anchor,
                Optional.of(right), Optional.of(RoyalChestRole.PRIMARY));

        assertTrue(reg.isPresent(), "Single primary registration succeeds");
        RoyalChestData.Instance instance = reg.get();
        assertTrue(instance.secondary().isEmpty(), "No secondary chest by default");
        assertEquals(Optional.of(anchor), instance.chestPos(RoyalChestRole.PRIMARY));
        assertEquals(Optional.empty(), instance.chestPos(RoyalChestRole.SECONDARY));
        assertEquals(Optional.of(RoyalChestRole.PRIMARY), instance.catalyst());

        // PRIMARY positions indexed
        assertTrue(data.linkAt(anchor).isPresent());
        assertEquals(RoyalChestRole.PRIMARY, data.linkAt(anchor).get().role());
        assertTrue(data.linkAt(right).isPresent());
        assertEquals(RoyalChestRole.PRIMARY, data.linkAt(right).get().role());

        // Secondary position has no link
        assertFalse(data.linkAt(anchor.south(10)).isPresent());
    }

    @Test
    void legacySchemaNbtDecodesWithoutMigrationFailure() {
        CompoundTag record = new CompoundTag();
        record.putString("id", "legacy-pyramid-instance");
        record.putLong("tomb_anchor", new BlockPos(100, 40, 200).asLong());
        record.putLong("spawn", new BlockPos(100, 38, 193).asLong());
        record.putLong("primary", new BlockPos(100, 40, 200).asLong());
        record.putLong("secondary", new BlockPos(105, 40, 200).asLong());
        record.putString("catalyst", "SECONDARY");

        ListTag instances = new ListTag();
        instances.add(record);

        CompoundTag root = new CompoundTag();
        root.put("instances", instances);
        root.put("chests", new ListTag());

        // Decode via Codec
        RoyalChestData data = RoyalChestData.TYPE.codec().parse(NbtOps.INSTANCE, root).getOrThrow();
        assertNotNull(data, "Decoded data must not be null");

        // Verify instance decoded cleanly
        Optional<RoyalChestData.Instance> instOpt = data.instance("legacy-pyramid-instance");
        assertTrue(instOpt.isPresent(), "Legacy instance exists");
        RoyalChestData.Instance inst = instOpt.get();
        assertEquals("legacy-pyramid-instance", inst.id());
        assertTrue(inst.secondary().isPresent(), "Legacy secondary position preserved");
        assertEquals(Optional.of(RoyalChestRole.SECONDARY), inst.catalyst(), "Legacy catalyst role preserved");

        // Round-trip encode
        var encoded = RoyalChestData.TYPE.codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        RoyalChestData roundTripped = RoyalChestData.TYPE.codec().parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(1, roundTripped.instanceCount());
    }

    @Test
    void mixDeterministicRandomStreams() {
        long seed = 42L;
        String id = "test-instance";
        long salt1 = 0x1L;
        long salt2 = 0x10L;

        long r1a = RoyalChestRuntime.mix(seed, id, salt1);
        long r1b = RoyalChestRuntime.mix(seed, id, salt1);
        assertEquals(r1a, r1b, "Mix must be purely deterministic");

        long r2 = RoyalChestRuntime.mix(seed, id, salt2);
        assertTrue(r1a != r2, "Different salts produce different streams");
    }
}
