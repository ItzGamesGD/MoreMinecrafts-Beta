package dev.itzgamesgd.moreminecrafts.skytower;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Domain records in Minecraft SavedData; no new event engine and no parallel vanilla reward ledger. */
public final class SkyTowerData extends SavedData {
    private record Save(String dimension,int x,int y,int z,int rotation,int mask) {}
    private static final Codec<Save> SAVE_CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.fieldOf("dimension").forGetter(Save::dimension),
        Codec.INT.fieldOf("x").forGetter(Save::x),Codec.INT.fieldOf("y").forGetter(Save::y),
        Codec.INT.fieldOf("z").forGetter(Save::z),Codec.INT.fieldOf("rotation").forGetter(Save::rotation),
        Codec.INT.fieldOf("completed_spawners").forGetter(Save::mask)
    ).apply(i,Save::new));
    private record Snapshot(int version,boolean quarantined,List<Save> sites,String quarantinePayload) {}
    private static final Codec<Snapshot> SNAPSHOT_CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.INT.fieldOf("version").forGetter(Snapshot::version),
        Codec.BOOL.fieldOf("quarantined").forGetter(Snapshot::quarantined),
        SAVE_CODEC.listOf().fieldOf("sites").forGetter(Snapshot::sites),
        Codec.STRING.optionalFieldOf("quarantine_payload", "").forGetter(Snapshot::quarantinePayload)
    ).apply(i,Snapshot::new));
    /** Vanilla uses resultOrPartial when loading SavedData. Never expose an unquarantined
     * partial registry or an empty decoder failure that computeIfAbsent could treat as new.
     * Preserve a canonical JSON copy of the unreadable input for operator recovery.
     */
    public static final Codec<SkyTowerData> CODEC=new Codec<>() {
        @Override public <T> DataResult<Pair<SkyTowerData,T>> decode(DynamicOps<T> ops,T input){
            var parsed=SNAPSHOT_CODEC.decode(ops,input);
            if(parsed.error().isPresent()){
                var d=new SkyTowerData();d.quarantined=true;
                d.quarantinePayload=ops.convertTo(JsonOps.INSTANCE,input).toString();
                return DataResult.success(Pair.of(d,ops.empty()));
            }
            return parsed.map(pair->{
                var d=restore(pair.getFirst());
                if(d.quarantined && d.quarantinePayload.isEmpty())
                    d.quarantinePayload=ops.convertTo(JsonOps.INSTANCE,input).toString();
                return Pair.of(d,pair.getSecond());
            });
        }
        @Override public <T> DataResult<T> encode(SkyTowerData d,DynamicOps<T> ops,T prefix){
            return SNAPSHOT_CODEC.encode(new Snapshot(1,d.quarantined,d.saves(),d.quarantinePayload),ops,prefix);
        }
    };
    public static final SavedDataType<SkyTowerData> TYPE=new SavedDataType<>(
        Identifier.parse("moreminecrafts:sky_tower_instances"),SkyTowerData::new,CODEC,DataFixTypes.LEVEL);
    private final Map<String,SkyTowerLayout.Site> sites=new LinkedHashMap<>();
    private final Map<String,SkyTowerProgress> progress=new LinkedHashMap<>();
    private boolean quarantined;
    private String quarantinePayload="";
    public SkyTowerData() {}
    public boolean quarantined(){return quarantined;}
    public Map<String,SkyTowerLayout.Site> sites(){return Collections.unmodifiableMap(sites);}
    public int completedMask(String id){return requiredProgress(id).completedMask();}
    private SkyTowerProgress requiredProgress(String id){
        var p=progress.get(id);if(p==null)throw new IllegalArgumentException("unregistered tower");return p;
    }
    /** Re-registration preserves all existing progress. Overlapping/differently rotated duplicates rejected. */
    public boolean register(SkyTowerLayout.Site site){
        if(quarantined)throw new IllegalStateException("sky tower registry quarantined");
        if(sites.containsKey(site.id()))return false;
        if(sites.values().stream().anyMatch(site::overlaps))throw new IllegalArgumentException("overlapping tower registration");
        sites.put(site.id(),site);progress.put(site.id(),new SkyTowerProgress(0));setDirty();return true;
    }
    /** Vanilla-event adapter entry. Exact world anchor required; arbitrary mobs/player events cannot complete a floor. */
    public boolean recordVanillaCompletion(String id,SkyTowerLayout.Point spawner){
        if(quarantined)return false;
        var site=sites.get(id);if(site==null)return false;
        int index=site.spawnerIndex(spawner);if(index<0)return false;
        boolean changed=requiredProgress(id).completeVanillaTrial(index);if(changed)setDirty();return changed;
    }
    private List<Save> saves(){
        List<Save> rows=new ArrayList<>();
        for(var s:sites.values())rows.add(new Save(s.dimension(),s.x(),s.y(),s.z(),s.rotation(),completedMask(s.id())));
        return rows;
    }
    private static SkyTowerData restore(Snapshot snapshot){
        var d=new SkyTowerData();d.quarantined=snapshot.quarantined || snapshot.version!=1;d.quarantinePayload=snapshot.quarantinePayload;
        for(var row:snapshot.sites){
            try {
                var site=new SkyTowerLayout.Site(row.dimension,row.x,row.y,row.z,row.rotation);
                var p=new SkyTowerProgress(row.mask);
                if(d.sites.containsKey(site.id()) || d.sites.values().stream().anyMatch(site::overlaps)){
                    d.quarantined=true;continue;
                }
                d.sites.put(site.id(),site);d.progress.put(site.id(),p);
            }catch(IllegalArgumentException | ArithmeticException e){d.quarantined=true;}
        }
        return d;
    }
}
