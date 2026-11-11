package dev.itzgamesgd.moreminecrafts.colosseum;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class GladiatorCombatTest {
    @Test void fiveFrontalBlocksDisableOnlyTheShieldForTwentySeconds() {
        GladiatorCombat c = new GladiatorCombat();
        for (int i=0;i<5;i++) assertTrue(c.block(false));
        assertFalse(c.canBlock()); assertEquals(400, c.shieldDisabled);
        for (int i=0;i<400;i++) c.tick();
        assertTrue(c.canBlock()); assertTrue(c.block(true)); assertFalse(c.canBlock());
    }
    @Test void phaseTwoIsPermanentAndHasAResponseWindow() {
        GladiatorCombat c = new GladiatorCombat();
        assertFalse(c.updatePhase(121,300)); assertTrue(c.updatePhase(120,300));
        assertFalse(c.canBlock()); assertTrue(c.recovery > 0);
        assertFalse(c.updatePhase(200,300)); assertTrue(c.phaseTwo);
        GladiatorCombat d = ArenaController.GSON.fromJson(ArenaController.GSON.toJson(c), GladiatorCombat.class);
        assertTrue(d.phaseTwo); assertEquals(c.recovery,d.recovery);
    }
    @Test void obstacleChargeRequiresFreshFailedPursuitAndOneSidedDamage() {
        GladiatorCombat c = new GladiatorCombat();
        for(int i=0;i<10;i++) c.failedPursuit();
        assertFalse(c.obstacleReady()); c.attackedWithoutRetaliation(); assertTrue(c.obstacleReady());
        c.beginObstacleCharge(); assertFalse(c.obstacleReady());
        for(int i=0;i<600;i++) c.tick();
        assertFalse(c.obstacleReady());
    }
    @Test void disablingShieldCancelsAnAlreadyQueuedCounter() {
        GladiatorCombat c=new GladiatorCombat();c.attack=GladiatorCombat.Attack.COUNTER;c.windup=12;c.comboLeft=2;
        assertTrue(c.block(true));
        assertEquals(GladiatorCombat.Attack.NONE,c.attack);assertEquals(0,c.windup);assertEquals(0,c.comboLeft);
    }

}
