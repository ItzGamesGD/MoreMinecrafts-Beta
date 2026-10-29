package dev.itzgamesgd.moreminecrafts.colosseum;

/**
 * ST-05 Ancient Colosseum rules taken verbatim from the current authority
 * (docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md). Pure Java so JUnit can check every formula.
 * Timers are in server ticks (20 per second) and advance only while the controller is actively ticking.
 */
public final class ColosseumRules {
    private ColosseumRules() {}

    public static final int TOTAL_ROUNDS = 7;
    /** R1-R6 are threat-budget rounds; R7 is the Immortal Gladiator. */
    public static final int LAST_BUDGET_ROUND = 6;
    public static final int BOSS_ROUND = 7;

    public static final int MAX_ALIVE = 12;
    /** Replenish pending allocations once alive trial mobs fall to this many or fewer. */
    public static final int REPLENISH_AT_OR_BELOW = 6;
    /** Score at or above which a mob is an elite (R3 allows exactly one). */
    public static final int ELITE_SCORE = 7;

    public static final int SPAWN_INNER_RADIUS = 12;
    public static final int SPAWN_OUTER_RADIUS = 24;
    /** About one second of particle warning before each initial or replenishment spawn. */
    public static final int SPAWN_WARNING_TICKS = 20;
    /** Batch-spawn evaluation cadence: the normal replenishment cycle (no separate retry loop). */
    public static final int BATCH_CYCLE_TICKS = 20;

    public static final int ORDINARY_INTERMISSION_TICKS = 10 * 20;
    public static final int SUPPLY_INTERMISSION_TICKS = 35 * 20;
    public static final int ABSENCE_GRACE_TICKS = 60 * 20;
    /** Q110: 60 consecutive seconds of active trial time with every eligible spawn position invalid. */
    public static final int SPAWN_BLOCKED_FAIL_TICKS = 60 * 20;
    /** 65 minutes after success, failure or reset. */
    public static final int COOLDOWN_TICKS = 78_000;

    public static boolean isBudgetRound(int round) { return round >= 1 && round <= LAST_BUDGET_ROUND; }

    /** Solo threat budget: R1-R2 20, R3-R4 32, R5-R6 48. */
    public static int soloBudget(int round) {
        if (!isBudgetRound(round)) throw new IllegalArgumentException("no threat budget for round " + round);
        return round <= 2 ? 20 : round <= 4 ? 32 : 48;
    }

    /** solo x (1 + 0.60 x (n - 1)), rounded to the nearest integer (half up), with exact integer arithmetic. */
    public static int budget(int round, int participants) {
        if (participants < 1) throw new IllegalArgumentException("a round needs at least one participant");
        return (soloBudget(round) * (10 + 6 * (participants - 1)) + 5) / 10;
    }

    /** Cumulative score eligibility: R1-R2 1-4, R3-R4 1-7, R5-R6 1-10. */
    public static int scoreCap(int round) {
        if (!isBudgetRound(round)) throw new IllegalArgumentException("no roster for round " + round);
        return round <= 2 ? 4 : round <= 4 ? 7 : 10;
    }

    /** ceil(85% x budget). */
    public static int normalFillMin(int budget) { return (budget * 85 + 99) / 100; }

    /** floor(105% x budget), only when no normal composition exists. */
    public static int exceptionalFillMax(int budget) { return budget * 105 / 100; }

    /** Supplies become accessible during a 35-second interval after R3 and R5. */
    public static boolean isSupplyRound(int clearedRound) { return clearedRound == 3 || clearedRound == 5; }

    public static int intermissionTicksAfter(int clearedRound) {
        return isSupplyRound(clearedRound) ? SUPPLY_INTERMISSION_TICKS : ORDINARY_INTERMISSION_TICKS;
    }
}
