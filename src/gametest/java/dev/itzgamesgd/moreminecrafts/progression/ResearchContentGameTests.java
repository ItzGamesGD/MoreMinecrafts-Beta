package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Event-level coverage for Stage 23-A research effects and purchase gates. */
public final class ResearchContentGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void dropResearchIsPurchaseGatedAndUsesTheProcessedDrop(GameTestHelper ctx) {
        // makeMockServerPlayerInLevel() is always creative, and research drop bonuses never apply in creative.
        ServerPlayer player = (ServerPlayer) ctx.makeMockServerPlayer(GameType.SURVIVAL);
        ctx.assertTrue(!player.isCreative(), "the drop fixture uses a genuine survival player");
        var ore = List.of(new ItemStack(Items.DIAMOND));
        var unowned = ResearchContentRuntime.applyBlockDropResearch(
                player, Blocks.DIAMOND_ORE.defaultBlockState(), ore, () -> 0.0D);
        ctx.assertTrue(unowned.size() == 1 && unowned.getFirst().getCount() == 1,
                "an unowned player receives only the post-enchantment base ore result");

        // This survival mock has no network connection, so seed the stored state without a client sync.
        player.setAttached(KnowledgeRuntime.STATE, KnowledgeRuntime.state(player)
                .with("research.v3.research.01", 5)
                .with("research.v3.research.02", 1)
                .with("research.v3.research.15", 3));
        // Approved Ore Mining L5: 30% proc, then an amount chosen uniformly from 1 through 3 (proc roll, amount roll).
        var oreDrops = ResearchContentRuntime.applyBlockDropResearch(player,
                Blocks.DIAMOND_ORE.defaultBlockState(), List.of(new ItemStack(Items.DIAMOND)), rolls(0.0D, 0.999D));
        ctx.assertTrue(oreDrops.size() == 2 && oreDrops.get(1).is(Items.DIAMOND)
                        && oreDrops.get(1).getCount() == 3,
                "Ore Mining copies the final Fortune/Silk Touch/Auto Smelt product with the approved max count");
        var minimumOreDrops = ResearchContentRuntime.applyBlockDropResearch(player,
                Blocks.DIAMOND_ORE.defaultBlockState(), List.of(new ItemStack(Items.DIAMOND)), rolls(0.0D, 0.0D));
        ctx.assertTrue(minimumOreDrops.size() == 2 && minimumOreDrops.get(1).getCount() == 1,
                "the lowest amount roll grants one extra item (uniform 1 through the level maximum)");
        var missedOreDrops = ResearchContentRuntime.applyBlockDropResearch(player,
                Blocks.DIAMOND_ORE.defaultBlockState(), List.of(new ItemStack(Items.DIAMOND)), rolls(0.30D));
        ctx.assertTrue(missedOreDrops.size() == 1, "a proc roll at the 30% boundary grants no extra item");

        var logDrops = ResearchContentRuntime.applyBlockDropResearch(player,
                Blocks.OAK_LOG.defaultBlockState(), List.of(new ItemStack(Items.OAK_LOG)), () -> 0.0D);
        ctx.assertTrue(logDrops.size() == 2 && logDrops.get(1).is(Items.OAK_LOG)
                        && logDrops.get(1).getCount() == 1,
                "Lumbering adds log items without breaking another log block");

