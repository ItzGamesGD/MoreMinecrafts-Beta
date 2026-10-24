package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int moreminecrafts$getLeftPos();

    @Accessor("topPos")
    int moreminecrafts$getTopPos();

    @Accessor("hoveredSlot")
    Slot moreminecrafts$getHoveredSlot();
}
