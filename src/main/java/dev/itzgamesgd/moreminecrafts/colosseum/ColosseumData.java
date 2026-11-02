package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-persistent (Overworld SavedData) registry of constructed arenas and each arena's controller state. Controller state is stored as
 * checksummed JSON; an unreadable or inconsistent record is restored as a quarantined controller instead of being guessed at.
 */
public final class ColosseumData extends SavedData {
    /** Wall clock source; replaced only by tests. */
    public static volatile LongSupplier clock = System::currentTimeMillis;

    public record Site(String id, int cx, int cz, int floorY) {
        public ArenaGeometry geometry() { return new ArenaGeometry(cx, cz, floorY); }
    }

    private record SiteSave(String id, int cx, int cz, int floorY, String state, String checksum) {}

    private static final Codec<SiteSave> SITE_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(SiteSave::id),
            Codec.INT.fieldOf("cx").forGetter(SiteSave::cx),
            Codec.INT.fieldOf("cz").forGetter(SiteSave::cz),
            Codec.INT.fieldOf("floor_y").forGetter(SiteSave::floorY),
            Codec.STRING.fieldOf("state").forGetter(SiteSave::state),
            Codec.STRING.fieldOf("checksum").forGetter(SiteSave::checksum)
    ).apply(i, SiteSave::new));

    private static final Codec<ColosseumData> CODEC = SITE_CODEC.listOf().xmap(ColosseumData::fromSaves, ColosseumData::toSaves);

    public static final SavedDataType<ColosseumData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:colosseum_arenas"), ColosseumData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<String, Site> sites = new LinkedHashMap<>();
    private final Map<String, ArenaController> controllers = new LinkedHashMap<>();

    public ColosseumData() {}

    private static ColosseumData fromSaves(List<SiteSave> saves) {
        ColosseumData d = new ColosseumData();
        long now = clock.getAsLong();
        for (SiteSave s : saves) {
            d.sites.put(s.id(), new Site(s.id(), s.cx(), s.cz(), s.floorY()));
            d.controllers.put(s.id(), ArenaController.restore(s.id(), s.state(), s.checksum(), now));
        }
        return d;
    }

    private List<SiteSave> toSaves() {
        List<SiteSave> out = new ArrayList<>();
        for (Site s : sites.values()) {
            String json = controllers.get(s.id()).toJson();
            out.add(new SiteSave(s.id(), s.cx(), s.cz(), s.floorY(), json, ArenaController.checksum(json)));
        }
        return out;
    }

    public Map<String, Site> sites() { return sites; }
    public ArenaController controller(String id) { return controllers.get(id); }
    void replaceController(String id, ArenaController c) { controllers.put(id,c); setDirty(); }

    /** Idempotent: a repeated registration of the same id never replaces geometry or state. Returns true if newly registered. */
    public boolean register(String id, int cx, int cz, int floorY) {
        if (sites.containsKey(id)) return false;
        sites.put(id, new Site(id, cx, cz, floorY));
        controllers.put(id, new ArenaController(id));
        setDirty();
        return true;
    }
}
