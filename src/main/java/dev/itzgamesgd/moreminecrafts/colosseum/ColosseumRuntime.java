package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.colosseum.ArenaAttempt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Server-side glue between the constructed arena and the pure {@link ArenaController}: per-tick interior detection, spawning with warnings,
 * keys, supply chests, cleanup, and reconciliation of persisted allocations with the real entities. Trial mobs carry two tags
 * (attempt and allocation) so a crash between spawning and saving can never produce a duplicate or an orphan that is silently adopted.
 */
public final class ColosseumRuntime {
    private ColosseumRuntime() {}

    public static final String TAG_ATTEMPT = "mm_st05_attempt:", TAG_ALLOC = "mm_st05_alloc:";
    /** Test-only: shortens the wall-clock limit / grace (system properties, never set in production). */
    private static final String PROP_LIMIT = "moreminecrafts.st05.limitMillis", PROP_GRACE = "moreminecrafts.st05.graceMillis";

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(ColosseumRuntime::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> onDeath(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> onUnload(entity, level));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> onLoad(entity));
        ColosseumCommand.initialize();
    }

    public static ColosseumData data(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(ColosseumData.TYPE); }

    /** Production hook for a verified Stage 0-F construction: registers the arena (idempotent). */
    public static boolean registerArena(MinecraftServer server, String id, int cx, int cz, int floorY) {
        ColosseumData d = data(server);
        boolean added = d.register(id, cx, cz, floorY);
        if (added) d.controller(id).setTiming(timingFromProperties());
        return added;
    }

    static RoundPlan.Timing timingFromProperties() {
        long limit = Long.getLong(PROP_LIMIT, RoundPlan.LIMIT_MILLIS), grace = Long.getLong(PROP_GRACE, RoundPlan.GRACE_MILLIS);
        return new RoundPlan.Timing(limit, grace);
    }

    // ------------------------------------------------------------------ events
    private static boolean tracked(Entity e) { return e.entityTags().stream().anyMatch(t -> t.startsWith(TAG_ALLOC)); }

    private static void onDeath(net.minecraft.world.entity.LivingEntity entity) {
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) return;
        ColosseumData d = data(level.getServer());
        if (d.sites().isEmpty()) return;
        if (entity instanceof ServerPlayer p) {
            for (String id : d.sites().keySet()) d.controller(id).onPlayerDeath(p.getUUID());
        } else if (tracked(entity)) {
            for (String id : d.sites().keySet()) d.controller(id).onMobKilled(entity.getUUID());
        }
    }

    private static void onUnload(Entity entity, ServerLevel level) {
        if (!tracked(entity)) return;
        var reason = entity.getRemovalReason();
        // Only real losses count; a chunk unload is never a loss (Q111-A) and never causes a duplicate spawn.
        if (reason == Entity.RemovalReason.DISCARDED || reason == Entity.RemovalReason.CHANGED_DIMENSION) {
            ColosseumData d = data(level.getServer());
            for (String id : d.sites().keySet()) d.controller(id).onMobLost(entity.getUUID());
        }
    }

    private static void onLoad(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !tracked(entity)) return;
        ColosseumData d = data(level.getServer());
        for (String t : entity.entityTags()) {
            if (!t.startsWith(TAG_ATTEMPT)) continue;
            UUID attempt = UUID.fromString(t.substring(TAG_ATTEMPT.length()));
            for (String id : d.sites().keySet()) if (d.controller(id).cleanupAttempts().contains(attempt)) { entity.discard(); return; }
        }
    }

    // ------------------------------------------------------------------ tick
    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ColosseumData d = data(server);
        if (d.sites().isEmpty()) return;
        long now = ColosseumData.clock.getAsLong();
        for (ColosseumData.Site site : d.sites().values()) tickSite(level, d, site, now);
    }

    static void tickSite(ServerLevel level, ColosseumData d, ColosseumData.Site site, long now) {
        ArenaController ctl = d.controller(site.id());
        ArenaGeometry g = site.geometry();
        Set<UUID> inside = new HashSet<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            BlockPos b = p.blockPosition();
            if (g.contains(b.getX(), b.getY(), b.getZ())) inside.add(p.getUUID());
        }
        boolean wasActive = ctl.active();
        boolean loaded = !ctl.active() || entitiesLoaded(level, g);
        if (ctl.active() && loaded) reconcile(level, ctl);
        List<Action> actions = ctl.tick(now, inside, loaded, alloc -> position(level, g, alloc));
        if (ctl.active() && ctl.attempt() != null) for (Alloc a : ctl.attempt().allocations()) if (a.status == AllocStatus.WARNING && a.warnLeft % 4 == 0)
            level.sendParticles(ParticleTypes.END_ROD, a.px + 0.5, a.py + 0.2, a.pz + 0.5, 3, 0.2, 0.4, 0.2, 0.01);
        for (Action a : actions) execute(level, site, ctl, a);
        if (wasActive || ctl.active() || !actions.isEmpty()) d.setDirty();
    }

    static boolean entitiesLoaded(ServerLevel level, ArenaGeometry g) {
        int minX = (g.cx() - ArenaGeometry.EXT_R) >> 4, maxX = (g.cx() + ArenaGeometry.EXT_R) >> 4;
        int minZ = (g.cz() - ArenaGeometry.EXT_R) >> 4, maxZ = (g.cz() + ArenaGeometry.EXT_R) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) if (!level.areEntitiesLoaded(ChunkPos.pack(x, z))) return false;
        return true;
    }

    /** Spawned allocations whose entity is confirmed gone (all arena entities loaded, no entity) are real losses and return to pending. */
    static void reconcile(ServerLevel level, ArenaController ctl) {
        ArenaAttempt a = ctl.attempt();
        if (a == null) return;
        for (Alloc al : a.allocations()) {
            if (al.status != AllocStatus.SPAWNED || al.entity == null) continue;
            Entity e = level.getEntity(al.entity);
            if (e == null || !e.isAlive()) ctl.onMobLost(al.entity);
        }
    }

    // ------------------------------------------------------------------ spawn position (12-24 annulus around the fixed centre)
    static int[] position(ServerLevel level, ArenaGeometry g, Alloc alloc) {
        Random rng = new Random(alloc.id.getLeastSignificantBits() ^ level.getGameTime() * 0x9E3779B97F4A7C15L);
        for (int i = 0; i < 24; i++) {
            double ang = rng.nextDouble() * Math.PI * 2, r = ArenaGeometry.SPAWN_MIN_R + rng.nextDouble() * (ArenaGeometry.SPAWN_MAX_R - ArenaGeometry.SPAWN_MIN_R);
            int x = g.cx() + (int) Math.round(Math.cos(ang) * r), z = g.cz() + (int) Math.round(Math.sin(ang) * r);
            if (!g.annulus(x, z) || !level.hasChunkAt(new BlockPos(x, g.floorY(), z))) continue;
            if (spawnable(level, new BlockPos(x, g.floorY(), z))) return new int[] {x, g.floorY(), z};
        }
        return null;
    }

    /** Solid supporting floor, 3x3x3 clearance (no wall/hazard intersection), no fluid, fire or magma. */
    static boolean spawnable(ServerLevel level, BlockPos feet) {
        BlockState below = level.getBlockState(feet.below());
        if (!below.isFaceSturdy(level, feet.below(), Direction.UP) || below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) return false;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = feet.offset(dx, dy, dz);
            BlockState s = level.getBlockState(p);
            if (!s.getCollisionShape(level, p).isEmpty() || !s.getFluidState().isEmpty() || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ action execution
    static void execute(ServerLevel level, ColosseumData.Site site, ArenaController ctl, Action action) {
        ArenaAttempt attempt = ctl.attempt();
        double cx = site.cx() + 0.5, cz = site.cz() + 0.5;
        switch (action) {
            case RoundStart rs -> {
                MoreMinecrafts.LOGGER.info("ST05 arena {} attempt {} round {} start n={} budget={}", site.id(), attempt.attemptId, rs.round(), rs.n(), rs.budget());
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, site.floorY() + 1.5, cz, 40, 1.5, 0.8, 1.5, 0.02);
            }
            case WarnSpawn w -> { }
            case SpawnNow s -> spawn(level, site, attempt, s.alloc());
            case DropKey k -> {
                ItemStack key = new ItemStack(k.ominous() ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY);
                level.addFreshEntity(new ItemEntity(level, cx, site.floorY() + 1.0, cz, key));
                MoreMinecrafts.LOGGER.info("ST05 arena {} attempt {} wave {} key awarded", site.id(), attempt.attemptId, k.round());
            }
            case SupplyChests c -> placeSupplies(level, site, attempt, c);
            case BossStart b -> {
                ColosseumBossHook.Provider p = ColosseumBossHook.provider();
                if (p == null || !p.start(level, site, attempt, b.maxHp())) attempt.bossRequested = false;   // retried next tick
            }
            case End e -> end(level, site, ctl, attempt, e);
        }
    }

    private static Optional<Mob> create(ServerLevel level, String rosterId) {
        boolean baby = rosterId.endsWith("#baby");
        Identifier id = Identifier.parse(baby ? rosterId.substring(0, rosterId.indexOf('#')) : rosterId);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) return Optional.empty();
        Entity e = type.create(level, EntitySpawnReason.EVENT);
        if (!(e instanceof Mob mob)) return Optional.empty();
        if (baby && mob instanceof Zombie z) z.setBaby(true);
        return Optional.of(mob);
    }

    static void spawn(ServerLevel level, ColosseumData.Site site, ArenaAttempt attempt, Alloc alloc) {
        String allocTag = TAG_ALLOC + alloc.id;
        // Crash-window guard: an entity already carrying this allocation's tag is adopted instead of spawning a second one.
        AABB box = new AABB(site.cx() - 40, site.floorY() - 5, site.cz() - 40, site.cx() + 40, site.floorY() + 30, site.cz() + 40);
        List<Mob> existing = level.getEntitiesOfClass(Mob.class, box, m -> m.entityTags().contains(allocTag));
        if (!existing.isEmpty()) { attempt.spawnResult(alloc, existing.get(0).getUUID()); return; }
        Optional<Mob> created = create(level, alloc.mob);
        if (created.isEmpty()) { attempt.spawnResult(alloc, null); MoreMinecrafts.LOGGER.warn("ST05 cannot create {}", alloc.mob); return; }
        Mob mob = created.get();
        BlockPos feet = new BlockPos(alloc.px, alloc.py, alloc.pz);
        if (!spawnable(level, feet)) { mob.discard(); attempt.spawnResult(alloc, null); return; }   // allocation stays pending, retried by the normal cycle
        mob.snapTo(alloc.px + 0.5, alloc.py, alloc.pz + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.addTag(TAG_ATTEMPT + attempt.attemptId);
        mob.addTag(allocTag);
        mob.setPersistenceRequired();
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || !site.geometry().contains(p.blockPosition().getX(), p.blockPosition().getY(), p.blockPosition().getZ())) continue;
            double dist = p.distanceToSqr(mob);
            if (dist < best) { best = dist; nearest = p; }
        }
        if (nearest != null) mob.setTarget(nearest);   // spawn-correction seeding only; native AI does the rest
        if (!level.addFreshEntity(mob)) { attempt.spawnResult(alloc, null); return; }
        attempt.spawnResult(alloc, mob.getUUID());
    }

    static void placeSupplies(ServerLevel level, ColosseumData.Site site, ArenaAttempt attempt, SupplyChests c) {
        Random rng = new Random(attempt.attemptId.getLeastSignificantBits() ^ c.round());
        int placed = 0;
        for (int i = 0; i < 400 && placed < c.count(); i++) {
            double ang = rng.nextDouble() * Math.PI * 2, r = 6 + rng.nextDouble() * 18;
            BlockPos pos = new BlockPos(site.cx() + (int) Math.round(Math.cos(ang) * r), site.floorY(), site.cz() + (int) Math.round(Math.sin(ang) * r));
            if (!level.hasChunkAt(pos) || !spawnable(level, pos) || attempt.supplyChests.contains(pos.asLong())) continue;
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                chest.setItem(0, new ItemStack(Items.COOKED_BEEF, 8));
                chest.setItem(1, new ItemStack(Items.ARROW, 16));
                chest.setItem(2, new ItemStack(Items.GOLDEN_APPLE, 1));
                chest.setChanged();
            }
            attempt.supplyChests.add(pos.asLong());
            placed++;
        }
    }

    static void end(ServerLevel level, ColosseumData.Site site, ArenaController ctl, ArenaAttempt attempt, End e) {
        MoreMinecrafts.LOGGER.info("ST05 arena {} attempt {} ended {} at round {}", site.id(), attempt.attemptId, e.outcome(), attempt.round());
        if (e.outcome() == ArenaAttempt.Outcome.TIMED_OUT) {
            ColosseumBossHook.Provider p = ColosseumBossHook.provider();
            if (p != null) p.vanish(level, site, attempt);
        }
        if (!e.cleanup()) return;
        // Remove only this trial's own mobs (no loot, no XP) and its generated supply chests; never unrelated entities or blocks.
        String tag = TAG_ATTEMPT + attempt.attemptId;
        AABB box = new AABB(site.cx() - 40, site.floorY() - 5, site.cz() - 40, site.cx() + 40, site.floorY() + 30, site.cz() + 40);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, m -> m.entityTags().contains(tag))) m.discard();
        for (long packed : new ArrayList<>(attempt.supplyChests)) {
            BlockPos pos = BlockPos.of(packed);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.CHEST)) {
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
        attempt.supplyChests.clear();
    }
}
