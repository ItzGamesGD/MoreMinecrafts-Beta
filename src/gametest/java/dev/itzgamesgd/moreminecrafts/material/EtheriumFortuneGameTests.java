package dev.itzgamesgd.moreminecrafts.material;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.registry.ModBlocks;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Evaluates the real production loot tables (Block.getDrops) for etherium ore under Fortune and Silk Touch. */
public final class EtheriumFortuneGameTests {
    private static final int SAMPLES = 40_000;

    private static ItemStack pickaxe(GameTestHelper ctx, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, int level) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        if (level > 0) {
            var holder = ctx.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
            EnchantmentHelper.updateEnchantments(tool, enchantments -> enchantments.set(holder, level));
        }
        return tool;
    }

    private static int etherium(java.util.List<ItemStack> drops) {
        int total = 0;
        for (ItemStack drop : drops) if (drop.is(ModItems.ETHERIUM)) total += drop.getCount();
        return total;
    }

    private void check(GameTestHelper ctx, Block ore) {
        BlockPos pos = ctx.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = ore.defaultBlockState();
        double[] vanillaMean = {0, 1.0 / 3, 3.0 / 4, 6.0 / 5};
        for (int level = 0; level <= 3; level++) {
            ItemStack tool = pickaxe(ctx, Enchantments.FORTUNE, level);
            long extras = 0;
            int min = Integer.MAX_VALUE;
            int[] histogram = new int[5];
            for (int i = 0; i < SAMPLES; i++) {
                int count = etherium(Block.getDrops(state, ctx.getLevel(), pos, null, null, tool));
                min = Math.min(min, count);
                histogram[Math.min(count, 4)]++;
                extras += count - 1;
            }
            double mean = (double) extras / SAMPLES;
            MoreMinecrafts.LOGGER.info("ETHERIUM_FORTUNE_STATS ore={} fortune={} samples={} mean_extra={} vanilla_mean_extra={} histogram_total_1_2_3_4plus={}",
                    ore, level, SAMPLES, mean, vanillaMean[level], java.util.Arrays.toString(java.util.Arrays.copyOfRange(histogram, 1, 5)));
            ctx.assertTrue(min >= 1, "Fortune " + level + " must always drop at least the base 1 etherium (min=" + min + ")");
            ctx.assertTrue(Math.abs(mean - 0.2 * vanillaMean[level]) < 0.02,
                    "Fortune " + level + " mean extras " + mean + " should be 20% of vanilla " + vanillaMean[level]);
        }
        ItemStack silk = pickaxe(ctx, Enchantments.SILK_TOUCH, 1);
        var drops = Block.getDrops(state, ctx.getLevel(), pos, null, null, silk);
        ctx.assertTrue(drops.size() == 1 && drops.getFirst().is(ore.asItem()), "Silk Touch drops the ore block itself");
    }

    @GameTest
    public void stoneEtheriumOreFortuneKeepsBaseDropAndThinsExtras(GameTestHelper ctx) {
        check(ctx, ModBlocks.ETHERIUM_ORE);
        ctx.succeed();
    }

    @GameTest
    public void deepslateEtheriumOreFortuneKeepsBaseDropAndThinsExtras(GameTestHelper ctx) {
        check(ctx, ModBlocks.DEEPSLATE_ETHERIUM_ORE);
        ctx.succeed();
    }
}
