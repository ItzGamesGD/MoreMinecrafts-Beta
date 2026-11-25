package dev.itzgamesgd.moreminecrafts.mixin;

import com.mojang.serialization.MapCodec;
import dev.itzgamesgd.moreminecrafts.client.PoseMeshRenderer;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers the {@code moreminecrafts:pose_mesh} special item model type next to the vanilla ones. */
@Mixin(SpecialModelRenderers.class)
public abstract class SpecialModelRenderersMixin {
    @Shadow
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("HEAD"))
    private static void moreminecrafts$registerPoseMesh(CallbackInfo ci) {
        ID_MAPPER.put(ModIds.id("pose_mesh"), PoseMeshRenderer.Unbaked.MAP_CODEC);
    }
}
