package dev.itzgamesgd.moreminecrafts.boss;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

/** Server-authoritative Tomb Guardian combat, loot and encounter-state coverage (no tomb structure needed). */
public final class TombGuardianGameTests {
    private static final AtomicInteger DEFEATED_EVENTS = new AtomicInteger();
    private static final AtomicReference<BlockPos> WATCHED_ANCHOR = new AtomicReference<>();

    static {
        TombGuardianEncounters.DEFEATED.register((level, anchor, killer) -> {
            if (anchor.equals(WATCHED_ANCHOR.get())) DEFEATED_EVENTS.incrementAndGet();
        });
    }

    // Arena helpers.

    private static void floor(GameTestHelper ctx, int radius) {
        for (int x = -radius; x <= radius + 8; x++)
            for (int z = -radius; z <= radius + 8; z++)
                ctx.setBlock(new BlockPos(x, 0, z), Blocks.SMOOTH_SANDSTONE);
    }

    private static TombGuardian freeGuardian(GameTestHelper ctx, BlockPos relative) {
        TombGuardian guardian = ctx.spawn(TombGuardianEntities.TOMB_GUARDIAN, relative);
        guardian.skipAwakening();
        return guardian;
    }

    private static Villager dummy(GameTestHelper ctx, Vec3 relative) {
        Villager villager = ctx.spawnMob(EntityTypes.VILLAGER, relative).withNoFreeWill().spawn();
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0D);
        villager.setHealth(100.0F);
        return villager;
    }

    /** Living guardians bound to {@code anchor}; tests run side by side, so never match another test's guardian. */
    private static List<TombGuardian> guardiansOf(ServerLevel level, BlockPos anchor) {
        return List.copyOf(level.getEntities(TombGuardianEntities.TOMB_GUARDIAN,
                guardian -> guardian.isAlive() && anchor.equals(guardian.tombAnchor())));
    }

    // Spawn and attributes.

    @GameTest
    public void guardianSpawnsWithProvisionalAttributesAndBossRules(GameTestHelper ctx) {
        floor(ctx, 2);
        TombGuardian guardian = ctx.spawn(TombGuardianEntities.TOMB_GUARDIAN, new BlockPos(2, 1, 2));
        ctx.assertTrue(guardian.getMaxHealth() == 200.0F, "provisional 200 HP");
        ctx.assertTrue(guardian.getAttributeValue(Attributes.ATTACK_DAMAGE) == 10.0D, "provisional 10 HP fist");
        ctx.assertTrue(guardian.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.8D, "heavy: 80% knockback resistance");
        ctx.assertTrue(guardian.getAttributeValue(Attributes.FOLLOW_RANGE) == 24.0D, "sight range ~24 blocks");
        ctx.assertTrue(Math.abs(guardian.getBbHeight() - 4.35F) < 0.01F && Math.abs(guardian.getBbWidth() - 1.35F) < 0.01F,
                "~4-5 block tall armored body");
        ctx.assertTrue(guardian.isPersistenceRequired() && !guardian.removeWhenFarAway(10000.0D),
                "a unique boss never despawns by distance");
        ctx.assertTrue(!TombGuardianEntities.TOMB_GUARDIAN.isAllowedInPeaceful(), "hostile boss is not allowed in peaceful");
        ctx.assertTrue(!guardian.isAwakening() && guardian.tombAnchor() == null,
                "a guardian spawned without the encounter API is unbound and not awakening");
        ctx.succeed();
    }

    // Sight-based awareness.

    /** A real survival player (the GameTest mock player always reports creative and is never a combat target). */
    private static ServerPlayer survivalPlayer(GameTestHelper ctx, Vec3 relative) {
        var server = ctx.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tomb-sight"), false);
        ServerPlayer player = new ServerPlayer(server, ctx.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 position = ctx.absoluteVec(relative);
        player.teleportTo(position.x, position.y, position.z);
        return player;
    }

    @GameTest(maxTicks = 100)
    public void guardianAcquiresAVisiblePlayer(GameTestHelper ctx) {
        floor(ctx, 4);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(1, 1, 3));
        ServerPlayer player = survivalPlayer(ctx, new Vec3(6.5D, 1.0D, 3.5D));
        ctx.succeedWhen(() -> {
            ctx.assertTrue(guardian.getTarget() == player, "a visible survival player in range is targeted");
            ctx.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    @GameTest(maxTicks = 100)
    public void guardianDoesNotAcquireAPlayerItCannotSee(GameTestHelper ctx) {
        floor(ctx, 4);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(1, 1, 3));
        // A wall between the guardian and the player: no vibration/Warden sensing, sight only.
        for (int y = 1; y <= 7; y++)
            for (int z = 0; z <= 7; z++)
                ctx.setBlock(new BlockPos(4, y, z), Blocks.STONE);
        ServerPlayer player = survivalPlayer(ctx, new Vec3(6.5D, 1.0D, 3.5D));
        ctx.runAfterDelay(80, () -> {
            ctx.assertTrue(guardian.getTarget() == null, "a hidden player is not targeted, got " + guardian.getTarget());
            ctx.getLevel().getServer().getPlayerList().remove(player);
            ctx.succeed();
        });
    }

    // Attacks.

    @GameTest(maxTicks = 60)
    public void meleeFistIsTelegraphedThenHitsOnlyAtTheStrikeFrame(GameTestHelper ctx) {
        floor(ctx, 2);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager target = dummy(ctx, new Vec3(4.5D, 1.0D, 2.5D));
        ctx.assertTrue(guardian.forceAttack(ctx.getLevel(), TombGuardianCombat.Attack.MELEE, target), "melee starts");
        ctx.runAfterDelay(TombGuardianTuning.MELEE_WINDUP_TICKS - 3, () -> {
            ctx.assertTrue(target.getHealth() == 100.0F, "no damage during the 0.65 s telegraph");
            ctx.assertTrue(guardian.currentAttack() == TombGuardianCombat.Attack.MELEE, "still winding up");
        });
        ctx.runAfterDelay(TombGuardianTuning.MELEE_WINDUP_TICKS + 4, () -> {
            ctx.assertTrue(target.getHealth() == 90.0F, "strike deals the 10 HP fist once, got " + target.getHealth());
            ctx.assertTrue(guardian.currentAttack() == TombGuardianCombat.Attack.NONE, "attack resolved");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void meleeWhiffsWhenTheTargetLeavesReachDuringTheTelegraph(GameTestHelper ctx) {
        floor(ctx, 2);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager target = dummy(ctx, new Vec3(4.5D, 1.0D, 2.5D));
        ctx.assertTrue(guardian.forceAttack(ctx.getLevel(), TombGuardianCombat.Attack.MELEE, target), "melee starts");
        ctx.runAfterDelay(4, () -> target.teleportTo(target.getX() + 6.0D, target.getY(), target.getZ()));
        ctx.runAfterDelay(TombGuardianTuning.MELEE_WINDUP_TICKS + 4, () -> {
            ctx.assertTrue(target.getHealth() == 100.0F, "stepping out of reach during the tell avoids the fist");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void groundShockwaveTravelsOutwardHitsOnceAndCanBeJumpedOrOutranged(GameTestHelper ctx) {
        floor(ctx, 10);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Vec3 origin = guardian.position().subtract(ctx.absoluteVec(Vec3.ZERO));
        Villager near = dummy(ctx, origin.add(3.0D, 0.0D, 0.0D));
        Villager far = dummy(ctx, origin.add(-7.5D, 0.0D, 0.0D));
        Villager airborne = dummy(ctx, origin.add(0.0D, 1.0D, 3.0D));
        airborne.setNoGravity(true);
        ctx.assertTrue(guardian.forceAttack(ctx.getLevel(), TombGuardianCombat.Attack.SLAM, near), "smash starts");
        int windup = TombGuardianTuning.SLAM_WINDUP_TICKS;
        ctx.runAfterDelay(windup - 2, () ->
                ctx.assertTrue(near.getHealth() == 100.0F, "no damage during the 1.3 s smash windup"));
        ctx.runAfterDelay(windup + 2, () ->
                ctx.assertTrue(near.getHealth() == 100.0F, "the wave front has not reached 3 blocks yet: it travels outward"));
        ctx.runAfterDelay(windup + TombGuardianCombat.waveTravelTicks() + 4, () -> {
            ctx.assertTrue(near.getHealth() == 88.0F, "a grounded target inside the radius takes 12 HP exactly once, got " + near.getHealth());
            ctx.assertTrue(far.getHealth() == 100.0F, "outside the 6-block radius is safe");
            ctx.assertTrue(airborne.getHealth() == 100.0F, "a target above the jump clearance hops over the wave");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void stoneEruptionShowsAMagicCircleAtAFixedPointAndSideStepAvoidsIt(GameTestHelper ctx) {
        floor(ctx, 8);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Vec3 point = new Vec3(9.5D, 1.0D, 2.5D);
        Villager dodger = dummy(ctx, point);
        Villager stander = dummy(ctx, point.add(0.6D, 0.0D, 0.6D));
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.STATUE, dodger), "eruption starts");
        Vec3 marked = guardian.statuePoint();
        ctx.assertTrue(marked != null && marked.distanceTo(dodger.position()) < 0.01D, "the site is fixed at the target's position");
        ctx.runAfterDelay(4, () -> {
            List<TombVfxDisplay> circles = level.getEntitiesOfClass(TombVfxDisplay.class, new AABB(BlockPos.containing(marked)).inflate(1.0D),
                    display -> !display.isStatue());
            ctx.assertTrue(circles.size() == 1, "exactly one magic circle marks the site during the warning");
            // Side-step: the marked site must not follow the moving target.
            dodger.teleportTo(dodger.getX(), dodger.getY(), dodger.getZ() + 3.0D);
        });
        ctx.runAfterDelay(8, () -> ctx.assertTrue(guardian.statuePoint() != null && guardian.statuePoint().equals(marked),
                "the eruption is never re-targeted after the warning begins"));
        ctx.runAfterDelay(TombGuardianTuning.STATUE_WARNING_TICKS - 2, () ->
                ctx.assertTrue(stander.getHealth() == 100.0F, "no damage before the statue rises"));
        ctx.runAfterDelay(TombGuardianTuning.STATUE_WARNING_TICKS + 4, () -> {
            ctx.assertTrue(stander.getHealth() == 92.0F, "the statue deals 8 HP inside the circle, got " + stander.getHealth());
            ctx.assertTrue(dodger.getHealth() == 100.0F, "lateral movement out of the circle avoids it");
            List<TombVfxDisplay> statues = level.getEntitiesOfClass(TombVfxDisplay.class, new AABB(BlockPos.containing(marked)).inflate(1.0D),
                    TombVfxDisplay::isStatue);
            ctx.assertTrue(statues.size() == 1 && !statues.get(0).shouldBeSaved(),
                    "one temporary, never-saved inscribed statue rises (no block edits)");
            ctx.assertTrue(level.getBlockState(BlockPos.containing(marked)).isAir(), "the arena floor is not edited");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void enrageStartsAtFortyPercentAndOnlyChangesTempo(GameTestHelper ctx) {
        floor(ctx, 2);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        double baseSpeed = guardian.getAttributeValue(Attributes.MOVEMENT_SPEED);
        guardian.setHealth(80.5F);
        ctx.runAfterDelay(2, () -> {
            ctx.assertTrue(!guardian.isEnraged(), "above 40% (80.5/200) stays in the base phase");
            guardian.setHealth(80.0F);
        });
        ctx.runAfterDelay(4, () -> {
            ctx.assertTrue(guardian.isEnraged(), "at 40% (80/200) the single enrage phase starts");
            double speed = guardian.getAttributeValue(Attributes.MOVEMENT_SPEED);
            ctx.assertTrue(Math.abs(speed - baseSpeed * 1.15D) < 1.0E-6D, "movement +15%, got " + speed);
            ctx.assertTrue(guardian.getAttributeValue(Attributes.ATTACK_DAMAGE) == 10.0D, "damage unchanged");
            ctx.assertTrue(TombGuardianCombat.attackInterval(true) < TombGuardianCombat.attackInterval(false)
                    && TombGuardianCombat.slamWindup(true) < TombGuardianCombat.slamWindup(false)
                    && TombGuardianCombat.slamCooldown(true) < TombGuardianCombat.slamCooldown(false), "faster cadence");
            guardian.setHealth(150.0F);
        });
        ctx.runAfterDelay(6, () -> {
            ctx.assertTrue(guardian.isEnraged(), "enrage is one-way");
            TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
            guardian.saveWithoutId(saved);
            TombGuardian loaded = TombGuardianEntities.TOMB_GUARDIAN.create(ctx.getLevel(), EntitySpawnReason.LOAD);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved.buildResult()));
            ctx.assertTrue(loaded.isEnraged() && Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed * 1.15D) < 1.0E-6D,
                    "enrage and its speed survive save/load");
            loaded.discard();
            ctx.succeed();
        });
    }

    // Loot.

    @GameTest
    public void killLootIsAncientHeartFiftyPercentTimesOneAndNothingElse(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        TombGuardian guardian = ctx.spawn(TombGuardianEntities.TOMB_GUARDIAN, new BlockPos(1, 1, 1));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.parse("moreminecrafts:entities/tomb_guardian")));
        ServerPlayer killer = ctx.makeMockServerPlayerInLevel();
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, guardian)
                .withParameter(LootContextParams.ORIGIN, guardian.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .create(LootContextParamSets.ENTITY);
        int rolls = 4000;
        int hearts = 0;
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> drops = table.getRandomItems(params, (long) i * 7919L + 17L);
            int stacks = 0;
            for (ItemStack stack : drops) {
                ctx.assertTrue(stack.is(ModItems.ANCIENT_HEART) && stack.getCount() == 1,
                        "the only kill drop is exactly one Ancient Heart, got " + stack);
                stacks++;
            }
            ctx.assertTrue(stacks <= 1, "never more than one Ancient Heart per kill");
            hearts += stacks;
        }
        double rate = hearts / (double) rolls;
        ctx.assertTrue(rate > 0.46D && rate < 0.54D, "Ancient Heart drop rate is 50%, measured " + rate);
        guardian.discard();
        ctx.succeed();
    }

    @GameTest
    public void ancientCityChestKeepsItsThreePercentAncientHeartRoute(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.withDefaultNamespace("chests/ancient_city")));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, ctx.absoluteVec(new Vec3(1.0D, 1.0D, 1.0D)))
                .create(LootContextParamSets.CHEST);
        int rolls = 6000;
        int hearts = 0;
        for (int i = 0; i < rolls; i++)
            for (ItemStack stack : table.getRandomItems(params, (long) i * 104729L + 3L))
                if (stack.is(ModItems.ANCIENT_HEART)) hearts += stack.getCount();
        double rate = hearts / (double) rolls;
        ctx.assertTrue(rate > 0.018D && rate < 0.042D, "Ancient City Ancient Heart stays at 3% x1, measured " + rate);
        ctx.succeed();
    }

    // Encounter state (option B).

    @GameTest
    public void royalChestInteractionIsIdempotentAndNeverDuplicatesTheGuardian(GameTestHelper ctx) {
        floor(ctx, 4);
        ServerLevel level = ctx.getLevel();
        BlockPos anchor = ctx.absolutePos(new BlockPos(1, 1, 1));
        BlockPos spawn = ctx.absolutePos(new BlockPos(4, 1, 4));
        var first = TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null);
        var second = TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null);
        ctx.assertTrue(first == TombGuardianEncounters.StartResult.STARTED, "first interaction starts the encounter, got " + first);
        ctx.assertTrue(second == TombGuardianEncounters.StartResult.ALREADY_ACTIVE, "a repeat interaction is refused, got " + second);
        List<TombGuardian> guardians = guardiansOf(level, anchor);
        ctx.assertTrue(guardians.size() == 1, "exactly one guardian exists, found " + guardians.size());
        TombGuardian guardian = guardians.get(0);
        ctx.assertTrue(guardian.isAwakening() && anchor.equals(guardian.tombAnchor()), "the bound guardian is awakening");
        ctx.assertTrue(TombGuardianEncounters.isRoyalChestSealed(level, anchor), "the royal chest stays sealed during the fight");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ctx.assertTrue(!guardian.hurtServer(level, level.damageSources().playerAttack(player), 50.0F)
                && guardian.getHealth() == guardian.getMaxHealth(), "the guardian cannot be hurt while it emerges");
        guardian.discard();
        ctx.succeed();
    }

    @GameTest
    public void guardianLeavingWithoutDyingAllowsARetry(GameTestHelper ctx) {
        floor(ctx, 4);
        ServerLevel level = ctx.getLevel();
        BlockPos anchor = ctx.absolutePos(new BlockPos(1, 1, 2));
        BlockPos spawn = ctx.absolutePos(new BlockPos(4, 1, 4));
        TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null);
        TombGuardian first = guardiansOf(level, anchor).get(0);
        UUID firstId = first.getUUID();
        first.discard(); // despawn / removal without death
        ctx.assertTrue(TombGuardianEncounters.data(level).active(TombGuardianEncounters.tombKey(anchor)) == null,
                "a non-death removal releases the encounter");
        ctx.assertTrue(TombGuardianEncounters.isRoyalChestSealed(level, anchor), "the chest stays sealed: the guardian was not killed");
        var retry = TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null);
        ctx.assertTrue(retry == TombGuardianEncounters.StartResult.STARTED, "the next interaction starts a new encounter, got " + retry);
        List<TombGuardian> guardians = guardiansOf(level, anchor);
        ctx.assertTrue(guardians.size() == 1 && !guardians.get(0).getUUID().equals(firstId), "one fresh guardian");

        TombGuardianEncounters.endEncounter(level, anchor);
        ctx.assertTrue(guardiansOf(level, anchor).isEmpty()
                        && TombGuardianEncounters.data(level).active(TombGuardianEncounters.tombKey(anchor)) == null,
                "ending an undefeated encounter removes the guardian and allows another retry");
        ctx.assertTrue(TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null).spawned(), "retry after end");
        guardiansOf(level, anchor).forEach(Entity::discard);
        ctx.succeed();
    }

    @GameTest
    public void interruptedEncounterIsResolvedOnTheNextInteraction(GameTestHelper ctx) {
        floor(ctx, 4);
        ServerLevel level = ctx.getLevel();
        TombEncounterData data = TombGuardianEncounters.data(level);
        BlockPos spawn = ctx.absolutePos(new BlockPos(4, 1, 4));
        // Crash-like stale binding: recorded guardian whose (loaded) chunk no longer contains it.
        BlockPos stale = ctx.absolutePos(new BlockPos(1, 1, 3));
        data.bind(TombGuardianEncounters.tombKey(stale), UUID.randomUUID(), ChunkPos.pack(spawn), level.getGameTime());
        var restarted = TombGuardianEncounters.onRoyalChestInteraction(level, stale, spawn, null);
        ctx.assertTrue(restarted == TombGuardianEncounters.StartResult.RESTARTED, "a vanished guardian is replaced, got " + restarted);
        // Guardian possibly still stored in an unloaded chunk: never spawn a duplicate.
        BlockPos unloaded = ctx.absolutePos(new BlockPos(1, 1, 4));
        BlockPos farAway = spawn.offset(30000, 0, 30000);
        data.bind(TombGuardianEncounters.tombKey(unloaded), UUID.randomUUID(), ChunkPos.pack(farAway), level.getGameTime());
        var refused = TombGuardianEncounters.onRoyalChestInteraction(level, unloaded, spawn, null);
        ctx.assertTrue(refused == TombGuardianEncounters.StartResult.GUARDIAN_NOT_LOADED, "unloaded guardian blocks a new spawn, got " + refused);
        ctx.assertTrue(guardiansOf(level, unloaded).isEmpty(), "nothing was spawned");
        ctx.assertTrue(guardiansOf(level, stale).size() == 1, "the restarted encounter has exactly one guardian");
        guardiansOf(level, stale).forEach(Entity::discard);
        ctx.succeed();
    }

    @GameTest(maxTicks = 60)
    public void killingTheBoundGuardianDefeatsTheTombOnceAndRefusesRespawn(GameTestHelper ctx) {
        floor(ctx, 4);
        ServerLevel level = ctx.getLevel();
        BlockPos anchor = ctx.absolutePos(new BlockPos(1, 1, 5));
        BlockPos spawn = ctx.absolutePos(new BlockPos(4, 1, 4));
        WATCHED_ANCHOR.set(anchor);
        DEFEATED_EVENTS.set(0);
        TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null);
        TombGuardian guardian = guardiansOf(level, anchor).get(0);
        guardian.skipAwakening();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        guardian.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        ctx.assertTrue(guardian.isDeadOrDying(), "the guardian dies");
        ctx.assertTrue(TombGuardianEncounters.isDefeated(level, anchor), "the tomb is recorded DEFEATED");
        ctx.assertTrue(!TombGuardianEncounters.isRoyalChestSealed(level, anchor), "the royal chest seal is released");
        ctx.assertTrue(DEFEATED_EVENTS.get() == 1, "the completion event fires once");
        int hearts = level.getEntitiesOfClass(ItemEntity.class, new AABB(spawn).inflate(6.0D),
                item -> item.getItem().is(ModItems.ANCIENT_HEART)).stream().mapToInt(item -> item.getItem().getCount()).sum();
        ctx.assertTrue(hearts <= 1, "at most one Ancient Heart from the kill, got " + hearts);
        var again = TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null);
        ctx.assertTrue(again == TombGuardianEncounters.StartResult.ALREADY_DEFEATED, "a defeated tomb never summons again, got " + again);
        ctx.assertTrue(!TombGuardianEncounters.data(level).markDefeated(TombGuardianEncounters.tombKey(anchor), guardian.getUUID()),
                "completion cannot be granted twice");
        ctx.runAfterDelay(30, () -> {
            ctx.assertTrue(guardiansOf(level, anchor).isEmpty(), "no replacement guardian appeared");
            ctx.assertTrue(DEFEATED_EVENTS.get() == 1, "still exactly one completion");
            level.getEntitiesOfClass(ItemEntity.class, new AABB(spawn).inflate(6.0D)).forEach(Entity::discard);
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void orphanGuardianCopyIsRemovedWithoutReward(GameTestHelper ctx) {
        floor(ctx, 4);
        ServerLevel level = ctx.getLevel();
        BlockPos anchor = ctx.absolutePos(new BlockPos(1, 1, 6));
        BlockPos spawn = ctx.absolutePos(new BlockPos(4, 1, 4));
        TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null);
        TombGuardian bound = guardiansOf(level, anchor).get(0);
        // A stale saved copy of the guardian (e.g. an older chunk save) appearing alongside the bound one.
        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        bound.saveWithoutId(saved);
        TombGuardian copy = TombGuardianEntities.TOMB_GUARDIAN.create(level, EntitySpawnReason.LOAD);
        copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
        copy.setUUID(UUID.randomUUID());
        level.addFreshEntity(copy);
        ctx.runAfterDelay(3, () -> {
            ctx.assertTrue(copy.isRemoved() && bound.isAlive(), "the unbound copy removes itself; the bound guardian stays");
            ctx.assertTrue(TombGuardianEncounters.data(level).isBound(TombGuardianEncounters.tombKey(anchor), bound.getUUID()),
                    "the orphan's removal does not release the real encounter");
            bound.discard();
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 900)
    public void chunkUnloadIsNotDeathAndNeverAllowsADuplicate(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos base = ctx.absolutePos(BlockPos.ZERO).offset(4096, 0, 4096);
        ChunkPos chunk = ChunkPos.containing(base);
        level.setChunkForced(chunk.x(), chunk.z(), true);
        BlockPos spawn = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        BlockPos anchor = spawn.offset(2, 0, 2);
        long tomb = TombGuardianEncounters.tombKey(anchor);
        AtomicInteger phase = new AtomicInteger();
        AtomicReference<UUID> guardianId = new AtomicReference<>();
        ctx.onEachTick(() -> {
            switch (phase.get()) {
                case 0 -> {
                    if (!level.areEntitiesLoaded(chunk.pack())) return;
                    var started = TombGuardianEncounters.onRoyalChestInteraction(level, anchor,
                            level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base), null);
                    ctx.assertTrue(started == TombGuardianEncounters.StartResult.STARTED, "encounter starts, got " + started);
                    guardianId.set(TombGuardianEncounters.data(level).active(tomb).guardian());
                    level.setChunkForced(chunk.x(), chunk.z(), false);
                    phase.set(1);
                }
                case 1 -> {
                    if (level.areEntitiesLoaded(chunk.pack())) return;
                    ctx.assertTrue(TombGuardianEncounters.data(level).isBound(tomb, guardianId.get()),
                            "unloading the chunk keeps the encounter bound (not death, not a loss)");
                    ctx.assertTrue(!TombGuardianEncounters.isDefeated(level, anchor), "unload is never a defeat");
                    var whileUnloaded = TombGuardianEncounters.onRoyalChestInteraction(level, anchor, anchor, null);
                    ctx.assertTrue(whileUnloaded == TombGuardianEncounters.StartResult.GUARDIAN_NOT_LOADED,
                            "no second guardian while the first is stored in an unloaded chunk, got " + whileUnloaded);
                    level.setChunkForced(chunk.x(), chunk.z(), true);
                    phase.set(2);
                }
                case 2 -> {
                    if (!level.areEntitiesLoaded(chunk.pack()) || level.getEntity(guardianId.get()) == null) return;
                    var reloaded = TombGuardianEncounters.onRoyalChestInteraction(level, anchor, anchor, null);
                    ctx.assertTrue(reloaded == TombGuardianEncounters.StartResult.ALREADY_ACTIVE,
                            "the reloaded guardian is the same encounter, got " + reloaded);
                    ctx.assertTrue(level.getEntitiesOfClass(TombGuardian.class, new AABB(spawn).inflate(32.0D), Entity::isAlive).size() == 1,
                            "exactly one guardian after reload");
                    TombGuardianEncounters.endEncounter(level, anchor);
                    level.setChunkForced(chunk.x(), chunk.z(), false);
                    phase.set(3);
                    ctx.succeed();
                }
                default -> {}
            }
        });
    }

    /**
     * Process-restart persistence, run in three separate server processes by the tomb restart workflow
     * (-Dmoreminecrafts.tomb.restart.phase=write|reload|verify). Inert in the ordinary GameTest run.
     */
    @GameTest(maxTicks = 600)
    public void tombEncounterStateSurvivesProcessRestart(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.tomb.restart.phase");
        if (phase == null || phase.isBlank()) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        BlockPos defeated = new BlockPos(-30001, 70, -30001);
        BlockPos interrupted = new BlockPos(-30002, 70, -30002);
        BlockPos spawn = ctx.absolutePos(new BlockPos(2, 1, 2));
        floor(ctx, 2);
        TombEncounterData data = TombGuardianEncounters.data(level);
        long interruptedKey = TombGuardianEncounters.tombKey(interrupted);
        switch (phase) {
            case "write" -> {
                ctx.assertTrue(!data.isDefeated(TombGuardianEncounters.tombKey(defeated))
                        && data.active(interruptedKey) == null, "the write phase starts from a fresh world");
                TombGuardianEncounters.onRoyalChestInteraction(level, defeated, spawn, null);
                TombGuardian guardian = guardiansOf(level, defeated).get(0);
                guardian.skipAwakening();
                guardian.hurtServer(level, level.damageSources().playerAttack(ctx.makeMockServerPlayerInLevel()), 1000.0F);
                ctx.assertTrue(TombGuardianEncounters.isDefeated(level, defeated), "defeated before shutdown");
                // An undefeated encounter whose guardian is gone after the restart (crash-like interruption).
                data.bind(interruptedKey, UUID.randomUUID(), ChunkPos.pack(spawn), level.getGameTime());
                finishRestartPhase(ctx, level, spawn, phase);
            }
            case "reload", "verify" -> {
                ctx.assertTrue(TombGuardianEncounters.isDefeated(level, defeated), "DEFEATED survives the restart");
                ctx.assertTrue(!TombGuardianEncounters.isRoyalChestSealed(level, defeated), "the chest stays unsealed after restart");
                var again = TombGuardianEncounters.onRoyalChestInteraction(level, defeated, spawn, null);
                ctx.assertTrue(again == TombGuardianEncounters.StartResult.ALREADY_DEFEATED, "no respawn after restart, got " + again);
                if (phase.equals("verify")) {
                    ctx.assertTrue(data.active(interruptedKey) == null, "the retried encounter was ended and saved");
                    finishRestartPhase(ctx, level, spawn, phase);
                    return;
                }
                TombEncounterData.Active binding = data.active(interruptedKey);
                ctx.assertTrue(binding != null, "the interrupted encounter binding was persisted");
                ChunkPos chunk = ChunkPos.unpack(binding.guardianChunk());
                level.setChunkForced(chunk.x(), chunk.z(), true);
                AtomicInteger done = new AtomicInteger();
                ctx.onEachTick(() -> {
                    // Until the recorded chunk's entities are loaded the guardian might still exist: no duplicate.
                    if (done.get() != 0 || !level.areEntitiesLoaded(binding.guardianChunk())) return;
                    done.set(1);
                    var retry = TombGuardianEncounters.onRoyalChestInteraction(level, interrupted, spawn, null);
                    ctx.assertTrue(retry == TombGuardianEncounters.StartResult.RESTARTED,
                            "the interrupted, undefeated encounter can be retried after restart, got " + retry);
                    ctx.assertTrue(guardiansOf(level, interrupted).size() == 1, "exactly one fresh guardian");
                    TombGuardianEncounters.endEncounter(level, interrupted);
                    level.setChunkForced(chunk.x(), chunk.z(), false);
                    finishRestartPhase(ctx, level, spawn, phase);
                });
            }
            default -> ctx.fail("unknown phase " + phase);
        }
    }

    private static void finishRestartPhase(GameTestHelper ctx, ServerLevel level, BlockPos spawn, String phase) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(spawn).inflate(8.0D)).forEach(Entity::discard);
        level.getServer().saveEverything(true, true, true);
        System.out.println("TOMB_GUARDIAN_RESTART_" + phase.toUpperCase());
        ctx.succeed();
    }
}
