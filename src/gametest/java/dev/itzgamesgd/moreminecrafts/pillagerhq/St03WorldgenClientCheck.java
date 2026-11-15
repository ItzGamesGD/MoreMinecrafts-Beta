package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
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
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import dev.itzgamesgd.moreminecrafts.registry.ModBlocks;

/**
 * Real-client worldgen check for the ST-03 Pillager Headquarters (gated by -Dmoreminecrafts.st03.worldgen.check=true).
 * Fresh world: locates three HQs, records coordinates and biome, enters each to see the first-entry garrison,
 * captures exterior and interior screenshots, clears the first HQ through the real death path, places 5 beds,
 * then watches for post-clear villager conversion. Results are printed as ST03_WORLDGEN lines.
 */
public final class St03WorldgenClientCheck implements FabricClientGameTest {
    private static final Identifier HQ_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st03_pillager_headquarters");
    private static final int SEARCH_RADIUS_CHUNKS = 48;
    private static final int HQ_COUNT = 3;

    /** Server-side results, written on the server thread and read on the client thread. */
    static final List<BlockPos> LOCATED = new ArrayList<>();
    static final List<String> BIOMES = new ArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.st03.worldgen.check")) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");
        // Locating structures can take longer than the default 60 s watchdog on this sandbox; disable it for this check only.
        properties.setProperty("max-tick-time", "-1");
        // Normal overworld (not the flat default) with a fixed seed, so HQ placement is the real worldgen path.
        var worldBuilder = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(st -> {
            st.setWorldType(st.getNormalPresetList().getFirst());
            st.setSeed("7001");
        });
        try (TestDedicatedServerContext server = worldBuilder.createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.waitTicks(40);

            // 1. Locate HQs in a fresh world: one search origin per server tick (a single long tick trips the watchdog).
            for (int attempt = 0; attempt < 25 && LOCATED.size() < HQ_COUNT; attempt++) {
                final int a = attempt;
                server.runOnServer(s -> locateOne(s.overworld(), a));
                connection.waitForClientboundPackets();
            }
            System.out.println("ST03_WORLDGEN located_count=" + LOCATED.size());
            connection.waitForClientboundPackets();
            if (LOCATED.size() < HQ_COUNT) {
                throw new AssertionError("ST03_WORLDGEN found only " + LOCATED.size() + " HQs in 25 search origins");
            }

            // 2. Enter each HQ and count the first-entry garrison by type.
            for (int i = 0; i < LOCATED.size(); i++) {
                BlockPos hqPos = LOCATED.get(i);
                int index = i;
                server.runOnServer(s -> teleport(s, s.overworld(), hqPos.getX() + 0.5, surface(s.overworld(), hqPos) + 1, hqPos.getZ() + 0.5, 0, 0));
                connection.waitForClientboundPackets();
                context.waitTicks(160);
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    St03HeadquartersData.HqInstance hq = findHq(level, hqPos);
                    if (hq == null) {
                        System.out.println("ST03_WORLDGEN hq=" + index + " NO_INSTANCE");
                        return;
                    }
                    System.out.println("ST03_WORLDGEN hq=" + index + " id=" + hq.id() + " garrisonInitialized=" + hq.garrisonInitialized
                            + " box=" + hq.box + " origin=" + hq.origin);
                    printGarrison(level, hq, "hq=" + index);
                });
                connection.waitForClientboundPackets();
                if (i == 0) {
                    // Exterior: about 50 blocks out, looking toward the compound.
                    server.runOnServer(s -> teleport(s, s.overworld(), hqPos.getX() - 50.5, surface(s.overworld(), hqPos) + 22, hqPos.getZ() - 50.5, -45, 12));
                    connection.waitForClientboundPackets();
                    context.waitTicks(60);
                    System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-00-hq-exterior"));

                    // Interior: inside the compound, looking at the garrison.
                    server.runOnServer(s -> teleport(s, s.overworld(), hqPos.getX() + 56.5, surface(s.overworld(), hqPos) + 1, hqPos.getZ() + 56.5, 0, 10));
                    connection.waitForClientboundPackets();
                    context.waitTicks(60);
                    System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-01-hq-interior-garrison"));
                }
            }

            // 3. Post-clear for HQ 0: kill every defender via the normal death path, then place 5 beds.
            BlockPos firstPos = LOCATED.get(0);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                St03HeadquartersData.HqInstance hq = findHq(level, firstPos);
                if (hq == null) return;
                for (Entity e : level.getEntities((Entity) null, boxOf(hq), e -> e.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id()))) {
                    if (e instanceof LivingEntity living && !(e instanceof Villager)) {
                        living.kill(level);
                    }
                }
                System.out.println("ST03_WORLDGEN post_clear_kill issued cleared=" + hq.cleared + " eliminated=" + hq.eliminatedSlots.size());
            });
            connection.waitForClientboundPackets();
            context.waitTicks(40);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                St03HeadquartersData.HqInstance hq = findHq(level, firstPos);
                if (hq == null) return;
                for (int i = 0; i < 5; i++) {
                    BlockPos foot = hq.origin.offset(1 + i * 2, 1, 2);
                    BlockPos head = foot.south();
                    level.setBlock(foot, ModBlocks.EXPLORATION_SLEEPING_BAG.defaultBlockState()
                            .setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.SOUTH), 3);
                    level.setBlock(head, ModBlocks.EXPLORATION_SLEEPING_BAG.defaultBlockState()
                            .setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.SOUTH), 3);
                }
                System.out.println("ST03_WORLDGEN beds_placed=5 bed_count=" + St03PostClearHandler.countBedsInBox(level, hq.box)
                        + " cleared=" + hq.cleared + " eliminated=" + hq.eliminatedSlots.size());
            });
            connection.waitForClientboundPackets();

            // 4. Wait across the morning window for conversion; report villagers inside the HQ box.
            for (int wait = 0; wait < 4; wait++) {
                context.waitTicks(400);
                final int tickWait = wait;
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    St03HeadquartersData.HqInstance hq = findHq(level, firstPos);
                    if (hq == null) return;
                    int villagers = level.getEntities((Entity) null, boxOf(hq), v -> v instanceof Villager).size();
                    System.out.println("ST03_WORLDGEN conversion tick_wait=" + tickWait + " spawnedVillagers=" + hq.spawnedVillagers
                            + " villagersInBox=" + villagers + " gameTime=" + level.getGameTime());
                });
                connection.waitForClientboundPackets();
            }
            server.runOnServer(s -> teleport(s, s.overworld(), firstPos.getX() + 0.5, surface(s.overworld(), firstPos) + 1, firstPos.getZ() + 0.5, 0, 10));
            connection.waitForClientboundPackets();
            context.waitTicks(60);
            System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-02-post-clear-conversion"));
            System.out.println("ST03_WORLDGEN_CHECK_COMPLETE");
        }
    }

    static void locateOne(ServerLevel level, int attempt) {
        var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, HQ_ID));
        int x = ((attempt % 5) - 2) * 1500;
        int z = ((attempt / 5) - 2) * 1500;
        long t0 = System.nanoTime();
        BlockPos found = level.findNearestMapStructure(HolderSet.direct(holder), new BlockPos(x, 64, z), SEARCH_RADIUS_CHUNKS, false);
        System.out.println("ST03_WORLDGEN search_ms=" + (System.nanoTime() - t0) / 1_000_000 + " origin=" + x + "," + z);
        if (found == null) {
            System.out.println("ST03_WORLDGEN search origin=" + x + "," + z + " found=none");
            return;
        }
        boolean duplicate = LOCATED.stream().anyMatch(p -> p.distSqr(found) < 2000L * 2000L);
        if (duplicate) return;
        LOCATED.add(found);
        String biome = level.getBiome(found).unwrapKey().map(k -> k.identifier().toString()).orElse("unknown");
        BIOMES.add(biome);
        System.out.println("ST03_WORLDGEN located hq=" + LOCATED.size() + " pos=" + found
                + " chunk=" + (found.getX() >> 4) + "," + (found.getZ() >> 4) + " biome=" + biome + " origin=" + x + "," + z);
    }

    static St03HeadquartersData.HqInstance findHq(ServerLevel level, BlockPos pos) {
        for (St03HeadquartersData.HqInstance hq : St03HeadquartersData.get(level).instances().values()) {
            if (hq.box != null && hq.box.isInside(pos)) return hq;
        }
        return null;
    }

    static AABB boxOf(St03HeadquartersData.HqInstance hq) {
        return new AABB(hq.box.minX(), hq.box.minY(), hq.box.minZ(), hq.box.maxX() + 1, hq.box.maxY() + 1, hq.box.maxZ() + 1);
    }

    private static void printGarrison(ServerLevel level, St03HeadquartersData.HqInstance hq, String label) {
        TreeMap<String, Integer> byType = new TreeMap<>();
        int defenders = 0;
        for (Entity e : level.getEntities((Entity) null, boxOf(hq), e -> e.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id()))) {
            if (e instanceof Villager) continue;
            defenders++;
            byType.merge(e.getType().toShortString(), 1, Integer::sum);
        }
        System.out.println("ST03_WORLDGEN_GARRISON " + label + " defenders=" + defenders + " byType=" + byType);
    }

    private static double surface(ServerLevel level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
    }

    private static void teleport(net.minecraft.server.MinecraftServer server, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.teleportTo(level, x, y, z, Set.of(), yaw, pitch, true);
        }
    }
}
