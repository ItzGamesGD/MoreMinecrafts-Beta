package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side entitlement and effects for V3 Soil Study. L1 and L3 use the recorded cultivator (L3
 * falls back to one nearby-researcher check when none is recorded); L2 uses the actual harvester.
 */
public final class SoilStudyRuntime {
    public static final String ID = "v3.research.16";
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> RESEARCH_SEEDS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("moreminecrafts", "research_seeds"));

    private SoilStudyRuntime() {}

    public static int level(ServerLevel level, UUID player) {
        return ResearchProgressLookup.researchLevel(level.getServer(), player, ID);
    }

    /** L1: only farmland with a recorded cultivator of at least L1 resists trampling. */
    public static boolean tramplingResisted(ServerLevel level, BlockPos farmland) {
        return FarmlandCultivators.cultivator(level, farmland)
                .map(player -> SoilStudyRules.tramplingResisted(level(level, player))).orElse(false);
    }

    /** L3: recorded cultivator entitlement, else at most one nearby-researcher fallback. */
    public static boolean level3Applies(ServerLevel level, BlockPos farmland) {
        Optional<UUID> cultivator = FarmlandCultivators.cultivator(level, farmland);
        if (cultivator.isPresent()) return level(level, cultivator.get()) >= 3;
        Vec3 center = Vec3.atCenterOf(farmland);
        double radiusSquared = (double) SoilStudyRules.FALLBACK_RESEARCHER_RADIUS
                * SoilStudyRules.FALLBACK_RESEARCHER_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && player.position().distanceToSqr(center) <= radiusSquared
                    && level(level, player.getUUID()) >= 3) return true;
        }
        return false;
    }

    /** Extra water search for qualifying farmland after vanilla's reach-4 search found nothing. */
    public static boolean extendedHydration(ServerLevel level, BlockPos farmland) {
        return level3Applies(level, farmland) && waterInExtendedRing(level, farmland);
    }

    /** Scans only columns at horizontal distance 5..6; loaded chunks only, so nothing is force-loaded. */
    static boolean waterInExtendedRing(ServerLevel level, BlockPos farmland) {
        int reach = SoilStudyRules.hydrationReach(3);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) <= SoilStudyRules.VANILLA_HYDRATION_REACH) continue;
                int x = farmland.getX() + dx;
                int z = farmland.getZ() + dz;
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                for (int dy = 0; dy <= 1; dy++) {
                    cursor.set(x, farmland.getY() + dy, z);
                    if (level.getBlockState(cursor).getFluidState().is(FluidTags.WATER)) return true;
                }
            }
        }
        return false;
    }

    /** Relative natural-growth bonus for a crop planted on qualifying farmland (0 when none). */
    public static double cropGrowthBonus(ServerLevel level, BlockPos crop) {
        BlockPos soil = crop.below();
        if (!(level.getBlockState(soil).getBlock() instanceof FarmlandBlock)) return 0.0D;
        return level3Applies(level, soil) ? SoilStudyRules.L3_GROWTH_RELATIVE_BONUS : 0.0D;
    }

    /** L2: the harvester's own entitlement; the bonus derives from vanilla loot, never from other bonuses. */
    public static List<ItemStack> applySeedYield(ServerPlayer harvester, BlockState state,
            List<ItemStack> vanillaDrops, List<ItemStack> drops) {
        return applySeedYield(harvester, state, vanillaDrops, drops, harvester.getRandom()::nextDouble);
    }

    static List<ItemStack> applySeedYield(ServerPlayer harvester, BlockState state, List<ItemStack> vanillaDrops,
            List<ItemStack> drops, DoubleSupplier random) {
        if (harvester.isCreative() || !(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return drops;
        return seedYieldDrops(ResearchBatch2Runtime.level(harvester, ID), vanillaDrops, drops, random);
    }

    static List<ItemStack> seedYieldDrops(int level, List<ItemStack> vanillaDrops, List<ItemStack> drops,
            DoubleSupplier random) {
        int eligible = 0;
        ItemStack template = ItemStack.EMPTY;
        for (ItemStack stack : vanillaDrops) {
            if (!stack.is(RESEARCH_SEEDS) || stack.getCount() <= 0) continue;
            eligible += stack.getCount();
            if (template.isEmpty()) template = stack;
        }
        if (level < 2 || eligible == 0) return drops;
        int bonus = SoilStudyRules.seedYieldBonus(eligible, level, random.getAsDouble());
        if (bonus <= 0) return drops;
        List<ItemStack> result = new ArrayList<>(drops.size() + 1);
        result.addAll(drops);
        result.add(template.copyWithCount(bonus));
        return result;
    }
}
