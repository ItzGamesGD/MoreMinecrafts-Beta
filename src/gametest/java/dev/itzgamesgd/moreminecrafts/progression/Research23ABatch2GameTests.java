package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.*;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public final class Research23ABatch2GameTests implements CustomTestMethodInvoker {
    @GameTest
    public void statusDurationUsesRecipientAndKeepsAmplifierAndInfiniteEffects(GameTestHelper ctx) {
        var player = survivalPlayer(ctx);
        var other = survivalPlayer(ctx);
        for (int level = 0; level <= 3; level++) {
            KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.v3.research.62", level));
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 2), other);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200), other);
            ctx.assertTrue(player.getEffect(MobEffects.SPEED).getDuration() == 200 + 10 * level, "beneficial duration at purchased level " + level);
            ctx.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 200 - 10 * level, "harmful duration at purchased level " + level);
            ctx.assertTrue(player.getEffect(MobEffects.SPEED).getAmplifier() == 2, "amplifier unchanged");
            other.removeAllEffects();
            other.addEffect(new MobEffectInstance(MobEffects.SPEED, 200), player);
            ctx.assertTrue(other.getEffect(MobEffects.SPEED).getDuration() == 200, "source research cannot affect another recipient");
        }
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, -1));
        ctx.assertTrue(player.getEffect(MobEffects.SPEED).isInfiniteDuration(), "infinite effects stay infinite");
        ctx.succeed();
    }

    @GameTest
    public void durationScalingPreservesHiddenEffectsAndDoesNotMutateSharedInput(GameTestHelper ctx) {
        var paid = survivalPlayer(ctx);
        var unpaid = survivalPlayer(ctx);
        KnowledgeRuntime.updateState(paid, ResearchProgressState.empty().with("research.v3.research.62", 3));
        var hidden = new MobEffectInstance(MobEffects.SPEED, 600, 0);
        var incoming = new MobEffectInstance(MobEffects.SPEED, 200, 1, true, false, false, hidden);
        paid.addEffect(incoming);
        unpaid.addEffect(incoming);
        ctx.assertTrue(incoming.getDuration() == 200 && unpaid.getEffect(MobEffects.SPEED).getDuration() == 200,
                "shared incoming effect is never modified for another recipient");
        try {
            var field = MobEffectInstance.class.getDeclaredField("hiddenEffect");
            field.setAccessible(true);
            var copiedHidden = (MobEffectInstance) field.get(paid.getEffect(MobEffects.SPEED));
            ctx.assertTrue(copiedHidden != null && copiedHidden.getDuration() == 600 && copiedHidden != hidden,
                    "scaling preserves a private copy of the hidden vanilla effect chain");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        ctx.succeed();
    }

    @GameTest
    public void spiderPoisonIsBlockedOnlyForTheResearchedVictim(GameTestHelper ctx) {
        var paid = survivalPlayer(ctx);
        paid.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var unpaid = survivalPlayer(ctx);
        KnowledgeRuntime.updateState(paid, ResearchProgressState.empty().with("research.v3.research.39", 1));
        var spider = ctx.spawn(EntityTypes.CAVE_SPIDER, new BlockPos(2, 2, 2));
        ctx.assertTrue(!paid.addEffect(new MobEffectInstance(MobEffects.POISON, 100), spider), "spider-sourced poison blocked");
        ctx.assertTrue(unpaid.addEffect(new MobEffectInstance(MobEffects.POISON, 100), spider), "unpaid victim retains vanilla poison");
        ctx.assertTrue(paid.addEffect(new MobEffectInstance(MobEffects.POISON, 100), unpaid), "unrelated poison remains allowed");
        var near = spider.position().add(3.0, 0, 0);
        paid.setPos(near.x, near.y, near.z);
        unpaid.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        unpaid.setPos(near.x, near.y, near.z);
        spider.setOnGround(true);
        spider.setTarget(paid);
        ctx.assertTrue(spider.getTarget() == paid, "the paid Survival player is an actual eligible target");
        var leap = new LeapAtTargetGoal(spider, 0.4F);
        for (int i = 0; i < 30; i++) ctx.assertTrue(!leap.canUse(), "researched target prevents leap goal");
        spider.setTarget(unpaid);
        spider.getRandom().setSeed(42L);
        boolean vanillaLeap = false;
        for (int i = 0; i < 100; i++) vanillaLeap |= leap.canUse();
        ctx.assertTrue(vanillaLeap, "the same valid context permits ordinary leaps toward an unpaid target");
        spider.setClimbing(true);
        ctx.assertTrue(spider.onClimbable(), "climbing remains vanilla");
        ctx.succeed();
    }

    @GameTest
    public void customSpiderRestoresExistingPoisonWithoutReapplyingDurationResearch(GameTestHelper ctx) {
        var player = survivalPlayer(ctx);
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty()
                .with("research.v3.research.39", 1).with("research.v3.research.62", 3));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        int before = player.getEffect(MobEffects.POISON).getDuration();
        var spider = ctx.spawn(dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog.SWAMP_SPIDER, new BlockPos(2, 2, 2));
        ctx.assertTrue(spider.doHurtTarget(ctx.getLevel(), player), "the custom spider lands an ordinary melee attack");
        ctx.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == before,
                "restoring an existing poison instance must not scale it again or replace it with new spider poison");
        ctx.succeed();
    }

    @GameTest
    public void engagedEndermanCannotTeleportButUnengagedCan(GameTestHelper ctx) {
        var player = survivalPlayer(ctx);
        KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.v3.research.40", 1));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var enderman = ctx.spawn(EntityTypes.ENDERMAN, new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        var destination = ctx.absolutePos(new BlockPos(4, 2, 4));
        enderman.setTarget(player);
        ctx.assertTrue(!enderman.teleport(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5), "engaged researcher blocks teleport");
        enderman.setTarget(null);
        ctx.assertTrue(enderman.teleport(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5), "unengaged teleport still works");
        ctx.succeed();
    }

    @GameTest
    public void creeperFuseUsesEveryLevelWithoutChangingItsSavedBase(GameTestHelper ctx) throws Exception {
        var player = survivalPlayer(ctx);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var safe = ctx.absolutePos(new BlockPos(25, 3, 25));
        player.setPos(safe.getX(), safe.getY(), safe.getZ());
        for (int level = 0; level <= 3; level++) {
            KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.v3.research.38", level));
            var creeper = ctx.spawn(EntityTypes.CREEPER, new BlockPos(2, 2, 2));
            creeper.setNoAi(true);
            creeper.setTarget(player);
            creeper.setSwellDir(1);
            Field swell = Creeper.class.getDeclaredField("swell");
            swell.setAccessible(true);
            swell.setInt(creeper, 29);
            creeper.tick();
            ctx.assertTrue(level == 0 ? creeper.isRemoved() : !creeper.isRemoved(), "purchased fuse delay at level " + level);
            if (level > 0) {
                Field base = Creeper.class.getDeclaredField("maxSwell");
                base.setAccessible(true);
                ctx.assertTrue(base.getInt(creeper) == 30, "base fuse is never cumulatively modified");
                int delay = new int[]{0, 6, 12, 20}[level];
                for (int i = 1; i < delay; i++) creeper.tick();
                ctx.assertTrue(!creeper.isRemoved(), "creeper remains alive until the exact extended threshold");
                creeper.tick();
                ctx.assertTrue(creeper.isRemoved(), "creeper explodes at the exact approved extended threshold");
            }
        }
        ctx.succeed();
    }
    @GameTest
    public void fireworkBoostUsesFlightLevelAndReloadNeverMultipliesAgain(GameTestHelper ctx) throws Exception {
        var player = survivalPlayer(ctx);
        var lifetime = net.minecraft.world.entity.projectile.FireworkRocketEntity.class.getDeclaredField("lifetime");
        lifetime.setAccessible(true);
        for (int level = 0; level <= 3; level++) {
            KnowledgeRuntime.updateState(player, ResearchProgressState.empty().with("research.v3.research.51", level));
            var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET);
            stack.set(net.minecraft.core.component.DataComponents.FIREWORKS,
                    new net.minecraft.world.item.component.Fireworks(100, java.util.List.of()));
            var rocket = new net.minecraft.world.entity.projectile.FireworkRocketEntity(ctx.getLevel(), stack, player);
            int ticks = lifetime.getInt(rocket);
            // Vanilla lifetime is 10*(flight+1) + nextInt(6) + nextInt(7): 1010..1021.
            // Large flight duration makes all four level intervals disjoint, independent of RNG.
            boolean matchesApprovedMultiplier = false;
            for (int base = 1010; base <= 1021; base++)
                matchesApprovedMultiplier |= ticks == (int) Math.round(base * (100 + 8 * level) / 100.0);
            ctx.assertTrue(matchesApprovedMultiplier, "attached constructor applies the precise purchased multiplier at level " + level);
            var placed = new net.minecraft.world.entity.projectile.FireworkRocketEntity(ctx.getLevel(), player,
                    player.getX(), player.getY(), player.getZ(), stack);
            ctx.assertTrue(lifetime.getInt(placed) >= 1010 && lifetime.getInt(placed) <= 1021,
                    "an owned but unattached firework retains vanilla lifetime");
            var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
            rocket.saveWithoutId(output);
            var restored = new net.minecraft.world.entity.projectile.FireworkRocketEntity(EntityTypes.FIREWORK_ROCKET, ctx.getLevel());
            restored.load(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), output.buildResult()));
            ctx.assertTrue(lifetime.getInt(restored) == ticks, "save/reload never applies research twice");
            ctx.assertTrue(ResearchBatch2Runtime.flightLifetime(player, 100) == 100 + 8 * level,
                    "flight adapter resolves every purchased level");
        }
        ctx.succeed();
    }

    private static net.minecraft.server.level.ServerPlayer survivalPlayer(GameTestHelper ctx) {
        // Minecraft's makeMockServerPlayerInLevel overrides gameMode() to ALWAYS return Creative.
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

    @Override public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
