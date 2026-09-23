package dev.itzgamesgd.moreminecrafts.fishing;

public enum FishSpecies {
    BLUEGILL("bluegill", 12, 30, 90, 1, 1, 110),
    PERCH("perch", 18, 42, 105, 1, 1, 100),
    TROUT("trout", 25, 70, 125, 2, 1, 88),
    CATFISH("catfish", 35, 120, 140, 2, 2, 72),
    SALMON("salmon", 45, 110, 155, 3, 2, 62),
    PIKE("pike", 50, 135, 175, 3, 3, 48),
    STURGEON("sturgeon", 80, 220, 205, 4, 4, 30),
    GOLDEN_KOI("golden_koi", 30, 85, 240, 2, 4, 20),
    ABYSSAL_ANGLER("abyssal_angler", 40, 100, 280, 5, 5, 12),
    ANCIENT_COELACANTH("ancient_coelacanth", 90, 190, 320, 5, 5, 8);

    private final String id;
    private final int minLengthCm, maxLengthCm, qualityFactor, depthTier, minnowGrade, weight;
    FishSpecies(String id, int minLengthCm, int maxLengthCm, int qualityFactor, int depthTier, int minnowGrade, int weight) {
        this.id=id; this.minLengthCm=minLengthCm; this.maxLengthCm=maxLengthCm; this.qualityFactor=qualityFactor;
        this.depthTier=depthTier; this.minnowGrade=minnowGrade; this.weight=weight;
    }
    public String id(){return id;} public int minLengthCm(){return minLengthCm;} public int maxLengthCm(){return maxLengthCm;}
    public int qualityFactor(){return qualityFactor;} public int depthTier(){return depthTier;} public int minnowGrade(){return minnowGrade;} public int weight(){return weight;}
    public int lengthFromRoll(int roll) { return minLengthCm + Math.floorMod(roll, maxLengthCm-minLengthCm+1); }
    public int quality(int grade, int lengthCm) {
        FishingGrade.requireValid(grade);
        int normalizedPermille = 500 + (lengthCm-minLengthCm)*500/Math.max(1,maxLengthCm-minLengthCm);
        return qualityFactor * grade * normalizedPermille / 1000;
    }
}
