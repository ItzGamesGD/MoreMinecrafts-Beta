package dev.itzgamesgd.moreminecrafts.heaven;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Opt-in real-client P3.8 check ({@code -Dmoreminecrafts.heaven.p38capture=true}), all through the isolated option-D test pools:
 * <ol>
 *   <li>six heaven islands (forest + meadow: medium with satellites, large with Sky Tower, temple, ruins) assembled chunk by chunk like
 *       world generation, photographed at day and night, saved, reopened and compared block by block;</li>
 *   <li>partial generation across a world reload: a pre-D (vanilla) large island is generated in HALF of its chunks, the world is saved and
 *       reopened, and the saved structure start finishes it. D: the saved start keeps its vanilla piece locations, so the result is
 *       identical to the uninterrupted vanilla island. Contrast A (templates overwritten in place, same location, heaven content): the same
 *       half-and-half generation produces a seam, reported as the number of blocks that differ from both pure islands.</li>
 * </ol>
 * Software renderer: no GPU measurement.
 */
public final class HeavenP38ClientCapture implements FabricClientGameTest {
    private static final String NS = "moreminecrafts-gametest";
    private record Site(String theme, String name, BlockPos at) {}
    private static final List<Site> SITES = List.of(
            new Site("forest", "plain_medium_d", new BlockPos(0, 150, 0)), new Site("forest", "large_d", new BlockPos(260, 150, 0)),
            new Site("forest", "temple", new BlockPos(560, 150, 0)), new Site("forest", "ruins", new BlockPos(0, 150, 300)),
            new Site("meadow", "plain_medium_d", new BlockPos(260, 150, 300)), new Site("meadow", "large_d", new BlockPos(560, 150, 300)));
    private static final BlockPos PARTIAL = new BlockPos(0, 150, 700);

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.heaven.p38capture")) return;
        int[] shot = {0};
        Map<String, Object> report = new LinkedHashMap<>();
        List<StructureStart> starts = new ArrayList<>(); List<String> before = new ArrayList<>(); List<Map<String, Integer>> beforeCounts = new ArrayList<>();
        CompoundTag[] tags = new CompoundTag[2];                    // 0 = pre-D saved start, 1 = the same start with the piece location rewritten to the heaven template (A: overwrite in place)
        int[][] refs = new int[2][]; BoundingBox[] pbox = new BoundingBox[1]; int[] mid = {0}; long[] partialHash = {0};
        TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
            save = world.getWorldSave(); TestServerContext server = world.getServer();
            setup(context, server);
            // ---- 1) six heaven islands, chunk by chunk like world generation
            server.runOnServer(s -> { for (Site site : SITES) starts.add(HeavenP38GameTests.assemble(s.overworld(), site.theme(), site.name(), site.at()).start()); });
            context.waitTicks(60);
            for (int i = 0; i < SITES.size(); i++) { orbit(context, server, starts.get(i).getBoundingBox(), 200); snap(context, shot, SITES.get(i).theme() + "-" + SITES.get(i).name() + "-day"); }
            for (int i : new int[]{1, 5}) {
                server.runOnServer(s -> cmd(s, "time set midnight"));
                orbit(context, server, starts.get(i).getBoundingBox(), 160); snap(context, shot, SITES.get(i).theme() + "-" + SITES.get(i).name() + "-night");
                server.runOnServer(s -> cmd(s, "time set noon"));
            }
            for (int i = 0; i < SITES.size(); i++) { BoundingBox b = starts.get(i).getBoundingBox(); before.add(server.computeOnServer(s -> HeavenP38GameTests.scan(s.overworld(), b).hash() + "")); beforeCounts.add(server.computeOnServer(s -> HeavenP38GameTests.scan(s.overworld(), b).counts())); }
            // ---- 2) partial generation of a pre-D island
            server.runOnServer(s -> {
                ServerLevel l = s.overworld();
                StructureStart old = HeavenP38GameTests.makeStart(l, NS + ":heaven_d/meadow/old/large_b", "meadow", PARTIAL);
                pbox[0] = old.getBoundingBox(); mid[0] = (pbox[0].minX() >> 4) + ((pbox[0].maxX() >> 4) - (pbox[0].minX() >> 4)) / 2;
                var sctx = StructurePieceSerializationContext.fromLevel(l);
                tags[0] = old.createTag(sctx, old.getChunkPos());
                try {
                    tags[1] = TagParser.parseCompoundFully(tags[0].toString().replace("sky_islands/meadow/large_b", "sky_islands/heaven/meadow/large_b"));
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) { throw new AssertionError(e); }
                HeavenP38GameTests.placeChunkwise(l, old); refs[0] = snapshot(l, pbox[0]); HeavenP38GameTests.clear(l, pbox[0]);
                StructureStart heaven = StructureStart.loadStaticStart(sctx, tags[1], l.getSeed());
                HeavenP38GameTests.placeChunkwise(l, heaven); refs[1] = snapshot(l, pbox[0]); HeavenP38GameTests.clear(l, pbox[0]);
                HeavenP38GameTests.placeChunks(l, old, c -> c.x() <= mid[0]);                   // generated so far: the west half
            });
            context.waitTicks(60);                                                          // vanilla's own post-placement ticks settle (covered grass -> dirt)
            partialHash[0] = server.computeOnServer(s -> HeavenP38GameTests.scan(s.overworld(), pbox[0]).hash());
            look(context, server, new BlockPos(PARTIAL.getX() + 40, 230, PARTIAL.getZ() - 60), new BlockPos(PARTIAL.getX() + 50, 170, PARTIAL.getZ() + 50), 200);
            snap(context, shot, "partial-before-save");
            server.runCommand("save-all flush");
            context.waitTicks(20);
        }
        try (TestSingleplayerContext world = save.open()) {
            TestServerContext server = world.getServer();
            setup(context, server);
            List<String> after = new ArrayList<>();
            for (int i = 0; i < SITES.size(); i++) { BoundingBox b = starts.get(i).getBoundingBox(); after.add(server.computeOnServer(s -> HeavenP38GameTests.scan(s.overworld(), b).hash() + "")); }
            report.put("six_islands_identical_after_reload", before.equals(after));
            List<String> changed = new ArrayList<>();
            for (int i = 0; i < SITES.size(); i++) { BoundingBox b = starts.get(i).getBoundingBox(); Map<String, Integer> now = server.computeOnServer(s -> HeavenP38GameTests.scan(s.overworld(), b).counts());
                Map<String, Integer> was = beforeCounts.get(i); Map<String, Integer> d = new java.util.TreeMap<>();
                for (String k : now.keySet()) if (!now.get(k).equals(was.get(k))) d.put(k, now.get(k) - was.getOrDefault(k, 0));
                for (String k : was.keySet()) if (!now.containsKey(k)) d.put(k, -was.get(k));
                if (!d.isEmpty()) changed.add(SITES.get(i).theme() + "/" + SITES.get(i).name() + d); }
            report.put("six_islands_block_count_changes", changed);
            report.put("partial_identical_after_reload", server.computeOnServer(s -> HeavenP38GameTests.scan(s.overworld(), pbox[0]).hash() == partialHash[0]));
            // D: the saved pre-D start finishes the island with its saved (vanilla) piece locations; compared with an uninterrupted island after the same settling time
            server.runOnServer(s -> {
                ServerLevel l = s.overworld(); var sctx = StructurePieceSerializationContext.fromLevel(l);
                HeavenP38GameTests.placeChunks(l, StructureStart.loadStaticStart(sctx, tags[0], l.getSeed()), c -> c.x() > mid[0]);
            });
            context.waitTicks(60);
            int[] resumed = server.computeOnServer(s -> snapshot(s.overworld(), pbox[0]));
            server.runOnServer(s -> {
                ServerLevel l = s.overworld(); var sctx = StructurePieceSerializationContext.fromLevel(l);
                HeavenP38GameTests.clear(l, pbox[0]); HeavenP38GameTests.placeChunkwise(l, StructureStart.loadStaticStart(sctx, tags[0], l.getSeed()));
            });
            context.waitTicks(60);
            int[] control = server.computeOnServer(s -> snapshot(s.overworld(), pbox[0]));
            StringBuilder det = new StringBuilder(); int shown = 0;
            for (int i = 0; i < control.length && shown < 10; i++) if (control[i] != resumed[i]) {
                det.append(" [").append(Block.stateById(control[i]).getBlock().getDescriptionId().replace("block.minecraft.", "")).append("->")
                        .append(Block.stateById(resumed[i]).getBlock().getDescriptionId().replace("block.minecraft.", "")).append("]"); shown++; }
            report.put("D_resume", "differs_from_uninterrupted_vanilla_island=" + diff(control, resumed) + det + " blocks of " + control.length
                    + " (vs the unsettled placement snapshot: " + diff(refs[0], resumed) + ", vanilla post-placement ticks)");
            server.runOnServer(s -> {                                                           // restore the resumed state for the screenshot
                ServerLevel l = s.overworld(); var sctx = StructurePieceSerializationContext.fromLevel(l);
                HeavenP38GameTests.clear(l, pbox[0]); HeavenP38GameTests.placeChunkwise(l, StructureStart.loadStaticStart(sctx, tags[0], l.getSeed()));
            });
            look(context, server, new BlockPos(PARTIAL.getX() + 40, 230, PARTIAL.getZ() - 60), new BlockPos(PARTIAL.getX() + 50, 170, PARTIAL.getZ() + 50), 200);
            snap(context, shot, "partial-D-resumed");
            // A (contrast): the same west half, but the east half generated from templates overwritten in place (same location, heaven content)
            report.put("A_seam", server.computeOnServer(s -> {
                ServerLevel l = s.overworld(); var sctx = StructurePieceSerializationContext.fromLevel(l);
                HeavenP38GameTests.clear(l, pbox[0]);
                HeavenP38GameTests.placeChunks(l, StructureStart.loadStaticStart(sctx, tags[0], l.getSeed()), c -> c.x() <= mid[0]);
                HeavenP38GameTests.placeChunks(l, StructureStart.loadStaticStart(sctx, tags[1], l.getSeed()), c -> c.x() > mid[0]);
                int[] hybrid = snapshot(l, pbox[0]);
                return "differs_from_pure_vanilla=" + diff(refs[0], hybrid) + " differs_from_pure_heaven=" + diff(refs[1], hybrid) + " blocks";
            }));
            look(context, server, new BlockPos(PARTIAL.getX() + 40, 230, PARTIAL.getZ() - 60), new BlockPos(PARTIAL.getX() + 50, 170, PARTIAL.getZ() + 50), 200);
            snap(context, shot, "partial-A-overwrite-seam");
            for (int i = 0; i < SITES.size(); i++) { orbit(context, server, starts.get(i).getBoundingBox(), 120); if (i == 1 || i == 3) snap(context, shot, SITES.get(i).theme() + "-" + SITES.get(i).name() + "-after-reload"); }
        }
        System.out.println("HEAVEN_P38_CLIENT " + report);
        System.out.println("HEAVEN_P38_CAPTURE_COMPLETE " + shot[0]);
    }

    private static int[] snapshot(ServerLevel l, BoundingBox b) {
        int[] out = new int[b.getXSpan() * b.getYSpan() * b.getZSpan()]; int i = 0;
        for (int x = b.minX(); x <= b.maxX(); x++) for (int y = b.minY(); y <= b.maxY(); y++) for (int z = b.minZ(); z <= b.maxZ(); z++)
            out[i++] = Block.getId(l.getBlockState(new BlockPos(x, y, z)));
        return out;
    }

    private static int diff(int[] a, int[] b) { int n = 0; for (int i = 0; i < a.length; i++) if (a[i] != b[i]) n++; return n; }

    private static void orbit(ClientGameTestContext context, TestServerContext server, BoundingBox b, int wait) {
        double cx = (b.minX() + b.maxX()) / 2.0, cy = (b.minY() + b.maxY()) / 2.0, cz = (b.minZ() + b.maxZ()) / 2.0, d = Math.max(b.getXSpan(), b.getZSpan()) * 0.9 + 25;
        look(context, server, new BlockPos((int) (cx - d * 0.45), (int) (cy + d * 0.55), (int) (cz - d * 0.9)), new BlockPos((int) cx, (int) (cy - 4), (int) cz), wait);
    }

    private static void setup(ClientGameTestContext context, TestServerContext server) {
        context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.renderDistance().set(12); });
        server.runOnServer(s -> {
            for (String c : new String[]{"gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear", "time set noon", "gamerule doMobSpawning false", "gamerule randomTickSpeed 0"}) cmd(s, c);
            s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
        });
    }

    private static void cmd(MinecraftServer s, String c) { s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c); }

    private static void look(ClientGameTestContext context, TestServerContext server, BlockPos eye, BlockPos target, int wait) {
        double px = eye.getX() + 0.5, py = eye.getY() + 0.5, pz = eye.getZ() + 0.5;
        double dx = target.getX() + 0.5 - px, dy = target.getY() + 0.5 - py, dz = target.getZ() + 0.5 - pz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), px, py, pz, Set.of(), yaw, pitch, true));
        context.waitFor(c -> c.level != null && c.level.hasChunkAt(target), 600);
        context.waitTicks(wait);
    }

    private static void snap(ClientGameTestContext context, int[] shot, String name) {
        context.runOnClient(c -> c.gui.toastManager().clear());
        System.out.println("HEAVEN_P38_CAPTURE_SCREENSHOT " + context.takeScreenshot("heaven-p38-%02d-%s".formatted(shot[0]++, name)));
    }
}
