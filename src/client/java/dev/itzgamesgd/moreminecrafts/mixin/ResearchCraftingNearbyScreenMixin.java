package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.client.ProgressionClientApi;
import dev.itzgamesgd.moreminecrafts.network.NearbyChestPullPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Side panel on CraftingScreen showing nearby chest ingredients for `nearby_chest_ingredients`. */
@Mixin(AbstractContainerScreen.class)
abstract class ResearchCraftingNearbyScreenMixin extends Screen {
    protected ResearchCraftingNearbyScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void moreminecrafts$addNearbyChestButtons(CallbackInfo ci) {
        if (!((Object) this instanceof CraftingScreen)) return;
        if (ProgressionClientApi.researchLevel("nearby_chest_ingredients") <= 0) return;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.moreminecrafts$getLeftPos() + 178;
        int y = accessor.moreminecrafts$getTopPos() + 20;
        for (int i = 0; i < 9; i++) {
            final int index = i;
            int col = i % 3;
            int row = i / 3;
            addRenderableWidget(Button.builder(Component.literal(" "), b -> {
                var entries = ProgressionClientApi.nearbyChestEntries();
                if (index < entries.size() && ClientPlayNetworking.canSend(NearbyChestPullPayload.TYPE)) {
                    ClientPlayNetworking.send(new NearbyChestPullPayload(entries.get(index).itemId(), 64));
                }
            }).bounds(x + col * 20, y + row * 20, 18, 18).build());
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void moreminecrafts$renderNearbyChestItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                       float delta, CallbackInfo ci) {
        if (!((Object) this instanceof CraftingScreen)) return;
        if (ProgressionClientApi.researchLevel("nearby_chest_ingredients") <= 0) return;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        int x = accessor.moreminecrafts$getLeftPos() + 178;
        int y = accessor.moreminecrafts$getTopPos() + 6;
        graphics.text(Minecraft.getInstance().font,
                Component.translatable("moreminecrafts.ui.nearby_chests.title"), x, y, 0xFFE0E0E0);

        var entries = ProgressionClientApi.nearbyChestEntries();
        for (int i = 0; i < entries.size() && i < 9; i++) {
            var entry = entries.get(i);
            Identifier id = Identifier.tryParse(entry.itemId());
            if (id == null) continue;
            var item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null) {
                int col = i % 3;
                int row = i / 3;
                graphics.item(new ItemStack(item), x + col * 20 + 1, y + 14 + row * 20 + 1);
            }
        }
    }
}
