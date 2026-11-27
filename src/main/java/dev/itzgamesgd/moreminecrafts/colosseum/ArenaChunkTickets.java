package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;

/** Non-persistent simulation tickets owned exclusively by ST05. Union avoids removing another arena's overlapping ticket. */
final class ArenaChunkTickets {
    static final TicketType TYPE = Registry.register(BuiltInRegistries.TICKET_TYPE, ModIds.id("colosseum_combat"),
            new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));
    private static final Map<ServerLevel, Set<Long>> held = new WeakHashMap<>();
    static void initialize() { }
    static Set<Long> region(ArenaGeometry g) {
        Set<Long> out = new HashSet<>();
        for(int x=(g.cx()-ArenaGeometry.EXT_R)>>4;x<=(g.cx()+ArenaGeometry.EXT_R)>>4;x++)
            for(int z=(g.cz()-ArenaGeometry.EXT_R)>>4;z<=(g.cz()+ArenaGeometry.EXT_R)>>4;z++) out.add(ChunkPos.pack(x,z));
        return out;
    }
    static void update(ServerLevel level, ColosseumData data) {
        Set<Long> needed = new HashSet<>();
        for(var site:data.sites().values()) {
            ArenaController c=data.controller(site.id()); ArenaAttempt a=c.attempt();
            if(a==null || c.quarantined() || (!c.active() && !a.cleanupPending)) continue;
            needed.addAll(region(site.geometry()));
            for(var al:a.allocations()) if(al.status==ArenaAttempt.AllocStatus.SPAWNED && al.chunkKnown) needed.add(al.lastChunk);
            if(a.bossChunkKnown) needed.add(a.bossChunk);
            for(long pos:a.terrain.pending.keySet()) needed.add(ChunkPos.pack(net.minecraft.core.BlockPos.of(pos).getX()>>4,net.minecraft.core.BlockPos.of(pos).getZ()>>4));
        }
        Set<Long> old=held.computeIfAbsent(level,k->new HashSet<>());
        for(long p:needed) if(!old.contains(p)) level.getChunkSource().addTicketWithRadius(TYPE,ChunkPos.unpack(p),2);
        for(long p:old) if(!needed.contains(p)) level.getChunkSource().removeTicketWithRadius(TYPE,ChunkPos.unpack(p),2);
        held.put(level,needed);
    }
    static void clear(ServerLevel level) {
        for(long p:held.getOrDefault(level,Set.of())) level.getChunkSource().removeTicketWithRadius(TYPE,ChunkPos.unpack(p),2);
        held.remove(level);
    }
}
