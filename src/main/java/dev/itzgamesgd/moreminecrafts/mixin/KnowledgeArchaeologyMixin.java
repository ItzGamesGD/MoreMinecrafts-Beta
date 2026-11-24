package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeEventCollectors;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import dev.itzgamesgd.moreminecrafts.progression.ResearchWorldLootRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Counts result items and applies `.52` brush-time reduction on vanilla archaeology. */
@Mixin(BrushableBlockEntity.class)
abstract class KnowledgeArchaeologyMixin {
    @ModifyConstant(method = "brush", constant = @Constant(intValue = 10))
    private int moreminecrafts$reduceBrushTime(int vanillaCount, long gameTime, ServerLevel level,
                                               LivingEntity brusher) {
        if (brusher instanceof ServerPlayer player) {
            int level52 = ResearchRewards.level(player, "v3.research.52", "archaeology_study");
            if (level52 > 0) return ResearchWorldLootRules.archaeologyRequiredBrushes(level52);
        }
        return vanillaCount;
    }

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
