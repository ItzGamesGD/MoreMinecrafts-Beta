package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.client.ProgressionClientApi;
import dev.itzgamesgd.moreminecrafts.network.HopperFilterPayload;
import dev.itzgamesgd.moreminecrafts.progression.HopperFilterState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Side filter panel on HopperScreen once `hopper_filter` is owned. */
@Mixin(AbstractContainerScreen.class)
abstract class ResearchHopperFilterScreenMixin extends Screen {
    protected ResearchHopperFilterScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void moreminecrafts$addHopperFilterWidgets(CallbackInfo ci) {
        if (!((Object) this instanceof HopperScreen)) return;
        if (ProgressionClientApi.researchLevel("hopper_filter") <= 0) return;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.moreminecrafts$getLeftPos() + 178;
        int y = accessor.moreminecrafts$getTopPos() + 6;
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.hopper_filter.mode"),
                b -> sendAction(HopperFilterPayload.ACTION_TOGGLE_MODE, -1)).bounds(x, y, 64, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("moreminecrafts.ui.hopper_filter.clear"),
                b -> sendAction(HopperFilterPayload.ACTION_CLEAR, -1)).bounds(x, y + 18, 64, 16).build());
        for (int slot = 0; slot < HopperFilterState.MAX_ENTRIES; slot++) {
            final int slotIndex = slot;
            int col = slot % 3;
            int row = slot / 3;
            addRenderableWidget(Button.builder(Component.literal("·"),
                            b -> sendAction(HopperFilterPayload.ACTION_SET_OR_TOGGLE_SLOT, slotIndex))
                    .bounds(x + col * 20, y + 40 + row * 20, 18, 18).build());
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void moreminecrafts$renderHopperFilterOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                          float delta, CallbackInfo ci) {
        if (!((Object) this instanceof HopperScreen)) return;
        if (ProgressionClientApi.researchLevel("hopper_filter") <= 0) return;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.moreminecrafts$getLeftPos() + 178;
        int y = accessor.moreminecrafts$getTopPos() + 6;
        HopperFilterState state = ProgressionClientApi.hopperFilterState();
        Component modeLabel = Component.translatable(state.whitelist()
                ? "moreminecrafts.ui.hopper_filter.whitelist"
                : "moreminecrafts.ui.hopper_filter.blacklist");
        graphics.text(Minecraft.getInstance().font, modeLabel, x, y + 104, 0xFFE0E0E0);

        for (int i = 0; i < state.entries().size() && i < HopperFilterState.MAX_ENTRIES; i++) {
            var entry = state.entries().get(i);
            Identifier id = Identifier.tryParse(entry.itemId());
            if (id == null) continue;
            var item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null) {
                int col = i % 3;
                int row = i / 3;
                graphics.item(new ItemStack(item), x + col * 20 + 1, y + 40 + row * 20 + 1);
            }
        }
    }

    private static void sendAction(int action, int slot) {
        if (ClientPlayNetworking.canSend(HopperFilterPayload.TYPE)) {
            ClientPlayNetworking.send(new HopperFilterPayload(action, slot));
        }
    }
}
