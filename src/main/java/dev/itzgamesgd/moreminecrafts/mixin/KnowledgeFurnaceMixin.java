package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Awards only recipe-backed ore smelts whose output the player successfully collects. */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class KnowledgeFurnaceMixin {
    @Shadow @Final private Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed;

    @Inject(method = "awardUsedRecipesAndPopExperience", at = @At("HEAD"))
    private void moreminecrafts$recordCollectedOreSmelts(ServerPlayer player, CallbackInfo ci) {
        var completed = new ArrayList<RecipeHolder<?>>();
        for (ResourceKey<Recipe<?>> recipeKey : recipesUsed.keySet())
            player.level().recipeAccess().byKey(recipeKey).ifPresent(completed::add);
        KnowledgeEventCollectors.smeltingCollected(player, completed);
    }
}
