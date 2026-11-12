package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.QuestWitherSkullProvenance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class QuestWitherSkullSpawnMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    private void moreminecrafts$markWitherSkullDrop(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.item.ItemStack stack, net.minecraft.world.phys.Vec3 offset,
            CallbackInfoReturnable<ItemEntity> cir) {
        QuestWitherSkullProvenance.spawned((Entity) (Object) this, cir.getReturnValue());
    }
}
