package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

import dev.itzgamesgd.moreminecrafts.mob.sky.SkyNormalMobs;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Sky Spirit / Storm Hunter runtime checks, driven only through spawn/summon-style creation (no natural spawn exists).
 * Probabilistic checks use fixed seeds and bounds several standard deviations wide.
 */
public final class SkyFlyerGameTests implements CustomTestMethodInvoker {
    private static String id(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
    }

    private static <T extends Mob> T create(GameTestHelper c, EntityType<T> type) {
        T mob = type.create(c.getLevel(), EntitySpawnReason.COMMAND);
        if (mob == null) throw new AssertionError("cannot create " + type);
        return mob;
    }

    private static boolean is(EntityType<?> type, net.minecraft.tags.TagKey<EntityType<?>> tag) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag);
    }

    /** GameTestHelper.spawn() leaves mobs without AI; every behaviour test needs it on. */
    private static <T extends Mob> T live(GameTestHelper c, EntityType<T> type, BlockPos pos) {
        T mob = c.spawn(type, pos);
        mob.setNoAi(false);
        mob.setPersistenceRequired();
        return mob;
    }

    private static boolean windFrom(ServerLevel level, Mob shooter) {
        return !level.getEntitiesOfClass(BreezeWindCharge.class, shooter.getBoundingBox().inflate(40.0D),
                charge -> charge.getOwner() == shooter).isEmpty();
    }

    // ------------------------------------------------------------------ registration

    @GameTest
    public void registrationStatsAndSeparationFromGroupC(GameTestHelper c) {
        c.assertTrue(SkyFlyers.allTypes().size() == 2, "two Sky Flyers in this task");
        c.assertTrue(id(SkyFlyers.SKY_SPIRIT).equals("moreminecrafts:sky_spirit"), "spirit id");
        c.assertTrue(id(SkyFlyers.STORM_HUNTER).equals("moreminecrafts:storm_hunter"), "hunter id");
        for (EntityType<? extends Mob> type : SkyFlyers.allTypes()) {
            c.assertTrue(is(type, SkyFlyers.TAG), "own marker tag: " + id(type));
            c.assertTrue(!is(type, SkyNormalMobs.TAG), "never in the group C tag: " + id(type));
            c.assertTrue(!SkyNormalMobs.allTypes().contains(type), "never in the group C registry: " + id(type));
        }
        c.assertTrue(SkyFlyers.SKY_SPIRIT.getCategory() == MobCategory.CREATURE && SkyFlyers.SKY_SPIRIT.isAllowedInPeaceful(),
                "neutral spirit stays in Peaceful");
        c.assertTrue(SkyFlyers.STORM_HUNTER.getCategory() == MobCategory.MONSTER && !SkyFlyers.STORM_HUNTER.isAllowedInPeaceful(),
                "hostile hunter is removed in Peaceful");

        PathfinderMob spirit = create(c, SkyFlyers.SKY_SPIRIT);
        c.assertTrue(!(spirit instanceof Allay), "own entity, not an Allay subclass (no brain / pickup / duplication)");
        Allay allay = create(c, EntityTypes.ALLAY);
        c.assertTrue(spirit.getAttributeBaseValue(Attributes.MAX_HEALTH) == allay.getAttributeBaseValue(Attributes.MAX_HEALTH)
                        && spirit.getAttributeBaseValue(Attributes.FLYING_SPEED) == allay.getAttributeBaseValue(Attributes.FLYING_SPEED)
                        && spirit.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) == allay.getAttributeBaseValue(Attributes.MOVEMENT_SPEED),
                "PROVISIONAL spirit stats equal the vanilla Allay");
        c.assertTrue(!spirit.canPickUpLoot() && !spirit.wantsToPickUp(c.getLevel(), new ItemStack(Items.DIAMOND)),
                "spirit does not pick up items");

        Mob hunter = create(c, SkyFlyers.STORM_HUNTER);
        c.assertTrue(hunter instanceof Phantom, "hunter is Phantom based");
        Mob phantom = create(c, EntityTypes.PHANTOM);
        phantom.finalizeSpawn(c.getLevel(), c.getLevel().getCurrentDifficultyAt(phantom.blockPosition()),
                EntitySpawnReason.COMMAND, null);
        c.assertTrue(hunter.getAttributeBaseValue(Attributes.MAX_HEALTH) == phantom.getAttributeBaseValue(Attributes.MAX_HEALTH)
                        && hunter.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == phantom.getAttributeBaseValue(Attributes.ATTACK_DAMAGE),
                "PROVISIONAL hunter stats equal the vanilla Phantom (size 0): hunter " + hunter.getAttributeBaseValue(Attributes.MAX_HEALTH)
                        + "/" + hunter.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) + " vs phantom "
                        + phantom.getAttributeBaseValue(Attributes.MAX_HEALTH) + "/" + phantom.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        c.succeed();
    }

    // ------------------------------------------------------------------ Sky Spirit
    //
    // Harness note: GameTestHelper's mock players cannot be AI targets (the in-level ServerPlayer mock reports
    // isCreative() = true, and a plain mock Player is not in the level, so a UUID lookup of it fails). A vanilla Zombie
    // and Phantom do not acquire them either. The behaviour tests therefore use a real in-level Pig as the attacker or
    // target; acquisition of a genuine survival player is checked in SkyFlyerClientCaptureTest.

    /** Static, effectively unkillable in-level dummy (spawned without AI by GameTestHelper). */
    private static net.minecraft.world.entity.animal.pig.Pig dummy(GameTestHelper c, double x, double y, double z) {
        net.minecraft.world.entity.animal.pig.Pig pig = c.spawn(EntityTypes.PIG, new BlockPos(0, 1, 0));
        pig.setPos(c.absoluteVec(new Vec3(x, y, z)));
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0D);
        pig.setHealth(2000.0F);
        return pig;
    }

    private static java.util.Set<String> targetGoalNames(Mob mob) {
        try {
            var field = Mob.class.getDeclaredField("targetSelector");
            field.setAccessible(true);
            var selector = (net.minecraft.world.entity.ai.goal.GoalSelector) field.get(mob);
            java.util.Set<String> names = new java.util.HashSet<>();
            selector.getAvailableGoals().forEach(w -> names.add(w.getGoal().getClass().getSimpleName()));
            return names;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @GameTest
    public void targetSelectionWiring(GameTestHelper c) {
        c.assertTrue(targetGoalNames(create(c, SkyFlyers.SKY_SPIRIT)).equals(java.util.Set.of("HurtByTargetGoal")),
                "spirit only ever targets what hurt it: " + targetGoalNames(create(c, SkyFlyers.SKY_SPIRIT)));
        c.assertTrue(targetGoalNames(create(c, SkyFlyers.STORM_HUNTER)).equals(java.util.Set.of("NearestAttackableTargetGoal")),
                "hunter hunts players on its own: " + targetGoalNames(create(c, SkyFlyers.STORM_HUNTER)));
        c.assertTrue(create(c, SkyFlyers.SKY_SPIRIT).getAttributeValue(Attributes.FOLLOW_RANGE) == SkyFlyerTuning.SPIRIT_DISENGAGE_DISTANCE,
                "spirit follow range matches the disengage distance");
        c.assertTrue(create(c, SkyFlyers.STORM_HUNTER).getAttributeValue(Attributes.FOLLOW_RANGE) == SkyFlyerTuning.HUNTER_TARGET_RANGE,
                "hunter follow range matches the target range");
        c.succeed();
    }

    @GameTest(maxTicks = 200)
    public void spiritIsNeutralAndNeverAttacksUnprovoked(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        var bystander = dummy(c, 6, 2, 4);
        Mob spirit = live(c, SkyFlyers.SKY_SPIRIT, new BlockPos(2, 3, 4));
        c.runAfterDelay(120, () -> {
            c.assertTrue(spirit.isAlive() && spirit.getTarget() == null, "no target without provocation");
            c.assertTrue(!windFrom(level, spirit), "no wind shot without provocation");
            c.assertTrue(bystander.getHealth() == bystander.getMaxHealth(), "bystander untouched");
            spirit.discard();
            c.succeed();
        });
    }

    @GameTest(maxTicks = 300)
    public void spiritAnswersAHitWithWindAndChasesTheAttacker(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        var attacker = dummy(c, 1.5D, 2.0D, 1.5D);
        SkySpirit spirit = (SkySpirit) live(c, SkyFlyers.SKY_SPIRIT, new BlockPos(1, 3, 6));
        // Not at tick 0: HurtByTargetGoal ignores a hit whose timestamp equals its initial timestamp (0).
        c.runAfterDelay(5, () -> spirit.hurtServer(level, level.damageSources().mobAttack(attacker), 1.0F));
        double start = spirit.distanceTo(attacker);
        boolean[] sawWind = {false};
        boolean[] targeted = {false};
        c.succeedWhen(() -> {
            if (spirit.getTarget() == attacker) targeted[0] = true;
            if (windFrom(level, spirit)) sawWind[0] = true;
            c.assertTrue(targeted[0], "the attacker became the target");
            c.assertTrue(sawWind[0], "a wind shot was fired");
            c.assertTrue(attacker.getHealth() < attacker.getMaxHealth(), "the wind shot reached the attacker");
            c.assertTrue(spirit.distanceTo(attacker) < start - 1.0D, "the spirit closed in: " + spirit.distanceTo(attacker) + " vs " + start);
        });
    }

    @GameTest(maxTicks = 400)
    public void spiritEndsTheChaseWhenSightIsLost(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        var attacker = dummy(c, 6.5D, 1.0D, 6.5D);
        SkySpirit spirit = (SkySpirit) live(c, SkyFlyers.SKY_SPIRIT, new BlockPos(1, 3, 1));
        c.runAfterDelay(5, () -> spirit.hurtServer(level, level.damageSources().mobAttack(attacker), 1.0F));
        boolean[] targeted = {false};
        c.runAfterDelay(15, () -> {
            // Wall the attacker in: opaque shell around the two cells it occupies.
            for (int x = 5; x <= 7; x++) for (int y = 0; y <= 3; y++) for (int z = 5; z <= 7; z++) {
                boolean inner = x == 6 && z == 6 && (y == 1 || y == 2);
                if (!inner) c.setBlock(x, y, z, Blocks.STONE);
            }
        });
        c.succeedWhen(() -> {
            if (spirit.getTarget() == attacker) targeted[0] = true;
            c.assertTrue(targeted[0], "the attacker first became the target");
            c.assertTrue(c.getTick() > 30 && spirit.getTarget() == null, "chase ended after sight was lost");
        });
    }

    @GameTest(maxTicks = 200)
    public void spiritEndsTheChaseWhenTheAttackerIsFarAway(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        var attacker = dummy(c, 3.0D, 2.0D, 3.0D);
        SkySpirit spirit = (SkySpirit) live(c, SkyFlyers.SKY_SPIRIT, new BlockPos(2, 3, 3));
        c.runAfterDelay(5, () -> spirit.hurtServer(level, level.damageSources().mobAttack(attacker), 1.0F));
        boolean[] targeted = {false};
        c.runAfterDelay(15, () -> attacker.setPos(c.absoluteVec(new Vec3(3.0D, 2.0D, 3.0D)).add(
                SkyFlyerTuning.SPIRIT_DISENGAGE_DISTANCE + 2.0D, 0.0D, 0.0D)));
        c.succeedWhen(() -> {
            if (spirit.getTarget() == attacker) targeted[0] = true;
            c.assertTrue(targeted[0], "the attacker first became the target");
            c.assertTrue(attacker.isAlive() && level.isLoaded(attacker.blockPosition()), "far attacker is still loaded (harness check)");
            c.assertTrue(c.getTick() > 20 && spirit.getTarget() == null, "chase ended once the attacker was far away");
        });
    }

    // ------------------------------------------------------------------ Storm Hunter

    @GameTest(maxTicks = 1200)
    public void hunterFliesTheFullLoopAndHitsWithTheDive(GameTestHelper c) {
        ServerLevel level = c.getLevel();
        var prey = dummy(c, 4.5D, 1.0D, 4.5D);
        StormHunter hunter = (StormHunter) live(c, SkyFlyers.STORM_HUNTER, new BlockPos(4, 7, 4));
        hunter.setTarget(prey);
        List<HunterPattern.Phase> seen = new ArrayList<>();
        boolean[] wind = {false};
        boolean[] diveHit = {false};
        c.succeedWhen(() -> {
            prey.setHealth(prey.getMaxHealth());
            HunterPattern.Phase phase = hunter.pattern().phase();
            if (seen.isEmpty() || seen.getLast() != phase) seen.add(phase);
            if (windFrom(level, hunter)) wind[0] = true;
            var source = prey.getLastDamageSource();
            if (source != null && source.getEntity() == hunter && source.getDirectEntity() == hunter) diveHit[0] = true;
            c.assertTrue(wind[0], "a wind shot was fired; phases so far " + seen);
            c.assertTrue(diveHit[0], "a dive connected with a melee hit; phases so far " + seen);
            List<HunterPattern.Phase> loop = List.of(HunterPattern.Phase.CIRCLE, HunterPattern.Phase.SHOOT,
                    HunterPattern.Phase.DIVE, HunterPattern.Phase.RISE, HunterPattern.Phase.CIRCLE);
            c.assertTrue(containsInOrder(seen, loop), "circle, shoot, dive, rise, circle in order; saw " + seen);
        });
    }

    private static boolean containsInOrder(List<HunterPattern.Phase> seen, List<HunterPattern.Phase> wanted) {
        int at = 0;
        for (HunterPattern.Phase phase : seen) if (at < wanted.size() && phase == wanted.get(at)) at++;
        return at == wanted.size();
    }

    @GameTest(maxTicks = 400)
    public void hunterReturnsToPatrolWhenItsTargetIsLost(GameTestHelper c) {
        var prey = dummy(c, 4.5D, 1.0D, 4.5D);
        StormHunter hunter = (StormHunter) live(c, SkyFlyers.STORM_HUNTER, new BlockPos(4, 6, 4));
        hunter.setTarget(prey);
        boolean[] engaged = {false};
        c.runAfterDelay(80, prey::discard);
        c.succeedWhen(() -> {
            if (hunter.getTarget() == prey && hunter.pattern().phase() != HunterPattern.Phase.CIRCLE) engaged[0] = true;
            c.assertTrue(engaged[0], "engaged first (left the plain circle)");
            c.assertTrue(c.getTick() > 80 && hunter.getTarget() == null, "target dropped");
            c.assertTrue(hunter.pattern().phase() == HunterPattern.Phase.CIRCLE && hunter.pattern().ticksInPhase() < 5,
                    "pattern restarted from a fresh circle");
        });
    }

    // ------------------------------------------------------------------ drops

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

    private static int feathers(List<ItemStack> items) {
        return items.stream().filter(s -> s.is(dev.itzgamesgd.moreminecrafts.registry.ModItems.SKY_FEATHER))
                .mapToInt(ItemStack::getCount).sum();
    }

    @GameTest
    public void spiritFeatherIsThreePercentSingleAndIgnoresLooting(GameTestHelper c) {
        Mob victim = create(c, SkyFlyers.SKY_SPIRIT);
        Player plain = killer(c, 0);
        Player looting = killer(c, 3);
        int trials = 30_000, hits = 0;
        net.minecraft.util.RandomSource seeds = net.minecraft.util.RandomSource.create(20261114L);
        for (int trial = 0; trial < trials; trial++) {
            long seed = seeds.nextLong();
            List<ItemStack> base = roll(c, "moreminecrafts:entities/sky_spirit", victim, plain, seed);
            List<ItemStack> boosted = roll(c, "moreminecrafts:entities/sky_spirit", victim, looting, seed);
            int n = feathers(base);
            c.assertTrue(n == feathers(boosted), "Looting III changes nothing for seed " + seed);
            c.assertTrue(n <= 1 && base.size() <= 1, "never more than one item, one feather: seed " + seed);
            if (n == 1) hits++;
        }
        c.assertTrue(Math.abs(hits - trials * 0.03) < 150, "3% feather rate, observed " + hits + "/" + trials);
        c.succeed();
    }

    @GameTest
    public void hunterFeatherIsSixPercentThenOneOrTwoAndIgnoresLooting(GameTestHelper c) {
        Mob victim = create(c, SkyFlyers.STORM_HUNTER);
        Player plain = killer(c, 0);
        Player looting = killer(c, 3);
        int trials = 40_000, none = 0, one = 0, two = 0;
        net.minecraft.util.RandomSource seeds = net.minecraft.util.RandomSource.create(20261114L);
        for (int trial = 0; trial < trials; trial++) {
            long seed = seeds.nextLong();
            int n = feathers(roll(c, "moreminecrafts:entities/storm_hunter", victim, plain, seed));
            c.assertTrue(n == feathers(roll(c, "moreminecrafts:entities/storm_hunter", victim, looting, seed)),
                    "Looting III changes nothing for seed " + seed);
            switch (n) {
                case 0 -> none++;
                case 1 -> one++;
                case 2 -> two++;
                default -> throw new AssertionError("impossible feather count " + n);
            }
        }
        c.assertTrue(Math.abs(one - trials * 0.048) < 215, "4.8% one feather, observed " + one);
        c.assertTrue(Math.abs(two - trials * 0.012) < 110, "1.2% two feathers, observed " + two);
        c.assertTrue(none + one + two == trials && Math.abs(none - trials * 0.94) < 300, "94% none, observed " + none);
        c.succeed();
    }

    // ------------------------------------------------------------------ persistence

    @GameTest
    public void bothSurviveSaveAndLoad(GameTestHelper c) {
        for (EntityType<? extends Mob> type : SkyFlyers.allTypes()) {
            Mob mob = create(c, type);
            mob.setHealth(Math.max(1.0F, mob.getMaxHealth() - 3.0F));
            mob.setCustomName(net.minecraft.network.chat.Component.literal("Named " + id(type)));
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mob.registryAccess());
            mob.saveWithoutId(output);
            CompoundTag saved = output.buildResult();
            Mob restored = create(c, type);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, c.getLevel().registryAccess(), saved));
            c.assertTrue(restored.getType() == type && restored.getHealth() == mob.getHealth()
                            && restored.getCustomName() != null, "round trip: " + id(type));
        }
        c.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }

}
