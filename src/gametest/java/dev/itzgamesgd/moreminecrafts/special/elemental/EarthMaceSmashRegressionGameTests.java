package dev.itzgamesgd.moreminecrafts.special.elemental;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Regression coverage for Earth Mace's vanilla smash post-hit lifecycle. */
public final class EarthMaceSmashRegressionGameTests {
    @GameTest
    public void smashResetsFallDistanceAndStartsExactlyOneElementalWave(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EARTH_MACE));
        var target = ctx.spawnMob(EntityTypes.HUSK, 2, 2, 2).withNoFreeWill().spawn();
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        target.setHealth(200.0F);
        EarthSeismicRuntime.clear();
        player.fallDistance = 6.0F;
        float healthBefore = target.getHealth();

        player.attack(target);

        ctx.assertTrue(target.getHealth() < healthBefore, "a real Earth Mace smash damages its target");
        ctx.assertTrue(player.fallDistance == 0.0F, "vanilla smash post-hit processing resets fall distance");
        ctx.assertTrue(EarthSeismicRuntime.activeWaveCount() == 1,
                "the successful smash starts one Earth wave even with vanilla post-hit processing enabled");
        ctx.succeed();
    }
}
