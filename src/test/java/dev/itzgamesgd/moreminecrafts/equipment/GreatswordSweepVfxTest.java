package dev.itzgamesgd.moreminecrafts.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class GreatswordSweepVfxTest {
    @Test
    void sweepParticleIsPlacedAheadOfTheAttackerAtVanillaSweepHeight() {
        Vec3 origin = new Vec3(4.0, 10.0, -3.0);
        Vec3 facing = new Vec3(0.0, 0.0, 1.0);

        Vec3 position = GreatswordSweepVfx.position(origin, facing);

        assertEquals(new Vec3(4.0, 10.8, -2.0), position);
    }

    @Test
    void verticalLookUsesStableHorizontalFallback() {
        Vec3 position = GreatswordSweepVfx.position(Vec3.ZERO, new Vec3(0.0, 1.0, 0.0));

        assertEquals(new Vec3(0.0, 0.8, 1.0), position);
    }
}
