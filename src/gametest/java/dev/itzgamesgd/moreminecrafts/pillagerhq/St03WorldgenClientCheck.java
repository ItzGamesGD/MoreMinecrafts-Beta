package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03Eligibility;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03PlacementConfig;
import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03HeadquartersStructure;
import dev.itzgamesgd.moreminecrafts.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;

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
    static final Map<String, Integer> VERDICTS = new TreeMap<>();
    static final Map<String, Integer> BIOME_HIST = new TreeMap<>();

    private static volatile boolean diagEventsRegistered;
    static final Map<UUID, String> TRACKED_META = new ConcurrentHashMap<>();
    static final Map<UUID, String> LAST_UNLOAD_REASON = new ConcurrentHashMap<>();
    static final Map<Integer, UUID> HQ0_SLOT_UUIDS = new ConcurrentHashMap<>();

    private static void registerDiagEventsIfNeeded() {
        if (diagEventsRegistered || !Boolean.getBoolean("moreminecrafts.st03.diag")) {
            return;
        }
        diagEventsRegistered = true;
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            String hqTag = findTagWithPrefix(entity, St03GarrisonManager.TAG_HQ_PREFIX);
            if (hqTag == null && !TRACKED_META.containsKey(entity.getUUID())) {
                return;
            }
            String slotTag = findTagWithPrefix(entity, St03GarrisonManager.TAG_SLOT_PREFIX);
            String role = entity.entityTags().contains(St03GarrisonManager.TAG_PRISONER)
                    ? "prisoner"
                    : (slotTag != null ? slotTag : "none");
            String meta = hqTag + "|" + role + "|" + entity.getType().toShortString();
            TRACKED_META.putIfAbsent(entity.getUUID(), meta);
            boolean persist = entity instanceof Mob m && m.isPersistenceRequired();
            boolean remFar = entity instanceof Mob m && m.removeWhenFarAway(10000.0D);
            boolean entitiesLoaded = level.areEntitiesLoaded(entity.chunkPosition().pack());
            System.out.println("ST03_DIAG ENTITY_LOAD meta=" + meta + " uuid=" + entity.getUUID()
                    + " pos=" + entity.blockPosition() + " chunk=" + entity.chunkPosition()
                    + " entitiesLoaded=" + entitiesLoaded + " persist=" + persist
                    + " removeWhenFarAway=" + remFar + " alive=" + entity.isAlive()
                    + " gameTime=" + level.getGameTime());
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            String hqTag = findTagWithPrefix(entity, St03GarrisonManager.TAG_HQ_PREFIX);
            if (hqTag == null && !TRACKED_META.containsKey(entity.getUUID())) {
                return;
            }
            String meta = TRACKED_META.getOrDefault(entity.getUUID(), hqTag + "|" + entity.getType().toShortString());
            Entity.RemovalReason reason = entity.getRemovalReason();
            String reasonStr = reason == null ? "null" : reason.name();
            LAST_UNLOAD_REASON.put(entity.getUUID(), reasonStr + "@" + level.getGameTime());
            boolean deadOrDying = entity instanceof LivingEntity le && le.isDeadOrDying();
            float health = entity instanceof LivingEntity le ? le.getHealth() : -1.0F;
            System.out.println("ST03_DIAG ENTITY_UNLOAD meta=" + meta + " uuid=" + entity.getUUID()
                    + " pos=" + entity.blockPosition() + " chunk=" + entity.chunkPosition()
                    + " reason=" + reasonStr + " deadOrDying=" + deadOrDying
                    + " health=" + health + " gameTime=" + level.getGameTime());
        });
    }

    private static String findTagWithPrefix(Entity entity, String prefix) {
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(prefix)) {
                return tag;
            }
        }
        return null;
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.st03.worldgen.check")) {
            return;
        }
        final boolean diag = Boolean.getBoolean("moreminecrafts.st03.diag");
        registerDiagEventsIfNeeded();

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

            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                // Keep the test observer alive in Survival so 40 garrison defenders do not kill the player
                // and drop chunk-loading tickets mid-run (Part 1 root cause of the 1e7eeb19 disappearance).
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                System.out.println("ST03_WORLDGEN env levelDifficulty=" + level.getDifficulty()
                        + " worldDifficulty=" + s.getWorldData().getDifficulty()
                        + " playerGameMode=" + p.gameMode()
                        + " invulnerable=" + p.getAbilities().invulnerable
                        + " playerHp=" + p.getHealth()
                        + " viewDist=6 simDist=6");
            });
            connection.waitForClientboundPackets();

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
                    ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                    System.out.println("ST03_WORLDGEN hq=" + index + " origin=" + c[0] + "," + c[1] + "," + c[2]
                            + " generated=" + generated + " forced=" + !generated + " id=" + hq.id() + " box=" + hq.box
                            + " playerHp=" + p.getHealth() + " playerDead=" + p.isDeadOrDying() + " playerPos=" + p.blockPosition());
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

                if (diag && i == 0) {
                    server.runOnServer(s -> recordHq0Uuids(s.overworld(), c[0], c[2]));
                    connection.waitForClientboundPackets();
                }

                if (i == 0) {
                    server.runOnServer(s -> teleport(s, s.overworld(), c[0] - 40.5, c[1] + 18, c[2] - 40.5, -45, 12));
                    connection.waitForClientboundPackets();
                    context.waitTicks(60);
                    System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-00-hq-exterior"));

                    server.runOnServer(s -> teleport(s, s.overworld(), c[0] + 56.5, c[1] + 3, c[2] + 56.5, 0, 10));
                    connection.waitForClientboundPackets();
                    context.waitTicks(60);
                    System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-01-hq-interior-garrison"));
                    if (diag) {
                        server.runOnServer(s -> inspectHq0Uuids(s.overworld(), c[0], c[2], "AFTER_SHOTS_HQ0"));
                        connection.waitForClientboundPackets();
                        context.waitTicks(1200);
                        server.runOnServer(s -> inspectHq0Uuids(s.overworld(), c[0], c[2], "EXP_A_STAY_1200"));
                        connection.waitForClientboundPackets();
                    }
                }

                // Part 2 Control A (pre-clear with 5 beds on HQ 1 while cleared == false): verify zero villager conversion.
                if (i == 1) {
                    server.runOnServer(s -> {
                        ServerLevel level = s.overworld();
                        St03HeadquartersData.HqInstance hq1 = findHq(level, c[0], c[2]);
                        if (hq1 == null) return;
                        buildPlazaAndBedFloor(level, hq1);
                        placeFiveBeds(level, hq1);
                        int beds = St03PostClearHandler.countBedsInBox(level, hq1.box);
                        int nonPrisoners = countSpawnedVillagersInBox(level, hq1);
                        System.out.println("ST03_WORLDGEN control_pre_clear_start hq=1 cleared=" + hq1.cleared
                                + " beds=" + beds + " spawnedVillagers=" + hq1.spawnedVillagers
                                + " nonPrisonerVillagers=" + nonPrisoners + " gameTime=" + level.getGameTime());
                    });
                    connection.waitForClientboundPackets();
                    sprintServerTicks(server, connection, context, 24000L);
                    server.runOnServer(s -> {
                        ServerLevel level = s.overworld();
                        St03HeadquartersData.HqInstance hq1 = findHq(level, c[0], c[2]);
                        if (hq1 == null) return;
                        int beds = St03PostClearHandler.countBedsInBox(level, hq1.box);
                        int nonPrisoners = countSpawnedVillagersInBox(level, hq1);
                        System.out.println("ST03_WORLDGEN control_pre_clear_end hq=1 cleared=" + hq1.cleared
                                + " beds=" + beds + " spawnedVillagers=" + hq1.spawnedVillagers
                                + " nonPrisonerVillagers=" + nonPrisoners + " gameTime=" + level.getGameTime());
                        if (hq1.cleared || hq1.spawnedVillagers != 0 || nonPrisoners != 0) {
                            throw new AssertionError("Pre-clear control converted unexpectedly: cleared=" + hq1.cleared
                                    + " spawned=" + hq1.spawnedVillagers + " nonPrisoners=" + nonPrisoners);
                        }
                    });
                    connection.waitForClientboundPackets();
                }
            }

            // 3. Return to HQ 0 and clear all 40 defenders via real death path (hurtServer).
            int[] first = chosen.get(0);
            System.out.println("ST03_WORLDGEN return_to_hq0 target=" + first[0] + "," + first[1] + "," + first[2] + " chosenSize=" + chosen.size());
            server.runOnServer(s -> {
                ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
                teleport(s, s.overworld(), first[0] + 56.5, first[1] + 3, first[2] + 56.5, 0, 10);
                System.out.println("ST03_WORLDGEN return_to_hq0 player_after=" + p.blockPosition()
                        + " playerHp=" + p.getHealth() + " playerDead=" + p.isDeadOrDying());
            });
            connection.waitForClientboundPackets();
            if (diag) {
                context.waitTicks(40);
                server.runOnServer(s -> inspectHq0Uuids(s.overworld(), first[0], first[2], "EXP_B_RETURN_T40"));
                connection.waitForClientboundPackets();
                context.waitTicks(1200);
                server.runOnServer(s -> inspectHq0Uuids(s.overworld(), first[0], first[2], "EXP_B_RETURN_T1240"));
                connection.waitForClientboundPackets();
            } else {
                context.waitTicks(120);
            }

            // Build darkroom probes inside and outside HQ 0, wait a few ticks for sky light=0, then verify pre-clear spawn rules.
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                St03HeadquartersData.HqInstance hq = findHq(level, first[0], first[2]);
                if (hq == null) return;
                buildDarkRoom(level, hq.origin.offset(20, 0, 20));
                buildDarkRoom(level, hq.origin.offset(-12, 0, 20));
            });
            connection.waitForClientboundPackets();
            context.waitTicks(10);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                St03HeadquartersData.HqInstance hq = findHq(level, first[0], first[2]);
                if (hq == null) return;
                checkSpawnRules(level, hq, "pre_clear");
            });
            connection.waitForClientboundPackets();

            // Kill passes until none are left in the loaded box (using real hurtServer -> AFTER_DEATH path).
            for (int pass = 0; pass < 15; pass++) {
                final int passNo = pass;
                final int[] left = {0};
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    St03HeadquartersData.HqInstance hq = findHq(level, first[0], first[2]);
                    if (hq == null) return;
                    for (Entity e : level.getEntities((Entity) null, boxOf(hq).inflate(32),
                            e -> e.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id()))) {
                        if (e instanceof LivingEntity living && !(e instanceof Villager)) {
                            boolean hurtRes = living.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
                            if (diag) {
                                System.out.println("ST03_DIAG KILL_ATTEMPT pass=" + passNo
                                        + " uuid=" + living.getUUID() + " type=" + living.getType().toShortString()
                                        + " hurtRes=" + hurtRes + " alive=" + living.isAlive()
                                        + " deadOrDying=" + living.isDeadOrDying() + " hp=" + living.getHealth()
                                        + " removed=" + living.isRemoved() + " pos=" + living.blockPosition());
                            }
                        }
                    }
                    left[0] = defenderCount(level, first[0], first[2]);
                    System.out.println("ST03_WORLDGEN kill_pass=" + passNo + " player=" + s.getPlayerList().getPlayers().getFirst().blockPosition()
                            + " loadedInBox=" + level.getEntities((Entity) null, boxOf(hq), e -> true).size()
                            + " defendersLeft=" + left[0] + " eliminated=" + hq.eliminatedSlots.size() + " cleared=" + hq.cleared);
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
                if (!hq.cleared || hq.eliminatedSlots.size() != SLOT_COUNT || defenderCount(level, first[0], first[2]) != 0) {
                    throw new AssertionError("HQ 0 did not clear cleanly: cleared=" + hq.cleared
                            + " eliminated=" + hq.eliminatedSlots.size() + " defendersLeft=" + defenderCount(level, first[0], first[2]));
                }
                checkSpawnRules(level, hq, "post_clear");
                // Place solid floor under plaza & beds in HQ 0 (superflat has air at origin.y + 0).
                buildPlazaAndBedFloor(level, hq);
                int beds = St03PostClearHandler.countBedsInBox(level, hq.box);
                System.out.println("ST03_WORLDGEN control_0_beds_start cleared=" + hq.cleared + " beds=" + beds
                        + " spawnedVillagers=" + hq.spawnedVillagers
                        + " nonPrisonerVillagers=" + countSpawnedVillagersInBox(level, hq)
                        + " gameTime=" + level.getGameTime());
            });
            connection.waitForClientboundPackets();

            // 4. Part 2 Control B: 0 beds after clear across 1 full day (24,000 ticks) -> must remain 0 villagers.
            sprintServerTicks(server, connection, context, 24000L);
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                St03HeadquartersData.HqInstance hq = findHq(level, first[0], first[2]);
                if (hq == null) return;
                int beds = St03PostClearHandler.countBedsInBox(level, hq.box);
                int nonPrisoners = countSpawnedVillagersInBox(level, hq);
                System.out.println("ST03_WORLDGEN control_0_beds_end cleared=" + hq.cleared + " beds=" + beds
                        + " spawnedVillagers=" + hq.spawnedVillagers
                        + " nonPrisonerVillagers=" + nonPrisoners
                        + " gameTime=" + level.getGameTime());
                if (beds != 0 || hq.spawnedVillagers != 0 || nonPrisoners != 0) {
                    throw new AssertionError("0-bed post-clear control converted unexpectedly: beds=" + beds
                            + " spawned=" + hq.spawnedVillagers + " nonPrisoners=" + nonPrisoners);
                }

                // 5. Place 5 beds inside HQ 0 plaza area and verify day-by-day villager growth up to the cap of 15.
                placeFiveBeds(level, hq);
                int countedBeds = St03PostClearHandler.countBedsInBox(level, hq.box);
                System.out.println("ST03_WORLDGEN beds placed=5 counted=" + countedBeds);
                System.out.println("ST03_WORLDGEN conversion_day day=0 gameTime=" + level.getGameTime()
                        + " timeOfDay=" + (level.getGameTime() % 24000L)
                        + " beds=" + countedBeds
                        + " spawnedVillagers=" + hq.spawnedVillagers
                        + " nonPrisonerVillagers=" + countSpawnedVillagersInBox(level, hq)
                        + " totalVillagersInBox=" + level.getEntities((Entity) null, boxOf(hq), v -> v instanceof Villager).size()
                        + " attemptsToday=" + hq.villagerAttemptsToday);
                if (countedBeds < 5) {
                    throw new AssertionError("Expected 5 counted beds, got " + countedBeds);
                }
            });
            connection.waitForClientboundPackets();

            final boolean[] hitCapExtraDayDone = {false};
            int capReachedDay = -1;
            for (int day = 1; day <= 12; day++) {
                final int dayNo = day;
                // Advance 1 day = 24,000 ticks split into 4 x 6,000-tick sprints to cover morning windows and keep ticks bounded.
                for (int q = 0; q < 4; q++) {
                    sprintServerTicks(server, connection, context, 6000L);
                }
                final int[] spawnedAfterDay = {0};
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    St03HeadquartersData.HqInstance hq = findHq(level, first[0], first[2]);
                    if (hq == null) return;
                    int countedBeds = St03PostClearHandler.countBedsInBox(level, hq.box);
                    int nonPrisoners = countSpawnedVillagersInBox(level, hq);
                    int totalInBox = level.getEntities((Entity) null, boxOf(hq), v -> v instanceof Villager).size();
                    spawnedAfterDay[0] = hq.spawnedVillagers;
                    System.out.println("ST03_WORLDGEN conversion_day day=" + dayNo
                            + " gameTime=" + level.getGameTime()
                            + " timeOfDay=" + (level.getGameTime() % 24000L)
                            + " beds=" + countedBeds
                            + " spawnedVillagers=" + hq.spawnedVillagers
                            + " nonPrisonerVillagers=" + nonPrisoners
                            + " totalVillagersInBox=" + totalInBox
                            + " attemptsToday=" + hq.villagerAttemptsToday);
                });
                connection.waitForClientboundPackets();
                if (spawnedAfterDay[0] > 15) {
                    throw new AssertionError("Villager count exceeded cap of 15: " + spawnedAfterDay[0]);
                }
                if (capReachedDay < 0 && spawnedAfterDay[0] == 15) {
                    capReachedDay = dayNo;
                } else if (capReachedDay > 0 && dayNo > capReachedDay) {
                    hitCapExtraDayDone[0] = true;
                    break;
                }
            }
            if (capReachedDay < 0 || !hitCapExtraDayDone[0]) {
                throw new AssertionError("Did not verify 15-villager cap + 1 extra stability day: capReachedDay=" + capReachedDay);
            }

            server.runOnServer(s -> teleport(s, s.overworld(), first[0] + 55.5, first[1] + 2.5, first[2] + 42.5, 180, 15));
            connection.waitForClientboundPackets();
            context.waitTicks(60);
            System.out.println("ST03_WORLDGEN_SHOT " + context.takeScreenshot("st03-worldgen-02-post-clear-conversion"));
            System.out.println("ST03_WORLDGEN_CHECK_COMPLETE");
        }
    }

    private static boolean loggedTickManagerMethods;

    private static void sprintServerTicks(TestDedicatedServerContext server, TestDedicatedServerConnection connection,
                                          ClientGameTestContext context, long ticks) {
        final long[] start = {0L};
        server.runOnServer(s -> start[0] = s.overworld().getGameTime());
        connection.waitForClientboundPackets();
        long target = start[0] + ticks;
        long current = start[0];
        long wallStart = System.currentTimeMillis();
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            var tm = s.tickRateManager();
            if (!loggedTickManagerMethods) {
                loggedTickManagerMethods = true;
                StringBuilder methods = new StringBuilder();
                for (var m : tm.getClass().getMethods()) {
                    if (m.getDeclaringClass() != Object.class) {
                        methods.append(m.getName()).append('(').append(m.getParameterCount()).append(") ");
                    }
                }
                System.out.println("ST03_WORLDGEN tick_rate_manager class=" + tm.getClass().getName()
                        + " methods=[" + methods.toString().trim() + "]");
            }
            List<String> msgs = new ArrayList<>();
            var src = s.createCommandSourceStack().withLevel(level)
                    .withCallback((ok, code) -> msgs.add("cb:ok=" + ok + ",code=" + code))
                    .withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
            // Try running /tick sprint without suppressed output
            s.getCommands().performPrefixedCommand(src, "tick sprint " + ticks);
            boolean sprintingAfterCmd = isTickSprinting(tm);
            if (!sprintingAfterCmd) {
                invokeRequestSprint(tm, (int) ticks);
            }
            System.out.println("ST03_WORLDGEN sprint_start requested=" + ticks
                    + " startGameTime=" + level.getGameTime()
                    + " sprintingAfterCmd=" + sprintingAfterCmd
                    + " sprintingNow=" + isTickSprinting(tm)
                    + " msgs=" + msgs);
        });
        connection.waitForClientboundPackets();

        for (int iter = 0; iter < 60 && current < target; iter++) {
            context.waitTicks(10);
            final long[] now = {0L};
            final boolean[] sprinting = {false};
            server.runOnServer(s -> {
                now[0] = s.overworld().getGameTime();
                sprinting[0] = isTickSprinting(s.tickRateManager());
            });
            connection.waitForClientboundPackets();
            current = now[0];
            if (iter == 0 || iter % 10 == 0 || current >= target) {
                long elapsedMs = Math.max(1L, System.currentTimeMillis() - wallStart);
                long delta = current - start[0];
                System.out.println("ST03_WORLDGEN sprint_poll iter=" + iter + " current=" + current
                        + " target=" + target + " delta=" + delta + " elapsedMs=" + elapsedMs
                        + " tps=" + String.format("%.1f", delta * 1000.0 / elapsedMs)
                        + " sprinting=" + sprinting[0]);
            }
        }
        if (current < target) {
            throw new AssertionError("sprintServerTicks timed out: current=" + current + " target=" + target
                    + " delta=" + (current - start[0]) + " elapsedMs=" + (System.currentTimeMillis() - wallStart));
        }
    }

    private static boolean isTickSprinting(Object tickRateManager) {
        try {
            var m = tickRateManager.getClass().getMethod("isSprinting");
            return Boolean.TRUE.equals(m.invoke(tickRateManager));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void invokeRequestSprint(Object tickRateManager, int ticks) {
        try {
            var m = tickRateManager.getClass().getMethod("requestGameToSprint", int.class);
            m.invoke(tickRateManager, ticks);
        } catch (Throwable t) {
            System.out.println("ST03_WORLDGEN sprint_invoke_err=" + t);
        }
    }

    private static void buildPlazaAndBedFloor(ServerLevel level, St03HeadquartersData.HqInstance hq) {
        for (int dx = 48; dx <= 62; dx++) {
            for (int dz = 25; dz <= 42; dz++) {
                level.setBlock(hq.origin.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    private static void placeFiveBeds(ServerLevel level, St03HeadquartersData.HqInstance hq) {
        for (int i = 0; i < 5; i++) {
            BlockPos foot = hq.origin.offset(50 + i * 2, 1, 27);
            BlockPos head = foot.south();
            level.setBlock(foot, ModBlocks.EXPLORATION_SLEEPING_BAG.defaultBlockState()
                    .setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.SOUTH), 3);
            level.setBlock(head, ModBlocks.EXPLORATION_SLEEPING_BAG.defaultBlockState()
                    .setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.SOUTH), 3);
        }
    }

    private static void buildDarkRoom(ServerLevel level, BlockPos base) {
        // 9x9x5 completely sealed stone enclosure (dy=0 floor, dy=1..3 interior air, dy=4 ceiling)
        for (int dx = 0; dx < 9; dx++) {
            for (int dz = 0; dz < 9; dz++) {
                for (int dy = 0; dy <= 4; dy++) {
                    boolean isWall = dx == 0 || dx == 8 || dz == 0 || dz == 8 || dy == 0 || dy == 4;
                    level.setBlock(base.offset(dx, dy, dz),
                            isWall ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                            3);
                }
            }
        }
    }

    private static void checkSpawnRules(ServerLevel level, St03HeadquartersData.HqInstance hq, String phase) {
        BlockPos insideDark = hq.origin.offset(24, 1, 24);
        BlockPos outsideDark = hq.origin.offset(-8, 1, 24);
        EntityType<?>[] types = new EntityType<?>[]{
                net.minecraft.world.entity.EntityTypes.ZOMBIE,
                net.minecraft.world.entity.EntityTypes.CREEPER,
                net.minecraft.world.entity.EntityTypes.ENDERMAN,
                net.minecraft.world.entity.EntityTypes.SKELETON,
                net.minecraft.world.entity.EntityTypes.SPIDER
        };
        for (EntityType<?> type : types) {
            boolean suppressed = St03PostClearHandler.shouldSuppressNaturalSpawn(level, type, EntitySpawnReason.NATURAL, insideDark);
            boolean insideAllowed = net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(
                    type, level, EntitySpawnReason.NATURAL, insideDark, level.getRandom());
            boolean outsideAllowed = net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(
                    type, level, EntitySpawnReason.NATURAL, outsideDark, level.getRandom());
            System.out.println("ST03_WORLDGEN hostile_check phase=" + phase + " cleared=" + hq.cleared
                    + " type=" + type.toShortString()
                    + " suppressedFlag=" + suppressed
                    + " insideSpawnAllowed=" + insideAllowed
                    + " outsideSpawnAllowed=" + outsideAllowed);
            if ("post_clear".equals(phase) && (!suppressed || insideAllowed)) {
                throw new AssertionError("Expected suppression post-clear for " + type.toShortString()
                        + " suppressed=" + suppressed + " insideAllowed=" + insideAllowed);
            }
        }
    }

    private static int countSpawnedVillagersInBox(ServerLevel level, St03HeadquartersData.HqInstance hq) {
        int count = 0;
        for (Entity e : level.getEntities((Entity) null, boxOf(hq),
                v -> v instanceof Villager && !v.entityTags().contains(St03GarrisonManager.TAG_PRISONER))) {
            count++;
        }
        return count;
    }

    private static void recordHq0Uuids(ServerLevel level, int x, int z) {
        St03HeadquartersData.HqInstance hq = findHq(level, x, z);
        if (hq == null) return;
        HQ0_SLOT_UUIDS.clear();
        for (Entity e : level.getEntities((Entity) null, boxOf(hq).inflate(64),
                e -> e.entityTags().contains(St03GarrisonManager.TAG_HQ_PREFIX + hq.id()))) {
            if (e instanceof Villager) continue;
            String slotTag = findTagWithPrefix(e, St03GarrisonManager.TAG_SLOT_PREFIX);
            int slot = slotTag == null ? -1 : Integer.parseInt(slotTag.substring(St03GarrisonManager.TAG_SLOT_PREFIX.length()));
            HQ0_SLOT_UUIDS.put(slot, e.getUUID());
            System.out.println("ST03_DIAG HQ0_UUID slot=" + slot + " type=" + e.getType().toShortString()
                    + " uuid=" + e.getUUID() + " pos=" + e.blockPosition()
                    + " inBox=" + hq.contains(e.blockPosition())
                    + " entitiesLoaded=" + level.areEntitiesLoaded(e.chunkPosition().pack()));
        }
        System.out.println("ST03_DIAG HQ0_RECORDED count=" + HQ0_SLOT_UUIDS.size() + " expected=" + SLOT_COUNT);
    }

    private static void inspectHq0Uuids(ServerLevel level, int x, int z, String phase) {
        St03HeadquartersData.HqInstance hq = findHq(level, x, z);
        if (hq == null) return;
        int found = 0;
        int alive = 0;
        int inBox = 0;
        int outOfBox = 0;
        int missing = 0;
        Map<String, Integer> missingReasons = new TreeMap<>();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            UUID uuid = HQ0_SLOT_UUIDS.get(slot);
            if (uuid == null) {
                missing++;
                missingReasons.merge("NEVER_RECORDED", 1, Integer::sum);
                continue;
            }
            Entity e = level.getEntity(uuid);
            if (e == null) {
                missing++;
                String lastUnload = LAST_UNLOAD_REASON.getOrDefault(uuid, "NO_UNLOAD_EVENT");
                missingReasons.merge(lastUnload.replaceAll("@\\d+", ""), 1, Integer::sum);
                System.out.println("ST03_DIAG " + phase + " MISSING slot=" + slot + " uuid=" + uuid
                        + " lastUnload=" + lastUnload);
            } else {
                found++;
                if (e.isAlive()) alive++;
                boolean inside = hq.contains(e.blockPosition());
                if (inside) {
                    inBox++;
                } else {
                    outOfBox++;
                    System.out.println("ST03_DIAG " + phase + " OUT_OF_BOX slot=" + slot + " uuid=" + uuid
                            + " type=" + e.getType().toShortString() + " pos=" + e.blockPosition()
                            + " alive=" + e.isAlive());
                }
            }
        }
        int totalChunks = 0;
        int loadedChunks = 0;
        for (int cx = hq.box.minX() >> 4; cx <= hq.box.maxX() >> 4; cx++) {
            for (int cz = hq.box.minZ() >> 4; cz <= hq.box.maxZ() >> 4; cz++) {
                totalChunks++;
                if (level.areEntitiesLoaded(ChunkPos.pack(cx, cz))) loadedChunks++;
            }
        }
        ServerPlayer p = level.getServer().getPlayerList().getPlayers().getFirst();
        System.out.println("ST03_DIAG " + phase + " SUMMARY found=" + found + "/40 alive=" + alive
                + " inBox=" + inBox + " outOfBox=" + outOfBox + " missing=" + missing
                + " boxDefenderCount=" + defenderCount(level, x, z)
                + " chunksEntitiesLoaded=" + loadedChunks + "/" + totalChunks
                + " missingReasons=" + missingReasons
                + " playerPos=" + p.blockPosition() + " playerHp=" + p.getHealth()
                + " gameTime=" + level.getGameTime());
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
