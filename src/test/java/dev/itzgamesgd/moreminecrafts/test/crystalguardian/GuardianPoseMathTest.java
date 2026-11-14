package dev.itzgamesgd.moreminecrafts.test.crystalguardian;

import static dev.itzgamesgd.moreminecrafts.test.crystalguardian.GuardianPoseMath.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GuardianPoseMathTest {
    private static final float EPS = 1.0E-5F;

    @Test
    void standingStillIsExactlyTheRestPose() {
        assertEquals(Pose.REST, walk(0.0F, 0.0F));
        assertEquals(Pose.REST, walk(123.4F, 0.0F));
        assertEquals(Pose.REST, compose(7.0F, 0.0F, -1.0F), "no swing and no walk -> rest");
        assertEquals(Pose.REST, compose(7.0F, 0.0F, SWING_SECONDS + 5.0F), "swing finished -> rest");
    }

    @Test
    void legsCrossAndArmsOpposeTheSameSideLeg() {
        for (float swing = 0.0F; swing < 40.0F; swing += 0.37F) {
            Pose p = walk(swing, 0.8F);
            assertEquals(p.legRight(), -p.legLeft(), EPS, "legs are in antiphase");
            assertTrue(p.legRight() * p.armRight() <= EPS, "right arm opposes right leg");
            assertTrue(p.legLeft() * p.armLeft() <= EPS, "left arm opposes left leg");
            assertEquals(p.armRight(), -p.armLeft(), EPS);
        }
    }

    @Test
    void shinsOnlyBendBackwardAndOnlyOnTheForwardSwing() {
        for (float swing = 0.0F; swing < 40.0F; swing += 0.11F) {
            Pose a = walk(swing, 1.0F), b = walk(swing + 0.02F, 1.0F);
            assertTrue(a.shinRight() >= 0 && a.shinLeft() >= 0, "knee never hyper-extends");
            assertTrue(a.shinRight() <= KNEE_FLEX + EPS);
            if (a.shinRight() > 1.0E-3F) assertTrue(b.legRight() < a.legRight() + EPS, "right knee flexes while the right leg swings forward");
            if (a.shinLeft() > 1.0E-3F) assertTrue(b.legLeft() < a.legLeft() + EPS, "left knee flexes while the left leg swings forward");
            assertTrue(a.forearmRight() <= EPS && a.forearmLeft() <= EPS, "elbows only flex forward");
        }
    }

    @Test
    void amplitudeFollowsSpeedAndIsClamped() {
        float slow = maxAbs(0.3F), fast = maxAbs(1.0F), over = maxAbs(5.0F);
        assertTrue(slow < fast);
        assertEquals(fast, over, EPS, "limb swing amount above 1 is clamped");
        assertEquals(LEG_AMPLITUDE, fast, 1.0E-3F);
    }

    @Test
    void feetStayOnTheGround() {
        for (float swing = 0.0F; swing < 60.0F; swing += 0.05F) {
            Pose p = walk(swing, 1.0F);
            assertTrue(p.rootDrop() >= 0.0F && p.rootDrop() < 4.0F, "root drop stays small and non-negative: " + p.rootDrop());
            float lowest = Math.min(sole(p.legRight(), p.shinRight()), sole(p.legLeft(), p.shinLeft())) - p.rootDrop();
            assertEquals(0.0F, lowest, 1.0E-3F, "lowest foot sole is planted on the ground plane");
        }
    }

    @Test
    void swingStartsAndEndsAtZeroAndHitsItsKeyPoses() {
        assertArrayEquals(new float[] {0, 0, 0}, swingKey(0.0F), EPS);
        assertArrayEquals(new float[] {0, 0, 0}, swingKey(SWING_SECONDS), EPS);
        assertEquals(ARM_WINDUP, swingKey(WINDUP_END - 1.0E-4F)[0], 1.0E-3F, "windup: arm pulled back/up");
        assertEquals(ARM_STRIKE, swingKey(STRIKE_END)[0], 1.0E-3F, "strike: arm swung far forward");
        assertTrue(swingKey(WINDUP_END)[0] > 0 && swingKey(STRIKE_END)[0] < 0, "back then forward");
        assertTrue(swingKey(STRIKE_END)[1] > swingKey(WINDUP_END)[1], "forearm unfolds during the strike");
    }

    @Test
    void swingIsContinuous() {
        float prev = swingKey(0.001F)[0];
        for (float t = 0.002F; t < SWING_SECONDS; t += 0.001F) {
            float cur = swingKey(t)[0];
            assertTrue(Math.abs(cur - prev) < 0.06F, "no pops in the arm curve at t=" + t);
            prev = cur;
        }
    }

    @Test
    void swingOwnsOnlyTheRightArmChainAndTorsoTwist() {
        for (float t = 0.0F; t <= SWING_SECONDS + 0.2F; t += 0.013F) {
            Pose w = walk(11.0F, 0.9F), c = compose(11.0F, 0.9F, t);
            assertEquals(w.legRight(), c.legRight(), EPS);
            assertEquals(w.legLeft(), c.legLeft(), EPS);
            assertEquals(w.shinRight(), c.shinRight(), EPS);
            assertEquals(w.shinLeft(), c.shinLeft(), EPS);
            assertEquals(w.armLeft(), c.armLeft(), EPS, "left arm keeps walking");
            assertEquals(w.forearmLeft(), c.forearmLeft(), EPS);
            assertEquals(w.rootDrop(), c.rootDrop(), EPS);
        }
        Pose mid = compose(11.0F, 0.9F, STRIKE_END);
        assertEquals(ARM_STRIKE, mid.armRight(), 1.0E-3F, "mid-swing the arm follows the swing, not the walk");
    }

    @Test
    void handOverBetweenWalkAndSwingHasNoPop() {
        Pose before = compose(5.0F, 1.0F, -1.0F), start = compose(5.0F, 1.0F, 0.0005F);
        assertEquals(before.armRight(), start.armRight(), 0.01F);
        Pose end = compose(5.0F, 1.0F, SWING_SECONDS - 0.0005F), after = compose(5.0F, 1.0F, SWING_SECONDS + 0.05F);
        assertEquals(after.armRight(), end.armRight(), 0.01F);
        assertEquals(after.forearmRight(), end.forearmRight(), 0.01F);
        assertEquals(after.torsoYaw(), end.torsoYaw(), 0.01F);
    }

    @Test
    void solverIsStatelessSoRepeatedRunsCannotAccumulate() {
        Pose first = compose(3.3F, 0.7F, 0.9F);
        for (int i = 0; i < 1000; i++) compose(i * 0.1F, 0.7F, i * 0.013F);
        assertEquals(first, compose(3.3F, 0.7F, 0.9F));
        assertEquals(Pose.REST, compose(0.0F, 0.0F, SWING_SECONDS), "after any number of swings the rest pose is exactly restored");
    }

    @Test
    void swingTicksCoverTheWholeTimeline() {
        assertTrue(SWING_TICKS * 0.05F >= SWING_SECONDS);
        assertTrue((SWING_TICKS - 1) * 0.05F < SWING_SECONDS);
    }

    private static float maxAbs(float amount) {
        float m = 0;
        for (float s = 0; s < 40; s += 0.01F) m = Math.max(m, Math.abs(walk(s, amount).legRight()));
        return m;
    }

    private static float sole(float thigh, float shin) {
        return HIP_HEIGHT - (THIGH_LENGTH * (float) Math.cos(thigh) + SHIN_LENGTH * (float) Math.cos(thigh + shin));
    }
}
