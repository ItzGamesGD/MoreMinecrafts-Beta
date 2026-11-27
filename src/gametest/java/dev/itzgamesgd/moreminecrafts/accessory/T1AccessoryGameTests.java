package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Owner-approved D01 boundary checks through real loadout, damage and effect paths. */
public final class T1AccessoryGameTests implements CustomTestMethodInvoker {
    static ServerPlayer player(GameTestHelper ctx) {
        var player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getAbilities().invulnerable = false;
        player.setPermanentlyInvulnerable(false);
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        player.setInvulnerableTime(0);
        // Mock players otherwise share the world spawn and can collect another test's drops in doTick().
        var position = ctx.absolutePos(new net.minecraft.core.BlockPos(3, 30, 3));
        player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5);
        return player;
    }
    static void equip(ServerPlayer player, AccessoryDefinition accessory, int level) {
        var stack = new ItemStack(ModItems.ACCESSORIES.get(accessory));
        stack.set(ModDataComponents.ENHANCEMENT_LEVEL, level);
        new AccessoryEquipmentContainer(player).setItem(accessory.category().ordinal(), stack);
    }

    @GameTest public void allSevenEquipAndEnhanceWithoutXpOrCatalyst(GameTestHelper ctx) {
        var player = player(ctx);
        for (var accessory : AccessoryDefinition.ALL) {
            if (!accessory.isT1()) continue;
            equip(player, accessory, 0);
            var slot = accessory.category().ordinal();
            var stack = new AccessoryEquipmentContainer(player).getItem(slot);
            ctx.assertTrue(stack.getItem() == ModItems.ACCESSORIES.get(accessory), "equipped " + accessory);
            var stones = new ItemStack(ModItems.ENHANCEMENT_STONE, 20);
            for (int i = 0; i < 5; i++)
                ctx.assertTrue(AccessoryEnhancementOperation.apply(player, stack, stones, ItemStack.EMPTY), "stone-only upgrade");
            ctx.assertTrue(stones.isEmpty() && AccessoryEnhancementOperation.level(stack) == 5, "+5 costs exactly 20 stones");
            ctx.assertTrue(!AccessoryEnhancementOperation.apply(player, stack,
                    new ItemStack(ModItems.ENHANCEMENT_STONE, 64), ItemStack.EMPTY), "cap rejects +6");
            new AccessoryEquipmentContainer(player).setItem(slot, ItemStack.EMPTY);
            ctx.assertTrue(AccessoryAbilities.equipped(player, accessory).isEmpty(), "unequipped");
        }
        ctx.succeed();
    }

    @GameTest public void harmfulEffectsAreShortenedOncePerApplicationAndInputIsNotMutated(GameTestHelper ctx) {
        var player = player(ctx);
        equip(player, AccessoryDefinition.T1_STATUS_RESILIENCE, 0);
        var shared = new MobEffectInstance(MobEffects.POISON, 400, 1);
        player.addEffect(shared);
        ctx.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 320, "+0 poison 400 -> 320 ticks");
        ctx.assertTrue(shared.getDuration() == 400, "shared input untouched");
        player.addEffect(shared);
        ctx.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 320, "refresh does not compound");
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 400));
        ctx.assertTrue(player.getEffect(MobEffects.SPEED).getDuration() == 400, "beneficial unchanged");
        equip(player, AccessoryDefinition.T1_STATUS_RESILIENCE, 5);
        player.removeEffect(MobEffects.POISON);
        player.addEffect(shared);
        ctx.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 240, "+5 poison 400 -> 240 ticks");
        ctx.succeed();
    }

    @GameTest public void healingRestartsAfterDamageAndSprintMultipliesFinalSpeed(GameTestHelper ctx) {
        var player = player(ctx);
        equip(player, AccessoryDefinition.T1_SUSTAINED_RECOVERY, 5);
        player.setHealth(10);
        for (int i = 0; i < 199; i++) T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getHealth() == 10, "no healing before ten seconds");
        T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getHealth() == 11, "first heal after ten seconds");
        for (int i = 0; i < 39; i++) T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getHealth() == 11, "+5 interval not yet elapsed");
        T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getHealth() == 12, "+5 heals every forty ticks");
        player.hurtServer(ctx.getLevel(), player.damageSources().generic(), 1);
        float hurt = player.getHealth();
        for (int i = 0; i < 199; i++) T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getHealth() == hurt, "actual damage restarts waiting");
        equip(player, AccessoryDefinition.T1_SPRINT_ACCELERATION, 5);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 1000));
        player.setSprinting(true);
        double before = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for (int i = 0; i < 100; i++) T1AccessoryRuntime.tick(player);
        ctx.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) / before - 1.35) < 1e-6,
                "speed incl. sprint and potion multiplied by 1.35");
        player.setSprinting(false);
        T1AccessoryRuntime.tick(player);
        ctx.assertTrue(!player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(T1AccessoryRuntime.SPRINT_ID), "stop removes modifier");
        ctx.succeed();
    }

    @GameTest public void bufferUsesPostArmorDamageBeforeLethalHealthLoss(GameTestHelper ctx) {
        var player = player(ctx);
        equip(player, AccessoryDefinition.T1_HEAVY_BUFFER, 0);
        player.hurtServer(ctx.getLevel(), player.damageSources().generic(), 8);
        ctx.assertTrue(Math.abs(player.getHealth() - 15.2f) < 0.001, "8 HP hit becomes 4.8 at +0");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_HEAVY_BUFFER) == 1200, "sixty-second cooldown");
        var max = player(ctx);
        equip(max, AccessoryDefinition.T1_HEAVY_BUFFER, 5);
        max.setHealth(10);
        max.hurtServer(ctx.getLevel(), max.damageSources().generic(), 20);
        ctx.assertTrue(max.isAlive() && Math.abs(max.getHealth() - 2) < 0.001, "lethal 20 becomes 8 before death");
        ctx.succeed();
    }

    @GameTest public void armorPreventsBufferThresholdAndPlusZeroPassivesUseTheirOwnBounds(GameTestHelper ctx) {
        var player = player(ctx);
        equip(player, AccessoryDefinition.T1_HEAVY_BUFFER, 0);
        player.getAttribute(Attributes.ARMOR).setBaseValue(8);
        var attacker = ctx.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(1, 2, 1));
        player.hurtServer(ctx.getLevel(), player.damageSources().mobAttack(attacker), 8);
        ctx.assertTrue(player.getHealth() > 12 && player.getHealth() < 20, "armor already reduced the eight-point attack");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_HEAVY_BUFFER) == 0,
                "pre-armor eight-point input does not meet post-armor threshold");
        equip(player, AccessoryDefinition.T1_SUSTAINED_RECOVERY, 0);
        player.setHealth(10);
        for (int i = 0; i < 200; i++) T1AccessoryRuntime.tick(player);
        for (int i = 0; i < 59; i++) T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getHealth() == 11, "+0 recovery interval is not +5's forty ticks");
        T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getHealth() == 12, "+0 heals each sixty ticks");
        equip(player, AccessoryDefinition.T1_SPRINT_ACCELERATION, 0);
        player.setSprinting(true);
        double before = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for (int i = 0; i < 99; i++) T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == before, "no early sprint activation");
        T1AccessoryRuntime.tick(player);
        ctx.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) / before - 1.15) < 1e-6, "+0 final sprint multiplier");
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.EXPLORATION_MOBILITY.ordinal(), ItemStack.EMPTY);
        ctx.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == before, "unequip removes speed immediately");
        ctx.succeed();
    }

    @GameTest public void shockwaveHasStrictThresholdAndNoTargetRequirement(GameTestHelper ctx) {
        var player = player(ctx);
        player.setHealth(6);
        equip(player, AccessoryDefinition.T1_CRISIS_ESCAPE, 0);
        T1AccessoryRuntime.tick(player);
        ctx.assertTrue(player.getHealth() == 6, "exactly thirty percent does not trigger");
        player.hurtServer(ctx.getLevel(), player.damageSources().generic(), 1);
        ctx.assertTrue(player.getHealth() == 9, "damage applied first then heal four");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_CRISIS_ESCAPE) == 1800, "cooldown starts without enemies");
        new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.COMBAT.ordinal(), ItemStack.EMPTY);
        AccessoryCooldownRuntime.tick(player);
        equip(player, AccessoryDefinition.T1_CRISIS_ESCAPE, 5);
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_CRISIS_ESCAPE) == 1799, "re-equipping does not reset shared cooldown");
        var lethal = player(ctx);
        equip(lethal, AccessoryDefinition.T1_CRISIS_ESCAPE, 0);
        lethal.hurtServer(ctx.getLevel(), lethal.damageSources().genericKill(), 1000);
        ctx.assertTrue(!lethal.isAlive(), "death cannot be rescued after damage");
        ctx.succeed();
    }

    @GameTest public void fourthChargedMeleeAddsWeaponDamage(GameTestHelper ctx) {
        var player = player(ctx);
        player.setOnGround(true);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
        equip(player, AccessoryDefinition.T1_COMBO, 5);
        for (int i = 0; i < 4; i++) {
            var target = ctx.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(1, 2, 1));
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.setNoAi(true);
            for (int j = 0; j < 25; j++) player.doTick();
            float hp = target.getHealth();
            player.attack(target);
            double expected = i == 3 ? 6 : 4;
            ctx.assertTrue(Math.abs(hp - target.getHealth() - expected) < 0.001,
                    "hit " + i + " expected " + expected + " got " + (hp - target.getHealth()));
        }
        ctx.succeed();
    }

    @GameTest public void absorbedFourthHitDoesNotRepeatBonusAndDragonPartsAreCombatTargets(GameTestHelper ctx) {
        var player = player(ctx);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
        player.setOnGround(true);
        equip(player, AccessoryDefinition.T1_COMBO, 5);
        for (int i = 0; i < 3; i++) {
            var zombie = ctx.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(1, 2, 1));
            zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
            zombie.setNoAi(true);
            for (int j = 0; j < 25; j++) player.doTick();
            player.attack(zombie);
        }
        var absorbed = ctx.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(2, 2, 2));
        absorbed.getAttribute(Attributes.ARMOR).setBaseValue(0);
        absorbed.setNoAi(true);
        absorbed.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(40);
        absorbed.setAbsorptionAmount(40);
        for (int j = 0; j < 25; j++) player.doTick();
        player.attack(absorbed);
        ctx.assertTrue(Math.abs(absorbed.getAbsorptionAmount() - 36) < 0.001,
                "invalid fully absorbed fourth candidate receives only ordinary damage: " + absorbed.getAbsorptionAmount());
        var dragon = ctx.spawn(net.minecraft.world.entity.EntityTypes.ENDER_DRAGON, new net.minecraft.core.BlockPos(3, 4, 3));
        for (int j = 0; j < 25; j++) player.doTick();
        float hp = dragon.getHealth();
        player.attack(dragon.head);
        ctx.assertTrue(Math.abs(hp - dragon.getHealth() - 6) < 0.001,
                "dragon head is fourth valid combat hit, got " + (hp - dragon.getHealth()));
        ctx.succeed();
    }

    @GameTest(maxTicks = 130) public void invalidHitsDoNotRefreshComboTimeoutAndUnequipResetsImmediately(GameTestHelper ctx) {
        var player = player(ctx);
        player.setOnGround(true);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
        equip(player, AccessoryDefinition.T1_COMBO, 0);
        for (int i = 0; i < 3; i++) ordinaryHit(ctx, player);
        ctx.runAfterDelay(60, () -> {
            var cow = ctx.spawn(net.minecraft.world.entity.EntityTypes.COW, new net.minecraft.core.BlockPos(2, 2, 2));
            cow.setNoAi(true);
            for (int j = 0; j < 25; j++) player.doTick();
            player.attack(cow);
        });
        ctx.runAfterDelay(81, () -> {
            ctx.assertTrue(Math.abs(ordinaryHit(ctx, player) - 4) < .001, "animal did not refresh the four-second timer");
            ordinaryHit(ctx, player); ordinaryHit(ctx, player);
            new AccessoryEquipmentContainer(player).setItem(AccessoryCategory.COMBAT.ordinal(), ItemStack.EMPTY);
            equip(player, AccessoryDefinition.T1_COMBO, 0);
            ctx.assertTrue(Math.abs(ordinaryHit(ctx, player) - 4) < .001, "unequip/re-equip resets unfinished combo");
            ordinaryHit(ctx, player); ordinaryHit(ctx, player);
            ctx.assertTrue(Math.abs(ordinaryHit(ctx, player) - 5.4) < .001, "+0 fourth hit adds thirty-five percent");
            ctx.succeed();
        });
    }
    private static float ordinaryHit(GameTestHelper ctx, ServerPlayer player) {
        var target = ctx.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(1, 2, 1));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setNoAi(true);
        for (int j = 0; j < 25; j++) player.doTick();
        float hp = target.getHealth();
        player.attack(target);
        float damage = hp - target.getHealth();
        target.discard();
        return damage;
    }

    @GameTest public void jumpChecksAirUseAndPreservesOnlineCooldownThroughReload(GameTestHelper ctx) {
        var player = player(ctx);
        equip(player, AccessoryDefinition.T1_DOUBLE_JUMP, 5);
        player.setOnGround(true);
        ctx.assertTrue(!T1AccessoryRuntime.onJump(player), "ground packet rejected");
        player.setOnGround(false);
        player.setDeltaMovement(0.2, -1, 0.3);
        player.fallDistance = 20;
        ctx.assertTrue(T1AccessoryRuntime.onJump(player), "descending jump accepted");
        ctx.assertTrue(player.fallDistance == 0 && player.getDeltaMovement().y > 0,
                "fall distance reset and upward impulse applied");
        ctx.assertTrue(player.getDeltaMovement().x == 0.2 && player.getDeltaMovement().z == 0.3, "horizontal momentum kept");
        ctx.assertTrue(!T1AccessoryRuntime.onJump(player), "second use rejected");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_DOUBLE_JUMP) == 300, "+5 cooldown fifteen seconds");
        var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess());
        player.saveWithoutId(saved);
        var loaded = player(ctx);
        loaded.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, loaded.registryAccess(), saved.buildResult()));
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(loaded, AccessoryDefinition.T1_DOUBLE_JUMP) == 300, "saved remaining ticks paused");
        AccessoryCooldownRuntime.start(loaded, AccessoryDefinition.EMBER_RING, 600);
        T1AccessoryRuntime.onDeath(loaded);
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(loaded, AccessoryDefinition.T1_DOUBLE_JUMP) == 0, "T1 death reset");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(loaded, AccessoryDefinition.EMBER_RING) == 600, "T2 death persistence untouched");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
