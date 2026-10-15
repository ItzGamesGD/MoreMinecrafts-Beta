package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchXpAcquisition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies paid XP Acquisition at pickup, using the collecting player's own progress. */
@Mixin(ExperienceOrb.class)
abstract class ResearchXpAcquisitionMixin {
    @Redirect(method = "playerTouch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private void moreminecrafts$applyRecipientResearchXp(Player recipient, int vanillaXp) {
        int adjusted = recipient instanceof ServerPlayer player
                ? ResearchXpAcquisition.adjustedAmount(player, vanillaXp) : vanillaXp;
        recipient.giveExperiencePoints(adjusted);
    }
}
