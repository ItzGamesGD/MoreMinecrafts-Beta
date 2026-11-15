package dev.itzgamesgd.moreminecrafts.progression;

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
 * World ledger of wither skeleton skull token redemptions.
 *
 * <p>Tracks tokens through PENDING -> COMMITTED states across server restarts.
 * Refuses any token that is PENDING or COMMITTED.
 */
public final class QuestWitherSkullLedger extends SavedData {
    public enum State { PENDING, COMMITTED }

    public record Entry(String token, State state, String tx, String player, int count) {
        static final Codec<State> STATE_CODEC = Codec.STRING.xmap(State::valueOf, State::name);
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("token").forGetter(Entry::token),
                STATE_CODEC.fieldOf("state").forGetter(Entry::state),
                Codec.STRING.optionalFieldOf("tx", "").forGetter(Entry::tx),
                Codec.STRING.optionalFieldOf("player", "").forGetter(Entry::player),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Entry::count)).apply(instance, Entry::new));
    }

    public static final Codec<QuestWitherSkullLedger> CODEC = Entry.CODEC.listOf()
            .xmap(QuestWitherSkullLedger::fromList, ledger -> new ArrayList<>(ledger.entries.values()));
    static final SavedDataType<QuestWitherSkullLedger> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:quest_wither_skulls"), QuestWitherSkullLedger::new, CODEC, DataFixTypes.LEVEL);

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Map<String, Entry> pending = new LinkedHashMap<>();

    public static QuestWitherSkullLedger of(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static void flush(ServerLevel level) {
        level.getServer().overworld().getDataStorage().saveAndJoin();
    }

    private static QuestWitherSkullLedger fromList(List<Entry> list) {
        QuestWitherSkullLedger ledger = new QuestWitherSkullLedger();
        for (Entry entry : list) {
            ledger.entries.put(entry.token(), entry);
            if (entry.state() == State.PENDING) ledger.pending.put(entry.token(), entry);
        }
        return ledger;
    }

    public Entry get(String token) { return entries.get(token); }

    public boolean blocked(String token) { return entries.containsKey(token); }

    public boolean prepare(String token, String tx, String player, int count) {
        if (entries.containsKey(token)) return false;
        Entry entry = new Entry(token, State.PENDING, tx, player, count);
        entries.put(token, entry);
        pending.put(token, entry);
        setDirty();
        return true;
    }

    public boolean commit(String token, String tx) {
        Entry entry = entries.get(token);
        if (entry == null || !entry.tx().equals(tx)) return false;
        if (entry.state() == State.COMMITTED) return true;
        entries.put(token, new Entry(token, State.COMMITTED, tx, entry.player(), entry.count()));
        pending.remove(token);
        setDirty();
        return true;
    }

    public boolean abort(String token, String tx) {
        Entry entry = entries.get(token);
        if (entry == null || entry.state() != State.PENDING || !entry.tx().equals(tx)) return false;
        entries.remove(token);
        pending.remove(token);
        setDirty();
        return true;
    }

    public boolean heal(String token, String tx, String player, int count) {
        if (entries.containsKey(token)) return false;
        entries.put(token, new Entry(token, State.COMMITTED, tx, player, count));
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
