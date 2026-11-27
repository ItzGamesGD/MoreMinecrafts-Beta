package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumRuntime;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Arena-owned mobs retain exact combat/HP/timer state during approved pause and recovery verification. */
@Mixin(Mob.class)
abstract class ColosseumMobPauseMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$pauseArenaCombat(CallbackInfo ci) {
        Mob self=(Mob)(Object)this;
        if(ColosseumRuntime.combatFrozen(self)) {self.setDeltaMovement(0,0,0);ci.cancel();}
    }
}
