package dev.itzgamesgd.moreminecrafts.accessory;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Feather Charm: fall damage reduction per level, cooldown-free once-per-airtime air jump, and coexistence. */
public final class FeatherCharmGameTests implements CustomTestMethodInvoker {
    private static final float RAW_FALL = 10.0F;

    private static float fallLoss(GameTestHelper ctx, ServerPlayer player) {
        player.setHealth(20);
        player.setInvulnerableTime(0);
        player.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().fall(), RAW_FALL);
        return 20 - player.getHealth();
    }

    private static void airborne(ServerPlayer player) {
        player.setOnGround(false);
        player.setDeltaMovement(0, -0.5, 0);
        player.fallDistance = 5;
    }

    @GameTest public void fallDamageReductionFollowsTheEnhancementTableAtEveryLevel(GameTestHelper ctx) {
        // One fresh player per measurement: vanilla damage cooldown state would otherwise swallow a second hit in the same tick.
        ctx.assertTrue(Math.abs(fallLoss(ctx, T1AccessoryGameTests.player(ctx)) - RAW_FALL) < 0.001F, "no charm: unreduced fall damage");
        for (int level = 0; level <= 10; level++) {
            var player = T1AccessoryGameTests.player(ctx);
            T1AccessoryGameTests.equip(player, AccessoryDefinition.FEATHER_CHARM, level);
            float expected = (float) (RAW_FALL * (1 - FeatherCharmRules.FALL_DAMAGE_REDUCTION_PERCENT.get(level) / 100));
            float loss = fallLoss(ctx, player);
            ctx.assertTrue(Math.abs(loss - expected) < 0.01F, "+" + level + " fall loss " + loss + " expected " + expected);
        }
        // Only fall damage is reduced.
        var other = T1AccessoryGameTests.player(ctx);
        T1AccessoryGameTests.equip(other, AccessoryDefinition.FEATHER_CHARM, 10);
        other.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().generic(), RAW_FALL);
        ctx.assertTrue(Math.abs(20 - other.getHealth() - RAW_FALL) < 0.001F, "non-fall damage unchanged");
        ctx.succeed();
    }

    @GameTest public void airJumpWorksAtEveryLevelWithZeroCooldownAndLandingRestoresIt(GameTestHelper ctx) {
        var player = T1AccessoryGameTests.player(ctx);
        for (int level = 0; level <= 10; level++) {
            T1AccessoryGameTests.equip(player, AccessoryDefinition.FEATHER_CHARM, level);
            player.setOnGround(true);
            T1AccessoryRuntime.tick(player); // landing restores the air-jump use
            airborne(player);
            ctx.assertTrue(T1AccessoryRuntime.onJump(player), "+" + level + " air jump accepted");
            ctx.assertTrue(player.getDeltaMovement().y == FeatherCharmRules.AIR_JUMP_VELOCITY, "+" + level + " upward velocity");
            ctx.assertTrue(player.fallDistance == 0, "+" + level + " fall distance cleared");
            ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.FEATHER_CHARM) == 0
                    && AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_DOUBLE_JUMP) == 0,
                    "+" + level + " no cooldown started");
            player.setDeltaMovement(new Vec3(0, -0.5, 0));
            ctx.assertTrue(!T1AccessoryRuntime.onJump(player), "+" + level + " only one air jump per airtime");
            player.setOnGround(true);
            T1AccessoryRuntime.tick(player);
            airborne(player);
            ctx.assertTrue(T1AccessoryRuntime.onJump(player), "+" + level + " landing restored the jump immediately (cooldown 0)");
            player.setOnGround(true);
            ctx.assertTrue(!T1AccessoryRuntime.onJump(player), "+" + level + " ground press rejected");
        }
        ctx.succeed();
    }

    @GameTest public void sharesTheExplorationSlotWithT1DoubleJumpSoOnlyOneAppliesAndT1IsUnchanged(GameTestHelper ctx) {
        var player = T1AccessoryGameTests.player(ctx);
        T1AccessoryGameTests.equip(player, AccessoryDefinition.FEATHER_CHARM, 10);
        T1AccessoryGameTests.equip(player, AccessoryDefinition.T1_DOUBLE_JUMP, 5); // replaces the charm in the same slot
        ctx.assertTrue(AccessoryAbilities.equipped(player, AccessoryDefinition.FEATHER_CHARM).isEmpty(), "charm no longer equipped");
        ctx.assertTrue(Math.abs(fallLoss(ctx, player) - RAW_FALL) < 0.001F, "T1 double jump gives no fall reduction");
        airborne(player);
        ctx.assertTrue(T1AccessoryRuntime.onJump(player), "T1 jump accepted");
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_DOUBLE_JUMP) == 300, "T1 +5 cooldown still 300 ticks");
        ctx.succeed();
    }

    @GameTest public void coexistsWithHeavyBufferWhichStillSeesTheUnreducedDamage(GameTestHelper ctx) {
        var player = T1AccessoryGameTests.player(ctx);
        T1AccessoryGameTests.equip(player, AccessoryDefinition.T1_HEAVY_BUFFER, 0); // -40% at >= 40% max health, cooldown 1200
        T1AccessoryGameTests.equip(player, AccessoryDefinition.FEATHER_CHARM, 0);   // -40% fall
        float loss = fallLoss(ctx, player);
        float expected = RAW_FALL * 0.6F * 0.6F;
        ctx.assertTrue(Math.abs(loss - expected) < 0.01F, "buffer then charm: " + loss + " expected " + expected);
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.T1_HEAVY_BUFFER) == 1200, "buffer triggered on the unreduced 10");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }

    /** Stalagmite damage: same percent table as fall damage, but one reduction per 10 second cooldown. */
    @GameTest public void stalagmiteReductionFollowsTheTableAndIsLimitedByTheTenSecondCooldown(GameTestHelper ctx) {
        var player = T1AccessoryGameTests.player(ctx);
        T1AccessoryGameTests.equip(player, AccessoryDefinition.FEATHER_CHARM, 10);
        var key = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace("stalagmite"));
        var holder = ctx.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
        DamageSource stalagmite = new DamageSource(holder);
        ctx.assertTrue(FeatherCharmAbility.isCooldownSource(stalagmite), "stalagmite uses the cooldown path");

        player.setHealth(20);
        player.setInvulnerableTime(0);
        player.hurtServer(ctx.getLevel(), stalagmite, RAW_FALL);
        float first = 20 - player.getHealth();
        ctx.assertTrue(Math.abs(first - RAW_FALL * 0.4F) < 0.01F, "first stalagmite hit reduced by +10 table (60%): " + first);
        ctx.assertTrue(AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.FEATHER_CHARM) == 200, "10 second cooldown started");

        // The second and third checks call the rule directly. In the CI run a second same-player hurtServer in the
        // same test returned zero loss (health stayed 20) while the cooldown was correctly at 200, so vanilla damage
        // state, not the Feather Charm rule, was the blocker. The first hit above still covers the vanilla path.
        float second = FeatherCharmAbility.reduceFallDamage(player, stalagmite, RAW_FALL);
        ctx.assertTrue(Math.abs(second - RAW_FALL) < 0.001F, "second hit inside the cooldown is unreduced; amount=" + second
                + " cooldownRemaining=" + AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.FEATHER_CHARM));

        for (int i = 0; i < 200; i++) AccessoryCooldownRuntime.tick(player);
        float third = FeatherCharmAbility.reduceFallDamage(player, stalagmite, RAW_FALL);
        ctx.assertTrue(Math.abs(third - RAW_FALL * 0.4F) < 0.01F, "reduced again after the cooldown; amount=" + third);
        ctx.succeed();
    }
}
