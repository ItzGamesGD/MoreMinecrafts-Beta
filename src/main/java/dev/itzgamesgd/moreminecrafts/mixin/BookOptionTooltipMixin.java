package dev.itzgamesgd.moreminecrafts.mixin;
import java.util.function.Consumer;
import java.util.*;
import dev.itzgamesgd.moreminecrafts.progression.BookOptionRules;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ItemEnchantments.class)
abstract class BookOptionTooltipMixin {
    @ModifyVariable(method="addToTooltip",at=@At("HEAD"),argsOnly=true)
    private Consumer<Component> moreminecrafts$optionTooltip(Consumer<Component> consumer,Item.TooltipContext context,Consumer<Component> ignored,TooltipFlag flag,DataComponentGetter components){
        Map<String,Integer> options=components.getOrDefault(ModDataComponents.BOOK_OPTIONS,Map.of());
        if(options.isEmpty())return consumer;
        ItemEnchantments ench=(ItemEnchantments)(Object)this;
        return line->{for(var entry:ench.entrySet()){
            String id=entry.getKey().unwrapKey().map(k->k.identifier().toString()).orElse("");int option=options.getOrDefault(id,0);
            if(option>0&&line.equals(Enchantment.getFullname(entry.getKey(),entry.getIntValue()))){consumer.accept(line.copy().append(" (+"+BookOptionRules.roman(option)+")"));return;}
        }consumer.accept(line);};
    }
}
