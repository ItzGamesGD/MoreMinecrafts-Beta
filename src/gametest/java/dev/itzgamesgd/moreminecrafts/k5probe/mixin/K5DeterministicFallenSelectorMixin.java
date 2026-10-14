package dev.itzgamesgd.moreminecrafts.k5probe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.RandomSelectorFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Forces one registered vanilla rare branch only when the K5 deterministic fixture arms it. */
@Mixin(RandomSelectorFeature.class)
abstract class K5DeterministicFallenSelectorMixin {
    @WrapOperation(method = "place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
    private float moreminecrafts$forceSelectedVanillaFallenBranch(RandomSource random,
            Operation<Float> original) {
        return K5NaturalTreeProbe.consumeDeterministicFallenBranch((RandomSelectorFeature) (Object) this)
                ? 0.0F : original.call(random);
    }
}
