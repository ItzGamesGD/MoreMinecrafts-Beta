package dev.itzgamesgd.moreminecrafts.progression;

import java.util.UUID;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PickaxeDamageSnapshotTrackerTest {
    @Test void canceledBreakDiscardsPendingSnapshot() {
        var tracker = new PickaxeDamageSnapshotTracker();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        tracker.capture(player, Items.IRON_PICKAXE, 12, 250);
        tracker.capture(otherPlayer, Items.DIAMOND_PICKAXE, 42, 1561);

        tracker.cancelBreak(player);

        assertNull(tracker.complete(player));
        assertNotNull(tracker.complete(otherPlayer));
        assertEquals(0, tracker.pendingCount());
    }

    @Test void disconnectDiscardsPendingSnapshotWithoutACompletionEvent() {
        var tracker = new PickaxeDamageSnapshotTracker();
        UUID player = UUID.randomUUID();
        tracker.capture(player, Items.DIAMOND_PICKAXE, 42, 1561);

        tracker.playerDisconnected(player);

        assertNull(tracker.complete(player));
        assertEquals(0, tracker.pendingCount());
    }

    @Test void successfulBreakStillConsumesItsSnapshotExactlyOnce() {
        var tracker = new PickaxeDamageSnapshotTracker();
        UUID player = UUID.randomUUID();
        tracker.capture(player, Items.IRON_PICKAXE, 12, 250);

        var snapshot = tracker.complete(player);

        assertNotNull(snapshot);
        assertSame(Items.IRON_PICKAXE, snapshot.item());
        assertEquals(12, snapshot.damage());
        assertEquals(250, snapshot.maximumDamage());
        assertNull(tracker.complete(player));
        assertEquals(0, tracker.pendingCount());
    }
}
