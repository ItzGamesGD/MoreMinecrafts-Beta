package dev.itzgamesgd.moreminecrafts.client.crystalguardian;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** Render state: vanilla living state (walk position/speed) plus the swing animation clock. */
public final class AncientCrystalGuardianRenderState extends LivingEntityRenderState {
    public final AnimationState swingAnimationState = new AnimationState();
}
