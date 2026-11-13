package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Runs one {@link EntranceCore} per registered ST-01 instance (P7 state machine, P11 owner-approved production settings:
 * O-1 600-tick fallback, O-3 timer starts at the defeat, O-4 1.5-block arrival at the stop point, O-6 scale 0.5 / speed 0.25
 * / {@code BOSS_S} spawn).
 *
 * <p>Opening rule (P7 approval record): after the Tomb Guardian is defeated a gold guide golem appears and walks to the
 * stop point; its arrival opens the cover, and if it never arrives the cover opens anyway once the fallback time has run
 * out. The opening is subordinate to the defeat (it never happens without the persisted Tomb Guardian completion), is
 * permanent (no transition back to CLOSED is approved) and never touches the guardian, its reward or the royal chest.
 *
 * <p>Instances are registered from the same placement metadata the single-PRIMARY royal-chest handoff uses (ground +
 * orientation), so the entrance key is the royal tomb anchor: natural worldgen registers them in
 * {@code St01PyramidCompletion}, controlled placements (tests, tools) call {@link #register} directly.
 */
public final class SecretEntranceRuntime {
    private SecretEntranceRuntime() {}

    /** Players within this horizontal-and-vertical distance of the tomb anchor are the golem's candidate leaders. */
    static final double LEADER_SEARCH_RADIUS = 64.0D;

    /** TEST-ONLY seams for game tests. Never set in production code. */
    public static final class TestHooks {
        private TestHooks() {}
        /** Tomb keys whose guide golem creation is forced to fail. Per instance so concurrently running tests do not interfere. */
        public static final java.util.Set<Long> failGolemSpawnFor = java.util.concurrent.ConcurrentHashMap.newKeySet();
    }

    private static final class Live {
        final EntranceCore core;
        final GuideTargetSelector selector = new GuideTargetSelector(3.0D);       // margin: P7/P8 candidate, unapproved
        Live(EntranceCore core) { this.core = core; }
    }

    private static final Map<String, Live> LIVE = new HashMap<>();
    private static boolean initialized;
    private static boolean warnedDisabled;

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(SecretEntranceRuntime::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LIVE.clear());
    }

    /** Test/diagnostic support: drops the in-memory cores (as a server restart would); persisted records are untouched. */
    public static void forgetLiveState() { LIVE.clear(); }

    public static SecretEntranceData data(ServerLevel level) { return level.getDataStorage().computeIfAbsent(SecretEntranceData.TYPE); }

    /** Registers one placed pyramid. Idempotent: an existing record (possibly OPEN) is never overwritten. */
    public static boolean register(ServerLevel level, BlockPos ground, St01PyramidAssembly.Orientation orientation) {
        return data(level).register(ground, orientation);
    }

    public static boolean register(ServerLevel level, St01PyramidAssembly.Placement placement) {
        return register(level, placement.ground(), placement.orientation());
    }

    /** Administrative / test cleanup: forgets an instance and its live core. Never part of normal play. */
    public static boolean discardInstance(ServerLevel level, BlockPos tombAnchor) {
        LIVE.remove(key(level, tombAnchor.asLong()));
        return data(level).discard(tombAnchor.asLong());
    }

    private static String key(ServerLevel level, long tomb) { return level.dimension().identifier() + "|" + tomb; }

    private static void tick(MinecraftServer server) {
        EntranceConfig cfg = EntranceConfig.current();
        if (cfg == null) {
            if (!warnedDisabled) { warnedDisabled = true; MoreMinecrafts.LOGGER.info("ST-01 secret entrance runtime disabled: EntranceConfig is null (explicitly disabled by test)."); }
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            SecretEntranceData data = data(level);
            if (data.isEmpty()) continue;
            for (SecretEntranceData.Instance inst : data.all()) {
                Live live = LIVE.computeIfAbsent(key(level, inst.key()), k -> new Live(new EntranceCore(cfg, new McPorts(level, inst, cfg),
                        new EntranceCore.Saved(inst.state(), inst.remaining(), inst.coverApplied()))));
                EntranceState before = live.core.state();
                live.core.step();
                lead(level, inst, live);
                if (data.put(inst.withSaved(live.core.saved())) && before != live.core.state())
                    MoreMinecrafts.LOGGER.info("ST-01 secret entrance at {} in {}: {} -> {}.", inst.tombAnchor().toShortString(),
                            level.dimension().identifier(), before, live.core.state());
            }
        }
    }

    /** Chooses the leader (sticky nearest online player near the tomb) and hands it to the guide golem. */
    private static void lead(ServerLevel level, SecretEntranceData.Instance inst, Live live) {
        UUID g = live.core.guide();
        if (live.core.state() != EntranceState.GOLEM_GUIDING || g == null) return;
        Entity e = level.getEntity(g);
        if (!(e instanceof GoldGuideGolem golem)) return;
        List<GuideTargetSelector.Candidate> candidates = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.blockPosition().distSqr(inst.tombAnchor()) >= LEADER_SEARCH_RADIUS * LEADER_SEARCH_RADIUS) continue;
            candidates.add(new GuideTargetSelector.Candidate(p.getUUID(), p.distanceTo(golem)));
        }
        golem.setLeader(live.selector.choose(candidates).orElse(null));
    }
}
