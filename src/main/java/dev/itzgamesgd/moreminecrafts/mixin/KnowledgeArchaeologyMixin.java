package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Counts result items only after vanilla archaeology successfully spawns the loot entity. */
@Mixin(BrushableBlockEntity.class)
abstract class KnowledgeArchaeologyMixin {
    @WrapOperation(method = "dropContent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean moreminecrafts$recordProducedLoot(ServerLevel level, Entity entity, Operation<Boolean> original,
            @Local(argsOnly = true) LivingEntity brusher) {
        boolean spawned = original.call(level, entity);
        if (spawned && entity instanceof ItemEntity item && brusher instanceof ServerPlayer player)
            KnowledgeEventCollectors.archaeologyItemProduced(player, item.getItem());
        return spawned;
    }
}
