"""ST-01 P7 reference model of the secret-entrance / gold-golem guidance state machine (design-only, pure Python, NOT game code).
Abstract time: one TICK = one abstract time unit; D (deadline length) is a SYMBOLIC parameter, not an approved value.
The model separates PERSISTENT data (survives restart: boss defeated record, entrance record, remaining time, physical cover blocks)
from VOLATILE data (golem entity, chunk loaded, players online). `bugs` injects deliberate faults for negative controls."""
from collections import namedtuple, deque
import itertools, random

D = 2                                   # abstract fallback length in TICK units (symbolic; real value UNAPPROVED)
RANK = {"SEALED": 0, "GUARDIAN_DEFEATED": 1, "GOLEM_GUIDING": 2, "ENTRANCE_OPEN": 3, "FALLBACK_OPEN": 3}
OPEN_STATES = ("ENTRANCE_OPEN", "FALLBACK_OPEN")

# persistent: boss (boss_defeated source of truth, TombEncounterData), rec (entrance record), remaining (ticks left), cover (physical blocks removed), trapped (a player was buried)
# volatile:   golem 'none'|'alive'|'stalled', ngolem (alive golem entities incl. stalled), chunk (cover chunk loaded), online, occupied (a player stands on a cover cell), spawn_pending, defeat_event_lost
S = namedtuple("S", "boss rec remaining cover trapped golem ngolem chunk online occupied spawn_pending fell")
INIT = S(boss=False, rec="SEALED", remaining=D, cover=False, trapped=False, golem="none", ngolem=0, chunk=True, online=True, occupied=False, spawn_pending=False, fell=False)
EVENTS = ["BOSS_KILL", "DEFEATED_EVENT", "FAKE_DEFEATED_EVENT", "SPAWN_OK", "SPAWN_FAIL", "GOLEM_PATH_FAIL", "GOLEM_LOST", "GOLEM_ARRIVE", "TICK", "PLAYER_LEAVE", "PLAYER_JOIN",
          "RESTART", "CHUNK_UNLOAD", "CHUNK_LOAD", "OPEN_REQUEST_DUP", "CHEST_INTERACT_EARLY", "PLAYER_STEP_ON_COVER", "PLAYER_STEP_OFF_COVER"]
IDEMPOTENT = {"DEFEATED_EVENT", "FAKE_DEFEATED_EVENT", "GOLEM_ARRIVE", "OPEN_REQUEST_DUP", "CHEST_INTERACT_EARLY", "GOLEM_PATH_FAIL", "BOSS_KILL"}

def _r(s, **k): return s._replace(**k)

def reconcile(s, bugs):
    """Idempotent: derive/repair the record from the source of truth and apply the physical cover. Runs after every event and on load."""
    if s.boss and s.rec == "SEALED" and "no_reconcile_record" not in bugs:
        s = _r(s, rec="GUARDIAN_DEFEATED", remaining=D, spawn_pending=(s.ngolem == 0))
    if s.rec in OPEN_STATES and s.chunk and not s.cover and "no_physical_reconcile" not in bugs:
        s = _r(s, cover=True, fell=s.occupied or s.fell)
        if "bury_on_open" in bugs and s.occupied: s = _r(s, trapped=True)
    return s

