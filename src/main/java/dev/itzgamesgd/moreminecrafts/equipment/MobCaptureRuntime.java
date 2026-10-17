package dev.itzgamesgd.moreminecrafts.equipment;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueOutput;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;

/**
 * Server-only mob capture. This is a draft C-path implementation, not yet
 * released: villager/custom-mob NBT roundtrip and loaded/unloaded UUID
 * uniqueness must pass GameTests before merge.
 */
public final class MobCaptureRuntime {
    private MobCaptureRuntime() {}

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, world, hand, target, hit) -> {
            ItemStack tool = player.getItemInHand(hand);
            if (!tool.is(ModItems.MOB_CAPTURE_TOOL)) return InteractionResult.PASS;
            if (!(target instanceof Mob mob) || forbiddenBoss(mob)) return InteractionResult.PASS;
            if (world.isClientSide()) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer server) || !(world instanceof ServerLevel level)) return InteractionResult.FAIL;
            if (!KnowledgeRuntime.researched(server, "mob_capture")) return InteractionResult.FAIL;
            return capture(server, level, tool, mob) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        });
    }

    private static boolean forbiddenBoss(Mob mob) {
        // Wither and dragon are intentionally excluded. The Warden is treated
        // as boss-class for this tool. Other modded bosses need a data tag.
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        return id.equals("minecraft:wither") || id.equals("minecraft:warden")
                || id.equals("minecraft:ender_dragon");
    }

    private static boolean capture(ServerPlayer player, ServerLevel level, ItemStack tool, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved() || mob.isPassenger() || !mob.getPassengers().isEmpty()) return false;
        // A complete instance snapshot, not merely a species-level spawn egg.
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        if (!mob.save(output)) return false;
        CompoundTag saved = output.buildResult();
        if (saved.isEmpty()) return false;

        ItemStack egg = new ItemStack(ModItems.CAPTURED_MOB_EGG);
        egg.set(DataComponents.CUSTOM_DATA, CustomData.of(saved));
        // Commit inventory storage before entity removal. No floor drop is
        // allowed: a full inventory simply cancels the capture transaction.
        if (!player.getInventory().add(egg)) return false;
        mob.remove(Entity.RemovalReason.DISCARDED);
        if (!player.hasInfiniteMaterials()) tool.hurtAndBreak(1, level, player, broken -> {});
        return true;
    }
}
