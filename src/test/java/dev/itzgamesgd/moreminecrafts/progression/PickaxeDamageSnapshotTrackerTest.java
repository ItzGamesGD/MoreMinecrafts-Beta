package dev.itzgamesgd.moreminecrafts.progression;

import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PickaxeDamageSnapshotTrackerTest {
    @BeforeAll static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void nestedScopesCompleteInLastInFirstOutOrder() {
        var tracker = new PickaxeDamageSnapshotTracker();
        UUID player = UUID.randomUUID();
        var outer = tracker.begin(player, Items.IRON_PICKAXE, 10, 250);
        var inner = tracker.begin(player, Items.IRON_PICKAXE, 14, 250);

        assertEquals(2, tracker.pendingCount());
        assertEquals(14, inner.complete().damage(), "inner break completes its own frame");
        assertEquals(1, tracker.pendingCount());
        assertEquals(10, outer.complete().damage(), "outer frame survives nested completion");
        assertNull(outer.complete(), "a frame can only be consumed once");
        assertEquals(0, tracker.pendingCount());
    }

    @Test void canceledNestedScopeOnlyDiscardsItsOwnFrame() {
        var tracker = new PickaxeDamageSnapshotTracker();
        UUID player = UUID.randomUUID();
        var outer = tracker.begin(player, Items.IRON_PICKAXE, 10, 250);
        try (var ignored = tracker.begin(player, Items.IRON_PICKAXE, 14, 250)) {
            assertEquals(2, tracker.pendingCount());
        }

        assertEquals(1, tracker.pendingCount());
        assertEquals(10, outer.complete().damage());
        assertEquals(0, tracker.pendingCount());
    }

    @Test void exceptionSafeCloseRemovesOnlyTheActiveBreakFrame() {
        var tracker = new PickaxeDamageSnapshotTracker();
        UUID player = UUID.randomUUID();
        var outer = tracker.begin(player, Items.IRON_PICKAXE, 10, 250);

        assertThrows(IllegalStateException.class, () -> {
            try (var ignored = tracker.begin(player, Items.DIAMOND_PICKAXE, 42, 1561)) {
                throw new IllegalStateException("simulated nested break failure");
            }
        });

        assertEquals(1, tracker.pendingCount());
        assertSame(Items.IRON_PICKAXE, outer.complete().item());
        assertEquals(0, tracker.pendingCount());
    }

    @Test void scopesAreIsolatedPerPlayerAndDisconnectInvalidatesOutstandingFrames() {
        var tracker = new PickaxeDamageSnapshotTracker();
        UUID player = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        var first = tracker.begin(player, Items.IRON_PICKAXE, 12, 250);
        var other = tracker.begin(otherPlayer, Items.DIAMOND_PICKAXE, 42, 1561);

        assertEquals(12, first.complete().damage());
        assertEquals(42, other.complete().damage());
        assertEquals(0, tracker.pendingCount());

        var disconnected = tracker.begin(player, Items.IRON_PICKAXE, 7, 250);
        tracker.playerDisconnected(player);
        assertNull(disconnected.complete(), "disconnect invalidates a frame before any later completion");
        assertEquals(0, tracker.pendingCount());
    }

    @Test void nonPickaxeOperationDoesNotCreateAFrame() {
        var tracker = new PickaxeDamageSnapshotTracker();
        try (var scope = tracker.begin(UUID.randomUUID(), Items.STICK, 0, 0)) {
            assertNull(scope.complete());
        }
        assertEquals(0, tracker.pendingCount());
    }
}
