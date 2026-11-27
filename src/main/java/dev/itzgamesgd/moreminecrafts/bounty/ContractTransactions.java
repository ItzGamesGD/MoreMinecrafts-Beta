package dev.itzgamesgd.moreminecrafts.bounty;

import dev.itzgamesgd.moreminecrafts.mixin.PlayerListAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Durable contract redemption.
 *
 * <p>Vanilla gives no transaction across the world ledger file, the player file, and chunk entity storage, so the
 * guarantee is built from ordering and one commit point:
 * <ol>
 *   <li><b>Prepare</b>: a {@code PENDING} ledger entry is written and flushed before any inventory change. A crash here
 *       or later can never leave a redeemable duplicate.</li>
 *   <li><b>Apply</b>: delivery items and the contract are consumed, rewards inserted, and a receipt is added to the
 *       player's data. Inventory and receipt are one file, so they are atomic with each other.</li>
 *   <li><b>Commit</b>: the player file is saved synchronously. This is the commit point.</li>
 *   <li><b>Overflow</b>: rewards that did not fit are dropped only after the commit, then the receipt is marked delivered.</li>
 *   <li><b>Finalize</b>: the ledger entry becomes {@code COMMITTED} and the receipt is cleared.</li>
 * </ol>
 * A crash leaves the player file either before step 3 (no receipt: roll back) or after it (receipt: roll forward).
 * {@link #recover} decides from the player's own receipt, so recovery is idempotent and needs no clock.
 *
 * <p>Residual limitation: overflow entities live in chunk storage, a third non-atomic domain. See the project record.
 */
public final class ContractTransactions {
    /** Boundaries at which tests may interrupt the process. Production leaves the hook as a no-op. */
    public enum Point {
        BEFORE_PREPARE, AFTER_PREPARE, AFTER_CONSUME, AFTER_REWARD, AFTER_PLAYER_SAVE,
        AFTER_OVERFLOW_SPAWN, AFTER_OVERFLOW_FLUSH, AFTER_OVERFLOW_DROP, AFTER_LEDGER_COMMIT
    }

    private static volatile Consumer<Point> faultHook = point -> { };

    /** Flushes chunk and entity storage; only tests that do not assess durability replace it. */
    private static volatile Consumer<ServerLevel> entityFlush = level -> level.save(null, true, false);

    private ContractTransactions() {}

    /** Test seam only. */
    public static void setFaultHook(Consumer<Point> hook) { faultHook = hook == null ? point -> { } : hook; }

    /** Test seam only: lets in-process suites skip a full level save that would disturb unrelated timed tests. */
    public static void setEntityFlush(Consumer<ServerLevel> flush) {
        entityFlush = flush == null ? level -> level.save(null, true, false) : flush;
    }

    private static void fault(Point point) { faultHook.accept(point); }

    private static void savePlayer(ServerPlayer player) {
        ((PlayerListAccessor) player.level().getServer().getPlayerList()).moreminecrafts$save(player);
    }

    /** Caller has validated the contract, the live inventory, the reward item, and that the ID is not in the ledger. */
    static boolean redeem(ServerPlayer player, ItemStack contractStack, ContractData data, Item bonus) {
        ServerLevel level = (ServerLevel) player.level();
        ContractLedger ledger = ContractLedger.of(level);
        fault(Point.BEFORE_PREPARE);

        String tx = UUID.randomUUID().toString();
        if (!ledger.prepare(data.id(), tx, player.getStringUUID())) return false;
        try {
            ContractLedger.flush(level);
        } catch (RuntimeException exception) {
            ledger.abort(data.id(), tx);
            throw exception;
        }
        fault(Point.AFTER_PREPARE);

        Inventory inventory = player.getInventory();
        for (var need : data.deliveryNeeds().entrySet()) {
            int remaining = need.getValue();
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack material = inventory.getItem(slot);
                if (material.isEmpty() || !BuiltInRegistries.ITEM.getKey(material.getItem()).toString().equals(need.getKey())) continue;
                int taken = Math.min(remaining, material.getCount());
                material.shrink(taken);
                remaining -= taken;
            }
        }
        contractStack.shrink(1);
        fault(Point.AFTER_CONSUME);

        List<ItemStack> overflow = new ArrayList<>();
        insert(inventory, new ItemStack(Items.EMERALD, data.reward().emeralds()), overflow);
        insert(inventory, new ItemStack(bonus, data.reward().count()), overflow);
        inventory.setChanged();
        putReceipts(player, receipts(player).with(
                new ContractAttachments.Receipt(tx, data.id(), List.copyOf(overflow), overflow.isEmpty())));
        fault(Point.AFTER_REWARD);

        savePlayer(player);
        fault(Point.AFTER_PLAYER_SAVE);

        if (!overflow.isEmpty()) {
            deliverOverflow(player, tx, overflow);
            putReceipts(player, receipts(player).with(
                    new ContractAttachments.Receipt(tx, data.id(), List.copyOf(overflow), true)));
            savePlayer(player);
        }
        fault(Point.AFTER_OVERFLOW_DROP);

        ledger.commit(data.id(), tx);
        ContractLedger.flush(level);
        putReceipts(player, receipts(player).without(tx));
        fault(Point.AFTER_LEDGER_COMMIT);
        return true;
    }

    /**
     * Inserts what fits and records the rest. The inserted amount is measured from the inventory, because
     * {@code Inventory.add} zeroes the stack for a creative player even when nothing was stored, which would silently
     * discard the reward instead of overflowing it.
     */
    private static void insert(Inventory inventory, ItemStack reward, List<ItemStack> overflow) {
        if (reward.isEmpty()) return;
        int before = inventory.countItem(reward.getItem());
        inventory.add(reward.copy());
        int remainder = reward.getCount() - (inventory.countItem(reward.getItem()) - before);
        if (remainder > 0) overflow.add(reward.copyWithCount(remainder));
    }

    /**
     * Drops overflow stacks, tagged with their transaction, then flushes chunk and entity storage so the drops are durable
     * before the receipt is allowed to say they were delivered.
     */
    private static void deliverOverflow(ServerPlayer player, String tx, List<ItemStack> overflow) {
        ServerLevel level = (ServerLevel) player.level();
        for (ItemStack stack : overflow) {
            ItemEntity entity = player.drop(stack.copy(), false, net.minecraft.util.Prediction.SERVER_ONLY);
            if (entity != null) entity.setAttached(ContractAttachments.DROP_TX, tx);
        }
        fault(Point.AFTER_OVERFLOW_SPAWN);
        // ServerChunkCache.save writes chunk blocks only; ServerLevel.save also flushes the entity sections that hold the drops.
        entityFlush.accept(level);
        fault(Point.AFTER_OVERFLOW_FLUSH);
    }

    private static ContractAttachments.Receipts receipts(ServerPlayer player) {
        ContractAttachments.Receipts receipts = player.getAttached(ContractAttachments.RECEIPTS);
        return receipts == null ? ContractAttachments.Receipts.empty() : receipts;
    }

    /** Stores the receipts, removing the attachment entirely when none remain. */
    private static void putReceipts(ServerPlayer player, ContractAttachments.Receipts receipts) {
        if (receipts.list().isEmpty()) player.removeAttached(ContractAttachments.RECEIPTS);
        else player.setAttached(ContractAttachments.RECEIPTS, receipts);
    }

    /**
     * Resolves every unfinished transaction this player owns, using only the player's own durable receipts.
     * Safe to call any number of times, including while the inventory is full.
     */
    public static void recover(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        ContractLedger ledger = ContractLedger.of(level);
        boolean ledgerChanged = false;

        for (ContractLedger.Entry entry : ledger.pendingFor(player.getStringUUID())) {
            ContractAttachments.Receipt receipt = receipts(player).find(entry.tx());
            if (receipt == null) {
                // The player's durable data predates the transaction: nothing was granted, the contract is intact.
                ledgerChanged |= ledger.abort(entry.id(), entry.tx());
                continue;
            }
            if (!receipt.delivered()) {
                // Entities of this chunk may not be loaded yet, which would make the duplicate check below blind.
                if (!level.isPositionEntityTicking(player.blockPosition())) continue;
                finishOverflow(player, receipt);
            }
            ledgerChanged |= ledger.commit(entry.id(), entry.tx());
            putReceipts(player, receipts(player).without(entry.tx()));
        }

        for (ContractAttachments.Receipt receipt : receipts(player).list()) {
            ContractLedger.Entry entry = ledger.get(receipt.contractId());
            if (entry == null) {
                // A receipt with no ledger entry cannot arise from the write order, but heal it defensively.
                if (!receipt.delivered()) {
                    if (!level.isPositionEntityTicking(player.blockPosition())) continue;
                    finishOverflow(player, receipt);
                }
                ledgerChanged |= ledger.heal(receipt.contractId(), receipt.tx(), player.getStringUUID());
            } else if (entry.state() == ContractLedger.State.PENDING && entry.tx().equals(receipt.tx())) {
                continue;
            }
            putReceipts(player, receipts(player).without(receipt.tx()));
        }
        if (ledgerChanged) ContractLedger.flush(level);
    }

    /** True while this player still owns an unresolved transaction or an uncleared receipt. */
    public static boolean needsRecovery(ServerPlayer player) {
        return !receipts(player).list().isEmpty()
                || !ContractLedger.of((ServerLevel) player.level()).pendingFor(player.getStringUUID()).isEmpty();
    }

    /** Delivers undelivered overflow once, skipping it if a tagged drop from the same transaction already exists. */
    private static void finishOverflow(ServerPlayer player, ContractAttachments.Receipt receipt) {
        ServerLevel level = (ServerLevel) player.level();
        boolean alreadyDropped = !level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(64.0),
                entity -> receipt.tx().equals(entity.getAttached(ContractAttachments.DROP_TX))).isEmpty();
        if (!alreadyDropped) deliverOverflow(player, receipt.tx(), receipt.overflow());
        putReceipts(player, receipts(player).with(receipt.asDelivered()));
    }
}
