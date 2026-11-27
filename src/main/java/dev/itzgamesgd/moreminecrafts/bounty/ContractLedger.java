package dev.itzgamesgd.moreminecrafts.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World ledger of contract redemptions, a two-phase index over the per-player receipts.
 *
 * <p>An entry is {@code PENDING} between the write-ahead flush and the moment the redeeming player's data is durable,
 * then {@code COMMITTED} forever. A pending entry blocks every copy of the contract. The redeeming player's own saved
 * receipt, not this file, decides whether a pending entry rolls forward or back (see {@link ContractTransactions}).
 */
public final class ContractLedger extends SavedData {
    public enum State { PENDING, COMMITTED }

    public record Entry(String id, State state, String tx, String player) {
        static final Codec<State> STATE_CODEC = Codec.STRING.xmap(State::valueOf, State::name);
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Entry::id),
                STATE_CODEC.fieldOf("state").forGetter(Entry::state),
                Codec.STRING.optionalFieldOf("tx", "").forGetter(Entry::tx),
                Codec.STRING.optionalFieldOf("player", "").forGetter(Entry::player)).apply(instance, Entry::new));
        /** Earlier builds stored a bare list of redeemed IDs; those are committed redemptions. */
        static final Codec<Entry> LEGACY = Codec.STRING.xmap(id -> new Entry(id, State.COMMITTED, "", ""), Entry::id);
    }

    public static final Codec<ContractLedger> CODEC = Codec.withAlternative(Entry.CODEC, Entry.LEGACY).listOf()
            .xmap(ContractLedger::fromList, ledger -> new ArrayList<>(ledger.entries.values()));
    static final SavedDataType<ContractLedger> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:contract_redemptions"), ContractLedger::new, CODEC, DataFixTypes.LEVEL);

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    /** Unfinished transactions only, so per-tick recovery checks never scan every redemption ever made. */
    private final Map<String, Entry> pending = new LinkedHashMap<>();

    public static ContractLedger of(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Synchronously writes every dirty world data file, including this ledger. */
    public static void flush(ServerLevel level) {
        level.getServer().overworld().getDataStorage().saveAndJoin();
    }

    private static ContractLedger fromList(List<Entry> list) {
        ContractLedger ledger = new ContractLedger();
        for (Entry entry : list) {
            ledger.entries.put(entry.id(), entry);
            if (entry.state() == State.PENDING) ledger.pending.put(entry.id(), entry);
        }
        return ledger;
    }

    public Entry get(String id) { return entries.get(id); }

    /** True when the ID is pending or committed, which refuses redemption and abandonment of every copy. */
    public boolean blocked(String id) { return entries.containsKey(id); }

    public boolean prepare(String id, String tx, String player) {
        if (entries.containsKey(id)) return false;
        Entry entry = new Entry(id, State.PENDING, tx, player);
        entries.put(id, entry);
        pending.put(id, entry);
        setDirty();
        return true;
    }

    public boolean commit(String id, String tx) {
        Entry entry = entries.get(id);
        if (entry == null || !entry.tx().equals(tx)) return false;
        if (entry.state() == State.COMMITTED) return true;
        entries.put(id, new Entry(id, State.COMMITTED, tx, entry.player()));
        pending.remove(id);
        setDirty();
        return true;
    }

    /** Removes a pending entry that never reached the player's durable data. Committed entries are never removed. */
    public boolean abort(String id, String tx) {
        Entry entry = entries.get(id);
        if (entry == null || entry.state() != State.PENDING || !entry.tx().equals(tx)) return false;
        entries.remove(id);
        pending.remove(id);
        setDirty();
        return true;
    }

    /** Records a committed redemption found only in a player receipt. Never overwrites an existing entry. */
    boolean heal(String id, String tx, String player) {
        if (entries.containsKey(id)) return false;
        entries.put(id, new Entry(id, State.COMMITTED, tx, player));
        setDirty();
        return true;
    }

    public List<Entry> pendingFor(String player) {
        List<Entry> owned = new ArrayList<>();
        for (Entry entry : pending.values())
            if (entry.player().equals(player)) owned.add(entry);
        return owned;
    }

    public int size() { return entries.size(); }
}
