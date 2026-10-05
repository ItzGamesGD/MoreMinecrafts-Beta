package dev.itzgamesgd.moreminecrafts.progression;
import net.minecraft.world.entity.player.Player;

/** Current spendable XP, derived from level/progress; totalExperience may be stale after vanilla anvil use. */
public final class RawXp {
    public static int available(Player player) {
        long level=Math.max(0,player.experienceLevel);
        double base=level<=16?level*level+6*level:level<=31?2.5*level*level-40.5*level+360:4.5*level*level-162.5*level+2220;
        return (int)Math.min(Integer.MAX_VALUE,base+Math.round(player.experienceProgress*player.getXpNeededForNextLevel()));
    }
    public static boolean canPay(Player player,int cost){return cost>=0&&(player.hasInfiniteMaterials()||available(player)>=cost);}
    public static boolean pay(Player player,int cost){if(!canPay(player,cost))return false;if(!player.hasInfiniteMaterials())player.giveExperiencePoints(-cost);return true;}
    private RawXp(){}
}
