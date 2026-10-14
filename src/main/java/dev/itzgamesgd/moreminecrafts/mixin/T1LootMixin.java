package dev.itzgamesgd.moreminecrafts.mixin;
import java.util.function.Consumer;
import dev.itzgamesgd.moreminecrafts.progression.T1Supply;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LootTable.class)
abstract class T1LootMixin {
    @Inject(method="createStackSplitter",at=@At("RETURN"),cancellable=true)
    private static void moreminecrafts$filterSupply(ServerLevel level,Consumer<ItemStack> output,CallbackInfoReturnable<Consumer<ItemStack>> cir){
        Consumer<ItemStack> vanilla=cir.getReturnValue();
        cir.setReturnValue(stack->{ItemStack filtered=T1Supply.filterLoot(stack,level.getRandom()::nextDouble);if(!filtered.isEmpty())vanilla.accept(filtered);});
    }
}
