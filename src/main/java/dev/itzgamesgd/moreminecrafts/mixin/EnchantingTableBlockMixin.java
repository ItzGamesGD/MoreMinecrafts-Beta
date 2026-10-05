package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.progression.EnchantingProgressionMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnchantingTableBlock.class)
abstract class EnchantingTableBlockMixin {
    @Inject(method="getMenuProvider",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$progression(BlockState state,Level level,BlockPos pos,CallbackInfoReturnable<MenuProvider> cir){
        cir.setReturnValue(new SimpleMenuProvider((id,inventory,player)->new EnchantingProgressionMenu(id,inventory,ContainerLevelAccess.create(level,pos)),Component.translatable("menu.moreminecrafts.enchantment")));
    }
}
