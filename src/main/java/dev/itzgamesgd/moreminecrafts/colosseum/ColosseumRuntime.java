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

    public static final String TAG_BOSS = "mm_st05_boss", TAG_PROJECTILE="mm_st05_projectile", TAG_REWARD="mm_st05_reward:";
    public static final String TAG_ATTEMPT = "mm_st05_attempt:", TAG_ALLOC = "mm_st05_alloc:";
    /** Test-only: shortens the wall-clock limit / grace (system properties, never set in production). */
    private static final String PROP_LIMIT = "moreminecrafts.st05.limitMillis", PROP_GRACE = "moreminecrafts.st05.graceMillis";

    public static void initialize() {
        ArenaChunkTickets.initialize();
        ArenaNaturalBlocks.initialize();
        ArenaSupplyOwnership.initialize();
        GladiatorEntities.initialize();
        ColosseumBossHook.register(new GladiatorBossProvider());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ArenaChunkTickets.clear(server.overworld()); ArenaRecovery.close(server);
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !combatFrozen(entity)
                && (source.getEntity()==null || !combatFrozen(source.getEntity()))
                && (source.getDirectEntity()==null || !combatFrozen(source.getDirectEntity())));
        ServerTickEvents.END_SERVER_TICK.register(ColosseumRuntime::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> onDeath(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> onUnload(entity, level));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> onLoad(entity));
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel) onPlayerBlockRemoved(serverLevel, pos);
        });
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

    /** Registers an arena with source structure template positions recorded into the structure mask. */
    public static boolean registerArena(MinecraftServer server, String id, int cx, int cz, int floorY, java.util.Collection<BlockPos> templatePositions) {
        boolean added = registerArena(server, id, cx, cz, floorY);
        ColosseumData d = data(server);
        ArenaController c = d.controller(id);
        if (c != null && templatePositions != null) {
            c.recordStructureMask(templatePositions);
            d.setDirty();
        }
        return added;
    }

    /** Records a player block placement in arenas covering this position. */
    public static void onPlayerPlaced(ServerLevel level, BlockPos pos) {
        ColosseumData d = data(level.getServer());
        for (ColosseumData.Site site : d.sites().values()) {
            if (site.geometry().contains(pos.getX(), pos.getY(), pos.getZ())) {
                ArenaController c = d.controller(site.id());
                if (c != null && c.protectBlock(pos.immutable())) {
                    ArenaRecovery.save(level.getServer(), c, false);
                    d.setDirty();
                }
            }
        }
    }

    /** Releases a player block placement protection when broken. */
    public static void onPlayerBlockRemoved(ServerLevel level, BlockPos pos) {
        ColosseumData d = data(level.getServer());
        for (ColosseumData.Site site : d.sites().values()) {
            ArenaController c = d.controller(site.id());
            if (c != null && c.releaseBlock(pos)) {
                ArenaRecovery.save(level.getServer(), c, false);
                d.setDirty();
            }
        }
    }

    static RoundPlan.Timing timingFromProperties() {
        long limit = Long.getLong(PROP_LIMIT, RoundPlan.LIMIT_MILLIS), grace = Long.getLong(PROP_GRACE, RoundPlan.GRACE_MILLIS);
        return new RoundPlan.Timing(limit, grace);
    }

    // ------------------------------------------------------------------ events
    private static boolean tracked(Entity e) { return e.entityTags().contains(TAG_BOSS) || e.entityTags().contains(TAG_PROJECTILE) || e.entityTags().stream().anyMatch(t -> t.startsWith(TAG_ALLOC)); }

    private static void onDeath(net.minecraft.world.entity.LivingEntity entity) {
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) return;
        ColosseumData d = data(level.getServer());
        if (d.sites().isEmpty()) return;
        if (entity instanceof ServerPlayer p) {
            for (String id : d.sites().keySet()) d.controller(id).onPlayerDeath(p.getUUID());
        } else if (entity instanceof ImmortalGladiator boss) {
            for (String id : d.sites().keySet()) {
                var c=d.controller(id);var a=c.attempt();
                if(c.active() && entity.getUUID().equals(a.bossEntity) && a.attemptId.equals(boss.attemptId()) && !combatFrozen(entity)) {
                    a.bossDeathX=entity.getX();a.bossDeathY=entity.getY();a.bossDeathZ=entity.getZ();
                    a.onBossDefeated();d.setDirty();
                }
            }
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
        if (!(entity.level() instanceof ServerLevel level)) return;
        inheritProjectileOwner(entity);
        if(!tracked(entity))return;
        ColosseumData d = data(level.getServer());
        for (String t : entity.entityTags()) {
            if (!t.startsWith(TAG_ATTEMPT)) continue;
            UUID attempt;
            try { attempt=UUID.fromString(t.substring(TAG_ATTEMPT.length())); } catch(IllegalArgumentException e) { continue; }
            for (String id : d.sites().keySet()) {
                var c=d.controller(id);
                if(c.cleanupAttempts().contains(attempt) || (entity instanceof ImmortalGladiator && c.attempt()!=null && c.attempt().attemptId.equals(attempt) && c.attempt().outcome()==Outcome.TIMED_OUT)) { entity.discard(); return; }
            }
        }
    }

    // ------------------------------------------------------------------ tick
    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ColosseumData d = data(server);
        if (d.sites().isEmpty()) return;
        long now = ColosseumData.clock.getAsLong();
        for(String id:d.sites().keySet()) ArenaRecovery.open(server,d,id,d.controller(id).now(now));
        ArenaChunkTickets.update(level,d);
        for (ColosseumData.Site site : d.sites().values()) tickSite(level, d, site, d.controller(site.id()).now(now));
        ArenaChunkTickets.update(level,d);
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
        boolean hadCooldown=ctl.cooldownLeft()>0, hadCleanup=ctl.attempt()!=null && ctl.attempt().cleanupPending;
        boolean loaded = !ctl.active() || entitiesLoaded(level, g) && allocationChunksLoaded(level,ctl.attempt());
        if (ctl.active() && loaded) reconcile(level, ctl);
        if(ctl.quarantined()) {d.setDirty();return;}
        if(entitiesLoaded(level,g) && !reconcileRewards(level,site,ctl)) {d.setDirty();return;}
        List<Action> actions = ctl.tick(now, inside, loaded, alloc -> position(level, g, alloc));
        if (ctl.active() && ctl.attempt() != null) for (Alloc a : ctl.attempt().allocations()) if (a.status == AllocStatus.WARNING && a.warnLeft % 4 == 0)
            level.sendParticles(ParticleTypes.END_ROD, a.px + 0.5, a.py + 0.2, a.pz + 0.5, 3, 0.2, 0.4, 0.2, 0.01);
        if(!actions.isEmpty()) {
            if(ctl.attempt().ended()) ctl.attempt().cleanupPending=true;
            for(Action action:actions)if(action instanceof DropKey k) {
                RewardDelivery delivery=ctl.rewardDeliveries.get(k.rewardId());
                if(delivery==null || !delivery.matches(ctl.attempt().attemptId,k.round(),k.ominous())) {ctl.isolate("missing or mismatched shared reward plan");return;}
                double angle=2.0*Math.PI*(k.round()-1)/(RoundPlan.ROUNDS-1.0);
                double x=k.ominous()?ctl.attempt().bossDeathX:site.cx()+0.5+3.0*Math.cos(angle);
                double y=k.ominous()?ctl.attempt().bossDeathY:site.floorY()+1.0;
                double z=k.ominous()?ctl.attempt().bossDeathZ:site.cz()+0.5+3.0*Math.sin(angle);
                if(!delivery.positionKnown)delivery.position(x,y,z);
                else if(delivery.x!=x || delivery.y!=y || delivery.z!=z) {ctl.isolate("shared reward position changed after planning");return;}
            }
            if(ctl.attempt().ended() ? !ArenaRecovery.saveCleanup(level.getServer(),ctl) : !ArenaRecovery.save(level.getServer(),ctl,true))return;
            // Keep the existing world-effect fence around all non-reward actions. Reward IDs have their own
            // write-ahead state machine, so finish those world effects before clearing the generic fence.
            for (Action a : actions) if (!(a instanceof DropKey)) { execute(level, site, ctl, a); if(ctl.quarantined())break; }
            if(!ctl.quarantined() && !ArenaRecovery.save(level.getServer(),ctl,false))return;
            if(!ctl.quarantined()) for (Action a : actions) if (a instanceof DropKey k) {
                RewardDelivery delivery=ctl.rewardDeliveries.get(k.rewardId());
                if(delivery==null || !delivery.matches(ctl.attempt().attemptId,k.round(),k.ominous())) {ctl.isolate("missing or mismatched shared reward plan");break;}
                if(!deliverReward(level,site,ctl,delivery))break;
            }
        }
        if(ctl.attempt()!=null && ctl.attempt().cleanupPending && !ctl.quarantined()) finishCleanup(level,site,ctl);
        if(!ctl.quarantined() && (ctl.active() || wasActive || hadCooldown || hadCleanup || !actions.isEmpty())) ArenaRecovery.save(level.getServer(),ctl,false);
        if (wasActive || ctl.active() || !actions.isEmpty()) d.setDirty();
    }

    static boolean entitiesLoaded(ServerLevel level, ArenaGeometry g) {
        int minX = (g.cx() - ArenaGeometry.EXT_R) >> 4, maxX = (g.cx() + ArenaGeometry.EXT_R) >> 4;
        int minZ = (g.cz() - ArenaGeometry.EXT_R) >> 4, maxZ = (g.cz() + ArenaGeometry.EXT_R) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) if (!level.areEntitiesLoaded(ChunkPos.pack(x, z))) return false;
        return true;
    }

    static boolean allocationChunksLoaded(ServerLevel level,ArenaAttempt a) {
        for(Alloc al:a.allocations()) if(al.status==AllocStatus.SPAWNED && al.chunkKnown && !level.areEntitiesLoaded(al.lastChunk))return false;
        return !a.bossChunkKnown || level.areEntitiesLoaded(a.bossChunk);
    }

    /** Every allocation is matched by attempt+allocation identity, not just a spatial scan or an unloaded UUID lookup. */
    static void reconcile(ServerLevel level, ArenaController ctl) {
        ArenaAttempt a=ctl.attempt(); if(a==null)return;
        java.util.Map<String,List<Entity>> tagged=new java.util.HashMap<>();
        String owner=TAG_ATTEMPT+a.attemptId;
        for(Entity e:level.getAllEntities()) if(e.isAlive() && e.entityTags().contains(owner)) {
            if(e instanceof ImmortalGladiator) {
                if(a.bossEntity==null || !e.getUUID().equals(a.bossEntity)) {ctl.isolate("unexpected or duplicate boss identity");return;}
                a.bossChunk=ChunkPos.pack(e.blockPosition());a.bossChunkKnown=true;
                continue;
            }
            for(String t:e.entityTags()) if(t.startsWith(TAG_ALLOC)) tagged.computeIfAbsent(t,k->new ArrayList<>()).add(e);
        }
        for(Alloc al:a.allocations()) {
            List<Entity> found=tagged.remove(TAG_ALLOC+al.id);
            if(found!=null && found.size()>1) {ctl.isolate("duplicate world allocation "+al.id);return;}
            Entity e=found==null?null:found.getFirst();
            if(e!=null) {
                String mob=al.mob.replace("#baby","");
                if(!BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(mob) || al.entity!=null && !al.entity.equals(e.getUUID())) {ctl.isolate("allocation entity/type mismatch");return;}
                if(al.status==AllocStatus.WARNING) a.spawnResult(al,e.getUUID());
                else if(al.status!=AllocStatus.SPAWNED && e.isAlive()) {ctl.isolate("unplanned live entity for "+al.status);return;}
                al.lastChunk=ChunkPos.pack(e.blockPosition());al.chunkKnown=true;
            }
            if(al.status!=AllocStatus.SPAWNED)continue;
            Entity bound=level.getEntity(al.entity);
            if(bound!=null && bound.isAlive() && e==null) {ctl.isolate("bound entity missing ownership tags");return;}
            if(bound==null || !bound.isAlive()) {
                if(ArenaRecovery.verifying(ctl)) {ctl.isolate("saved entity absent/dead after restart; independent saves cannot establish a loss");return;}
                if(!al.chunkKnown) {ctl.isolate("missing entity without verified last chunk");return;}
                if(level.areEntitiesLoaded(al.lastChunk))ctl.onMobLost(al.entity);
            }
        }
        if(!tagged.isEmpty()) {ctl.isolate("world entity has no allocation in saved plan");return;}
        if(a.bossEntity!=null && !a.bossDefeated) {
            Entity boss=level.getEntity(a.bossEntity);
            if(!(boss instanceof ImmortalGladiator b) || !a.attemptId.equals(b.attemptId()) || !boss.isAlive()) {ctl.isolate("bound boss missing or inconsistent");return;}
            if(ArenaRecovery.verifying(ctl) && (a.bossHealth>=0 && a.bossHealth!=b.getHealth() || a.bossState!=null && !a.bossState.isEmpty() && !a.bossState.equals(b.combatSnapshot()))) {ctl.isolate("boss HP/combat journal disagrees with entity save");return;}
            a.bossHealth=b.getHealth();a.bossState=b.combatSnapshot();
        }
        ArenaRecovery.verified(ctl);
    }

    private static void inheritProjectileOwner(Entity entity) {
        if(!(entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) || entity.entityTags().contains(TAG_PROJECTILE))return;
        Entity owner=projectile.getOwner();if(owner==null || owner==entity || !tracked(owner))return;
        for(String tag:owner.entityTags()) if(tag.startsWith(TAG_ATTEMPT))entity.addTag(tag);
        entity.addTag(TAG_PROJECTILE);
    }

    /** Freeze only arena-owned combat, including HP, while absent/unverified/isolated. Vanilla and other entities remain unaffected. */
    public static boolean combatFrozen(Entity entity) {
        if(!(entity.level() instanceof ServerLevel l))return false;
        inheritProjectileOwner(entity);
        if(!tracked(entity))return false;
        ColosseumData d=data(l.getServer());
        for(String id:d.sites().keySet()) {
            ArenaController c=d.controller(id);ArenaAttempt a=c.attempt();
            if(a==null || !entity.entityTags().contains(TAG_ATTEMPT+a.attemptId))continue;
            if(c.quarantined())return true;
            if(!c.active())return false;
            if(ArenaRecovery.verifying(c) || !entitiesLoaded(l,d.sites().get(id).geometry()) || !allocationChunksLoaded(l,a))return true;
            for(ServerPlayer p:l.players()) {
                Member m=a.member(p.getUUID());BlockPos pos=p.blockPosition();
                if(m!=null && !m.dead && p.isAlive() && !p.isSpectator() && d.sites().get(id).geometry().contains(pos.getX(),pos.getY(),pos.getZ()))return false;
            }
            return true;
        }
        return true; // owned orphan: cannot safely act until reconciliation
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
            case DropKey k -> { /* handled after the generic world-effect fence by the reward journal */ }
            case SupplyChests c -> placeSupplies(level, site, attempt, c);
            case BossStart b -> {
                ColosseumBossHook.Provider p = ColosseumBossHook.provider();
                if (p == null || !p.start(level, site, attempt, b.maxHp())) attempt.bossRequested = false;   // retried next tick
            }
            case End e -> end(level, site, ctl, attempt, e);
        }
    }

    /** Reconciles only identity-tagged reward entities. Ambiguous insertion is quarantined, never replayed. */
    static boolean reconcileRewards(ServerLevel level,ColosseumData.Site site,ArenaController ctl) {
        if(ctl.rewardDeliveries.isEmpty())return true;
        boolean verifyAll=ArenaRecovery.rewardVerifying(ctl);
        for(RewardDelivery delivery:ctl.rewardDeliveries.values()) {
            if(!verifyAll && delivery.state==RewardDelivery.State.DELIVERED)continue;
            if(!delivery.validPosition()) {ctl.isolate("shared reward plan has no verifiable world position");ArenaRecovery.save(level.getServer(),ctl,false);return false;}
            List<ItemEntity> found=new ArrayList<>();
            String tag=TAG_REWARD+delivery.rewardId;
            for(Entity e:level.getAllEntities()) if(e instanceof ItemEntity item && item.isAlive() && item.entityTags().contains(tag))found.add(item);
            if(found.size()==1 && !validRewardEntity(found.getFirst(),delivery)) {ctl.isolate("shared reward entity disagrees with durable plan");ArenaRecovery.save(level.getServer(),ctl,false);return false;}
            RewardDelivery.Reconcile result=delivery.reconcile(found.size());
            if(result==RewardDelivery.Reconcile.QUARANTINE) {ctl.isolate("shared reward delivery is ambiguous or duplicated: "+delivery.rewardId);ArenaRecovery.save(level.getServer(),ctl,false);return false;}
            if(result==RewardDelivery.Reconcile.RETRY) {
                if(!deliverReward(level,site,ctl,delivery))return false;
            } else if(result==RewardDelivery.Reconcile.CONFIRMED) {
                if(!ArenaRecovery.save(level.getServer(),ctl,false))return false;
                data(level.getServer()).setDirty();
            }
        }
        if(verifyAll)ArenaRecovery.rewardsVerified(ctl);
        return true;
    }

    private static boolean validRewardEntity(ItemEntity entity,RewardDelivery delivery) {
        String expected=delivery.itemId;
        boolean item=expected.equals("minecraft:ominous_trial_key") ? entity.getItem().is(Items.OMINOUS_TRIAL_KEY) : entity.getItem().is(Items.TRIAL_KEY);
        return item && entity.getItem().getCount()==1 && entity.getUUID().toString().equals(delivery.entityId)
                && entity.entityTags().contains(TAG_ATTEMPT+delivery.challengeId);
    }

    private static boolean deliverReward(ServerLevel level,ColosseumData.Site site,ArenaController ctl,RewardDelivery delivery) {
        List<ItemEntity> found=new ArrayList<>();
        for(Entity e:level.getAllEntities()) if(e instanceof ItemEntity item && item.isAlive() && item.entityTags().contains(TAG_REWARD+delivery.rewardId))found.add(item);
        if(found.size()>1) {ctl.isolate("duplicate shared reward entities: "+delivery.rewardId);ArenaRecovery.save(level.getServer(),ctl,false);return false;}
        if(found.size()==1) {
            if(!validRewardEntity(found.getFirst(),delivery)) {ctl.isolate("shared reward entity identity/item mismatch");ArenaRecovery.save(level.getServer(),ctl,false);return false;}
            RewardDelivery.Reconcile result=delivery.reconcile(1);
            if(result==RewardDelivery.Reconcile.QUARANTINE) {ctl.isolate("shared reward receipt conflicts with delivery plan");return false;}
            return ArenaRecovery.save(level.getServer(),ctl,false);
        }
        RewardDelivery.Reconcile before=delivery.reconcile(0);
        if(before==RewardDelivery.Reconcile.ALREADY_DELIVERED) return true;
        if(before!=RewardDelivery.Reconcile.RETRY) {ctl.isolate("shared reward insertion has no verifiable entity");ArenaRecovery.save(level.getServer(),ctl,false);return false;}

        UUID entityId=RewardDelivery.entityUuid(delivery.rewardId);
        delivery.begin(entityId);
        if(!ArenaRecovery.save(level.getServer(),ctl,false))return false;
        Entity collision=level.getEntity(entityId);
        if(collision!=null) {ctl.isolate("deterministic shared reward UUID is occupied");ArenaRecovery.save(level.getServer(),ctl,false);return false;}
        ItemStack stack=new ItemStack(delivery.itemId.equals("minecraft:ominous_trial_key")?Items.OMINOUS_TRIAL_KEY:Items.TRIAL_KEY,1);
        boolean ominous=delivery.itemId.equals("minecraft:ominous_trial_key");
        ItemEntity item=new ItemEntity(level,delivery.x,delivery.y,delivery.z,stack);item.setUUID(entityId);
        item.addTag(TAG_REWARD+delivery.rewardId);item.addTag(TAG_ATTEMPT+delivery.challengeId);
        if(!level.addFreshEntity(item)) {ctl.isolate("shared reward entity insertion failed");ArenaRecovery.save(level.getServer(),ctl,false);return false;}
        // Persist the world side before recording completion on the independent controller side.
        level.getServer().saveEverything(true,true,true);
        delivery.delivered(entityId);
        if(!ArenaRecovery.save(level.getServer(),ctl,false))return false;
        data(level.getServer()).setDirty();
        MoreMinecrafts.LOGGER.info("ST05 arena {} challenge {} reward {} entity {} recorded",site.id(),delivery.challengeId,delivery.rewardId,entityId);
        return true;
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
        if (existing.size()>1) { data(level.getServer()).controller(site.id()).isolate("duplicate spawn allocation"); return; }
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
        alloc.lastChunk=ChunkPos.pack(mob.blockPosition());alloc.chunkKnown=true;
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
                ArenaSupplyOwnership.mark(chest,attempt.attemptId.toString());
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
        attempt.cleanupPending=e.outcome()!=Outcome.QUARANTINED;
        MoreMinecrafts.LOGGER.info("ST05 arena {} attempt {} ended {} at round {}", site.id(), attempt.attemptId, e.outcome(), attempt.round());
        if(e.outcome()!=Outcome.QUARANTINED && ArenaRecovery.saveCleanup(level.getServer(),ctl))finishCleanup(level,site,ctl);
    }

    static void finishCleanup(ServerLevel level,ColosseumData.Site site,ArenaController ctl) {
        ArenaAttempt a=ctl.attempt();
        if(!entitiesLoaded(level,site.geometry()) || !allocationChunksLoaded(level,a))return;
        if(!ArenaRecovery.saveCleanup(level.getServer(),ctl))return;
        if(a.outcome()==Outcome.TIMED_OUT) {
            ColosseumBossHook.provider().vanish(level,site,a);
        } else if(a.outcome()!=Outcome.QUARANTINED) {
            String owner=TAG_ATTEMPT+a.attemptId;
            // Entity removal mutates the live lookup; collect first so back-shifting iterators cannot skip remaining mobs.
            List<Entity> owned=new ArrayList<>();
            for(Entity e:level.getAllEntities()) if((e instanceof Mob || e instanceof net.minecraft.world.entity.projectile.Projectile) && e.entityTags().contains(owner))owned.add(e);
            for(Entity e:owned)e.discard();
            for(long packed:new ArrayList<>(a.supplyChests)) {
                BlockPos p=BlockPos.of(packed);if(!level.hasChunkAt(p))continue;
                if(level.getBlockState(p).is(Blocks.CHEST) && ArenaSupplyOwnership.owned(level.getBlockEntity(p),a.attemptId.toString())) {level.removeBlockEntity(p);level.setBlock(p,Blocks.AIR.defaultBlockState(),18);}
                a.supplyChests.remove(packed);
            }
        }
        ArenaTerrain.restore(level,ctl);
        if(!ctl.quarantined() && a.terrain.pending.isEmpty() && (a.outcome()==Outcome.TIMED_OUT || a.supplyChests.isEmpty()))a.cleanupPending=false;
    }

}
