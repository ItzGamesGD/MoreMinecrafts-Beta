package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Runtime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRemainingRules;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Extend the splash query, cutoff and distance falloff together for the throwing researcher. */
@Mixin(ThrownSplashPotion.class)
abstract class ResearchSplashPotionMixin {
    @Unique private double moreminecrafts$splashRadius() {
        var owner = ((ThrownSplashPotion) (Object) this).getOwner();
        return ResearchRemainingRules.splashRadius(
                owner instanceof net.minecraft.server.level.ServerPlayer player
                        ? ResearchBatch2Runtime.level(player, "v3.research.61") : 0);
    }

    @ModifyConstant(method = "onHitAsPotion", constant = @Constant(doubleValue = 4.0D, ordinal = 0))
    private double moreminecrafts$queryX(double vanilla) { return moreminecrafts$splashRadius(); }

    @ModifyConstant(method = "onHitAsPotion", constant = @Constant(doubleValue = 4.0D, ordinal = 1))
    private double moreminecrafts$queryZ(double vanilla) { return moreminecrafts$splashRadius(); }

    @ModifyConstant(method = "onHitAsPotion", constant = @Constant(doubleValue = 16.0D))
    private double moreminecrafts$distanceLimit(double vanilla) {
        double radius = moreminecrafts$splashRadius();
        return radius * radius;
    }

    @ModifyConstant(method = "onHitAsPotion", constant = @Constant(doubleValue = 4.0D, ordinal = 2))
    private double moreminecrafts$falloff(double vanilla) { return moreminecrafts$splashRadius(); }
}
