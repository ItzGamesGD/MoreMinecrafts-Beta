package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumOwnership.Kind;
import dev.itzgamesgd.moreminecrafts.colosseum.WaveRecord.KeyState;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import org.junit.jupiter.api.Test;

/** D1/D4/D7: schema v1, v0 migration, newer-version preservation, archive restore, and tag compatibility. */
class ColosseumSchemaTest {
    private static final Codec<ColosseumArenaData> CODEC = ColosseumArenaData.TYPE.codec();
    private static final BlockPos CENTER = new BlockPos(100, 64, -40);

    private static JsonElement uuid(UUID id) { return UUIDUtil.CODEC.encodeStart(JsonOps.INSTANCE, id).getOrThrow(); }

    /** A save in the pre-schema layout: a bare list of arenas, boss_started in the timers, cleared_waves in the content. */
    private static JsonArray v0World(UUID trial, UUID player) {
        JsonObject core = new JsonObject();
        core.addProperty("phase", "ROUND");
        core.addProperty("last_outcome", "NONE");
        core.add("trial", uuid(trial));
        core.addProperty("serial", 3);
        core.addProperty("seed", 5);
        core.addProperty("round", 3);
        JsonObject timers = new JsonObject();
        timers.addProperty("intermission", 0);
        timers.addProperty("absence", 0);
        timers.addProperty("spawn_blocked_ticks", 0);
        timers.addProperty("spawn_blocked", false);
        timers.addProperty("batch_timer", 0);
        timers.addProperty("cooldown", 0);
        timers.addProperty("boss_started", true);
        JsonObject party = new JsonObject();
        JsonArray locked = new JsonArray();
        locked.add(uuid(player));
        party.add("locked", locked);
        party.add("dead", new JsonArray());
        party.add("ever", locked.deepCopy());
        JsonObject content = new JsonObject();
        content.add("allocations", new JsonArray());
        content.add("supplies", new JsonArray());
        JsonArray waves = new JsonArray();
        for (int round = 1; round <= 2; round++) {
            JsonObject wave = new JsonObject();
            wave.add("trial", uuid(trial));
            wave.addProperty("round", round);
            wave.add("locked", locked.deepCopy());
            wave.add("present", locked.deepCopy());
            wave.addProperty("game_time", 100 * round);
            waves.add(wave);
        }
        content.add("cleared_waves", waves);
        JsonObject state = new JsonObject();
        state.add("core", core);
        state.add("timers", timers);
        state.add("party", party);
        state.add("content", content);
        JsonObject arena = new JsonObject();
        arena.add("geometry", ColosseumArenaGeometry.CODEC.encodeStart(JsonOps.INSTANCE,
                ColosseumArenaGeometry.testFixture(CENTER)).getOrThrow());
        arena.add("state", state);
        JsonArray root = new JsonArray();
        root.add(arena);
        return root;
    }

    @Test
    void aV0WorldMigratesWithItsWavesBossFlagAndAFullTrialLimit() {
        UUID trial = UUID.randomUUID(), player = UUID.randomUUID();
        ColosseumArenaData data = CODEC.parse(JsonOps.INSTANCE, v0World(trial, player)).getOrThrow();
        assertFalse(data.readOnly());
        assertEquals(1, data.arenas().size());
        ColosseumArena arena = data.arena(CENTER.asLong());
        assertNotNull(arena);
        assertEquals(ColosseumArena.testInstanceId(ColosseumArena.UNKNOWN_DIMENSION, CENTER), arena.instanceId());
        ColosseumTrialState state = arena.state();
        assertEquals(trial, state.trialId());
        assertEquals(List.of(player), state.lockedParticipants());
        assertTrue(state.boss().started(), "timers.boss_started moves to boss.started");
        assertEquals(ColosseumRules.TRIAL_LIMIT_TICKS, state.trialRemaining(), "v0 had no limit: a full 65 minutes");
        assertEquals(2, state.ledger().waves().size());
        assertTrue(state.waveCleared(trial, 1) && state.waveCleared(trial, 2));
        for (WaveRecord wave : state.ledger().waves())
            assertEquals(KeyState.NONE, wave.key().state(), "v0 never issued keys, so none is issued retroactively");
        data.adoptDimension("minecraft:overworld");
        assertEquals(ColosseumArena.testInstanceId("minecraft:overworld", CENTER), data.arena(CENTER.asLong()).instanceId());
        assertNotNull(data.arenaByInstance(ColosseumArena.testInstanceId("minecraft:overworld", CENTER)));
    }

