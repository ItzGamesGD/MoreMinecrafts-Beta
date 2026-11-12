package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.mixin.PlayerListAccessor;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * P3 Protocol Transaction Handler for Wither Skeleton Skull pickups.
 *
 * <p>Guarantee (D-F1-1):
 * 1. PENDING ledger entry flushed before any inventory change or credit.
 * 2. Skull item transferred to inventory, credit counter updated, and durable receipt attached in one player snapshot.
 * 3. Synchronous player save (Commit Point).
 * 4. COMMITTED ledger entry flushed, receipt cleared.
 *
 * <p>Recovery resolves PENDING entries using the player's own durable receipts:
 * - Receipt present: rolls forward to COMMITTED, clears receipt.
 * - Receipt absent: player data predates transaction -> rolls back (aborts PENDING entry).
 */
public final class QuestWitherSkullTransactions {
    public enum Point {
        BEFORE_PREPARE,
        AFTER_PREPARE,
        AFTER_ADD,
        AFTER_PLAYER_SAVE,
        AFTER_LEDGER_COMMIT
    }

    private static volatile Consumer<Point> faultHook = point -> { };

    private QuestWitherSkullTransactions() {}

    public static void setFaultHook(Consumer<Point> hook) {
        faultHook = hook == null ? point -> { } : hook;
    }

    private static void fault(Point point) {
        faultHook.accept(point);
    }

    private static void savePlayer(ServerPlayer player) {
        ((PlayerListAccessor) player.level().getServer().getPlayerList()).moreminecrafts$save(player);
    }

    private static QuestWitherSkullAttachments.Receipts receipts(ServerPlayer player) {
        QuestWitherSkullAttachments.Receipts receipts = player.getAttached(QuestWitherSkullAttachments.RECEIPTS);
        return receipts == null ? QuestWitherSkullAttachments.Receipts.empty() : receipts;
    }

    private static void putReceipts(ServerPlayer player, QuestWitherSkullAttachments.Receipts receipts) {
        if (receipts.list().isEmpty()) player.removeAttached(QuestWitherSkullAttachments.RECEIPTS);
        else player.setAttached(QuestWitherSkullAttachments.RECEIPTS, receipts);
    }

    /**
     * Executes the P3 transaction for a genuine skull pickup.
     * Called from QuestWitherSkullPickupMixin.
     */
    public static boolean pickup(ServerPlayer player, ItemEntity item, ItemStack identity, int offered,
                                  java.util.function.Supplier<Integer> doAdd) {
        String token = QuestWitherSkullProvenance.getToken(item);
        if (token == null) return false;

        ServerLevel level = (ServerLevel) player.level();
        QuestWitherSkullLedger ledger = QuestWitherSkullLedger.of(level);

        fault(Point.BEFORE_PREPARE);

        String tx = UUID.randomUUID().toString();
        // Prepare PENDING in ledger and synchronously flush
        if (!ledger.prepare(token, tx, player.getStringUUID(), offered)) {
            return false;
        }
        try {
            QuestWitherSkullLedger.flush(level);
        } catch (RuntimeException exception) {
            ledger.abort(token, tx);
            throw exception;
        }

        fault(Point.AFTER_PREPARE);

        // Execute inventory insertion
        int received = doAdd.get();
        if (received <= 0) {
            // Nothing was added (e.g. inventory full) -> abort transaction
            ledger.abort(token, tx);
            QuestWitherSkullLedger.flush(level);
            return false;
        }

        // Token consumed on entity
        if (item.getItem().isEmpty()) {
            QuestWitherSkullProvenance.consumeToken(item);
        }

        // Record credit and durable receipt in player data
        XpQuestTracking.addCounter(player, QuestWitherSkullProvenance.DROPS, received);
        XpQuestTracking.reconcile(player);

        putReceipts(player, receipts(player).with(new QuestWitherSkullAttachments.Receipt(tx, token, received)));

        fault(Point.AFTER_ADD);

        // Synchronous player save -> Commit Point
        savePlayer(player);

        fault(Point.AFTER_PLAYER_SAVE);

        // Commit in ledger and flush
        ledger.commit(token, tx);
        QuestWitherSkullLedger.flush(level);

        // Clear receipt and save player if needed (or cleared in-memory, cleaned up on next save)
        putReceipts(player, receipts(player).without(tx));

        fault(Point.AFTER_LEDGER_COMMIT);
        return true;
    }

    /**
     * Recovers any unresolved PENDING transactions for the given player.
     */
    public static void recover(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        QuestWitherSkullLedger ledger = QuestWitherSkullLedger.of(level);
        boolean ledgerChanged = false;

        for (QuestWitherSkullLedger.Entry entry : ledger.pendingFor(player.getStringUUID())) {
            QuestWitherSkullAttachments.Receipt receipt = receipts(player).find(entry.tx());
            if (receipt == null) {
                // Player file predates transaction: roll back
                ledgerChanged |= ledger.abort(entry.token(), entry.tx());
            } else {
                // Player file has receipt: roll forward
                ledgerChanged |= ledger.commit(entry.token(), entry.tx());
                putReceipts(player, receipts(player).without(entry.tx()));
            }
        }

        for (QuestWitherSkullAttachments.Receipt receipt : receipts(player).list()) {
            QuestWitherSkullLedger.Entry entry = ledger.get(receipt.token());
            if (entry == null) {
                // Receipt without ledger entry (defensive heal)
                ledgerChanged |= ledger.heal(receipt.token(), receipt.tx(), player.getStringUUID(), receipt.count());
            } else if (entry.state() == QuestWitherSkullLedger.State.PENDING && entry.tx().equals(receipt.tx())) {
                continue;
            }
            putReceipts(player, receipts(player).without(receipt.tx()));
        }

        if (ledgerChanged) {
            QuestWitherSkullLedger.flush(level);
        }
    }

    public static boolean needsRecovery(ServerPlayer player) {
        return !receipts(player).list().isEmpty()
                || !QuestWitherSkullLedger.of((ServerLevel) player.level()).pendingFor(player.getStringUUID()).isEmpty();
    }
}
