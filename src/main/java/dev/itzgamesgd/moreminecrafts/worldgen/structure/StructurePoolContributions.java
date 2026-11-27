package dev.itzgamesgd.moreminecrafts.worldgen.structure;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/** Additive contributions keep vanilla pools, fallbacks, processors and selection intact. */
public final class StructurePoolContributions {
    private record Contribution(Identifier target, Identifier source) {}

    private static final List<Contribution> CONTRIBUTIONS = List.of(new Contribution(
            Identifier.parse("minecraft:ancient_city/structures"),
            Identifier.parse("moreminecrafts:ancient_city/arena_additions")));

    private StructurePoolContributions() {}

    public static void initialize() {
        DynamicRegistrySetupCallback.EVENT.register(view -> {
            if (view.getOptional(Registries.TEMPLATE_POOL).isEmpty()) return;
            // State belongs to this registry load, not to a global server or reload history.
            var seen = new HashMap<Identifier, StructureTemplatePool>();
            var applied = new HashSet<Contribution>();
            view.registerEntryAdded(Registries.TEMPLATE_POOL, (rawId, id, pool) -> {
                seen.put(id, pool);
                for (var contribution : CONTRIBUTIONS) {
                    var target = seen.get(contribution.target());
                    var source = seen.get(contribution.source());
                    if (target == null || source == null || !applied.add(contribution)) continue;
                    ((StructurePoolExtension) target).moreminecrafts$append(source.getTemplates());
                    int weight = source.getTemplates().stream().mapToInt(entry -> entry.getSecond()).sum();
                    MoreMinecrafts.LOGGER.info("STRUCTURE_POOL_CONTRIBUTION source={} target={} added_weight={} total_weight={}",
                            contribution.source(), contribution.target(), weight, target.size());
                }
            });
        });
    }
}
