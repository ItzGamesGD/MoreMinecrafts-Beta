package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.client.ProgressionClientApi;
import dev.itzgamesgd.moreminecrafts.network.BrewingResearchActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Side panel on BrewingStandScreen for `.68`/`.70` Mixing/Concentration and `.69` 9+9 Process Control. */
@Mixin(AbstractContainerScreen.class)
abstract class ResearchBrewingStandScreenMixin extends Screen {
    protected ResearchBrewingStandScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void moreminecrafts$addBrewingResearchButtons(CallbackInfo ci) {
        if (!((Object) this instanceof BrewingStandScreen)) return;
        boolean hasMix = ProgressionClientApi.researchLevel("v3.research.68") > 0
                || ProgressionClientApi.researchLevel("v3.research.70") > 0;
        boolean hasProcess = ProgressionClientApi.researchLevel("v3.research.69") > 0;
        if (!hasMix && !hasProcess) return;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.moreminecrafts$getLeftPos() + 178;
        int y = accessor.moreminecrafts$getTopPos() + 6;
        if (hasMix) {
            addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.brewing.mix_conc"),
                            b -> sendAction(BrewingResearchActionPayload.ACTION_MIX_OR_CONCENTRATE))
                    .bounds(x, y, 66, 16).build());
            y += 20;
        }
        if (hasProcess) {
            addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.brewing.auto_toggle"),
                            b -> sendAction(BrewingResearchActionPayload.ACTION_TOGGLE_AUTO))
                    .bounds(x, y, 66, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.brewing.queue"),
                            b -> sendAction(BrewingResearchActionPayload.ACTION_QUEUE_CARRIED))
                    .bounds(x, y + 18, 66, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.brewing.collect"),
                            b -> sendAction(BrewingResearchActionPayload.ACTION_COLLECT_OUTPUTS))
                    .bounds(x, y + 36, 66, 16).build());
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void moreminecrafts$renderBrewingResearchStatus(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                            float delta, CallbackInfo ci) {
        if (!((Object) this instanceof BrewingStandScreen)) return;
        if (ProgressionClientApi.researchLevel("v3.research.69") <= 0) return;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.moreminecrafts$getLeftPos() + 178;
        int y = accessor.moreminecrafts$getTopPos() + 84;
        var status = ProgressionClientApi.brewingAutoStatus();
        graphics.text(Minecraft.getInstance().font,
                Component.translatable("moreminecrafts.ui.brewing.status",
                        status.autoEnabled() ? "ON" : "OFF", status.queuedCount(), status.outputCount()),
                x, y, status.autoEnabled() ? 0xFF55FF55 : 0xFFCCCCCC);
    }

    private static void sendAction(int action) {
        if (ClientPlayNetworking.canSend(BrewingResearchActionPayload.TYPE)) {
            ClientPlayNetworking.send(new BrewingResearchActionPayload(action));
        }
    }
}
