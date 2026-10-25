package dev.itzgamesgd.moreminecrafts.progression;

/** Conserves every vanilla orb point while carrying the approved per-player fractional bonus. */
public final class ResearchXpAcquisitionRules {
    public record Award(int totalXp, int remainder) {}
    private ResearchXpAcquisitionRules() {}

    public static Award apply(int vanillaXp, int level, int remainder) {
        int percent = Math.max(0, Math.min(3, level)) * 3;
        ResearchPassiveMath.Fraction fraction = ResearchPassiveMath.accumulatePercent(vanillaXp, remainder, percent);
        return new Award(Math.max(0, vanillaXp) + fraction.wholeBonus(), fraction.remainder());
    }
}
