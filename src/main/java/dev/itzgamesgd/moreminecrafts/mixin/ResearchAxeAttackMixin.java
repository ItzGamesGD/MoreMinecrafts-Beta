package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.ResearchAxeCombatRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Captures one server attack attempt so the damage hook can apply its first-strike bonus once. */
@Mixin(Player.class)
abstract class ResearchAxeAttackMixin {
    @WrapMethod(method = "attack")
    private void moreminecrafts$trackAttack(Entity target, Operation<Void> original) {
        if (!((Object) this instanceof ServerPlayer player) || player.level().isClientSide()) {
            original.call(target);
            return;
        }
        try (ResearchAxeCombatRuntime.Scope ignored = ResearchAxeCombatRuntime.beginAttack(player)) {
            original.call(target);
        }
    }
}
