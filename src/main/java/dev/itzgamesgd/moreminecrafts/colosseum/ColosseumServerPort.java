package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** {@link ColosseumWorldPort} for a real server level and one registered arena. */
public final class ColosseumServerPort implements ColosseumWorldPort {
    private static final int RANDOM_SPAWN_ATTEMPTS = 48;
    private static final Set<Long> WARNED_NO_BOSS = Collections.synchronizedSet(new LinkedHashSet<>());

    private final ServerLevel level;
    private final ColosseumArena arena;
    private final ColosseumArenaGeometry geometry;

    public ColosseumServerPort(ServerLevel level, ColosseumArena arena) {
        this.level = level;
        this.arena = arena;
        this.geometry = arena.geometry();
    }

    /** Roster entry to entity type. Baby Zombie is a Zombie variant, not its own type. */
    public static EntityType<? extends Mob> entityType(ColosseumMob mob) {
        return switch (mob) {
            case ZOMBIE, BABY_ZOMBIE -> EntityTypes.ZOMBIE;
            case HUSK -> EntityTypes.HUSK;
            case SKELETON -> EntityTypes.SKELETON;
            case BOGGED -> EntityTypes.BOGGED;
            case STRAY -> EntityTypes.STRAY;
            case BREEZE -> EntityTypes.BREEZE;
            case CAVE_ZOMBIE -> CustomMobCatalog.CAVE_ZOMBIE;
            case CAVE_SKELETON -> CustomMobCatalog.CAVE_SKELETON;
            case BANDIT -> CustomMobCatalog.BANDIT;
            case BANDIT_ARCHER -> CustomMobCatalog.BANDIT_ARCHER;
            case LARGE_ZOMBIE -> CustomMobCatalog.LARGE_ZOMBIE;
            case LESSER_WITHER_SKELETON -> CustomMobCatalog.LESSER_WITHER_SKELETON;
            case ARMORED_SKELETON -> CustomMobCatalog.ARMORED_SKELETON;
            case BANDIT_WARRIOR -> CustomMobCatalog.BANDIT_WARRIOR;
        };
    }

    /** Fixes the zombie-family baby roll and suppresses chicken jockeys so the allocated roster entry is exact. */
    @Nullable private static SpawnGroupData groupData(ColosseumMob mob, Mob entity) {
        if (entity instanceof Zombie) return new Zombie.ZombieGroupData(mob == ColosseumMob.BABY_ZOMBIE, false);
        return null;
    }

    public static boolean eligiblePlayer(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    @Override
    public Set<UUID> playersInCombatZone() {
        Set<UUID> present = new LinkedHashSet<>();
        for (ServerPlayer player : level.players())
            if (eligiblePlayer(player) && geometry.inCombatZone(player.getX(), player.getY(), player.getZ()))
                present.add(player.getUUID());
        return present;
    }

    @Override
    public boolean playerAtActivation() {
        for (ServerPlayer player : level.players())
            if (eligiblePlayer(player) && geometry.atActivation(player.getX(), player.getY(), player.getZ())) return true;
        return false;
    }

    @Override
    public long findSpawnPosition(ColosseumMob mob, Set<Long> reserved, Random random) {
        EntityType<? extends Mob> type = entityType(mob);
        BlockPos center = geometry.center();
        double inner = ColosseumRules.SPAWN_INNER_RADIUS, outer = ColosseumRules.SPAWN_OUTER_RADIUS;
        for (int attempt = 0; attempt < RANDOM_SPAWN_ATTEMPTS; attempt++) {
            double radius = Math.sqrt(inner * inner + random.nextDouble() * (outer * outer - inner * inner));
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = (int) Math.floor(center.getX() + 0.5D + radius * Math.cos(angle));
            int z = (int) Math.floor(center.getZ() + 0.5D + radius * Math.sin(angle));
            long found = validInColumn(type, x, z, reserved);
            if (found != ColosseumTrialState.NO_POSITION) return found;
        }
        // Exhaustive scan: decides "no valid position anywhere" for Q110 rather than trusting random misses.
        List<int[]> columns = new ArrayList<>();
        for (int dx = -ColosseumRules.SPAWN_OUTER_RADIUS - 1; dx <= ColosseumRules.SPAWN_OUTER_RADIUS + 1; dx++)
            for (int dz = -ColosseumRules.SPAWN_OUTER_RADIUS - 1; dz <= ColosseumRules.SPAWN_OUTER_RADIUS + 1; dz++)
                columns.add(new int[]{center.getX() + dx, center.getZ() + dz});
        Collections.shuffle(columns, random);
        long reservedFallback = ColosseumTrialState.NO_POSITION;
        for (int[] column : columns) {
            long found = validInColumn(type, column[0], column[1], reserved);
            if (found != ColosseumTrialState.NO_POSITION) return found;
            if (reservedFallback == ColosseumTrialState.NO_POSITION)
                reservedFallback = validInColumn(type, column[0], column[1], Set.of());
        }
        return reservedFallback;
    }

    private long validInColumn(EntityType<? extends Mob> type, int x, int z, Set<Long> reserved) {
        for (int y = geometry.spawnMinY(); y <= geometry.spawnMaxY(); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!reserved.contains(pos.asLong()) && isValid(type, pos)) return pos.asLong();
        }
        return ColosseumTrialState.NO_POSITION;
    }

