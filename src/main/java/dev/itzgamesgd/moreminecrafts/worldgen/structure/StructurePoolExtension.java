package dev.itzgamesgd.moreminecrafts.worldgen.structure;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

/** Implemented on the target-version pool by a narrow load-time mixin. */
public interface StructurePoolExtension {
    void moreminecrafts$append(List<Pair<StructurePoolElement, Integer>> entries);
}