    @Test
    void migratedWavesGetNoKeyEvenAfterTheControllerRuns() {
        UUID trial = UUID.randomUUID(), player = UUID.randomUUID();
        ColosseumArena arena = CODEC.parse(JsonOps.INSTANCE, v0World(trial, player)).getOrThrow().arena(CENTER.asLong());
        FakeColosseumWorld world = new FakeColosseumWorld();
        world.present.add(player);
        world.tick(arena.state(), 100);
        assertEquals(0, world.keysSpawned);
    }

    @Test
    void aMigratedWorldIsWrittenAsV1AndReadsBackIdentically() {
        ColosseumArenaData migrated = CODEC.parse(JsonOps.INSTANCE, v0World(UUID.randomUUID(), UUID.randomUUID())).getOrThrow();
        JsonElement v1 = CODEC.encodeStart(JsonOps.INSTANCE, migrated).getOrThrow();
        assertEquals(1, v1.getAsJsonObject().get("schema").getAsInt());
        ColosseumArenaData reread = CODEC.parse(JsonOps.INSTANCE, v1).getOrThrow();
        assertEquals(v1, CODEC.encodeStart(JsonOps.INSTANCE, reread).getOrThrow());
    }

    @Test
    void aV1WorldRoundTripsWithEverySection() {
        ColosseumArenaData data = new ColosseumArenaData();
        ColosseumArena a = data.register(ColosseumArenaGeometry.testFixture(CENTER), "structure|x|1");
        ColosseumArena b = data.register(ColosseumArenaGeometry.testFixture(CENTER.offset(500, 0, 0)), "structure|x|2");
        UUID trial = UUID.randomUUID(), player = UUID.randomUUID();
        a.state().ledger().addWave(WaveRecord.cleared(trial, 1, 1, List.of(player), List.of(player), 5L, KeyState.PENDING));
        a.state().ledger().addEligibility(new RewardLedger.EligibilityRecord(trial, 1, player, RewardLedger.BASIS_Q114_C, 5L));
        a.state().ledger().release(trial);
        a.state().bossVanish.add(new BossVanish(trial, UUID.randomUUID(), 9L, 11L));
        a.state().journal().add(trial, 12L, new net.minecraft.nbt.CompoundTag(), null);
        data.archiveAndRemove(b);
        b.state().ledger().addWave(WaveRecord.cleared(trial, 1, 2, List.of(), List.of(), 1L, KeyState.NONE));
        JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        ColosseumArenaData loaded = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(json, CODEC.encodeStart(JsonOps.INSTANCE, loaded).getOrThrow());
        ColosseumArena la = loaded.arenaByInstance("structure|x|1");
        assertEquals(1, la.state().bossVanish().size());
        assertEquals(1, la.state().journal().entries().size());
        assertTrue(la.state().ledger().isReleased(trial));
        assertEquals(1, la.state().ledger().eligibility().size());
        assertEquals(KeyState.PENDING, la.state().ledger().wave(trial, 1).key().state());
    }

    @Test
    void dataWrittenByANewerBuildIsKeptVerbatimAndNeverModified() {
        JsonObject future = new JsonObject();
        future.addProperty("schema", 2);
        future.add("arenas", new JsonArray());
        JsonObject extra = new JsonObject();
        extra.addProperty("hello", "world");
        future.add("brand_new_section", extra);
        ColosseumArenaData data = CODEC.parse(JsonOps.INSTANCE, future).getOrThrow();
        assertTrue(data.readOnly());
        assertTrue(data.arenas().isEmpty(), "nothing is ticked");
        assertEquals(future, CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow(), "written back byte for byte");
        assertThrows(IllegalStateException.class, () -> data.register(ColosseumArenaGeometry.testFixture(CENTER), "x"));
    }

    @Test
    void dataWithoutAVersionOrWithAnUnknownOldOneIsRejectedRatherThanGuessed() {
        assertTrue(CODEC.parse(JsonOps.INSTANCE, new JsonObject()).error().isPresent());
        JsonObject zero = new JsonObject();
        zero.addProperty("schema", 0);
        assertTrue(CODEC.parse(JsonOps.INSTANCE, zero).error().isPresent());
    }

