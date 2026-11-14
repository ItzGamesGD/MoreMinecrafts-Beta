package dev.itzgamesgd.moreminecrafts.client.crystalguardian;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.test.crystalguardian.AncientCrystalGuardianTest;
import dev.itzgamesgd.moreminecrafts.test.crystalguardian.AncientCrystalGuardianTestEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the POC test guardian. The model is loaded from {@code assets/moreminecrafts/entity_models/ancient_crystal_guardian.json}
 * through the resource manager (so resource packs / reloads apply) and drawn by the stock vanilla living-entity pipeline.
 */
public final class AncientCrystalGuardianRenderer
        extends MobRenderer<AncientCrystalGuardianTest, AncientCrystalGuardianRenderState, AncientCrystalGuardianModel> {
    public static final Identifier MODEL = ModIds.id("entity_models/ancient_crystal_guardian.json");
    private static final Identifier TEXTURE = ModIds.id("textures/entity/ancient_crystal_guardian/ancient_crystal_guardian.png");

    public AncientCrystalGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new AncientCrystalGuardianModel(BoneModelLoader.load(context.getResourceManager(), MODEL)), 1.6F);
    }

    /** Verification hook for the real-client GameTest. */
    public AncientCrystalGuardianModel debugModel() { return model; }

    public static void register() {
        EntityRendererRegistry.register(AncientCrystalGuardianTestEntities.TYPE, AncientCrystalGuardianRenderer::new);
    }

    @Override public AncientCrystalGuardianRenderState createRenderState() { return new AncientCrystalGuardianRenderState(); }

    @Override
    public void extractRenderState(AncientCrystalGuardianTest entity, AncientCrystalGuardianRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.swingAnimationState.copyFrom(entity.swingAnimationState);
    }

    @Override public Identifier getTextureLocation(AncientCrystalGuardianRenderState state) { return TEXTURE; }
}
