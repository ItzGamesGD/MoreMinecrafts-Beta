package dev.itzgamesgd.moreminecrafts.progression;

import java.util.function.DoubleSupplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla Anvil use wear/sound semantics for custom successful Anvil operations. */
public final class T1AnvilUse {
    private static final double DAMAGE_CHANCE = .12;

    private T1AnvilUse() {}

    public static boolean shouldDamage(boolean creative, boolean anvil, double roll) {
        return !creative && anvil && roll >= 0 && roll < DAMAGE_CHANCE;
    }

    public static void complete(ContainerLevelAccess access, Player player, DoubleSupplier random) {
        if (player.level().isClientSide()) return;
        access.execute((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (shouldDamage(player.hasInfiniteMaterials(), state.is(BlockTags.ANVIL), random.getAsDouble())) {
                BlockState damaged = AnvilBlock.damage(state);
                if (damaged == null) {
                    level.removeBlock(pos, false);
                    level.levelEvent(1029, pos, 0);
                } else {
                    level.setBlock(pos, damaged, 2);
                    level.levelEvent(1030, pos, 0);
                }
            } else {
                level.levelEvent(1030, pos, 0);
            }
        });
    }
}
