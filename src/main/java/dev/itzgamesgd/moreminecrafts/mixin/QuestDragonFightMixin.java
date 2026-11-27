package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.QuestDragonRematch;
import java.util.UUID;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the fight's previous-kill state before {@code setDragonKilled} changes it. */
@Mixin(EnderDragonFight.class)
abstract class QuestDragonFightMixin {
    @Shadow private boolean dragonKilled;
    @Shadow private UUID dragonUUID;
    @Shadow public abstract boolean hasPreviouslyKilledDragon();

    @Inject(method = "setDragonKilled", at = @At("HEAD"))
    private void moreminecrafts$captureBeforeMutation(EnderDragon dragon, CallbackInfo callback) {
        QuestDragonRematch.fightCompleting(dragon,
                dragon.getUUID().equals(this.dragonUUID) && !this.dragonKilled, this.hasPreviouslyKilledDragon());
    }
}
