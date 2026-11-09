package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Intercept before the concrete projectile's whole tick: cancelling a superclass tick alone would still move arrows/wind charges. */
@Mixin(ServerLevel.class)
abstract class ColosseumProjectilePauseMixin {
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void moreminecrafts$pauseOwnedProjectile(Entity entity,CallbackInfo ci) {
        if(entity instanceof Projectile && ColosseumRuntime.combatFrozen(entity))ci.cancel();
    }
}