def step(s, e, bugs=frozenset()):
    n = s
    if e == "BOSS_KILL":
        if not s.boss: n = _r(s, boss=True)
    elif e in ("DEFEATED_EVENT", "FAKE_DEFEATED_EVENT"):
        # FAKE_DEFEATED_EVENT = an event delivered although the source of truth says "not defeated" (spoof/stale/wrong tomb); a correct coordinator re-checks the record
        if s.boss or "trust_event" in bugs:
            if s.rec == "SEALED":
                n = _r(s, rec="GUARDIAN_DEFEATED", remaining=D, spawn_pending=True)
                if "trust_event" in bugs and not s.boss: n = _r(n, rec="ENTRANCE_OPEN")
            elif "dup_spawn" in bugs and s.rec in ("GUARDIAN_DEFEATED", "GOLEM_GUIDING"):
                n = _r(s, spawn_pending=True)
    elif e == "SPAWN_OK":
        if s.spawn_pending and s.rec == "GUARDIAN_DEFEATED":
            n = _r(s, rec="GOLEM_GUIDING", golem="alive", ngolem=min(s.ngolem + 1, 3), spawn_pending=False)
        elif s.spawn_pending and "dup_spawn" in bugs:
            n = _r(s, golem="alive", ngolem=min(s.ngolem + 1, 3), spawn_pending=False)
    elif e == "SPAWN_FAIL":
        if s.spawn_pending and s.rec == "GUARDIAN_DEFEATED": n = _r(s, spawn_pending=False)       # stays GUARDIAN_DEFEATED; timer keeps running
    elif e == "GOLEM_PATH_FAIL":
        if s.golem == "alive": n = _r(s, golem="stalled")
    elif e == "GOLEM_LOST":
        if s.ngolem > 0:
            n = _r(s, golem="none", ngolem=0)
            if "cancel_timer_on_loss" in bugs: n = _r(n, remaining=-1)
    elif e == "GOLEM_ARRIVE":
        if s.golem == "alive" and s.rec == "GOLEM_GUIDING": n = _r(s, rec="ENTRANCE_OPEN")
    elif e == "TICK":
        if s.rec in ("GUARDIAN_DEFEATED", "GOLEM_GUIDING") and s.remaining >= 0:
            rem = s.remaining - 1
            n = _r(s, remaining=rem)
            if rem <= 0:
                if s.boss or {"chest_starts_timer", "fallback_unlocks_chest"} & set(bugs):
                    n = _r(n, rec="FALLBACK_OPEN")
                    if "fallback_unlocks_chest" in bugs: n = _r(n, boss=True)
        elif s.rec == "SEALED" and "open_from_sealed_timer" in bugs: n = _r(s, rec="FALLBACK_OPEN")
    elif e == "PLAYER_LEAVE":
        n = _r(s, online=False, occupied=False)
        if "timer_needs_player" in bugs: pass
    elif e == "PLAYER_JOIN": n = _r(s, online=True)
    elif e == "RESTART":
        # persistent survive; volatile reset. Golem entity is not assumed to survive (conservative).
        n = _r(s, golem="none", ngolem=0, chunk=True, online=False, occupied=False, spawn_pending=False)
        if "restart_resets_record" in bugs: n = _r(n, rec="SEALED", remaining=D)
        if "restart_loses_deadline" in bugs and s.rec in ("GUARDIAN_DEFEATED", "GOLEM_GUIDING"): n = _r(n, remaining=-1)
    elif e == "CHUNK_UNLOAD": n = _r(s, chunk=False)
    elif e == "CHUNK_LOAD": n = _r(s, chunk=True)
    elif e == "OPEN_REQUEST_DUP":
        if s.rec in OPEN_STATES and s.chunk:
            n = _r(s, cover=True)
        elif "open_from_sealed_request" in bugs and s.rec == "SEALED" and s.chunk: n = _r(s, cover=True)
    elif e == "CHEST_INTERACT_EARLY":
        if "chest_opens_entrance" in bugs and not s.boss: n = _r(s, rec="ENTRANCE_OPEN")
        if ({"chest_starts_timer", "fallback_unlocks_chest"} & set(bugs)) and not s.boss and s.rec == "SEALED": n = _r(s, rec="GUARDIAN_DEFEATED", remaining=D)
    elif e == "PLAYER_STEP_ON_COVER":
        if s.online: n = _r(s, occupied=True)
    elif e == "PLAYER_STEP_OFF_COVER": n = _r(s, occupied=False)
    # --- bookkeeping after every event
    if "auto_restore" in bugs and n.cover and not n.occupied and s.occupied and not n.online: n = _r(n, cover=False, trapped=False)
    if "auto_restore_buries" in bugs and s.cover and e == "PLAYER_STEP_ON_COVER": n = _r(n, trapped=True)
    n = reconcile(n, bugs)
    if "cancel_timer_on_loss" in bugs and n.remaining == -1 and n.rec in ("GUARDIAN_DEFEATED", "GOLEM_GUIDING"): pass
    return n

def enabled(s, e):
    if e == "DEFEATED_EVENT": return s.boss                      # event is only ever fired after a kill (may be lost: simply never chosen)
    if e in ("SPAWN_OK", "SPAWN_FAIL"): return s.spawn_pending
    if e == "GOLEM_ARRIVE": return s.golem == "alive" or True
    return True

def check_transition(s, e, n, bugs):
    v = []
    if (n.rec in OPEN_STATES or n.cover) and not n.boss: v.append("I1 opened before the guardian was defeated")
    if RANK[n.rec] < RANK[s.rec]: v.append("I2 record regressed")
    if s.cover and not n.cover: v.append("I2 cover blocks restored (no restore rule approved)")
    if n.ngolem > 1: v.append("I5 more than one golem")
    if n.boss != s.boss and e != "BOSS_KILL": v.append("I6 P7 event changed the boss-defeated (reward) condition")
    if n.trapped: v.append("I9 player buried by the cover")
    if n.cover and n.rec not in OPEN_STATES: v.append("I8 cover removed without an open record")
    if e in IDEMPOTENT:
        n2 = step(n, e, bugs)
        if n2 != n: v.append(f"I3 event {e} not idempotent")
    if e == "RESTART":
        for f in ("boss", "rec", "remaining"):
            if getattr(n, f) != getattr(s, f) and not (f == "rec" and s.rec == "SEALED" and s.boss): v.append(f"I4 restart changed persistent field {f}")
        if s.cover and not n.cover: v.append("I4 restart lost the physical cover state")
        if s.rec in ("GUARDIAN_DEFEATED", "GOLEM_GUIDING") and n.remaining < 0: v.append("I4 restart lost the fallback deadline")
    return v

