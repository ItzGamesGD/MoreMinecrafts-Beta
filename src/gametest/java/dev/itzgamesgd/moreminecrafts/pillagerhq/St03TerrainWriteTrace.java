package dev.itzgamesgd.moreminecrafts.pillagerhq;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Validation-only provenance of the reproducible seed 10148 foundation mutation. Never changes a block. */
public final class St03TerrainWriteTrace {
    private St03TerrainWriteTrace() {}
    public static void observe(String source,BlockPos pos,BlockState state) {
        if(Boolean.getBoolean("mm.st03.terrain.trace")&&pos.getX()==-26097&&pos.getY()==66&&pos.getZ()==-26086)
            org.slf4j.LoggerFactory.getLogger("st03-terrain-trace").warn("ST03_TERRAIN_TRACE {} {} {}",source,pos,state,new Exception("Validation write provenance"));
    }
}
