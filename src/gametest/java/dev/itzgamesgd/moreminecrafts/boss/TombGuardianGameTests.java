package dev.itzgamesgd.moreminecrafts.boss;

import dev.itzgamesgd.moreminecrafts.accessory.AccessoryDefinition;
import dev.itzgamesgd.moreminecrafts.mob.pyramid.PyramidMobs;
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
import net.minecraft.world.item.Item;
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

    /**
     * Empty 16x8x16 structure with the same origin as the default 8x8x8 one. Stone Eruption tests put the guardian at
     * (2, 1, 2) and the eruption site up to x = 12.5, so every entity they rely on lies inside this structure. The
     * GameTest framework force-loads every chunk the structure box touches, so those entities tick wherever the
     * randomly placed test lands relative to chunk borders (with the 8-wide default box they could sit in an
     * unforced neighbour chunk and never tick).
     */
    private static final String ARENA = "moreminecrafts-gametest:tomb_guardian_arena";

    /**
     * Fails fast unless the guardian, the marked eruption site (where the statue body and display spawn) and every
     * other entity the test relies on are in entity-ticking chunks; the arena structure guarantees it.
     */
    private static void assertArenaTicks(GameTestHelper ctx, TombGuardian guardian, Entity... others) {
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(guardian.spikePoint() != null
                        && level.isPositionEntityTicking(BlockPos.containing(guardian.spikePoint())),
                "eruption site must be entity-ticking, at " + guardian.spikePoint());
        ctx.assertTrue(level.isPositionEntityTicking(guardian.blockPosition()), "guardian must be entity-ticking");
        for (Entity other : others)
            ctx.assertTrue(level.isPositionEntityTicking(other.blockPosition()),
                    other.getType() + " must be entity-ticking, at " + other.position());
    }

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
    public void guardianSpawnsWithSpecAttributesAndBossRules(GameTestHelper ctx) {
        floor(ctx, 2);
        TombGuardian guardian = ctx.spawn(TombGuardianEntities.TOMB_GUARDIAN, new BlockPos(2, 1, 2));
        ctx.assertTrue(guardian.getMaxHealth() == 300.0F, "spec max health 300, got " + guardian.getMaxHealth());
        ctx.assertTrue(guardian.getAttributeValue(Attributes.ARMOR) == 12.0D, "spec armor 12");
        ctx.assertTrue(Math.abs(guardian.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.21D) < 1.0E-9, "spec speed 0.21");
        ctx.assertTrue(guardian.getAttributeValue(Attributes.FOLLOW_RANGE) == 24.0D, "sight range ~24 blocks");
        ctx.assertTrue(Math.abs(guardian.getBbHeight() - 4.35F) < 0.01F && Math.abs(guardian.getBbWidth() - 1.35F) < 0.01F,
                "spec hitbox 1.35 x 4.35");
        ctx.assertTrue(Math.abs(guardian.getEyeHeight() - 3.6975F) < 0.001F, "spec eye height 3.6975, got " + guardian.getEyeHeight());
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
        return mockPlayer(ctx, relative, GameType.SURVIVAL);
    }

    private static ServerPlayer mockPlayer(GameTestHelper ctx, Vec3 relative, GameType mode) {
        var server = ctx.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tomb-sight"), false);
        ServerPlayer player = new ServerPlayer(server, ctx.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(mode);
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

    // Attacks (spec 4.1). Victims are villager dummies with 100 HP and no armor, so damage is exact.

    @GameTest(maxTicks = 80)
    public void sweepTelegraphsThenHitsOnlyTheForwardFanAtTheStrikeFrame(GameTestHelper ctx) {
        floor(ctx, 4);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        guardian.setYRot(0.0F);
        Villager front = dummy(ctx, new Vec3(2.5D, 1.0D, 4.5D));
        Villager behind = dummy(ctx, new Vec3(2.5D, 1.0D, 0.5D));
        ctx.assertTrue(guardian.forceAttack(ctx.getLevel(), TombGuardianCombat.Attack.SWEEP, front), "sweep starts");
        float frontBefore = front.getHealth();
        float behindBefore = behind.getHealth();
        ctx.runAfterDelay(TombGuardianTuning.SWEEP_WINDUP_TICKS - 3, () -> {
            ctx.assertTrue(guardian.currentAttack() == TombGuardianCombat.Attack.SWEEP, "still winding up");
            ctx.assertTrue(front.getHealth() == frontBefore, "no damage during the windup");
        });
        ctx.runAfterDelay(TombGuardianTuning.SWEEP_WINDUP_TICKS + 4, () -> {
            ctx.assertTrue(guardian.currentAttack() != TombGuardianCombat.Attack.SWEEP, "sweep resolved");
            float taken = frontBefore - front.getHealth();
            ctx.assertTrue(Math.abs(taken - 9.0F) < 0.01F, "the forward victim takes exactly 9, got " + taken);
            ctx.assertTrue(behind.getHealth() == behindBefore, "a victim behind the guardian is never hit");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 80)
    public void sweepTracksTheTargetDuringItsWindup(GameTestHelper ctx) {
        floor(ctx, 4);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        guardian.setYRot(0.0F);
        // Target is on the -x side; the windup turns the body (yaw 90 faces -x) so the strike lands on it.
        Villager side = dummy(ctx, new Vec3(0.5D, 1.0D, 2.5D));
        ctx.assertTrue(guardian.forceAttack(ctx.getLevel(), TombGuardianCombat.Attack.SWEEP, side), "sweep starts");
        float before = side.getHealth();
        ctx.runAfterDelay(TombGuardianTuning.SWEEP_WINDUP_TICKS + 4, () -> {
            ctx.assertTrue(Math.abs(guardian.getYRot() - 90.0F) < 1.0F || Math.abs(guardian.getYRot() + 270.0F) < 1.0F,
                    "the body turned to face the target, yaw " + guardian.getYRot());
            ctx.assertTrue(Math.abs((before - side.getHealth()) - 9.0F) < 0.01F, "the tracked target is hit by the fan");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void groundSlamWaveHitsOnceInsideItsRingAndNotBeyond(GameTestHelper ctx) {
        floor(ctx, 10);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager near = dummy(ctx, new Vec3(2.5D, 1.0D, 5.5D));
        Villager far = dummy(ctx, new Vec3(2.5D, 1.0D, 9.5D));
        ctx.assertTrue(guardian.forceAttack(ctx.getLevel(), TombGuardianCombat.Attack.SLAM, near), "slam starts");
        float nearBefore = near.getHealth();
        float farBefore = far.getHealth();
        ctx.runAfterDelay(TombGuardianTuning.SLAM_WINDUP_TICKS + TombGuardianCombat.waveTravelTicks(TombGuardianTuning.WAVE_RADIUS) + 4, () -> {
            float taken = nearBefore - near.getHealth();
            ctx.assertTrue(Math.abs(taken - 12.0F) < 0.01F, "the victim inside the 6 m ring takes exactly 12, got " + taken);
            ctx.assertTrue(far.getHealth() == farBefore, "a victim beyond 6 m is not hit");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 80, structure = ARENA)
    public void stoneEruptionShowsAMagicCircleAtAFixedPointAndSideStepAvoidsIt(GameTestHelper ctx) {
        floor(ctx, 8);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Vec3 point = new Vec3(9.5D, 1.0D, 2.5D);
        Villager dodger = dummy(ctx, point);
        Villager stander = dummy(ctx, point.add(0.6D, 0.0D, 0.6D));
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SPIKES, dodger), "eruption starts");
        assertArenaTicks(ctx, guardian, dodger, stander);
        Vec3 marked = guardian.spikePoint();
        ctx.assertTrue(marked != null && marked.distanceTo(dodger.position()) < 0.01D, "the site is fixed at the target's position");
        ctx.runAfterDelay(4, () -> {
            List<TombVfxDisplay> circles = level.getEntitiesOfClass(TombVfxDisplay.class, new AABB(BlockPos.containing(marked)).inflate(1.0D),
                    display -> !display.isStatue() && !display.isFill());
            ctx.assertTrue(circles.size() == 1, "exactly one gold magic circle marks the site during the warning");
            List<TombVfxDisplay> gauges = level.getEntitiesOfClass(TombVfxDisplay.class, new AABB(BlockPos.containing(marked)).inflate(1.0D),
                    TombVfxDisplay::isFill);
            ctx.assertTrue(gauges.isEmpty(), "tomb spikes use the gold circle only: no red charge gauge");
            // Side-step: the marked site must not follow the moving target.
            dodger.teleportTo(dodger.getX(), dodger.getY(), dodger.getZ() + 3.0D);
        });
        ctx.runAfterDelay(8, () -> ctx.assertTrue(guardian.spikePoint() != null && guardian.spikePoint().equals(marked),
                "the eruption is never re-targeted after the warning begins"));
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS - 2, () ->
                ctx.assertTrue(stander.getHealth() == 100.0F, "no damage before the statue rises"));
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS + 4, () -> {
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

    // Stone Eruption collision (owner-approved rule A: shove to a safe place, else cancel the eruption).

    private static List<TombStatueCollider> collidersAt(ServerLevel level, Vec3 point) {
        return level.getEntitiesOfClass(TombStatueCollider.class, new AABB(BlockPos.containing(point)).inflate(2.0D));
    }

    private static List<TombVfxDisplay> displaysAt(ServerLevel level, Vec3 point) {
        return level.getEntitiesOfClass(TombVfxDisplay.class, new AABB(BlockPos.containing(point)).inflate(2.0D));
    }

    @GameTest(maxTicks = 120, structure = ARENA)
    public void risenStatueIsSolidAtTheApprovedSizeAndTelegraphStaysWalkable(GameTestHelper ctx) {
        floor(ctx, 8);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Vec3 site = new Vec3(9.5D, 1.0D, 2.5D);
        Villager target = dummy(ctx, site.add(2.5D, 0.0D, 0.0D));
        Villager probe = dummy(ctx, site.add(5.0D, 0.0D, 3.0D));
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SPIKES, target), "eruption starts");
        assertArenaTicks(ctx, guardian, target, probe);
        Vec3 marked = guardian.spikePoint();
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS - 2, () -> {
            ctx.assertTrue(collidersAt(level, marked).isEmpty()
                            && level.getEntityCollisions(probe, TombStatueShape.fullBox(marked)).isEmpty(),
                    "the warning telegraph has no collision, so the original dodge window is unchanged");
        });
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS + 4 + TombStatueShape.RISE_TICKS, () -> {
            List<TombStatueCollider> colliders = collidersAt(level, marked);
            ctx.assertTrue(colliders.size() == 1, "exactly one collision body, got " + colliders.size());
            TombStatueCollider collider = colliders.get(0);
            ctx.assertTrue(collider.getBoundingBox().equals(TombStatueShape.fullBox(marked)),
                    "fully risen: collision equals the approved obelisk, got " + collider.getBoundingBox());
            ctx.assertTrue(collider.solidHeight() == (float) TombGuardianTuning.SPIKE_HEIGHT, "full height");
            ctx.assertTrue(!level.getEntityCollisions(probe, TombStatueShape.fullBox(marked).deflate(0.05D)).isEmpty(),
                    "the server treats the statue as solid for movers");
            ctx.assertTrue(level.getBlockState(BlockPos.containing(marked)).isAir(), "no block was placed");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 160, structure = ARENA)
    public void statueCollisionFollowsTheDisplayedRiseAndSinkThenVanishes(GameTestHelper ctx) {
        floor(ctx, 8);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager target = dummy(ctx, new Vec3(12.5D, 1.0D, 2.5D));
        Villager probe = dummy(ctx, new Vec3(13.5D, 1.0D, 6.5D));
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SPIKES, target), "eruption starts");
        assertArenaTicks(ctx, guardian, target, probe);
        Vec3 marked = guardian.spikePoint();
        AtomicReference<Float> last = new AtomicReference<>(0.0F);
        AtomicInteger peaks = new AtomicInteger();
        AtomicInteger partial = new AtomicInteger();
        ctx.onEachTick(() -> {
            List<TombStatueCollider> colliders = collidersAt(level, marked);
            List<TombVfxDisplay> displays = displaysAt(level, marked).stream().filter(TombVfxDisplay::isStatue).toList();
            if (colliders.isEmpty()) {
                if (last.get() > 0.0F) {
                    ctx.assertTrue(displays.isEmpty(), "the collision is removed together with the display");
                    ctx.assertTrue(level.getEntityCollisions(probe, TombStatueShape.fullBox(marked)).isEmpty(),
                            "after the sink nothing solid remains");
                    ctx.assertTrue(peaks.get() > 0 && partial.get() >= 2, "saw a full stand and partial rise/sink heights");
                    ctx.succeed();
                }
                return;
            }
            TombStatueCollider collider = colliders.get(0);
            ctx.assertTrue(displays.size() == 1 && displays.get(0).position().equals(collider.position()),
                    "visual and collision share one position");
            double expected = Math.min(collider.solidHeight(), TombGuardianTuning.SPIKE_HEIGHT);
            ctx.assertTrue(Math.abs(collider.getBoundingBox().maxY - (marked.y + expected)) < 1.0E-4D,
                    "collision top follows the synced height, got " + collider.getBoundingBox());
            AABB below = TombStatueShape.box(marked, expected);
            ctx.assertTrue(collider.getBoundingBox().equals(below), "box is exactly the current slice");
            float h = collider.solidHeight();
            if (h >= (float) TombGuardianTuning.SPIKE_HEIGHT) peaks.incrementAndGet();
            else if (h > 0.0F) partial.incrementAndGet();
            if (h > 0.0F) {
                AABB inside = TombStatueShape.box(marked, h).deflate(0.02D);
                ctx.assertTrue(!level.getEntityCollisions(probe, inside).isEmpty(), "solid up to the current height");
            }
            last.set(Math.max(last.get(), h));
        });
    }

    @GameTest(maxTicks = 80, structure = ARENA)
    public void anOccupantIsShovedToASafeSpotWithoutBeingTrapped(GameTestHelper ctx) {
        floor(ctx, 8);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Vec3 site = new Vec3(9.5D, 1.0D, 2.5D);
        Villager inside = dummy(ctx, site);
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SPIKES, inside), "eruption starts");
        assertArenaTicks(ctx, guardian, inside);
        Vec3 marked = guardian.spikePoint();
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS + 8, () -> {
            AABB body = TombStatueShape.fullBox(marked);
            ctx.assertTrue(!inside.getBoundingBox().intersects(body), "the occupant is outside the solid statue");
            ctx.assertTrue(level.noCollision(inside, inside.getBoundingBox()), "and not wedged into terrain or the statue");
            ctx.assertTrue(inside.getY() >= marked.y - 0.01D, "not sunk into the floor");
            ctx.assertTrue(inside.getHealth() == 92.0F, "the eruption still hits an entity that was inside, got " + inside.getHealth());
            ctx.assertTrue(collidersAt(level, marked).size() == 1, "the statue rose");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 140, structure = ARENA)
    public void anEnclosedOccupantCancelsTheEruptionWithoutDamageOrLeftovers(GameTestHelper ctx) {
        floor(ctx, 8);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        BlockPos cell = new BlockPos(9, 1, 2);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0)
                    for (int y = 0; y < 4; y++) ctx.setBlock(cell.offset(dx, y, dz), Blocks.STONE);
        Villager trapped = dummy(ctx, new Vec3(9.5D, 1.0D, 2.5D));
        Villager bystander = dummy(ctx, new Vec3(9.5D, 1.0D, 6.5D));
        ServerLevel level = ctx.getLevel();
        Vec3 before = trapped.position();
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SPIKES, trapped), "eruption starts");
        assertArenaTicks(ctx, guardian, trapped, bystander);
        Vec3 marked = guardian.spikePoint();
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS + 3, () -> {
            ctx.assertTrue(trapped.getHealth() == 100.0F && bystander.getHealth() == 100.0F, "a cancelled eruption deals no damage");
            ctx.assertTrue(trapped.position().equals(before), "nobody was moved through the walls");
            ctx.assertTrue(collidersAt(level, marked).isEmpty() && displaysAt(level, marked).stream().noneMatch(TombVfxDisplay::isStatue),
                    "no collision body or statue display remains");
            ctx.assertTrue(guardian.currentAttack() == TombGuardianCombat.Attack.NONE && guardian.spikePoint() == null,
                    "the attack ended as a failure with no stored site");
            long spikeCooldown = guardian.cooldownRemaining(TombGuardianCombat.Attack.SPIKES);
            ctx.assertTrue(spikeCooldown > 0L && spikeCooldown <= TombGuardianTuning.SPIKE_COOLDOWN_TICKS,
                    "the eruption cooldown is running after the cancelled attempt, got " + spikeCooldown);
        });
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS + 40, () -> {
            ctx.assertTrue(guardian.currentAttack() != TombGuardianCombat.Attack.SPIKES, "no immediate repeat of the cancelled eruption");
            ctx.assertTrue(displaysAt(level, marked).isEmpty() && collidersAt(level, marked).isEmpty(), "the telegraph is gone too");
            ctx.assertTrue(level.getBlockState(BlockPos.containing(marked)).isAir(), "no block edits");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 120, structure = ARENA)
    public void everyoneInTheCircleIsHitExactlyOnceAndOneBlockedOccupantCancelsForAll(GameTestHelper ctx) {
        floor(ctx, 8);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Vec3 site = new Vec3(9.5D, 1.0D, 2.5D);
        Villager a = dummy(ctx, site);
        Villager b = dummy(ctx, site.add(0.9D, 0.0D, 0.0D));
        Villager c = dummy(ctx, site.add(0.0D, 0.0D, -0.9D));
        Villager outside = dummy(ctx, site.add(3.0D, 0.0D, 3.0D));
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SPIKES, a), "eruption starts");
        assertArenaTicks(ctx, guardian, a, b, c, outside);
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS + 3, () ->
                ctx.assertTrue(a.getHealth() == 92.0F && b.getHealth() == 92.0F && c.getHealth() == 92.0F && outside.getHealth() == 100.0F,
                        "each target in the circle takes 8 HP once, others none: " + a.getHealth() + "/" + b.getHealth() + "/" + c.getHealth()));
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS + TombGuardianTuning.SPIKE_VISIBLE_TICKS + 6, () -> {
            ctx.assertTrue(a.getHealth() == 92.0F && b.getHealth() == 92.0F && c.getHealth() == 92.0F,
                    "the standing or sinking statue never damages again (no duplicate damage)");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 80, structure = ARENA)
    public void anOccupantBlockedAtTheEruptionMomentCancelsTheWholeEruptionForEveryone(GameTestHelper ctx) {
        floor(ctx, 8);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        BlockPos cell = new BlockPos(9, 1, 2);
        Villager free = dummy(ctx, new Vec3(9.5D, 1.0D, 2.5D));
        Villager trapped = dummy(ctx, new Vec3(9.5D, 1.0D, 2.5D));
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0)
                    for (int y = 0; y < 4; y++) ctx.setBlock(cell.offset(dx, y, dz), Blocks.STONE);
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SPIKES, free), "eruption starts");
        assertArenaTicks(ctx, guardian, free, trapped);
        Vec3 marked = guardian.spikePoint();
        ctx.runAfterDelay(TombGuardianTuning.SPIKE_WINDUP_TICKS + 4, () -> {
            ctx.assertTrue(free.getHealth() == 100.0F && trapped.getHealth() == 100.0F,
                    "all-or-nothing: a cancelled eruption damages nobody: " + free.getHealth() + "/" + trapped.getHealth() + " free=" + free.position() + " trapped=" + trapped.position());
            ctx.assertTrue(collidersAt(level, marked).isEmpty(), "no collision body");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void statueBodyAndDisplayAreNeverWrittenToChunkStorage(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Vec3 point = ctx.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D));
        TombStatueCollider collider = TombStatueCollider.spawn(level, point, 60);
        TombVfxDisplay display = TombVfxDisplay.statue(level, point, 60);
        ctx.assertTrue(collider != null && display != null, "both spawn");
        // Chunk unload and server restart write only serialisable types, so neither can come back stale.
        ctx.assertTrue(!TombGuardianEntities.TOMB_STATUE_COLLIDER.canSerialize() && !TombGuardianEntities.TOMB_VFX.canSerialize(),
                "neither type is serialisable");
        TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        ctx.assertTrue(!collider.saveAsPassenger(saved) && !display.saveAsPassenger(saved), "neither entity serialises");
        collider.discard();
        display.discard();
        ctx.succeed();
    }

    @GameTest(maxTicks = 40)
    public void enrageStartsAtFiftyPercentWithSpecSpeedAndSurvivesSaveLoad(GameTestHelper ctx) {
        floor(ctx, 2);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        guardian.setHealth(150.5F);
        ctx.runAfterDelay(2, () -> {
            ctx.assertTrue(!guardian.isEnraged(), "above 50% (150.5/300) stays in the base phase");
            guardian.setHealth(150.0F);
        });
        ctx.runAfterDelay(4, () -> {
            ctx.assertTrue(guardian.isEnraged(), "at 50% (150/300) the single enrage phase starts");
            double speed = guardian.getAttributeValue(Attributes.MOVEMENT_SPEED);
            ctx.assertTrue(Math.abs(speed - 0.24D) < 1.0E-6D, "enraged speed is 0.24, got " + speed);
            guardian.setHealth(250.0F);
        });
        ctx.runAfterDelay(6, () -> {
            ctx.assertTrue(guardian.isEnraged(), "enrage is one-way");
            TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
            guardian.saveWithoutId(saved);
            TombGuardian loaded = TombGuardianEntities.TOMB_GUARDIAN.create(ctx.getLevel(), EntitySpawnReason.LOAD);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved.buildResult()));
            ctx.assertTrue(loaded.isEnraged() && Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED) - 0.24D) < 1.0E-6D,
                    "enrage and its speed survive save/load");
            loaded.discard();
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 220)
    public void enrageStartsTheUltimateAtOnceAndItsThreeStrikesLand(GameTestHelper ctx) {
        floor(ctx, 14);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager victim = dummy(ctx, new Vec3(2.5D, 1.0D, 7.5D));
        guardian.setTarget(victim);
        guardian.setHealth(150.0F);
        float before = victim.getHealth();
        ctx.runAfterDelay(4, () -> {
            ctx.assertTrue(guardian.isEnraged(), "enraged at 50%");
            ctx.assertTrue(guardian.currentAttack() == TombGuardianCombat.Attack.ULTIMATE,
                    "the ultimate starts at once on enrage, got " + guardian.currentAttack());
            // Clearing the target stops other patterns from chaining onto the victim; the ultimate itself keeps going.
            guardian.setTarget(null);
        });
        ctx.runAfterDelay(200, () -> {
            float taken = before - victim.getHealth();
            // 8 m strike 10 + 12 m strike 10 + full-range strike 12. Each wave hits the victim once.
            ctx.assertTrue(Math.abs(taken - 32.0F) < 0.01F, "three strikes hit the victim exactly once each (32), got " + taken);
            ctx.assertTrue(guardian.currentAttack() == TombGuardianCombat.Attack.NONE, "the ultimate resolves");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void royalGuardSummonsTwoMummiesAndOneTombSkeletonAndNeverExceedsThree(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        floor(ctx, 10);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager victim = dummy(ctx, new Vec3(2.5D, 1.0D, 9.5D));
        // Summons are ordinary monsters: Mob.checkDespawn discards them once the nearest player is over 128 blocks away,
        // and other parallel tests keep mock players near the origin. A creative player beside this test (ignored by the
        // guardian's targeting and area hits) keeps the nearest-player distance small, as it is in a real fight.
        ServerPlayer anchor = mockPlayer(ctx, new Vec3(8.5D, 1.0D, 8.5D), GameType.CREATIVE);
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SUMMON, victim), "summon starts");
        ctx.runAfterDelay(TombGuardianTuning.SUMMON_WINDUP_TICKS + 2, () -> {
            ctx.assertTrue(guardian.aliveSummons(level) == 3, "three summons alive, got " + guardian.aliveSummons(level)
                    + " [diag players=" + level.players().size() + " nearestPlayerDist="
                    + (level.getNearestPlayer(guardian, -1.0D) == null ? "none"
                            : String.format("%.0f", Math.sqrt(level.getNearestPlayer(guardian, -1.0D).distanceToSqr(guardian))))
                    + " difficulty=" + level.getDifficulty() + " guardianAlive=" + guardian.isAlive() + "]");
            AABB around = guardian.getBoundingBox().inflate(12.0D);
            int mummies = level.getEntities(PyramidMobs.BURIED_MUMMY, around, e -> e.isAlive()).size();
            int archers = level.getEntities(PyramidMobs.CRYPT_ARCHER, around, e -> e.isAlive()).size();
            ctx.assertTrue(mummies == 2 && archers == 1, "2 mummies and 1 tomb skeleton, got " + mummies + "/" + archers);
            ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SUMMON, victim), "a second summon may be started");
        });
        ctx.runAfterDelay(TombGuardianTuning.SUMMON_WINDUP_TICKS * 2 + 6, () -> {
            ctx.assertTrue(guardian.aliveSummons(level) == 3, "the living total stays at 3, got " + guardian.aliveSummons(level));
            level.getServer().getPlayerList().remove(anchor);
            ctx.succeed();
        });
    }


    // Red telegraph (spec 4.2) is used by the direct attacks: sweep, ground slam and the ultimate. Spikes keep the gold circle.

    private static long gauges(ServerLevel level, Vec3 centre) {
        return displaysAt(level, centre).stream().filter(TombVfxDisplay::isFill).count();
    }

    private static long areas(ServerLevel level, Vec3 centre) {
        return displaysAt(level, centre).stream().filter(d -> !d.isFill() && !d.isStatue()).count();
    }

    @GameTest(maxTicks = 80)
    public void sweepShowsARedFanAreaAndChargeGaugeThatEndWithTheWindup(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        floor(ctx, 4);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager target = dummy(ctx, new Vec3(2.5D, 1.0D, 4.5D));
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SWEEP, target), "sweep starts");
        guardian.setTarget(null);
        Vec3 where = guardian.position();
        ctx.runAfterDelay(4, () -> {
            ctx.assertTrue(areas(level, where) == 1 && gauges(level, where) == 1, "one red fan area and one red gauge during the windup");
        });
        ctx.runAfterDelay(TombGuardianTuning.SWEEP_WINDUP_TICKS + 6, () -> {
            ctx.assertTrue(areas(level, where) == 0 && gauges(level, where) == 0, "the fan telegraph is gone after the strike");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void groundSlamShowsARedZoneOfTheSlamRadius(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        floor(ctx, 10);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager target = dummy(ctx, new Vec3(2.5D, 1.0D, 5.5D));
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.SLAM, target), "slam starts");
        guardian.setTarget(null);
        Vec3 where = guardian.position();
        ctx.runAfterDelay(4, () -> {
            ctx.assertTrue(areas(level, where) == 1 && gauges(level, where) == 1, "one red zone and one red gauge during the windup");
        });
        ctx.runAfterDelay(TombGuardianTuning.SLAM_WINDUP_TICKS + 4, () -> {
            ctx.assertTrue(areas(level, where) == 0 && gauges(level, where) == 0, "the zone is gone once the slam lands");
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 200)
    public void ultimateShowsOneRedZoneBeforeEachOfItsThreeStrikes(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        floor(ctx, 14);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Villager target = dummy(ctx, new Vec3(2.5D, 1.0D, 8.5D));
        ctx.assertTrue(guardian.forceAttack(level, TombGuardianCombat.Attack.ULTIMATE, target), "ultimate starts");
        guardian.setTarget(null);
        Vec3 where = guardian.position();
        int interval = TombGuardianTuning.ULTIMATE_HIT_INTERVAL_TICKS;
        int windup = TombGuardianTuning.ULTIMATE_WINDUP_TICKS;
        ctx.runAfterDelay(4, () -> ctx.assertTrue(areas(level, where) == 1 && gauges(level, where) == 1,
                "a red zone and gauge charge for strike 1"));
        ctx.runAfterDelay(windup + 4, () -> ctx.assertTrue(areas(level, where) == 1 && gauges(level, where) == 1,
                "strike 2 gets its own red zone and gauge"));
        ctx.runAfterDelay(windup + interval + 4, () -> ctx.assertTrue(areas(level, where) == 1 && gauges(level, where) == 1,
                "strike 3 gets its own red zone and gauge"));
        ctx.runAfterDelay(windup + 2 * interval + 6, () -> {
            ctx.assertTrue(areas(level, where) == 0 && gauges(level, where) == 0, "no red zone remains after the third strike");
            ctx.succeed();
        });
    }

    // Loot.

    @GameTest
    public void killLootIsAncientHeartOneHundredPercentTimesOneAndNothingElse(GameTestHelper ctx) {
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
        int rolls = 2000;
        int hearts = 0;
        int gravity = 0;
        Item gravityItem = ModItems.ACCESSORIES.get(AccessoryDefinition.HEART_OF_GRAVITY);
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> drops = table.getRandomItems(params, (long) i * 7919L + 17L);
            int stacks = 0;
            int gravityStacks = 0;
            for (ItemStack stack : drops) {
                // 2026-11-14 P1-BUG-04: Ancient Heart is 100% x1. Heart of Gravity remains its own independent 20% x1 pool.
                if (stack.is(ModItems.ANCIENT_HEART)) {
                    ctx.assertTrue(stack.getCount() == 1, "Ancient Heart is exactly one, got " + stack);
                    stacks++;
                } else {
                    ctx.assertTrue(stack.is(gravityItem) && stack.getCount() == 1,
                            "the only other kill drop is exactly one Heart of Gravity, got " + stack);
                    gravityStacks++;
                }
            }
            ctx.assertTrue(stacks == 1, "exactly one Ancient Heart on every kill, got " + stacks);
            ctx.assertTrue(gravityStacks <= 1, "never more than one Heart of Gravity per kill");
            hearts += stacks;
            gravity += gravityStacks;
        }
        ctx.assertTrue(hearts == rolls, "Ancient Heart drop rate is 100%, measured " + hearts + "/" + rolls);
        double gravityRate = gravity / (double) rolls;
        ctx.assertTrue(gravityRate > 0.17D && gravityRate < 0.23D, "Heart of Gravity drop rate is 20%, measured " + gravityRate);
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

    @GameTest(maxTicks = 40)
    public void aGuardianWithoutTheEncounterTagNeverDropsTheHeart(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        floor(ctx, 2);
        TombGuardian guardian = freeGuardian(ctx, new BlockPos(2, 1, 2));
        Vec3 where = guardian.position();
        guardian.hurtServer(level, level.damageSources().genericKill(), 10000.0F);
        ctx.runAfterDelay(4, () -> {
            AABB around = new AABB(where.x - 4, where.y - 4, where.z - 4, where.x + 4, where.y + 4, where.z + 4);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, around,
                    item -> item.getItem().is(ModItems.ANCIENT_HEART));
            ctx.assertTrue(items.isEmpty(), "an untagged guardian drops no Ancient Heart, found " + items.size());
            ctx.succeed();
        });
    }

    @GameTest(maxTicks = 320, structure = ARENA)
    public void noLivingPlayerForFiveSecondsReturnsTheEncounterToIdleAndDespawnsTheGuardian(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos anchor = ctx.absolutePos(new BlockPos(1, 0, 1));
        BlockPos spawn = ctx.absolutePos(new BlockPos(2, 1, 2));
        TombGuardianEncounters.StartResult started = TombGuardianEncounters.onRoyalChestInteraction(level, anchor, spawn, null);
        ctx.assertTrue(started.spawned(), "the first interaction starts the encounter, got " + started);
        ctx.runAfterDelay(4, () -> {
            List<TombGuardian> guardians = guardiansOf(level, anchor);
            ctx.assertTrue(guardians.size() == 1, "one bound guardian, got " + guardians.size());
            guardians.get(0).skipAwakening();
        });
        ctx.runAfterDelay(TombGuardianTuning.NO_SURVIVOR_RESET_TICKS + 20, () -> {
            ctx.assertTrue(guardiansOf(level, anchor).isEmpty(), "the guardian despawns after 5 s without a living player");
            ctx.assertTrue(!TombGuardianEncounters.isDefeated(level, anchor), "the tomb is not defeated by the reset");
            ctx.assertTrue(TombGuardianEncounters.data(level).active(TombGuardianEncounters.tombKey(anchor)) == null,
                    "the binding is released, so the encounter is back to IDLE");
            ctx.succeed();
        });
    }
}
