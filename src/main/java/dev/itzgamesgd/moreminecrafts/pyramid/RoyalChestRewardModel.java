package dev.itzgamesgd.moreminecrafts.pyramid;

import dev.itzgamesgd.moreminecrafts.accessory.PyramidRoyalChestRewards;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/**
 * D13-03-I06 pure reward model: tables, probabilities and quantities exactly as recorded in
 * {@code docs/planning/D13_03_I06_ROYAL_CHEST_REWARDS_AUTHORITY_2026-11-07.md}. No Minecraft runtime state is touched,
 * so every rule is unit-testable with a controlled {@link RandomGenerator}.
 *
 * <p>Order independence: a chest's result is a function of its own random stream only; the structure-level catalyst
 * assignment uses a separate stream. Neither depends on which chest is opened first.
 */
public final class RoyalChestRewardModel {
    public static final int SLOTS = 54;
    public static final double RARE_GATE_CHANCE = 0.10D;
    public static final double ACCESSORY_CHANCE = PyramidRoyalChestRewards.CHANCE;
    public static final double CATALYST_CHANCE = 0.05D;
    public static final String CATALYST_ID = "moreminecrafts:relic_catalyst";

    /** One independent item: success probability, then an inclusive uniform quantity. */
    public record Entry(String itemId, double chance, int min, int max) {}

    /** One reward line before it is split into stacks. */
    public record Reward(String itemId, int count) {}

    /** One concrete inventory stack. */
    public record Stack(String itemId, int count) {}

    public static final List<Entry> PRIMARY_ORDINARY = List.of(
            new Entry("minecraft:gold_ingot", 1.00D, 64, 128),
            new Entry("minecraft:diamond", 0.85D, 10, 18),
            new Entry("minecraft:emerald", 0.75D, 12, 24),
            new Entry("moreminecrafts:mythril", 0.25D, 2, 3));

    /** Rare pool, conditional on the single 10% gate; weights are percent of the gate and sum to 100. */
    public record RareEntry(String itemId, int weightPercent, int min, int max) {}

    public static final List<RareEntry> PRIMARY_RARE = List.of(
            new RareEntry("moreminecrafts:adamantium_ingot", 20, 1, 1),
            new RareEntry("minecraft:netherite_ingot", 10, 1, 1),
            new RareEntry("minecraft:netherite_scrap", 35, 1, 2),
            new RareEntry("minecraft:ancient_debris", 35, 1, 2));

    public static final List<Entry> SECONDARY_MATERIALS = List.of(
            new Entry("moreminecrafts:enhancement_stone", 1.00D, 64, 96),
            new Entry("minecraft:redstone", 1.00D, 64, 128),
            new Entry("minecraft:lapis_lazuli", 1.00D, 64, 128),
            new Entry("minecraft:diamond", 1.00D, 10, 18),
            new Entry("moreminecrafts:titanium_ingot", 0.75D, 32, 64),
            new Entry("moreminecrafts:tungsten_ingot", 0.75D, 32, 64));

    private RoyalChestRewardModel() {}

    /** Inclusive discrete uniform integer in [min, max]. */
    static int quantity(RandomGenerator random, int min, int max) {
        return min == max ? min : min + random.nextInt(max - min + 1);
    }

    /**
     * Base materials for one chest (no accessory, no catalyst). Each entry consumes exactly one chance draw, and
     * one quantity draw on success; PRIMARY then consumes one gate draw and, on success, one pick and one quantity.
     */
    public static List<Reward> rollMaterials(RoyalChestRole role, RandomGenerator random) {
        List<Reward> rewards = new ArrayList<>();
        for (Entry entry : role == RoyalChestRole.PRIMARY ? PRIMARY_ORDINARY : SECONDARY_MATERIALS) {
            if (random.nextDouble() < entry.chance())
                rewards.add(new Reward(entry.itemId(), quantity(random, entry.min(), entry.max())));
        }
        if (role == RoyalChestRole.PRIMARY && random.nextDouble() < RARE_GATE_CHANCE) {
            int pick = random.nextInt(100);
            int cumulative = 0;
            for (RareEntry rare : PRIMARY_RARE) {
                cumulative += rare.weightPercent();
                if (pick < cumulative) {
                    rewards.add(new Reward(rare.itemId(), quantity(random, rare.min(), rare.max())));
                    break;
                }
            }
        }
        return rewards;
    }

    /** Independent per-chest T2 accessory: 20%, then Seed of Life / Ring of Time 50:50 (reuses the I05 draw). */
    public static Optional<String> rollAccessory(RandomGenerator random) {
        return PyramidRoyalChestRewards.draw(random::nextDouble, random::nextInt)
                .map(definition -> "moreminecrafts:" + definition.registryId());
    }

    /** Structure-level catalyst: one 5% roll per instance, then PRIMARY / SECONDARY 50:50. At most one result. */
    public static Optional<RoyalChestRole> assignCatalyst(RandomGenerator random) {
        if (!(random.nextDouble() < CATALYST_CHANCE)) return Optional.empty();
        return Optional.of(random.nextInt(2) == 0 ? RoyalChestRole.PRIMARY : RoyalChestRole.SECONDARY);
    }

    /** All rewards one chest gets: materials, optional accessory, and the catalyst when this chest is assigned it. */
    public static List<Reward> chestRewards(RoyalChestRole role, RandomGenerator chestRandom, boolean holdsCatalyst) {
        List<Reward> rewards = new ArrayList<>(rollMaterials(role, chestRandom));
        rollAccessory(chestRandom).ifPresent(id -> rewards.add(new Reward(id, 1)));
        if (holdsCatalyst) rewards.add(new Reward(CATALYST_ID, 1));
        return rewards;
    }

    /** Splits rewards by each item's real maximum stack size; never truncates. Fails loudly past 54 slots. */
    public static List<Stack> split(List<Reward> rewards, ToIntFunction<String> maxStack) {
        List<Stack> stacks = new ArrayList<>();
        for (Reward reward : rewards) {
            int max = Math.max(1, maxStack.applyAsInt(reward.itemId()));
            for (int left = reward.count(); left > 0; left -= max) stacks.add(new Stack(reward.itemId(), Math.min(max, left)));
        }
        if (stacks.size() > SLOTS)
            throw new IllegalStateException("Royal chest rewards need " + stacks.size() + " stacks but a chest has " + SLOTS + " slots");
        return stacks;
    }
}
