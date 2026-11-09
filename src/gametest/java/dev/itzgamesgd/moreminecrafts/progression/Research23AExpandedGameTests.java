package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchAttribution;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchRuntime;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchStateAccess;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Event-level checks for the six selected paid effects. */
public final class Research23AExpandedGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void purchasedFuelAndCookingLevelsAlterPermanentFurnaceTicks(GameTestHelper ctx) {
        BlockPos relative = new BlockPos(2, 2, 2);
        BlockPos pos = ctx.absolutePos(relative);
        ctx.setBlock(relative, Blocks.FURNACE);
        var furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        var player = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research.v3.research.06", 3)
                .with("research.v3.research.11", 3));
        FurnaceResearchAttribution.opened(furnace, player);
        ctx.assertTrue(ResearchProgressLookup.researchLevel(ctx.getLevel().getServer(),
                player.getUUID(), "v3.research.06") == 3,
                "the furnace owner resolves a purchased level from the live attachment");
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        furnace.setItem(1, new ItemStack(Items.COAL));
        // Compare a bounded ten-tick interval after vanilla has initialized its cook time.
        AbstractFurnaceBlockEntity.serverTick(ctx.getLevel(), pos,
                ctx.getLevel().getBlockState(pos), furnace);
        int timerBefore = field(furnace, "cookingTimer");
        for (int tick = 0; tick < 10; tick++)
            AbstractFurnaceBlockEntity.serverTick(ctx.getLevel(), pos,
                    ctx.getLevel().getBlockState(pos), furnace);
        ctx.assertTrue(field(furnace, "litTotalTime") > 1600,
                "Fuel Saving stretches the next consumed coal's burn duration");
        ctx.assertTrue(field(furnace, "cookingTimer") - timerBefore == 13,
                "Faster Smelting adds exactly three cooking ticks over ten baseline ticks: observed "
                        + (field(furnace, "cookingTimer") - timerBefore));
        ctx.assertTrue(((FurnaceResearchStateAccess) furnace).moreminecrafts$getResearchState()
                        .fuelSavingRemainder() > 0,
                "the fractional fuel remainder is kept on the furnace");
        ctx.succeed();
    }

    @GameTest
    public void precisionSmeltingReservesOutputSpaceAndExcludesNonMinerals(GameTestHelper ctx) {
        BlockPos relative = new BlockPos(2, 2, 2);
        BlockPos pos = ctx.absolutePos(relative);
        ctx.setBlock(relative, Blocks.FURNACE);
        var furnace = (AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(pos);
        var player = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research.v3.research.07", 5));
        FurnaceResearchAttribution.interacted(furnace, player);
        ctx.assertTrue(FurnaceResearchRuntime.eligibleMineral(furnace,
                new ItemStack(Items.RAW_IRON), new ItemStack(Items.IRON_INGOT)),
                "tagged raw iron to ingot is eligible");
        ctx.assertTrue(!FurnaceResearchRuntime.eligibleMineral(furnace,
                new ItemStack(Items.RAW_IRON), new ItemStack(Items.STONE)),
                "nonmineral output is excluded");
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 63));
        AbstractFurnaceBlockEntity.serverTick(ctx.getLevel(), pos,
                ctx.getLevel().getBlockState(pos), furnace);
        ctx.assertTrue(furnace.getItem(0).getCount() == 1 && furnace.getItem(2).getCount() == 63
                        && field(furnace, "litTotalTime") == 0,
                "possible bonus overflow pauses the recipe without consuming fuel or input");
        ctx.succeed();
    }

    @GameTest
    public void ecologyBonusesResolveOnlyFromTheAttackerAndTaggedMobFamily(GameTestHelper ctx) {
        var attacker = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(attacker, ResearchProgressState.empty()
                .with("research.v3.research.44", 3).with("knowledge.49", 1)
                .with("research.v3.research.45", 3).with("knowledge.50", 1)
                .with("research.v3.research.47", 3).with("knowledge.52", 1));
        var hoglin = ctx.spawn(EntityTypes.HOGLIN, new BlockPos(2, 2, 2));
        var drowned = ctx.spawn(EntityTypes.DROWNED, new BlockPos(3, 2, 2));
        var wither = ctx.spawn(EntityTypes.WITHER, new BlockPos(4, 2, 2));
        ctx.assertTrue(Math.abs(ResearchEcologyDamage.bonus(attacker, hoglin) - 0.13) < 1.0E-8,
                "Hoglin paid level three and its separate knowledge award compose once");
        ctx.assertTrue(Math.abs(ResearchEcologyDamage.bonus(attacker, drowned) - 0.13) < 1.0E-8,
                "aquatic-hostile paid level three and its knowledge award compose once");
        ctx.assertTrue(Math.abs(ResearchEcologyDamage.bonus(attacker, wither) - 0.12) < 1.0E-8,
                "boss paid level three and its knowledge award compose once");
        var unowned = ctx.makeMockServerPlayerInLevel();
        var baselineHoglin = ctx.spawn(EntityTypes.HOGLIN, new BlockPos(5, 2, 2));
        float beforeResearched = hoglin.getHealth();
        float beforeBaseline = baselineHoglin.getHealth();
        hoglin.hurtServer(ctx.getLevel(), attacker.damageSources().playerAttack(attacker), 5.0F);
        baselineHoglin.hurtServer(ctx.getLevel(), unowned.damageSources().playerAttack(unowned), 5.0F);
        float researchedDamage = beforeResearched - hoglin.getHealth();
        float baselineDamage = beforeBaseline - baselineHoglin.getHealth();
        ctx.assertTrue(researchedDamage > baselineDamage * 1.12F
                        && researchedDamage < baselineDamage * 1.14F,
                "the actual damage hook applies the paid plus knowledge Hoglin bonus exactly once");
        ctx.succeed();
    }

    private static int field(AbstractFurnaceBlockEntity furnace, String name) {
        try {
            Field field = AbstractFurnaceBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(furnace);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
