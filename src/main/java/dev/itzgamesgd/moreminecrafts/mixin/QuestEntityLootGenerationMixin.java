package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.QuestStructureLoot;
import net.minecraft.advancements.triggers.LootTableTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ContainerEntity.class)
interface QuestEntityLootGenerationMixin {
    @WrapOperation(method = "unpackChestVehicleLootTable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/advancements/triggers/LootTableTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceKey;)V"))
    private void moreminecrafts$recordGeneratedLoot(LootTableTrigger trigger, ServerPlayer player,
            ResourceKey<LootTable> table, Operation<Void> original) {
        original.call(trigger, player, table);
        ContainerEntity container = (ContainerEntity) this;
        if (container instanceof Entity entity)
            QuestStructureLoot.entityGenerated(player, table, container.level(), entity.getUUID());
    }
}
