package dev.itzgamesgd.moreminecrafts.progression;

import java.util.*;

/** Registry-independent authoritative tree catalogue and transaction planner. */
public final class EnchantmentTrees {
    public enum Equipment { NONE, SWORD, AXE, PICKAXE, SHOVEL, HOE, MACE, SPEAR, TRIDENT, BOW, CROSSBOW,
        HELMET, CHESTPLATE, LEGGINGS, BOOTS, ELYTRA, SHIELD, FISHING_ROD, GAUNTLET }
    public record Node(String id, int maximum, String prerequisite, int prerequisiteLevel, int[] costs) {
        public int cost(int next) { return costs[next - 1]; }
    }
    public record Quote(boolean allowed, int nextLevel, int cost, int premium, String reason) {}
    private static final Map<Equipment,List<Node>> TREES = new EnumMap<>(Equipment.class);
    private static final int[] ORDINARY = {100,150,200,300,400};
    private static final Set<String> PROTECTION = Set.of("minecraft:protection","minecraft:fire_protection","minecraft:blast_protection","minecraft:projectile_protection");
    private static final Set<String> DAMAGE = Set.of("minecraft:sharpness","minecraft:smite","minecraft:bane_of_arthropods");
    private static final Set<String> MACE_DAMAGE = Set.of("minecraft:density","minecraft:breach");
    static {
        for (Equipment type : Equipment.values()) {
            List<Node> nodes = new ArrayList<>();
            switch (type) {
                case SWORD, GAUNTLET -> { damage(nodes); add(nodes,"sweeping_edge",3); add(nodes,"fire_aspect",2); add(nodes,"knockback",2); add(nodes,"looting",3); }
                case AXE -> { tools(nodes); custom(nodes,"lumberjack",1,"minecraft:efficiency",3,800); damage(nodes); }
                case PICKAXE -> { tools(nodes); custom(nodes,"vein_mining",2,"minecraft:efficiency",3,500,800); custom(nodes,"auto_smelt",1,"",0,800); }
                case SHOVEL -> tools(nodes);
                case HOE -> { tools(nodes); custom(nodes,"auto_replant",1,"minecraft:efficiency",3,800); }
                case MACE -> { add(nodes,"density",5); add(nodes,"breach",4); add(nodes,"wind_burst",3); custom(nodes,"explosive_mace",1,"minecraft:wind_burst",2,1000); }
                case SPEAR -> { damage(nodes); add(nodes,"fire_aspect",2); add(nodes,"knockback",2); add(nodes,"looting",3); add(nodes,"lunge",3); }
                case TRIDENT -> { add(nodes,"impaling",5); add(nodes,"loyalty",3); add(nodes,"riptide",3); add(nodes,"channeling",1); }
                case BOW -> { add(nodes,"power",5); custom(nodes,"multi_shot",2,"minecraft:power",3,500,800); custom(nodes,"laser_arrow",1,"minecraft:power",3,1000); add(nodes,"punch",2); add(nodes,"flame",1); add(nodes,"infinity",1); }
                case CROSSBOW -> { add(nodes,"quick_charge",3); custom(nodes,"crossbow_barrage",1,"minecraft:quick_charge",3,1000); add(nodes,"piercing",4); add(nodes,"multishot",1); }
                case HELMET -> { protection(nodes); add(nodes,"respiration",3); add(nodes,"aqua_affinity",1); }
                case CHESTPLATE -> { protection(nodes); add(nodes,"thorns",3); }
                case LEGGINGS -> { protection(nodes); add(nodes,"swift_sneak",3); }
                case BOOTS -> { protection(nodes); add(nodes,"feather_falling",4); custom(nodes,"feather_boots",1,"minecraft:feather_falling",4,1000); add(nodes,"depth_strider",3); add(nodes,"frost_walker",2); add(nodes,"soul_speed",3); custom(nodes,"jet_boots",3,"",0,800,1000,1200); }
                case ELYTRA -> custom(nodes,"elytra_launch",1,"",0,1000);
                case FISHING_ROD -> { add(nodes,"luck_of_the_sea",3); add(nodes,"lure",3); custom(nodes,"multi_catch",1,"minecraft:lure",3,1000); }
                default -> {}
            }
            if (type != Equipment.NONE) {
                nodes.add(new Node("minecraft:unbreaking",3,"",0,new int[]{200,300,500}));
                nodes.add(new Node("minecraft:mending",1,"minecraft:unbreaking",3,new int[]{1000}));
            }
            TREES.put(type,List.copyOf(nodes));
        }
    }
    private static void add(List<Node> nodes, String id, int max) { nodes.add(new Node("minecraft:"+id,max,"",0,Arrays.copyOf(ORDINARY,max))); }
    private static void custom(List<Node> nodes,String id,int max,String prerequisite,int level,int...costs) { nodes.add(new Node("moreminecrafts:"+id,max,prerequisite,level,costs)); }
    private static void damage(List<Node> nodes) { add(nodes,"sharpness",5);add(nodes,"smite",5);add(nodes,"bane_of_arthropods",5); }
    private static void tools(List<Node> nodes) { add(nodes,"efficiency",5);add(nodes,"fortune",3);add(nodes,"silk_touch",1); }
    private static void protection(List<Node> nodes) { add(nodes,"protection",4);add(nodes,"fire_protection",4);add(nodes,"blast_protection",4);add(nodes,"projectile_protection",4); }
    public static List<Node> tree(Equipment type) { return TREES.getOrDefault(type,List.of()); }
    public static Quote quote(Equipment type, Map<String,Integer> owned, String id) {
        Node node = tree(type).stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
        if (node == null) return new Quote(false,0,0,0,"equipment");
        int current = owned.getOrDefault(id,0);
        if(current<0 || current>=node.maximum())return new Quote(false,current,0,0,"maximum");
        int next = current+1;
        if(owned.getOrDefault(node.prerequisite(),0)<node.prerequisiteLevel())return new Quote(false,next,node.cost(next),0,"prerequisite");
        for(String other:owned.keySet())if(owned.get(other)>0 && conflicts(id,other))return new Quote(false,next,node.cost(next),0,"conflict");
        Set<String> family = PROTECTION.contains(id)?PROTECTION:DAMAGE.contains(id)?DAMAGE:MACE_DAMAGE.contains(id)?MACE_DAMAGE:Set.of();
        int count=(int)family.stream().filter(key->owned.getOrDefault(key,0)>0).count();
        int premium=current==0 && !family.isEmpty()?T1Balance.OVERLAP_PREMIUMS[Math.min(count,3)]:0;
        return new Quote(true,next,node.cost(next)+premium,premium,"");
    }
    public static boolean conflicts(String a,String b) {
        return pair(a,b,"moreminecrafts:multi_shot","moreminecrafts:laser_arrow")
                ||pair(a,b,"moreminecrafts:crossbow_barrage","minecraft:multishot")
                ||pair(a,b,"moreminecrafts:feather_boots","moreminecrafts:jet_boots")
                ||pair(a,b,"minecraft:riptide","minecraft:loyalty")||pair(a,b,"minecraft:riptide","minecraft:channeling")
                ;
    }
    private static boolean pair(String a,String b,String x,String y){return a.equals(x)&&b.equals(y)||a.equals(y)&&b.equals(x);}
    private EnchantmentTrees() {}
}
