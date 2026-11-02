package dev.itzgamesgd.moreminecrafts.colosseum;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Runtime per-server recovery fence. Never assumes atomicity between controller journal and entity/region saves. */
final class ArenaRecovery {
    private static final Map<MinecraftServer,Set<String>> opened=new WeakHashMap<>();
    private static final Set<ArenaController> verifying=Collections.newSetFromMap(new WeakHashMap<>());
    static Path root(MinecraftServer s) { return s.getWorldPath(LevelResource.DATA).resolve("moreminecrafts-st05"); }
    static void open(MinecraftServer server, ColosseumData d, String id, long now) {
        if(!opened.computeIfAbsent(server,k->new HashSet<>()).add(id)) return;
        ArenaController c=d.controller(id);
        try {
            EffectJournal.Record r=EffectJournal.read(root(server),id);
            if(r!=null) {
                c=ArenaController.restore(id,r.state(),r.checksum(),now);
                d.replaceController(id,c);
                if(r.inFlight() && !(r.cleanup() && c.attempt()!=null && c.attempt().ended() && c.attempt().cleanupPending)) c.isolate("interrupted world effect: journal/entity saves need operator reconciliation");
            }
            if(c.active()) {
                verifying.add(c);
                if(c.attempt().graceExpiryMillis==0) c.attempt().graceExpiryMillis=now+RoundPlan.GRACE_MILLIS;
            }
        } catch(IOException e) { c.isolate("journal unavailable: "+e.getMessage()); }
    }
    static boolean verifying(ArenaController c) { return verifying.contains(c); }
    static void verified(ArenaController c) { verifying.remove(c); }
    static boolean save(MinecraftServer server,ArenaController c,boolean inFlight) {
        try { EffectJournal.write(root(server),c.arenaId,c.toJson(),inFlight); return true; }
        catch(IOException e) { c.isolate("cannot durably record world effect: "+e.getMessage()); return false; }
    }
    static boolean saveCleanup(MinecraftServer s,ArenaController c) {
        try {EffectJournal.write(root(s),c.arenaId,c.toJson(),true,true);return true;}
        catch(IOException e) {c.isolate("cannot durably record cleanup: "+e.getMessage());return false;}
    }
    static void close(MinecraftServer s) { opened.remove(s); }
}
