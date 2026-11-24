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

    static List<ItemStack> applyOreResearch(ServerPlayer player, BlockState state,
                                                     List<ItemStack> baseDrops,
                                                     java.util.function.DoubleSupplier random) {
        if (player == null || player.isCreative()) return baseDrops;
        return oreDropsAtPaidLevel(state, baseDrops,
                ResearchRewards.level(player, "v3.research.01", "ore_extra_drops"), random);
    }

    /** Pure deterministic stage for testing the drop rule independently of GameTest mock abilities. */
    static List<ItemStack> oreDropsAtPaidLevel(BlockState state, List<ItemStack> baseDrops,
                                                int paidLevel, java.util.function.DoubleSupplier random) {
        if (baseDrops.isEmpty() || !state.is(RESEARCH_ORES)) return baseDrops;
        List<ItemStack> drops = copyDrops(baseDrops);
        addGatheringBonus(paidLevel, drops, stack -> !stack.isEmpty(), random);
        return drops;
    }

    /** Run before the one existing enchantment pass so Lumbering bonuses share its processing. */
    public static List<ItemStack> applyLumberingResearch(ServerPlayer player, BlockState state,
                                                          List<ItemStack> baseDrops) {
        if (player == null) return baseDrops;
        return applyLumberingResearch(player, state, baseDrops, player.getRandom()::nextDouble);
    }

    static List<ItemStack> applyLumberingResearch(ServerPlayer player, BlockState state,
                                                           List<ItemStack> baseDrops,
                                                           java.util.function.DoubleSupplier random) {
        if (player == null || player.isCreative()) return baseDrops;
        return lumberingDropsAtPaidLevel(state, baseDrops,
                ResearchRewards.level(player, "v3.research.02", "lumbering_extra_drops"), random);
    }

    /** Pure deterministic stage: log extras are created before the enchantment pass. */
    static List<ItemStack> lumberingDropsAtPaidLevel(BlockState state, List<ItemStack> baseDrops,
                                                      int paidLevel, java.util.function.DoubleSupplier random) {
        if (baseDrops.isEmpty() || !state.is(BlockTags.LOGS)) return baseDrops;
        List<ItemStack> drops = copyDrops(baseDrops);
        addGatheringBonus(paidLevel, drops, stack -> stack.is(net.minecraft.tags.ItemTags.LOGS), random);
        return drops;
    }

    private static List<ItemStack> copyDrops(List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>(drops.size() + 1);
        for (ItemStack stack : drops) result.add(stack.copy());
        return result;
    }

    private static void addGatheringBonus(int level, List<ItemStack> drops,
                                           java.util.function.Predicate<ItemStack> eligible,
                                           java.util.function.DoubleSupplier random) {
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

    private static final TagKey<net.minecraft.world.item.Item> BREWING_INGREDIENTS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("moreminecrafts", "research/brewing_ingredients"));
    private static final ThreadLocal<Boolean> IN_MAGICAL_MOB_BONUS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static List<ItemStack> applyCropCultivationResearch(ServerPlayer player, BlockState state,
                                                                List<ItemStack> baseDrops) {
        if (player == null) return baseDrops;
        return applyCropCultivationResearch(player, state, baseDrops, player.getRandom()::nextDouble);
    }

    static List<ItemStack> applyCropCultivationResearch(ServerPlayer player, BlockState state,
                                                         List<ItemStack> baseDrops,
                                                         java.util.function.DoubleSupplier random) {
        if (player == null || player.isCreative() || baseDrops.isEmpty()) return baseDrops;
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return baseDrops;
        net.minecraft.world.item.Item produce = stapleCropProduce(state.getBlock());
        if (produce == null) return baseDrops;
        int level = ResearchRewards.level(player, "v3.research.14", "crop_cultivation_extra");
        int extra = ResearchStage3HarvestRules.cropCultivationExtra(level, random.getAsDouble());
        if (extra <= 0) return baseDrops;
        List<ItemStack> drops = copyDrops(baseDrops);
        drops.add(new ItemStack(produce, extra));
        return drops;
    }

    private static net.minecraft.world.item.Item stapleCropProduce(net.minecraft.world.level.block.Block block) {
        if (block == net.minecraft.world.level.block.Blocks.WHEAT) return net.minecraft.world.item.Items.WHEAT;
        if (block == net.minecraft.world.level.block.Blocks.CARROTS) return net.minecraft.world.item.Items.CARROT;
        if (block == net.minecraft.world.level.block.Blocks.POTATOES) return net.minecraft.world.item.Items.POTATO;
        if (block == net.minecraft.world.level.block.Blocks.BEETROOTS) return net.minecraft.world.item.Items.BEETROOT;
        return null;
    }

    public static List<ItemStack> applyMagicalMaterialBlockResearch(ServerPlayer player, BlockState state,
                                                                     List<ItemStack> baseDrops) {
        if (player == null) return baseDrops;
        return applyMagicalMaterialBlockResearch(player, state, baseDrops, player.getRandom()::nextDouble);
    }

    static List<ItemStack> applyMagicalMaterialBlockResearch(ServerPlayer player, BlockState state,
                                                              List<ItemStack> baseDrops,
                                                              java.util.function.DoubleSupplier random) {
        if (player == null || player.isCreative() || baseDrops.isEmpty()) return baseDrops;
        int level = ResearchRewards.level(player, "v3.research.63", "magical_material_extra");
        if (level <= 0) return baseDrops;
        ItemStack eligible = baseDrops.stream()
                .filter(stack -> !stack.isEmpty() && stack.is(BREWING_INGREDIENTS))
                .findFirst().orElse(ItemStack.EMPTY);
        if (eligible.isEmpty() || !ResearchStage3HarvestRules.magicalMaterialExtraSucceeds(level, random.getAsDouble())) {
            return baseDrops;
        }
        List<ItemStack> drops = copyDrops(baseDrops);
        drops.add(eligible.copyWithCount(1));
        return drops;
    }

    public static void tryBonusMagicalMobDrop(net.minecraft.world.entity.Entity sourceEntity,
                                              net.minecraft.server.level.ServerLevel level,
                                              ItemStack droppedStack) {
        if (Boolean.TRUE.equals(IN_MAGICAL_MOB_BONUS.get())) return;
        if (droppedStack == null || droppedStack.isEmpty() || !droppedStack.is(BREWING_INGREDIENTS)) return;
        if (!(sourceEntity instanceof net.minecraft.world.entity.LivingEntity victim) || !victim.isDeadOrDying()) return;
        if (!(victim.getKillCredit() instanceof ServerPlayer killer) || killer.isCreative()) return;
        int level63 = ResearchRewards.level(killer, "v3.research.63", "magical_material_extra");
        if (!ResearchStage3HarvestRules.magicalMaterialExtraSucceeds(level63, killer.getRandom().nextDouble())) return;
        IN_MAGICAL_MOB_BONUS.set(Boolean.TRUE);
        try {
            victim.spawnAtLocation(level, droppedStack.copyWithCount(1));
        } finally {
            IN_MAGICAL_MOB_BONUS.set(Boolean.FALSE);
        }
    }

    public static boolean isResearchOre(BlockState state) {
        return state.is(RESEARCH_ORES);
    }
}
