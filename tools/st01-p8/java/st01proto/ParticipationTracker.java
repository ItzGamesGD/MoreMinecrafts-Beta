package st01proto;

import java.util.*;

/**
 * Valid-victory judgement (PROPOSED; every numeric parameter here is TEST-ONLY and not approved).
 * Convention copied from the existing QuestMonumentParticipation: a participant is a player who DAMAGED the boss (the damage source resolves
 * projectiles/owned entities to the player); presence alone is never evidence.
 */
public final class ParticipationTracker {
    public enum DeathCause { PLAYER, MOB, ENVIRONMENT, VOID, COMMAND_KILL }
    public enum Policy { ANY_DAMAGE, LAST_HIT_WINDOW, DAMAGE_SHARE, COMBINED }
    public record Verdict(boolean valid, String reason, Set<UUID> participants) {}

    /** TEST-ONLY parameters. */
    public record Params(double minShare, long windowTicks) {
        public static Params testOnly() { return new Params(0.5, 100); }
    }

    private final double maxHealth; private final Params params; private final Policy policy;
    private final Map<UUID, Double> byPlayer = new LinkedHashMap<>();
    private double playerTotal = 0, otherTotal = 0; private long lastPlayerHit = Long.MIN_VALUE;

    public ParticipationTracker(double maxHealth, Policy policy, Params params) { this.maxHealth = maxHealth; this.policy = policy; this.params = params; }

    public void onPlayerDamage(UUID player, double amount, long tick) {
        if (amount <= 0) return;
        byPlayer.merge(player, amount, Double::sum); playerTotal += amount; lastPlayerHit = tick;
    }
    public void onOtherDamage(double amount, long tick) { if (amount > 0) otherTotal += amount; }

    public Verdict judge(DeathCause cause, long deathTick) {
        Set<UUID> parts = new LinkedHashSet<>(byPlayer.keySet());
        if (cause == DeathCause.COMMAND_KILL) return new Verdict(false, "administrative kill is never a victory", parts);
        if (parts.isEmpty()) return new Verdict(false, "no player damaged the boss", parts);
        boolean share = playerTotal >= params.minShare() * maxHealth;
        boolean window = cause == DeathCause.PLAYER || deathTick - lastPlayerHit <= params.windowTicks();
        boolean ok = switch (policy) {
            case ANY_DAMAGE -> true;
            case LAST_HIT_WINDOW -> window;
            case DAMAGE_SHARE -> share;
            case COMBINED -> share && window;
        };
        return new Verdict(ok, ok ? "valid (" + policy + ")" : "participation too weak (" + policy + "): share=" + share + " window=" + window, parts);
    }
}
