package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.server.level.ServerPlayer;

/** Transient caller attribution for synchronous server block-break hooks. */
public final class ResearchMiningContext {
    private static final ThreadLocal<Deque<ServerPlayer>> PLAYERS = ThreadLocal.withInitial(ArrayDeque::new);
    private ResearchMiningContext() {}

    public static Scope enter(ServerPlayer player) {
        Deque<ServerPlayer> stack = PLAYERS.get();
        stack.push(player);
        return new Scope(stack);
    }

    public static ServerPlayer currentPlayer() {
        Deque<ServerPlayer> stack = PLAYERS.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static final class Scope implements AutoCloseable {
        private final Deque<ServerPlayer> stack;
        private boolean closed;
        private Scope(Deque<ServerPlayer> stack) { this.stack = stack; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            stack.pop();
            if (stack.isEmpty()) PLAYERS.remove();
        }
    }
}
