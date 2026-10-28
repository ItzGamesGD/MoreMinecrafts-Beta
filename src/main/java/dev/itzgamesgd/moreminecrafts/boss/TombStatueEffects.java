package dev.itzgamesgd.moreminecrafts.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Server-sent telegraph visuals for the Tomb Guardian's area attacks. */
final class TombStatueEffects {
    private static final DustParticleOptions CIRCLE_GOLD = new DustParticleOptions(0xFFC440, 1.2F);
    private static final DustParticleOptions WAVE_DUST = new DustParticleOptions(0xC8B078, 1.6F);

    private TombStatueEffects() {}

    /** Mandatory magic-circle tell at the fixed eruption site; visible for the whole warning. */
    static void spawnCircle(ServerLevel level, Vec3 point, int warningTicks) {
        TombVfxDisplay.circle(level, point, warningTicks + 1);
    }

    /** Glyph-ring particle accent drawn along the circle each tick (the display carries the readable shape). */
    static void circleParticles(ServerLevel level, Vec3 point, int tick) {
        double radius = TombGuardianTuning.STATUE_RADIUS;
        int points = 12;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points + tick * 0.08D;
            level.sendParticles(CIRCLE_GOLD, true, true, point.x + Math.cos(angle) * radius, point.y + 0.1D,
                    point.z + Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (tick % 4 == 0)
            level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y + 0.6D, point.z, 8, radius * 0.5D, 0.3D,
                    radius * 0.5D, 0.2D);
    }

    static void spawnStatue(ServerLevel level, Vec3 point, int visibleTicks) {
        TombVfxDisplay.statue(level, point, visibleTicks);
        BlockState floor = level.getBlockState(BlockPos.containing(point).below());
        if (floor.isAir()) floor = Blocks.SANDSTONE.defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), true, true, point.x, point.y + 0.2D,
                point.z, 40, 0.6D, 0.3D, 0.6D, 0.15D);
    }

    /** Outward-moving floor ring of the ground smash, drawn at the current wave front. */
    static void waveRing(ServerLevel level, Vec3 origin, double front) {
        if (front <= 0.0D) return;
        int points = Math.max(10, (int) (front * 10.0D));
        BlockState floor = level.getBlockState(BlockPos.containing(origin).below());
        if (floor.isAir()) floor = Blocks.SANDSTONE.defaultBlockState();
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, floor);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double x = origin.x + Math.cos(angle) * front;
            double z = origin.z + Math.sin(angle) * front;
            level.sendParticles(WAVE_DUST, true, true, x, origin.y + 0.15D, z, 1, 0.0D, 0.05D, 0.0D, 0.0D);
            if (i % 2 == 0)
                level.sendParticles(debris, true, true, x, origin.y + 0.1D, z, 1, 0.1D, 0.1D, 0.1D, 0.1D);
        }
    }
}
