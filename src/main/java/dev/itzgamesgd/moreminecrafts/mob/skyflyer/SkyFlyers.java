package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

/**
 * Registry for the Sky Flyers (group A): Sky Spirit and Storm Hunter. Deliberately separate from the Sky Realm normal
 * mobs (group C, {@code mob.sky}): own IDs, own marker tag, no sky-island {@code spawn_overrides}, no spawn placement
 * and no habitat rule. Natural spawning is not implemented because the group-A spawn location policy is undecided;
 * these mobs exist only through /summon, spawn eggs or commands.
 */
public final class SkyFlyers {
    /** Data-only marker tag for the group-A entities; kept apart from {@code moreminecrafts:sky_normal_mobs}. */
    public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE, ModIds.id("sky_flyers"));

    private static boolean initialized;

    private SkyFlyers() {}

    public static final EntityType<SkySpirit> SKY_SPIRIT = register("sky_spirit", SkySpirit::new,
            MobCategory.CREATURE, 0.35F, 0.6F, 8, false);
    public static final EntityType<StormHunter> STORM_HUNTER = register("storm_hunter", StormHunter::new,
            MobCategory.MONSTER, 0.9F, 0.5F, 8, true);

    private static <T extends Mob> EntityType<T> register(String path, EntityType.EntityFactory<T> factory,
                                                           MobCategory category, float width, float height,
                                                           int trackingRange, boolean hostile) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(path));
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, category)
                .sized(width, height)
                .clientTrackingRange(trackingRange);
        if (hostile) builder.notInPeaceful();
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static List<EntityType<? extends Mob>> allTypes() {
        return List.of(SKY_SPIRIT, STORM_HUNTER);
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        FabricDefaultAttributeRegistry.register(SKY_SPIRIT, SkySpirit.createAttributes());
        FabricDefaultAttributeRegistry.register(STORM_HUNTER, StormHunter.createAttributes());
        MoreMinecrafts.LOGGER.info("Registered {} Sky Flyers (no natural spawn).", allTypes().size());
    }
}
