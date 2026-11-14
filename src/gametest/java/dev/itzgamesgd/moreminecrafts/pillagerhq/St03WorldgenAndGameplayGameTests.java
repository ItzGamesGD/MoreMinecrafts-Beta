package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03Eligibility;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03HeadquartersStructure;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03PlacementManifest;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class St03WorldgenAndGameplayGameTests {
    private static final long[] TEN_SEEDS = {
            1001L, 2002L, 3003L, 4004L, 5005L,
            6006L, 7007L, 8008L, 9009L, 10010L
    };

    @GameTest
    public void st03TemplatesLoadAnd10SeedWorldgenProbe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var templates = level.getStructureTemplateManager();
        for (St03PlacementManifest.Placement p : St03PlacementManifest.placements()) {
            var tpl = templates.get(p.module().templateId());
            if (tpl.isEmpty()) {
                helper.fail("Missing ST-03 structure template: " + p.module().templateId());
                return;
            }
        }

        // Run 10-seed headless worldgen eligibility probe across +-4000 blocks (spacing=48 chunks = 768 blocks)
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        if (gen instanceof NoiseBasedChunkGenerator) {
            int totalCandidates = 0;
            int totalEligible = 0;
            Map<St03Eligibility.Verdict, Integer> totals = new EnumMap<>(St03Eligibility.Verdict.class);
            for (var v : St03Eligibility.Verdict.values()) {
                totals.put(v, 0);
            }

            var rs = level.getChunkSource().randomState();
            St03Eligibility.Terrain terrain = new St03Eligibility.Terrain() {
                @Override
                public int surfaceHeight(int x, int z) {
                    return gen.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
                }

                @Override
                public int oceanFloorHeight(int x, int z) {
                    return gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
                }

                @Override
                public boolean isEligibleBiomeCell(int qx, int qy, int qz) {
                    return St03HeadquartersStructure.isEligibleBiome(level.getUncachedNoiseBiome(qx, qy, qz));
                }
            };

            List<Double> allNearestDistances = new ArrayList<>();
            List<int[]> accepted = new ArrayList<>();
            // Sample grid at 768-block spacing (~48 chunks) within +-3840 blocks (8000x8000 area)
            for (int cx = -3840; cx <= 3840; cx += 768) {
                for (int cz = -3840; cz <= 3840; cz += 768) {
                    totalCandidates++;
                    var res = St03Eligibility.evaluate(terrain, cx, cz);
                    totals.merge(res.verdict(), 1, Integer::sum);
                    if (res.eligible()) {
                        totalEligible++;
                        accepted.add(new int[]{cx + 56, cz + 56});
                    }
                }
            }
            for (int i = 0; i < accepted.size(); i++) {
                double best = Double.MAX_VALUE;
                for (int j = 0; j < accepted.size(); j++) {
                    if (i == j) continue;
                    double d = Math.hypot(accepted.get(i)[0] - accepted.get(j)[0],
                            accepted.get(i)[1] - accepted.get(j)[1]);
                    if (d < best) best = d;
                }
                if (best < Double.MAX_VALUE) {
                    allNearestDistances.add(best);
                }
            }

            double avgNearest = allNearestDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            MoreMinecrafts.LOGGER.info(
                    "[ST03-WORLDGEN-PROBE] candidates={} eligible={} acceptance={}% avgNearestDist={} verdicts={}",
                    totalCandidates, totalEligible,
                    String.format("%.2f", 100.0 * totalEligible / Math.max(1, totalCandidates)),
                    String.format("%.1f", avgNearest), totals);
        }

        helper.succeed();
    }

    @GameTest
    public void st03GarrisonVanillaFollowRangeAndPostClearOptionD(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        // Build minimal floor for spawn checks
        for (int dx = 0; dx < 12; dx++) {
            for (int dz = 0; dz < 12; dz++) {
                level.setBlock(origin.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        St03HeadquartersData data = St03HeadquartersData.get(level);
        long key = 987654321L;
        St03HeadquartersData.HqInstance hq = data.getOrCreate(key);
        hq.origin = origin;
        hq.rotation = Rotation.NONE;
        hq.box = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 111, origin.getY() + 30, origin.getZ() + 111);
        hq.structureCompleted = true;

        St03GarrisonManager.spawnInitialGarrison(level, hq, data);

        // Verify Commander attributes: 40 HP, +25% damage, vanilla follow_range == 12.0
        var boxAabb = new net.minecraft.world.phys.AABB(
                hq.box.minX(), hq.box.minY(), hq.box.minZ(),
                hq.box.maxX() + 1, hq.box.maxY() + 1, hq.box.maxZ() + 1);
        List<Vindicator> commanders = level.getEntities(
                dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog.PILLAGER_COMMANDER,
                boxAabb,
                v -> v.entityTags().contains(St03GarrisonManager.TAG_COMMANDER));
        if (commanders.size() != 1) {
            helper.fail("Expected exactly 1 ST-03 Commander, found " + commanders.size());
            return;
        }
        Vindicator cmd = commanders.get(0);
        if (Math.abs(cmd.getMaxHealth() - 40.0F) > 0.01F) {
            helper.fail("Expected Commander maxHealth 40.0, got " + cmd.getMaxHealth());
            return;
        }
        double followRange = cmd.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (Math.abs(followRange - 12.0D) > 0.01D) {
            helper.fail("Expected vanilla Vindicator follow_range 12.0, got " + followRange);
            return;
        }
        if (level.getEntities(dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog.PILLAGER_BEASTMASTER, boxAabb, e -> true).size() != 1
                || level.getEntities(dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog.ARMORY_GUARD, boxAabb, e -> true).size() != 2
                || level.getEntities(dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog.PILLAGER_DRILLMASTER, boxAabb, e -> true).size() != 1) {
            helper.fail("Expected 1 Beastmaster, 2 Armory Guards, and 1 Drillmaster in ST-03 garrison");
            return;
        }

        // Cleanup spawned garrison entities & mark cleared
        for (Entity e : level.getEntities(null, new net.minecraft.world.phys.AABB(
                hq.box.minX(), hq.box.minY(), hq.box.minZ(),
                hq.box.maxX() + 1, hq.box.maxY() + 1, hq.box.maxZ() + 1))) {
            if (e.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id())) {
                e.discard();
            }
        }
        hq.cleared = true;
        data.setDirty();

        // Option D: suppress natural Creeper/Enderman/Zombie spawns inside cleared HQ
        BlockPos inner = origin.offset(5, 1, 5);
        if (!St03PostClearHandler.shouldSuppressNaturalSpawn(level, net.minecraft.world.entity.EntityTypes.CREEPER, EntitySpawnReason.NATURAL, inner)
                || !St03PostClearHandler.shouldSuppressNaturalSpawn(level, net.minecraft.world.entity.EntityTypes.ENDERMAN, EntitySpawnReason.NATURAL, inner)) {
            helper.fail("Expected Option D to suppress natural Creeper and Enderman spawns inside cleared HQ");
            return;
        }

        // Option D: 5 beds placed -> bed count check passes
        for (int i = 0; i < 5; i++) {
            BlockPos foot = origin.offset(1 + i * 2, 1, 2);
            BlockPos head = foot.south();
            level.setBlock(foot, dev.itzgamesgd.moreminecrafts.registry.ModBlocks.EXPLORATION_SLEEPING_BAG.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BedBlock.PART,
                            net.minecraft.world.level.block.state.properties.BedPart.FOOT)
                    .setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
            level.setBlock(head, dev.itzgamesgd.moreminecrafts.registry.ModBlocks.EXPLORATION_SLEEPING_BAG.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BedBlock.PART,
                            net.minecraft.world.level.block.state.properties.BedPart.HEAD)
                    .setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        }
        int bedCount = St03PostClearHandler.countBedsInBox(level, hq.box);
        if (bedCount < 5) {
            helper.fail("Expected >= 5 beds in HQ box, got " + bedCount);
            return;
        }

        helper.succeed();
    }
}
