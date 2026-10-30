package dev.itzgamesgd.moreminecrafts.colosseum;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Shared fixtures for the ST-05 GameTests: flat roofed test floors built in the sky (no natural colosseum structure),
 * fake survival players that keep their UUID across a restart, and a WAVE_CLEARED counter. Each test owns an arena far
 * from every other test.
 */
final class ColosseumFixtures {
    private ColosseumFixtures() {}

    private static final Map<Long, AtomicInteger> WAVES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_SLOT = new AtomicInteger();
    /** The GameTest world persists between runs; a per-run base keeps every run on untouched chunks. */
    static final int RUN_BASE_X = 100_000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 20_000) * 1_000;

    /** Arenas whose R7 is a stub boss: a tagged zombie standing in for the Immortal Gladiator, which does not exist yet. */
    static final Set<String> STUB_BOSS_ARENAS = ConcurrentHashMap.newKeySet();
    /** Block positions the test structure mask protects (original structure, crystal, player construction...). */
    static final Set<Long> PROTECTED = ConcurrentHashMap.newKeySet();
    static final List<String> BOSS_REMOVALS = new CopyOnWriteArrayList<>();
    private static final Map<String, AtomicInteger> CRYSTAL_ENDINGS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> BOSS_VANISHED = new ConcurrentHashMap<>();

    static {
        ColosseumIntegration.WAVE_CLEARED.register((level, arena, record) ->
                WAVES.computeIfAbsent(arena.key(), key -> new AtomicInteger()).incrementAndGet());
        ColosseumIntegration.CRYSTAL_ENDING.register((level, arena, trial, reason) ->
                CRYSTAL_ENDINGS.computeIfAbsent(arena.instanceId(), id -> new AtomicInteger()).incrementAndGet());
        ColosseumIntegration.BOSS_VANISHED.register((level, arena, trial) ->
                BOSS_VANISHED.computeIfAbsent(arena.instanceId(), id -> new AtomicInteger()).incrementAndGet());
        ColosseumIntegration.setStructureMask((level, arena, pos, state) -> PROTECTED.contains(pos.asLong()));
        ColosseumIntegration.registerBossHandler(new ColosseumIntegration.BossHandler() {
            @Override
            public boolean handles(ServerLevel level, ColosseumArena arena) {
                return STUB_BOSS_ARENAS.contains(arena.instanceId());
            }

            @Override
            public boolean start(ServerLevel level, ColosseumArena arena, UUID trialId, List<UUID> locked,
                                 ColosseumWorldPort.Binder bind) {
                Zombie boss = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
                if (boss == null) return false;
                BlockPos c = arena.geometry().center();
                boss.snapTo(c.getX() + 3.5D, c.getY(), c.getZ() + 0.5D, 0.0F, 0.0F);
                boss.setPersistenceRequired();
                boss.setCustomName(Component.literal("Stub Immortal Gladiator"));
                ColosseumOwnership.set(boss, ColosseumOwnership.Tag.of(arena.instanceId(), trialId, ColosseumRules.BOSS_ROUND,
                        ColosseumOwnership.Kind.BOSS, ColosseumOwnership.BOSS_ALLOCATION));
                bind.bind(boss.getUUID(), boss.chunkPosition().pack());
                if (level.addFreshEntity(boss)) return true;
                boss.discard();
                return false;
            }

            @Override
            public void onRemoved(ServerLevel level, ColosseumArena arena, UUID trialId, ColosseumWorldPort.RemovalCause cause) {
                BOSS_REMOVALS.add(arena.instanceId() + ":" + cause);
            }
        });
    }

    static int crystalEndings(ColosseumArena arena) {
        AtomicInteger n = CRYSTAL_ENDINGS.get(arena.instanceId());
        return n == null ? 0 : n.get();
    }

    static int bossVanishedEvents(ColosseumArena arena) {
        AtomicInteger n = BOSS_VANISHED.get(arena.instanceId());
        return n == null ? 0 : n.get();
    }

    static long removals(ColosseumArena arena, String cause) {
        return BOSS_REMOVALS.stream().filter((arena.instanceId() + ":" + cause)::equals).count();
    }

    /** The stub boss and any other BOSS-kind entity of this arena that is loaded. */
    static List<Mob> bosses(ServerLevel level, ColosseumArena arena) {
        BlockPos c = arena.geometry().center();
        return level.getEntitiesOfClass(Mob.class, new AABB(c).inflate(40.0D), mob -> {
            ColosseumOwnership.Tag tag = ColosseumOwnership.tag(mob);
            return mob.isAlive() && tag != null && tag.instance().equals(arena.instanceId())
                    && tag.kind() == ColosseumOwnership.Kind.BOSS;
        });
    }

    /** Shared Trial Key item entities of this arena that are loaded. */
    static List<ItemEntity> keyItems(ServerLevel level, ColosseumArena arena) {
        BlockPos c = arena.geometry().center();
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(8.0D), item -> {
            ColosseumOwnership.KeyTag tag = ColosseumOwnership.keyTag(item);
            return tag != null && tag.instance().equals(arena.instanceId());
        });
    }

    /**
     * Total Trial Keys lying near the arena center, counting stack sizes. Keys dropped at the same spot merge into one
     * stack like any item, so the number of key <em>entities</em> can be smaller than the number of keys issued.
     */
    static int keyCount(ServerLevel level, ColosseumArena arena) {
        BlockPos c = arena.geometry().center();
        int total = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(8.0D),
                i -> i.getItem().is(net.minecraft.world.item.Items.TRIAL_KEY))) total += item.getItem().getCount();
        return total;
    }

    /**
     * A small roofed platform at {@code at}: a mob parked there cannot burn in daylight and fall to its death, which
     * would corrupt a test that counts kills.
     */
    static void shelter(ServerLevel level, BlockPos at) {
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++) {
                level.setBlockAndUpdate(at.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState());
                level.setBlockAndUpdate(at.offset(dx, 3, dz), Blocks.STONE.defaultBlockState());
            }
    }

    /**
     * Plays R1-R6 through the real controller (killing every spawned mob, waiting out each intermission) until the
     * stub boss exists, then runs {@code onBoss} once. Works whether or not the controller is held, because it drives
     * the controller itself.
     */
    static void driveToBoss(GameTestHelper ctx, ColosseumArena arena, Runnable onBoss) {
        ServerLevel level = ctx.getLevel();
        BlockPos center = arena.geometry().center();
        STUB_BOSS_ARENAS.add(arena.instanceId());
        AtomicBoolean done = new AtomicBoolean();
        ctx.onEachTick(() -> {
            if (done.get()) return;
            ColosseumTrialState s = arena.state();
            switch (s.phase()) {
                case READY -> ColosseumIntegration.advanceController(level, center, 1); // a held controller does not start R1 itself
                case ROUND -> {
                    killAll(level, arena);
                    ColosseumIntegration.advanceController(level, center, 2);
                }
                case INTERMISSION -> ColosseumIntegration.advanceController(level, center, Math.max(1, s.intermissionRemaining()));
                case BOSS -> {
                    if (!s.boss().started()) {
                        ColosseumIntegration.advanceController(level, center, 1);
                        return;
                    }
                    done.set(true);
                    onBoss.run();
                }
                default -> {}
            }
        });
    }

    /** A fresh arena center, unique per test, high above terrain. */
    static BlockPos freshCenter() {
        int slot = NEXT_SLOT.getAndIncrement();
        return new BlockPos(RUN_BASE_X + slot * 160, 200, 24_000);
    }

    static void forceChunks(ServerLevel level, BlockPos center, boolean forced) {
        for (int cx = (center.getX() - 32) >> 4; cx <= (center.getX() + 32) >> 4; cx++)
            for (int cz = (center.getZ() - 32) >> 4; cz <= (center.getZ() + 32) >> 4; cz++)
                level.setChunkForced(cx, cz, forced);
    }

    /** Smooth-stone floor (radius {@code floorRadius}, optionally only inside {@code innerOnly}), stone roof 6 up. */
    static void buildFloor(ServerLevel level, BlockPos center, int floorRadius, boolean roof) {
        for (int dx = -27; dx <= 27; dx++)
            for (int dz = -27; dz <= 27; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 27 * 27) continue;
                level.setBlockAndUpdate(center.offset(dx, -1, dz), d2 <= floorRadius * floorRadius
                        ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                if (roof) level.setBlockAndUpdate(center.offset(dx, 6, dz), Blocks.STONE.defaultBlockState());
                if (roof && d2 > 26 * 26) // perimeter wall keeps mobs on the platform (falls would count as kills)
                    for (int dy = 0; dy < 6; dy++) level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
            }
    }

    /** Builds the floor and registers a fresh test arena (the per-run random base keeps every center unused). */
    static ColosseumArena arena(GameTestHelper ctx, BlockPos center, int floorRadius) {
        ServerLevel level = ctx.getLevel();
        forceChunks(level, center, true);
        buildFloor(level, center, floorRadius, true);
        return ColosseumIntegration.registerArena(level, ColosseumArenaGeometry.testFixture(center));
    }

    static ServerPlayer player(GameTestHelper ctx, String name, double x, double y, double z) {
        return player(ctx, UUID.randomUUID(), name, x, y, z);
    }

    static ServerPlayer player(GameTestHelper ctx, UUID id, String name, double x, double y, double z) {
        var server = ctx.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(id, name), false);
        ServerPlayer player = new ServerPlayer(server, ctx.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = true; // trial mobs fight; tests decide deaths explicitly
        player.teleportTo(ctx.getLevel(), x, y, z, Set.of(), 0.0F, 0.0F, false);
        return player;
    }

    static ServerPlayer playerAtCenter(GameTestHelper ctx, BlockPos center, String name) {
        return player(ctx, name, center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
    }

    static void moveTo(ServerPlayer player, double x, double y, double z) {
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), 0.0F, 0.0F, false);
    }

    /**
     * Ends a test: the arena is archived (its ledger is kept, never deleted), the players leave and the chunks are
     * released. Archiving is refused while a boss vanish or restore entry is unfinished, which fails the test.
     */
    static void finish(GameTestHelper ctx, BlockPos center, List<ServerPlayer> players) {
        ServerLevel level = ctx.getLevel();
        var result = ColosseumIntegration.archiveAndUnregister(level, center);
        ctx.assertTrue(result == ColosseumIntegration.UnregisterResult.REMOVED
                || result == ColosseumIntegration.UnregisterResult.NOT_FOUND, "arena cleanup was " + result);
        for (ServerPlayer player : players) level.getServer().getPlayerList().remove(player);
        forceChunks(level, center, false);
        ctx.succeed();
    }

    /** Living entities of this arena carrying an ownership tag (ordinary mobs and any stub boss). */
    static List<Mob> trialMobs(ServerLevel level, ColosseumArena arena) {
        BlockPos c = arena.geometry().center();
        return level.getEntitiesOfClass(Mob.class, new AABB(c).inflate(40.0D), mob -> {
            ColosseumOwnership.Tag tag = ColosseumOwnership.tag(mob);
            return mob.isAlive() && tag != null && tag.instance().equals(arena.instanceId())
                    && tag.kind() == ColosseumOwnership.Kind.MOB;
        });
    }

    static void killAll(ServerLevel level, ColosseumArena arena) {
        for (Mob mob : trialMobs(level, arena)) mob.kill(level);
    }

    static int items(ServerLevel level, BlockPos center) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(center).inflate(30.0D)).size();
    }

    static int waves(ColosseumArena arena) {
        AtomicInteger count = WAVES.get(arena.key());
        return count == null ? 0 : count.get();
    }
}
