package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ResearchRemainingGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void mobCaptureDraftRequiresPurchasedResearchBeforeRemovingTarget(GameTestHelper ctx) {
        var player = survivalPlayer(ctx);
        var tool = new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.MOB_CAPTURE_TOOL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
        var mob = ctx.spawn(EntityTypes.COW, new BlockPos(3, 2, 3));
        var result = net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(
                player, ctx.getLevel(), net.minecraft.world.InteractionHand.MAIN_HAND, mob,
                new net.minecraft.world.phys.EntityHitResult(mob));
        ctx.assertTrue(result == net.minecraft.world.InteractionResult.FAIL && mob.isAlive(),
                "the item cannot capture or remove a mob without its paid research");
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.mob_capture", 1));
        result = net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(
                player, ctx.getLevel(), net.minecraft.world.InteractionHand.MAIN_HAND, mob,
                new net.minecraft.world.phys.EntityHitResult(mob));
        ctx.assertTrue(result == net.minecraft.world.InteractionResult.SUCCESS_SERVER && mob.isRemoved(),
                "the same valid target can be captured after purchase");
        ctx.assertTrue(player.getInventory().countItem(dev.itzgamesgd.moreminecrafts.registry.ModItems.CAPTURED_MOB_EGG) == 1,
                "a successful capture stores one egg");
        ctx.assertTrue(tool.getDamageValue() == 1, "the tool spends exactly one durability");
        ctx.succeed();
    }

    @GameTest
    public void normalHorseUsesRiderResearchWithoutChangingNativeAttributes(GameTestHelper ctx) {
        var paid = survivalPlayer(ctx);
        var unpaid = survivalPlayer(ctx);
        var horse = ctx.spawn(EntityTypes.HORSE, new BlockPos(3, 2, 3));
        horse.setTamed(true);
        horse.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        paid.startRiding(horse, true, true);
        ctx.assertTrue(horse.getControllingPassenger() == paid, "the saddled horse has the researched rider");
        double baseSpeed = horse.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double baseJump = horse.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH);
        double baseHealth = horse.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        for (int level = 0; level <= 5; level++) {
            KnowledgeRuntime.updateState(paid, ResearchProgressState.empty().with("research.v3.research.50", level));
            horse.tick();
            ctx.assertTrue(Math.abs(horse.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    - baseSpeed - ResearchRemainingRules.horseSpeedBonus(level)) < 1.0e-7, "speed level " + level);
            ctx.assertTrue(Math.abs(horse.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)
                    - baseJump - ResearchRemainingRules.horseJumpBonus(level)) < 1.0e-7, "jump level " + level);
            ctx.assertTrue(Math.abs(horse.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                    - baseHealth - ResearchRemainingRules.horseHealthBonus(level)) < 1.0e-7, "health level " + level);
            ctx.assertTrue(horse.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) == baseHealth,
                    "the native horse base is not rewritten");
        }
        var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
        horse.saveWithoutId(saved);
        var restored = new net.minecraft.world.entity.animal.equine.Horse(EntityTypes.HORSE, ctx.getLevel());
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved.buildResult()));
        ctx.assertTrue(Math.abs(restored.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                - baseSpeed) < 1.0e-7, "save/reload retains the native speed base");
        ctx.assertTrue(restored.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                .getModifier(ResearchRidingRuntime.SPEED_ID) == null,
                "transient rider bonuses are recomputed from the next controller, not saved");
        paid.stopRiding();
        unpaid.startRiding(horse, true, true);
        horse.tick();
        ctx.assertTrue(horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                .getModifier(ResearchRidingRuntime.SPEED_ID) == null, "an unpaid rider cannot borrow bonuses");
        ctx.assertTrue(horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH)
                .getModifier(ResearchRidingRuntime.JUMP_ID) == null, "jump bonus is removed on rider switch");
        ctx.assertTrue(horse.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .getModifier(ResearchRidingRuntime.HEALTH_ID) == null, "health bonus is removed on rider switch");
        ctx.succeed();
    }

    @GameTest
    public void swordHitBuildsSweepStageOnlyForPurchaser(GameTestHelper ctx) throws Exception {
        var player = survivalPlayer(ctx);
        var unpaid = survivalPlayer(ctx);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        unpaid.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.v3.research.33", 1));
        var target = ctx.spawn(EntityTypes.IRON_GOLEM, new BlockPos(3, 2, 3));
        player.setPos(target.getX() - 1, target.getY(), target.getZ());
        unpaid.setPos(target.getX() + 1, target.getY(), target.getZ());
        player.attack(target);
        ctx.assertTrue(ResearchConsecutiveSweep.currentStage(player) == 1,
                "a successful purchased sword hit starts the sweep chain");
        ctx.assertTrue(ResearchConsecutiveSweep.currentStage(unpaid) == 0,
                "another player never borrows the attacker's stage");
        for (int i = 1; i < 5; i++) ResearchConsecutiveSweep.recordHit(player, target);
        ctx.assertTrue(ResearchConsecutiveSweep.currentStage(player) == 5, "five valid hits cap the chain");
        ctx.assertTrue(Math.abs(ResearchConsecutiveSweep.sweepDamage(player, 10.0F) - 12.0F) < 0.001F,
                "the maximum stage scales only sweep damage by twenty percent");
        var secondary = ctx.spawn(EntityTypes.COW, new BlockPos(2, 2, 4));
        secondary.setPos(target.getX(), target.getY(), target.getZ() + 1.0D);
        float before = secondary.getHealth();
        var sweep = net.minecraft.world.entity.player.Player.class.getDeclaredMethod("doSweepAttack",
                net.minecraft.world.entity.Entity.class, float.class,
                net.minecraft.world.damagesource.DamageSource.class, float.class);
        sweep.setAccessible(true);
        sweep.invoke(player, target, 1.0F, player.damageSources().playerAttack(player), 1.0F);
        ctx.assertTrue(Math.abs((before - secondary.getHealth()) - 1.2F) < 0.01F,
                "the vanilla secondary sweep damage receives the stage-five bonus exactly once");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        ctx.assertTrue(ResearchConsecutiveSweep.currentStage(player) == 0, "non-swords cannot receive sweep bonus");
        ctx.succeed();
    }

    @GameTest
    public void lingeringCloudDurationUsesThrowerLevelAndPersists(GameTestHelper ctx) {
        var shooter = ctx.makeMockServerPlayerInLevel();
        var impact = Vec3.atCenterOf(ctx.absolutePos(new BlockPos(3, 2, 3)));
        for (int level = 0; level <= 3; level++) {
            KnowledgeRuntime.updateState(shooter, ResearchProgressState.empty().with("research.v3.research.61", level));
            var potion = new ThrownLingeringPotion(ctx.getLevel(), shooter,
                    new ItemStack(Items.LINGERING_POTION));
            potion.setPos(impact);
            potion.onHitAsPotion(ctx.getLevel(), potion.getItem(),
                    new BlockHitResult(impact, Direction.UP, BlockPos.containing(impact), false));
            var clouds = ctx.getLevel().getEntitiesOfClass(AreaEffectCloud.class, new AABB(impact, impact).inflate(2));
            ctx.assertTrue(clouds.size() == 1, "one lingering cloud is produced");
            var cloud = clouds.getFirst();
            ctx.assertTrue(cloud.getDuration() == 600 + level * 48, "cloud lifetime matches purchased level " + level);
            ctx.assertTrue(cloud.getOwner() == shooter, "the cloud retains thrower attribution");
            if (level == 3) {
                var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                        net.minecraft.util.ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
                cloud.saveWithoutId(saved);
                var restored = new AreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD, ctx.getLevel());
                restored.load(net.minecraft.world.level.storage.TagValueInput.create(
                        net.minecraft.util.ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved.buildResult()));
                ctx.assertTrue(restored.getDuration() == cloud.getDuration(),
                        "the researched duration persists without applying the multiplier again");
            }
            cloud.discard();
        }
        ctx.succeed();
    }

    @GameTest
    public void splashRangeOnlyExtendsForPurchasedThrower(GameTestHelper ctx) {
        var shooter = ctx.makeMockServerPlayerInLevel();
        var victim = ctx.spawn(EntityTypes.COW, new BlockPos(8, 2, 3));
        var impact = Vec3.atCenterOf(ctx.absolutePos(new BlockPos(3, 2, 3)));
        victim.setPos(impact.x + 5.0D, impact.y, impact.z);
        var stack = new ItemStack(Items.SPLASH_POTION);
        stack.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withEffectAdded(
                new MobEffectInstance(MobEffects.SPEED, 1200)));
        for (int level : new int[]{0, 3}) {
            KnowledgeRuntime.updateState(shooter, ResearchProgressState.empty().with("research.v3.research.61", level));
            victim.removeAllEffects();
            var potion = new ThrownSplashPotion(ctx.getLevel(), shooter, stack);
            potion.setPos(impact);
            potion.onHitAsPotion(ctx.getLevel(), stack,
                    new BlockHitResult(impact, Direction.UP, BlockPos.containing(impact), false));
            ctx.assertTrue((victim.getEffect(MobEffects.SPEED) != null) == (level == 3),
                    "splash at the extended edge follows the throwing player's purchased level " + level);
        }
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }

    private static net.minecraft.server.level.ServerPlayer survivalPlayer(GameTestHelper ctx) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "research-survivor");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new net.minecraft.server.level.ServerPlayer(ctx.getLevel().getServer(), ctx.getLevel(),
                cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        ctx.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        return player;
    }
}
