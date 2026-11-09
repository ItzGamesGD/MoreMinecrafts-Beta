package dev.itzgamesgd.moreminecrafts.colosseum;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
/** Exactly one deterministic boss identity per attempt; missing saved bosses on recovery are quarantined by the reconciler. */
final class GladiatorBossProvider implements ColosseumBossHook.Provider {
    public boolean start(ServerLevel level,ColosseumData.Site site,ArenaAttempt a,int maxHp) {
        UUID id=UUID.nameUUIDFromBytes((a.attemptId+":R7:gladiator").getBytes(StandardCharsets.UTF_8));
        if(a.bossEntity!=null && !a.bossEntity.equals(id))return false;
        var existing=level.getEntity(id);
        if(existing!=null)return existing instanceof ImmortalGladiator b && a.attemptId.equals(b.attemptId());
        if(a.bossEntity!=null)return false; // Bound but missing: never spawn an unverified replacement.
        var b=GladiatorEntities.TYPE.create(level,EntitySpawnReason.EVENT);if(b==null)return false;
        b.setUUID(id);b.bind(site.id(),a.attemptId,maxHp);
        b.snapTo(site.cx()+.5,site.floorY()+1,site.cz()+.5,0,0);
        b.addTag(ColosseumRuntime.TAG_ATTEMPT+a.attemptId);b.addTag(ColosseumRuntime.TAG_BOSS);
        // Planned identity is journaled before insertion, independently of SavedData/entity saves.
        a.bossEntity=id;a.bossHealth=b.getHealth();a.bossState=b.combatSnapshot();a.bossChunk=net.minecraft.world.level.ChunkPos.pack(site.cx()>>4,site.cz()>>4);a.bossChunkKnown=true;
        ArenaController c=ColosseumRuntime.data(level.getServer()).controller(site.id());
        if(!ArenaRecovery.save(level.getServer(),c,true))return false;
        if(!level.addFreshEntity(b)) {c.isolate("boss insertion failed after durable plan");return false;}
        return true;
    }
    public void vanish(ServerLevel l,ColosseumData.Site site,ArenaAttempt a) {
        if(a.bossEntity!=null) {var e=l.getEntity(a.bossEntity);if(e!=null)e.discard();}
    }
}
