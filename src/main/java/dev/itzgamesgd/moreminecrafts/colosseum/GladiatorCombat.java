package dev.itzgamesgd.moreminecrafts.colosseum;

/** Persisted arena-owned combat clock. Timings/reach/special damage in the entity are provisional engineering tuning, not new authority. */
final class GladiatorCombat {
    enum Attack { NONE, COMBO, LUNGE, SWEEP, CHARGE, OBSTACLE, COUNTER }
    boolean phaseTwo;
    int blocks, shieldDisabled, recovery, windup, comboLeft, attackIndex, attackTicks;
    int pursuitWindow, failures, oneSidedHits, obstacleCooldown;
    Attack attack = Attack.NONE;
    double targetX, targetY, targetZ, dirX, dirZ;
    boolean canBlock() { return !phaseTwo && shieldDisabled == 0; }
    boolean block(boolean axe) {
        if (!canBlock()) return false;
        if (axe || ++blocks >= 5) { shieldDisabled = 400; blocks = 0;
            if(attack==Attack.COUNTER) {attack=Attack.NONE;windup=0;comboLeft=0;} }
        return true;
    }
    boolean updatePhase(float health, float max) {
        if (phaseTwo || health <= 0 || health > max * .4F) return false;
        phaseTwo = true; recovery = 40; attack = Attack.NONE; windup = 0; comboLeft = 0; return true;
    }
    void tick() {
        if (shieldDisabled > 0) shieldDisabled--;
        if (recovery > 0) recovery--;
        if (obstacleCooldown > 0) obstacleCooldown--;
        if (pursuitWindow > 0 && --pursuitWindow == 0) { failures = 0; oneSidedHits = 0; }
    }
    void failedPursuit() { if (pursuitWindow == 0) pursuitWindow = 400; failures++; }
    void attackedWithoutRetaliation() { if (pursuitWindow == 0) pursuitWindow = 400; oneSidedHits++; }
    boolean obstacleReady() { return obstacleCooldown == 0 && pursuitWindow > 0 && failures >= 10 && oneSidedHits > 0; }
    void beginObstacleCharge() { obstacleCooldown = 600; failures = 0; oneSidedHits = 0; pursuitWindow = 0; }
    void retaliated() { failures = 0; oneSidedHits = 0; pursuitWindow = 0; }
}
