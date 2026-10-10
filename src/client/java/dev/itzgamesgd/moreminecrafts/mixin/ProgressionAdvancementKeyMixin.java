package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.client.ProgressionSelectionScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes the vanilla advancement key through the approved three-choice entry point. */
@Mixin(Minecraft.class)
abstract class ProgressionAdvancementKeyMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void moreminecrafts$openProgressionSelector(CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.options.keyAdvancements.consumeClick() && client.player != null)
            client.gui.setScreen(new ProgressionSelectionScreen());
    }
}
