package dev.itzgamesgd.moreminecrafts.accessory;

import static dev.itzgamesgd.moreminecrafts.accessory.StarCovenantVaultRewards.*;
import static org.junit.jupiter.api.Assertions.*;

import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StarCovenantVaultRewardsTest {
    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    private final VaultRef v1 = new VaultRef("tower-1", "top"), v2 = new VaultRef("tower-1", "top-b"),
            otherTower = new VaultRef("tower-2", "top");

    @Test void tenPercentBoundaryIsExact() {
        assertEquals(0.10D, CHANCE);
        assertEquals(Outcome.HIT, resolve(new Ledger(), a, v1, () -> 0.0D));
        assertEquals(Outcome.HIT, resolve(new Ledger(), a, v1, () -> 0.0999999D));
        assertEquals(Outcome.MISS, resolve(new Ledger(), a, v1, () -> 0.10D));
        assertEquals(Outcome.MISS, resolve(new Ledger(), a, v1, () -> 0.9999D));
    }

    @Test void oneRollPerPlayerPerVaultHitOrMiss() {
        Ledger l = new Ledger();
        assertEquals(Outcome.MISS, resolve(l, a, v1, () -> 0.5D));
        assertEquals(Outcome.ALREADY_CLAIMED, resolve(l, a, v1, () -> { fail("no re-roll"); return 0.0D; }));
        Ledger h = new Ledger();
        assertEquals(Outcome.HIT, resolve(h, a, v1, () -> 0.0D));
        assertEquals(Outcome.ALREADY_CLAIMED, resolve(h, a, v1, () -> 0.0D), "no duplicate payout");
    }

    @Test void playersVaultsAndTowersAreIndependent() {
        Ledger l = new Ledger();
        assertEquals(Outcome.MISS, resolve(l, a, v1, () -> 0.5D));
        assertEquals(Outcome.HIT, resolve(l, b, v1, () -> 0.0D), "other player");
        assertEquals(Outcome.HIT, resolve(l, a, v2, () -> 0.0D), "other vault");
        assertEquals(Outcome.HIT, resolve(l, a, otherTower, () -> 0.0D), "other tower, same vault id");
    }

    @Test void identityIsStringIdsNotCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new VaultRef("", "x"));
        assertThrows(IllegalArgumentException.class, () -> new VaultRef("t", " "));
        assertEquals(new VaultRef("t", "v"), new VaultRef("t", "v"));
    }

    @Test void stateRestoresThroughCodecRoundTrip() {
        Ledger l = new Ledger();
        resolve(l, a, v1, () -> 0.5D); resolve(l, b, otherTower, () -> 0.0D);
        var json = ENTRIES_CODEC.encodeStart(JsonOps.INSTANCE, l.entries()).getOrThrow();
        Ledger restored = Ledger.of(ENTRIES_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertTrue(restored.hasClaimed(a, v1));
        assertTrue(restored.hasClaimed(b, otherTower));
        assertFalse(restored.hasClaimed(b, v1));
        assertEquals(Outcome.ALREADY_CLAIMED, resolve(restored, a, v1, () -> 0.0D), "restart cannot re-roll");
    }

    @Test void existingVaultFeatherAndShardPoolsAndNoStructureWiring() throws Exception {
        String vault = Files.readString(Path.of("src/main/resources/data/moreminecrafts/loot_table/chests/sky_islands/vault.json"));
        assertTrue(vault.contains("\"chance\": 0.15") && vault.contains("moreminecrafts:sky_feather"));
        assertTrue(vault.contains("\"chance\": 0.05") && vault.contains("moreminecrafts:gale_shard"));
        assertFalse(vault.contains("star_covenant"), "I04-A does not touch the vault loot table");
        String src = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/accessory/StarCovenantVaultRewards.java"));
        assertFalse(src.contains("BlockPos"), "no coordinates in reward logic");
        String main = Files.readString(Path.of("src/main/java/dev/itzgamesgd/moreminecrafts/MoreMinecrafts.java"));
        assertFalse(main.contains("StarCovenantVaultRewards"), "structure wiring is deferred to I04-B");
    }
}
