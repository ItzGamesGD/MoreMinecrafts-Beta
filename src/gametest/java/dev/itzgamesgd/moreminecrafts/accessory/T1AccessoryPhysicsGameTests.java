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

/** Measurements run vanilla 26.3 travel/collision in a real server world; no copied physics model. */
public final class T1AccessoryPhysicsGameTests implements CustomTestMethodInvoker {
    @GameTest public void flatStoneShockwaveAndDoubleJumpUseVanillaPhysics(GameTestHelper ctx) {
        var level = ctx.getLevel();
        BlockPos floor = ctx.absolutePos(new BlockPos(0, 20, 0));
        for (int x = -10; x <= 16; x++) for (int z = -10; z <= 16; z++)
            level.setBlock(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
        var player = T1AccessoryGameTests.player(ctx);
        player.setPos(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5);
        var zombie = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 21, 0));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
        zombie.setOnGround(true);
        zombie.setDeltaMovement(Vec3.ZERO);
        Vec3 start = zombie.position();
        T1AccessoryGameTests.equip(player, AccessoryDefinition.T1_CRISIS_ESCAPE, 0);
        player.setHealth(5);
        T1Shockwave.tryActivate(player);
        double peak = start.y;
        for (int i = 0; i < 35; i++) {
            zombie.travel(Vec3.ZERO);
            peak = Math.max(peak, zombie.getY());
        }
        double horizontal = zombie.position().subtract(start).multiply(1, 0, 1).length();
        System.out.println("D01_PHYSICS shockwave_horizontal=" + horizontal + " rise=" + (peak - start.y));
        ctx.assertTrue(horizontal >= 2 && horizontal <= 3, "flat-stone knockback near 2.5 blocks: " + horizontal);
        ctx.assertTrue(peak - start.y >= 0.4 && peak - start.y <= 0.65, "shockwave rise near 0.5 blocks");
        ctx.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "shockwave deals no direct damage");

        player.setPos(floor.getX() + 10.5, floor.getY() + 5, floor.getZ() + 10.5);
        player.setOnGround(false);
        player.setDeltaMovement(0, -1, 0);
        player.fallDistance = 30;
        player.setHealth(20);
        T1AccessoryGameTests.equip(player, AccessoryDefinition.T1_DOUBLE_JUMP, 0);
        double jumpStart = player.getY();
        ctx.assertTrue(T1AccessoryRuntime.onJump(player), "jump accepted");
        peak = jumpStart;
        for (int i = 0; i < 45; i++) {
            player.travel(Vec3.ZERO);
            peak = Math.max(peak, player.getY());
        }
        double rise = peak - jumpStart;
        System.out.println("D01_PHYSICS double_jump_rise=" + rise + " landed=" + player.onGround()
                + " health=" + player.getHealth() + " fall_distance=" + player.fallDistance);
        ctx.assertTrue(rise >= 1.4 && rise <= 1.6, "additional rise near 1.5 blocks: " + rise);
        ctx.assertTrue(player.onGround(), "vanilla collision lands player");
        ctx.assertTrue(player.getHealth() > 0, "prior thirty-block fall distance was cleared");
        ctx.succeed();
    }
    @Override public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
