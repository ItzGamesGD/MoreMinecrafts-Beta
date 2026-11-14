package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyVariantMobs;
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
        var type = ((Creeper) (Object) this).getType();
        if (type == CustomMobCatalog.CAVE_CREEPER) return radius * 0.60F;
        if (type == SkyVariantMobs.ROCK_CREEPER) return radius * SkyVariantMobs.ROCK_CREEPER_EXPLOSION_MULTIPLIER;
        return radius;
    }
}
