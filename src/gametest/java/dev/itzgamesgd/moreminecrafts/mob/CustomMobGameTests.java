package dev.itzgamesgd.moreminecrafts.mob;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

public final class CustomMobGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void allRosterEntriesRegisterAttributesAndInstantiate(GameTestHelper context) {
        var types = CustomMobCatalog.allTypes();
        context.assertTrue(types.size() == 40, "All forty approved mob types must have spawn definitions");
        Set<String> ids = new HashSet<>();
        for (var type : types) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            context.assertTrue(id != null && id.getNamespace().equals("moreminecrafts"),
                    "Every roster type must have a stable MoreMinecrafts registry id");
            context.assertTrue(ids.add(id.toString()), "Roster registry ids must be unique: " + id);
            context.assertTrue(SpawnPlacements.getPlacementType(type) != null,
                    "Every naturally spawned roster entry must have a placement rule: " + id);
            Mob mob = type.create(context.getLevel(), EntitySpawnReason.COMMAND);
            context.assertTrue(mob != null, "Roster entity must instantiate: " + id);
            DifficultyInstance difficulty = context.getLevel().getCurrentDifficultyAt(mob.blockPosition());
            mob.finalizeSpawn(context.getLevel(), difficulty, EntitySpawnReason.COMMAND, null);
            context.assertTrue(mob.getAttribute(Attributes.MAX_HEALTH) != null
                            && mob.getAttributeValue(Attributes.MAX_HEALTH) > 0,
                    "Roster entity must have registered health attributes: " + id);
            context.assertTrue(mob.getAttribute(Attributes.MOVEMENT_SPEED) != null,
                    "Roster entity must have movement attributes: " + id);
            String path = id.getPath();
            if (path.endsWith("_zombie") || path.endsWith("_skeleton") || path.contains("drowned")
                    || path.equals("lesser_phantom")) {
                context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(EntityTypeTags.UNDEAD),
                        "Undead variants must retain vanilla undead semantics: " + id);
            }
            if (path.contains("spider") || path.endsWith("_bug") || path.equals("giant_bee")) {
                context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(EntityTypeTags.ARTHROPOD),
                        "Arthropod variants must retain vanilla Bane semantics: " + id);
            }
            if (id.getPath().equals("miner_zombie")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_PICKAXE),
                        "Miner Zombie must spawn with its pickaxe");
            } else if (id.getPath().equals("rotten_skeleton") || id.getPath().equals("cave_skeleton")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.STONE_SWORD),
                        "Melee skeleton variants must spawn with stone swords");
            } else if (id.getPath().equals("miner_skeleton")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_PICKAXE),
                        "Miner Skeleton must spawn with its pickaxe");
            } else if (id.getPath().equals("armored_skeleton")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                                && mob.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
                        "Armored Skeleton must spawn with its armor");
            } else if (id.getPath().equals("bandit_archer")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BOW),
                        "Bandit Archer must spawn with a bow");
            } else if (id.getPath().equals("bandit")) {
                context.assertTrue(mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD),
                        "Bandit must use its approved melee sword");
            }
            mob.discard();
        }
        context.assertTrue(ids.size() == 40, "All forty resource identities must remain unique");
        context.assertTrue(SpawnPlacements.getPlacementType(CustomMobCatalog.LESSER_PHANTOM)
                        == net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                "Lesser Phantom must use a sky-capable spawn placement rather than a ground-only placement");
        Mob largeZombie = CustomMobCatalog.LARGE_ZOMBIE.create(context.getLevel(), EntitySpawnReason.COMMAND);
        largeZombie.finalizeSpawn(context.getLevel(), context.getLevel().getCurrentDifficultyAt(largeZombie.blockPosition()),
                EntitySpawnReason.COMMAND, null);
        double healthAfterFirstFinalize = largeZombie.getAttributeValue(Attributes.MAX_HEALTH);
        double speedAfterFirstFinalize = largeZombie.getAttributeValue(Attributes.MOVEMENT_SPEED);
        largeZombie.finalizeSpawn(context.getLevel(), context.getLevel().getCurrentDifficultyAt(largeZombie.blockPosition()),
                EntitySpawnReason.COMMAND, null);
        context.assertTrue(Math.abs(largeZombie.getAttributeValue(Attributes.MAX_HEALTH) - healthAfterFirstFinalize) < 0.001
                        && Math.abs(largeZombie.getAttributeValue(Attributes.MOVEMENT_SPEED) - speedAfterFirstFinalize) < 0.000001,
                "Catalog stat multipliers must not compound when spawn finalization is invoked twice");
        context.assertTrue(Math.abs(largeZombie.getBbWidth() - 0.78F) < 0.02F
                        && Math.abs(largeZombie.getBbHeight() - 2.54F) < 0.02F,
                "Large Zombie dimensions must come from EntityType sizing exactly once");
        Mob caveZombie = CustomMobCatalog.CAVE_ZOMBIE.create(context.getLevel(), EntitySpawnReason.COMMAND);
        context.assertTrue(Math.abs(caveZombie.getBbWidth() - 0.50F) < 0.02F
                        && Math.abs(caveZombie.getBbHeight() - 1.65F) < 0.02F,
                "Cave Zombie dimensions must not be multiplied by an additional SCALE attribute");
        caveZombie.discard();
        Mob giantBee = CustomMobCatalog.GIANT_BEE.create(context.getLevel(), EntitySpawnReason.COMMAND);
        context.assertTrue(CustomMobCatalog.GIANT_BEE.getCategory() == net.minecraft.world.entity.MobCategory.CREATURE,
                "Giant Bee must retain animal-family population metadata");
        context.assertTrue(CustomMobCatalog.CAVE_BAT.getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT,
                "Cave Bat must retain ambient-family population metadata");
        context.assertTrue(CustomMobCatalog.FERAL_WOLF.getCategory() == net.minecraft.world.entity.MobCategory.CREATURE,
                "Feral Wolf must retain animal-family population metadata");
        var noWindAnger = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:no_anger_from_wind_charge"));
        var powderSnowWalkable = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:powder_snow_walkable_mobs"));
        var fallDamageImmune = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:fall_damage_immune"));
        var freezeHurtsExtra = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:freeze_hurts_extra_types"));
        var nonControllingRider = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("minecraft:non_controlling_rider"));
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.SWAMP_ZOMBIE).is(noWindAnger)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.MOUNTAIN_SKELETON).is(noWindAnger)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.SMALL_SPIDER).is(noWindAnger)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.WILD_SLIME).is(noWindAnger),
                "Vanilla wind-charge anger immunity semantics must follow the corresponding base families");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.CAVE_BUG).is(powderSnowWalkable)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.END_BUG).is(powderSnowWalkable),
                "Silverfish/Endermite-family variants must retain powder-snow walking semantics");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.CAVE_BAT).is(fallDamageImmune)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.GIANT_BEE).is(fallDamageImmune)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.LESSER_PHANTOM).is(fallDamageImmune)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.LESSER_MAGMA_CUBE).is(fallDamageImmune),
                "Flying and magma-cube variants must retain vanilla fall-damage immunity semantics");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.LESSER_MAGMA_CUBE).is(freezeHurtsExtra),
                "Lesser Magma Cube must retain vanilla extra freezing vulnerability");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.WILD_SLIME).is(nonControllingRider)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.LESSER_MAGMA_CUBE).is(nonControllingRider),
                "Slime-family variants must retain vanilla non-controlling rider semantics");

        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.SWAMP_ZOMBIE)
                        .is(EntityTypeTags.CAN_BREATHE_UNDER_WATER)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.LESSER_PHANTOM)
                        .is(EntityTypeTags.CAN_BREATHE_UNDER_WATER),
                "Custom undead must inherit vanilla underwater-breathing semantics through #minecraft:undead");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.CAVE_SKELETON)
                        .is(EntityTypeTags.IGNORES_POISON_AND_REGEN)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.DROWNED_SAILOR)
                        .is(EntityTypeTags.WITHER_FRIENDS),
                "Nested undead tags must propagate poison/regen immunity and Wither friendship to custom undead");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.BANDIT)
                        .is(EntityTypeTags.ILLAGER_FRIENDS),
                "Custom illagers must inherit vanilla illager-friend semantics through #minecraft:illager");
        context.assertTrue(!BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.DROWNED_SAILOR)
                        .is(EntityTypeTags.AQUATIC)
                        && !BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.DROWNED_SAILOR)
                        .is(EntityTypeTags.AXOLOTL_HUNT_TARGETS)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.DROWNED_SAILOR)
                        .is(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES),
                "Drowned variants must match vanilla Drowned tags: not aquatic prey, but always hostile to axolotls");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.MOUNTAIN_SKELETON)
                        .is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES),
                "Mountain Skeleton must retain Stray freeze immunity");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.LARGE_SPIDER)
                        .is(EntityTypeTags.DISMOUNTS_UNDERWATER)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.ICE_SPIDER)
                        .is(EntityTypeTags.DISMOUNTS_UNDERWATER)
                        && !BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.SMALL_SPIDER)
                        .is(EntityTypeTags.DISMOUNTS_UNDERWATER),
                "Spider-based variants must retain Spider underwater-dismount semantics without applying them to Cave Spider variants");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.FERAL_WOLF)
                        .is(EntityTypeTags.FOLLOWABLE_FRIENDLY_MOBS)
                        && BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.GIANT_BEE)
                        .is(EntityTypeTags.FOLLOWABLE_FRIENDLY_MOBS),
                "Wolf- and Bee-family variants must retain vanilla adult-sensor followable semantics");
        context.assertTrue(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.WILD_SLIME)
                        .is(EntityTypeTags.IMMUNE_TO_OOZING)
                        && !BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(CustomMobCatalog.LESSER_MAGMA_CUBE)
                        .is(EntityTypeTags.IMMUNE_TO_OOZING),
                "Oozing immunity belongs to Slime, not Magma Cube");
        Slime vanillaSlime = context.spawn(EntityTypes.SLIME, new BlockPos(1, 1, 1));
        Slime wildSlime = context.spawn(CustomMobCatalog.WILD_SLIME, new BlockPos(2, 1, 1));
        vanillaSlime.setSize(4, true);
        wildSlime.setSize(4, true);
        context.assertTrue(Math.abs(wildSlime.getAttributeValue(Attributes.MAX_HEALTH)
                        - vanillaSlime.getAttributeValue(Attributes.MAX_HEALTH) * 0.82) < 0.001,
                "Wild Slime health multiplier must survive vanilla size-derived attribute rewrites");
        context.assertTrue(Math.abs(wildSlime.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        - vanillaSlime.getAttributeValue(Attributes.MOVEMENT_SPEED)) < 0.000001,
                "Wild Slime keeps vanilla size-derived movement speed");
        wildSlime.setSize(2, true);
        vanillaSlime.setSize(2, true);
        context.assertTrue(Math.abs(wildSlime.getAttributeValue(Attributes.MAX_HEALTH)
                        - vanillaSlime.getAttributeValue(Attributes.MAX_HEALTH) * 0.82) < 0.001,
                "Repeated Slime setSize calls must replace, not compound, the approved multiplier");

        Slime mudSlime = context.spawn(CustomMobCatalog.MUD_SLIME, new BlockPos(3, 1, 1));
        Slime sandSlime = context.spawn(CustomMobCatalog.SAND_SLIME, new BlockPos(4, 1, 1));
        vanillaSlime.setSize(4, true);
        mudSlime.setSize(4, true);
        sandSlime.setSize(4, true);
        context.assertTrue(Math.abs(mudSlime.getAttributeValue(Attributes.MAX_HEALTH)
                        - vanillaSlime.getAttributeValue(Attributes.MAX_HEALTH) * 1.15) < 0.001
                        && Math.abs(mudSlime.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        - vanillaSlime.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.78) < 0.000001,
                "Mud Slime size lifecycle must retain its health and speed profile");
        context.assertTrue(Math.abs(sandSlime.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        - vanillaSlime.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.08) < 0.000001,
                "Sand Slime size lifecycle must retain its speed profile");

        MagmaCube vanillaMagma = context.spawn(EntityTypes.MAGMA_CUBE, new BlockPos(5, 1, 1));
        MagmaCube lesserMagma = context.spawn(CustomMobCatalog.LESSER_MAGMA_CUBE, new BlockPos(6, 1, 1));
        vanillaMagma.setSize(4, true);
        lesserMagma.setSize(4, true);
        context.assertTrue(Math.abs(lesserMagma.getAttributeValue(Attributes.MAX_HEALTH)
                        - vanillaMagma.getAttributeValue(Attributes.MAX_HEALTH) * 0.78) < 0.001
                        && Math.abs(lesserMagma.getAttributeValue(Attributes.ATTACK_DAMAGE)
                        - vanillaMagma.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.80) < 0.001,
                "Lesser Magma Cube size lifecycle must retain its health and damage profile");

        FeralWolf feralWolf = (FeralWolf) context.spawn(CustomMobCatalog.FERAL_WOLF, new BlockPos(8, 1, 1));
        var mockPlayer = context.makeMockPlayer(GameType.SURVIVAL);
        feralWolf.setTarget(mockPlayer);
        context.assertTrue(feralWolf.getTarget() == mockPlayer,
                "Feral Wolf target-clearing test must begin with an acquired player target");
        feralWolf.setTame(true, true);
        context.assertTrue(feralWolf.isTame() && feralWolf.getTarget() == null,
                "Taming a Feral Wolf must immediately clear any player target, including direct setTame paths");

        vanillaSlime.discard();
        wildSlime.discard();
        mudSlime.discard();
        sandSlime.discard();
        vanillaMagma.discard();
        lesserMagma.discard();
        feralWolf.discard();
        mockPlayer.discard();
        giantBee.discard();
        largeZombie.discard();
        context.succeed();
    }



    @GameTest
    public void customSlimeSplitLifecycle(GameTestHelper context) {
        Slime splitter = context.spawn(CustomMobCatalog.WILD_SLIME, new BlockPos(2, 1, 2));
        splitter.setSize(4, true);
        var splitSearchBounds = splitter.getBoundingBox().inflate(4.0);
        splitter.setHealth(0.0F);
        splitter.remove(Entity.RemovalReason.KILLED);

        // Conversion children are admitted through the server entity manager at the end of the
        // current tick. Querying them synchronously makes this test order-dependent/flaky.
        context.runAfterDelay(2, () -> {
            var splitChildren = context.getLevel().getEntitiesOfClass(Slime.class, splitSearchBounds,
                    slime -> slime.getType() == CustomMobCatalog.WILD_SLIME);
            context.assertTrue(splitChildren.size() >= 2 && splitChildren.size() <= 4,
                    "A dying custom Slime must split into 2-4 children of the same custom EntityType");
            for (Slime child : splitChildren) {
                context.assertTrue(child.getSize() == 2,
                        "Split custom Slime children must retain vanilla half-size semantics");
                context.assertTrue(Math.abs(child.getAttributeValue(Attributes.MAX_HEALTH)
                                - Math.pow(child.getSize(), 2) * 0.82) < 0.001,
                        "Split custom Slime children must receive the approved health profile");
                child.discard();
            }
            context.succeed();
        });
    }

    @GameTest
    public void banditArcherUsesRangedDamageMultiplier(GameTestHelper context) {
        BanditArcher archer = (BanditArcher) context.spawn(CustomMobCatalog.BANDIT_ARCHER, new BlockPos(1, 1, 1));
        archer.finalizeSpawn(context.getLevel(),
                context.getLevel().getCurrentDifficultyAt(archer.blockPosition()),
                EntitySpawnReason.NATURAL, null);
        context.assertTrue(archer.getMainHandItem().is(Items.BOW),
                "Bandit Archer natural spawn finalization must equip its bow before ranged AI can fire");
        context.assertTrue(archer.canUseNonMeleeWeapon(archer.getMainHandItem()),
                "Bandit Archer must classify its bow as its usable ranged weapon");
        archer.setAggressive(true);
        context.assertTrue(archer.getArmPose() == AbstractIllager.IllagerArmPose.BOW_AND_ARROW,
                "Bandit Archer must expose the bow-and-arrow illager pose while aiming");
        archer.applyRaidBuffs(context.getLevel(), 6, false);
        context.assertTrue(archer.getMainHandItem().is(Items.BOW),
                "Raid buff handling must not replace Bandit Archer's bow with a crossbow after the crossbow goal is removed");
        var target = context.spawn(EntityTypes.COW, new BlockPos(5, 1, 1));
        archer.performRangedAttack(target, 1.0F);
        var arrows = context.getLevel().getEntitiesOfClass(Arrow.class, archer.getBoundingBox().inflate(4.0));
        context.assertTrue(arrows.size() == 1,
                "Bandit Archer must spawn exactly one bow arrow per ranged attack");
        arrows.getFirst().discard();
        archer.discard();
        target.discard();
        context.succeed();
    }

    @GameTest
    public void customEntityTypeMetadataMatchesBaseFamilies(GameTestHelper context) {
        context.assertTrue(CustomMobCatalog.WILD_SLIME.clientTrackingRange() == EntityTypes.SLIME.clientTrackingRange()
                        && CustomMobCatalog.MUD_SLIME.clientTrackingRange() == EntityTypes.SLIME.clientTrackingRange(),
                "Slime-family variants must retain vanilla Slime tracking range");
        context.assertTrue(CustomMobCatalog.FERAL_WOLF.clientTrackingRange() == EntityTypes.WOLF.clientTrackingRange(),
                "Feral Wolf must retain vanilla Wolf tracking range");
        context.assertTrue(CustomMobCatalog.CAVE_BAT.clientTrackingRange() == EntityTypes.BAT.clientTrackingRange(),
                "Cave Bat must retain vanilla Bat tracking range");
        context.assertTrue(CustomMobCatalog.SAND_SLIME.clientTrackingRange() == EntityTypes.SLIME.clientTrackingRange(),
                "All Slime-family variants, including Sand Slime, must retain vanilla Slime tracking range");
        context.assertTrue(CustomMobCatalog.BANDIT.canSpawnFarFromPlayer()
                        && CustomMobCatalog.BANDIT_ARCHER.canSpawnFarFromPlayer()
                        && CustomMobCatalog.WANDERING_RAIDER.canSpawnFarFromPlayer(),
                "Pillager-family variants must retain vanilla far-from-player spawn metadata");
        context.assertTrue(!CustomMobCatalog.SWAMP_ZOMBIE.isAllowedInPeaceful()
                        && !CustomMobCatalog.LESSER_WITHER_SKELETON.isAllowedInPeaceful()
                        && !CustomMobCatalog.WILD_SLIME.isAllowedInPeaceful()
                        && !CustomMobCatalog.BANDIT_ARCHER.isAllowedInPeaceful()
                        && !CustomMobCatalog.LESSER_PHANTOM.isAllowedInPeaceful(),
                "Hostile variants whose vanilla families are excluded from Peaceful must retain that EntityType metadata");
        context.assertTrue(CustomMobCatalog.WILD_PIGLIN.isAllowedInPeaceful() == EntityTypes.PIGLIN.isAllowedInPeaceful(),
                "Wild Piglin must retain vanilla Piglin Peaceful metadata rather than generic monster metadata");
        context.assertTrue(CustomMobCatalog.WILD_SLIME.updateInterval() == EntityTypes.SLIME.updateInterval()
                        && CustomMobCatalog.CAVE_BAT.updateInterval() == EntityTypes.BAT.updateInterval()
                        && CustomMobCatalog.BANDIT_ARCHER.updateInterval() == EntityTypes.PILLAGER.updateInterval(),
                "Representative custom families must retain vanilla update intervals");
        context.assertTrue(CustomMobCatalog.LESSER_WITHER_SKELETON.isBlockDangerous(Blocks.WITHER_ROSE.defaultBlockState())
                        == EntityTypes.WITHER_SKELETON.isBlockDangerous(Blocks.WITHER_ROSE.defaultBlockState()),
                "Lesser Wither Skeleton must retain vanilla Wither Skeleton block immunity metadata");
        context.assertTrue(CustomMobCatalog.MOUNTAIN_SKELETON.isBlockDangerous(Blocks.POWDER_SNOW.defaultBlockState())
                        == EntityTypes.STRAY.isBlockDangerous(Blocks.POWDER_SNOW.defaultBlockState()),
                "Mountain Skeleton must retain vanilla Stray powder-snow block immunity metadata");
        context.succeed();
    }

    @GameTest
    public void spiderPoisonSemantics(GameTestHelper context) {
        SmallCaveSpider smallSpider = (SmallCaveSpider) context.spawn(CustomMobCatalog.SMALL_SPIDER, new BlockPos(1, 1, 1));
        var cleanTarget = context.spawn(EntityTypes.COW, new BlockPos(2, 1, 1));
        context.assertTrue(smallSpider.doHurtTarget(context.getLevel(), cleanTarget),
                "Small Spider test attack must connect");
        context.assertTrue(cleanTarget.getEffect(MobEffects.POISON) == null,
                "Small Spider must never introduce Cave Spider poison");

        var preservedTarget = context.spawn(EntityTypes.COW, new BlockPos(3, 1, 1));
        preservedTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        context.assertTrue(smallSpider.doHurtTarget(context.getLevel(), preservedTarget),
                "Small Spider preservation test attack must connect");
        var preservedPoison = preservedTarget.getEffect(MobEffects.POISON);
        context.assertTrue(preservedPoison != null && preservedPoison.getAmplifier() == 1
                        && preservedPoison.getDuration() == 200,
                "Small Spider must preserve a pre-existing stronger poison exactly");

        SwampCaveSpider swampSpider = (SwampCaveSpider) context.spawn(CustomMobCatalog.SWAMP_SPIDER, new BlockPos(4, 1, 1));
        var weakTarget = context.spawn(EntityTypes.COW, new BlockPos(5, 1, 1));
        context.assertTrue(swampSpider.doHurtTarget(context.getLevel(), weakTarget),
                "Swamp Spider test attack must connect");
        var weakPoison = weakTarget.getEffect(MobEffects.POISON);
        context.assertTrue(weakPoison != null && weakPoison.getAmplifier() == 0
                        && weakPoison.getDuration() == 60,
                "Swamp Spider must replace vanilla Cave Spider poison with exactly 60 ticks of Poison I");

        var strongTarget = context.spawn(EntityTypes.COW, new BlockPos(6, 1, 1));
        strongTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
        context.assertTrue(swampSpider.doHurtTarget(context.getLevel(), strongTarget),
                "Swamp Spider stronger-effect test attack must connect");
        var strongPoison = strongTarget.getEffect(MobEffects.POISON);
        context.assertTrue(strongPoison != null && strongPoison.getAmplifier() == 1
                        && strongPoison.getDuration() == 200,
                "Swamp Spider must not downgrade an existing stronger poison");

        smallSpider.discard();
        swampSpider.discard();
        cleanTarget.discard();
        preservedTarget.discard();
        weakTarget.discard();
        strongTarget.discard();
        context.succeed();
    }

    @GameTest
    public void wildPiglinRetainsEnvironmentalConversion(GameTestHelper context) {
        Piglin wildPiglin = (Piglin) context.spawn(CustomMobCatalog.WILD_PIGLIN, new BlockPos(1, 1, 1));
        wildPiglin.setTimeInOverworld(300);
        context.runAfterDelay(5, () -> {
            context.assertTrue(wildPiglin.isRemoved(),
                    "Wild Piglin must complete normal Piglin overworld zombification");
            var converted = context.getLevel().getEntities(EntityTypes.ZOMBIFIED_PIGLIN,
                    context.getBounds().inflate(2.0), Entity::isAlive);
            context.assertTrue(!converted.isEmpty(),
                    "Wild Piglin conversion must produce a vanilla Zombified Piglin");
            converted.forEach(Entity::discard);
            context.succeed();
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
