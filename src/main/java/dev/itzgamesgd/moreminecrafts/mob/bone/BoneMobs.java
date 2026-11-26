package dev.itzgamesgd.moreminecrafts.mob.bone;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.itzgamesgd.moreminecrafts.boss.DeepSeaPoseidon;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.sky.FloatingJellyfish;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyWyvern;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;

/**
 * Registration of the .bbmodel-animated mobs added on 2026-11-26 (Deep Sea Poseidon, Sky Wyvern, Floating Jellyfish)
 * and the operator-only {@code /mmmob} command used to film them. None has a natural spawn yet.
 */
public final class BoneMobs {
    private BoneMobs() {}

    public static final EntityType<DeepSeaPoseidon> DEEP_SEA_POSEIDON = register("deep_sea_poseidon",
            EntityType.Builder.<DeepSeaPoseidon>of(DeepSeaPoseidon::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.6F).eyeHeight(2.3F).clientTrackingRange(12).notInPeaceful());
    public static final EntityType<SkyWyvern> SKY_WYVERN = register("sky_wyvern",
            EntityType.Builder.<SkyWyvern>of(SkyWyvern::new, MobCategory.MONSTER)
                    .sized(2.2F, 1.4F).eyeHeight(1.2F).clientTrackingRange(12).notInPeaceful());
    public static final EntityType<FloatingJellyfish> FLOATING_JELLYFISH = register("floating_jellyfish",
            EntityType.Builder.<FloatingJellyfish>of(FloatingJellyfish::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.5F).eyeHeight(1.0F).clientTrackingRange(8));

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ModIds.id(path));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(DEEP_SEA_POSEIDON, DeepSeaPoseidon.createAttributes());
        FabricDefaultAttributeRegistry.register(SKY_WYVERN, SkyWyvern.createAttributes());
        FabricDefaultAttributeRegistry.register(FLOATING_JELLYFISH, FloatingJellyfish.createAttributes());
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> dispatcher.register(
                Commands.literal("mmmob")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("scripted")
                                .then(Commands.argument("value", BoolArgumentType.bool()).executes(BoneMobs::scripted)))
                        .then(Commands.literal("skill")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            Set<String> names = new LinkedHashSet<>();
                                            names.addAll(DeepSeaPoseidon.SKILLS);
                                            names.addAll(SkyWyvern.SKILLS);
                                            names.addAll(FloatingJellyfish.SKILLS);
                                            names.forEach(b::suggest);
                                            return b.buildFuture();
                                        })
                                        .executes(BoneMobs::skill)))));
    }

    private static BoneSkillMonster nearest(CommandSourceStack source) {
        return source.getLevel().getEntitiesOfClass(BoneSkillMonster.class, AABB.ofSize(source.getPosition(), 128, 128, 128)).stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(source.getPosition()))).orElse(null);
    }

    private static int scripted(CommandContext<CommandSourceStack> c) {
        BoneSkillMonster mob = nearest(c.getSource());
        if (mob == null) {
            c.getSource().sendFailure(Component.literal("No animated mob within 64 blocks"));
            return 0;
        }
        boolean value = BoolArgumentType.getBool(c, "value");
        mob.setScripted(value);
        c.getSource().sendSuccess(() -> Component.literal(mob.getName().getString() + " scripted=" + value), true);
        return 1;
    }

    private static int skill(CommandContext<CommandSourceStack> c) {
        BoneSkillMonster mob = nearest(c.getSource());
        if (mob == null) {
            c.getSource().sendFailure(Component.literal("No animated mob within 64 blocks"));
            return 0;
        }
        String name = StringArgumentType.getString(c, "name");
        boolean ok = mob.forceSkill(name, c.getSource().getPosition());
        if (!ok) {
            c.getSource().sendFailure(Component.literal(mob.getName().getString() + " has no skill " + name + " (" + mob.skillNames() + ")"));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal(mob.getName().getString() + " " + name), true);
        return 1;
    }
}
