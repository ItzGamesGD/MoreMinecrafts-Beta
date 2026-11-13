package dev.itzgamesgd.moreminecrafts.skytower;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mixin.TrialSpawnerConfigAccessor;
import dev.itzgamesgd.moreminecrafts.mixin.TrialSpawnerStateDataAccessor;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;

/**
 * SKY-MOB §15.6 P4/P5 + C2: the 16 Sky Tower Trial Spawners use the owner-approved group C combinations instead of the
 * vanilla zombie configs baked into the approved NBT. The NBT (and its SHA256) is untouched: configs are swapped at
 * runtime on the registered spawner block entities. Only the normal/ominous config holders change; the vanilla
 * trial state, cooldown length and player range from the structure are preserved (T3).
 */
public final class SkyTowerTrialSpawners {
    /** Combination per spawner index (floor-major; index % 4 is the (17,17)/(17,31)/(31,17)/(31,31) altar order). */
    private static final String[] LAYOUT = {
            "a", "a", "b", "a",
            "b", "c", "c", "b",
            "c", "d", "d", "e",
            "d", "e", "e", "e"};

    private SkyTowerTrialSpawners() {}

    public static String combo(int spawnerIndex) {
        if (spawnerIndex < 0 || spawnerIndex >= LAYOUT.length) throw new IllegalArgumentException("invalid spawner");
        return LAYOUT[spawnerIndex];
    }

    public static ResourceKey<TrialSpawnerConfig> key(String combo, boolean ominous) {
        return ResourceKey.create(Registries.TRIAL_SPAWNER_CONFIG,
                ModIds.id("sky_tower/combo_" + combo + "/" + (ominous ? "ominous" : "normal")));
    }

    /** Applies the combination configs to every loaded spawner of the site; idempotent. Returns spawners configured. */
    public static int configureSite(ServerLevel level, SkyTowerLayout.Site site) {
        if (level == null || site == null) return 0;
        int configured = 0;
        for (int i = 0; i < 16; i++) {
            var pt = site.spawner(i);
            var pos = new BlockPos(pt.x(), pt.y(), pt.z());
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof TrialSpawnerBlockEntity spawner && configure(level, spawner, i))
                configured++;
        }
        return configured;
    }

    /** Applies the combination to a spawner in a fully loaded chunk and marks it changed. */
    public static boolean configure(ServerLevel level, TrialSpawnerBlockEntity spawner, int index) {
        if (apply(level, spawner, index)) spawner.setChanged();
        return true;
    }

    /**
     * Applies the combination to a spawner whose chunk is still being loaded (CHUNK_LOAD). The chunk itself is marked
     * unsaved instead of {@link TrialSpawnerBlockEntity#setChanged()}, which would wait on that same chunk through the
     * level and stall the server thread. Returns whether the config changed.
     */
    public static boolean configureLoading(ServerLevel level, ChunkAccess chunk, TrialSpawnerBlockEntity spawner, int index) {
        if (!apply(level, spawner, index)) return false;
        chunk.markUnsaved();
        return true;
    }

    /**
     * Swaps only the normal/ominous holders (keeping cooldown and player range) and drops a pending spawn pick made
     * from the previous config, so a reconfigured existing-world spawner never spawns a stale vanilla mob (C2).
     * Never touches the level. Returns whether anything changed; idempotent.
     */
    private static boolean apply(ServerLevel level, TrialSpawnerBlockEntity spawner, int index) {
        var configs = level.registryAccess().lookupOrThrow(Registries.TRIAL_SPAWNER_CONFIG);
        String combo = combo(index);
        var normal = configs.getOrThrow(key(combo, false));
        var ominous = configs.getOrThrow(key(combo, true));
        var trial = spawner.getTrialSpawner();
        var accessor = (TrialSpawnerConfigAccessor) (Object) trial;
        TrialSpawner.FullConfig current = accessor.moreminecrafts$config();
        if (current.normal().is(normal.key()) && current.ominous().is(ominous.key())) return false;
        accessor.moreminecrafts$setConfig(new TrialSpawner.FullConfig(normal, ominous,
                current.targetCooldownLength(), current.requiredPlayerRange()));
        var state = (TrialSpawnerStateDataAccessor) trial.getStateData();
        state.moreminecrafts$setNextSpawnData(Optional.empty());
        state.moreminecrafts$setDisplayEntity(null);
        return true;
    }
}
