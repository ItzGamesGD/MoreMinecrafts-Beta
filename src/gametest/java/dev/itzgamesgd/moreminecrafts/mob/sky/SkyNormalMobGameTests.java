package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.mixin.AbstractArrowAccessor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * SKY-MOB P1 runtime verification (Issue #230 Q01-Q87, P1 D1-D6). Probabilistic checks use fixed seeds or large
 * sample counts with bounds many standard deviations wide. Natural spawn / Trial Spawner behaviour is out of scope.
 */
public final class SkyNormalMobGameTests implements CustomTestMethodInvoker {
    private static final BlockPos P = new BlockPos(2, 1, 2);

    private static <T extends Mob> T create(GameTestHelper c, EntityType<T> type) {
        T mob = type.create(c.getLevel(), EntitySpawnReason.COMMAND);
        if (mob == null) throw new AssertionError("cannot create " + type);
        return mob;
    }

    private static <T extends Mob> T finalized(GameTestHelper c, EntityType<T> type, EntitySpawnReason reason) {
        T mob = create(c, type);
        mob.finalizeSpawn(c.getLevel(), c.getLevel().getCurrentDifficultyAt(mob.blockPosition()), reason, null);
        return mob;
    }

    private static String id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    private static boolean is(EntityType<?> type, net.minecraft.tags.TagKey<EntityType<?>> tag) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag);
    }

    private static final List<EntityType<? extends Mob>> ALL = SkyNormalMobs.allTypes();

    // ------------------------------------------------------------------ registration / stats / classification

    @GameTest
    public void registrationStatsDimensionsAndCategory(GameTestHelper c) {
        c.assertTrue(ALL.size() == 5, "five Sky Realm normal mobs");
        Set<String> ids = new HashSet<>();
        for (EntityType<? extends Mob> type : ALL) {
            String id = id(type);
            c.assertTrue(id.startsWith("moreminecrafts:") && ids.add(id), "unique own id " + id);
            c.assertTrue(type.getCategory() == MobCategory.MONSTER, "Q30 shares the hostile cap: " + id);
            c.assertTrue(type.fireImmune() && !type.isAllowedInPeaceful(), "Q36/Q49/Q65/Q74 flags: " + id);
            c.assertTrue(is(type, SkyNormalMobs.TAG), "marker tag: " + id);
            c.assertTrue(!is(type, EntityTypeTags.BURN_IN_DAYLIGHT), "Q36 not sun-sensitive: " + id);
            c.assertTrue(is(type, EntityTypeTags.FALL_DAMAGE_IMMUNE), "Q37: " + id);
        }
        check(c, create(c, SkyNormalMobs.CLOUD_WRAITH), 30, 5, 0.23);
        check(c, create(c, SkyNormalMobs.FALLEN_SENTINEL), 40, 6, 0.23);
        Mob archer = create(c, SkyNormalMobs.SKYGUARD_ARCHER);
        Mob vanillaSkeleton = create(c, EntityTypes.SKELETON);
        c.assertTrue(archer.getAttributeBaseValue(Attributes.MAX_HEALTH) == 30.0
                        && archer.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
                        == vanillaSkeleton.getAttributeBaseValue(Attributes.MOVEMENT_SPEED),
                "Q08: 30 HP, skeleton speed");
        Mob bug = create(c, SkyNormalMobs.CRYSTAL_BUG);
        Mob fish = create(c, EntityTypes.SILVERFISH);
        c.assertTrue(bug.getAttributeBaseValue(Attributes.MAX_HEALTH) == 24.0
                        && bug.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 4.0
                        && bug.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
                        == fish.getAttributeBaseValue(Attributes.MOVEMENT_SPEED),
                "Q13: 24 HP / 4 attack / silverfish speed");
        c.assertTrue(Math.abs(bug.getScale() - 2.5F) < 1.0E-4F, "Q25 scale 2.5, was " + bug.getScale());
        c.assertTrue(Math.abs(bug.getBbWidth() - 2.5F * fish.getBbWidth()) < 0.02F
                        && Math.abs(bug.getBbHeight() - 2.5F * fish.getBbHeight()) < 0.02F,
                "Q25 collision box is 2.5x the silverfish box: " + bug.getBbWidth() + "x" + bug.getBbHeight());
        c.succeed();
    }

    private static void check(GameTestHelper c, Mob mob, double health, double attack, double speed) {
        c.assertTrue(mob.getAttributeBaseValue(Attributes.MAX_HEALTH) == health, id(mob.getType()) + " health");
        c.assertTrue(mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == attack, id(mob.getType()) + " attack");
        c.assertTrue(Math.abs(mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) - speed) < 1.0E-6,
                id(mob.getType()) + " speed");
    }

    @GameTest
    public void lifeformClassificationMatchesQ51ToQ54(GameTestHelper c) {
        for (EntityType<? extends Mob> type : ALL) {
            boolean sentinel = type == SkyNormalMobs.FALLEN_SENTINEL;
            boolean bug = type == SkyNormalMobs.CRYSTAL_BUG;
            String id = id(type);
            c.assertTrue(is(type, EntityTypeTags.UNDEAD) == sentinel, "Q51 undead only for sentinel: " + id);
            c.assertTrue(is(type, EntityTypeTags.INVERTED_HEALING_AND_HARM) == sentinel, "Q52: " + id);
            c.assertTrue(is(type, EntityTypeTags.SENSITIVE_TO_SMITE) == sentinel, "Q53: " + id);
            c.assertTrue(is(type, EntityTypeTags.ARTHROPOD) == bug, "Q54 arthropod only for crystal bug: " + id);
            c.assertTrue(is(type, EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS) == bug, "Q54 bane: " + id);
            // V5 (decision pending): no #zombies / #skeletons membership was added.
            c.assertTrue(!is(type, EntityTypeTags.ZOMBIES) && !is(type, EntityTypeTags.SKELETONS),
                    "V5: no zombies/skeletons tag added: " + id);
            // Consequence of Q51 + vanilla tags: only the undead sentinel breathes under water.
            c.assertTrue(is(type, EntityTypeTags.CAN_BREATHE_UNDER_WATER) == sentinel,
                    "undead-only underwater breathing: " + id);
        }
        // Class-based vanilla target selection is inherited (Q58): the Zombie/Silverfish/Slime/AbstractSkeleton lines.
        c.assertTrue(create(c, SkyNormalMobs.CLOUD_WRAITH) instanceof Zombie
                        && create(c, SkyNormalMobs.FALLEN_SENTINEL) instanceof Zombie
                        && create(c, SkyNormalMobs.SKYGUARD_ARCHER) instanceof AbstractSkeleton
                        && create(c, SkyNormalMobs.CRYSTAL_BUG) instanceof Silverfish
                        && create(c, SkyNormalMobs.CLOUD_SLIME) instanceof Slime,
                "base classes");
        c.succeed();
    }

    // ------------------------------------------------------------------ damage immunities

    @GameTest
    public void damageImmunitiesAndNormalDamage(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        for (EntityType<? extends Mob> type : ALL) {
            String id = id(type);
            expectImmune(c, type, "inFire", s -> s.inFire());
            expectImmune(c, type, "onFire", s -> s.onFire());
            expectImmune(c, type, "lava", s -> s.lava());
            expectImmune(c, type, "hotFloor", s -> s.hotFloor());
            expectImmune(c, type, "freeze", s -> s.freeze());
            expectImmune(c, type, "lightning", s -> s.lightningBolt());
            // Q66 suffocation, Q50 explosion-class and ordinary physical damage still apply.
            expectDamaged(c, type, "inWall", s -> s.inWall());
            expectDamaged(c, type, "generic", s -> s.generic());
            expectDamaged(c, type, "explosion", s -> s.explosion(null, null));

            Mob fall = create(c, type);
            c.assertTrue(!fall.causeFallDamage(20.0D, 1.0F, level.damageSources().fall())
                            && fall.getHealth() == fall.getMaxHealth(),
                    "Q37 fall damage immune: " + id);
            Mob burning = create(c, type);
            burning.igniteForSeconds(10.0F);
            burning.setRemainingFireTicks(200);
            c.assertTrue(!burning.isOnFire(), "Q65 cannot enter the burning state: " + id);
            Mob powder = create(c, type);
            c.assertTrue(!powder.canFreeze(), "Q68 cannot freeze: " + id);
        }
        c.succeed();
    }

    private interface Src { DamageSource of(net.minecraft.world.damagesource.DamageSources sources); }

    private static void expectImmune(GameTestHelper c, EntityType<? extends Mob> type, String label, Src src) {
        Mob mob = create(c, type);
        float before = mob.getHealth();
        mob.hurtServer(c.getLevel(), src.of(c.getLevel().damageSources()), 4.0F);
        c.assertTrue(mob.getHealth() == before && !mob.isOnFire(),
                id(type) + " must be immune to " + label + " (health " + before + " -> " + mob.getHealth() + ")");
    }

    private static void expectDamaged(GameTestHelper c, EntityType<? extends Mob> type, String label, Src src) {
        Mob mob = create(c, type);
        float before = mob.getHealth();
        mob.hurtServer(c.getLevel(), src.of(c.getLevel().damageSources()), 4.0F);
        c.assertTrue(mob.getHealth() < before, id(type) + " must still take " + label + " damage");
    }

    @GameTest
    public void drowningFollowsUndeadClassification(GameTestHelper c) {
        // Q67 + Q51: sentinel (undead) breathes under water; the four general-lifeform mobs have no water-breathing tag.
        c.assertTrue(is(SkyNormalMobs.FALLEN_SENTINEL, EntityTypeTags.CAN_BREATHE_UNDER_WATER), "sentinel");
        for (EntityType<? extends Mob> type : List.of(SkyNormalMobs.CLOUD_WRAITH, SkyNormalMobs.SKYGUARD_ARCHER,
                SkyNormalMobs.CRYSTAL_BUG, SkyNormalMobs.CLOUD_SLIME)) {
            Mob mob = create(c, type);
            float before = mob.getHealth();
            mob.hurtServer(c.getLevel(), c.getLevel().damageSources().drown(), 2.0F);
            c.assertTrue(mob.getHealth() < before, "drowning damage applies to " + id(type));
        }
        c.succeed();
    }

    @GameTest(maxTicks = 120)
    public void sunAndFireStateImmunityHoldsAfterTicking(GameTestHelper c) {
        List<Mob> mobs = new ArrayList<>();
        for (EntityType<? extends Mob> type : ALL) {
            Mob mob = finalized(c, type, EntitySpawnReason.COMMAND);
            mob.setPos(c.absoluteVec(Vec3.atCenterOf(P)));
            mob.setPersistenceRequired();
            c.getLevel().addFreshEntity(mob);
            mobs.add(mob);
        }
        c.runAfterDelay(60, () -> {
            for (Mob mob : mobs) {
                c.assertTrue(mob.isAlive() && !mob.isOnFire() && mob.getHealth() == mob.getMaxHealth(),
                        "Q36/Q65 no sun or fire damage after 60 ticks: " + id(mob.getType()));
                mob.discard();
            }
            c.succeed();
        });
    }

    // ------------------------------------------------------------------ D1 / Q55 spawn finalisation

    @GameTest
    public void noBabiesNoJockeysFixedLoadout(GameTestHelper c) {
        long before = c.getLevel().getEntities(EntityTypes.CHICKEN, c.getBounds().inflate(32), e -> true).size();
        for (EntityType<? extends Mob> type : ALL) {
            for (int i = 0; i < 150; i++) {
                for (EntitySpawnReason reason : List.of(EntitySpawnReason.NATURAL, EntitySpawnReason.COMMAND)) {
                    Mob mob = finalized(c, type, reason);
                    String id = id(type);
                    c.assertTrue(!mob.isBaby() && !mob.isPassenger() && !mob.isVehicle(),
                            "D1 no baby / jockey: " + id);
                    boolean archer = type == SkyNormalMobs.SKYGUARD_ARCHER;
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        ItemStack stack = mob.getItemBySlot(slot);
                        if (archer && slot == EquipmentSlot.MAINHAND) {
                            c.assertTrue(stack.is(Items.BOW) && !stack.isEnchanted(), "Q55 fixed plain bow");
                        } else {
                            c.assertTrue(stack.isEmpty(), "Q55 no random equipment in " + slot + ": " + id);
                        }
                        c.assertTrue(mob.getDropChances().byEquipment(slot) == 0.0F, "Q55 no equipment drops: " + id);
                    }
                    c.assertTrue(!mob.canPickUpLoot(), "Q55 no pickup: " + id);
                    c.assertTrue(!mob.wantsToPickUp(c.getLevel(), new ItemStack(Items.IRON_SWORD)),
                            "Q55 does not want items: " + id);
                }
            }
        }
        long after = c.getLevel().getEntities(EntityTypes.CHICKEN, c.getBounds().inflate(32), e -> true).size();
        c.assertTrue(before == after, "no jockey chickens created");
        c.succeed();
    }

    @GameTest
    public void babyNbtIsRejected(GameTestHelper c) {
        for (EntityType<? extends Zombie> type : List.of(SkyNormalMobs.CLOUD_WRAITH, SkyNormalMobs.FALLEN_SENTINEL)) {
            Zombie z = create(c, type);
            z.setBaby(true);
            c.assertTrue(!z.isBaby(), "D1 setBaby(true) ignored: " + id(type));
            CompoundTag tag = save(z);
            tag.putBoolean("IsBaby", true);
            Zombie reloaded = create(c, type);
            reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, c.getLevel().registryAccess(), tag));
            c.assertTrue(!reloaded.isBaby(), "D1 IsBaby NBT ignored: " + id(type));
        }
        c.succeed();
    }

    // ------------------------------------------------------------------ individual behaviour

    @GameTest
    public void skyguardArcherArrowsDealFortyPercentMore(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        Mob archer = finalized(c, SkyNormalMobs.SKYGUARD_ARCHER, EntitySpawnReason.COMMAND);
        Mob skeleton = finalized(c, EntityTypes.SKELETON, EntitySpawnReason.COMMAND);
        Pig dummy = c.spawn(EntityTypes.PIG, new BlockPos(2, 1, 2));
        dummy.setNoAi(true);
        archer.setPos(c.absoluteVec(Vec3.atCenterOf(new BlockPos(1, 1, 1))));
        skeleton.setPos(c.absoluteVec(Vec3.atCenterOf(new BlockPos(1, 1, 1))));
        level.addFreshEntity(archer);
        level.addFreshEntity(skeleton);
        c.assertTrue(archer.getMainHandItem().is(Items.BOW) && skeleton.getMainHandItem().is(Items.BOW), "bows");
        int shots = 400;
        for (int i = 0; i < shots; i++) {
            ((AbstractSkeleton) archer).performRangedAttack(dummy, 1.0F);
            ((AbstractSkeleton) skeleton).performRangedAttack(dummy, 1.0F);
        }
        c.runAfterDelay(2, () -> {
            double archerSum = 0, skeletonSum = 0;
            int archerCount = 0, skeletonCount = 0;
            for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class,
                    archer.getBoundingBox().inflate(6.0), a -> a.getOwner() == archer || a.getOwner() == skeleton)) {
                double damage = ((AbstractArrowAccessor) arrow).moreminecrafts$getBaseDamage();
                if (arrow.getOwner() == archer) { archerSum += damage; archerCount++; }
                else { skeletonSum += damage; skeletonCount++; }
                arrow.discard();
            }
            c.assertTrue(archerCount >= shots * 0.9 && skeletonCount >= shots * 0.9,
                    "arrows observed archer=" + archerCount + " skeleton=" + skeletonCount);
            double ratio = (archerSum / archerCount) / (skeletonSum / skeletonCount);
            c.assertTrue(Math.abs(ratio - 1.4) < 0.06, "Q08 mean arrow base damage ratio " + ratio + " must be ~1.40");
            archer.discard();
            skeleton.discard();
            dummy.discard();
            c.succeed();
        });
    }

    @GameTest(maxTicks = 300)
    public void skyguardArcherNeverBecomesAStray(GameTestHelper c) {
        c.assertTrue(!Skeleton.class.isAssignableFrom(SkyguardArcher.class),
                "D4: the archer is not a Skeleton, so the powder-snow Stray conversion path does not exist");
        // Positive control: the vanilla Skeleton does carry the conversion entry point.
        Skeleton vanilla = create(c, EntityTypes.SKELETON);
        vanilla.startFreezeConversion(1);
        c.assertTrue(vanilla.isShaking(), "control: vanilla Skeleton can start a Stray conversion");
        AbstractSkeleton archer = c.spawn(SkyNormalMobs.SKYGUARD_ARCHER, P);
        archer.setNoAi(false);
        c.setBlock(P, Blocks.POWDER_SNOW);
        c.runAfterDelay(200, () -> {
            c.assertTrue(archer.isAlive() && archer.getType() == SkyNormalMobs.SKYGUARD_ARCHER
                            && !archer.isShaking() && archer.getHealth() == archer.getMaxHealth(),
                    "D4 + Q68: stays an unharmed Skyguard Archer in powder snow");
            c.assertTrue(c.getLevel().getEntities(EntityTypes.STRAY, c.getBounds().inflate(4), e -> true).isEmpty(),
                    "no Stray appears");
            archer.discard();
            c.succeed();
        });
    }

    @GameTest
    public void fallenSentinelAppliesWeaknessFortyPercentAndNeverHunger(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        FallenSentinel sentinel = (FallenSentinel) finalized(c, SkyNormalMobs.FALLEN_SENTINEL, EntitySpawnReason.COMMAND);
        level.addFreshEntity(sentinel);
        int trials = 3000, hits = 0, weak = 0, hunger = 0;
        for (int i = 0; i < trials; i++) {
            Pig target = create(c, EntityTypes.PIG); // fresh target: no hurt cooldown carry-over
            if (sentinel.doHurtTarget(level, target)) {
                hits++;
                var effect = target.getEffect(MobEffects.WEAKNESS);
                if (effect != null) {
                    c.assertTrue(effect.getAmplifier() == 0 && effect.getDuration() == 60,
                            "Q21: Weakness I for 3 s, was amp=" + effect.getAmplifier() + " ticks=" + effect.getDuration());
                    weak++;
                }
                if (target.hasEffect(MobEffects.HUNGER)) hunger++;
            }
        }
        double rate = weak / (double) hits;
        c.assertTrue(hits > trials * 0.95, "melee attacks connect: " + hits);
        c.assertTrue(hunger == 0, "Q23: Husk hunger replaced");
        c.assertTrue(Math.abs(rate - 0.40) < 0.04, "Q23 weakness rate " + rate + " (n=" + hits + ") must be ~0.40");
        sentinel.discard();
        c.succeed();
    }

    @GameTest(maxTicks = 100)
    public void waterConversionWraithNeverSentinelFollowsHuskToZombie(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        BlockPos pool = new BlockPos(3, 1, 3);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            c.setBlock(pool.offset(dx, 0, dz), Blocks.WATER);
            c.setBlock(pool.offset(dx, 1, dz), Blocks.WATER);
        }
        Zombie wraith = c.spawn(SkyNormalMobs.CLOUD_WRAITH, pool.offset(-1, 0, 0));
        Zombie sentinel = c.spawn(SkyNormalMobs.FALLEN_SENTINEL, pool.offset(1, 0, 0));
        Zombie control = c.spawn(EntityTypes.HUSK, pool.offset(0, 0, 1));
        for (Zombie z : List.of(wraith, sentinel, control)) {
            z.setNoAi(false);
            z.setPersistenceRequired();
            z.setInWaterTime(10_000);
        }
        c.runAfterDelay(4, () -> {
            for (Zombie z : List.of(wraith, sentinel, control)) z.setConversionTime(0);
        });
        c.runAfterDelay(12, () -> {
            c.assertTrue(wraith.isAlive() && !wraith.isUnderWaterConverting(),
                    "Q48: Cloud Wraith never starts a Drowned (or any water) conversion");
            c.assertTrue(level.getEntities(EntityTypes.DROWNED, c.getBounds().inflate(8), e -> true).isEmpty(),
                    "no Drowned appears");
            boolean sentinelConverted = !sentinel.isAlive() || sentinel.isRemoved();
            boolean huskConverted = !control.isAlive() || control.isRemoved();
            c.assertTrue(sentinelConverted == huskConverted,
                    "V1 (reported): sentinel water behaviour equals vanilla Husk; sentinel converted=" + sentinelConverted
                            + ", husk control converted=" + huskConverted);
            var zombies = level.getEntities(EntityTypes.ZOMBIE, c.getBounds().inflate(8), e -> true);
            System.out.println("SKY_MOB_V1 husk_control_converted=" + huskConverted
                    + " sentinel_converted=" + sentinelConverted + " plain_zombies=" + zombies.size());
            zombies.forEach(Entity::discard);
            wraith.discard(); sentinel.discard(); control.discard();
            c.succeed();
        });
    }

    @GameTest(maxTicks = 200)
    public void crystalBugDoesNotSummonOrHide(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        Silverfish vanilla = create(c, EntityTypes.SILVERFISH);
        c.assertTrue(goalNames(vanilla).stream().anyMatch(n -> n.endsWith("SilverfishWakeUpFriendsGoal"))
                        && goalNames(vanilla).stream().anyMatch(n -> n.endsWith("SilverfishMergeWithStoneGoal")),
                "control: vanilla Silverfish has both goals");
        CrystalBug bug = c.spawn(SkyNormalMobs.CRYSTAL_BUG, new BlockPos(2, 1, 2));
        Set<String> names = goalNames(bug);
        c.assertTrue(names.stream().noneMatch(n -> n.contains("WakeUpFriends") || n.contains("MergeWithStone")),
                "D2/D3 goals removed, remaining: " + names);
        c.assertTrue(names.stream().anyMatch(n -> n.contains("MeleeAttackGoal")), "keeps melee AI: " + names);
        BlockPos stone = new BlockPos(4, 1, 2);
        c.setBlock(stone, Blocks.INFESTED_STONE);
        c.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        var mock = c.makeMockPlayer(GameType.SURVIVAL);
        bug.hurtServer(level, level.damageSources().playerAttack(mock), 1.0F);
        long fishBefore = level.getEntities(EntityTypes.SILVERFISH, c.getBounds().inflate(8), e -> true).size();
        c.runAfterDelay(120, () -> {
            c.assertTrue(c.getBlockState(stone).is(Blocks.INFESTED_STONE), "infested block stays untouched");
            c.assertTrue(c.getBlockState(new BlockPos(4, 1, 3)).is(Blocks.STONE), "no stone is infested (D3)");
            long fishAfter = level.getEntities(EntityTypes.SILVERFISH, c.getBounds().inflate(8), e -> true).size();
            c.assertTrue(fishBefore == fishAfter && fishAfter == 0, "D2 no friend summoned: " + fishAfter);
            c.assertTrue(bug.isAlive() && bug.getType() == SkyNormalMobs.CRYSTAL_BUG, "bug still present");
            bug.discard();
            c.succeed();
        });
    }

    private static Set<String> goalNames(Mob mob) {
        Set<String> names = new HashSet<>();
        mob.getGoalSelector().getAvailableGoals().forEach(w -> names.add(w.getGoal().getClass().getName()));
        return names;
    }

    // ------------------------------------------------------------------ cloud slime

    @GameTest
    public void cloudSlimeSizeStatsAndNoBaby(GameTestHelper c) {
        Slime vanilla = create(c, EntityTypes.SLIME);
        int[] sizes = {1, 2, 4};
        double[] health = {4, 12, 32};
        for (int i = 0; i < sizes.length; i++) {
            CloudSlime slime = create(c, SkyNormalMobs.CLOUD_SLIME);
            slime.setSize(sizes[i], true);
            vanilla.setSize(sizes[i], true);
            c.assertTrue(slime.getAttributeValue(Attributes.MAX_HEALTH) == health[i]
                            && slime.getHealth() == (float) health[i],
                    "Q16 health for size " + sizes[i] + " was " + slime.getAttributeValue(Attributes.MAX_HEALTH));
            double expectedAttack = sizes[i] == 1 ? vanilla.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    : vanilla.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5;
            c.assertTrue(Math.abs(slime.getAttributeValue(Attributes.ATTACK_DAMAGE) - expectedAttack) < 1.0E-6,
                    "Q16/D6 attack for size " + sizes[i] + " was " + slime.getAttributeValue(Attributes.ATTACK_DAMAGE));
            c.assertTrue(Math.abs(slime.getAttributeValue(Attributes.MOVEMENT_SPEED)
                    - vanilla.getAttributeValue(Attributes.MOVEMENT_SPEED)) < 1.0E-9, "vanilla speed");
        }
        int[] seen = new int[5];
        for (int i = 0; i < 400; i++) {
            CloudSlime s = finalized(c, SkyNormalMobs.CLOUD_SLIME, EntitySpawnReason.NATURAL);
            c.assertTrue(!s.isBaby(), "D1 no baby slime");
            c.assertTrue(s.getSize() == 1 || s.getSize() == 2 || s.getSize() == 4, "vanilla spawn sizes");
            seen[s.getSize()]++;
            c.assertTrue(s.getMaxHealth() == SkyMobTraits.cloudSlimeHealth(s.getSize()), "spawn health matches size");
        }
        c.assertTrue(seen[1] > 0 && seen[2] > 0 && seen[4] > 0, "all three spawn sizes occur");
        c.succeed();
    }

    @GameTest(maxTicks = 200)
    public void cloudSlimeTinyDealsNoDamageDamagingSizesDo(GameTestHelper c) {
        int[] sizes = {1, 2, 4};
        Player[] players = new Player[3];
        CloudSlime[] slimes = new CloudSlime[3];
        float[] before = new float[3];
        for (int i = 0; i < sizes.length; i++) {
            players[i] = c.makeMockPlayer(GameType.SURVIVAL);
            slimes[i] = c.spawn(SkyNormalMobs.CLOUD_SLIME, P);
            slimes[i].setSize(sizes[i], true);
            slimes[i].setPos(players[i].getX(), players[i].getY(), players[i].getZ());
            slimes[i].setTarget(players[i]);
            before[i] = players[i].getHealth();
        }
        c.runAfterDelay(120, () -> {
            double[] taken = new double[3];
            for (int i = 0; i < 3; i++) { taken[i] = before[i] - players[i].getHealth(); slimes[i].discard(); }
            c.assertTrue(taken[0] == 0.0, "D6 tiny cloud slime deals 0 damage, dealt " + taken[0]);
            c.assertTrue(taken[1] > 0.0 && taken[2] > taken[1],
                    "medium and large deal damage, large more than medium: " + taken[1] + " / " + taken[2]);
            c.succeed();
        });
    }

    @GameTest(maxTicks = 600)
    public void cloudSlimeSplitCountsAndTinyNeverSplits(GameTestHelper c) {
        List<Integer> largeCounts = new ArrayList<>();
        List<Integer> mediumCounts = new ArrayList<>();
        splitTrials(c, 4, 2, 24, largeCounts, () ->
                splitTrials(c, 2, 1, 24, mediumCounts, () -> {
                    CloudSlime tiny = c.spawn(SkyNormalMobs.CLOUD_SLIME, P);
                    tiny.setSize(1, true);
                    var area = tiny.getBoundingBox().inflate(4.0);
                    tiny.setHealth(0.0F);
                    tiny.remove(Entity.RemovalReason.KILLED);
                    c.runAfterDelay(2, () -> {
                        c.assertTrue(c.getLevel().getEntitiesOfClass(Slime.class, area,
                                s -> s.getType() == SkyNormalMobs.CLOUD_SLIME).isEmpty(), "Q17 tiny never splits");
                        c.assertTrue(largeCounts.stream().allMatch(n -> n >= 2 && n <= 4)
                                        && mediumCounts.stream().allMatch(n -> n >= 2 && n <= 4),
                                "Q17 split counts 2-4: large=" + largeCounts + " medium=" + mediumCounts);
                        c.assertTrue(new HashSet<>(largeCounts).size() > 1 && new HashSet<>(mediumCounts).size() > 1,
                                "split counts vary: large=" + largeCounts + " medium=" + mediumCounts);
                        System.out.println("SKY_MOB_SPLIT large=" + largeCounts + " medium=" + mediumCounts);
                        c.succeed();
                    });
                }));
    }

    private static void splitTrials(GameTestHelper c, int parentSize, int childSize, int remaining,
                                    List<Integer> counts, Runnable done) {
        if (remaining == 0) { done.run(); return; }
        CloudSlime parent = c.spawn(SkyNormalMobs.CLOUD_SLIME, P);
        parent.setSize(parentSize, true);
        parent.setNoAi(true);
        var area = parent.getBoundingBox().inflate(4.0);
        parent.setHealth(0.0F);
        parent.remove(Entity.RemovalReason.KILLED);
        // Conversion children are admitted at the end of the tick (same note as the existing slime test).
        c.runAfterDelay(2, () -> {
            var children = c.getLevel().getEntitiesOfClass(Slime.class, area,
                    s -> s.getType() == SkyNormalMobs.CLOUD_SLIME);
            for (Slime child : children) {
                c.assertTrue(child.getSize() == childSize && !child.isBaby(),
                        "children are half-size cloud slimes: " + child.getSize());
                child.discard();
            }
            counts.add(children.size());
            splitTrials(c, parentSize, childSize, remaining - 1, counts, done);
        });
    }

    // ------------------------------------------------------------------ loot

    private static List<ItemStack> roll(GameTestHelper c, String table, Mob victim, Player killer, long seed) {
        ServerLevel level = c.getLevel();
        LootTable lootTable = level.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table)));
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
        return lootTable.getRandomItems(builder.create(LootContextParamSets.ENTITY), seed);
    }

    private static Player killer(GameTestHelper c, int lootingLevel) {
        Player player = c.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        if (lootingLevel > 0) {
            Holder<Enchantment> looting = c.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LOOTING);
            sword.enchant(looting, lootingLevel);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        return player;
    }

    private static int count(List<ItemStack> items, net.minecraft.world.item.Item item) {
        return items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest
    public void lootTablesAndLooting(GameTestHelper c) {
        Player plain = killer(c, 0);
        Player looted = killer(c, 3);
        int n = 4000;

        Mob wraith = create(c, SkyNormalMobs.CLOUD_WRAITH);
        int plainDust = 0, lootedDust = 0, maxPlain = 0;
        for (long seed = 0; seed < n; seed++) {
            int p = count(roll(c, "moreminecrafts:entities/cloud_wraith", wraith, plain, seed), Items.GLOWSTONE_DUST);
            maxPlain = Math.max(maxPlain, p);
            plainDust += p;
            lootedDust += count(roll(c, "moreminecrafts:entities/cloud_wraith", wraith, looted, seed), Items.GLOWSTONE_DUST);
        }
        c.assertTrue(maxPlain == 2 && Math.abs(plainDust / (double) n - 1.0) < 0.06,
                "Q07 glowstone dust 0-2 (mean " + plainDust / (double) n + ", max " + maxPlain + ")");
        c.assertTrue(lootedDust > plainDust * 1.3, "Q64 Looting raises dust: " + plainDust + " -> " + lootedDust);

        Mob bug = create(c, SkyNormalMobs.CRYSTAL_BUG);
        int emeralds = 0, lootedEmeralds = 0, shards = 0, maxShards = 0;
        for (long seed = 0; seed < n; seed++) {
            List<ItemStack> drops = roll(c, "moreminecrafts:entities/crystal_bug", bug, plain, seed);
            emeralds += count(drops, Items.EMERALD);
            int s = count(drops, Items.AMETHYST_SHARD);
            shards += s; maxShards = Math.max(maxShards, s);
            lootedEmeralds += count(roll(c, "moreminecrafts:entities/crystal_bug", bug, looted, seed), Items.EMERALD);
        }
        double rate = emeralds / (double) n;
        c.assertTrue(Math.abs(rate - 0.05) < 0.012, "Q85 emerald base rate ~5%, was " + rate);
        c.assertTrue(lootedEmeralds > emeralds * 1.2, "Q64 Looting III raises emerald rate: " + emeralds
                + " -> " + lootedEmeralds);
        c.assertTrue(maxShards == 2 && Math.abs(shards / (double) n - 1.0) < 0.06, "Q14 amethyst shards 0-2");
        System.out.println("SKY_MOB_LOOT emerald_rate=" + rate + " emerald_rate_looting3=" + lootedEmeralds / (double) n);

        int[] sizes = {1, 2, 4};
        for (int size : sizes) {
            CloudSlime slime = create(c, SkyNormalMobs.CLOUD_SLIME);
            slime.setSize(size, true);
            int charges = 0, maxCharges = 0;
            for (long seed = 0; seed < 600; seed++) {
                int k = count(roll(c, "moreminecrafts:entities/cloud_slime", slime, plain, seed), Items.WIND_CHARGE);
                charges += k; maxCharges = Math.max(maxCharges, k);
            }
            if (size == 1) {
                c.assertTrue(maxCharges == 2 && charges > 0, "Q18 final small slime drops 0-2 wind charges");
            } else {
                c.assertTrue(charges == 0, "Q18 size " + size + " never drops wind charges");
            }
        }

        Mob sentinel = create(c, SkyNormalMobs.FALLEN_SENTINEL);
        Mob archer = create(c, SkyNormalMobs.SKYGUARD_ARCHER);
        int flesh = 0, bones = 0, arrows = 0;
        for (long seed = 0; seed < 600; seed++) {
            flesh += count(roll(c, "moreminecrafts:entities/fallen_sentinel", sentinel, plain, seed), Items.ROTTEN_FLESH);
            List<ItemStack> a = roll(c, "moreminecrafts:entities/skyguard_archer", archer, plain, seed);
            bones += count(a, Items.BONE); arrows += count(a, Items.ARROW);
        }
        c.assertTrue(flesh > 300 && bones > 300 && arrows > 300, "Q22/Q24 vanilla zombie / skeleton drops matched");
        c.succeed();
    }

    // ------------------------------------------------------------------ peaceful, persistence

    @GameTest
    public void peacefulRemovesExistingMobsAndBlocksCreation(GameTestHelper c) {
        var server = c.getLevel().getServer();
        Difficulty previous = server.getWorldData().getDifficulty();
        List<Mob> mobs = new ArrayList<>();
        for (EntityType<? extends Mob> type : ALL) {
            Mob mob = create(c, type);
            mob.setPos(c.absoluteVec(Vec3.atCenterOf(P)));
            c.getLevel().addFreshEntity(mob);
            mobs.add(mob);
        }
        boolean[] removed = new boolean[ALL.size()];
        boolean[] creationBlocked = new boolean[ALL.size()];
        // Difficulty is changed and restored inside one synchronous callback so parallel tests never observe it.
        try {
            server.setDifficulty(Difficulty.PEACEFUL, true);
            for (int i = 0; i < mobs.size(); i++) {
                mobs.get(i).checkDespawn();
                removed[i] = mobs.get(i).isRemoved();
                creationBlocked[i] = ALL.get(i).create(c.getLevel(), EntitySpawnReason.NATURAL) == null;
            }
        } finally {
            server.setDifficulty(previous, true);
        }
        for (int i = 0; i < removed.length; i++) {
            c.assertTrue(removed[i], "Q74 peaceful removes " + id(ALL.get(i)));
            c.assertTrue(creationBlocked[i], "Q74 peaceful blocks creation of " + id(ALL.get(i)));
            mobs.get(i).discard();
        }
        c.succeed();
    }

    @GameTest
    public void nameTagPersistenceAndSaveRestore(GameTestHelper c) {
        for (EntityType<? extends Mob> type : ALL) {
            Mob mob = finalized(c, type, EntitySpawnReason.COMMAND);
            mob.setCustomName(Component.literal("Named " + id(type)));
            mob.setPersistenceRequired(); // what a name tag does
            if (mob instanceof Slime slime) slime.setSize(2, true);
            mob.setHealth(Math.max(1.0F, mob.getMaxHealth() - 1.0F));
            CompoundTag saved = save(mob);
            Mob restored = create(c, type);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, c.getLevel().registryAccess(), saved));
            String id = id(type);
            c.assertTrue(restored.hasCustomName() && restored.getCustomName().getString().equals("Named " + id),
                    "Q84 name survives save/restore: " + id);
            c.assertTrue(restored.isPersistenceRequired(),
                    "Q83/Q84 name-tag persistence survives: " + id);
            c.assertTrue(restored.getHealth() == mob.getHealth(), "health restored: " + id);
            c.assertTrue(restored.getMaxHealth() == mob.getMaxHealth(), "max health restored: " + id);
            c.assertTrue(restored.getType() == type, "type restored: " + id);
            if (restored instanceof Slime s) c.assertTrue(s.getSize() == 2, "slime size restored");
            if (restored instanceof CrystalBug) {
                c.assertTrue(Math.abs(restored.getScale() - 2.5F) < 1.0E-4F, "scale restored");
            }
            if (restored instanceof SkyguardArcher) {
                c.assertTrue(restored.getMainHandItem().is(Items.BOW), "archer bow restored");
            }
        }
        // Un-named mobs follow vanilla hostile distance despawn.
        c.assertTrue(create(c, SkyNormalMobs.CLOUD_WRAITH).removeWhenFarAway(1.0E6), "Q35 vanilla despawn inherited");
        c.succeed();
    }

    private static CompoundTag save(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    // ------------------------------------------------------------------ combat smoke

    @GameTest
    public void allFiveEngageAndHurtAPlayer(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        Player player = c.makeMockPlayer(GameType.SURVIVAL);
        List<Mob> mobs = new ArrayList<>();
        for (EntityType<? extends Mob> type : List.of(SkyNormalMobs.CLOUD_WRAITH, SkyNormalMobs.FALLEN_SENTINEL,
                SkyNormalMobs.CRYSTAL_BUG)) {
            Mob mob = finalized(c, type, EntitySpawnReason.COMMAND);
            mob.setPos(player.getX() + 0.5, player.getY(), player.getZ());
            level.addFreshEntity(mob);
            mob.setTarget(player);
            mobs.add(mob);
        }
        for (Mob mob : mobs) {
            Player victim = c.makeMockPlayer(GameType.SURVIVAL); // fresh player: no hurt cooldown carry-over
            float before = victim.getHealth();
            c.assertTrue(mob.doHurtTarget(level, victim) && victim.getHealth() < before,
                    "melee connects and hurts: " + id(mob.getType()));
        }
        // Players can kill each of them: damage reaches health and death drops loot without error.
        for (EntityType<? extends Mob> type : ALL) {
            Mob victim = finalized(c, type, EntitySpawnReason.COMMAND);
            level.addFreshEntity(victim);
            victim.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
            c.assertTrue(victim.isDeadOrDying(), "killable: " + id(type));
        }
        mobs.forEach(Entity::discard);
        level.getEntities(EntityTypes.ITEM, c.getBounds().inflate(16), e -> true).forEach(Entity::discard);
        c.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
