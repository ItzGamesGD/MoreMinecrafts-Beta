package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchXpAcquisition;
import dev.itzgamesgd.moreminecrafts.progression.ResearchParallelBranchRules;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies paid XP Acquisition at pickup, using the collecting player's own progress. */
@Mixin(ExperienceOrb.class)
abstract class ResearchXpAcquisitionMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void moreminecrafts$collectWithinPaidXpControlRadius(CallbackInfo callbackInfo) {
        ExperienceOrb orb = (ExperienceOrb) (Object) this;
        if (!(orb.level() instanceof ServerLevel level) || orb.isRemoved()) return;
        var candidates = level.getEntitiesOfClass(ServerPlayer.class, orb.getBoundingBox().inflate(6.0), player -> {
            int paidLevel = ResearchRewards.level(player, "v3.research.67", "xp_orb_collection");
            if (paidLevel == 0) return false;
            double dx = player.getX() - orb.getX();
            double dy = player.getY() - orb.getY();
            double dz = player.getZ() - orb.getZ();
            return ResearchParallelBranchRules.withinXpCollectionRadius(dx, dy, dz, paidLevel);
        });
        candidates.sort(Comparator.comparingDouble(orb::distanceToSqr));
        for (ServerPlayer player : candidates) {
            // Deliberately enter vanilla pickup. The .64 redirect below applies its recipient
            // bonus once, and vanilla removes the orb only when a pickup actually succeeds.
            orb.playerTouch(player);
            if (orb.isRemoved()) return;
        }
    }

    @Redirect(method = "playerTouch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private void moreminecrafts$applyRecipientResearchXp(Player recipient, int vanillaXp) {
        int adjusted = recipient instanceof ServerPlayer player
                ? ResearchXpAcquisition.adjustedAmount(player, vanillaXp) : vanillaXp;
        recipient.giveExperiencePoints(adjusted);
    }
}
