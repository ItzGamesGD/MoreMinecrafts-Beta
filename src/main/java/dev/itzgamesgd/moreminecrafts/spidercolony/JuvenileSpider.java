package dev.itzgamesgd.moreminecrafts.spidercolony;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

/** Technical variant of the regular spider, not a naturally spawning species. */
public final class JuvenileSpider extends Spider {
    public JuvenileSpider(EntityType<? extends Spider> type,Level level) {super(type,level);}
    public static AttributeSupplier.Builder attributes() {return Spider.createAttributes()
        .add(Attributes.SCALE,MotherSpider.JUVENILE_SCALE_PROVISIONAL)
        .add(Attributes.MAX_HEALTH,MotherSpider.JUVENILE_HEALTH_PROVISIONAL)
        .add(Attributes.ATTACK_DAMAGE,MotherSpider.JUVENILE_DAMAGE_PROVISIONAL);}
    @Override protected int getBaseExperienceReward(ServerLevel level) {return 2;}
}
