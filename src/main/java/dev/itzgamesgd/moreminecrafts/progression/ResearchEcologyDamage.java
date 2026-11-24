package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Explicit mob-family classification for the paid ecology damage bonuses. */
public final class ResearchEcologyDamage {
    private static final TagKey<EntityType<?>> HOGLINS = tag("hoglins");
    private static final TagKey<EntityType<?>> AQUATIC_HOSTILES = tag("aquatic_hostiles");
    private static final TagKey<EntityType<?>> RARE_MOBS = tag("rare_mobs");
    private static final TagKey<EntityType<?>> BOSSES = tag("bosses");

    private ResearchEcologyDamage() {}

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ModIds.id("research/ecology/" + name));
    }

    public static double paidBonus(int researchNumber, int level) {
        int safe = Math.min(3, Math.max(0, level));
        return switch (researchNumber) {
            case 44, 45 -> new double[]{0, 0.04, 0.06, 0.08}[safe];
            case 46 -> new double[]{0, 0.05, 0.08, 0.12}[safe];
            case 47 -> new double[]{0, 0.03, 0.05, 0.07}[safe];
            default -> 0;
        };
    }

    public static double bonus(ServerPlayer attacker, LivingEntity target) {
        ResearchProgressState state = KnowledgeRuntime.state(attacker);
        // Families are disjoint in the baseline tags; the knowledge award is capped once if a
        // datapack extends more than one family with the same entity type.
        double paid = 0;
        boolean knowledge = false;
        boolean isBoss = target.getType().builtInRegistryHolder().is(BOSSES);
        if (target.getType().builtInRegistryHolder().is(HOGLINS)) {
            paid += paidBonus(44, state.researchLevel("v3.research.44"));
            knowledge |= state.hasKnowledge(49);
        }
        if (target.getType().builtInRegistryHolder().is(AQUATIC_HOSTILES)) {
            paid += paidBonus(45, state.researchLevel("v3.research.45"));
            knowledge |= state.hasKnowledge(50);
        }
        if (!isBoss && target.getType().builtInRegistryHolder().is(RARE_MOBS)) {
            paid += paidBonus(46, state.researchLevel("v3.research.46"));
            knowledge |= state.hasKnowledge(51);
        }
        if (isBoss) {
            paid += paidBonus(47, state.researchLevel("v3.research.47"));
            knowledge |= state.hasKnowledge(52);
        }
        return paid + (knowledge ? 0.05D : 0.0D);
    }
}
