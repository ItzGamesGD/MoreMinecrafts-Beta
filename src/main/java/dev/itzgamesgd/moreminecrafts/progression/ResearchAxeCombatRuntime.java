package dev.itzgamesgd.moreminecrafts.progression;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/** Tracks server attack attempts, including blocked attempts, without saved or shared player state. */
public final class ResearchAxeCombatRuntime {
    private record AttackContext(UUID playerId, boolean firstStrike) {}
    private static final Map<UUID, Long> LAST_ATTACK = new HashMap<>();
    private static final ThreadLocal<AttackContext> CURRENT = new ThreadLocal<>();

    private ResearchAxeCombatRuntime() {}

    public static void initialize() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_ATTACK.remove(handler.player.getUUID()));
    }

    public static Scope beginAttack(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long previous = LAST_ATTACK.put(player.getUUID(), now);
        AttackContext old = CURRENT.get();
        CURRENT.set(new AttackContext(player.getUUID(), ResearchAxeCombatRules.isFirstStrike(
                now, previous == null ? Long.MIN_VALUE : previous)));
        return new Scope(old);
    }

    public static double damageBonus(ServerPlayer player) {
        AttackContext context = CURRENT.get();
        if (context == null || !context.playerId().equals(player.getUUID())) return 0.0D;
        int level = ResearchRewards.level(player, "v3.research.28", "axe_combat_first_strike");
        return ResearchAxeCombatRules.bonus(level, context.firstStrike());
    }

    public static final class Scope implements AutoCloseable {
        private final AttackContext previous;
        private boolean closed;
        private Scope(AttackContext previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
