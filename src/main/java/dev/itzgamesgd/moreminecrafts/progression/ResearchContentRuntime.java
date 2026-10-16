package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Server-authoritative, one-pass drop extensions for Batch 1 harvest research. */
public final class ResearchContentRuntime {
    private static final TagKey<net.minecraft.world.level.block.Block> RESEARCH_ORES = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath("moreminecrafts", "research_ores"));
    private static final TagKey<net.minecraft.world.item.Item> RESEARCH_SEEDS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("moreminecrafts", "research_seeds"));

    private ResearchContentRuntime() {}

    public static List<ItemStack> applyBlockDropResearch(ServerPlayer player, BlockState state,
                                                          List<ItemStack> baseDrops) {
        if (player == null) return baseDrops;
        return applyBlockDropResearch(player, state, baseDrops, player.getRandom()::nextDouble);
    }

    static List<ItemStack> applyBlockDropResearch(ServerPlayer player, BlockState state,
                                                   List<ItemStack> baseDrops,
                                                   java.util.function.DoubleSupplier random) {
        if (player == null || player.isCreative() || baseDrops.isEmpty()) return baseDrops;
        List<ItemStack> seedAdjusted = applySeedHarvestResearch(player, state, baseDrops, random);
        return applyOreAndLogResearch(player, state, seedAdjusted, random);
    }

    public static List<ItemStack> applySeedHarvestResearch(ServerPlayer player, BlockState state,
                                                            List<ItemStack> baseDrops) {
        if (player == null) return baseDrops;
        return applySeedHarvestResearch(player, state, baseDrops, player.getRandom()::nextDouble);
    }

    private static List<ItemStack> applySeedHarvestResearch(ServerPlayer player, BlockState state,
                                                             List<ItemStack> baseDrops,
                                                             java.util.function.DoubleSupplier random) {
        if (player == null || player.isCreative() || baseDrops.isEmpty()) return baseDrops;
        List<ItemStack> drops = copyDrops(baseDrops);
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state))
            addSeedBonus(player, drops, random);
        return drops;
    }

    public static List<ItemStack> applyOreAndLogResearch(ServerPlayer player, BlockState state,
                                                          List<ItemStack> baseDrops) {
        if (player == null) return baseDrops;
        return applyOreAndLogResearch(player, state, baseDrops, player.getRandom()::nextDouble);
    }

    private static List<ItemStack> applyOreAndLogResearch(ServerPlayer player, BlockState state,
                                                           List<ItemStack> baseDrops,
                                                           java.util.function.DoubleSupplier random) {
        // Kept for deterministic rules tests and call sites combining these independent effects.
        return applyLumberingResearch(player, state, applyOreResearch(player, state, baseDrops, random), random);
    }

    /** Run only after Auto Smelt, so Ore Mining duplicates the final processed loot. */
    public static List<ItemStack> applyOreResearch(ServerPlayer player, BlockState state,
                                                    List<ItemStack> baseDrops) {
        if (player == null) return baseDrops;
        return applyOreResearch(player, state, baseDrops, player.getRandom()::nextDouble);
    }

    private static List<ItemStack> applyOreResearch(ServerPlayer player, BlockState state,
                                                     List<ItemStack> baseDrops,
                                                     java.util.function.DoubleSupplier random) {
        if (player == null || player.isCreative() || baseDrops.isEmpty() || !state.is(RESEARCH_ORES))
            return baseDrops;
        List<ItemStack> drops = copyDrops(baseDrops);
        addGatheringBonus(player, "v3.research.01", "ore_extra_drops", drops,
                stack -> !stack.isEmpty(), random);
        return drops;
    }

    /** Run before the one existing enchantment pass so Lumbering bonuses share its processing. */
    public static List<ItemStack> applyLumberingResearch(ServerPlayer player, BlockState state,
                                                          List<ItemStack> baseDrops) {
        if (player == null) return baseDrops;
        return applyLumberingResearch(player, state, baseDrops, player.getRandom()::nextDouble);
    }

    private static List<ItemStack> applyLumberingResearch(ServerPlayer player, BlockState state,
                                                           List<ItemStack> baseDrops,
                                                           java.util.function.DoubleSupplier random) {
        if (player == null || player.isCreative() || baseDrops.isEmpty() || !state.is(BlockTags.LOGS))
            return baseDrops;
        List<ItemStack> drops = copyDrops(baseDrops);
        addGatheringBonus(player, "v3.research.02", "lumbering_extra_drops", drops,
                stack -> stack.is(net.minecraft.tags.ItemTags.LOGS), random);
        return drops;
    }

    private static List<ItemStack> copyDrops(List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>(drops.size() + 1);
        for (ItemStack stack : drops) result.add(stack.copy());
        return result;
    }

    private static void addGatheringBonus(ServerPlayer player, String researchId, String effectId,
                                           List<ItemStack> drops,
                                           java.util.function.Predicate<ItemStack> eligible,
                                           java.util.function.DoubleSupplier random) {
        int level = ResearchRewards.level(player, researchId, effectId);
        int maximum = ResearchContentRules.maximumGatheringExtra(level);
        if (maximum == 0) return;
        if (!ResearchContentRules.extraItemSucceeds(random.getAsDouble(),
                ResearchContentRules.gatheringChance(level))) return;
        int bonus = ResearchContentRules.gatheringExtraCountAfterSuccess(level, random.getAsDouble());
        drops.stream().filter(stack -> eligible.test(stack) && stack.getCount() > 0)
                .findFirst().ifPresent(stack -> drops.add(stack.copyWithCount(bonus)));
    }

    private static void addSeedBonus(ServerPlayer player, List<ItemStack> drops,
                                     java.util.function.DoubleSupplier random) {
        int level = ResearchRewards.level(player, "v3.research.15", "seed_extra_drops");
        double chance = ResearchContentRules.seedChance(level);
        if (chance <= 0.0D || drops.stream().noneMatch(stack -> stack.is(RESEARCH_SEEDS) && stack.getCount() > 0)
                || !ResearchContentRules.extraItemSucceeds(random.getAsDouble(), chance)) return;
        drops.stream().filter(stack -> stack.is(RESEARCH_SEEDS) && stack.getCount() > 0)
                .findFirst().ifPresent(stack -> drops.add(stack.copyWithCount(1)));
    }

    public static boolean isResearchOre(BlockState state) {
        return state.is(RESEARCH_ORES);
    }
}