    @Test
    void archivingKeepsTheLedgerAndReRegisteringTheSameInstanceRestoresIt() {
        ColosseumArenaData data = new ColosseumArenaData();
        ColosseumArena arena = data.register(ColosseumArenaGeometry.testFixture(CENTER), "structure|x|1");
        UUID trial = UUID.randomUUID();
        arena.state().ledger().addWave(WaveRecord.cleared(trial, 1, 1, List.of(), List.of(), 1L, KeyState.NONE));
        data.archiveAndRemove(arena);
        assertTrue(data.arenas().isEmpty());
        assertEquals(1, data.archived().get("structure|x|1").waves().size());
        ColosseumArena again = data.register(ColosseumArenaGeometry.testFixture(CENTER), "structure|x|1");
        assertTrue(again.state().waveCleared(trial, 1), "the history comes back with the arena");
        assertTrue(data.archived().isEmpty());
        assertTrue(data.register(ColosseumArenaGeometry.testFixture(CENTER), "other-id") == again, "idempotent by center");
    }

    @Test
    void anArenaWithAnEmptyLedgerLeavesNothingInTheArchive() {
        ColosseumArenaData data = new ColosseumArenaData();
        ColosseumArena arena = data.register(ColosseumArenaGeometry.testFixture(CENTER), "structure|x|1");
        data.archiveAndRemove(arena);
        assertTrue(data.archived().isEmpty());
    }

    @Test
    void structureMaskCoverageAndPlayerModificationsSurviveSaveAndRestart() {
        ColosseumArenaData data = new ColosseumArenaData();
        ColosseumArena arena = data.register(ColosseumArenaGeometry.testFixture(CENTER), "structure|x|mask");
        BlockPos original = CENTER.offset(2, 0, 0);
        BlockPos playerPlaced = CENTER.offset(3, 0, 0);
        BlockPos externalNatural = CENTER.offset(4, 0, 0);
        arena.structureMask().recordStructure(List.of(original));
        assertTrue(arena.structureMask().protect(playerPlaced));

        JsonElement saved = CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        ColosseumArena restored = CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().arena(CENTER.asLong());
        assertTrue(restored.structureMask().complete());
        assertTrue(restored.structureMask().protects(original));
        assertTrue(restored.structureMask().protects(playerPlaced));
        assertFalse(restored.structureMask().protects(externalNatural),
                "external natural blocks remain distinguishable from recorded structure/player positions");

        ColosseumArena legacy = new ColosseumArena("legacy", ColosseumArenaGeometry.testFixture(CENTER), new ColosseumTrialState());
        JsonElement oldFormat = ColosseumArena.CODEC.encodeStart(JsonOps.INSTANCE, legacy).getOrThrow();
        oldFormat.getAsJsonObject().remove("structure_mask");
        ColosseumArena decodedLegacy = ColosseumArena.CODEC.parse(JsonOps.INSTANCE, oldFormat).getOrThrow();
        assertFalse(decodedLegacy.structureMask().complete(), "old save data loads but cannot claim missing provenance");
    }

    @Test
    void ownershipTagsRoundTripAndLegacyTagsStillDecode() {
        UUID trial = UUID.randomUUID();
        ColosseumOwnership.Tag boss = ColosseumOwnership.Tag.of("structure|x|1", trial, 7, Kind.BOSS, ColosseumOwnership.BOSS_ALLOCATION);
        JsonElement json = ColosseumOwnership.Tag.CODEC.encodeStart(JsonOps.INSTANCE, boss).getOrThrow();
        assertEquals(boss, ColosseumOwnership.Tag.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertFalse(json.getAsJsonObject().has("arena"), "new tags do not carry the legacy field");

        JsonObject legacy = new JsonObject();
        legacy.addProperty("arena", 123456789L);
        legacy.add("trial", uuid(trial));
        legacy.addProperty("allocation", 4);
        ColosseumOwnership.Tag old = ColosseumOwnership.Tag.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        assertEquals("", old.instance());
        assertEquals(123456789L, old.legacyArena());
        assertEquals(Kind.MOB, old.kind(), "a pre-kind tag is a plain mob");
        assertEquals(0, old.round());
        assertEquals(4, old.allocation());

        ColosseumOwnership.KeyTag key = new ColosseumOwnership.KeyTag("structure|x|1", trial, 3);
        assertEquals(key, ColosseumOwnership.KeyTag.CODEC.parse(JsonOps.INSTANCE,
                ColosseumOwnership.KeyTag.CODEC.encodeStart(JsonOps.INSTANCE, key).getOrThrow()).getOrThrow());
        assertNull(null);
    }
}
