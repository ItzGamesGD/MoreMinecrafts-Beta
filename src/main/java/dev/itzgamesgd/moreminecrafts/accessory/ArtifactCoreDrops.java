package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.random.RandomGenerator;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Death-position relic-core drops using the entity's existing random stream. */
public final class ArtifactCoreDrops {
    private ArtifactCoreDrops() {}

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level)) return;
            ArtifactCoreDropRules.Tier tier = ArtifactCoreDropRules.tier(entity);
            if (tier == null) return;
            Item item = item(ArtifactCoreBiomeRules.classify(level, entity.blockPosition()));
            if (item == null) return;
            var sourceRandom = entity.getRandom();
            RandomGenerator random = new RandomGenerator() {
                @Override public long nextLong() { return sourceRandom.nextLong(); }
            };
            int count = ArtifactCoreDropRules.rollCount(tier, random);
            if (count > 0) entity.spawnAtLocation(level, new ItemStack(item, count));
        });
    }

    private static Item item(ArtifactCore core) {
        if (core == null) return null;
        return switch (core) {
            case EARTHEN -> ModItems.EARTHEN_RELIC_CORE;
            case ABYSSAL -> ModItems.ABYSSAL_RELIC_CORE;
            case GLACIAL -> ModItems.GLACIAL_RELIC_CORE;
            case TIDAL -> ModItems.TIDAL_RELIC_CORE;
            case BLAZING -> ModItems.BLAZING_RELIC_CORE;
        };
    }
}
