package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * Generation-time village overlap check for ST-03 candidates.
 * Rebuilds the generator structure state from the generation context (the context does not carry it)
 * and reports whether any village start bounding box intersects the candidate headquarters box.
 * Village starts are cached per chunk generator, since the seed and state are fixed for one generator.
 */
final class St03VillageOverlap {
    private static final Map<ChunkGenerator, Map<String, Optional<BoundingBox>>> CACHES = new WeakHashMap<>();
    private static final Map<ChunkGenerator, ChunkGeneratorStructureState> STATES = new WeakHashMap<>();

    private St03VillageOverlap() {
    }

    static synchronized boolean overlapsVillage(Structure.GenerationContext context, BoundingBox headquartersBox) {
        ChunkGenerator generator = context.chunkGenerator();
        var registries = context.registryAccess();
        ChunkGeneratorStructureState state = STATES.computeIfAbsent(generator, g ->
                ChunkGeneratorStructureState.createForNormal(context.randomState(), context.seed(),
                        context.biomeSource(), registries.lookupOrThrow(Registries.STRUCTURE_SET)));
        Map<String, Optional<BoundingBox>> cache = CACHES.computeIfAbsent(generator, g -> new HashMap<>());

        int minCx = (headquartersBox.minX() >> 4) - 5, maxCx = (headquartersBox.maxX() >> 4) + 5;
        int minCz = (headquartersBox.minZ() >> 4) - 5, maxCz = (headquartersBox.maxZ() >> 4) + 5;
        long seed = context.seed();
        for (Holder<StructureSet> holder : state.possibleStructureSets()) {
            String setId = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("");
            if (!setId.contains("village")) continue;
            if (!(holder.value().placement() instanceof RandomSpreadStructurePlacement placement)) continue;
            int spacing = placement.spacing();
            for (int rx = Math.floorDiv(minCx, spacing); rx <= Math.floorDiv(maxCx, spacing); rx++) {
                for (int rz = Math.floorDiv(minCz, spacing); rz <= Math.floorDiv(maxCz, spacing); rz++) {
                    ChunkPos chunk = placement.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                    if (chunk.x() < minCx || chunk.x() > maxCx || chunk.z() < minCz || chunk.z() > maxCz) continue;
                    if (!placement.isStructureChunk(state, chunk.x(), chunk.z())) continue;
                    String key = setId + "@" + chunk.x() + "," + chunk.z();
                    Optional<BoundingBox> box = cache.get(key);
                    if (box == null) {
                        box = Optional.ofNullable(firstVillageBox(context, state, holder.value(), chunk));
                        cache.put(key, box);
                    }
                    if (box.isPresent() && box.get().intersects(headquartersBox)) return true;
                }
            }
        }
        return false;
    }

    private static BoundingBox firstVillageBox(Structure.GenerationContext context, ChunkGeneratorStructureState state,
                                               StructureSet set, ChunkPos chunk) {
        List<StructureSet.StructureSelectionEntry> options = new ArrayList<>(set.structures());
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(context.seed(), chunk.x(), chunk.z());
        int total = options.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();
        var sampler = context.randomState().createClimateSampler(SamplerContext.builder().enableCaches().build());
        while (!options.isEmpty()) {
            int index = 0;
            if (options.size() != 1) {
                int choice = random.nextInt(total);
                for (var option : options) {
                    choice -= option.weight();
                    if (choice < 0) break;
                    index++;
                }
            }
            var option = options.get(index);
            Structure structure = option.structure().value();
            StructureStart start = structure.generate(option.structure(), Level.OVERWORLD, context.registryAccess(),
                    context.chunkGenerator(), context.biomeSource(), sampler, context.randomState(),
                    context.structureTemplateManager(), context.seed(), chunk, 0, context.heightAccessor(),
                    structure.biomes()::contains);
            if (start.isValid()) return start.getBoundingBox();
            total -= option.weight();
            options.remove(index);
        }
        return null;
    }
}
