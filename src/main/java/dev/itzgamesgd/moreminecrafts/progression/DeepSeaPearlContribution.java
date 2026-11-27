package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryDefinition;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.item.ItemStack;

/**
 * D13-03-I02 (Q13-Q20): personal Deep Sea Pearl draw on Elder Guardian death. Independent of the 12,000-tick
 * {@link QuestMonumentParticipation} record: this keeps its own in-memory 60-second per-elder hit log that is never
 * persisted, so a logout (or restart) discards it and a reconnect does not restore it. Player death keeps the log.
 */
public final class DeepSeaPearlContribution {
    public static final long WINDOW_TICKS = 1_200L;
    public static final double CHANCE = 0.20D;

    private DeepSeaPearlContribution() {}

    /** Pure state: elder -> (player -> last valid hit tick), plus elders already settled. */
    public static final class Tracker {
        private final Map<UUID, Map<UUID, Long>> hits = new HashMap<>();
        private final Set<UUID> settled = new HashSet<>();

        public void recordHit(UUID elder, UUID player, long tick) {
            if (settled.contains(elder)) return;
            hits.computeIfAbsent(elder, k -> new HashMap<>()).put(player, tick);
        }

        /** Logout: forget this player's hits on every elder. */
        public void forget(UUID player) {
            hits.values().forEach(m -> m.remove(player));
            hits.values().removeIf(Map::isEmpty);
        }

        /** Eligible players for this death (hit within the last 60s, inclusive). Empty if already settled. */
        public Set<UUID> settle(UUID elder, long deathTick) {
            if (!settled.add(elder)) return Set.of();
            Map<UUID, Long> log = hits.remove(elder);
            Set<UUID> eligible = new HashSet<>();
            if (log != null) log.forEach((p, t) -> { if (deathTick - t <= WINDOW_TICKS && deathTick >= t) eligible.add(p); });
            return eligible;
        }

        public void clear() { hits.clear(); settled.clear(); }
    }

    /** One independent roll per eligible player; a roll strictly below CHANCE wins. */
    public static List<UUID> winners(Set<UUID> eligible, ToDoubleFunction<UUID> roll) {
        List<UUID> won = new ArrayList<>();
        for (UUID p : eligible) if (roll.applyAsDouble(p) < CHANCE) won.add(p);
        return won;
    }

    /** Where the pearl goes: inventory first, any quantity that did not fit is dropped as a normal world item. */
    public interface Sink {
        /** @return how many of {@code count} were inserted into the inventory */
        int insert(int count);
        void drop(int count);
    }

    public static void deliver(int count, Sink sink) {
        int remaining = count - sink.insert(count);
        if (remaining > 0) sink.drop(remaining);
    }

    // ---- wiring ----

    private static final Tracker TRACKER = new Tracker();

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, base, taken, blocked) -> {
            if (!blocked && taken > 0.0F) hit(victim, source);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ElderGuardian elder && elder.level() instanceof ServerLevel level) {
                hit(elder, source); // AFTER_DEATH runs before AFTER_DAMAGE, so count the killing blow here
                settle(level, elder);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TRACKER.forget(handler.player.getUUID()));
    }

    private static void hit(LivingEntity victim, DamageSource source) {
        if (victim instanceof ElderGuardian elder && elder.level() instanceof ServerLevel level
                && source.getEntity() instanceof ServerPlayer attacker)
            TRACKER.recordHit(elder.getUUID(), attacker.getUUID(), level.getGameTime());
    }

    private static void settle(ServerLevel level, ElderGuardian elder) {
        Set<UUID> eligible = TRACKER.settle(elder.getUUID(), level.getGameTime());
        // Q13: Ocean Monument only (Overworld monument containing the elder).
        if (eligible.isEmpty() || !QuestMonumentParticipation.monumentAt(level, elder.blockPosition()).isValid()) return;
        for (UUID id : winners(eligible, p -> level.getRandom().nextDouble())) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) give(player);
        }
    }

    private static void give(ServerPlayer player) {
        ItemStack stack = new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.DEEP_SEA_PEARL));
        deliver(1, new Sink() {
            @Override public int insert(int count) { player.getInventory().add(stack); return count - stack.getCount(); }
            @Override public void drop(int count) { player.spawnAtLocation(player.level(), stack); }
        });
    }
}
