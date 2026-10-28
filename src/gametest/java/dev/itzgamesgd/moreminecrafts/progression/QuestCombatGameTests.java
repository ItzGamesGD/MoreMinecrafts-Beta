package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** C3 combat attribution: real deaths through the vanilla kill-credit path, per-player evidence. */
public final class QuestCombatGameTests implements CustomTestMethodInvoker {
    private static ServerLevel level;

    private static <T extends net.minecraft.world.entity.Entity> T spawn(GameTestHelper ctx, EntityType<T> type, BlockPos local) {
        level = ctx.getLevel();
        T entity = type.create(level, EntitySpawnReason.COMMAND);
        BlockPos pos = ctx.absolutePos(local);
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        level.addFreshEntity(entity);
        return entity;
    }

    private static LivingEntity mob(GameTestHelper ctx, EntityType<? extends LivingEntity> type) {
        return spawn(ctx, type, new BlockPos(1, 1, 1));
    }

    private static void melee(GameTestHelper ctx, ServerPlayer attacker, LivingEntity victim, float damage) {
        victim.hurtServer(ctx.getLevel(), attacker.damageSources().playerAttack(attacker), damage);
    }

    private static void kill(GameTestHelper ctx, ServerPlayer attacker, EntityType<? extends LivingEntity> type) {
        melee(ctx, attacker, mob(ctx, type), 100_000F);
    }

    private static int killed(ServerPlayer player, EntityType<?> type) {
        return player.getStats().getValue(Stats.ENTITY_KILLED.get(type));
    }

    private static void setKills(ServerPlayer player, EntityType<?> type, int count) {
        player.getStats().setValue(player, Stats.ENTITY_KILLED.get(type), count);
    }

    private static int progress(ServerPlayer player, String quest) {
        XpQuestTracking.reconcile(player);
        return KnowledgeRuntime.state(player).questProgress(quest);
    }

    private static boolean complete(ServerPlayer player, String quest) {
        XpQuestTracking.reconcile(player);
        return KnowledgeRuntime.state(player).questComplete(quest);
    }

    private static int counter(ServerPlayer player, String key) { return QuestRules.counter(KnowledgeRuntime.state(player), key); }

    static XpQuestCatalogData.Spec caveDwellersSpec() {
        return XpQuestCatalogData.catalog().quests().stream()
                .filter(q -> q.id().equals("xpq.cmb.cave_dwellers")).findFirst().orElseThrow();
    }

    /** Reuses the same stat reconciliation helper for focused cave-roster assertions. */
    static ResearchProgressState reconcileCaveDwellersForTest(ServerPlayer player) {
        XpQuestCatalogData.Spec spec = caveDwellersSpec();
        if (QuestCatalog.byId(spec.id()) == null) QuestCatalog.register(spec.definition());
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState after = XpQuestTracking.reconcile(player, before, List.of(spec));
        if (!after.equals(before)) KnowledgeRuntime.updateState(player, after);
        return KnowledgeRuntime.state(player);
    }

