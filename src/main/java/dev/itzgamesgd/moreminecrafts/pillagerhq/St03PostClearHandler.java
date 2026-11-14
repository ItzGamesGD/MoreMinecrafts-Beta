package dev.itzgamesgd.moreminecrafts.pillagerhq;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Approved Option D post-clear handler:
 * 1. Once all illagers in an HQ are eliminated (cleared == true), suppress ALL natural hostile & neutral mob spawns
 *    (Creepers, Endermen, Monsters, etc.) inside the HQ compound.
 * 2. When 5+ beds exist inside the cleared HQ, grant vanilla village conversion and evaluate villager spawning
 *    twice per day starting from morning, up to 15 total villagers. Dead villagers never respawn beyond remaining attempts.
 */
public final class St03PostClearHandler {
    private St03PostClearHandler() {}

    public static final int MIN_BEDS_FOR_VILLAGE = 5;
    public static final int MAX_SPAWNED_VILLAGERS = 15;
    public static final int MAX_ATTEMPTS_PER_DAY = 2;

    /**
     * Called from Mixin on SpawnPlacements.checkSpawnRules.
     * Blocks all hostile and neutral creature natural spawns inside any cleared ST-03 HQ.
     */
    public static boolean shouldSuppressNaturalSpawn(ServerLevel level, EntityType<?> entityType,
                                                     EntitySpawnReason reason, BlockPos pos) {
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION
                && reason != EntitySpawnReason.PATROL && reason != EntitySpawnReason.REINFORCEMENT) {
            return false;
        }
        MobCategory category = entityType.getCategory();
        boolean isHostileOrNeutral = category == MobCategory.MONSTER
                || entityType == EntityType.ENDERMAN
                || entityType == EntityType.CREEPER;
        if (!isHostileOrNeutral) {
            return false;
        }
        St03HeadquartersData data = St03HeadquartersData.get(level);
        for (St03HeadquartersData.HqInstance hq : data.instances().values()) {
            if (hq.cleared && hq.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerLevel level, St03HeadquartersData data) {
        long dayTime = level.getDayTime();
        long currentDay = Math.floorDiv(dayTime, 24000L);
        long timeOfDay = Math.floorMod(dayTime, 24000L);

        for (St03HeadquartersData.HqInstance hq : data.instances().values()) {
            if (!hq.cleared || hq.box == null || hq.origin == null) {
                continue;
            }
            if (hq.spawnedVillagers >= MAX_SPAWNED_VILLAGERS) {
                continue;
            }
            if (!level.isLoaded(hq.origin)) {
                continue;
            }

            if (hq.lastVillagerDay != currentDay) {
                hq.lastVillagerDay = currentDay;
                hq.villagerAttemptsToday = 0;
                data.setDirty();
            }

            // Two condition checks per day starting at morning (window 1: [0..6000), window 2: [6000..12000))
            int targetAttempts = (timeOfDay >= 6000L && timeOfDay < 12000L) ? 2 : (timeOfDay < 6000L ? 1 : 0);
            if (targetAttempts <= hq.villagerAttemptsToday || hq.villagerAttemptsToday >= MAX_ATTEMPTS_PER_DAY) {
                continue;
            }

            int bedCount = countBedsInBox(level, hq.box);
            if (bedCount < MIN_BEDS_FOR_VILLAGE) {
                continue;
            }

            hq.villagerAttemptsToday++;
            if (trySpawnVillager(level, hq)) {
                hq.spawnedVillagers++;
            }
            data.setDirty();
        }
    }

    public static int countBedsInBox(ServerLevel level, BoundingBox box) {
        int beds = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (!level.isLoaded(cursor.set(x, box.minY(), z))) {
                    continue;
                }
                for (int y = box.minY(); y <= Math.min(box.maxY(), box.minY() + 20); y++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) {
                        beds++;
                    }
                }
            }
        }
        return beds;
    }

    private static boolean trySpawnVillager(ServerLevel level, St03HeadquartersData.HqInstance hq) {
        // Spawn in central plaza / avenue area inside the compound
        int[] plazaXZ = dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03PlacementManifest
                .rotateLocalIn112(55, 34, hq.rotation);
        BlockPos base = hq.origin.offset(plazaXZ[0], 1, plazaXZ[1]);
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = level.getRandom().nextInt(9) - 4;
            int dz = level.getRandom().nextInt(9) - 4;
            BlockPos candidate = base.offset(dx, 0, dz);
            if (hq.contains(candidate)
                    && level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()
                    && !level.getBlockState(candidate.below()).isAir()) {
                Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.NATURAL);
                if (villager != null) {
                    villager.snapTo(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5,
                            level.getRandom().nextFloat() * 360.0F, 0.0F);
                    level.addFreshEntity(villager);
                    return true;
                }
            }
        }
        return false;
    }
}
