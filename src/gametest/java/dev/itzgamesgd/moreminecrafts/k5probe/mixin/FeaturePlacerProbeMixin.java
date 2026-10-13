package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import org.spongepowered.asm.mixin.Mixin;

/** Associates trunk callbacks with the registry identity of the top-level placed feature. */
@Mixin(FeaturePlacer.class)
abstract class FeaturePlacerProbeMixin {
    @WrapMethod(method = "placeWithBiomeCheck(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z")
    private boolean moreminecrafts$probePlacedFeatureIdentity(PlacedFeature placedFeature, RandomSource random,
            BlockPos origin, Operation<Boolean> original) {
        boolean eligible = K5NaturalTreeProbe.enterPlacedFeature(placedFeature);
        try {
            return original.call(placedFeature, random, origin);
        } finally {
            K5NaturalTreeProbe.exitPlacedFeature(eligible);
        }
    }
}
