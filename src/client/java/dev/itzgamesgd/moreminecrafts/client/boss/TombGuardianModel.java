package dev.itzgamesgd.moreminecrafts.client.boss;

import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.warden.WardenModel;
import net.minecraft.client.renderer.entity.state.WardenRenderState;

/**
 * Warden skeleton with the Tomb Guardian's attack keyframes layered on top. The vanilla pass (emerge, walk, head
 * look, tendrils) runs first and is untouched; the five pattern animations then add their rotations to the same bones.
 */
final class TombGuardianModel extends WardenModel {
    private final KeyframeAnimation sweep;
    private final KeyframeAnimation slam;
    private final KeyframeAnimation spikes;
    private final KeyframeAnimation summon;
    private final KeyframeAnimation ultimate;

    TombGuardianModel(ModelPart root) {
        super(root);
        sweep = TombGuardianAnimations.SWEEP.bake(root);
        slam = TombGuardianAnimations.SLAM.bake(root);
        spikes = TombGuardianAnimations.SPIKES.bake(root);
        summon = TombGuardianAnimations.SUMMON.bake(root);
        ultimate = TombGuardianAnimations.ULTIMATE.bake(root);
    }

    @Override
    public void setupAnim(WardenRenderState state) {
        super.setupAnim(state);
        if (state instanceof TombGuardianRenderState guardian) {
            sweep.apply(guardian.sweepAnimationState, guardian.ageInTicks);
            slam.apply(guardian.slamAnimationState, guardian.ageInTicks);
            spikes.apply(guardian.spikeAnimationState, guardian.ageInTicks);
            summon.apply(guardian.summonAnimationState, guardian.ageInTicks);
            ultimate.apply(guardian.ultimateAnimationState, guardian.ageInTicks);
        }
    }
}
