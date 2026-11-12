package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.progression.QuestWitherSkullProvenance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
abstract class QuestWitherSkullDeathLootMixin {
    /** Only the death-loot table path; equipment drops and commands never run inside this bracket. */
    @WrapMethod(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V")
    private void moreminecrafts$bracketDeathLoot(ServerLevel level, DamageSource source, boolean playerKilled,
            Operation<Void> original) {
        Entity previous = QuestWitherSkullProvenance.enterDeathLoot((Entity) (Object) this);
        try {
            original.call(level, source, playerKilled);
        } finally {
            QuestWitherSkullProvenance.exitDeathLoot(previous);
        }
    }
}
