package dev.itzgamesgd.moreminecrafts.accessory;

/** Rate/count bounds copied from §2.2; exact rolls and counts remain owner decisions where the source gives ranges. */
public final class ArtifactCoreDropRules {
    public enum Tier {
        GENERAL_STRONG(8, 12, 1, 1),
        HIGH_ELITE(35, 50, 1, 1),
        BOSS(100, 100, 1, 2);

        private final int minPercent;
        private final int maxPercent;
        private final int minCount;
        private final int maxCount;

        Tier(int minPercent, int maxPercent, int minCount, int maxCount) {
            this.minPercent = minPercent;
            this.maxPercent = maxPercent;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }

        public int minPercent() { return minPercent; }
        public int maxPercent() { return maxPercent; }
        public int minCount() { return minCount; }
        public int maxCount() { return maxCount; }
    }

    private ArtifactCoreDropRules() {}
}
