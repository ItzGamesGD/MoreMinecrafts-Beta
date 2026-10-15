package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.ResearchXpOrbContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;

/** Attributes hostile-mob XP orbs to the eligible player who caused the mob's XP drop. */
@Mixin(Mob.class)
abstract class ResearchMobXpMixin {
    @WrapMethod(method = "dropExperience")
    private void moreminecrafts$scopeHostileMobXp(ServerLevel level, Entity source, Operation<Void> original) {
        if ((Object) this instanceof Monster && source instanceof ServerPlayer player) {
            try (var ignored = ResearchXpOrbContext.enter(player,
                    "ecology_root", "hostile_xp_orbs", "ecology_hostile")) {
                original.call(level, source);
            }
            return;
        }
        original.call(level, source);
    }
}
