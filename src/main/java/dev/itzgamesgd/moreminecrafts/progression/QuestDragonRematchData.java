package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Dragon-rematch bookkeeping that must outlive a restart: which player holds kill credit for each dying
 * dragon (the 200-tick death animation can span a save), and rematch credits owed to players who were offline
 * when the fight completed.
 */
final class QuestDragonRematchData extends SavedData {
    record Candidate(UUID dragon, UUID player) {
        static final Codec<Candidate> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("dragon").forGetter(Candidate::dragon),
                UUIDUtil.CODEC.fieldOf("player").forGetter(Candidate::player)).apply(i, Candidate::new));
    }

    record Saved(List<Candidate> candidates, List<UUID> pending) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
                Candidate.CODEC.listOf().optionalFieldOf("candidates", List.of()).forGetter(Saved::candidates),
                UUIDUtil.CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(Saved::pending)).apply(i, Saved::new));
    }

    private static final int LIMIT = 256;
    static final SavedDataType<QuestDragonRematchData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:quest_dragon_rematch"), QuestDragonRematchData::new,
            Saved.CODEC.xmap(QuestDragonRematchData::fromSaved, QuestDragonRematchData::toSaved), DataFixTypes.LEVEL);

    private final Map<UUID, UUID> candidates = new LinkedHashMap<>();
    private final Set<UUID> pending = new LinkedHashSet<>();

    static QuestDragonRematchData fromSaved(Saved saved) {
        QuestDragonRematchData data = new QuestDragonRematchData();
        saved.candidates().forEach(c -> data.candidates.put(c.dragon(), c.player()));
        data.pending.addAll(saved.pending());
        return data;
    }

    Saved toSaved() {
        List<Candidate> list = new ArrayList<>();
        candidates.forEach((dragon, player) -> list.add(new Candidate(dragon, player)));
        return new Saved(list, new ArrayList<>(pending));
    }

    void putCandidate(UUID dragon, UUID player) {
        candidates.put(dragon, player);
        while (candidates.size() > LIMIT) candidates.remove(candidates.keySet().iterator().next());
        setDirty();
    }

    UUID takeCandidate(UUID dragon) {
        UUID player = candidates.remove(dragon);
        if (player != null) setDirty();
        return player;
    }

    UUID candidate(UUID dragon) { return candidates.get(dragon); }

    void addPending(UUID player) {
        if (pending.add(player)) setDirty();
    }

    boolean takePending(UUID player) {
        boolean had = pending.remove(player);
        if (had) setDirty();
        return had;
    }

    boolean hasPending(UUID player) { return pending.contains(player); }
}
