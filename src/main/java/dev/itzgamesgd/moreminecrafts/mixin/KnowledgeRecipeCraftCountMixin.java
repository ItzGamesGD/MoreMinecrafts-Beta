package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts an actual player take from the crafting output, once per completed recipe action. */
@Mixin(ResultSlot.class)
abstract class KnowledgeRecipeCraftCountMixin {
    @Inject(method = "onTake", at = @At("TAIL"))
    private void moreminecrafts$countCompletedRecipe(Player player, ItemStack crafted,
                                                      CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer) || crafted.isEmpty()) return;
        ResearchProgressState state = KnowledgeRuntime.state(serverPlayer);
        KnowledgeRuntime.recordCondition(serverPlayer, "recipes_completed",
                state.conditionProgress("recipes_completed") + 1);
        if (crafted.has(net.minecraft.core.component.DataComponents.FOOD))
            KnowledgeRuntime.recordDistinctCondition(serverPlayer, "foods_crafted_or_cooked",
                    String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crafted.getItem())));
    }
}
