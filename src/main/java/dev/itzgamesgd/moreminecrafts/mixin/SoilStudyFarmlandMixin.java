package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.SoilStudyRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Soil Study L1 trample resistance and L3 water reach, each applied at one vanilla call site. */
@Mixin(FarmlandBlock.class)
abstract class SoilStudyFarmlandMixin {
    /** Skips only the fall-damage trampling conversion; random-tick and support loss are untouched. */
    @WrapOperation(method = "fallOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToBaseBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void moreminecrafts$resistTrampling(FarmlandBlock farmland, Entity source, BlockState state,
            Level level, BlockPos pos, Operation<Void> original) {
        if (level instanceof ServerLevel serverLevel && SoilStudyRuntime.tramplingResisted(serverLevel, pos)) return;
        original.call(farmland, source, state, level, pos);
    }

    /** Vanilla reach (4) is evaluated first; the extra ring (5..6) is searched only for qualifying farmland. */
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/FarmlandBlock;isNearWater(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean moreminecrafts$extendHydrationReach(LevelReader reader, BlockPos pos,
            Operation<Boolean> original, BlockState state, ServerLevel level, BlockPos tickPos, RandomSource random) {
        return original.call(reader, pos) || SoilStudyRuntime.extendedHydration(level, pos);
    }
}
