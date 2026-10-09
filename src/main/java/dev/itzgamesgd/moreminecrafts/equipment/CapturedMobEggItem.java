package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueInput;

/** Restores the exact per-individual saved NBT rather than summoning a species default. */
public final class CapturedMobEggItem extends Item {
    public CapturedMobEggItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        var stack = context.getItemInHand();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return InteractionResult.FAIL;

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!serverLevel.isLoaded(pos)) return InteractionResult.FAIL;
        var input = TagValueInput.create(ProblemReporter.DISCARDING, serverLevel.registryAccess(), data.copyTag());
        Entity entity = EntityType.loadEntityRecursive(input, serverLevel, EntitySpawnReason.SPAWN_ITEM_USE, e -> e);
        if (!(entity instanceof Mob)) return InteractionResult.FAIL;

        // Preserve saved UUID and all per-villager/other persistent state.
        // Refuse an already-loaded UUID; never consume the egg on failure.
        if (serverLevel.getEntity(entity.getUUID()) != null) return InteractionResult.FAIL;
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                entity.getYRot(), entity.getXRot());
        if (!serverLevel.noCollision(entity)) return InteractionResult.FAIL;
        if (!serverLevel.addFreshEntity(entity)) return InteractionResult.FAIL;
        stack.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
