package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchXpOrbContext;
import dev.itzgamesgd.moreminecrafts.progression.Research23AEffectRules;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ExperienceOrb;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scales only experience orbs created inside a classified, purchase-gated source scope. */
@Mixin(net.minecraft.world.entity.ExperienceOrb.class)
abstract class ResearchExperienceOrbMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void moreminecrafts$instantlyCollectNearbyXpOrbs(CallbackInfo callbackInfo) {
        ExperienceOrb orb = (ExperienceOrb) (Object) this;
        if (!(orb.level() instanceof ServerLevel level) || orb.isRemoved()) return;
        AABB search = orb.getBoundingBox().inflate(6.0);
        var candidates = level.getEntitiesOfClass(ServerPlayer.class, search, player -> {
            int paidLevel = ResearchRewards.level(player, "v3.research.67", "xp_orb_collection");
            if (paidLevel == 0) return false;
            double dx = player.getX() - orb.getX();
            double dy = player.getY() - orb.getY();
            double dz = player.getZ() - orb.getZ();
            return Research23AEffectRules.withinXpCollectionRadius(dx, dy, dz, paidLevel);
        });
        candidates.sort(Comparator.comparingDouble(orb::distanceToSqr));
        for (ServerPlayer player : candidates) {
            orb.playerTouch(player);
            if (orb.isRemoved()) return;
        }
    }

    @ModifyVariable(method = "award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int moreminecrafts$applySourceResearchBonus(int value) {
        return ResearchXpOrbContext.adjustOrbValue(value);
    }
}
