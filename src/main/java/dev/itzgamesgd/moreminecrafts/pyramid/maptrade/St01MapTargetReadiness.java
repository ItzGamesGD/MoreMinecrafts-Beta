package dev.itzgamesgd.moreminecrafts.pyramid.maptrade;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01SearchPreparation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * ST-01 P8-D8: whether vanilla's explorer-map search for a villager can run on the server thread without a long stall.
 *
 * <p>READY requires, all without blocking: (1) the C6 preparation for the villager's region and the map's radius is complete
 * and its StructureCheck warm-up is idle, so vanilla's locator finds every candidate already decided; (2) every eligible
 * start that vanilla will inspect (ring by ring, up to the first ring holding a start that can still be referenced, which is
 * what {@code skip_existing_chunks} requires) is loaded at {@code STRUCTURE_STARTS}, so the locator generates no chunk.
 * Missing chunks are preloaded with a short-lived, non-persistent ticket refreshed while waiting; it expires by itself
 * ({@value #TICKET_TIMEOUT} ticks) once the trade is resolved. If every prepared start is already claimed, the preparation
 * is extended to more starts. A failed or cancelled preparation, a rejected request or a preload that does not finish
 * counts as a failure and is retried after an exponential back-off.
 */
public final class St01MapTargetReadiness {
    private St01MapTargetReadiness() {}

    public enum State { NOT_READY, READY, NOT_FOUND }

    static final long TICKET_TIMEOUT = 200L;
    static final long TICKET_REFRESH = 40L;
    static final long PRELOAD_LIMIT = 1200L;
    public static final AtomicLong PRELOADS = new AtomicLong();
    public static final AtomicLong FAILURES = new AtomicLong();

    static TicketType ticket;

    static void registerTicket(TicketType type) {
        ticket = type;
    }

    /** Per-villager progress (in memory; a restart starts over from the persisted placeholder). */
    public static final class Pending {
        int minEligible = 1;
        boolean requested;
        int failures;
        long nextAttempt;
        final Map<ChunkPos, Long> ticketTimes = new HashMap<>();
        final Map<ChunkPos, Long> preloadStart = new HashMap<>();

        public int failures() { return failures; }

        /** Records a failed attempt without a back-off delay (tests). */
        public void countFailure() { failures++; }

        public long nextAttempt() { return nextAttempt; }
    }

    /** Back-off after the n-th failure: 80, 160, 320, 640, 1280, then 1600 ticks. */
    public static long backoffTicks(int failures) {
        return Math.min(1600L, 40L << Math.min(Math.max(failures, 1), 6));
    }

    static void failure(Pending p, ServerLevel level) {
        p.failures++;
        p.nextAttempt = level.getGameTime() + backoffTicks(p.failures);
        FAILURES.incrementAndGet();
    }

    public static State evaluate(Villager v, ServerLevel level, int radius, Pending p, boolean allowPreload) {
        var target = St01SearchPreparation.targetFor(level);
        if (target.isEmpty()) return State.NOT_FOUND; // no ST-01 placement in this dimension: vanilla finds nothing without searching
        var service = St01SearchPreparation.service();
        if (service == null) return State.NOT_READY;
        BlockPos pos = v.blockPosition();
        if (service.status(target.get(), pos, radius, p.minEligible) != St01SearchPreparation.Status.READY) {
            St01SearchPreparation.Status s = service.request(target.get(), pos, radius, p.minEligible);
            if (s == St01SearchPreparation.Status.REJECTED || (s == St01SearchPreparation.Status.QUEUED && p.requested)) failure(p, level);
            p.requested = true;
            return State.NOT_READY;
        }
        if (!service.warmIdle()) return State.NOT_READY;
        Optional<St01SearchPreparation.Result> result = service.result(target.get(), pos, radius, p.minEligible);
        if (result.isEmpty()) return State.NOT_READY;
        List<St01SearchPreparation.Eligible> eligible = result.get().eligible();
        if (eligible.isEmpty()) return State.NOT_FOUND;
        int i = 0;
        while (i < eligible.size()) {
            int ring = eligible.get(i).ring();
            List<ChunkPos> ringChunks = new ArrayList<>();
            for (; i < eligible.size() && eligible.get(i).ring() == ring; i++) ringChunks.add(eligible.get(i).chunk());
            boolean loaded = true, referenceable = false;
            for (ChunkPos c : ringChunks) {
                ChunkAccess chunk = structureStartsChunk(level, c);
                if (chunk == null) {
                    loaded = false;
                    if (allowPreload) preload(level, c, p);
                    continue;
                }
                StructureStart start = chunk.getStartForStructure(target.get().structure().value());
                if (start != null && start.isValid() && start.canBeReferenced()) referenceable = true;
            }
            if (!loaded) return State.NOT_READY;
            if (referenceable) return State.READY;
        }
        // every prepared start is already claimed: prepare further, unless the radius is exhausted
        if (eligible.size() < p.minEligible) return State.NOT_FOUND;
        p.minEligible = eligible.size() + 1;
        p.requested = false;
        return State.NOT_READY;
    }

    /** The chunk if it is loaded at STRUCTURE_STARTS or beyond; never blocks and never loads. */
    static ChunkAccess structureStartsChunk(ServerLevel level, ChunkPos c) {
        CompletableFuture<ChunkResult<ChunkAccess>> f = level.getChunkSource().getChunkFuture(c.x(), c.z(), ChunkStatus.STRUCTURE_STARTS, false);
        if (!f.isDone()) return null;
        ChunkResult<ChunkAccess> r = f.getNow(null);
        return r != null && r.isSuccess() ? r.orElse(null) : null;
    }

    static void preload(ServerLevel level, ChunkPos c, Pending p) {
        long now = level.getGameTime();
        Long last = p.ticketTimes.get(c);
        if (last == null || now - last >= TICKET_REFRESH) {
            level.getChunkSource().addTicket(new Ticket(ticket, ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS)), c);
            p.ticketTimes.put(c, now);
            if (last == null) {
                PRELOADS.incrementAndGet();
                p.preloadStart.put(c, now);
            }
        }
        if (now - p.preloadStart.getOrDefault(c, now) > PRELOAD_LIMIT) {
            p.preloadStart.put(c, now);
            failure(p, level);
        }
    }
}
