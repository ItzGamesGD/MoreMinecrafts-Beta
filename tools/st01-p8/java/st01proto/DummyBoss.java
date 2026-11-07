package st01proto;

import java.util.*;
import java.util.function.Consumer;
import st01proto.ParticipationTracker.*;
import st01proto.Ports.*;

/** Minimal stand-in for the Tomb Guardian (no attacks, no model, no AI, no size). Replaceable through BossLifecycle. */
public final class DummyBoss implements BossLifecycle {
    private final InstanceId id; private final EncounterStore store; private final RewardPort reward; private final ParticipationTracker tracker;
    private final double maxHealth; private double hp; private Consumer<DefeatReport> listener = r -> {}; private boolean dead;
    public boolean retryAllowed;

    public DummyBoss(InstanceId id, EncounterStore store, RewardPort reward, Policy policy, ParticipationTracker.Params params, double maxHealth) {
        this.id = id; this.store = store; this.reward = reward; this.maxHealth = maxHealth; this.hp = maxHealth; this.tracker = new ParticipationTracker(maxHealth, policy, params);
    }
    @Override public void setListener(Consumer<DefeatReport> l) { listener = l; }
    public void hitByPlayer(UUID p, double dmg, long tick) { if (dead) return; tracker.onPlayerDamage(p, dmg, tick); hp -= dmg; }
    public void hitByOther(double dmg, long tick) { if (dead) return; tracker.onOtherDamage(dmg, tick); hp -= dmg; }
    public boolean dying() { return hp <= 0; }
    public void die(DeathCause cause, long tick) {
        if (dead) return; dead = true;
        Verdict v = tracker.judge(cause, tick);
        store.markDeath(id);
        if (v.valid()) { store.markVictory(id); reward.grant(id); } else retryAllowed = true;      // invalid: chest stays sealed, no reward, encounter may be retried
        listener.accept(new DefeatReport(id, v.valid(), v.participants(), v.reason()));
    }
}
