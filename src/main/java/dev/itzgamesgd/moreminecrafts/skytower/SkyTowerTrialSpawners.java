package dev.itzgamesgd.moreminecrafts.skytower;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mixin.TrialSpawnerConfigAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
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

    public static boolean configure(ServerLevel level, TrialSpawnerBlockEntity spawner, int index) {
        var configs = level.registryAccess().lookupOrThrow(Registries.TRIAL_SPAWNER_CONFIG);
        String combo = combo(index);
        var normal = configs.getOrThrow(key(combo, false));
        var ominous = configs.getOrThrow(key(combo, true));
        var accessor = (TrialSpawnerConfigAccessor) (Object) spawner.getTrialSpawner();
        TrialSpawner.FullConfig current = accessor.moreminecrafts$config();
        if (current.normal().is(normal.key()) && current.ominous().is(ominous.key())) return true;
        accessor.moreminecrafts$setConfig(new TrialSpawner.FullConfig(normal, ominous,
                current.targetCooldownLength(), current.requiredPlayerRange()));
        spawner.setChanged();
        return true;
    }
}