    @Override
    public boolean isValidSpawnPosition(ColosseumMob mob, long pos) {
        return isValid(entityType(mob), BlockPos.of(pos));
    }

    /**
     * Inside the annulus and spawn band, loaded, inside the world border, on a sturdy floor, with collision clearance
     * for the entity's box, and no liquid or block that is dangerous to the entity (fire, lava, magma, cactus, berry
     * bush, wither rose, powder snow) in its box or floor.
     */
    private boolean isValid(EntityType<? extends Mob> type, BlockPos pos) {
        if (!geometry.inSpawnAnnulus(pos)) return false;
        BlockPos floor = pos.below();
        if (!level.isLoaded(pos) || !level.isLoaded(floor) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        BlockState floorState = level.getBlockState(floor);
        if (!floorState.isFaceSturdy(level, floor, Direction.UP) || type.isBlockDangerous(floorState)) return false;
        AABB box = type.getSpawnAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (!level.noCollision(box) || level.containsAnyLiquid(box)) return false;
        return level.getBlockStatesIfLoaded(box).noneMatch(type::isBlockDangerous);
    }

    @Override
    public boolean spawn(UUID trialId, ColosseumTrialState.Allocation allocation, long pos, Binder bind) {
        EntityType<? extends Mob> type = entityType(allocation.mob());
        Mob mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) return false;
        BlockPos at = BlockPos.of(pos);
        mob.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, groupData(allocation.mob(), mob));
        mob.setPersistenceRequired();
        ColosseumOwnership.set(mob, new ColosseumOwnership.Tag(arena.key(), trialId, allocation.index()));
        ServerPlayer target = nearestPresentPlayer(mob);
        if (target != null) mob.setTarget(target); // Q109 spawn correction only; native AI keeps targeting
        bind.bind(mob.getUUID(), ChunkPos.pack(at));
        if (!level.addFreshEntity(mob)) {
            mob.discard();
            return false;
        }
        return true;
    }

    @Nullable private ServerPlayer nearestPresentPlayer(Mob mob) {
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (!eligiblePlayer(player) || !geometry.inCombatZone(player.getX(), player.getY(), player.getZ())) continue;
            double distance = player.distanceToSqr(mob);
            if (distance < best) { best = distance; nearest = player; }
        }
        return nearest;
    }

    @Override
    public Lookup locate(UUID entity, long lastKnownChunk) {
        Entity found = level.getEntity(entity);
        if (found != null && !found.isRemoved()) return new Lookup(Presence.ALIVE, found.chunkPosition().pack());
        return new Lookup(level.areEntitiesLoaded(lastKnownChunk) ? Presence.GONE : Presence.NOT_LOADED, lastKnownChunk);
    }

    @Override
    public boolean discard(UUID entity) {
        Entity found = level.getEntity(entity);
        if (found == null) return false;
        found.discard(); // no death: no loot, no XP
        return true;
    }

    @Override
    public void warningEffect(long pos) {
        if (level.getGameTime() % 4 != 0) return;
        BlockPos at = BlockPos.of(pos);
        level.sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, at.getX() + 0.5D, at.getY() + 0.3D,
                at.getZ() + 0.5D, 3, 0.3D, 0.4D, 0.3D, 0.0D);
    }

    @Override
    public void activationEffect() {
        BlockPos at = geometry.center();
        MoreMinecrafts.LOGGER.info("ST-05 arena {} activated by {}.", at.toShortString(), level.players().stream()
                .filter(p -> eligiblePlayer(p) && geometry.atActivation(p.getX(), p.getY(), p.getZ()))
                .map(p -> p.getGameProfile().name() + "@" + p.blockPosition().toShortString()).toList());
        level.sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, at.getX() + 0.5D, at.getY() + 0.5D,
                at.getZ() + 0.5D, 40, 1.2D, 0.8D, 1.2D, 0.02D);
    }

    @Override
    public void waveCleared(ColosseumTrialState.WaveRecord record) {
        MoreMinecrafts.LOGGER.info("ST-05 arena {} trial {} cleared round {} ({} locked participants).",
                geometry.center().toShortString(), record.trialId(), record.round(), record.lockedParticipants().size());
        ColosseumIntegration.WAVE_CLEARED.invoker().onWaveCleared(level, arena, record);
    }

    @Override
    public List<Long> placeSupplies(UUID trialId, int clearedRound, int count) {
        ColosseumIntegration.SupplyHandler handler = ColosseumIntegration.supplyHandler(level, arena);
        if (handler == null) return List.of();
        List<Long> positions = new ArrayList<>();
        for (BlockPos pos : handler.place(level, arena, trialId, clearedRound, count)) positions.add(pos.asLong());
        return positions;
    }

    @Override
    public void cleanupSupplies(UUID trialId, List<Long> positions) {
        ColosseumIntegration.SupplyHandler handler = ColosseumIntegration.supplyHandler(level, arena);
        if (handler == null || positions.isEmpty()) return;
        handler.cleanup(level, arena, trialId, positions.stream().map(BlockPos::of).toList());
    }

    @Override
    public boolean startBoss(UUID trialId, List<UUID> lockedParticipants) {
        ColosseumIntegration.BossHandler handler = ColosseumIntegration.bossHandler(level, arena);
        if (handler == null) {
            if (WARNED_NO_BOSS.add(arena.key()))
                MoreMinecrafts.LOGGER.warn("ST-05 arena {} reached R7 but no Immortal Gladiator integration exists yet; "
                        + "the trial waits in BOSS.", geometry.center().toShortString());
            return false;
        }
        return handler.start(level, arena, trialId, lockedParticipants);
    }

    @Override
    public void cleanupBoss(UUID trialId) {
        ColosseumIntegration.BossHandler handler = ColosseumIntegration.bossHandler(level, arena);
        if (handler != null) handler.cleanup(level, arena, trialId);
    }

    @Override
    public void trialEnded(UUID trialId, ColosseumTrialState.Outcome outcome) {
        MoreMinecrafts.LOGGER.info("ST-05 arena {} trial {} ended: {}; cooldown {} ticks.",
                geometry.center().toShortString(), trialId, outcome, ColosseumRules.COOLDOWN_TICKS);
        ColosseumIntegration.TRIAL_ENDED.invoker().onTrialEnded(level, arena, trialId, outcome);
    }

    @Override
    public long gameTime() { return level.getGameTime(); }

    @Override
    public long nextSeed() { return level.getRandom().nextLong(); }
}
