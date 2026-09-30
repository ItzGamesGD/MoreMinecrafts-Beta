package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @WrapMethod(method = "destroyBlock")
    private boolean moreminecrafts$scopeBlockBreak(BlockPos pos, Operation<Boolean> original) {
        EnchantmentRuntime.enterBlockBreak();
        try {
            return original.call(pos);
        } finally {
            EnchantmentRuntime.exitBlockBreak();
        }
    }
}
