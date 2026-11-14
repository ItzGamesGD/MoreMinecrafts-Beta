package dev.itzgamesgd.moreminecrafts.pyramid.entrance;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardian;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEntities;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestData;
import dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.St01RoyalChestPlacement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * ST-01 P7-P11 secret entrance and gold guide golem on the ACTUAL approved P8-C2-A templates (not the historical P10
 * fixture). Covers: the 12-cell cut lands on the real cover in all four whole-pyramid rotations and changes nothing else;
 * Tomb Guardian defeat through the single-PRIMARY royal chest -> guide golem -> arrival -> cover open -> passage from the
 * royal hall to the fixed treasury, for both access routes; and the 600-tick fallback when the golem cannot be created.
 * Production settings ({@link EntranceConfig#PRODUCTION}) are used throughout; nothing here is gated by a property.
 */
public final class St01SecretEntranceGameTests {
    private static final int GROUND_Y = 150;
    /**
     * Tomb Guardian completion records cannot be deleted, and a reused GameTest world keeps them; every run therefore uses
     * fresh, chunk-aligned coordinates so a flow never starts from a previous run's defeat.
     */
    private static final int RUN_Z = 256 * java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 4000);

    // ---- placement helpers ----

    private static void forceChunks(ServerLevel level, BoundingBox box, boolean force) {
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
                level.setChunkForced(cx, cz, force);
            }
    }

    private static void clear(ServerLevel level, BoundingBox box) {
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Places only the D1 royal hall and the R1-01 seam (the pieces that hold the cover), with the assembly transform. */
    private static void placeHall(GameTestHelper ctx, ServerLevel level, BlockPos ground, Orientation o) {
        for (St01PyramidAssembly.Piece piece : St01PyramidAssembly.pieces(AccessRoute.EAST).subList(1, 3)) {
            var template = level.getStructureTemplateManager().get(piece.template()).orElseThrow();
            BlockPos origin = piece.placementOrigin(ground);
            ctx.assertTrue(template.placeInWorld(level, origin, origin, piece.settings(o), level.getRandom(), Block.UPDATE_CLIENTS),
                    piece.name() + " placed " + o);
        }
    }

    private static BlockState[] snapshot(ServerLevel level, BoundingBox box) {
        BlockState[] out = new BlockState[box.getXSpan() * box.getYSpan() * box.getZSpan()];
        int i = 0;
        for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++)
            out[i++] = level.getBlockState(new BlockPos(x, y, z));
        return out;
    }

    /** Every cell whose state differs between the snapshot and the level. */
    private static Set<BlockPos> changed(ServerLevel level, BoundingBox box, BlockState[] before) {
        Set<BlockPos> out = new HashSet<>();
        int i = 0;
        for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            BlockPos p = new BlockPos(x, y, z);
            if (before[i++] != level.getBlockState(p)) out.add(p);
        }
        return out;
    }

    private static boolean passable(ServerLevel level, BlockPos p) { return level.getBlockState(p).getCollisionShape(level, p).isEmpty(); }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        return passable(level, feet) && passable(level, feet.above()) && !passable(level, feet.below());
    }

    /** 6-connected flood over non-colliding cells inside {@code box}: does {@code from} connect to {@code to}? */
    private static boolean connected(ServerLevel level, BoundingBox box, BlockPos from, BlockPos to) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (p.equals(to)) return true;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (box.isInside(n) && passable(level, n) && seen.add(n)) queue.add(n);
            }
        }
        return false;
    }

    /** Forces a Tomb Guardian completion record without a fight (synthetic: only for the fallback / cut tests). */
    private static void recordSyntheticDefeat(ServerLevel level, BlockPos tombAnchor) {
        var data = TombGuardianEncounters.data(level);
        long tomb = TombGuardianEncounters.tombKey(tombAnchor);
        UUID fake = UUID.randomUUID();
        if (!data.isDefeated(tomb)) {
            data.bind(tomb, fake, net.minecraft.world.level.ChunkPos.containing(tombAnchor).pack(), level.getGameTime());
            data.markDefeated(tomb, fake);
        }
    }

    private static List<GoldGuideGolem> golems(ServerLevel level, BlockPos tombAnchor) {
        return level.getEntitiesOfClass(GoldGuideGolem.class, new AABB(tombAnchor).inflate(128.0D), g -> g.tomb() == tombAnchor.asLong() && g.isAlive());
    }

    private static SecretEntranceData.Instance inst(ServerLevel level, BlockPos tombAnchor) {
        return SecretEntranceRuntime.data(level).get(tombAnchor.asLong());
    }

    /** Polls {@code check} every tick until it returns true; then runs {@code then}. Fails the test at the deadline. */
    private static void await(GameTestHelper ctx, int deadline, String what, java.util.function.BooleanSupplier check, Runnable then) {
        int[] t = {0};
        Consumer<Object>[] loop = new Consumer[1];
        loop[0] = o -> {
            if (check.getAsBoolean()) { then.run(); return; }
            if (++t[0] > deadline) { ctx.fail("timed out waiting for " + what); return; }
            ctx.runAfterDelay(1, () -> loop[0].accept(null));
        };
        loop[0].accept(null);
    }

    // ---- 1. the cut, all four rotations ----

    @GameTest(maxTicks = 100)
    public void coverCutIsExactlyTheTwelveRealCoverCellsInEveryRotation(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        EntranceConfig cfg = EntranceConfig.PRODUCTION;
        for (Orientation o : Orientation.values()) {
            BlockPos ground = new BlockPos(44_000 + 192 * o.ordinal(), GROUND_Y, 44_000);
            BoundingBox box = St01PyramidAssembly.completionBox(ground, o);
            forceChunks(level, box, true);
            placeHall(ctx, level, ground, o);
            List<BlockPos> cover = EntranceLayout.coverCells(ground, o);
            for (BlockPos c : cover)
                ctx.assertTrue(McPorts.obstructs(level.getBlockState(c)), o + " CLOSED cover cell is solid " + c + " " + level.getBlockState(c));
            // The 12 approved R1-01 seam cells (X-8..-5 / Y-24 / Z33..35) under the cover are air in the placed assembly.
            for (int x = -8; x <= -5; x++) for (int z = 33; z <= 35; z++) {
                BlockPos seam = St01PyramidAssembly.worldPos(new BlockPos(x, -24, z), ground, o);
                ctx.assertTrue(level.getBlockState(seam).isAir(), o + " R1-01 seam cell is air " + seam);
            }
            BlockPos spawn = EntranceLayout.golemSpawn(ground, o), stop = EntranceLayout.stopPoint(ground, o);
            ctx.assertTrue(standable(level, spawn), o + " golem spawn " + spawn + " is a clear standable hall cell");
            ctx.assertTrue(standable(level, stop), o + " stop point " + stop + " is a clear standable hall cell");
            ctx.assertTrue(level.getBlockState(EntranceLayout.tombAnchor(ground, o)).is(Blocks.CHEST), o + " tomb anchor is the PRIMARY chest");

            ctx.assertTrue(SecretEntranceRuntime.register(level, ground, o), "registered " + o);
            SecretEntranceData.Instance instance = inst(level, EntranceLayout.tombAnchor(ground, o));
            McPorts ports = new McPorts(level, instance, cfg);
            ctx.assertTrue(!ports.victory() && !ports.coverOpen(), o + " sealed and closed before any defeat");
            BlockState[] before = snapshot(level, box);
            ports.openCover();
            ports.openCover(); // idempotent
            Set<BlockPos> diff = changed(level, box, before);
            ctx.assertTrue(diff.equals(new HashSet<>(cover)), o + " opening changes exactly the 12 cover cells, got " + diff.size() + " " + diff);
            ctx.assertTrue(ports.coverOpen(), o + " open");
            SecretEntranceRuntime.discardInstance(level, instance.tombAnchor());
            clear(level, box);
            forceChunks(level, box, false);
        }
        ctx.succeed();
    }

    // ---- 2. full flow on the complete assembly ----

    private static void fullFlow(GameTestHelper ctx, Orientation o, AccessRoute route, int slot) {
        EntranceConfig.resetToProduction();
        ServerLevel level = ctx.getLevel();
        BlockPos ground = new BlockPos(40_000 + 192 * slot, GROUND_Y, 40_000 + RUN_Z);
        BoundingBox box = St01PyramidAssembly.completionBox(ground, o);
        forceChunks(level, box, true);
        clear(level, box);
        ctx.assertTrue(St01PyramidAssembly.place(level, level.getStructureTemplateManager(), ground, o, route, level.getRandom(), Block.UPDATE_CLIENTS),
                "assembly placed " + o + "/" + route);
        St01PyramidAssembly.Placement placement = St01PyramidAssembly.describe(ground, o, route);
        RoyalChestData.Instance royal = St01RoyalChestPlacement.registerPlaced(level, placement.d1PlacementOrigin(), placement.d1Settings()).orElseThrow();
        ctx.assertTrue(SecretEntranceRuntime.register(level, placement), "entrance registered");
        ctx.assertTrue(!SecretEntranceRuntime.register(level, placement), "entrance registration is idempotent");
        BlockPos anchor = royal.tombAnchor();
        ctx.assertTrue(anchor.equals(EntranceLayout.tombAnchor(ground, o)) && royal.secondary().isEmpty(), "one key, single PRIMARY");

        List<BlockPos> cover = EntranceLayout.coverCells(ground, o);
        BlockPos stop = EntranceLayout.stopPoint(ground, o);
        BlockPos treasury = placement.treasuryUiAnchor().above(); // approved clear cell above the treasury UI chest
        ctx.assertTrue(passable(level, treasury), "treasury target cell is clear");
        ctx.assertTrue(!connected(level, box, stop, treasury), "CLOSED: the treasury is not reachable from the royal hall");

        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ctx.runAfterDelay(20, () -> {
            ctx.assertTrue(inst(level, anchor).state() == EntranceState.SEALED && golems(level, anchor).isEmpty(), "sealed before the fight");
            // The existing single-PRIMARY trigger starts the encounter.
            var start = RoyalChestRuntime.tryOpen(level, player, placement.primaryLeft());
            ctx.assertTrue(start == RoyalChestRuntime.OpenResult.SEALED_ENCOUNTER_STARTED, "PRIMARY starts the guardian, got " + start);
            List<TombGuardian> guardians = level.getEntitiesOfClass(TombGuardian.class, new AABB(anchor).inflate(48.0D), g -> g.isAlive());
            ctx.assertTrue(guardians.size() == 1, "one guardian");
            TombGuardian guardian = guardians.get(0);
            guardian.skipAwakening();
            ctx.runAfterDelay(5, () -> {
                ctx.assertTrue(inst(level, anchor).state() == EntranceState.SEALED, "the fight alone opens nothing");
                guardian.hurtServer(level, level.damageSources().playerAttack(player), 100_000.0F);
                ctx.assertTrue(guardian.isDeadOrDying() && TombGuardianEncounters.isDefeated(level, anchor), "guardian defeated");
                BlockState[] beforeOpen = snapshot(level, box);
                await(ctx, 10, "guide golem spawn", () -> golems(level, anchor).size() == 1, () -> {
                    GoldGuideGolem golem = golems(level, anchor).get(0);
                    ctx.assertTrue(golem.blockPosition().equals(EntranceLayout.golemSpawn(ground, o)), "golem at BOSS_S " + golem.blockPosition());
                    ctx.assertTrue(golem.stop().equals(stop), "golem targets the stop point");
                    ctx.assertTrue(inst(level, anchor).state() == EntranceState.GOLEM_GUIDING, "guiding");
                    ctx.assertTrue(!level.getBlockState(cover.get(0)).isAir(), "still closed while guiding");
                    await(ctx, (int) EntranceConfig.APPROVED_FALLBACK_TICKS + 40, "entrance open", () -> inst(level, anchor).state().isOpen(), () -> {
                        EntranceState state = inst(level, anchor).state();
                        ctx.assertTrue(state == EntranceState.ENTRANCE_OPEN, "the golem itself reached the stop point, got " + state);
                        ctx.assertTrue(inst(level, anchor).coverApplied(), "cover applied");
                        for (BlockPos c : cover) ctx.assertTrue(level.getBlockState(c).isAir(), "cover cell open " + c);
                        Set<BlockPos> diff = changed(level, box, beforeOpen);
                        ctx.assertTrue(diff.equals(new HashSet<>(cover)), "only the 12 cover cells changed, got " + diff.size() + " " + diff);
                        ctx.assertTrue(connected(level, box, stop, treasury), "OPEN: royal hall -> secret descent -> treasury passage exists");
                        // Coexistence with the single-PRIMARY royal chest: unsealed, same instance, no SECONDARY, pair intact.
                        ctx.assertTrue(!TombGuardianEncounters.isRoyalChestSealed(level, anchor), "royal chest unsealed");
                        ctx.assertTrue(RoyalChestRuntime.data(level).instance(royal.id()).orElseThrow().equals(royal), "royal instance unchanged");
                        ctx.assertTrue(level.getBlockState(placement.primaryLeft()).is(Blocks.CHEST)
                                && level.getBlockState(placement.primaryRight()).is(Blocks.CHEST), "PRIMARY pair intact");
                        ctx.assertTrue(TombGuardianEncounters.onRoyalChestInteraction(level, anchor, royal.spawn(), null)
                                == TombGuardianEncounters.StartResult.ALREADY_DEFEATED, "no second guardian");
                        ctx.runAfterDelay(20, () -> {
                            ctx.assertTrue(inst(level, anchor).state() == EntranceState.ENTRANCE_OPEN, "OPEN is terminal");
                            ctx.assertTrue(golems(level, anchor).size() <= 1, "never more than one golem");
                            golems(level, anchor).forEach(net.minecraft.world.entity.Entity::discard);
                            SecretEntranceRuntime.discardInstance(level, anchor);
                            RoyalChestRuntime.discardInstance(level, royal.id());
                            clear(level, box);
                            forceChunks(level, box, false);
                            ctx.succeed();
                        });
                    });
                });
            });
        });
    }

    @GameTest(maxTicks = 1200)
    public void guardianDefeatGolemGuideOpensPassageSouthEast(GameTestHelper ctx) { fullFlow(ctx, Orientation.SOUTH, AccessRoute.EAST, 0); }

    @GameTest(maxTicks = 1200)
    public void guardianDefeatGolemGuideOpensPassageWestWest(GameTestHelper ctx) { fullFlow(ctx, Orientation.WEST, AccessRoute.WEST, 1); }

    // ---- 3. fallback when the golem cannot help ----

    @GameTest(maxTicks = 900)
    public void golemFailureFallsBackAfterTheApproved600Ticks(GameTestHelper ctx) {
        EntranceConfig.resetToProduction();
        ServerLevel level = ctx.getLevel();
        Orientation o = Orientation.NORTH;
        BlockPos ground = new BlockPos(48_000, GROUND_Y, 48_000 + RUN_Z);
        BoundingBox box = St01PyramidAssembly.completionBox(ground, o);
        forceChunks(level, box, true);
        placeHall(ctx, level, ground, o);
        ctx.assertTrue(SecretEntranceRuntime.register(level, ground, o), "registered");
        BlockPos anchor = EntranceLayout.tombAnchor(ground, o);
        SecretEntranceRuntime.TestHooks.failGolemSpawnFor.add(anchor.asLong());
        List<BlockPos> cover = EntranceLayout.coverCells(ground, o);
        recordSyntheticDefeat(level, anchor);
        long[] start = {level.getGameTime()};
        await(ctx, 5, "defeat recorded", () -> inst(level, anchor).state() == EntranceState.GUARDIAN_DEFEATED, () -> {
            start[0] = level.getGameTime();
            ctx.runAfterDelay(550, () -> {
                ctx.assertTrue(inst(level, anchor).state() == EntranceState.GUARDIAN_DEFEATED && golems(level, anchor).isEmpty(), "still waiting, no golem");
                ctx.assertTrue(!level.getBlockState(cover.get(0)).isAir(), "closed before the fallback time");
                await(ctx, 80, "fallback open", () -> inst(level, anchor).state() == EntranceState.FALLBACK_OPEN, () -> {
                    long elapsed = level.getGameTime() - start[0];
                    ctx.assertTrue(elapsed >= 595 && elapsed <= 605, "opens at the approved 600 ticks, elapsed " + elapsed);
                    for (BlockPos c : cover) ctx.assertTrue(level.getBlockState(c).isAir(), "cover cell open " + c);
                    // A block placed into the opening is cleared again: the open state is permanent.
                    level.setBlock(cover.get(4), Blocks.SANDSTONE.defaultBlockState(), Block.UPDATE_ALL);
                    ctx.runAfterDelay(3, () -> {
                        ctx.assertTrue(level.getBlockState(cover.get(4)).isAir(), "OPEN is permanent");
                        SecretEntranceRuntime.TestHooks.failGolemSpawnFor.remove(anchor.asLong());
                        SecretEntranceRuntime.discardInstance(level, anchor);
                        clear(level, box);
                        forceChunks(level, box, false);
                        ctx.succeed();
                    });
                });
            });
        });
    }
}
