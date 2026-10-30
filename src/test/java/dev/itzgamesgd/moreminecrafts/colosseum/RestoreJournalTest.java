package dev.itzgamesgd.moreminecrafts.colosseum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.itzgamesgd.moreminecrafts.colosseum.RestoreJournal.AddResult;
import dev.itzgamesgd.moreminecrafts.colosseum.RestoreJournal.Status;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/**
 * Q91 / Q117 / Q118-A at contract level: there is no boss that breaks blocks yet, so these drive the journal and its
 * restore pass directly. They prove the rules; they do not claim a live boss was observed breaking and restoring terrain.
 */
class RestoreJournalTest {
    private final Scenario s = new Scenario();

    private static CompoundTag block(String name) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        return tag;
    }

    private static final CompoundTag STONE = block("minecraft:stone");
    private static final CompoundTag PLANKS = block("minecraft:oak_planks");
    private static final CompoundTag WATER = block("minecraft:water");

    /** One active trial with entries at positions 1..n, all broken (the fake world holds air there). */
    private UUID trialWith(int entries) {
        s.start(1);
        UUID trial = s.state.trialId();
        for (long pos = 1; pos <= entries; pos++) s.state.journal().add(trial, pos, STONE, null);
        return trial;
    }

    private void endTrial(String how) {
        switch (how) {
            case "reset" -> ColosseumController.forceReset(s.state, s.world);
            case "timeout" -> {
                s.state.trialRemaining = 1;
                s.world.tick(s.state, 1);
            }
            default -> {
                s.world.bossAvailable = true;
                s.toBoss();
                // the boss' own journal entries belong to the same trial; success is reported by its handler
                assertTrue(ColosseumController.onBossDefeated(s.state, s.world, s.state.trialId()));
            }
        }
    }

    @Test
    void theActiveTrialsTerrainStaysBrokenAndIsRestoredWhenEveryKindOfEndingArrives() {
        for (String how : new String[]{"reset", "timeout", "success"}) {
            Scenario t = new Scenario();
            UUID p = t.start(1);
            UUID trial = t.state.trialId();
            for (long pos = 1; pos <= 3; pos++) t.state.journal().add(trial, pos, STONE, null);
            t.world.tick(t.state, 100);
            assertTrue(t.world.blocks.isEmpty(), how + ": nothing is restored while the trial is active");
            assertEquals(3, t.state.journal().entries().size());
            if (how.equals("reset")) ColosseumController.forceReset(t.state, t.world);
            else if (how.equals("timeout")) {
                t.state.trialRemaining = 1;
                t.world.tick(t.state, 1);
            } else {
                t.world.bossAvailable = true;
                t.clearThrough(6);
                t.world.tick(t.state, t.state.intermissionRemaining());
                t.world.tick(t.state, 2);
                assertTrue(ColosseumController.onBossDefeated(t.state, t.world, t.state.trialId()));
            }
            assertTrue(t.state.hasOpenWork(), how + ": the ending itself restores nothing");
            t.world.tick(t.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
            assertEquals(STONE, t.world.blocks.get(1L), how);
            assertEquals(STONE, t.world.blocks.get(3L), how);
            assertTrue(t.state.journal().isEmpty(), how + ": restored entries leave the journal");
            assertFalse(t.state.hasOpenWork());
            assertNotNull(p);
        }
    }

    @Test
    void q118aO13ConflictIsTerminalRetainedAndNeverReevaluated() {
        trialWith(3);
        endTrial("reset");
        s.world.blocks.put(2L, PLANKS); // a player built here after the boss broke the block
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertEquals(STONE, s.world.blocks.get(1L));
        assertEquals(STONE, s.world.blocks.get(3L));
        assertEquals(PLANKS, s.world.blocks.get(2L), "Q118-A: the player's building is untouched");
        assertEquals(1, s.state.journal().entries().size());
        RestoreJournal.Entry conflict = s.state.journal().at(2L);
        assertEquals(Status.SKIPPED_CONFLICT, conflict.status());
        assertEquals(PLANKS, conflict.found(), "the foreign block is recorded");
        assertTrue(conflict.conflictGameTime() > 0);
        assertEquals(1, s.world.restoreAttempts.get(2L));
        s.world.tick(s.state, 200);
        assertEquals(PLANKS, s.world.blocks.get(2L), "later passes still never overwrite it");
        assertEquals(Status.SKIPPED_CONFLICT, s.state.journal().at(2L).status());
        assertFalse(s.state.hasOpenWork(), "a terminal conflict is not open work");
        // Even if the player later removes the block, O-13 prevents a new restore attempt.
        s.world.blocks.remove(2L);
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertNull(s.world.blocks.get(2L));
        assertEquals(1, s.world.restoreAttempts.get(2L));
        assertEquals(1, s.state.journal().entries().size(), "the terminal conflict remains in history");
    }

    @Test
    void aPositionThatAlreadyHoldsThePriorBlockIsJustClosed() {
        trialWith(1);
        endTrial("timeout");
        s.world.blocks.put(1L, STONE);
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertTrue(s.state.journal().isEmpty());
        assertEquals(STONE, s.world.blocks.get(1L));
    }

    @Test
    void unloadedPositionsWaitAndAreRestoredWhenTheChunkLoads() {
        trialWith(2);
        endTrial("reset");
        s.world.unloadedBlocks.add(1L);
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertEquals(STONE, s.world.blocks.get(2L));
        assertNull(s.world.blocks.get(1L), "an unloaded position is not touched");
        assertEquals(1, s.state.journal().entries().size());
        assertEquals(Status.PENDING, s.state.journal().at(1L).status(), "waiting is not a conflict");
        s.world.tick(s.state, 300);
        assertEquals(1, s.state.journal().entries().size());
        s.world.unloadedBlocks.clear(); // the chunk reloads
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertEquals(STONE, s.world.blocks.get(1L));
        assertTrue(s.state.journal().isEmpty());
    }

    @Test
    void aRestartMidRestoreKeepsTheRemainingEntriesAndTheRecordedConflict() {
        trialWith(5);
        endTrial("reset");
        s.world.unloadedBlocks.add(4L);
        s.world.unloadedBlocks.add(5L);
        s.world.blocks.put(3L, PLANKS);
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertEquals(STONE, s.world.blocks.get(1L));
        assertEquals(STONE, s.world.blocks.get(2L));
        assertEquals(3, s.state.journal().entries().size());
        // Server restart.
        ColosseumTrialState restarted = TestArenas.roundTrip(s.state);
        assertEquals(3, restarted.journal().entries().size());
        assertEquals(Status.SKIPPED_CONFLICT, restarted.journal().at(3L).status());
        assertEquals(PLANKS, restarted.journal().at(3L).found());
        FakeColosseumWorld after = new FakeColosseumWorld();
        after.blocks.putAll(s.world.blocks);
        after.blocks.remove(3L);
        after.tick(restarted, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertEquals(STONE, after.blocks.get(4L));
        assertEquals(STONE, after.blocks.get(5L));
        assertNull(after.blocks.get(3L), "a conflict remains skipped after restart even if the position is now empty");
        assertEquals(0, after.restoreAttempts.getOrDefault(3L, 0));
        assertEquals(1, restarted.journal().entries().size(), "only the conflict is left");
        assertEquals(Status.SKIPPED_CONFLICT, restarted.journal().at(3L).status());
        assertFalse(restarted.hasOpenWork(), "the terminal conflict does not block arena removal");
        assertEquals(STONE, after.blocks.get(1L), "already-restored blocks stay as they were");
    }

    @Test
    void aPassHandlesAtMostTheBatchLimit() {
        trialWith(100);
        endTrial("reset");
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertEquals(100 - ColosseumRules.JOURNAL_BATCH, s.state.journal().entries().size());
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertTrue(s.state.journal().isEmpty());
        assertEquals(100, s.world.blocks.size());
    }

    @Test
    void oneOpenEntryPerPositionKeepsTheStateBeforeTheFirstBreak() {
        s.start(1);
        UUID trial = s.state.trialId();
        assertEquals(AddResult.RECORDED, s.state.journal().add(trial, 7L, STONE, null));
        assertEquals(AddResult.ALREADY_RECORDED, s.state.journal().add(trial, 7L, PLANKS, WATER),
                "a second break of the same spot keeps the original prior state");
        assertEquals(1, s.state.journal().entries().size());
        assertEquals(STONE, s.state.journal().at(7L).prior());
        assertEquals(WATER, s.state.journal().at(7L).left(), "only the left state is refreshed");
    }

    @Test
    void anExplicitLeftStateIsTheOnlyThingCountedAsEmpty() {
        trialWith(0);
        UUID trial = s.state.trialId();
        s.state.journal().add(trial, 1L, STONE, WATER);
        s.state.journal().add(trial, 2L, STONE, WATER);
        endTrial("reset");
        s.world.blocks.put(1L, WATER); // exactly what the boss left
        // position 2 holds nothing (air), which is not the recorded left state
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertEquals(STONE, s.world.blocks.get(1L));
        assertEquals(Status.SKIPPED_CONFLICT, s.state.journal().at(2L).status());
    }

    @Test
    void theJournalCodecKeepsEveryFieldIncludingAConflict() {
        trialWith(2);
        s.state.journal().add(s.state.trialId(), 3L, STONE, WATER);
        RestoreJournal.Entry e = s.state.journal().at(1L);
        e.markSkippedConflict(77L, PLANKS);
        var json = RestoreJournal.CODEC.encodeStart(JsonOps.INSTANCE, s.state.journal()).getOrThrow();
        RestoreJournal loaded = RestoreJournal.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(3, loaded.entries().size());
        assertEquals(Status.SKIPPED_CONFLICT, loaded.at(1L).status());
        assertEquals(77L, loaded.at(1L).conflictGameTime());
        assertEquals(PLANKS, loaded.at(1L).found());
        assertEquals(WATER, loaded.at(3L).left());
        assertEquals(STONE, loaded.at(2L).prior());
    }

    @Test
    void legacyConflictStatusLoadsAsTerminalAndDoesNotBreakOldJournalData() {
        trialWith(1);
        RestoreJournal.Entry entry = s.state.journal().at(1L);
        entry.markSkippedConflict(51L, PLANKS);
        var encoded = RestoreJournal.CODEC.encodeStart(JsonOps.INSTANCE, s.state.journal()).getOrThrow();
        encoded.getAsJsonArray().get(0).getAsJsonObject().addProperty("status", "CONFLICT");
        RestoreJournal loaded = RestoreJournal.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(Status.CONFLICT, loaded.at(1L).status(), "pre-O-13 saved values remain readable");
        assertTrue(loaded.at(1L).terminalConflict(), "the legacy conflict is treated as terminal");
        assertFalse(TestArenas.roundTrip(s.state).hasPendingWork());
    }

    @Test
    void openWorkBlocksRemovalUntilItIsDone() {
        trialWith(1);
        assertTrue(s.state.hasOpenWork());
        endTrial("reset");
        assertTrue(s.state.hasOpenWork());
        s.world.tick(s.state, ColosseumRules.JOURNAL_PERIOD_TICKS);
        assertFalse(s.state.hasOpenWork());
    }
}
