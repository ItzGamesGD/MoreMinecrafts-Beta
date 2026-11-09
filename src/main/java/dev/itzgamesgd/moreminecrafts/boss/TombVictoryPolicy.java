package dev.itzgamesgd.moreminecrafts.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.OwnableEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Valid-victory judgement for the Tomb Guardian (ST-01 P9, CR-1 + CR-2).
 *
 * <p>A guardian DEATH is not automatically a VICTORY. Only a valid victory unseals the royal chest, grants the guardian reward and
 * advances the secret entrance. Participation is tracked from Fabric damage events (same convention as {@code QuestMonumentParticipation}:
 * a participant is a player who actually damaged the boss; damage dealt through projectiles or owned entities is attributed to the owner;
 * presence alone is never evidence).
 *
 * <p>The numeric thresholds (damage share and last-hit window) are NOT approved. They are therefore never defaulted: with no
 * {@link Params} installed the judgement uses only the structural minimum below, which needs no numbers. Params can be installed only
 * explicitly (TEST-ONLY validation, or a future approved configuration).
 *
 * <p>Structural minimum (no numbers): VALID only when the killing blow is attributed to a player. Environmental damage, other mobs and
 * administrative kills are never an automatic victory.
 */
public final class TombVictoryPolicy {
    private TombVictoryPolicy() {}

    public enum DeathCause { PLAYER, MOB, ENVIRONMENT, COMMAND_KILL }

    /** Unapproved numeric parameters. Install only for TEST-ONLY validation or after the owner approves values. */
    public record Params(double minShare, long windowTicks) {}

    private static volatile Params params;

    /** TEST-ONLY / future-approved configuration entry; production leaves this unset. */
    public static void install(@Nullable Params p) { params = p; }
    @Nullable public static Params installed() { return params; }

    public record Verdict(boolean valid, DeathCause cause, String reason, Set<UUID> participants) {}

    /** Per-guardian participation record (persisted with the guardian entity so a mid-fight restart keeps it). */
    public static final class Participation {
        private final Map<UUID, Double> byPlayer = new LinkedHashMap<>();
        private double playerTotal;
        private double otherTotal;
        private long lastPlayerHitTick = Long.MIN_VALUE;

        public void onPlayerDamage(UUID player, double amount, long gameTime) {
            if (amount <= 0.0D) return;
            byPlayer.merge(player, amount, Double::sum);
            playerTotal += amount;
            lastPlayerHitTick = gameTime;
        }

        public void onOtherDamage(double amount) { if (amount > 0.0D) otherTotal += amount; }

        public Set<UUID> participants() { return new LinkedHashSet<>(byPlayer.keySet()); }
        public double playerDamage() { return playerTotal; }
        public double otherDamage() { return otherTotal; }
        public long lastPlayerHitTick() { return lastPlayerHitTick; }
        public double recordedTotal() { return playerTotal + otherTotal; }

        /** Judgement at the moment of death. The killing blow itself must already be folded in via {@link #foldKillingBlow}. */
        public Verdict judge(DeathCause cause, long deathTick, double maxHealth) {
            Set<UUID> parts = participants();
            if (cause == DeathCause.COMMAND_KILL) return new Verdict(false, cause, "administrative kill is never a victory", parts);
            Params p = params;
            if (p == null) {
                boolean ok = cause == DeathCause.PLAYER && !parts.isEmpty();
                return new Verdict(ok, cause, ok ? "structural minimum: killing blow by a participating player" : "structural minimum: killing blow not by a player (" + cause + ")", parts);
            }
            if (parts.isEmpty()) return new Verdict(false, cause, "no player damaged the boss", parts);
            boolean share = playerTotal >= p.minShare() * maxHealth;
            boolean window = cause == DeathCause.PLAYER || deathTick - lastPlayerHitTick <= p.windowTicks();
            boolean ok = share && window;
            return new Verdict(ok, cause, ok ? "valid (share and window)" : "participation too weak: share=" + share + " window=" + window, parts);
        }

        /** The lethal blow is delivered inside {@code die()}, before Fabric's AFTER_DAMAGE runs; fold its remainder in here. */
        public void foldKillingBlow(@Nullable ServerPlayer player, double maxHealth, long gameTime) {
            double remainder = Math.max(0.0D, maxHealth - recordedTotal());
            if (player != null) onPlayerDamage(player.getUUID(), Math.max(remainder, Double.MIN_VALUE), gameTime);
            else onOtherDamage(remainder);
        }

        // ---- persistence (stored in the guardian's own entity data)
        record Entry(UUID player, double damage) {}
        record State(List<Entry> entries, double other, long lastHit) {}
        private static final Codec<Entry> ENTRY = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player), Codec.DOUBLE.fieldOf("damage").forGetter(Entry::damage)).apply(i, Entry::new));
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                ENTRY.listOf().fieldOf("entries").forGetter(State::entries), Codec.DOUBLE.fieldOf("other").forGetter(State::other),
                Codec.LONG.fieldOf("last_hit").forGetter(State::lastHit)).apply(i, State::new));

        State toState() {
            List<Entry> list = new ArrayList<>();
            byPlayer.forEach((k, v) -> list.add(new Entry(k, v)));
            return new State(list, otherTotal, lastPlayerHitTick);
        }

        void fromState(State s) {
            byPlayer.clear(); playerTotal = 0.0D; otherTotal = s.other(); lastPlayerHitTick = s.lastHit();
            for (Entry e : s.entries()) { byPlayer.put(e.player(), e.damage()); playerTotal += e.damage(); }
        }
    }

    // ---- attribution

    /** The player a damage source belongs to: the attacker, the shooter of a projectile, or the owner of an owned entity. Null if none. */
    @Nullable
    public static ServerPlayer attributedPlayer(DamageSource source) {
        net.minecraft.world.entity.Entity cause = source.getEntity();
        if (cause instanceof ServerPlayer player) return player;
        if (cause instanceof OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer owner) return owner;
        return null;
    }

    public static DeathCause classify(DamageSource source) {
        if (source.is(DamageTypes.GENERIC_KILL)) return DeathCause.COMMAND_KILL;
        if (attributedPlayer(source) != null) return DeathCause.PLAYER;
        return source.getEntity() != null ? DeathCause.MOB : DeathCause.ENVIRONMENT;
    }

    // ---- Fabric damage events (CR-2)

    private static boolean registered;

    public static synchronized void initialize() {
        if (registered) return;
        registered = true;
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, base, taken, blocked) -> {
            if (blocked || taken <= 0.0F || !(victim instanceof TombGuardian guardian) || !guardian.isAlive() || guardian.isDeadOrDying()) return;
            guardian.recordDamage(source, taken);
        });
    }
}
