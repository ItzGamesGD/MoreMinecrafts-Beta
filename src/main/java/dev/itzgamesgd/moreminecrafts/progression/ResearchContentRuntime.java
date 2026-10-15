package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Server-authoritative, one-pass drop extensions for Batch 1 harvest research. */
public final class ResearchContentRuntime {
    private static final TagKey<net.minecraft.world.level.block.Block> RESEARCH_ORES = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath("moreminecrafts", "research_ores"));

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
        if (player == null || player.isCreative() || baseDrops.isEmpty()) return baseDrops;
        List<ItemStack> drops = copyDrops(baseDrops);

        if (state.is(RESEARCH_ORES)) {
            addGatheringBonus(player, "v3.research.01", "ore_extra_drops", drops,
                    stack -> !stack.isEmpty(), random);
        }
        if (state.is(BlockTags.LOGS)) {
            addGatheringBonus(player, "v3.research.02", "lumbering_extra_drops", drops,
                    stack -> stack.is(ItemTags.LOGS), random);
        }
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
        if (chance <= 0.0D || drops.stream().noneMatch(stack -> stack.is(ItemTags.SEEDS) && stack.getCount() > 0)
                || !ResearchContentRules.extraItemSucceeds(random.getAsDouble(), chance)) return;
        drops.stream().filter(stack -> stack.is(ItemTags.SEEDS) && stack.getCount() > 0)
                .findFirst().ifPresent(stack -> drops.add(stack.copyWithCount(1)));
    }

    public static boolean isResearchOre(BlockState state) {
        return state.is(RESEARCH_ORES);
    }
}
