package dev.itzgamesgd.moreminecrafts.st01;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import dev.itzgamesgd.moreminecrafts.st01.P10Support.Sched;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * ST-01 P10 cover-opening safety and negative controls on the real hall geometry. Occupants are PROXIES: armor stands (deterministic drop),
 * AI villagers (real fall damage), the gold guide golem and a mock server player stepped with doTick (no client, no real input). None of this is a
 * real-player test.
 */
public final class RoyalTombSafetyGameTests {
    static { EntranceConfig.install(EntranceConfig.testOnly(600L, 1.5D)); }

    private record Occ(String kind, Entity e, double y0, float hp0, BlockPos cell) {}

    private static void ready(Sched s, RoyalTombFixture.Placed p, Runnable body) {
        AtomicBoolean started = new AtomicBoolean();
        s.each(() -> {
            if (started.get()) return;
            for (var c : p.chunks()) if (!p.level.areEntitiesLoaded(c.pack())) return;
            started.set(true); body.run();
        });
    }

    private void coverSafety(GameTestHelper ctx, String side) {
        Sched s = new Sched(ctx);
        RoyalTombFixture f = RoyalTombFixture.load(side);
        RoyalTombFixture.Placed p = f.place(ctx.getLevel(), P10Support.nextOrigin(ctx));
        p.build();
        ready(s, p, () -> {
            ServerLevel level = p.level;
            SecretEntranceRuntime.register(level, p.anchor, f.orientation);
            SecretEntranceData.Instance inst = SecretEntranceRuntime.data(level).get(p.anchor.asLong());
            McPorts ports = new McPorts(level, inst, EntranceConfig.current());
            List<BlockPos> cover = EntranceLayout.coverCells(p.anchor, f.orientation);
            List<Occ> occ = new ArrayList<>();
            int i = 0;
            for (BlockPos c : cover) {
                ArmorStand st = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.EVENT);
                st.snapTo(c.getX() + 0.5D, c.getY() + 1.0D, c.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(st);
                occ.add(new Occ("armor_stand", st, st.getY(), 0.0F, c));
                if (i % 3 == 0) {          // four AI villagers: real fall damage (they may wander off the cell once the AI runs; recorded)
                    Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.EVENT);
                    v.snapTo(c.getX() + 0.5D, c.getY() + 1.0D, c.getZ() + 0.5D, 0.0F, 0.0F);
                    level.addFreshEntity(v);
                    occ.add(new Occ("villager", v, v.getY(), v.getHealth(), c));
                }
                i++;
            }
            BlockPos pc = cover.get(5);
            ServerPlayer mock = ctx.makeMockServerPlayerInLevel();
            mock.teleportTo(level, pc.getX() + 0.5D, pc.getY() + 1.0D, pc.getZ() + 0.5D, java.util.Set.of(), 0.0F, 0.0F, false);
            occ.add(new Occ("mock_player", mock, mock.getY(), mock.getHealth(), pc));
            BlockPos dc = cover.stream().max(java.util.Comparator.comparingInt(c -> Math.abs(c.getX() - p.anchor.getX()))).get();   // the deepest column (4-block drop)
            ServerPlayer mockDeep = ctx.makeMockServerPlayerInLevel();
            mockDeep.teleportTo(level, dc.getX() + 0.5D, dc.getY() + 1.0D, dc.getZ() + 0.5D, java.util.Set.of(), 0.0F, 0.0F, false);
            occ.add(new Occ("mock_player_deepest_column", mockDeep, mockDeep.getY(), mockDeep.getHealth(), dc));
            BlockPos gc = cover.get(0);
            GoldGuideGolem gg = SecretEntranceEntities.GOLD_GUIDE_GOLEM.create(level, EntitySpawnReason.EVENT);
            gg.snapTo(gc.getX() + 0.5D, gc.getY() + 1.0D, gc.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(gg);
            occ.add(new Occ("gold_golem_on_cover", gg, gg.getY(), gg.getHealth(), gc));
            GoldGuideGolem near = SecretEntranceEntities.GOLD_GUIDE_GOLEM.create(level, EntitySpawnReason.EVENT);
            BlockPos nc = EntranceLayout.stopPoint(p.anchor, f.orientation);
            near.snapTo(nc.getX() + 0.5D, nc.getY(), nc.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(near);
            occ.add(new Occ("gold_golem_at_stop", near, near.getY(), near.getHealth(), nc));
            boolean[] playerTicked = {true};
            s.each(() -> {
                if (s.tick > 2) for (ServerPlayer mp : new ServerPlayer[]{mock, mockDeep}) if (!mp.isRemoved()) { try { mp.doTick(); } catch (Throwable t) { playerTicked[0] = false; } }
            });
            s.after(3, () -> {
                for (Occ o : occ) if (!o.kind().equals("gold_golem_at_stop")) ctx.assertTrue(Math.abs(o.e().getY() - o.y0()) < 0.1D, side + " " + o.kind() + " stands on the closed cover, y " + o.e().getY() + " vs " + o.y0());
                ports.openCover();
                ctx.assertTrue(ports.coverOpen(), "all 12 cover blocks removed");
                s.after(60, () -> {
                    List<String> rows = new ArrayList<>();
                    int worst = 0; double maxDrop = 0.0D; int buried = 0, dead = 0;
                    for (Occ o : occ) {
                        Entity e = o.e();
                        double drop = o.y0() - e.getY();
                        boolean inWall = e.isInWall();
                        double hp = e instanceof LivingEntity le ? le.getHealth() : Double.NaN;
                        boolean stayed = o.kind().equals("gold_golem_at_stop") || (Math.abs(e.getX() - (o.cell().getX() + 0.5D)) < 0.45D && Math.abs(e.getZ() - (o.cell().getZ() + 0.5D)) < 0.45D);
                        if (stayed && !o.kind().equals("gold_golem_at_stop")) maxDrop = Math.max(maxDrop, drop);
                        if (inWall) buried++;
                        if (!e.isAlive()) dead++;
                        rows.add(String.join(",", side, o.kind(), Integer.toString(o.cell().getX() - p.anchor.getX()), Integer.toString(o.cell().getZ() - p.anchor.getZ()),
                                String.format(Locale.ROOT, "%.3f", drop), Boolean.toString(stayed), Boolean.toString(inWall), String.format(Locale.ROOT, "%.1f", o.hp0()), String.format(Locale.ROOT, "%.1f", hp), Boolean.toString(e.isAlive())));
                        if (o.kind().equals("armor_stand")) ctx.assertTrue(drop >= 0.0D && drop <= 4.01D, side + " armor stand dropped " + drop + " (max 4)");
                    }
                    System.out.println("P10_COVER_SAFETY " + side + " maxDropOfOccupantsThatStayedOnTheirCell=" + maxDrop + " buried=" + buried + " dead=" + dead + " playerProxyTicked=" + playerTicked[0]);
                    for (String r : rows) System.out.println("P10_COVER_ROW " + r);
                    P10Support.csv("cover_safety.csv", "side,occupant,dx_from_anchor,dz_from_anchor,drop,stayed_on_cell,in_wall,hp_before,hp_after,alive", rows);
                    ctx.assertTrue(buried == 0, side + ": nobody is buried (" + buried + ")");
                    ctx.assertTrue(maxDrop <= 4.01D, side + ": no occupant fell more than the 4 blocks of the model");
                    ctx.assertTrue(occ.stream().filter(o -> o.kind().equals("armor_stand")).allMatch(o -> o.e().isAlive()), "armor stands survive");
                    occ.forEach(o -> { if (!(o.e() instanceof ServerPlayer)) o.e().discard(); }); mock.discard(); mockDeep.discard();
                    // the CLOSED->OPEN diff is exactly the 12 cover cells
                    ctx.assertTrue(p.voxelDiff()[0] == 12, "only the 12 cover cells differ from the CLOSED model, got " + p.voxelDiff()[0]);
                    p.release(); ctx.succeed();
                });
            });
        });
    }

    @GameTest(maxTicks = 600, environment = "moreminecrafts-gametest:p10_flows") public void coverOpensSafelyUnderOccupantsEast(GameTestHelper ctx) { coverSafety(ctx, "E"); }
    @GameTest(maxTicks = 600, environment = "moreminecrafts-gametest:p10_flows") public void coverOpensSafelyUnderOccupantsWest(GameTestHelper ctx) { coverSafety(ctx, "W"); }

    // ------------------------------------------------------------------ after opening: can things walk through the hatch?

    private void hatchWalk(GameTestHelper ctx, String side) {
        Sched s = new Sched(ctx);
        RoyalTombFixture f = RoyalTombFixture.load(side);
        RoyalTombFixture.Placed p = f.place(ctx.getLevel(), P10Support.nextOrigin(ctx));
        p.build();
        ready(s, p, () -> {
            ServerLevel level = p.level;
            for (BlockPos c : EntranceLayout.coverCells(p.anchor, f.orientation)) level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
            double[] scales = {0.5D, 0.75D, 1.0D};
            String[] goalNames = {"HATCH", "VAULT_ENTRY"};
            List<GoldGuideGolem> gs = new ArrayList<>(); List<String> labels = new ArrayList<>(); List<BlockPos> goals = new ArrayList<>(); int[] reached = new int[scales.length * goalNames.length];
            java.util.Arrays.fill(reached, -1);
            var team = level.getScoreboard().addPlayerTeam("p10_hatch_" + side);
            team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.NEVER);
            BlockPos stop = EntranceLayout.stopPoint(p.anchor, f.orientation);
            for (double sc : scales) for (String gn : goalNames) {
                GoldGuideGolem g = SecretEntranceEntities.GOLD_GUIDE_GOLEM.create(level, EntitySpawnReason.EVENT);
                g.getAttribute(Attributes.SCALE).setBaseValue(sc); g.refreshDimensions();
                g.snapTo(stop.getX() + 0.5D, stop.getY(), stop.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(g); level.getScoreboard().addPlayerToTeam(g.getScoreboardName(), team);
                int[] gl = f.goals.get(gn);
                gs.add(g); labels.add("scale" + sc + "->" + gn); goals.add(p.world(gl[0], gl[1], gl[2]));
            }
            s.each(() -> {
                if (s.tick % 10 == 1) for (int k = 0; k < gs.size(); k++) if (reached[k] < 0) { BlockPos g = goals.get(k); gs.get(k).getNavigation().moveTo(g.getX() + 0.5D, g.getY(), g.getZ() + 0.5D, 1.0D); }
                for (int k = 0; k < gs.size(); k++) if (reached[k] < 0) { BlockPos g = goals.get(k); var e = gs.get(k); if (Math.abs(e.getX() - g.getX() - 0.5D) < 1.6D && Math.abs(e.getZ() - g.getZ() - 0.5D) < 1.6D && Math.abs(e.getY() - g.getY()) < 1.6D) reached[k] = s.tick; }
            });
            s.after(1200, () -> {
                List<String> rows = new ArrayList<>();
                for (int k = 0; k < gs.size(); k++) {
                    rows.add(side + "," + labels.get(k) + "," + reached[k]);
                    System.out.println("P10_HATCH_WALK " + side + " " + labels.get(k) + " reachedTick=" + reached[k] + " finalPos=" + gs.get(k).position());
                }
                P10Support.csv("hatch_walk.csv", "side,golem_goal,reached_tick", rows);
                for (int k = 0; k < gs.size(); k++) if (labels.get(k).startsWith("scale0.5")) ctx.assertTrue(reached[k] >= 0, side + " " + labels.get(k) + " reachable after the cover is open (scale 0.5)");
                gs.forEach(Entity::discard); level.getScoreboard().removePlayerTeam(team); p.release(); ctx.succeed();
            });
        });
    }

    @GameTest(maxTicks = 1400, environment = "moreminecrafts-gametest:p10_flows") public void openedHatchIsWalkableEast(GameTestHelper ctx) { hatchWalk(ctx, "E"); }
    @GameTest(maxTicks = 1400, environment = "moreminecrafts-gametest:p10_flows") public void openedHatchIsWalkableWest(GameTestHelper ctx) { hatchWalk(ctx, "W"); }

    // ------------------------------------------------------------------ negative controls (the checks must be able to fail)

    @GameTest(maxTicks = 300, environment = "moreminecrafts-gametest:p10_flows")
    public void negativeControlsMutatedFixturesAreDetected(GameTestHelper ctx) {
        RoyalTombFixture e = RoyalTombFixture.load("E"), w = RoyalTombFixture.load("W");
        RoyalTombFixture.Placed pe = e.place(ctx.getLevel(), P10Support.nextOrigin(ctx)), pw = w.place(ctx.getLevel(), P10Support.nextOrigin(ctx));
        pe.build(); pw.build();
        ctx.assertTrue(pe.voxelDiff()[0] == 0, "control: unmodified fixture matches");
        // N1: one wall block removed (a hole in the hall shell) -> the voxel comparison must report exactly that cell
        BlockPos wall = pe.world(e.hallCx + e.hallHalf + 1, e.hallFloor + 2, e.hallCz);
        ctx.assertTrue(!pe.level.getBlockState(wall).isAir(), "N1 precondition: that cell is wall");
        pe.level.setBlock(wall, Blocks.AIR.defaultBlockState(), 2);
        ctx.assertTrue(pe.voxelDiff()[0] == 1, "N1: a single removed wall block is detected, got " + pe.voxelDiff()[0]);
        // N2: one floor block added where the model has air (an extra obstacle)
        BlockPos air = pe.world(e.hallCx + 3, e.hallFloor, e.hallCz + 7);
        ctx.assertTrue(pe.level.getBlockState(air).isAir(), "N2 precondition: free hall cell");
        pe.level.setBlock(air, Blocks.STONE.defaultBlockState(), 2);
        ctx.assertTrue(pe.voxelDiff()[0] == 2, "N2: an added obstacle is detected, got " + pe.voxelDiff()[0]);
        // N3: the WEST layout (sign -1) applied to the EAST fixture must NOT reproduce the model cover (orientation errors are caught)
        java.util.Set<BlockPos> model = new java.util.HashSet<>(); for (int[] c : e.coverModel) model.add(pe.world(c[0], c[1], c[2]));
        ctx.assertTrue(!new java.util.HashSet<>(EntranceLayout.coverCells(pe.anchor, EntranceLayout.Orientation.WEST)).equals(model), "N3: wrong orientation yields different cover cells");
        // N4: EAST data placed with the WEST anchor convention differs from the WEST model
        int diff = 0;
        for (int x = e.x0; x <= e.x1; x++) for (int y = e.y0; y <= e.y1; y++) for (int z = e.z0; z <= e.z1; z++) if (e.modelSolid(x, y, z) != w.modelSolid(x, y, z)) diff++;
        ctx.assertTrue(diff > 1000, "N4: un-mirrored EAST and WEST models are clearly different (" + diff + " cells)");
        System.out.println("P10_NEGATIVE N1/N2/N3/N4 all detected; east-vs-west unmirrored cell diff=" + diff);
        pe.release(); pw.release(); ctx.succeed();
    }

    /** N5: a wall across the hall blocks the route; the harness must report the golem as NOT arrived and the entrance must fall back (never open early). */
    @GameTest(maxTicks = 3000, environment = "moreminecrafts-gametest:p10_flows")
    public void negativeControlBlockedRouteNeverReportsArrival(GameTestHelper ctx) {
        Sched s = new Sched(ctx);
        RoyalTombFixture f = RoyalTombFixture.load("E");
        RoyalTombFixture.Placed p = f.place(ctx.getLevel(), P10Support.nextOrigin(ctx));
        p.build();
        // seal the whole hall width at hall-local v = +8 (between the BOSS_S spawn cell at v=+6 and the stop point at v=+15): 4 high wall from the west wall to the east wall
        for (int x = f.hallCx - f.hallHalf; x <= f.hallCx + f.hallHalf; x++) for (int y = f.hallFloor; y <= f.hallFloor + 3; y++) p.level.setBlock(p.world(x, y, f.hallCz + 8), Blocks.STONE.defaultBlockState(), 2);
        RoyalTombFlowGameTests.Flow[] fl = new RoyalTombFlowGameTests.Flow[1];
        fl[0] = new RoyalTombFlowGameTests.Flow(ctx, s, p, "blocked", 0, "wall across the hall between the golem and the stop point", x -> {
            ctx.assertTrue(x.finalState.equals("FALLBACK_OPEN"), "blocked route ends in the fallback opening, got " + x.finalState);
            ctx.assertTrue(x.openTick >= 600, "never opened before the fallback deadline, open tick " + x.openTick);
            ctx.assertTrue(x.arrivedUndetected < 0, "the golem never reached the stop point");
            System.out.println("P10_NEGATIVE blocked route: state=" + x.finalState + " openTick=" + x.openTick);
            ctx.succeed();
        });
        fl[0].fols.add(RoyalTombFlowGameTests.follower(-2.0D, 0.0D, 0.3D, 0, true));
    }

    // ------------------------------------------------------------------ O-9 support: which damage sources count as "a player's" under the victory rule

    /** Measurement only (no production change): how TombVictoryPolicy classifies and attributes common damage sources. */
    @GameTest(maxTicks = 100)
    public void victoryRuleDamageSourceAttribution(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer pl = ctx.makeMockServerPlayerInLevel();
        var ds = level.damageSources();
        net.minecraft.world.entity.projectile.arrow.Arrow arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(level, pl.getX(), pl.getY(), pl.getZ(), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
        arrow.setOwner(pl);
        net.minecraft.world.entity.monster.zombie.Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
        java.util.LinkedHashMap<String, net.minecraft.world.damagesource.DamageSource> src = new java.util.LinkedHashMap<>();
        src.put("player melee (playerAttack)", ds.playerAttack(pl));
        src.put("player arrow (arrow, owner = player)", ds.arrow(arrow, pl));
        src.put("player mace", ds.mace(pl));
        src.put("player-caused explosion (explosion(null, player))", ds.explosion(null, pl));
        src.put("thorns reflected onto the boss (thorns(player))", ds.thorns(pl));
        src.put("hostile mob melee (mobAttack(zombie))", ds.mobAttack(zombie));
        src.put("unowned explosion (explosion(null, null))", ds.explosion(null, null));
        src.put("burning (onFire) - includes Fire Aspect / flame DOT after the hit", ds.onFire());
        src.put("standing in fire (inFire)", ds.inFire());
        src.put("lava", ds.lava());
        src.put("magic (poison / instant damage potions)", ds.magic());
        src.put("wither effect", ds.wither());
        src.put("lightning bolt (no cause)", ds.lightningBolt());
        src.put("fall", ds.fall());
        src.put("cactus", ds.cactus());
        src.put("freeze", ds.freeze());
        src.put("drown", ds.drown());
        src.put("out of world (void)", ds.fellOutOfWorld());
        src.put("generic kill (/kill)", ds.genericKill());
        List<String> rows = new ArrayList<>();
        for (var en : src.entrySet()) {
            var cause = dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.classify(en.getValue());
            boolean attributed = dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.attributedPlayer(en.getValue()) != null;
            rows.add("\"" + en.getKey() + "\"," + cause + "," + attributed);
            System.out.println("P10_DAMAGE_SOURCE " + en.getKey() + " -> " + cause + " attributedToPlayer=" + attributed);
        }
        P10Support.csv("damage_source_attribution.csv", "damage_source,death_cause_class,attributed_to_player", rows);
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.classify(ds.playerAttack(pl)) == dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.DeathCause.PLAYER, "melee is a player's");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.classify(ds.onFire()) == dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.DeathCause.ENVIRONMENT, "burning is environmental");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.classify(ds.genericKill()) == dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.DeathCause.COMMAND_KILL, "/kill is a command kill");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.classify(ds.mobAttack(zombie)) == dev.itzgamesgd.moreminecrafts.boss.TombVictoryPolicy.DeathCause.MOB, "a mob is a mob");
        pl.discard(); zombie.discard();
        ctx.succeed();
    }

    // ------------------------------------------------------------------ real process restart on the real hall (three separate server processes)

    private static final BlockPos RESTART_ORIGIN = new BlockPos(32768, 150, 32768);

    /**
     * -Dmoreminecrafts.st01.p10.restart.phase=write|reload|verify and -Dmoreminecrafts.st01.p10.restart.mode=guiding|open (inert otherwise).
     * guiding: write stops the golem mid-guidance and saves; reload must restore GOLEM_GUIDING/FALLBACK_OPEN and open by the timer; verify: OPEN survives.
     * open: write runs the entrance to ENTRANCE_OPEN (follower simulated) and saves; reload/verify: OPEN, cover open, no golem, voxel diff = 12 cells.
     */
    @GameTest(maxTicks = 2000, environment = "moreminecrafts-gametest:p10_isolated")
    public void entranceStateSurvivesProcessRestartOnTheRealHall(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.st01.p10.restart.phase");
        String mode = System.getProperty("moreminecrafts.st01.p10.restart.mode", "guiding");
        if (phase == null || phase.isBlank()) { ctx.succeed(); return; }
        Sched s = new Sched(ctx);
        RoyalTombFixture f = RoyalTombFixture.load("E");
        BlockPos origin = RESTART_ORIGIN.offset(mode.equals("open") ? 256 : 0, 0, 0);
        RoyalTombFixture.Placed p = f.place(ctx.getLevel(), origin);
        boolean write = phase.equals("write");
        if (write) p.build(); else for (var c : p.chunks()) { p.level.getChunk(c.x(), c.z()); p.level.setChunkForced(c.x(), c.z(), true); }
        ready(s, p, () -> {
            ServerLevel level = p.level;
            switch (phase) {
                case "write" -> {
                    SecretEntranceRuntime.register(level, p.anchor, f.orientation);
                    RoyalTombFlowGameTests.Flow[] holder = new RoyalTombFlowGameTests.Flow[1];
                    holder[0] = new RoyalTombFlowGameTests.Flow(ctx, s, p, "restart-" + mode, 0, "", x -> {});
                    // drive the pipeline by hand: the Flow helper registers/starts itself, so only use it for the follower; stop it here when the state is reached
                    holder[0].fols.add(RoyalTombFlowGameTests.follower(-2.0D, 0.0D, 0.3D, 0, true));
                    s.each(() -> {
                        var in = SecretEntranceRuntime.data(level).get(p.anchor.asLong());
                        if (in == null) return;
                        if (mode.equals("guiding") && in.state() == EntranceState.GOLEM_GUIDING && holder[0].golemSeenTick >= 0) {
                            holder[0].golems().forEach(Entity::discard);
                            System.out.println("P10_RESTART_WRITE guiding remaining=" + in.remaining());
                            level.getServer().saveEverything(true, true, true); ctx.succeed();
                        } else if (mode.equals("open") && in.state() == EntranceState.ENTRANCE_OPEN) {
                            System.out.println("P10_RESTART_WRITE open voxelDiff=" + p.voxelDiff()[0]);
                            level.getServer().saveEverything(true, true, true); ctx.succeed();
                        }
                    });
                }
                case "reload" -> {
                    var in = SecretEntranceRuntime.data(level).get(p.anchor.asLong());
                    ctx.assertTrue(in != null && TombGuardianEncounters.data(level).isVictory(p.anchor.asLong()), "entrance record and valid victory survived the restart");
                    if (mode.equals("open")) {
                        ctx.assertTrue(in.state() == EntranceState.ENTRANCE_OPEN, "OPEN survived, got " + in.state());
                        s.after(40, () -> {
                            ctx.assertTrue(in.state().isOpen() && SecretEntranceRuntime.data(level).get(p.anchor.asLong()).state() == EntranceState.ENTRANCE_OPEN, "still ENTRANCE_OPEN");
                            ctx.assertTrue(p.voxelDiff()[0] == 12, "cover still open: only the 12 cover cells differ, got " + p.voxelDiff()[0]);
                            ctx.assertTrue(level.getEntitiesOfClass(GoldGuideGolem.class, new AABB(p.anchor).inflate(128.0D)).isEmpty(), "no golem after restart");
                            System.out.println("P10_RESTART_RELOAD open ok");
                            level.getServer().saveEverything(true, true, true); ctx.succeed();
                        });
                    } else {
                        ctx.assertTrue(in.state() == EntranceState.GOLEM_GUIDING || in.state() == EntranceState.FALLBACK_OPEN, "guiding state survived, got " + in.state());
                        s.each(() -> {
                            var cur = SecretEntranceRuntime.data(level).get(p.anchor.asLong());
                            if (cur.state() == EntranceState.FALLBACK_OPEN && p.voxelDiff()[0] == 12) {
                                System.out.println("P10_RESTART_RELOAD guiding->fallback ok");
                                level.getServer().saveEverything(true, true, true); ctx.succeed();
                            }
                        });
                    }
                }
                default -> {
                    var in = SecretEntranceRuntime.data(level).get(p.anchor.asLong());
                    ctx.assertTrue(in != null && in.state().isOpen(), "OPEN survived the second restart, got " + (in == null ? null : in.state()));
                    s.after(40, () -> {
                        ctx.assertTrue(p.voxelDiff()[0] == 12 && level.getEntitiesOfClass(GoldGuideGolem.class, new AABB(p.anchor).inflate(128.0D)).isEmpty(), "cover open, no golem, nothing re-progressed");
                        ctx.assertTrue(TombGuardianEncounters.isDefeated(level, p.anchor) && !TombGuardianEncounters.isRoyalChestSealed(level, p.anchor), "tomb stays completed, chest unsealed");
                        System.out.println("P10_RESTART_VERIFY ok");
                        level.getServer().saveEverything(true, true, true); ctx.succeed();
                    });
                }
            }
        });
    }
}
