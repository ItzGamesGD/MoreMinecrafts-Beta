package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.RandomSource;

/**
 * Rolls a fixed objective set and reward on acquisition. Objective and reward are chosen once here; the result is
 * stored on the item and never re-rolled. Provisional choices are documented in the Oct 22 authority record.
 */
public final class ContractGenerator {
    private ContractGenerator() {}

    /** Converts an acquired Advanced contract into a Special contract with 5% chance; other tiers pass through. */
    public static ContractData.Tier convertOnAcquisition(ContractData.Tier tier, RandomSource random) {
        return ContractEconomy.convertAdvanced(tier, random.nextInt(100));
    }

    /** Full acquisition: applies special conversion, then generates the fixed objectives and reward. */
    public static ContractData acquire(ContractData.Tier rolledTier, RandomSource random) {
        ContractData.Tier tier = convertOnAcquisition(rolledTier, random);
        return generate(UUID.randomUUID().toString(), tier, random);
    }

    /** Whole-set regeneration limit when a drawn combat target has no valid candidate. */
    static final int MAX_ATTEMPTS = 64;

    /**
     * Generates objectives and reward for a tier. SPECIAL uses the Advanced objective structure. A combat draw with no
     * valid candidate rejects the whole objective set, which is regenerated; no target is substituted.
     */
    public static ContractData generate(String id, ContractData.Tier tier, RandomSource random) {
        ContractData.Tier shape = tier == ContractData.Tier.SPECIAL ? ContractData.Tier.ADVANCED : tier;
        List<ContractData.Objective> objectives = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS && objectives == null; attempt++)
            objectives = tryObjectives(shape, random);
        if (objectives == null) throw new IllegalStateException("no valid objective set for tier " + tier);
        ContractEconomy.Rarity rarity = ContractEconomy.rarity(tier, random.nextInt(1000));
        ContractRewards.Pick pick = ContractRewards.roll(rarity, random);
        int[] payout = ContractEconomy.band(tier);
        int emeralds = payout[2] + random.nextInt(payout[3] - payout[2] + 1);
        return new ContractData(id, tier, ContractData.Status.OFFERED, objectives,
                new ContractData.Reward(emeralds, pick.item(), pick.count()));
    }

    /** One attempt at an objective set; null when a combat draw had no valid candidate. */
    static List<ContractData.Objective> tryObjectives(ContractData.Tier shape, RandomSource random) {
        ContractCatalog.Dimension dimension = pickDimension(shape, random);
        boolean highValueUsed = false;
        int slots = switch (shape) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 1 + random.nextInt(2);
            case ADVANCED, SPECIAL -> 2 + random.nextInt(2);
        };
        List<ContractData.Objective> objectives = new ArrayList<>(slots);
        for (int slot = 0; slot < slots; slot++) {
            int category = random.nextInt(3);
            if (category == 0) {
                Optional<ContractData.Objective> combat = combat(shape, dimension, random);
                if (combat.isEmpty()) return null;
                objectives.add(combat.get());
            } else if (category == 1) {
                ContractData.Objective delivery = delivery(shape, random, highValueUsed);
                highValueUsed |= ContractCatalog.HIGH_VALUE_DELIVERY.contains(delivery.target());
                objectives.add(delivery);
            } else {
                objectives.add(exploration(random));
            }
        }
        return objectives;
    }

    /** Beginner is Overworld only; Intermediate may use Nether; Advanced may use Nether or End (uniform). */
    static ContractCatalog.Dimension pickDimension(ContractData.Tier shape, RandomSource random) {
        List<ContractCatalog.Dimension> allowed = switch (shape) {
            case BEGINNER -> List.of(ContractCatalog.Dimension.OVERWORLD);
            case INTERMEDIATE -> List.of(ContractCatalog.Dimension.OVERWORLD, ContractCatalog.Dimension.NETHER);
            case ADVANCED, SPECIAL -> List.of(ContractCatalog.Dimension.OVERWORLD, ContractCatalog.Dimension.NETHER,
                    ContractCatalog.Dimension.END);
        };
        return allowed.get(random.nextInt(allowed.size()));
    }

    /** Empty when the drawn strength has no valid candidate for this tier and dimension; never substitutes. */
    public static Optional<ContractData.Objective> combat(ContractData.Tier shape, ContractCatalog.Dimension dimension, RandomSource random) {
        ContractCatalog.Strength wanted = random.nextInt(100) < ContractCatalog.strongPercent(shape)
                ? ContractCatalog.Strength.STRONG : ContractCatalog.Strength.ORDINARY;
        List<String> pool = mobPool(shape, dimension, wanted);
        if (pool.isEmpty()) return Optional.empty();
        String target = pool.get(random.nextInt(pool.size()));
        int[] band = wanted == ContractCatalog.Strength.ORDINARY ? ordinaryKillBand(shape) : strongKillBand(shape);
        return Optional.of(new ContractData.Objective(ContractData.Category.COMBAT, target, band(band, random), 0));
    }

    /** Valid targets for a tier, dimension and strength: hostile, available, and at or above their minimum tier. */
    static List<String> mobPool(ContractData.Tier shape, ContractCatalog.Dimension dimension, ContractCatalog.Strength strength) {
        List<String> ids = new ArrayList<>();
        for (List<ContractCatalog.Mob> source : List.of(ContractCatalog.VANILLA_MOBS, ContractCatalog.MM_ORDINARY_MOBS,
                ContractCatalog.MM_STRONG_MOBS))
            for (ContractCatalog.Mob mob : source)
                if (mob.dimension() == dimension && mob.strength() == strength
                        && mob.minTier().ordinal() <= shape.ordinal()) ids.add(mob.id());
        return ids;
    }

    static ContractData.Objective delivery(ContractData.Tier shape, RandomSource random, boolean highValueUsed) {
        int choices = ContractCatalog.DELIVERY_ORDINARY.size() + ContractCatalog.DELIVERY_RARE.size()
                + (highValueUsed ? 0 : ContractCatalog.HIGH_VALUE_DELIVERY.size());
        int pick = random.nextInt(choices);
        int ordinaryEnd = ContractCatalog.DELIVERY_ORDINARY.size();
        int rareEnd = ordinaryEnd + ContractCatalog.DELIVERY_RARE.size();
        String target;
        int[] band;
        if (pick < ordinaryEnd) {
            target = ContractCatalog.DELIVERY_ORDINARY.get(pick);
            band = ordinaryDeliveryBand(shape);
        } else if (pick < rareEnd) {
            target = ContractCatalog.DELIVERY_RARE.get(pick - ordinaryEnd);
            band = rareDeliveryBand(shape);
        } else {
            target = ContractCatalog.HIGH_VALUE_DELIVERY.get(pick - rareEnd);
            band = target.equals("minecraft:diamond") ? new int[]{2, 4} : new int[]{1, 1};
        }
        return new ContractData.Objective(ContractData.Category.DELIVERY, target, band(band, random), 0);
    }

    static ContractData.Objective exploration(RandomSource random) {
        String structure = ContractCatalog.EXPLORATION_STRUCTURES.get(
                random.nextInt(ContractCatalog.EXPLORATION_STRUCTURES.size()));
        return new ContractData.Objective(ContractData.Category.EXPLORATION, structure, 1, 0);
    }

    static int band(int[] range, RandomSource random) {
        return range[0] + random.nextInt(range[1] - range[0] + 1);
    }

    /** Approved combat count bands (Oct 22 owner-approved count table). */
    static int[] ordinaryKillBand(ContractData.Tier shape) {
        return switch (shape) {
            case BEGINNER -> new int[]{8, 16};
            case INTERMEDIATE -> new int[]{20, 40};
            default -> new int[]{40, 80};
        };
    }

    static int[] strongKillBand(ContractData.Tier shape) {
        return switch (shape) {
            case BEGINNER -> new int[]{2, 4};
            case INTERMEDIATE -> new int[]{5, 10};
            default -> new int[]{10, 20};
        };
    }

    static int[] ordinaryDeliveryBand(ContractData.Tier shape) {
        return switch (shape) {
            case BEGINNER -> new int[]{16, 32};
            case INTERMEDIATE -> new int[]{32, 64};
            default -> new int[]{64, 128};
        };
    }

    static int[] rareDeliveryBand(ContractData.Tier shape) {
        return switch (shape) {
            case BEGINNER -> new int[]{2, 4};
            case INTERMEDIATE -> new int[]{4, 12};
            default -> new int[]{8, 24};
        };
    }
}
