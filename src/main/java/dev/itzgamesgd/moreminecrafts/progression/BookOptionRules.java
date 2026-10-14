package dev.itzgamesgd.moreminecrafts.progression;
import java.util.Map;
import java.util.function.DoubleSupplier;

public final class BookOptionRules {
    public static final Map<String,Integer> MAXIMUMS = Map.ofEntries(
        Map.entry("minecraft:unbreaking",3),Map.entry("minecraft:mending",1),Map.entry("minecraft:sharpness",5),
        Map.entry("minecraft:smite",5),Map.entry("minecraft:bane_of_arthropods",5),Map.entry("minecraft:sweeping_edge",3),
        Map.entry("minecraft:fire_aspect",2),Map.entry("minecraft:knockback",2),Map.entry("minecraft:power",5),
        Map.entry("minecraft:quick_charge",3),Map.entry("minecraft:piercing",4),Map.entry("minecraft:protection",4),
        Map.entry("minecraft:fire_protection",4),Map.entry("minecraft:projectile_protection",4),Map.entry("minecraft:blast_protection",4));
    public static boolean retained(String id) { return MAXIMUMS.containsKey(id); }
    public static double probability(int level,int maximum) {
        if(maximum<1 || level<1 || level>maximum)throw new IllegalArgumentException("Invalid source book level");
        return maximum==1?1:.2+.6*(level-1)/(maximum-1);
    }
    public static int roll(int maximum,int level,int bookMaximum,DoubleSupplier random) {
        if(maximum<1)throw new IllegalArgumentException("Invalid option maximum");
        double p=probability(level,bookMaximum);
        int result=1;
        for(int i=1;i<maximum;i++)if(random.getAsDouble()<p)result++;
        return result;
    }
    public static int combine(String first,int a,String second,int b) {
        int max=MAXIMUMS.getOrDefault(first,0);
        return first.equals(second)&&a==b&&a>0&&a<max?a+1:0;
    }
    /** Add percentage points to vanilla's measured reduction, retaining the final 90% cap. */
    public static double protectedDamage(double before,double vanillaAfter,double bonus) {
        if(before<=0 || bonus<=0)return vanillaAfter;
        return before*(1-Math.min(.9,Math.max(0,1-vanillaAfter/before)+bonus));
    }
    public static String roman(int n) { return switch(n){case 1->"I";case 2->"II";case 3->"III";case 4->"IV";case 5->"V";default->Integer.toString(n);}; }
    private BookOptionRules() {}
}
