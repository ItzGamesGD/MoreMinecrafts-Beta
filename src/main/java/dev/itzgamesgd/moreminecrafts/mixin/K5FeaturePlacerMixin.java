package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import org.spongepowered.asm.mixin.Mixin;

/** Associates trunk callbacks with the registry identity of the top-level placed feature. */
@Mixin(FeaturePlacer.class)
abstract class K5FeaturePlacerMixin {
    @WrapMethod(method = "placeWithBiomeCheck(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z")
    private boolean moreminecrafts$placedFeatureIdentity(PlacedFeature placedFeature, RandomSource random,
            BlockPos origin, Operation<Boolean> original) {
        K5NaturalTreeProvenance.PlacedFeatureScope scope = K5NaturalTreeProvenance.enterPlacedFeature(placedFeature);
        try {
            return original.call(placedFeature, random, origin);
        } finally {
            K5NaturalTreeProvenance.exitPlacedFeature(scope);
        }
    }
}
