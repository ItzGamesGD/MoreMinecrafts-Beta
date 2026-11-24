package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.client.ProgressionClientApi;
import dev.itzgamesgd.moreminecrafts.network.ChestSortPayload;
import dev.itzgamesgd.moreminecrafts.progression.ChestSortRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds Category and Name sort buttons to vanilla ContainerScreen when `v3.research.13` is owned. */
@Mixin(AbstractContainerScreen.class)
abstract class ResearchChestSortScreenMixin extends Screen {
    protected ResearchChestSortScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void moreminecrafts$addChestSortButtons(CallbackInfo ci) {
        if (!((Object) this instanceof ContainerScreen)) return;
        if (ProgressionClientApi.researchLevel("v3.research.13") <= 0) return;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.moreminecrafts$getLeftPos() + 178;
        int y = accessor.moreminecrafts$getTopPos() + 6;
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.chest_sort.category"),
                btn -> sendSort(ChestSortRules.MODE_CATEGORY)).bounds(x, y, 52, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.chest_sort.name"),
                btn -> sendSort(ChestSortRules.MODE_NAME)).bounds(x, y + 18, 52, 16).build());
    }

    private static void sendSort(int mode) {
        if (ClientPlayNetworking.canSend(ChestSortPayload.TYPE)) {
            ClientPlayNetworking.send(new ChestSortPayload(mode));
        }
    }
}
