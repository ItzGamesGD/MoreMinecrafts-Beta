package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

/** Adds only the purchased research offspring after vanilla's successful breed event. */
public final class ResearchLivestockRuntime {
    private ResearchLivestockRuntime() {}

    public static void afterVanillaBreeding(Animal parent, ServerLevel level, Animal partner) {
        if (level.isClientSide()) return;
        if (!(parent.getLoveCause() instanceof ServerPlayer breeder)) return;
        int researchLevel = ResearchRewards.level(breeder, "v3.research.17", "livestock_offspring");
        if (researchLevel <= 0) return;
        double roll = researchLevel == 2 ? 0.0D : breeder.getRandom().nextDouble();
        int extra = ResearchLivestockRules.additionalOffspring(researchLevel, roll);
        for (int i = 0; i < extra; i++) {
            AgeableMob offspring = parent.getBreedOffspring(level, partner);
            if (offspring == null) continue;
            offspring.setBaby(true);
            level.addFreshEntityWithPassengers(offspring);
        }
    }
}