    private static void holding(ServerPlayer player, String item) {
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(item))));
    }

    private static EntityType<? extends LivingEntity> mm(String path) {
        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> type = (EntityType<? extends LivingEntity>)
                BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("moreminecrafts:" + path));
        return type;
    }

    // ---- vanilla kill credit (statistic) attribution -------------------------------------------------------------

    @GameTest
    public void meleeKillCreditsOnlyTheAttacker(GameTestHelper ctx) {
        ServerPlayer attacker = ctx.makeMockServerPlayerInLevel();
        ServerPlayer nearby = ctx.makeMockServerPlayerInLevel();
        kill(ctx, attacker, EntityTypes.ZOMBIE);
        ctx.assertTrue(killed(attacker, EntityTypes.ZOMBIE) == 1 && killed(nearby, EntityTypes.ZOMBIE) == 0,
                "only the killing player's statistic advances");
        ctx.assertTrue(progress(attacker, "xpq.cmb.hostile_100") == 1 && progress(nearby, "xpq.cmb.hostile_100") == 0,
                "hostile-kill progress is per player");
        ctx.succeed();
    }

    @GameTest
    public void projectileOwnerIsCreditedNotTheNearbyPlayerOrAMobShooter(GameTestHelper ctx) {
        ServerPlayer shooter = ctx.makeMockServerPlayerInLevel();
        ServerPlayer nearby = ctx.makeMockServerPlayerInLevel();
        LivingEntity first = mob(ctx, EntityTypes.ZOMBIE);
        Arrow arrow = new Arrow(ctx.getLevel(), shooter, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        first.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().arrow(arrow, shooter), 100_000F);
        ctx.assertTrue(killed(shooter, EntityTypes.ZOMBIE) == 1 && killed(nearby, EntityTypes.ZOMBIE) == 0,
                "an arrow's owner is credited");
        var skeleton = spawn(ctx, EntityTypes.SKELETON, new BlockPos(3, 1, 3));
        LivingEntity second = mob(ctx, EntityTypes.ZOMBIE);
        Arrow mobArrow = new Arrow(ctx.getLevel(), skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        second.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().arrow(mobArrow, skeleton), 100_000F);
        ctx.assertTrue(killed(shooter, EntityTypes.ZOMBIE) == 1 && killed(nearby, EntityTypes.ZOMBIE) == 0,
                "a mob-owned arrow credits no player");
        ctx.succeed();
    }

    @GameTest
    public void indirectOwnedExplosionCreditsItsOwnerAndEnvironmentCreditsNobody(GameTestHelper ctx) {
        ServerPlayer owner = ctx.makeMockServerPlayerInLevel();
        ServerPlayer nearby = ctx.makeMockServerPlayerInLevel();
        var tnt = spawn(ctx, EntityTypes.TNT, new BlockPos(2, 1, 2));
        mob(ctx, EntityTypes.ZOMBIE).hurtServer(ctx.getLevel(),
                ctx.getLevel().damageSources().explosion(tnt, owner), 100_000F);
        ctx.assertTrue(killed(owner, EntityTypes.ZOMBIE) == 1 && killed(nearby, EntityTypes.ZOMBIE) == 0,
                "TNT lit by a player credits that player");
        mob(ctx, EntityTypes.ZOMBIE).hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().lava(), 100_000F);
        mob(ctx, EntityTypes.ZOMBIE).hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().fall(), 100_000F);
        ctx.assertTrue(killed(owner, EntityTypes.ZOMBIE) == 1 && killed(nearby, EntityTypes.ZOMBIE) == 0,
                "environmental deaths without player damage credit nobody");
        ctx.succeed();
    }

    @GameTest
    public void lastPlayerToDamageGetsTheKillNotAnEarlierAttacker(GameTestHelper ctx) {
        ServerPlayer first = ctx.makeMockServerPlayerInLevel();
        ServerPlayer finisher = ctx.makeMockServerPlayerInLevel();
        LivingEntity victim = mob(ctx, EntityTypes.ZOMBIE);
        melee(ctx, first, victim, 3F);
        melee(ctx, finisher, victim, 100_000F);
        ctx.assertTrue(killed(first, EntityTypes.ZOMBIE) == 0 && killed(finisher, EntityTypes.ZOMBIE) == 1,
                "vanilla kill credit goes to the last player attacker");
        ctx.succeed();
    }

    @GameTest
    public void duplicateDeathCallbacksCountOnce(GameTestHelper ctx) {
        ServerPlayer attacker = ctx.makeMockServerPlayerInLevel();
        holding(attacker, "moreminecrafts:iron_dagger");
        LivingEntity victim = mob(ctx, EntityTypes.ZOMBIE);
        DamageSource source = attacker.damageSources().playerAttack(attacker);
        victim.hurtServer(ctx.getLevel(), source, 100_000F);
        victim.die(source);
        ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(victim, source);
        ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(victim, source);
        ctx.assertTrue(killed(attacker, EntityTypes.ZOMBIE) == 1, "vanilla statistic counted once");
        ctx.assertTrue(counter(attacker, "kills_dagger") == 1, "weapon counter counted once across repeated callbacks");
        ctx.succeed();
    }

    @GameTest
    public void mobTypeRestrictionsUseExactEntityTypes(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        kill(ctx, player, EntityTypes.COW);
        ctx.assertTrue(progress(player, "xpq.cmb.hostile_100") == 0, "a cow is not a hostile mob");
        kill(ctx, player, mm("cave_zombie"));
        ctx.assertTrue(progress(player, "xpq.cmb.hostile_100") == 1 && progress(player, "xpq.cmb.bestiary_5") == 1
                && progress(player, "xpq.cmb.vanilla_species_20") == 0,
                "a MoreMinecrafts hostile counts as hostile and bestiary but not as a vanilla species");
        kill(ctx, player, EntityTypes.ZOMBIE);
        ctx.assertTrue(progress(player, "xpq.cmb.vanilla_species_20") == 1 && progress(player, "xpq.cmb.bestiary_5") == 1,
                "the vanilla zombie is its own species, separate from the similar cave zombie");
        kill(ctx, player, EntityTypes.ELDER_GUARDIAN);
        ctx.assertTrue(progress(player, "xpq.cmb.guardians_30") == 0 && complete(player, "xpq.boss.elder_guardian"),
                "an elder guardian is not a guardian");
        kill(ctx, player, EntityTypes.WITHER_SKELETON);
        ctx.assertTrue(progress(player, "xpq.cmb.lesser_wither_10") == 0, "a vanilla wither skeleton is not the lesser one");
        kill(ctx, player, EntityTypes.PIGLIN);
        ctx.assertTrue(progress(player, "xpq.cmb.brutes_3") == 0, "an ordinary piglin is not a piglin brute");
        kill(ctx, player, EntityTypes.PILLAGER);
        kill(ctx, player, mm("bandit"));
        ctx.assertTrue(counter(player, "kills_bandit") == 1 && counter(player, "kills_bandit_archer") == 0,
                "a vanilla pillager is not a bandit, and bandit species are separate");
        ctx.succeed();
    }

    @GameTest
    public void targetedMobQuestsFollowTheirOwnEntityType(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        kill(ctx, player, EntityTypes.BREEZE);
        kill(ctx, player, EntityTypes.GUARDIAN);
        kill(ctx, player, EntityTypes.RAVAGER);
        kill(ctx, player, EntityTypes.PIGLIN_BRUTE);
        kill(ctx, player, mm("lesser_wither_skeleton"));
        ctx.assertTrue(progress(player, "xpq.cmb.breeze_10") == 1 && progress(player, "xpq.cmb.guardians_30") == 1
                && progress(player, "xpq.cmb.ravagers_5") == 1 && progress(player, "xpq.cmb.brutes_3") == 1
                && progress(player, "xpq.cmb.lesser_wither_10") == 1, "each real kill advances exactly its quest");
        ctx.assertTrue(progress(bystander, "xpq.cmb.breeze_10") == 0 && progress(bystander, "xpq.cmb.ravagers_5") == 0,
                "a bystander has no progress");
        ctx.succeed();
    }

    @GameTest
    public void bossKillsAreRealAndTwinBossesNeedBothTypes(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        net.minecraft.world.entity.boss.wither.WitherBoss wither = spawn(ctx, EntityTypes.WITHER, new BlockPos(1, 1, 1));
        wither.setInvulnerableTicks(0);
        melee(ctx, player, wither, 100_000F);
        ctx.assertTrue(complete(player, "xpq.boss.wither") && !complete(bystander, "xpq.boss.wither"),
                "a real wither kill completes the wither quest for its killer only");
        kill(ctx, player, EntityTypes.WARDEN);
        ctx.assertTrue(complete(player, "xpq.boss.warden") && progress(player, "xpq.boss.warden_3") == 1
                && !complete(player, "xpq.boss.warden_3"), "one warden does not satisfy warden_3");
        setKills(player, EntityTypes.WARDEN, 3);
        ctx.assertTrue(complete(player, "xpq.boss.warden_3"), "three wardens complete warden_3");
        ctx.assertTrue(progress(player, "xpq.boss.twin_bosses") == 1 && !complete(player, "xpq.boss.twin_bosses"),
                "the wither alone is half of the twin bosses");
        setKills(player, EntityTypes.WITHER, 5);
        ctx.assertTrue(progress(player, "xpq.boss.twin_bosses") == 1, "repeated withers never substitute for the dragon");
        setKills(player, EntityTypes.ENDER_DRAGON, 1);
        ctx.assertTrue(complete(player, "xpq.boss.twin_bosses"), "dragon plus wither completes the twin bosses");
        ctx.succeed();
    }

    @GameTest
    public void distinctSpeciesAndCountThresholdsAreExact(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        setKills(player, EntityTypes.ZOMBIE, 99);
        ctx.assertTrue(progress(player, "xpq.cmb.hostile_100") == 99 && progress(player, "xpq.cmb.vanilla_species_20") == 1,
                "99 kills of one species is 99 hostiles and one species");
        kill(ctx, player, EntityTypes.ZOMBIE);
        ctx.assertTrue(complete(player, "xpq.cmb.hostile_100"), "the hundredth live kill completes hostile_100");
        ServerPlayer species = ctx.makeMockServerPlayerInLevel();
        List<EntityType<?>> vanilla = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(t -> t.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
                .filter(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals("minecraft")).toList();
        ctx.assertTrue(vanilla.size() >= 21, "enough vanilla monster types exist: " + vanilla.size());
        for (int i = 0; i < 19; i++) setKills(species, vanilla.get(i), 1 + i);
        ctx.assertTrue(!complete(species, "xpq.cmb.vanilla_species_20") && progress(species, "xpq.cmb.vanilla_species_20") == 19,
                "19 species are insufficient");
        setKills(species, vanilla.get(19), 1);
        ctx.assertTrue(complete(species, "xpq.cmb.vanilla_species_20"), "the 20th species completes");
        List<EntityType<?>> custom = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(t -> t.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
                .filter(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals("moreminecrafts")).toList();
        ServerPlayer collector = ctx.makeMockServerPlayerInLevel();
        for (int i = 0; i < 4; i++) setKills(collector, custom.get(i), 7);
        ctx.assertTrue(progress(collector, "xpq.cmb.bestiary_5") == 4 && !complete(collector, "xpq.cmb.bestiary_5"),
                "four custom species are insufficient however often they are killed");
        setKills(collector, custom.get(4), 1);
        ctx.assertTrue(complete(collector, "xpq.cmb.bestiary_5") && !complete(collector, "xpq.cmb.bestiary_10"),
                "the fifth custom species completes bestiary_5 only");
        for (int i = 5; i < 10; i++) setKills(collector, custom.get(i), 1);
        ctx.assertTrue(complete(collector, "xpq.cmb.bestiary_10"), "ten custom species complete bestiary_10");
        ServerPlayer farmer = ctx.makeMockServerPlayerInLevel();
        setKills(farmer, EntityTypes.ZOMBIE, 1_999);
        ctx.assertTrue(!complete(farmer, "xpq.cmb.hostile_2000") && complete(farmer, "xpq.cmb.hostile_500"),
                "1,999 hostiles complete 500 but not 2,000");
        setKills(farmer, EntityTypes.ZOMBIE, 5_000);
        ctx.assertTrue(complete(farmer, "xpq.cmb.hostile_2000") && complete(farmer, "xpq.cmb.hostile_5000"),
                "5,000 hostiles complete both");
        ctx.succeed();
    }

    @GameTest
    public void preexistingVanillaStatisticsAreRecognisedRetroactively(GameTestHelper ctx) {
        ServerPlayer veteran = ctx.makeMockServerPlayerInLevel();
        setKills(veteran, EntityTypes.RAVAGER, 5);
        setKills(veteran, EntityTypes.ELDER_GUARDIAN, 1);
        ctx.assertTrue(complete(veteran, "xpq.cmb.ravagers_5") && complete(veteran, "xpq.boss.elder_guardian"),
                "history already stored in the vanilla statistics counts at the first reconciliation");
        ctx.succeed();
    }

    // ---- weapon, bandit, long-shot and smash kills ----------------------------------------------------------------

    @GameTest
    public void daggerAndGreatswordKillsNeedTheFinalBlowWithThatWeapon(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        holding(player, "moreminecrafts:iron_dagger");
        kill(ctx, player, EntityTypes.ZOMBIE);
        ctx.assertTrue(counter(player, "kills_dagger") == 1 && counter(bystander, "kills_dagger") == 0
                && !complete(player, "xpq.cmb.mm_arms"), "a dagger kill counts for its wielder only");
        holding(player, "moreminecrafts:diamond_greatsword");
        kill(ctx, player, EntityTypes.SKELETON);
        ctx.assertTrue(counter(player, "kills_greatsword") == 1 && complete(player, "xpq.cmb.mm_arms"),
                "dagger plus greatsword completes the new-weapons quest");
        holding(player, "minecraft:iron_sword");
        kill(ctx, player, EntityTypes.ZOMBIE);
        kill(ctx, player, EntityTypes.COW);
        holding(player, "moreminecrafts:iron_dagger");
        kill(ctx, player, EntityTypes.COW);
        ctx.assertTrue(counter(player, "kills_dagger") == 1 && counter(player, "kills_greatsword") == 1,
                "other weapons and non-hostile victims count for neither weapon");
        // Fire after the dagger hit: vanilla keeps the player as kill credit (loot, advancements) but awards the
        // ENTITY_KILLED statistic only to the final blow's causing entity, and the dagger did not deliver it.
        LivingEntity burned = mob(ctx, EntityTypes.ZOMBIE);
        melee(ctx, player, burned, 1F);
        burned.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().onFire(), 100_000F);
        ctx.assertTrue(burned.isDeadOrDying() && burned.getKillCredit() == player,
                "the burned zombie died with the player as vanilla kill credit");
        ctx.assertTrue(killed(player, EntityTypes.ZOMBIE) == 2,
                "the statistic is not awarded for a fire death (final blow was not the player's)");
        ctx.assertTrue(counter(player, "kills_dagger") == 1, "indirect kills never count as dagger kills");
        ServerPlayer finisher = ctx.makeMockServerPlayerInLevel();
        holding(finisher, "moreminecrafts:iron_dagger");
        LivingEntity shared = mob(ctx, EntityTypes.ZOMBIE);
        melee(ctx, player, shared, 1F);
        melee(ctx, finisher, shared, 100_000F);
        ctx.assertTrue(counter(player, "kills_dagger") == 1 && counter(finisher, "kills_dagger") == 1,
                "only the finishing wielder is credited");
        ctx.succeed();
    }

    @GameTest
    public void daggerAndGreatswordHundredsAreUncappedCounters(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        XpQuestTracking.addCounter(player, "kills_dagger", 99);
        XpQuestTracking.addCounter(player, "kills_greatsword", 100);
        ctx.assertTrue(!complete(player, "xpq.cmb.dagger_100") && complete(player, "xpq.cmb.greatsword_100"),
                "99 dagger kills are insufficient, 100 greatsword kills complete");
        holding(player, "moreminecrafts:wood_dagger");
        kill(ctx, player, EntityTypes.ZOMBIE);
        ctx.assertTrue(complete(player, "xpq.cmb.dagger_100"), "the hundredth live dagger kill completes the quest");
        ctx.succeed();
    }

    @GameTest
    public void netherriteBladesNeedBothNetheriteWeapons(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        holding(player, "moreminecrafts:iron_dagger");
        kill(ctx, player, EntityTypes.ZOMBIE);
        holding(player, "moreminecrafts:netherite_dagger");
        kill(ctx, player, EntityTypes.ZOMBIE);
        kill(ctx, player, EntityTypes.SKELETON);
        ctx.assertTrue(progress(player, "xpq.cmb.netherite_blades") == 1 && !complete(player, "xpq.cmb.netherite_blades"),
                "two netherite dagger kills and an iron dagger kill are one of two weapons");
        holding(player, "moreminecrafts:netherite_greatsword");
        kill(ctx, player, EntityTypes.ZOMBIE);
        ctx.assertTrue(complete(player, "xpq.cmb.netherite_blades"), "both netherite weapons complete the quest");
        ctx.succeed();
    }

    @GameTest
    public void banditsNeedTwentyKillsWithEachOfTheThreeSpecies(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        XpQuestTracking.addCounter(player, "kills_bandit_archer", 25);
        ctx.assertTrue(progress(player, "xpq.cmb.bandits_20") == 19 && !complete(player, "xpq.cmb.bandits_20"),
                "25 of one species never completes");
        kill(ctx, player, mm("bandit"));
        ctx.assertTrue(!complete(player, "xpq.cmb.bandits_20"), "two of three species are insufficient");
        kill(ctx, player, mm("bandit_warrior"));
        ctx.assertTrue(complete(player, "xpq.cmb.bandits_20") && counter(player, "kills_bandit") == 1
                && counter(player, "kills_bandit_warrior") == 1, "three species with a total of 20+ complete");
        ServerPlayer small = ctx.makeMockServerPlayerInLevel();
        kill(ctx, small, mm("bandit"));
        kill(ctx, small, mm("bandit_archer"));
        kill(ctx, small, mm("bandit_warrior"));
        ctx.assertTrue(!complete(small, "xpq.cmb.bandits_20") && progress(small, "xpq.cmb.bandits_20") == 3,
                "one of each is three kills, not twenty");
        ctx.succeed();
    }

    private static void shoot(GameTestHelper ctx, ServerPlayer shooter, double distance, ItemStack weapon, EntityType<? extends LivingEntity> type) {
        LivingEntity victim = mob(ctx, type);
        shooter.setPos(victim.getX() - distance, victim.getY(), victim.getZ());
        Arrow arrow = new Arrow(ctx.getLevel(), shooter, new ItemStack(Items.ARROW), weapon);
        victim.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().arrow(arrow, shooter), 100_000F);
    }

    @GameTest
    public void longShotNeedsThirtyBlocksWithABowOrCrossbowFromTheOwner(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        shoot(ctx, player, 29.0D, new ItemStack(Items.BOW), EntityTypes.ZOMBIE);
        ctx.assertTrue(counter(player, "kills_long_shot") == 0, "29 blocks is too close");
        shoot(ctx, player, 30.0D, new ItemStack(Items.BOW), EntityTypes.ZOMBIE);
        shoot(ctx, player, 45.0D, new ItemStack(Items.CROSSBOW), EntityTypes.SKELETON);
        ctx.assertTrue(counter(player, "kills_long_shot") == 2, "30 blocks with a bow and a crossbow both count");
        shoot(ctx, player, 40.0D, new ItemStack(Items.STICK), EntityTypes.ZOMBIE);
        shoot(ctx, player, 40.0D, new ItemStack(Items.BOW), EntityTypes.COW);
        ctx.assertTrue(counter(player, "kills_long_shot") == 2, "an arrow fired from a non-bow item and a non-hostile victim do not count");
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        LivingEntity victim = mob(ctx, EntityTypes.ZOMBIE);
        other.setPos(victim.getX() - 50, victim.getY(), victim.getZ());
        Arrow ownedByPlayer = new Arrow(ctx.getLevel(), player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        player.setPos(victim.getX() - 50, victim.getY(), victim.getZ());
        victim.hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().arrow(ownedByPlayer, player), 100_000F);
        ctx.assertTrue(counter(player, "kills_long_shot") == 3 && counter(other, "kills_long_shot") == 0,
                "only the arrow's owner is credited even when another player is equally far away");
        melee(ctx, player, mob(ctx, EntityTypes.ZOMBIE), 100_000F);
        ctx.assertTrue(counter(player, "kills_long_shot") == 3, "melee never counts as a long shot");
        ctx.succeed();
    }

    @GameTest
    public void maceSmashKillsNeedTheSmashDamageType(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        holding(player, "minecraft:mace");
        kill(ctx, player, EntityTypes.ZOMBIE);
        ctx.assertTrue(counter(player, "kills_mace_smash") == 0, "an ordinary mace hit is not a smash");
        mob(ctx, EntityTypes.ZOMBIE).hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().mace(player), 100_000F);
        ctx.assertTrue(counter(player, "kills_mace_smash") == 1, "a smash kill counts");
        mob(ctx, EntityTypes.COW).hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().mace(player), 100_000F);
        ctx.assertTrue(counter(player, "kills_mace_smash") == 1, "a smash on a non-hostile does not count");
        XpQuestTracking.addCounter(player, "kills_mace_smash", 8);
        ctx.assertTrue(!complete(player, "xpq.cmb.mace_smash_10"), "nine smashes are insufficient");
        mob(ctx, EntityTypes.ZOMBIE).hurtServer(ctx.getLevel(), ctx.getLevel().damageSources().mace(player), 100_000F);
        ctx.assertTrue(complete(player, "xpq.cmb.mace_smash_10"), "the tenth completes");
        ctx.succeed();
    }

    // ---- bestiary audit -------------------------------------------------------------------------------------------

    @GameTest
    public void bestiaryAuditAgainstTheActualRegistry(GameTestHelper ctx) {
        List<Identifier> custom = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(t -> t.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
                .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                .filter(k -> k.getNamespace().equals("moreminecrafts")).sorted().toList();
        System.out.println("QUEST_BESTIARY_AUDIT monster-category MoreMinecrafts types=" + custom.size() + " " + custom);
        // 37 audited roster types plus the unique Tomb Guardian boss (registry-driven bestiary rule).
        ctx.assertTrue(custom.size() == 38, "the actual registry contains all 38 Monster-category MoreMinecrafts types: " + custom.size());
        for (String missing : List.of("small_cave_spider", "swamp_cave_spider"))
            ctx.assertTrue(!BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse("moreminecrafts:" + missing)),
                    "cave_dwellers lists an id that is not registered: " + missing);
        ctx.assertTrue(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("moreminecrafts:cave_bat")).getCategory()
                != net.minecraft.world.entity.MobCategory.MONSTER, "cave_bat is not a Monster-category type");
        ctx.succeed();
    }

    @GameTest
    public void caveDwellersCountsOneKillOfEachExactTypeIncludingAmbientBat(GameTestHelper ctx) {
        ServerPlayer hunter = ctx.makeMockServerPlayerInLevel();
        List<EntityType<? extends LivingEntity>> roster = List.of(
                mm("cave_zombie"), mm("cave_skeleton"), mm("cave_creeper"),
                mm("small_spider"), mm("swamp_spider"), mm("cave_bat"));
        for (EntityType<? extends LivingEntity> type : roster) kill(ctx, hunter, type);

        ctx.assertTrue(mm("cave_bat").getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT,
                "the included cave bat retains its AMBIENT population category");
        for (EntityType<? extends LivingEntity> type : roster)
            ctx.assertTrue(killed(hunter, type) == 1, "the vanilla statistic records the player kill for "
                    + BuiltInRegistries.ENTITY_TYPE.getKey(type));

        ResearchProgressState state = reconcileCaveDwellersForTest(hunter);
        ctx.assertTrue(state.questProgress("xpq.cmb.cave_dwellers") == 6
                        && state.questComplete("xpq.cmb.cave_dwellers"),
                "one credited kill of each exact type, including cave_bat, completes the VERIFY quest in the test fixture");
        ctx.succeed();
    }

    @GameTest
    public void caveDwellersIgnoresDuplicateKillsOtherPlayersAndUnlistedTypes(GameTestHelper ctx) {
        ServerPlayer hunter = ctx.makeMockServerPlayerInLevel();
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        EntityType<? extends LivingEntity> caveZombie = mm("cave_zombie");
        kill(ctx, hunter, caveZombie);
        kill(ctx, hunter, caveZombie);
        kill(ctx, other, mm("small_spider"));
        kill(ctx, hunter, EntityTypes.ZOMBIE);

        ResearchProgressState hunterState = reconcileCaveDwellersForTest(hunter);
        ResearchProgressState otherState = reconcileCaveDwellersForTest(other);
        ctx.assertTrue(hunterState.questProgress("xpq.cmb.cave_dwellers") == 1
                        && !hunterState.questComplete("xpq.cmb.cave_dwellers"),
                "repeated kills of one species and an unlisted vanilla zombie still count as one species");
        ctx.assertTrue(otherState.questProgress("xpq.cmb.cave_dwellers") == 1,
                "the other player receives only their own small_spider kill");
        ctx.assertTrue(killed(hunter, mm("small_spider")) == 0 && killed(other, caveZombie) == 0,
                "a player's statistics do not include another player's kill");
        ctx.succeed();
    }

    @GameTest
    public void caveDwellersRecognizesPersistedVanillaStatisticsRetroactively(GameTestHelper ctx) {
        ServerPlayer veteran = ctx.makeMockServerPlayerInLevel();
        List<EntityType<? extends LivingEntity>> roster = List.of(
                mm("cave_zombie"), mm("cave_skeleton"), mm("cave_creeper"),
                mm("small_spider"), mm("swamp_spider"), mm("cave_bat"));
        for (int i = 0; i < roster.size() - 1; i++) setKills(veteran, roster.get(i), 3 + i);

        ResearchProgressState partial = reconcileCaveDwellersForTest(veteran);
        ctx.assertTrue(partial.questProgress("xpq.cmb.cave_dwellers") == 5
                        && !partial.questComplete("xpq.cmb.cave_dwellers"),
                "existing ENTITY_KILLED totals for five species are recognized without inventing the sixth");
        setKills(veteran, roster.getLast(), 1);
        ResearchProgressState complete = reconcileCaveDwellersForTest(veteran);
        ctx.assertTrue(complete.questProgress("xpq.cmb.cave_dwellers") == 6
                        && complete.questComplete("xpq.cmb.cave_dwellers"),
                "the stored AMBIENT cave-bat statistic completes the sixth species");
        ctx.succeed();
    }

    @GameTest
    public void habitatsUseDisjointTypeGroupsAndPersistPerPlayerStats(GameTestHelper ctx) {
        ServerPlayer hunter = ctx.makeMockServerPlayerInLevel();
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        setKills(hunter, mm("desert_spider"), 2);
        setKills(hunter, mm("sand_slime"), 1);
        setKills(hunter, mm("swamp_spider"), 1);
        setKills(hunter, mm("frozen_zombie"), 1);
        setKills(hunter, mm("fungal_zombie"), 1);
        setKills(hunter, mm("wild_slime"), 1);
        XpQuestTracking.reconcile(hunter);
        ResearchProgressState state = KnowledgeRuntime.state(hunter);
        ctx.assertTrue(state.questProgress("xpq.cmb.habitats_5") == 5
                        && state.questComplete("xpq.cmb.habitats_5"),
                "one qualifying kill per disjoint type group completes all five habitats");
        ctx.assertTrue(state.get(XpQuestTracking.targetStatusKey("xpq.cmb.habitats_5", "moreminecrafts:desert_spider")) == 1
                        && state.get(XpQuestTracking.targetStatusKey("xpq.cmb.habitats_5", "moreminecrafts:sand_slime")) == 1,
                "both candidate kills remain visible without adding duplicate group progress");
        XpQuestTracking.reconcile(bystander);
        ctx.assertTrue(KnowledgeRuntime.state(bystander).questProgress("xpq.cmb.habitats_5") == 0,
                "another player's statistics do not leak into habitat progress");
        ctx.succeed();
    }

    @GameTest
    public void bestiaryCountsDistinctRegisteredMonsterTypesAndExcludesNonMonsterTypes(GameTestHelper ctx) {
        ServerPlayer hunter = ctx.makeMockServerPlayerInLevel();
        List<EntityType<?>> monsters = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
                .filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("moreminecrafts"))
                .sorted(java.util.Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()))
                .toList();
        ctx.assertTrue(monsters.size() == 38, "the live registry supplies all 38 eligible bestiary types");
        for (EntityType<?> type : monsters.subList(0, 25)) setKills(hunter, type, 3);
        setKills(hunter, monsters.getFirst(), 99);
        setKills(hunter, mm("cave_bat"), 5);
        XpQuestTracking.reconcile(hunter);
        ResearchProgressState state = KnowledgeRuntime.state(hunter);
        ctx.assertTrue(state.questProgress("xpq.cmb.bestiary_25") == 25
                        && state.questComplete("xpq.cmb.bestiary_25"),
                "repeated kills do not inflate the 25 distinct-type threshold");
        ctx.assertTrue(state.get(XpQuestTracking.targetStatusKey("xpq.cmb.bestiary_25",
                        BuiltInRegistries.ENTITY_TYPE.getKey(monsters.getFirst()).toString())) == 1
                        && state.get(XpQuestTracking.targetStatusKey("xpq.cmb.bestiary_25", "moreminecrafts:cave_bat")) == 0,
                "all 38 monster statuses are player-specific and cave_bat remains outside bestiary");
        var bestiaryRow = ProgressionScreenData.quests(state, "combat").stream()
                .filter(row -> row.id().equals("xpq.cmb.bestiary_25")).findFirst().orElseThrow();
        ctx.assertTrue(bestiaryRow.groups().size() == 1 && bestiaryRow.groups().getFirst().targets().size() == 38
                        && bestiaryRow.groups().getFirst().targets().stream().filter(ProgressionScreenData.QuestTarget::complete).count() == 25,
                "the server-produced UI row exposes all 38 species and the player's 25 completed targets");
        var habitatRow = ProgressionScreenData.quests(state, "combat").stream()
                .filter(row -> row.id().equals("xpq.cmb.habitats_5")).findFirst().orElseThrow();
        ctx.assertTrue(habitatRow.groups().size() == 5 && habitatRow.groups().stream()
                        .mapToInt(group -> group.targets().size()).sum() == 9,
                "the server-produced habitat checklist exposes five groups and all nine eligible candidate types");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
