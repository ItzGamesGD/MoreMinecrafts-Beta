package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Scopes durability conservation to the actual tool used for the current server block break. */
public final class ResearchMiningContext {
    private record Context(ServerPlayer player, ItemStack tool, boolean effectiveTool) {}
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private ResearchMiningContext() {}

    public static Scope enter(ServerPlayer player, BlockPos pos, BlockState state) {
        Context previous = CURRENT.get();
        ItemStack tool = player.getMainHandItem();
        CURRENT.set(new Context(player, tool, !tool.isEmpty() && tool.getDestroySpeed(state) > 1.0F));
        return new Scope(previous);
    }

    public static boolean applies(ServerPlayer player, ItemStack stack) {
        Context context = CURRENT.get();
        return context != null && context.player() == player && context.tool() == stack
                && context.effectiveTool() && stack.isDamageableItem();
    }

    public static final class Scope implements AutoCloseable {
        private final Context previous;
        private boolean closed;
        private Scope(Context previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
