package dev.itzgamesgd.moreminecrafts.st01;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Minecraft implementation of {@link EntranceCore.Ports} for one structure instance. Server-authoritative; no client input. */
final class McPorts implements EntranceCore.Ports {
    private static final double GOLEM_SEARCH_RADIUS = 128.0D;

    private final ServerLevel level;
    private final BlockPos anchor;
    private final long tomb;
    private final List<BlockPos> cover;
    private final BlockPos stop;
    private final BlockPos spawn;
    private final EntranceConfig cfg;

    McPorts(ServerLevel level, SecretEntranceData.Instance inst, EntranceConfig cfg) {
        this.level = level;
        this.anchor = inst.anchor();
        this.tomb = inst.key();
        this.cover = EntranceLayout.coverCells(anchor, inst.orientation());
        this.stop = EntranceLayout.stopPoint(anchor, inst.orientation());
        this.spawn = EntranceLayout.golemSpawn(anchor, inst.orientation());
        this.cfg = cfg;
    }

    BlockPos stop() { return stop; }
    BlockPos anchor() { return anchor; }

    @Override public boolean victory() { return TombGuardianEncounters.data(level).isVictory(tomb); }
    @Override public boolean spawnAreaLoaded() { return level.hasChunkAt(spawn) && level.hasChunkAt(stop); }

    @Override
    public boolean coverChunkLoaded() {
        for (BlockPos p : cover) if (!level.hasChunkAt(p)) return false;
        return true;
    }

    @Override
    public boolean coverOpen() {
        for (BlockPos p : cover) if (!level.getBlockState(p).isAir()) return false;
        return true;
    }

    /** Removes exactly the 12 cover cells (idempotent; air stays air; nothing ever places the blocks back). */
    @Override
    public void openCover() {
        for (BlockPos p : cover) if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private List<GoldGuideGolem> golems() {
        return level.getEntitiesOfClass(GoldGuideGolem.class, new AABB(anchor).inflate(GOLEM_SEARCH_RADIUS), g -> g.tomb() == tomb && g.isAlive());
    }

    @Override public int golemCount() { return golems().size(); }

    @Override
    public boolean golemAlive(UUID id) {
        Entity e = level.getEntity(id);
        return e instanceof GoldGuideGolem g && g.isAlive() && g.tomb() == tomb;
    }

    @Nullable
    @Override
    public UUID spawnGolem() {
        if (SecretEntranceRuntime.TestHooks.failGolemSpawnFor.contains(tomb)) return null;
        GoldGuideGolem golem = SecretEntranceEntities.GOLD_GUIDE_GOLEM.create(level, EntitySpawnReason.EVENT);
        if (golem == null) return null;
        golem.snapTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
        golem.configureGuide(tomb, stop);
        return level.addFreshEntity(golem) ? golem.getUUID() : null;
    }

    /** Arrival = the golem's own server position is within the (unapproved) arrival radius of the stop point, standing on the ground. */
    @Override
    public boolean golemAtStop(UUID id) {
        Entity e = level.getEntity(id);
        if (!(e instanceof GoldGuideGolem g) || !g.isAlive() || !g.onGround()) return false;
        double dx = g.getX() - (stop.getX() + 0.5D), dz = g.getZ() - (stop.getZ() + 0.5D);
        return Math.hypot(dx, dz) <= cfg.arrivalRadius() && Math.abs(g.getY() - stop.getY()) < 2.0D;
    }
}