BENIGN = ("CHUNK_LOAD", "TICK", "PLAYER_JOIN")
def liveness_ok(s, bugs, k=D + 4):
    """From any state with a defeated guardian, benign events alone (chunks load, time passes) must open the cover within k events."""
    if not s.boss: return True
    seen = {s}; fr = [s]
    for _ in range(k):
        nx = []
        for x in fr:
            if x.cover: return True
            for e in BENIGN:
                y = step(x, e, bugs)
                if y not in seen: seen.add(y); nx.append(y)
        fr = nx
    return any(x.cover for x in seen)

def explore(bugs=frozenset(), max_states=200000):
    seen = {INIT: None}; q = deque([INIT]); viol = []; edges = 0
    while q:
        s = q.popleft()
        for e in EVENTS:
            if not enabled(s, e): continue
            n = step(s, e, bugs); edges += 1
            for m in check_transition(s, e, n, bugs): viol.append((m, s, e, n))
            if n not in seen:
                seen[n] = (s, e)
                if len(seen) > max_states: raise RuntimeError("state explosion")
                q.append(n)
    for s in seen:
        if not liveness_ok(s, bugs): viol.append(("I7 liveness: defeated guardian but cover never opens by benign events", s, "-", s))
    return seen, viol, edges

def trace(seen, s):
    t = []
    while seen[s] is not None: p, e = seen[s]; t.append(e); s = p
    return t[::-1]

# ---- multi-tomb isolation: two independent records keyed by tomb; random event sequences. A global/shared record (bug) leaks.
def multi_tomb_isolation(bugs=frozenset(), runs=4000, depth=14, seed=7):
    rng = random.Random(seed); bad = 0
    for _ in range(runs):
        a = b = INIT
        for _ in range(depth):
            tomb = rng.choice("AB"); e = rng.choice(EVENTS)
            if tomb == "A":
                a = step(a, e, bugs)
                if "shared_record" in bugs and a.rec in OPEN_STATES: b = _r(b, rec=a.rec)
            else:
                b = step(b, e, bugs)
            if not b.boss and (b.rec in OPEN_STATES or b.cover): bad += 1; break
    return bad

# ---- scenario traces (event scripts) used in the documentation
SCENARIOS = {
 "N1 normal: kill -> golem spawns -> arrives -> OPEN": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "GOLEM_ARRIVE"],
 "F1 golem spawn fails -> timer -> FALLBACK": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_FAIL", "TICK", "TICK"],
 "F2 path failure -> timer -> FALLBACK": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "GOLEM_PATH_FAIL", "TICK", "TICK"],
 "F3 golem lost/dies while guiding -> timer -> FALLBACK": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "GOLEM_LOST", "TICK", "TICK"],
 "F4 server restart while guiding -> record kept -> FALLBACK": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "TICK", "RESTART", "TICK"],
 "F5 chunk unloaded at deadline -> cover applied on reload": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "CHUNK_UNLOAD", "TICK", "TICK", "CHUNK_LOAD"],
 "F6 DEFEATED event lost (listener failure) -> reconcile from boss record": ["BOSS_KILL", "TICK", "SPAWN_OK", "GOLEM_ARRIVE"],
 "E1 duplicate DEFEATED events -> one golem": ["BOSS_KILL", "DEFEATED_EVENT", "DEFEATED_EVENT", "SPAWN_OK", "DEFEATED_EVENT", "GOLEM_ARRIVE"],
 "E2 duplicate open requests": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "GOLEM_ARRIVE", "OPEN_REQUEST_DUP", "OPEN_REQUEST_DUP"],
 "E3 chest touched before the kill": ["CHEST_INTERACT_EARLY", "TICK", "TICK", "TICK", "OPEN_REQUEST_DUP"],
 "E4 player stands on the cover when it opens": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "PLAYER_STEP_ON_COVER", "GOLEM_ARRIVE"],
 "E5 all players leave during guiding; timer still opens it": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "PLAYER_LEAVE", "TICK", "TICK", "PLAYER_JOIN"],
 "E6 arrival after fallback is a no-op": ["BOSS_KILL", "DEFEATED_EVENT", "SPAWN_OK", "TICK", "TICK", "GOLEM_ARRIVE"],
}
def run_scenario(evs, bugs=frozenset()):
    s = INIT; out = [("-", s)]
    for e in evs: s = step(s, e, bugs); out.append((e, s))
    return out