        var seedDrops = ResearchContentRuntime.applyBlockDropResearch(player,
                Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7),
                List.of(new ItemStack(Items.WHEAT_SEEDS)), () -> 0.0D);
        ctx.assertTrue(seedDrops.size() == 2 && seedDrops.get(1).is(Items.WHEAT_SEEDS),
                "Seed Study adds one eligible seed and does not touch crop yield logic");
        ctx.succeed();
    }

    @GameTest
    public void woodProcessingChangesTheAuthoritativeRecipeResultOnce(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(player,
                KnowledgeRuntime.state(player).with("research.v3.research.09", 2));
        CraftingMenu menu = new CraftingMenu(1, player.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(ctx.getLevel(),
                        ctx.absolutePos(new BlockPos(1, 1, 1))));
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
        menu.slotsChanged(menu.getSlot(1).container);
        ctx.assertTrue(menu.getSlot(0).getItem().is(Items.OAK_PLANKS)
                        && menu.getSlot(0).getItem().getCount() == 6,
                "level two adds two to the vanilla four-plank recipe result");
        menu.quickMoveStack(player, 0);
        ctx.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS) == 6,
                "shift-click transfers the augmented result exactly once");
        ctx.assertTrue(menu.getSlot(1).getItem().isEmpty(), "one log is consumed once");
        ctx.succeed();
    }

    @GameTest
    public void miningConservationRunsOnlyInsideTheEffectiveBlockBreakScope(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(player,
                KnowledgeRuntime.state(player).with("research.v3.research.03", 5));
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pickaxe);
        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        player.getRandom().setSeed(314159L);
        int breaks = 32;
        for (int i = 0; i < breaks; i++) {
            ctx.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            ctx.assertTrue(player.gameMode.destroyBlock(pos), "effective pickaxe break succeeds");
        }
        ctx.assertTrue(pickaxe.getDamageValue() > 0 && pickaxe.getDamageValue() < breaks,
                "mining durability is independently prevented without double-consuming damage");
        ctx.succeed();
    }

    @GameTest
    public void equipmentMaintenanceImprovesOnlyAnActualMaterialRepair(GameTestHelper ctx) {
        ServerPlayer baseline = ctx.makeMockServerPlayerInLevel();
        baseline.setGameMode(GameType.SURVIVAL);
        AnvilMenu baselineMenu = new AnvilMenu(1, baseline.getInventory());
        ((T1AnvilModes) baselineMenu).moreminecrafts$button(baseline, 1);
        ItemStack baselineSword = new ItemStack(Items.DIAMOND_SWORD);
        baselineSword.setDamageValue(1000);
        baselineMenu.getSlot(0).set(baselineSword);
        baselineMenu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        baselineMenu.createResult();
        ctx.assertTrue(baselineMenu.getSlot(2).hasItem(), "vanilla material repair has a result");
        int baselineDamage = baselineMenu.getSlot(2).getItem().getDamageValue();

        ServerPlayer researcher = ctx.makeMockServerPlayerInLevel();
        researcher.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(researcher,
                KnowledgeRuntime.state(researcher).with("research.v3.research.10", 1));
        AnvilMenu researchMenu = new AnvilMenu(1, researcher.getInventory());
        ((T1AnvilModes) researchMenu).moreminecrafts$button(researcher, 1);
        ItemStack researchSword = new ItemStack(Items.DIAMOND_SWORD);
        researchSword.setDamageValue(1000);
        researchMenu.getSlot(0).set(researchSword);
        researchMenu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        researchMenu.createResult();
        ctx.assertTrue(researchMenu.getSlot(2).hasItem()
                        && researchMenu.getSlot(2).getItem().getDamageValue() < baselineDamage,
                "Equipment Maintenance adds durability only to a successful material repair");
        ctx.succeed();
    }

    @GameTest
    public void beekeepingAddsOneCombOnlyAfterAFullHiveShearsHarvest(GameTestHelper ctx)
            throws ReflectiveOperationException {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(player,
                KnowledgeRuntime.state(player).with("research.v3.research.18", 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        long seed = 0;
        do {
            player.getRandom().setSeed(seed++);
        } while (player.getRandom().nextDouble() >= 0.60D);
        player.getRandom().setSeed(seed - 1);

        BlockPos pos = ctx.absolutePos(new BlockPos(2, 2, 2));
        var before = Blocks.BEEHIVE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 5);
        ctx.getLevel().setBlock(pos, before, 3);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var useItemOn = Blocks.BEEHIVE.getClass().getDeclaredMethod("useItemOn",
                ItemStack.class, net.minecraft.world.level.block.state.BlockState.class,
                net.minecraft.world.level.Level.class, BlockPos.class,
                net.minecraft.world.entity.player.Player.class, InteractionHand.class, BlockHitResult.class);
        useItemOn.setAccessible(true);
        InteractionResult result = (InteractionResult) useItemOn.invoke(Blocks.BEEHIVE,
                player.getMainHandItem(), before, ctx.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        ctx.assertTrue(result.consumesAction(), "vanilla handles the full-hive shears action");
        int honeycombCount = ctx.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                        pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2)).stream()
                .filter(item -> item.getItem().is(Items.HONEYCOMB))
                .mapToInt(item -> item.getItem().getCount()).sum();
        ctx.assertTrue(honeycombCount >= 4,
                "the full-hive activation yields vanilla combs plus one purchased bonus");
        ctx.assertTrue(ctx.getLevel().getBlockState(pos)
                        .getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) == 0,
                "bonus grant does not re-run or preserve the vanilla harvest");
        ctx.succeed();
    }

    @GameTest
    public void livestockAddsOnlyThePurchasedNumberAfterVanillaBreeding(GameTestHelper ctx) {
        ServerPlayer breeder = ctx.makeMockServerPlayerInLevel();
        breeder.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(breeder, KnowledgeRuntime.state(breeder).with("research.v3.research.17", 2));
        BlockPos center = ctx.absolutePos(new BlockPos(3, 2, 3));
        // GameTestHelper.spawn takes structure-relative positions; the AABB below is absolute.
        var parent = ctx.spawn(EntityTypes.COW, new BlockPos(3, 2, 3));
        var partner = ctx.spawn(EntityTypes.COW, new BlockPos(4, 2, 3));
        parent.setInLove(breeder);
        partner.setInLove(breeder);
        ctx.getLevel().addFreshEntity(parent);
        ctx.getLevel().addFreshEntity(partner);

        parent.spawnChildFromBreeding((ServerLevel) ctx.getLevel(), partner);
        long babies = ctx.getLevel().getEntitiesOfClass(Animal.class,
                new AABB(center.getX() - 2, center.getY() - 2, center.getZ() - 2,
                        center.getX() + 3, center.getY() + 3, center.getZ() + 3)).stream()
                .filter(animal -> animal.getType() == EntityTypes.COW && animal.isBaby()).count();
        ctx.assertTrue(babies == 2,
                "level two adds exactly one baby to vanilla's one-baby breeding result");
        ctx.assertTrue(parent.getAge() > 0 && partner.getAge() > 0,
                "vanilla breeding cooldown remains in effect for both parents");
        ctx.succeed();
    }

    @GameTest
    public void axeFirstStrikeBonusUsesTheAttackersResearchAndQuietPeriod(GameTestHelper ctx) {
        ServerPlayer attacker = ctx.makeMockServerPlayerInLevel();
        attacker.setGameMode(GameType.SURVIVAL);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        KnowledgeRuntime.updateState(attacker, KnowledgeRuntime.state(attacker).with("research.v3.research.28", 3));
        var researchedTarget = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2));
        attacker.attack(researchedTarget);

        var unresearchedTarget = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 2));
        ServerPlayer unresearched = ctx.makeMockServerPlayerInLevel();
        unresearched.setGameMode(GameType.SURVIVAL);
        unresearched.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        unresearched.attack(unresearchedTarget);
        ctx.assertTrue(researchedTarget.getHealth() < unresearchedTarget.getHealth(),
                "only a purchased level-three first axe strike gets the nine percent damage increase");
        ctx.succeed();
    }

    @GameTest
    public void xpAcquisitionBonusBelongsToThePlayerCollectingTheOrb(GameTestHelper ctx) {
        ServerPlayer researcher = ctx.makeMockServerPlayerInLevel();
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        researcher.setGameMode(GameType.SURVIVAL);
        other.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(researcher,
                KnowledgeRuntime.state(researcher).with("research.v3.research.64", 3));
        ExperienceOrb researchedOrb = new ExperienceOrb(ctx.getLevel(), 2.0D, 2.0D, 2.0D, 100);
        researchedOrb.playerTouch(researcher);
        ExperienceOrb ordinaryOrb = new ExperienceOrb(ctx.getLevel(), 4.0D, 2.0D, 2.0D, 100);
        ordinaryOrb.playerTouch(other);
        ctx.assertTrue(researcher.totalExperience == 109,
                "a level-three recipient gets the approved nine percent bonus on the orb pickup");
        ctx.assertTrue(other.totalExperience == 100,
                "an unresearched recipient receives vanilla XP even beside the researcher");
        ctx.succeed();
    }

    @GameTest
    public void movementResearchHonorsPurchaseWindowAndConsumesEachActionOnce(GameTestHelper ctx) {
        ServerPlayer jumper = ctx.makeMockServerPlayerInLevel();
        jumper.setGameMode(GameType.SURVIVAL);
        jumper.setOnGround(false);
        KnowledgeRuntime.updateState(jumper,
                KnowledgeRuntime.state(jumper).with("research.wall_jump", 1).with("research.fall_roll", 1));
        ResearchMovementRuntime.observeWallContact(jumper, true);
        ctx.assertTrue(ResearchMovementRuntime.onJump(jumper),
                "a purchased wall jump consumes a recent wall-contact window");
        ctx.assertTrue(jumper.getDeltaMovement().y == 0.55D
                        && jumper.getDeltaMovement().horizontalDistance() > 0.54D,
                "the accepted jump impulse pushes up and away from the wall");
        ctx.assertTrue(!ResearchMovementRuntime.onJump(jumper),
                "the same contact window cannot trigger a second jump");

        ServerPlayer unownedJumper = ctx.makeMockServerPlayerInLevel();
        unownedJumper.setGameMode(GameType.SURVIVAL);
        unownedJumper.setOnGround(false);
        ResearchMovementRuntime.observeWallContact(unownedJumper, true);
        ctx.assertTrue(!ResearchMovementRuntime.onJump(unownedJumper),
                "wall contact alone does not grant an unpurchased wall jump");

        ServerPlayer faller = ctx.makeMockServerPlayerInLevel();
        faller.setGameMode(GameType.SURVIVAL);
        KnowledgeRuntime.updateState(faller, KnowledgeRuntime.state(faller).with("research.fall_roll", 1));
        faller.setDeltaMovement(0.0D, -0.5D, 0.0D);
        faller.fallDistance = 5.0F;
        ResearchMovementRuntime.observeFallRollInput(faller, false);
        ResearchMovementRuntime.observeFallRollInput(faller, true);
        float healthBefore = faller.getHealth();
        faller.causeFallDamage(10.0F, 1.0F, faller.damageSources().fall());
        ctx.assertTrue(faller.getHealth() > healthBefore - 3.0F,
                "a purchased roll reduces fall damage by sixty percent");
        ctx.assertTrue(!ResearchMovementRuntime.consumeFallRoll(faller),
                "the same roll window cannot mitigate a second damage event");
        ctx.succeed();
    }

    /** Deterministic random source: returns the given rolls in order, then repeats the last one. */
    private static java.util.function.DoubleSupplier rolls(double... values) {
        int[] next = {0};
        return () -> values[Math.min(next[0]++, values.length - 1)];
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
