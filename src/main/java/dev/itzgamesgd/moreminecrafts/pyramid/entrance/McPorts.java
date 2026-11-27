package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
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
        this.anchor = inst.tombAnchor();
        this.tomb = inst.key();
        this.cover = EntranceLayout.coverCells(inst.ground(), inst.orientation());
        this.stop = EntranceLayout.stopPoint(inst.ground(), inst.orientation());
        this.spawn = EntranceLayout.golemSpawn(inst.ground(), inst.orientation());
        this.cfg = cfg;
    }

    /** The existing persisted Tomb Guardian completion (the record that also unseals the PRIMARY chest) is the only truth. */
    @Override public boolean victory() { return TombGuardianEncounters.isDefeated(level, anchor); }
    @Override public boolean spawnAreaLoaded() { return level.hasChunkAt(spawn) && level.hasChunkAt(stop); }

    @Override
    public boolean coverChunkLoaded() {
        for (BlockPos p : cover) if (!level.hasChunkAt(p)) return false;
        return true;
    }

    /**
     * A cover cell obstructs entry when it holds anything but air or a plain fluid. Fluids are deliberately left alone:
     * Policy D accepts natural water inside chambers and forbids added fluid deletion, and a fluid does not block entry.
     */
    static boolean obstructs(BlockState state) { return !state.isAir() && !(state.getBlock() instanceof LiquidBlock); }

    @Override
    public boolean coverOpen() {
        for (BlockPos p : cover) if (obstructs(level.getBlockState(p))) return false;
        return true;
    }

    /**
     * Clears exactly the 12 transformed cover cells, whatever solid block occupies them (sandstone, sand, a player-placed
     * block); idempotent; nothing outside the 12 cells is written and nothing ever places the cover back.
     */
    @Override
    public void openCover() {
        for (BlockPos p : cover) if (obstructs(level.getBlockState(p))) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
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

    /** Arrival = the golem's own server position is within the approved O-4 arrival radius (1.5 blocks) of the stop point, standing on the ground. */
    @Override
    public boolean golemAtStop(UUID id) {
        Entity e = level.getEntity(id);
        if (!(e instanceof GoldGuideGolem g) || !g.isAlive() || !g.onGround()) return false;
        double dx = g.getX() - (stop.getX() + 0.5D), dz = g.getZ() - (stop.getZ() + 0.5D);
        return Math.hypot(dx, dz) <= cfg.arrivalRadius() && Math.abs(g.getY() - stop.getY()) < 2.0D;
    }
}
