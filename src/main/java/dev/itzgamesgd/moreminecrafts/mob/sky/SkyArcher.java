package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

/**
 * 천공 궁수 / Sky Archer (group B, §5 #2): vanilla Skeleton with its vanilla bow loadout, AI and sounds.
 *
 * <p>Not the group C Skyguard Archer ({@code skyguard_archer}). The powder-snow Stray conversion is disabled
 * (required policy): the explicit entry point is a no-op here and the per-tick freezing tracker of {@link Skeleton}
 * is skipped for this type by {@code SkyArcherFreezeConversionMixin}. Freezing damage itself stays vanilla.
 */
public class SkyArcher extends Skeleton {
    public SkyArcher(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    /** Required policy: never converts to a Stray. */
    @Override
    public void startFreezeConversion(int time) {
    }
}
