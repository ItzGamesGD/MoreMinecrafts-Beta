package dev.itzgamesgd.moreminecrafts.fishing;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public final class FishingLogic {
    private FishingLogic() {}
    public static int accessibleDepth(int measuredDepth, int reelGrade) { return Math.min(Math.max(1, measuredDepth), FishingGrade.requireValid(reelGrade)); }
    public static List<FishSpecies> eligible(int measuredDepth, int reelGrade, int minnowGrade) {
        int accessible = accessibleDepth(measuredDepth, reelGrade);
        FishingGrade.requireValid(minnowGrade);
        return java.util.Arrays.stream(FishSpecies.values()).filter(s -> s.depthTier() <= accessible && s.minnowGrade() <= minnowGrade).toList();
    }
    public static int weight(FishSpecies species, int fishGrade, int crankbaitGrade, int luck) {
        FishingGrade.requireValid(fishGrade); FishingGrade.requireValid(crankbaitGrade);
        int upperBias = 100 + (crankbaitGrade-1)*12*(fishGrade-1) + Math.max(0,luck)*3*(fishGrade-1);
        return Math.max(1, species.weight() * gradeBaseWeight(fishGrade) * upperBias / 100);
    }
    static int gradeBaseWeight(int grade) { return switch(grade){case 1->100; case 2->55; case 3->25; case 4->9; case 5->3; default->throw new IllegalArgumentException();}; }
    public static CatchRoll select(List<FishSpecies> eligible, int crankbaitGrade, int luck, RandomGenerator random) {
        if (eligible.isEmpty()) throw new IllegalArgumentException("No eligible fish");
        record Weighted(FishSpecies species,int grade,int weight) {}
        List<Weighted> values=new ArrayList<>(); int total=0;
        for(FishSpecies s:eligible) for(int g=1;g<=5;g++){int w=weight(s,g,crankbaitGrade,luck);values.add(new Weighted(s,g,w));total+=w;}
        int roll=random.nextInt(total);
        for(Weighted value:values){if(roll<value.weight()) return new CatchRoll(value.species(),value.grade());roll-=value.weight();}
        throw new IllegalStateException();
    }
    public record CatchRoll(FishSpecies species,int grade) {}
}
