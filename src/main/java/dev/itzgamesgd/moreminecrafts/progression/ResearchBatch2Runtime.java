package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Event-time server progression lookup; never uses nearby players or a cached client level. */
public final class ResearchBatch2Runtime {
    private ResearchBatch2Runtime() {}
    public static int level(LivingEntity subject, String researchId) {
        return subject instanceof ServerPlayer player && player.isAlive()
                ? ResearchProgressLookup.researchLevel(player.level().getServer(), player.getUUID(), researchId) : 0;
    }
    public static int flightLifetime(LivingEntity boosted, int baseline) {
        return ResearchBatch2Rules.flightLifetime(baseline, level(boosted, "v3.research.51"));
    }
}
