package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.*;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Restoration is a resumable world effect. No loaded chunk, no write; occupied/conflicting positions are never overwritten. */
final class ArenaTerrain {
    static void breakObstacles(ServerLevel l,ArenaController c,AABB box) {
        var site=ColosseumRuntime.data(l.getServer()).sites().get(c.arenaId);var a=c.attempt();
        for(BlockPos p:BlockPos.betweenClosed((int)Math.floor(box.minX),(int)Math.floor(box.minY),(int)Math.floor(box.minZ),(int)Math.floor(box.maxX),(int)Math.floor(box.maxY),(int)Math.floor(box.maxZ))) {
            if(c.structureMask().protects(p) || !ArenaNaturalBlocks.eligible(l,p,site))continue;
            a.terrain.record(p.asLong(),BlockStateParser.serialize(l.getBlockState(p)));
            if(!ArenaRecovery.save(l.getServer(),c,true))return;
            l.setBlock(p,Blocks.AIR.defaultBlockState(),3);
            if(!ArenaRecovery.save(l.getServer(),c,false))return;
        }
    }
    static void restore(ServerLevel l,ArenaController c) {
        var ledger=c.attempt().terrain;
        for(long packed:new ArrayList<>(ledger.pending.keySet())) {
            BlockPos p=BlockPos.of(packed);if(!l.hasChunkAt(p))continue;
            if(c.structureMask().protects(p)) {ledger.restore(packed,false);continue;}
            try {
                var state=BlockStateParser.parseForBlock(l.registryAccess().lookupOrThrow(Registries.BLOCK),ledger.pending.get(packed),false).blockState();
                // Keep the prior state pending until the write finishes. A restart can safely complete it or record a conflict.
                if(!l.getBlockState(p).isAir()) {ledger.restore(packed,false);continue;}
                if(!ArenaRecovery.saveCleanup(l.getServer(),c))return;
                l.setBlock(p,state,3);
                ledger.pending.remove(packed);
                if(!ArenaRecovery.saveCleanup(l.getServer(),c))return;
            } catch(Exception e) {c.isolate("invalid terrain restoration record: "+e.getMessage());return;}
        }
    }
}
