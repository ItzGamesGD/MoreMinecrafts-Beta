package dev.itzgamesgd.moreminecrafts.fishing;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
public final class FishItem extends Item {
 private final FishSpecies species;
 public FishItem(FishSpecies species, Properties properties){super(properties);this.species=species;}
 public FishSpecies species(){return species;}
 @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                      Consumer<Component> tooltip, TooltipFlag flag) {
  tooltip.accept(Component.translatable("item.moreminecrafts.custom_fish.sale_hint"));
 }
}
