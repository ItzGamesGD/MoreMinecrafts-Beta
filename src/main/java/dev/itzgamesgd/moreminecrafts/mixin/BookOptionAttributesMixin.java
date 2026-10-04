package dev.itzgamesgd.moreminecrafts.mixin;
import java.util.function.BiConsumer;
import dev.itzgamesgd.moreminecrafts.progression.T1Operations;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemStack.class)
abstract class BookOptionAttributesMixin {
    @Inject(method="forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",at=@At("RETURN"))
    private void moreminecrafts$attributes(EquipmentSlot slot,BiConsumer<Holder<Attribute>,AttributeModifier> consumer,CallbackInfo ci){
        ItemStack stack=(ItemStack)(Object)this;
        if(slot==EquipmentSlot.MAINHAND){int sweep=T1Operations.option(stack,"sweeping_edge");if(sweep>0)consumer.accept(Attributes.SWEEPING_DAMAGE_RATIO,new AttributeModifier(ModIds.id("book_option_sweeping"),.035*sweep,AttributeModifier.Operation.ADD_VALUE));}
        if(slot==EquipmentSlot.HEAD||slot==EquipmentSlot.CHEST||slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET){
            // Only the armor's own equipped slot may contribute; prevents offhand armor bonuses.
            var equippable=stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);if(equippable==null||equippable.slot()!=slot)return;
            int fire=T1Operations.option(stack,"fire_protection"),blast=T1Operations.option(stack,"blast_protection");
            if(fire>0)consumer.accept(Attributes.BURNING_TIME,new AttributeModifier(ModIds.id("book_option_fire_"+slot.getName()),-.05*fire,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if(blast>0)consumer.accept(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,new AttributeModifier(ModIds.id("book_option_blast_"+slot.getName()),.025*blast,AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
