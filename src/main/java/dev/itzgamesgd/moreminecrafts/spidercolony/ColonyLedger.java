package dev.itzgamesgd.moreminecrafts.spidercolony;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Saved reservations include unloaded juveniles; only confirmed removal releases a slot. */
public final class ColonyLedger extends SavedData {
    static final Codec<ColonyLedger> CODEC=Codec.unboundedMap(Codec.STRING,Codec.STRING)
            .xmap(ColonyLedger::new, d -> Map.copyOf(d.entries));
    public static final SavedDataType<ColonyLedger> TYPE=new SavedDataType<>(
            Identifier.parse("moreminecrafts:spider_colony"),ColonyLedger::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,String> entries=new HashMap<>();
    public ColonyLedger() {}
    private ColonyLedger(Map<String,String> entries) { this.entries.putAll(entries); }
    public int childCount(UUID mother) { return (int)entries.entrySet().stream().filter(e->e.getKey().startsWith("child/") && e.getValue().equals(mother.toString())).count(); }
    public void addChild(UUID mother,UUID child) { entries.put("child/"+child,mother.toString());setDirty(); }
    public void removeChild(UUID child) { if(entries.remove("child/"+child)!=null)setDirty(); }
    public boolean bind(String nest,UUID mother) {
        String key="nest/"+nest, existing=entries.get(key);
        if(existing!=null)return existing.equals(mother.toString());
        entries.put(key,mother.toString());setDirty();return true;
    }
    public boolean defeated(String nest) {return "defeated".equals(entries.get("nest/"+nest));}
    public void defeat(String nest,UUID mother) {
        String key="nest/"+nest;
        if(mother.toString().equals(entries.get(key))) {entries.put(key,"defeated");setDirty();}
    }

    public record RecordedColony(String nestId, int x, int y, int z, int nestRadius, int regionX, int regionZ) {}

    public void recordColony(String nestId, int x, int y, int z, int nestRadius, int regionX, int regionZ) {
        entries.put("colony/" + nestId, x + "," + y + "," + z + "," + nestRadius + "," + regionX + "," + regionZ);
        String rKey = "region/" + regionX + "," + regionZ;
        int prev = regionColonyCount(regionX, regionZ);
        entries.put(rKey, Integer.toString(prev + 1));
        setDirty();
    }

    public int regionColonyCount(int regionX, int regionZ) {
        String raw = entries.get("region/" + regionX + "," + regionZ);
        if (raw == null) return 0;
        try { return Integer.parseInt(raw); } catch (NumberFormatException ignored) { return 0; }
    }

    public List<RecordedColony> recordedColonies() {
        List<RecordedColony> out = new ArrayList<>();
        for (var e : entries.entrySet()) {
            if (!e.getKey().startsWith("colony/")) continue;
            String nestId = e.getKey().substring("colony/".length());
            String[] parts = e.getValue().split(",");
            if (parts.length != 6) continue;
            try {
                out.add(new RecordedColony(
                        nestId,
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]),
                        Integer.parseInt(parts[4]),
                        Integer.parseInt(parts[5])));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    public boolean isWindowEvaluated(int wx, int wz) {
        return entries.containsKey("win/" + wx + "," + wz);
    }

    public void markWindowEvaluated(int wx, int wz) {
        entries.put("win/" + wx + "," + wz, "1");
        setDirty();
    }
}
