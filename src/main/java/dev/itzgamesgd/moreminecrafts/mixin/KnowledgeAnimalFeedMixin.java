package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records a species only after a successful animal interaction consumes its breeding food. */
@Mixin(Animal.class)
abstract class KnowledgeAnimalFeedMixin {
    private static final Map<UUID, FeedSnapshot> MOREMINECRAFTS$FEED_SNAPSHOTS = new HashMap<>();
    private record FeedSnapshot(Item item, int count) {}

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void moreminecrafts$snapshotFeed(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> result) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            MOREMINECRAFTS$FEED_SNAPSHOTS.put(serverPlayer.getUUID(),
                    new FeedSnapshot(held.getItem(), held.getCount()));
        }
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void moreminecrafts$recordSuccessfulFeed(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> result) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        FeedSnapshot before = MOREMINECRAFTS$FEED_SNAPSHOTS.remove(serverPlayer.getUUID());
        if (before == null || player.level().isClientSide()
                || !result.getReturnValue().consumesAction()) return;
        Animal animal = (Animal) (Object) this;
        ItemStack offered = new ItemStack(before.item());
        ItemStack after = player.getItemInHand(hand);
        if (before.count() <= after.getCount() || !animal.isFood(offered)) return;
        KnowledgeRuntime.recordDistinctCondition(serverPlayer, "animal_types_fed",
                String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType())));
    }
}
