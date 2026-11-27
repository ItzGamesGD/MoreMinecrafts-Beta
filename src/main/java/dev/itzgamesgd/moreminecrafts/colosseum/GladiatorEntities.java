package dev.itzgamesgd.moreminecrafts.colosseum;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
final class GladiatorEntities {
    static final EntityType<ImmortalGladiator> TYPE;
    static {
        var key=ResourceKey.<EntityType<?>>create(Registries.ENTITY_TYPE,ModIds.id("immortal_gladiator"));
        TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ImmortalGladiator>of(ImmortalGladiator::new,MobCategory.MONSTER)
                .sized(1.14F,3.42F).eyeHeight(3.0F).clientTrackingRange(12).noLootTable().build(key));
    }
    static void initialize() { FabricDefaultAttributeRegistry.register(TYPE,ImmortalGladiator.createAttributes()); }
}
