package dev.itzgamesgd.moreminecrafts.skeleton;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

public final class SkeletonKingGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void kingHealthAndGuardOwnershipSurviveEntityReload(GameTestHelper context) {
        var level = context.getLevel();
        Skeleton king = EntityTypes.SKELETON.create(level, EntitySpawnReason.COMMAND);
        context.assertTrue(king != null, "Skeleton King entity should be constructible");
        ((SkeletonKingState) king).moreminecrafts$makeSkeletonKing();
        king.setHealth(83.0F);
        level.addFreshEntity(king);

        Skeleton restoredKing = reloadSkeleton(level, save(king));
        context.assertTrue(((SkeletonKingState) restoredKing).moreminecrafts$isSkeletonKing(),
                "King identity should survive entity save/load");
        context.assertTrue(Math.abs(restoredKing.getHealth() - 83.0F) < 0.01F,
                "King health should not reset to max during entity reload");

        Skeleton guard = EntityTypes.SKELETON.create(level, EntitySpawnReason.COMMAND);
        context.assertTrue(guard != null, "Guard entity should be constructible");
        ((SkeletonKingState) guard).moreminecrafts$setGuardOwner(king.getUUID());
        level.addFreshEntity(guard);
        CompoundTag guardData = save(guard);
        guard.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

        Skeleton restoredGuard = reloadSkeleton(level, guardData);
        level.addFreshEntity(restoredGuard);
        context.assertTrue(king.getUUID().equals(((SkeletonKingState) restoredGuard).moreminecrafts$getGuardOwner()),
                "Guard owner UUID should survive the chunk-style entity unload/reload save path");
        context.runAfterDelay(1, () -> {
            context.assertTrue(restoredGuard.isAlive(), "Reloaded owned guard should remain alive with its owner loaded");
            context.succeed();
        });
    }

    @GameTest
    public void targetSwitchCancelsPendingBarrage(GameTestHelper context) {
        var level = context.getLevel();
        Skeleton king = context.spawnMob(EntityTypes.SKELETON, 1, 1, 1).withNoFreeWill().spawn();
        ((SkeletonKingState) king).moreminecrafts$makeSkeletonKing();
        king.setNoAi(true);

        ServerPlayer first = context.makeMockServerPlayerInLevel();
        ServerPlayer second = context.makeMockServerPlayerInLevel();
        first.setPos(king.getX() + 2.0D, king.getY(), king.getZ());
        second.setPos(king.getX() + 3.0D, king.getY(), king.getZ());
        king.setTarget(first);

        context.runAfterDelay(125, () -> king.setTarget(second));
        context.runAfterDelay(151, () -> {
            List<Arrow> arrows = level.getEntitiesOfClass(Arrow.class,
                    new AABB(king.getX() - 32, king.getY() - 8, king.getZ() - 32,
                            king.getX() + 32, king.getY() + 16, king.getZ() + 32),
                    arrow -> arrow.getOwner() == king);
            context.assertTrue(arrows.isEmpty(), "Changing target during the telegraph must cancel the barrage");
            context.succeed();
        });
    }

    private static CompoundTag save(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static Skeleton reloadSkeleton(net.minecraft.server.level.ServerLevel level, CompoundTag data) {
        Skeleton reloaded = EntityTypes.SKELETON.create(level, EntitySpawnReason.COMMAND);
        if (reloaded == null) throw new AssertionError("Skeleton entity should be constructible");
        reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
        return reloaded;
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
