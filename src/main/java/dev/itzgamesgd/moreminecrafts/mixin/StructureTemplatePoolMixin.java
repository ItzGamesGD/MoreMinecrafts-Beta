package dev.itzgamesgd.moreminecrafts.mixin;

import com.mojang.datafixers.util.Pair;
import dev.itzgamesgd.moreminecrafts.worldgen.structure.StructurePoolExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StructureTemplatePool.class)
public abstract class StructureTemplatePoolMixin implements StructurePoolExtension {
    @Shadow @Final @Mutable private List<Pair<StructurePoolElement, Integer>> rawTemplates;
    @Shadow @Final private ObjectArrayList<StructurePoolElement> templates;
    @Shadow private int maxSize;

    @Override
    public void moreminecrafts$append(List<Pair<StructurePoolElement, Integer>> entries) {
        var combined = new ArrayList<>(rawTemplates);
        for (var entry : entries) {
            int weight = entry.getSecond();
            if (weight < 1 || weight > 150) throw new IllegalArgumentException("Pool contribution weight outside 1..150");
            combined.add(entry);
            for (int copy = 0; copy < weight; copy++) templates.add(entry.getFirst());
        }
        rawTemplates = combined;
        maxSize = Integer.MIN_VALUE;
    }
}
