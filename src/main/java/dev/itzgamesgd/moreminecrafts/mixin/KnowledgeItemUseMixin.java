package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeConditionSemantics;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes item-on-block results after vanilla has completed the server-side action. */
@Mixin(ItemStack.class)
abstract class KnowledgeItemUseMixin {
    @Unique private static final ThreadLocal<UseSnapshot> MOREMINECRAFTS$USE_SNAPSHOT = new ThreadLocal<>();
    private record UseSnapshot(ServerPlayer player, Item item, BlockPos clicked, BlockPos adjacent) {}

    @Inject(method = "useOn", at = @At("HEAD"))
    private void moreminecrafts$snapshotUse(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> result) {
        if (context.getLevel().isClientSide() || !(context.getPlayer() instanceof ServerPlayer player)) {
            MOREMINECRAFTS$USE_SNAPSHOT.remove();
            return;
        }
        MOREMINECRAFTS$USE_SNAPSHOT.set(new UseSnapshot(player, context.getItemInHand().getItem(),
                context.getClickedPos(), context.getClickedPos().relative(context.getClickedFace())));
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void moreminecrafts$recordSuccessfulUse(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> result) {
        UseSnapshot snapshot = MOREMINECRAFTS$USE_SNAPSHOT.get();
        MOREMINECRAFTS$USE_SNAPSHOT.remove();
        if (snapshot == null || !KnowledgeConditionSemantics.successfulServerAction(
                !context.getLevel().isClientSide(), result.getReturnValue().consumesAction())) return;

        ItemStack originalItem = new ItemStack(snapshot.item());
        Level level = context.getLevel();
        if (originalItem.is(ItemTags.HOES)
                && level.getBlockState(snapshot.clicked()).is(Blocks.FARMLAND))
            KnowledgeEventCollectors.successfulFarmlandCultivation(snapshot.player());

        if (snapshot.item() == Items.REDSTONE
                && (level.getBlockState(snapshot.clicked()).is(Blocks.REDSTONE_WIRE)
                || level.getBlockState(snapshot.adjacent()).is(Blocks.REDSTONE_WIRE)))
            KnowledgeEventCollectors.blockPlaced(snapshot.player(), originalItem);
    }

}
