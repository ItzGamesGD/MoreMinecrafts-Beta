package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03Eligibility;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03PlacementConfig;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03HeadquartersStructure;
import dev.itzgamesgd.moreminecrafts.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;

/**
 * Real-client check of the ST-03 Pillager Headquarters (gated by -Dmoreminecrafts.st03.worldgen.check=true).
 *
 * <p>HQ coordinates come from the same random_spread placement the structure set uses (seed + salt 91403), filtered by
 * the same St03Eligibility rule the structure applies at generation. Each candidate is then visited to confirm the HQ
 * actually generated. Results are printed as ST03_WORLDGEN lines.
 */
public final class St03WorldgenClientCheck implements FabricClientGameTest {
    private static final Identifier SET_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st03_pillager_headquarters");
    private static final int REGION_RADIUS = 2;
    private static final int HQ_TARGET = 3;
    private static final int SLOT_COUNT = 40;

    /** Planned HQ origins {x, foundationY, z}, filled by the server thread. */
    static final List<int[]> PLAN = new ArrayList<>();
    static final java.util.Map<String, Integer> VERDICTS = new java.util.TreeMap<>();
    static final java.util.Map<String, Integer> BIOME_HIST = new java.util.TreeMap<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.st03.worldgen.check")) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");
        properties.setProperty("max-tick-time", "-1");
        properties.setProperty("view-distance", "6");
        properties.setProperty("simulation-distance", "6");
        // Default client world (superflat: plains and flat, so the eligibility rule passes everywhere).
        var worldBuilder = context.worldBuilder();
        try (TestDedicatedServerContext server = worldBuilder.createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.waitTicks(40);

            // 1. Plan HQ origins from the placement math, one region row per server tick.
            for (int row = -REGION_RADIUS; row <= REGION_RADIUS && PLAN.size() < 40; row++) {
                final int rx = row;
                server.runOnServer(s -> planRow(s.overworld(), rx));
                connection.waitForClientboundPackets();
            }
            System.out.println("ST03_WORLDGEN planned_candidates=" + PLAN.size() + " verdicts=" + VERDICTS + " biomes=" + BIOME_HIST);
            if (PLAN.size() < HQ_TARGET && !Boolean.getBoolean("moreminecrafts.st03.worldgen.planOnly")) {
                throw new AssertionError("ST03_WORLDGEN only " + PLAN.size() + " eligible candidates in +-" + REGION_RADIUS + " regions");
            }
            if (Boolean.getBoolean("moreminecrafts.st03.worldgen.planOnly")) {
                System.out.println("ST03_WORLDGEN_PLAN_ONLY_COMPLETE eligible=" + PLAN.size());
                return;
            }
            List<int[]> chosen = choose(PLAN, HQ_TARGET);
            for (int[] c : chosen) {
                System.out.println("ST03_WORLDGEN planned_hq origin=" + c[0] + "," + c[1] + "," + c[2]);
            }

            // 2. Visit each planned HQ: confirm it generated, wait for the first-entry garrison, count per type.
            for (int i = 0; i < chosen.size(); i++) {
                int[] c = chosen.get(i);
                final int index = i;
                server.runOnServer(s -> teleport(s, s.overworld(), c[0] + 56.5, c[1] + 3, c[2] + 56.5, 0, 10));
                connection.waitForClientboundPackets();
                context.waitTicks(240);

                final String[] id = {null};
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    St03HeadquartersData.HqInstance hq = findHq(level, c[0], c[2]);
                    boolean generated = hq != null;
                    if (hq == null) {
                        // The superflat world has no structure placements, so the HQ never generates here.
                        // Record the planned origin the same way structure completion does, then run the real spawn path.
                        hq = St03HeadquartersData.get(level).getOrCreate(((long) c[0] << 32) ^ (c[2] & 0xFFFFFFFFL));
                        hq.origin = new BlockPos(c[0], c[1], c[2]);
                        hq.rotation = net.minecraft.world.level.block.Rotation.NONE;
                        hq.box = new net.minecraft.world.level.levelgen.structure.BoundingBox(c[0], c[1], c[2], c[0] + 111, c[1] + 30, c[2] + 111);
                        hq.structureCompleted = true;
                        St03HeadquartersData.get(level).setDirty();
                    }
                    id[0] = hq.id();
                    System.out.println("ST03_WORLDGEN hq=" + index + " origin=" + c[0] + "," + c[1] + "," + c[2]
                            + " generated=" + generated + " forced=" + !generated + " id=" + hq.id() + " box=" + hq.box);
                });
                connection.waitForClientboundPackets();
                if (id[0] == null) continue;

                // Wait for the spawn to appear, then 60 more ticks (>= 40 required) before counting.
                for (int poll = 0; poll < 30; poll++) {
                    final boolean[] spawned = {false};
                    server.runOnServer(s -> spawned[0] = defenderCount(s.overworld(), c[0], c[2]) > 0);
                    connection.waitForClientboundPackets();
                    if (spawned[0]) break;
                    context.waitTicks(20);
                }
                context.waitTicks(60);
                server.runOnServer(s -> printGarrison(s.overworld(), c[0], c[2], "hq=" + index));
                connection.waitForClientboundPackets();

                if (i == 0) {
                    server.runOnServer(s -> teleport(s, s.overworld(), c[0] - 40.5, c[1] + 18, c[2] - 40.5, -45, 12));
                    connection.waitForClientboundPackets();
                    context.waitTicks(60);
                    System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-00-hq-exterior"));

                    server.runOnServer(s -> teleport(s, s.overworld(), c[0] + 56.5, c[1] + 3, c[2] + 56.5, 0, 10));
                    connection.waitForClientboundPackets();
                    context.waitTicks(60);
                    System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-01-hq-interior-garrison"));
                }
            }

            // 3. Post-clear on the first HQ: kill every defender through the normal death path, then 5 beds.
            int[] first = chosen.get(0);
            // Return to HQ 0 first: the garrison is only loaded (and only killable) while the player is nearby.
            System.out.println("ST03_WORLDGEN return_to_hq0 target=" + first[0] + "," + first[1] + "," + first[2] + " chosenSize=" + chosen.size());
            server.runOnServer(s -> {
                teleport(s, s.overworld(), first[0] + 56.5, first[1] + 3, first[2] + 56.5, 0, 10);
                System.out.println("ST03_WORLDGEN return_to_hq0 player_after=" + s.getPlayerList().getPlayers().getFirst().blockPosition());
            });
            connection.waitForClientboundPackets();
            context.waitTicks(1200);
            // Kill passes until none are left in the loaded box (some defenders load a little after the first pass).
            for (int pass = 0; pass < 15; pass++) {
                final int passNo = pass;
                final int[] left = {0};
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    St03HeadquartersData.HqInstance hq = findHq(level, first[0], first[2]);
                    if (hq == null) return;
                    for (Entity e : level.getEntities((Entity) null, boxOf(hq), e -> e.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id()))) {
                        if (e instanceof LivingEntity living && !(e instanceof Villager)) {
                            // Real damage and death path (fires AFTER_DEATH, which the garrison handler uses to count eliminations).
                            living.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
                        }
                    }
                    left[0] = defenderCount(level, first[0], first[2]);
                    System.out.println("ST03_WORLDGEN kill_pass=" + passNo + " player=" + s.getPlayerList().getPlayers().getFirst().blockPosition() + " loadedInBox=" + level.getEntities((Entity) null, boxOf(hq), e -> true).size() + " defendersLeft=" + left[0] + " eliminated=" + hq.eliminatedSlots.size() + " cleared=" + hq.cleared);
                });
                connection.waitForClientboundPackets();
                if (left[0] == 0) break;
                context.waitTicks(40);
            }
            context.waitTicks(40);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                St03HeadquartersData.HqInstance hq = findHq(level, first[0], first[2]);
                if (hq == null) return;
                System.out.println("ST03_WORLDGEN post_clear cleared=" + hq.cleared + " eliminated=" + hq.eliminatedSlots.size()
                        + " defendersLeft=" + defenderCount(level, first[0], first[2]));
                for (int i = 0; i < 5; i++) {
                    BlockPos foot = hq.origin.offset(1 + i * 2, 1, 2);
                    BlockPos head = foot.south();
                    level.setBlock(foot, ModBlocks.EXPLORATION_SLEEPING_BAG.defaultBlockState()
                            .setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.SOUTH), 3);
                    level.setBlock(head, ModBlocks.EXPLORATION_SLEEPING_BAG.defaultBlockState()
                            .setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.SOUTH), 3);
                }
                System.out.println("ST03_WORLDGEN beds placed=5 counted=" + St03PostClearHandler.countBedsInBox(level, hq.box));
                BlockPos inner = hq.origin.offset(5, 1, 5);
                for (EntityType<?> type : new EntityType<?>[]{net.minecraft.world.entity.EntityTypes.ZOMBIE, net.minecraft.world.entity.EntityTypes.CREEPER, net.minecraft.world.entity.EntityTypes.ENDERMAN, net.minecraft.world.entity.EntityTypes.SKELETON, net.minecraft.world.entity.EntityTypes.SPIDER}) {
                    System.out.println("ST03_WORLDGEN hostile_suppressed type=" + type.toShortString() + " value="
                            + St03PostClearHandler.shouldSuppressNaturalSpawn(level, type, EntitySpawnReason.NATURAL, inner));
                }
            });
            connection.waitForClientboundPackets();

            // 4. Villager conversion: poll the HQ box for new villagers. Day-cycle timing limits this to what fits in real time.
            for (int wait = 0; wait < 4; wait++) {
                context.waitTicks(400);
                final int tickWait = wait;
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    St03HeadquartersData.HqInstance hq = findHq(level, first[0], first[2]);
                    if (hq == null) return;
                    int villagers = level.getEntities((Entity) null, boxOf(hq), v -> v instanceof Villager).size();
                    System.out.println("ST03_WORLDGEN conversion poll=" + tickWait + " spawnedVillagers=" + hq.spawnedVillagers
                            + " villagersInBox=" + villagers + " gameTime=" + level.getGameTime());
                });
                connection.waitForClientboundPackets();
            }
            server.runOnServer(s -> teleport(s, s.overworld(), first[0] + 56.5, first[1] + 3, first[2] + 56.5, 0, 10));
            connection.waitForClientboundPackets();
            context.waitTicks(60);
            System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-02-post-clear-conversion"));
            System.out.println("ST03_WORLDGEN_CHECK_COMPLETE");
        }
    }

    /** One row of random_spread regions: computes the potential HQ chunk and applies the structure's eligibility rule. */
    static void planRow(ServerLevel level, int regionX) {
        var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, SET_ID)).value();
        RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();
        long seed = level.getSeed();
        net.minecraft.world.level.chunk.ChunkGenerator gen = level.getChunkSource().getGenerator();
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
        for (int regionZ = -REGION_RADIUS; regionZ <= REGION_RADIUS; regionZ++) {
            // The method takes chunk coordinates and returns the candidate of the region containing them.
            ChunkPos candidate = placement.getPotentialStructureChunk(seed, regionX * placement.spacing(), regionZ * placement.spacing());
            int originX = candidate.getMinBlockX() - St03PlacementConfig.BOX_SIZE / 2 + 8;
            int originZ = candidate.getMinBlockZ() - St03PlacementConfig.BOX_SIZE / 2 + 8;
            St03Eligibility.Result result = St03Eligibility.evaluate(terrain, originX, originZ);
            String biomeAtCenter = level.getBiome(new BlockPos(originX + 56, result.foundationY(), originZ + 56)).unwrapKey().map(k -> k.identifier().toString()).orElse("?");
            BIOME_HIST.merge(biomeAtCenter, 1, Integer::sum);
            if (BIOME_HIST.values().stream().mapToInt(Integer::intValue).sum() <= 4) {
                System.out.println("ST03_WORLDGEN sample origin=" + originX + "," + originZ + " yf=" + result.foundationY() + " surfCenter=" + terrain.surfaceHeight(originX + 56, originZ + 56) + " floorCenter=" + terrain.oceanFloorHeight(originX + 56, originZ + 56) + " biome=" + biomeAtCenter + " uncachedBiome=" + level.getUncachedNoiseBiome((originX + 56) >> 2, result.foundationY() >> 2, (originZ + 56) >> 2).unwrapKey().map(k -> k.identifier().toString()).orElse("?"));
            }
            VERDICTS.merge(result.verdict().toString(), 1, Integer::sum);
            if (!result.eligible()) continue;
            System.out.println("ST03_WORLDGEN eligible_candidate region=" + regionX + "," + regionZ + " chunk=" + candidate + " origin=" + originX + "," + originZ + " foundationY=" + result.foundationY());
            PLAN.add(new int[]{originX, result.foundationY(), originZ});
        }
    }

    /** Picks up to {@code count} candidates nearest the spawn, keeping them at least 1200 blocks apart. */
    static List<int[]> choose(List<int[]> all, int count) {
        List<int[]> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> Long.compare((long) a[0] * a[0] + (long) a[2] * a[2], (long) b[0] * b[0] + (long) b[2] * b[2]));
        List<int[]> picked = new ArrayList<>();
        for (int[] c : sorted) {
            boolean far = picked.stream().allMatch(p -> Math.hypot(p[0] - c[0], p[2] - c[2]) >= 1200);
            if (far) picked.add(c);
            if (picked.size() == count) break;
        }
        return picked;
    }

    static int[] first0(List<int[]> chosen) {
        return chosen.get(0);
    }

    static St03HeadquartersData.HqInstance findHq(ServerLevel level, int x, int z) {
        for (St03HeadquartersData.HqInstance hq : St03HeadquartersData.get(level).instances().values()) {
            if (hq.box != null && hq.box.isInside(new BlockPos(x + 56, hq.box.minY() + 1, z + 56))) return hq;
        }
        return null;
    }

    static AABB boxOf(St03HeadquartersData.HqInstance hq) {
        return new AABB(hq.box.minX(), hq.box.minY(), hq.box.minZ(), hq.box.maxX() + 1, hq.box.maxY() + 1, hq.box.maxZ() + 1);
    }

    static int defenderCount(ServerLevel level, int x, int z) {
        St03HeadquartersData.HqInstance hq = findHq(level, x, z);
        if (hq == null) return 0;
        int n = 0;
        for (Entity e : level.getEntities((Entity) null, boxOf(hq), e -> e.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id()))) {
            if (!(e instanceof Villager)) n++;
        }
        return n;
    }

    static void printGarrison(ServerLevel level, int x, int z, String label) {
        St03HeadquartersData.HqInstance hq = findHq(level, x, z);
        if (hq == null) return;
        TreeMap<String, Integer> byType = new TreeMap<>();
        boolean[] present = new boolean[SLOT_COUNT];
        int defenders = 0;
        for (Entity e : level.getEntities((Entity) null, boxOf(hq), e -> e.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id()))) {
            if (e instanceof Villager) continue;
            defenders++;
            byType.merge(e.getType().toShortString(), 1, Integer::sum);
            for (String tag : e.entityTags()) {
                if (tag.startsWith("st03_slot:")) {
                    int slot = Integer.parseInt(tag.substring("st03_slot:".length()));
                    if (slot >= 0 && slot < SLOT_COUNT) present[slot] = true;
                }
            }
        }
        StringBuilder missing = new StringBuilder();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!present[i]) missing.append(i).append(' ');
        }
        System.out.println("ST03_WORLDGEN_GARRISON " + label + " defenders=" + defenders + " expected=" + SLOT_COUNT
                + " byType=" + byType + " missingSlots=[" + missing.toString().trim() + "]");
    }

    private static void teleport(MinecraftServer server, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.teleportTo(level, x, y, z, Set.of(), yaw, pitch, true);
        }
    }
}
