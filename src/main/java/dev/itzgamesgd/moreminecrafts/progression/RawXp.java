package dev.itzgamesgd.moreminecrafts.progression;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Raw-XP balance and exact transactions. The balance is derived from level/progress because
 * totalExperience may be stale after vanilla anvil use; every exact write sets all three fields.
 */
public final class RawXp {
    public static int available(Player player) {
        int level=Math.max(0,player.experienceLevel);
        long base=XpMath.totalForLevel(level);
        return (int)Math.min(Integer.MAX_VALUE,base+Math.round(player.experienceProgress*player.getXpNeededForNextLevel()));
    }
    public static boolean canPay(Player player,int cost){return cost>=0&&(player.hasInfiniteMaterials()||available(player)>=cost);}
    /** Exact deduction; creative-style infinite materials keep their existing free behaviour. */
    public static boolean pay(Player player,int cost){
        if(!canPay(player,cost))return false;
        if(!player.hasInfiniteMaterials())setTotal(player,(long)available(player)-cost);
        return true;
    }
    /** Exact grant; saturates at the largest representable total. */
    public static void grant(Player player,int amount){
        if(amount>0)setTotal(player,(long)available(player)+amount);
    }
    /**
     * Sets level first (so the bar uses the new level's requirement), then the leftover points, then
     * totalExperience. On a server player the vanilla setters reset lastSentExp, so the next tick sends
     * the updated experience packet.
     */
    public static void setTotal(Player player,long total){
        XpMath.Split split=XpMath.split(total);
        if(player instanceof ServerPlayer serverPlayer){
            serverPlayer.setExperienceLevels(split.level());
            serverPlayer.setExperiencePoints(split.points());
        }else{
            player.experienceLevel=split.level();
            player.experienceProgress=split.points()/(float)player.getXpNeededForNextLevel();
        }
        player.totalExperience=(int)Math.min(Integer.MAX_VALUE,XpMath.totalForLevel(split.level())+split.points());
    }
    /** Captured XP fields for transaction rollback. */
    public record Snapshot(int level,float progress,int totalExperience){
        public static Snapshot of(Player player){return new Snapshot(player.experienceLevel,player.experienceProgress,player.totalExperience);}
        public void restore(Player player){
            if(player instanceof ServerPlayer serverPlayer){
                serverPlayer.setExperienceLevels(level);
                serverPlayer.setExperiencePoints(Math.round(progress*serverPlayer.getXpNeededForNextLevel()));
            }
            player.experienceLevel=level;player.experienceProgress=progress;player.totalExperience=totalExperience;
        }
    }
    private RawXp(){}
}
