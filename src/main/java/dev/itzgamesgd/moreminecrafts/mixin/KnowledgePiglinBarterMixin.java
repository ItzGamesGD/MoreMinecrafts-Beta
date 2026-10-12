package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Carries the actual gold offerer through vanilla's asynchronous admiration/barter sequence. */
@Mixin(PiglinAi.class)
abstract class KnowledgePiglinBarterMixin {
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private static void moreminecrafts$rememberDirectOffer(ServerLevel level, Piglin piglin, Player player,
            InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer serverPlayer && cir.getReturnValue().consumesAction()
                && piglin.getOffhandItem().is(Items.GOLD_INGOT))
            KnowledgeEventCollectors.rememberPiglinOffer(piglin, serverPlayer);
    }

    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private static void moreminecrafts$rememberDroppedOffer(ServerLevel level, Piglin piglin,
            ItemEntity item, CallbackInfo ci) {
        if (item.getItem().is(Items.GOLD_INGOT) && item.getOwner() instanceof ServerPlayer serverPlayer)
            KnowledgeEventCollectors.rememberPiglinOffer(piglin, serverPlayer);
    }

    @WrapOperation(method = "stopHoldingOffHandItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;throwItems(Lnet/minecraft/world/entity/monster/piglin/Piglin;Ljava/util/List;)V", ordinal = 0))
    private static void moreminecrafts$recordSuccessfulBarter(Piglin piglin, List<ItemStack> barterDrops,
            Operation<Void> original) {
        original.call(piglin, barterDrops);
        if (!barterDrops.isEmpty()) KnowledgeEventCollectors.completePiglinBarter(piglin);
        else KnowledgeEventCollectors.clearPiglinOffer(piglin);
    }

    @Inject(method = "stopHoldingOffHandItem", at = @At("TAIL"))
    private static void moreminecrafts$clearOffer(ServerLevel level, Piglin piglin, boolean shouldBarter,
            CallbackInfo ci) {
        KnowledgeEventCollectors.clearPiglinOffer(piglin);
    }
}
