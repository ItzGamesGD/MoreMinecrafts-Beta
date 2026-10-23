package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A registered accessory with immutable category metadata. */
public final class AccessoryItem extends Item {
    private final AccessoryDefinition definition;

    public AccessoryItem(AccessoryDefinition definition, Properties properties) {
        super(properties);
        this.definition = definition;
    }

    public AccessoryDefinition definition() { return definition; }
    public AccessoryCategory category() { return definition.category(); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.moreminecrafts.accessory_category", category().displayName())
                .withStyle(ChatFormatting.GRAY));
    }

}
