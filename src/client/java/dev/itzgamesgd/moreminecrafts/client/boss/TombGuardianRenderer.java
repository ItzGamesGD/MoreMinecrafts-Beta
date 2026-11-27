package dev.itzgamesgd.moreminecrafts.client.boss;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardian;
import dev.itzgamesgd.moreminecrafts.client.model.BoneModelLoader;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Custom Blockbench colossus renderer for the Tomb Guardian. Loaded from
 * {@code assets/moreminecrafts/entity_models/tomb_guardian.json} at 1.0x scale (the .bbmodel is already full 4.22m
 * scale matching the 1.35x4.35 hitbox).
 */
public final class TombGuardianRenderer extends MobRenderer<TombGuardian, TombGuardianRenderState, TombGuardianModel> {
    public static final Identifier MODEL = ModIds.id("entity_models/tomb_guardian.json");
    private static final Identifier TEXTURE = ModIds.id("textures/entity/tomb_guardian/tomb_guardian.png");

    public TombGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new TombGuardianModel(BoneModelLoader.load(context.getResourceManager(), MODEL)), 1.35F);
    }

    /** Kept for TombGuardianClient compatibility; BoneModelLoader builds the ModelPart tree from JSON. */
    public static void registerLayer() {}

    @Override
    public TombGuardianRenderState createRenderState() {
        return new TombGuardianRenderState();
    }

    @Override
    public void extractRenderState(TombGuardian guardian, TombGuardianRenderState state, float partialTick) {
        super.extractRenderState(guardian, state, partialTick);
        state.enraged = guardian.isEnraged();
        state.emergeAnimationState.copyFrom(guardian.emergeAnimationState);
        state.sweepAnimationState.copyFrom(guardian.sweepAnimationState);
        state.slamAnimationState.copyFrom(guardian.slamAnimationState);
        state.spikeAnimationState.copyFrom(guardian.spikeAnimationState);
        state.summonAnimationState.copyFrom(guardian.summonAnimationState);
        state.ultimateAnimationState.copyFrom(guardian.ultimateAnimationState);
    }

    @Override
    public Identifier getTextureLocation(TombGuardianRenderState state) {
        return TEXTURE;
    }
}
