package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the vanilla quad's alpha setter for the client-only cloud wisp. */
@Mixin(SingleQuadParticle.class)
public interface WraithParticleAlphaInvoker {
    @Invoker("setAlpha") void moreminecrafts$setWraithAlpha(float alpha);
}
