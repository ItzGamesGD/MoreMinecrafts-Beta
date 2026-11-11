package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.itzgamesgd.moreminecrafts.progression.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
abstract class ResearchCreeperMixin {
    @Unique private static final EntityDataAccessor<Integer> MOREMINECRAFTS_FUSE_DELAY =
            SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void moreminecrafts$defineFuseDelay(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(MOREMINECRAFTS_FUSE_DELAY, 0);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void moreminecrafts$resolveTargetFuse(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        if (!creeper.level().isClientSide()) creeper.getEntityData().set(MOREMINECRAFTS_FUSE_DELAY,
                ResearchBatch2Rules.fuseDelay(ResearchBatch2Runtime.level(creeper.getTarget(), "v3.research.38")));
    }
    @ModifyExpressionValue(method = {"tick", "getSwelling"}, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/monster/Creeper;maxSwell:I"))
    private int moreminecrafts$effectiveFuse(int baseline) {
        return (int) Math.min(Integer.MAX_VALUE, (long) baseline
                + ((Creeper) (Object) this).getEntityData().get(MOREMINECRAFTS_FUSE_DELAY));
    }
}
