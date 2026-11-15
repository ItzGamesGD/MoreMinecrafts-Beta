package dev.itzgamesgd.moreminecrafts.client.boss;

import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.world.entity.AnimationState;

/** Warden-anatomy render state plus the synced enrage flag and one animation state per Tomb Guardian pattern. */
public class TombGuardianRenderState extends WardenRenderState {
    public boolean enraged;
    public final AnimationState sweepAnimationState = new AnimationState();
    public final AnimationState slamAnimationState = new AnimationState();
    public final AnimationState spikeAnimationState = new AnimationState();
    public final AnimationState summonAnimationState = new AnimationState();
    public final AnimationState ultimateAnimationState = new AnimationState();
}
