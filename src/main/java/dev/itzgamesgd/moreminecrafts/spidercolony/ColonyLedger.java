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
}
