package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchPassiveMath;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adds a relative 1% chance only to CropBlock's ordinary random growth roll. */
@Mixin(CropBlock.class)
abstract class ResearchCropGrowthMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int moreminecrafts$increaseNaturalGrowthProbability(RandomSource random, int bound,
            Operation<Integer> original, BlockState state, ServerLevel level, BlockPos pos, RandomSource tickRandom) {
        int roll = original.call(random, bound);
        if (roll == 0 || bound <= 1 || !moreminecrafts$hasNearbyResearcher(level, pos)) return roll;
        double extraChance = ResearchPassiveMath.conditionalExtraProbability(1.0D / bound, 0.01D);
        return random.nextDouble() < extraChance ? 0 : roll;
    }

    private static boolean moreminecrafts$hasNearbyResearcher(ServerLevel level, BlockPos pos) {
        Vec3 cropCenter = Vec3.atCenterOf(pos);
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && KnowledgeRuntime.researched(player, "living_root")
                    && player.position().distanceToSqr(cropCenter) <= 256.0D) return true;
        }
        return false;
    }
}
