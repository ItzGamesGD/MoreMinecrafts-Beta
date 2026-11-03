package dev.itzgamesgd.moreminecrafts.accessory;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class T1AccessoryShockwaveGameTests implements CustomTestMethodInvoker {
    @GameTest public void sphereCenterLosProjectilesResistanceAndEnhancementRadius(GameTestHelper ctx) {
        var player = T1AccessoryGameTests.player(ctx);
        var origin = ctx.absolutePos(new BlockPos(0, 40, 0));
        player.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
        var level = ctx.getLevel();
        var blocked = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 40, 0));
        blocked.setNoAi(true); blocked.setDeltaMovement(Vec3.ZERO);
        for (int y = 40; y <= 42; y++) level.setBlock(ctx.absolutePos(new BlockPos(1, y, 0)), Blocks.STONE.defaultBlockState(), 3);
        var outside = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(0, 40, 5));
        outside.setNoAi(true); outside.setDeltaMovement(Vec3.ZERO);
        var boundary = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(0, 40, -4));
        boundary.setPos(player.getX(), player.getY(), player.getZ() - 4.1);
        boundary.setNoAi(true); boundary.setDeltaMovement(Vec3.ZERO);
        var resistant = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(-3, 40, 0));
        resistant.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        resistant.setNoAi(true); resistant.setDeltaMovement(Vec3.ZERO);
        var animal = ctx.spawn(EntityTypes.COW, new BlockPos(0, 40, 2));
        animal.setNoAi(true); animal.setDeltaMovement(Vec3.ZERO);
        var arrow = ctx.spawn(EntityTypes.ARROW, new BlockPos(-2, 41, -1));
        arrow.setOwner(player); arrow.setDeltaMovement(.3, -.2, .4);
        double speed = arrow.getDeltaMovement().length();
        var egg = ctx.spawn(EntityTypes.EGG, new BlockPos(-1, 41, 2));
        egg.setDeltaMovement(.1, -.1, .1);
        Vec3 eggBefore = egg.getDeltaMovement();
        var behindWall = ctx.spawn(EntityTypes.ARROW, new BlockPos(3, 41, 0));
        behindWall.setDeltaMovement(-.5, 0, 0);
        var trident = ctx.spawn(EntityTypes.TRIDENT, new BlockPos(-2, 41, 1));
        trident.setOwner(player); trident.setDeltaMovement(.4, 0, -.3);
        T1AccessoryGameTests.equip(player, AccessoryDefinition.T1_CRISIS_ESCAPE, 0);
        player.setHealth(5);
        T1Shockwave.tryActivate(player);
        ctx.assertTrue(blocked.getDeltaMovement().equals(Vec3.ZERO), "wall hides mob body center");
        ctx.assertTrue(outside.getDeltaMovement().equals(Vec3.ZERO), "+0 excludes five-block target");
        ctx.assertTrue(boundary.getDeltaMovement().equals(Vec3.ZERO), "bbox overlap does not include a center outside sphere");
        ctx.assertTrue(resistant.getDeltaMovement().equals(Vec3.ZERO), "full knockback resistance respected");
        ctx.assertTrue(animal.getDeltaMovement().equals(Vec3.ZERO), "ordinary animal excluded");
        ctx.assertTrue(Math.abs(arrow.getDeltaMovement().length() - speed) < 1e-9 && arrow.getOwner() == player,
                "arrow keeps speed and original owner");
        ctx.assertTrue(arrow.getDeltaMovement().dot(arrow.position().subtract(player.getBoundingBox().getCenter())) > 0,
                "arrow sent outward");
        ctx.assertTrue(trident.getDeltaMovement().dot(trident.position().subtract(player.getBoundingBox().getCenter())) > 0
                && trident.getOwner() == player && Math.abs(trident.getDeltaMovement().length() - .5) < 1e-9, "trident reflected without ownership change");
        ctx.assertTrue(egg.getDeltaMovement().equals(eggBefore), "egg excluded");
        ctx.assertTrue(behindWall.getDeltaMovement().equals(new Vec3(-.5, 0, 0)), "wall also hides projectile");
        T1AccessoryRuntime.onDeath(player);
        player.setHealth(20);
        T1AccessoryGameTests.equip(player, AccessoryDefinition.T1_CRISIS_ESCAPE, 5);
        player.setHealth(5);
        T1Shockwave.tryActivate(player);
        ctx.assertTrue(outside.getDeltaMovement().lengthSqr() > 0, "+5 includes five-block target");
        ctx.assertTrue(boundary.getDeltaMovement().lengthSqr() > 0, "+5 includes former +0 boundary target");
        ctx.assertTrue(level.getBlockState(ctx.absolutePos(new BlockPos(1, 41, 0))).is(Blocks.STONE), "blocks preserved");
        ctx.succeed();
    }
    @Override public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
