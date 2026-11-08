package dev.itzgamesgd.moreminecrafts.st01;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy;
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
 * Operational adapter that runs one {@link EntranceCore} per registered structure instance.
 *
 * <p>Per ST-01 P11 owner approval (O-1 = 600 ticks, O-3 = immediate timer start on valid victory,
 * O-4 = 1.5-block arrival radius at Z3 stop point {@code (±9, -2, 3)}, O-6 = scale 0.5 / speed 0.25
 * / {@code BOSS_S} spawn), {@link EntranceConfig#PRODUCTION} is installed by default on the production
 * server tick path. Natural worldgen placement of ST-01 structures (O-14) and final East/West
 * orientation selection (D-3) remain unapproved, so no worldgen structure auto-registration is
 * performed here; registered tomb instances use the production runtime directly.
 */
public final class SecretEntranceRuntime {
    private SecretEntranceRuntime() {}

    /** TEST-ONLY seams for game tests. Never set in production code. */
    public static final class TestHooks {
        private TestHooks() {}
        /** Tomb keys whose guide golem creation is forced to fail. Per instance so concurrently running tests do not interfere. */
        public static final java.util.Set<Long> failGolemSpawnFor = java.util.concurrent.ConcurrentHashMap.newKeySet();
    }

    private static final class Live {
        final EntranceCore core;
        final McPorts ports;
        final GuideTargetSelector selector = new GuideTargetSelector(3.0D);       // margin: candidate, unapproved
        Live(EntranceCore core, McPorts ports) { this.core = core; this.ports = ports; }
    }

    private static final Map<String, Live> LIVE = new HashMap<>();
    private static boolean initialized;
    private static boolean warnedDisabled;

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        TombGuardianEncounters.VICTORY.register(SecretEntranceRuntime::onVictory);
        ServerTickEvents.END_SERVER_TICK.register(SecretEntranceRuntime::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LIVE.clear());
    }

    /** Test/diagnostic support: drops the in-memory cores (as a server restart would); persisted records are untouched. */
    public static void forgetLiveState() { LIVE.clear(); }

    public static SecretEntranceData data(ServerLevel level) { return level.getDataStorage().computeIfAbsent(SecretEntranceData.TYPE); }

    /** Registers a structure instance (called by structure placement in the future; today only by tests). Never overwrites an existing record. */
    public static boolean register(ServerLevel level, BlockPos anchor, EntranceLayout.Orientation orientation) {
        return data(level).register(anchor, orientation);
    }

    /** Valid-victory signal. Only records participants; the state change happens in the tick (never inside the boss death handler). */
    private static void onVictory(ServerLevel level, BlockPos anchor, TombVictoryPolicy.Verdict verdict) {
        SecretEntranceData data = data(level);
        SecretEntranceData.Instance inst = data.get(anchor.asLong());
        if (inst != null && inst.participants().isEmpty()) data.put(inst.withParticipants(new ArrayList<>(verdict.participants())));
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
                Live live = LIVE.computeIfAbsent(key(level, inst.key()), k -> {
                    McPorts ports = new McPorts(level, inst, cfg);
                    return new Live(new EntranceCore(cfg, ports, new EntranceCore.Saved(inst.state(), inst.remaining(), inst.coverApplied())), ports);
                });
                live.core.step();
                lead(level, inst, live);
                data.put(inst.withSaved(live.core.saved()));
            }
        }
    }

    /** Chooses the leader (sticky nearest online participant) and hands it to the guide golem. */
    private static void lead(ServerLevel level, SecretEntranceData.Instance inst, Live live) {
        UUID g = live.core.guide();
        if (live.core.state() != EntranceState.GOLEM_GUIDING || g == null) return;
        Entity e = level.getEntity(g);
        if (!(e instanceof GoldGuideGolem golem)) return;
        List<GuideTargetSelector.Candidate> candidates = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            boolean participant = inst.participants().isEmpty() ? p.blockPosition().distSqr(inst.anchor()) < 64.0D * 64.0D : inst.participants().contains(p.getUUID());
            if (participant) candidates.add(new GuideTargetSelector.Candidate(p.getUUID(), p.distanceTo(golem)));
        }
        golem.setLeader(live.selector.choose(candidates).orElse(null));
    }
}
