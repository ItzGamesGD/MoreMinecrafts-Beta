package dev.itzgamesgd.moreminecrafts.spidercolony;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerLevel;

public final class SpiderColonyEntities {
    // PROVISIONAL collision dimensions, derived from vanilla visual scale; live validation pending.
    public static final float VISUAL_SCALE_PROVISIONAL=2.3F;
    public static final float WIDTH_PROVISIONAL=1.4F*VISUAL_SCALE_PROVISIONAL, HEIGHT_PROVISIONAL=0.9F*VISUAL_SCALE_PROVISIONAL;
    public static final EntityType<MotherSpider> MOTHER;
    public static final EntityType<JuvenileSpider> JUVENILE;
    static {
        var key=ResourceKey.<EntityType<?>>create(Registries.ENTITY_TYPE,ModIds.id("mother_spider"));
        MOTHER=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<MotherSpider>of(MotherSpider::new,MobCategory.MONSTER)
                .sized(WIDTH_PROVISIONAL,HEIGHT_PROVISIONAL).clientTrackingRange(10).notInPeaceful().build(key));
        var childKey=ResourceKey.<EntityType<?>>create(Registries.ENTITY_TYPE,ModIds.id("juvenile_spider"));
        JUVENILE=Registry.register(BuiltInRegistries.ENTITY_TYPE,childKey,EntityType.Builder.<JuvenileSpider>of(JuvenileSpider::new,MobCategory.MONSTER)
                .sized(1.4F,0.9F).clientTrackingRange(8).notInPeaceful().build(childKey));
    }
    private SpiderColonyEntities() {}
    public static ColonyLedger data(ServerLevel level) {return level.getServer().overworld().getDataStorage().computeIfAbsent(ColonyLedger.TYPE);}
    public static void initialize() {
        FabricDefaultAttributeRegistry.register(MOTHER,MotherSpider.attributes());
        FabricDefaultAttributeRegistry.register(JUVENILE,JuvenileSpider.attributes());
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)-> {
            if(entity.level() instanceof ServerLevel level) {
                if(entity instanceof JuvenileSpider) data(level).removeChild(entity.getUUID());
                if(entity instanceof MotherSpider mother) mother.recordDefeat(level);
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)-> {
            if(entity instanceof JuvenileSpider) for(String tag:entity.entityTags()) if(tag.startsWith("mm_spider_owner_")) {
                try { data(level).addChild(java.util.UUID.fromString(tag.substring("mm_spider_owner_".length())),entity.getUUID()); }
                catch(IllegalArgumentException ignored) { }
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)-> {
            if(entity instanceof JuvenileSpider && entity.getRemovalReason()==Entity.RemovalReason.DISCARDED)
                data(level).removeChild(entity.getUUID());
        });
    }
}
