package dev.itzgamesgd.moreminecrafts.progression;

import net.minecraft.server.level.ServerPlayer;

/** Attributes an XP orb at its creation site, before it can be collected by another player. */
public final class ResearchXpOrbContext {
    public record Context(ServerPlayer player, String researchId, String effectId, String fractionKey) {}
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private ResearchXpOrbContext() {}

    public static Scope enter(ServerPlayer player, String researchId, String effectId, String fractionKey) {
        Context previous = CURRENT.get();
        Context next = player != null && ResearchRewards.level(player, researchId, effectId) > 0
                ? new Context(player, researchId, effectId, fractionKey) : null;
        CURRENT.set(next);
        return new Scope(previous);
    }

    public static int adjustOrbValue(int baseValue) {
        Context context = CURRENT.get();
        if (context == null || baseValue <= 0) return baseValue;
        ServerPlayer player = context.player();
        ResearchProgressState state = KnowledgeRuntime.state(player);
        String key = "research.xp_fraction." + context.fractionKey();
        ResearchPassiveMath.Fraction result = ResearchPassiveMath.accumulatePercent(
                baseValue, state.get(key), 1);
        if (result.remainder() != state.get(key))
            KnowledgeRuntime.updateState(player, state.with(key, result.remainder()));
        return baseValue + result.wholeBonus();
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
