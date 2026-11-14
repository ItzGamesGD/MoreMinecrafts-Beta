package dev.itzgamesgd.moreminecrafts.test.crystalguardian;

import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Server-side checks for the POC test entity: registration, inert behaviour, forced walk, single-flight swing, no drops. */
public final class AncientCrystalGuardianGameTests {
    private static void floor(GameTestHelper ctx) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) ctx.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static AncientCrystalGuardianTest spawn(GameTestHelper ctx, BlockPos pos) {
        floor(ctx);
        return ctx.spawn(AncientCrystalGuardianTestEntities.TYPE, pos);
    }

    @GameTest
    public void entityIsRegisteredAsAnInertMiscEntity(GameTestHelper ctx) {
        Identifier id = Identifier.fromNamespaceAndPath("moreminecrafts", "ancient_crystal_guardian_test");
        ctx.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id), "entity type registered");
        ctx.assertTrue(BuiltInRegistries.ENTITY_TYPE.getValue(id) == AncientCrystalGuardianTestEntities.TYPE, "registry maps to the constant");
        EntityType<?> type = AncientCrystalGuardianTestEntities.TYPE;
        ctx.assertTrue(type.getCategory() == MobCategory.MISC, "MISC category: never part of natural spawn cycles");
        ctx.assertTrue(!type.canSerialize(), "not saved with the world");
        ctx.assertTrue(!BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("moreminecrafts", "ancient_crystal_guardian_test_spawn_egg")),
                "no spawn egg");
        ctx.succeed();
    }

    @GameTest
    public void guardianHasNoAiAttributesOrCombatGoals(GameTestHelper ctx) {
        AncientCrystalGuardianTest g = spawn(ctx, new BlockPos(3, 1, 3));
        ctx.assertTrue(g.getTarget() == null, "no target");
        ctx.assertTrue(!g.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                "no attack damage attribute: it cannot hurt anything");
        ctx.assertTrue(g.targetGoalCount() == 0, "no target goals");
        ctx.assertTrue(g.goalCount() == 1, "only the forced-walk test goal");
        ctx.assertTrue(!g.isForcedWalking() && !g.isSwinging(), "idle by default");
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void ignoresNearbyCreaturesForTwoHundredTicks(GameTestHelper ctx) {
        AncientCrystalGuardianTest g = spawn(ctx, new BlockPos(1, 1, 1));
        Villager bystander = ctx.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER, new BlockPos(6, 1, 6));
        float health = bystander.getHealth();
        ctx.runAfterDelay(90, () -> {
            ctx.assertTrue(g.getTarget() == null, "never targets");
            ctx.assertTrue(bystander.getHealth() == health, "bystander untouched");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void forcedWalkMovesStraightAheadThenStops(GameTestHelper ctx) {
        AncientCrystalGuardianTest g = spawn(ctx, new BlockPos(3, 1, 0));
        double startZ = g.getZ();
        g.startWalk(30, 1.0D);
        ctx.assertTrue(g.isForcedWalking(), "walk requested");
        ctx.runAfterDelay(60, () -> {
            ctx.assertTrue(!g.isForcedWalking(), "walk ended after its tick budget");
            ctx.assertTrue(g.getZ() - startZ > 2.0D, "walked forward (south), moved " + (g.getZ() - startZ));
            ctx.assertTrue(Math.abs(g.getX() - (ctx.absoluteVec(new net.minecraft.world.phys.Vec3(3.5D, 1.0D, 0.5D)).x)) < 1.5D, "walked straight");
            double z = g.getZ();
            ctx.runAfterDelay(10, () -> {
                ctx.assertTrue(Math.abs(g.getZ() - z) < 0.3D, "stands still after the walk ends");
                ctx.succeed();
            });
        });
    }

    @GameTest(maxTicks = 80)
    public void swingIsSingleFlightAndRepeatable(GameTestHelper ctx) {
        AncientCrystalGuardianTest g = spawn(ctx, new BlockPos(3, 1, 3));
        ctx.assertTrue(g.requestSwing(), "first swing starts");
        ctx.assertTrue(g.isSwinging(), "swinging");
        ctx.assertTrue(!g.requestSwing(), "second request during a swing is ignored (no overlap)");
        ctx.runAfterDelay(GuardianPoseMath.SWING_TICKS + 2, () -> {
            ctx.assertTrue(!g.isSwinging(), "swing finished by itself");
            ctx.assertTrue(g.requestSwing(), "can swing again after finishing");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 300)
    public void swingLoopRunsExactlyTheRequestedNumberOfSwings(GameTestHelper ctx) {
        AncientCrystalGuardianTest g = spawn(ctx, new BlockPos(3, 1, 3));
        AtomicInteger risingEdges = new AtomicInteger();
        boolean[] wasSwinging = {false};
        g.startSwingLoop(3, 10);
        ctx.onEachTick(() -> {
            boolean now = g.isSwinging();
            if (now && !wasSwinging[0]) risingEdges.incrementAndGet();
            wasSwinging[0] = now;
        });
        ctx.runAfterDelay(3 * (GuardianPoseMath.SWING_TICKS + 10) + 20, () -> {
            ctx.assertTrue(risingEdges.get() == 3, "exactly 3 swings, saw " + risingEdges.get());
            ctx.assertTrue(g.swingRepeatsLeft() == 0 && !g.isSwinging(), "loop finished");
            ctx.succeed();
        });
    }

    @GameTest
    public void resetCancelsWalkAndPendingSwings(GameTestHelper ctx) {
        AncientCrystalGuardianTest g = spawn(ctx, new BlockPos(3, 1, 3));
        g.startWalk(200, 1.0D);
        g.startSwingLoop(5, 10);
        g.resetTest();
        ctx.assertTrue(!g.isForcedWalking() && g.swingRepeatsLeft() == 0, "reset clears walk and loop");
        ctx.succeed();
    }

    @GameTest(maxTicks = 60)
    public void dropsNothingWhenKilled(GameTestHelper ctx) {
        AncientCrystalGuardianTest g = spawn(ctx, new BlockPos(3, 1, 3));
        g.kill(ctx.getLevel());
        ctx.runAfterDelay(30, () -> {
            AABB area = new AABB(ctx.absolutePos(new BlockPos(0, 0, 0))).inflate(10.0D);
            ctx.assertTrue(ctx.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area).isEmpty(), "no item drops");
            ctx.assertTrue(ctx.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, area).isEmpty(), "no experience");
            ctx.succeed();
        });
    }
}
