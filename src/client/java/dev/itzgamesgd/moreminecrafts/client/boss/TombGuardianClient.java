package dev.itzgamesgd.moreminecrafts.client.boss;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.DisplayRenderer;

/** Client registration for the Tomb Guardian renderer and its telegraph displays. */
public final class TombGuardianClient {
    private TombGuardianClient() {}

    public static void register() {
        TombGuardianRenderer.registerLayer();
        EntityRendererRegistry.register(TombGuardianEntities.TOMB_GUARDIAN, TombGuardianRenderer::new);
        // The magic circle and inscribed statue are plain item displays drawn by the vanilla display renderer.
        EntityRendererRegistry.register(TombGuardianEntities.TOMB_VFX, context -> new DisplayRenderer.ItemDisplayRenderer(context) {});
    }
}
