package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Counts successful dirt-to-farmland conversions, not hoe interaction attempts. */
@Mixin(HoeItem.class)
abstract class KnowledgeHoeUseMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void moreminecrafts$recordCultivation(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> result) {
        if (context.getLevel().isClientSide() || !result.getReturnValue().consumesAction()
                || !(context.getPlayer() instanceof ServerPlayer player)
                || !context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.FARMLAND)) return;
        KnowledgeEventCollectors.successfulFarmlandCultivation(player);
    }
}
