package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Creeper.class)
public abstract class CaveCreeperExplosionMixin {
    @ModifyArg(method = "explodeCreeper", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"),
            index = 4)
    private float moreminecrafts$reduceCaveCreeperBlast(float radius) {
        return ((Creeper) (Object) this).getType() == CustomMobCatalog.CAVE_CREEPER ? radius * 0.60F : radius;
    }
}
