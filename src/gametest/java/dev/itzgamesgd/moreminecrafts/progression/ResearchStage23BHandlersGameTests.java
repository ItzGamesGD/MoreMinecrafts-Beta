package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.fishing.FishTrapBlockEntity;
import dev.itzgamesgd.moreminecrafts.registry.ModBlocks;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Server integration tests for Stage 1 (`.24`, `.41`) and Stage 2 (`.26`, `.59`, `.60`, `.66`) handlers. */
public final class ResearchStage23BHandlersGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 120)
    public void autoFarmRecipeAndOperationAreGatedOnResearch24(GameTestHelper ctx) {
        var recipeKey = ResourceKey.create(Registries.RECIPE,
                ModIds.id("research/v3.research.24/level_1/auto_farm"));
        var recipe = ctx.getLevel().recipeAccess().byKey(recipeKey).orElse(null);
        ctx.assertTrue(recipe != null, "auto_farm research recipe must be loaded");

        ServerPlayer unpurchased = ctx.makeMockServerPlayerInLevel();
        ServerPlayer researched = ctx.makeMockServerPlayerInLevel();
        researched.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(researched, ResearchProgressState.empty().with("research.v3.research.24", 1));

        ctx.assertTrue(!ProgressionRecipeLocks.allowed(unpurchased, recipe),
                "unpurchased player cannot craft Auto Farm");
        ctx.assertTrue(ProgressionRecipeLocks.allowed(researched, recipe),
                "purchased v3.research.24 player can craft Auto Farm");

        BlockPos farmRel = new BlockPos(5, 2, 5);
        BlockPos farmPos = ctx.absolutePos(farmRel);
        BlockPos redstoneRel = new BlockPos(5, 2, 4);
        BlockPos cropRel = new BlockPos(5, 4, 5);
        ctx.setBlock(farmRel, ModBlocks.AUTO_FARM);
        ctx.setBlock(redstoneRel, Blocks.REDSTONE_BLOCK);
        ctx.setBlock(new BlockPos(5, 3, 5), Blocks.FARMLAND);
        ctx.setBlock(cropRel, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));

        AutoFarmBlockEntity farm = (AutoFarmBlockEntity) ctx.getLevel().getBlockEntity(farmPos);
        ctx.assertTrue(farm != null, "AutoFarmBlockEntity created");
        farm.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 1));

        AutoFarmBlock autoFarmBlock = (AutoFarmBlock) ModBlocks.AUTO_FARM;
        var hit = new BlockHitResult(Vec3.atCenterOf(farmPos), Direction.UP, farmPos, false);
        InteractionResult denied = autoFarmBlock.useWithoutItem(
                ModBlocks.AUTO_FARM.defaultBlockState(), ctx.getLevel(), farmPos, unpurchased, hit);
        ctx.assertTrue(denied == InteractionResult.FAIL, "unpurchased player cannot open Auto Farm");

        // Run real server ticks for 25 ticks without attribution: must remain idle.
        ctx.runAfterDelay(25, () -> {
            ctx.assertTrue(ctx.getBlockState(cropRel).getValue(CropBlock.AGE) == 7,
                    "unattributed Auto Farm does not harvest");

            InteractionResult allowed = autoFarmBlock.useWithoutItem(
                    ModBlocks.AUTO_FARM.defaultBlockState(), ctx.getLevel(), farmPos, researched, hit);
            ctx.assertTrue(allowed.consumesAction(), "researched player opens and attributes Auto Farm");

            ctx.runAfterDelay(45, () -> {
                ctx.assertTrue(ctx.getBlockState(cropRel).getValue(CropBlock.AGE) == 0,
                        "attributed Auto Farm harvests and replants wheat");

                AutoFarmBlockEntity reloaded = new AutoFarmBlockEntity(farmPos, ModBlocks.AUTO_FARM.defaultBlockState());
                loadAdditional(reloaded, saveAdditional(farm, ctx), ctx);
                ctx.assertTrue(reloaded.researchAttribution().attributedPlayer()
                                .equals(java.util.Optional.of(researched.getUUID())),
                        "Auto Farm attribution persists across save/load");
                ctx.succeed();
            });
        });
    }

    @GameTest
    public void slimeResearchPreventsSplittingAndAwardsDescendantLootAndXp(GameTestHelper ctx) {
        ServerPlayer researched = ctx.makeMockServerPlayerInLevel();
        researched.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(researched, ResearchProgressState.empty().with("research.v3.research.41", 1));

        Slime researchedSlime = ctx.spawn(EntityTypes.SLIME, new BlockPos(3, 2, 3));
        researchedSlime.setSize(4, true);
        AABB area = researchedSlime.getBoundingBox().inflate(6.0D);
        researchedSlime.hurtServer(ctx.getLevel(), researched.damageSources().playerAttack(researched), 200.0F);
        researchedSlime.remove(Entity.RemovalReason.KILLED);

        List<Slime> remaining = ctx.getLevel().getEntitiesOfClass(Slime.class, area);
        ctx.assertTrue(remaining.isEmpty(), "v3.research.41 prevents child slime spawning on death");
        int orbXp = ctx.getLevel().getEntitiesOfClass(ExperienceOrb.class, area).stream()
                .mapToInt(ExperienceOrb::getValue).sum();
        ctx.assertTrue(orbXp >= 8, "descendant XP orbs are awarded (expected >= 8, got " + orbXp + ")");

        // Baseline unpurchased killer still triggers vanilla splitting.
        ServerPlayer unpurchased = ctx.makeMockServerPlayerInLevel();
        Slime baselineSlime = ctx.spawn(EntityTypes.SLIME, new BlockPos(8, 2, 8));
        baselineSlime.setSize(4, true);
        AABB baselineArea = baselineSlime.getBoundingBox().inflate(6.0D);
        baselineSlime.hurtServer(ctx.getLevel(), unpurchased.damageSources().playerAttack(unpurchased), 200.0F);
        baselineSlime.remove(Entity.RemovalReason.KILLED);
        ctx.assertTrue(!ctx.getLevel().getEntitiesOfClass(Slime.class, baselineArea).isEmpty(),
                "unpurchased killer retains vanilla slime splitting");
        ctx.succeed();
    }

    @GameTest
    public void fishTrapImprovementReducesIntervalTo3m30sForAttributedOwner(GameTestHelper ctx) {
        ServerPlayer researched = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(researched, ResearchProgressState.empty().with("research.v3.research.26", 1));
        ServerPlayer unpurchased = ctx.makeMockServerPlayerInLevel();

        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), ModBlocks.FISH_TRAP);
        FishTrapBlockEntity trap = (FishTrapBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ctx.assertTrue(trap != null, "FishTrapBlockEntity exists");

        // Unpurchased player: 4200 ticks is not enough (needs 6000).
        trap.recordInteracted(unpurchased.getUUID());
        for (int t = 0; t < 4200; t++) {
            FishTrapBlockEntity.serverTick(ctx.getLevel(), pos, ModBlocks.FISH_TRAP.defaultBlockState(), trap);
        }
        ctx.assertTrue(trap.getItem(0).isEmpty(), "unpurchased owner does not produce at 4200 ticks");

        // Switch attribution to researched player: next tick hits >= 4200 and produces immediately.
        trap.recordInteracted(researched.getUUID());
        FishTrapBlockEntity.serverTick(ctx.getLevel(), pos, ModBlocks.FISH_TRAP.defaultBlockState(), trap);
        ctx.assertTrue(!trap.getItem(0).isEmpty(), "researched owner triggers 3m30s (4200 tick) production");

        FishTrapBlockEntity reloaded = new FishTrapBlockEntity(pos, ModBlocks.FISH_TRAP.defaultBlockState());
        loadAdditional(reloaded, saveAdditional(trap, ctx), ctx);
        ctx.assertTrue(reloaded.researchAttribution().attributedPlayer().equals(java.util.Optional.of(researched.getUUID())),
                "Fish Trap attribution persists across save/load");
        ctx.succeed();
    }

    @GameTest
    public void brewingStandAppliesSpeedFuelAndPotionStudyEffectsWithPersistence(GameTestHelper ctx) {
        ServerPlayer researched = ctx.makeMockServerPlayerInLevel();
        KnowledgeRuntime.updateState(researched, ResearchProgressState.empty()
                .with("research.v3.research.59", 3)
                .with("research.v3.research.60", 3)
                .with("research.v3.research.66", 1));

        BlockPos rel = new BlockPos(2, 2, 2);
        BlockPos pos = ctx.absolutePos(rel);
        ctx.setBlock(rel, Blocks.BREWING_STAND);
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) ctx.getLevel().getBlockEntity(pos);
        ctx.assertTrue(stand != null, "BrewingStandBlockEntity exists");

        BrewingResearchRuntime.interacted(stand, researched);
        stand.setItem(0, PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS));
        stand.setItem(3, new ItemStack(Items.REDSTONE));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER, 1));

        // First tick initializes brewTime = 400 and refills fuel to 25 (.66).
        BrewingStandBlockEntity.serverTick(ctx.getLevel(), pos, ctx.getBlockState(rel), stand);
        ctx.assertTrue(intField(stand, "fuel") == 25, "v3.research.66 grants 25 fuel uses per blaze powder");

        // Remaining 220 ticks complete L3 Brewing Basics (-45% of 400 = 220 ticks) and apply .60 Redstone bonus.
        for (int t = 0; t < 220; t++) {
            BrewingStandBlockEntity.serverTick(ctx.getLevel(), pos, ctx.getBlockState(rel), stand);
        }
        ctx.assertTrue(stand.getItem(3).isEmpty(), "L3 Brewing Basics completes brew in 220 ticks");
        ctx.assertTrue(intField(stand, "fuel") == 24, "one fuel unit deducted on brew");

        PotionContents contents = stand.getItem(0).get(DataComponents.POTION_CONTENTS);
        ctx.assertTrue(contents != null, "brewed potion keeps POTION_CONTENTS");
        int maxSpeedDuration = 0;
        for (var eff : contents.getAllEffects()) {
            if (eff.is(MobEffects.SPEED)) maxSpeedDuration = Math.max(maxSpeedDuration, eff.getDuration());
        }
        ctx.assertTrue(maxSpeedDuration == 11904,
                "v3.research.60 L3 extends 9600-tick redstone Speed to 11904 ticks (got " + maxSpeedDuration + ")");

        // Glowstone test: Speed I (3600) -> Strong Speed II (1800) mitigated by 30% -> 2340 ticks.
        stand.setItem(1, PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS));
        stand.setItem(3, new ItemStack(Items.GLOWSTONE_DUST));
        for (int t = 0; t < 221; t++) {
            BrewingStandBlockEntity.serverTick(ctx.getLevel(), pos, ctx.getBlockState(rel), stand);
        }
        PotionContents glowContents = stand.getItem(1).get(DataComponents.POTION_CONTENTS);
        int strongDuration = 0;
        for (var eff : glowContents.getAllEffects()) {
            if (eff.is(MobEffects.SPEED) && eff.getAmplifier() == 1) {
                strongDuration = Math.max(strongDuration, eff.getDuration());
            }
        }
        ctx.assertTrue(strongDuration == 2340,
                "v3.research.60 L3 mitigates glowstone duration penalty to 2340 ticks (got " + strongDuration + ")");

        BrewingStandBlockEntity reloaded = new BrewingStandBlockEntity(pos, Blocks.BREWING_STAND.defaultBlockState());
        loadAdditional(reloaded, saveAdditional(stand, ctx), ctx);
        ctx.assertTrue(((BrewingResearchStateAccess) reloaded).moreminecrafts$getResearchAttribution()
                        .attributedPlayer().equals(java.util.Optional.of(researched.getUUID())),
                "Brewing stand attribution survives save/load");
        ctx.succeed();
    }

    private static int intField(BrewingStandBlockEntity stand, String name) {
        try {
            Field f = BrewingStandBlockEntity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(stand);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static CompoundTag saveAdditional(BlockEntity entity, GameTestHelper ctx) {
        try {
            TagValueOutput output = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
            Method save = findDeclaredMethod(entity.getClass(), "saveAdditional", ValueOutput.class);
            save.setAccessible(true);
            save.invoke(entity, output);
            return output.buildResult();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void loadAdditional(BlockEntity entity, CompoundTag data, GameTestHelper ctx) {
        try {
            Method load = findDeclaredMethod(entity.getClass(), "loadAdditional", ValueInput.class);
            load.setAccessible(true);
            load.invoke(entity, TagValueInput.create(
                    ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), data));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Method findDeclaredMethod(Class<?> type, String name, Class<?> argument)
            throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name, argument);
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    @Override
    public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
