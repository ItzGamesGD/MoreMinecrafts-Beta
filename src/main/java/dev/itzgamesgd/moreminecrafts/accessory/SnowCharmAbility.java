package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Snow Charm's approved powder-snow sinking reduction. The snow/ice slip component is not applied
 * because its baseline formula is still undecided.
 */
public final class SnowCharmAbility {
    private static final int EXPLORATION_SLOT = AccessoryCategory.EXPLORATION_MOBILITY.ordinal();
    private static final String SINKING_REDUCTION = "powder_snow_sinking_speed_reduction_percent";

    private SnowCharmAbility() {}

    /** Factor applied to downward powder-snow movement; 1.0 when no valid Snow Charm is equipped. */
    public static double sinkingMultiplier(Player player) {
        if (!player.isAlive()) return 1.0D;
        ItemStack stack = player.getAttachedOrElse(EquipmentAttachments.ACCESSORIES, AccessoryLoadout.empty())
                .get(EXPLORATION_SLOT);
        if (!AccessorySlotRules.accepts(EXPLORATION_SLOT, stack)
                || stack.getItem() != ModItems.ACCESSORIES.get(AccessoryDefinition.SNOW_CHARM)) return 1.0D;
        var reduction = AccessoryEnhancementCatalog.valueAt(AccessoryDefinition.SNOW_CHARM, SINKING_REDUCTION,
                stack.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL, 0));
        return reduction.isPresent() ? 1.0D - reduction.getAsDouble() / 100.0D : 1.0D;
    }

    /**
     * Adjusts the movement vanilla has just scaled by a stuck-in-block multiplier. Only downward
     * movement caused by powder snow changes; rising, horizontal and other stuck blocks are untouched.
     */
    public static Vec3 adjustStuckMovement(Entity entity, BlockState stuckState, Vec3 movement) {
        if (!(entity instanceof Player player) || movement.y >= 0.0D
                || stuckState == null || !stuckState.is(Blocks.POWDER_SNOW)) return movement;
        double multiplier = sinkingMultiplier(player);
        return multiplier == 1.0D ? movement : new Vec3(movement.x, movement.y * multiplier, movement.z);
    }
}
