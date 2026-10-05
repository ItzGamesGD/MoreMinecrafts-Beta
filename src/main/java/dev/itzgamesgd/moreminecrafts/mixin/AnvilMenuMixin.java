package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.progression.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Adds synchronized workstation modes while leaving normal repair/name calculation vanilla. */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin extends ItemCombinerMenu implements T1AnvilModes {
    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;
    @Unique private final DataSlot moreminecrafts$mode=DataSlot.standalone();
    protected AnvilMenuMixin(MenuType<?> type,int id,Inventory inventory,ContainerLevelAccess access,ItemCombinerMenuSlotDefinition slots){super(type,id,inventory,access,slots);}
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("RETURN"))
    private void moreminecrafts$syncMode(int id,Inventory inventory,ContainerLevelAccess access,CallbackInfo ci){addDataSlot(moreminecrafts$mode);}
    @Override public int moreminecrafts$mode(){return moreminecrafts$mode.get();}
    @Override public boolean moreminecrafts$button(Player player,int action){
        if(player.level().isClientSide()||!stillValid(player))return false;
        if(action>=0&&action<=2){moreminecrafts$mode.set(action);((AnvilMenu)(Object)this).createResult();return true;}
        if(action==3&&moreminecrafts$mode()==0){
            boolean changed=EnhancementOperation.apply(player,getSlot(0).getItem(),getSlot(1).getItem());
            if(changed){
                inputSlots.setChanged();cost.set(0);repairItemCountCost=0;
                ((AnvilMenu)(Object)this).createResult();
                T1AnvilUse.complete(access,player,player.getRandom()::nextDouble);
            }
            return changed;
        }
        if(action==3&&moreminecrafts$mode()==2&&getSlot(2).hasItem()&&getSlot(2).mayPickup(player)){quickMoveStack(player,2);return true;}
        return false;
    }
    @Inject(method="createResult",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$result(CallbackInfo ci){
        AnvilMenu menu=(AnvilMenu)(Object)this;ItemStack equipment=getSlot(0).getItem(),addition=getSlot(1).getItem();
        if(moreminecrafts$mode()==1){
            if(addition.is(Items.ENCHANTED_BOOK)||equipment.is(Items.ENCHANTED_BOOK)||EnhancementOperation.isEnhancementMaterial(addition)){
                getSlot(2).set(ItemStack.EMPTY);cost.set(0);broadcastChanges();ci.cancel();
            }return;
        }
        if(moreminecrafts$mode()==0){
            getSlot(2).set(ItemStack.EMPTY);cost.set(0);repairItemCountCost=0;
        }else {
            var result=T1Operations.combine(player,equipment,addition);
            getSlot(2).set(result.orElse(ItemStack.EMPTY));cost.set(result.map(T1Operations::combineCost).orElse(0));repairItemCountCost=1;
        }
        broadcastChanges();ci.cancel();
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="createResult",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentsForCrafting(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/enchantment/ItemEnchantments;"))
    private net.minecraft.world.item.enchantment.ItemEnchantments moreminecrafts$ignoreSacrificeEnchantments(
            ItemStack stack,com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.item.enchantment.ItemEnchantments> original){
        return moreminecrafts$mode()==1 && stack==inputSlots.getItem(1)
                ? net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY : original.call(stack);
    }
    @Inject(method="createResult",at=@At("RETURN"))
    private void moreminecrafts$preservePurchasedEnchantments(CallbackInfo ci){
        if(moreminecrafts$mode()!=1 || !getSlot(2).hasItem())return;
        ItemStack original=getSlot(0).getItem(),result=getSlot(2).getItem();
        result.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                original.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY));
        if(result.getDamageValue()==original.getDamageValue()
                && java.util.Objects.equals(result.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME),original.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME))){
            getSlot(2).set(ItemStack.EMPTY);cost.set(0);
        }
        broadcastChanges();
    }
    @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$bookPayment(Player player,boolean hasItem,CallbackInfoReturnable<Boolean> cir){
        if(moreminecrafts$mode()==0)cir.setReturnValue(false);
        else if(moreminecrafts$mode()==2)cir.setReturnValue(hasItem&&cost.get()>0&&RawXp.canPay(player,cost.get()));
    }
    @Inject(method="onTake",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$takeBook(Player player,ItemStack carried,CallbackInfo ci){
        if(moreminecrafts$mode()!=2)return;
        if(player.level().isClientSide()){ci.cancel();return;}
        if(!RawXp.pay(player,cost.get())){
            getSlot(2).set(carried.copy());
            broadcastChanges();
            ci.cancel();
            return;
        }
        inputSlots.getItem(0).shrink(1);inputSlots.getItem(1).shrink(1);cost.set(0);
        inputSlots.setChanged();((AnvilMenu)(Object)this).createResult();
        T1AnvilUse.complete(access,player,player.getRandom()::nextDouble);ci.cancel();
    }
}
