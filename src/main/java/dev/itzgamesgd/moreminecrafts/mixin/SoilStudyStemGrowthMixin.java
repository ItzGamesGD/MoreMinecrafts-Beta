package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.ResearchPassiveMath;
import dev.itzgamesgd.moreminecrafts.progression.SoilStudyRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Soil Study L3 relative bonus on a stem's ordinary growth-stage roll only (not fruit placement). */
@Mixin(StemBlock.class)
abstract class SoilStudyStemGrowthMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int moreminecrafts$increaseNaturalGrowthProbability(RandomSource random, int bound,
            Operation<Integer> original, BlockState state, ServerLevel level, BlockPos pos, RandomSource tickRandom) {
        int roll = original.call(random, bound);
        if (roll == 0 || bound <= 1 || state.getValue(StemBlock.AGE) >= 7) return roll;
        double bonus = SoilStudyRuntime.cropGrowthBonus(level, pos);
        if (bonus <= 0.0D) return roll;
        double extraChance = ResearchPassiveMath.conditionalExtraProbability(1.0D / bound, bonus);
        return random.nextDouble() < extraChance ? 0 : roll;
    }
}
