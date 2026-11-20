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
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Opt-in real-client P3 check ({@code -Dmoreminecrafts.heaven.p3capture=true}). Places six representative heaven meadow
 * templates (small, medium, large, ruins + its ruin, temple + its temple, Sky Tower island) as a world would assemble
 * them (attached pieces at their jigsaws, jigsaws turned into their final_state), photographs them at noon, then
 * <ol>
 *   <li>breaks a giant-flower stem on the server and reads every cell back from the CLIENT level (sync check);</li>
 *   <li>saves, closes and reopens the world, compares the medium island block by block and breaks a second stem after
 *       the reload (persistence check).</li>
 * </ol>
 * FPS is logged but the CI renderer is software (lavapipe): it is not a GPU measurement.
 */
public final class HeavenP3ClientCapture implements FabricClientGameTest {
    private static final String GATE = "moreminecrafts.heaven.p3capture";

    private record Site(String name, BlockPos at, String attach, BlockPos attachOffset, double cx, double cy, double cz, double dist) {}

    private static final List<Site> SITES = List.of(
            new Site("plain_small_b", new BlockPos(0, 200, 0), null, null, 13.5, 12, 11.5, 30),
            new Site("plain_medium_a", new BlockPos(70, 200, 0), null, null, 22.5, 13, 17.5, 46),
            new Site("large_a", new BlockPos(0, 200, 120), null, null, 48.5, 28, 48.5, 105),
            // ruins: ruins_top jigsaw (16,11,16) up; ruin piece jigsaw (7,0,7) -> piece origin (9,12,9)
            new Site("ruins", new BlockPos(150, 200, 0), "moreminecrafts:sky_islands/ruins_a", new BlockPos(9, 12, 9), 18, 14, 16.5, 40),
            // temple: temple_top jigsaw (40,19,28) up; temple piece jigsaw (22,0,13) -> piece origin (18,20,15)
            new Site("temple", new BlockPos(150, 200, 120), "moreminecrafts:sky_islands/temple", new BlockPos(18, 20, 15), 40.5, 24, 28.5, 85),
            new Site("tower", new BlockPos(300, 200, 0), null, null, 29, 45, 24.5, 105));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean(GATE)) return;
        int[] shot = {0};
        Map<String, Object> report = new LinkedHashMap<>();
        TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
            save = world.getWorldSave();
            TestServerContext server = world.getServer();
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.renderDistance().set(12); });
            server.runOnServer(s -> {
                cmd(s, "gamerule doDaylightCycle false"); cmd(s, "gamerule doWeatherCycle false"); cmd(s, "weather clear"); cmd(s, "time set noon");
                cmd(s, "gamerule doMobSpawning false");
                s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
            });
            for (Site site : SITES) {
                server.runOnServer(s -> report.put("placed_" + site.name(), place(s.overworld(), site)));
                context.waitTicks(20);
            }
            context.waitTicks(40);
            for (Site site : SITES) {
                for (double[] v : new double[][]{{35, 22}, {215, 30}}) {
                    orbit(context, server, site.at(), site.cx(), site.cy(), site.cz(), v[0], v[1], site.dist(), 160);
                    fps(context, report, site.name() + "-" + (int) v[0]);
                    snap(context, shot, site.name() + "-" + (int) v[0]);
                }
            }
            // close-ups on the medium island: giant flowers, trees, ground cover
            Site med = SITES.get(1);
            orbit(context, server, med.at(), 15, 18, 14, 140, 12, 22, 120);
            snap(context, shot, "plain_medium_a-closeup-giants");
            orbit(context, server, med.at(), 22, 15, 18, 300, 35, 14, 100);
            snap(context, shot, "plain_medium_a-closeup-ground");
            // 1) giant flower break: server removes, client must see every cell gone
            BlockPos giantRoot = med.at().offset(10, 14, 13);                // report positions: [10,14,13] (starlotus)
            List<BlockPos> cells = server.computeOnServer(s -> giantCells(s.overworld(), giantRoot));
            orbit(context, server, med.at(), 10, 19, 13, 110, 8, 16, 80);
            snap(context, shot, "giant-before-break");
            server.runOnServer(s -> {
                ServerLevel l = s.overworld();
                BlockPos stem = cells.stream().filter(p -> l.getBlockState(p).is(blk(l, "moreminecrafts:heaven_giant_stem")) && p.getY() > giantRoot.getY())
                        .findFirst().orElseThrow();
                l.destroyBlock(stem, true, s.getPlayerList().getPlayers().getFirst());
            });
            context.waitTicks(40);
            int clientLeft = context.computeOnClient(c -> (int) cells.stream().filter(p -> !c.level.getBlockState(p).isAir()).count());
            int serverLeft = server.computeOnServer(s -> (int) cells.stream().filter(p -> !s.overworld().getBlockState(p).isAir()).count());
            report.put("break_sync", Map.of("cells", cells.size(), "server_left", serverLeft, "client_left", clientLeft,
                    "removal", String.valueOf(HeavenPlants.lastRemoval())));
            snap(context, shot, "giant-after-break");
            // 2) persistence: fingerprint, save, close
            report.put("fingerprint_before", server.computeOnServer(s -> fingerprint(s.overworld(), med)));
            server.runCommand("save-all flush");
            context.waitTicks(20);
        }
        try (TestSingleplayerContext world = save.open()) {
            TestServerContext server = world.getServer();
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            Site med = SITES.get(1);
            server.runOnServer(s -> { s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR); cmd(s, "time set noon"); });
            orbit(context, server, med.at(), med.cx(), med.cy(), med.cz(), 35, 22, med.dist(), 160);
            snap(context, shot, "plain_medium_a-after-reload");
            report.put("fingerprint_after", server.computeOnServer(s -> fingerprint(s.overworld(), med)));
            BlockPos second = med.at().offset(22, 14, 28);                   // report positions: [22,14,28] (lily)
            List<BlockPos> cells = server.computeOnServer(s -> giantCells(s.overworld(), second));
            server.runOnServer(s -> {
                ServerLevel l = s.overworld();
                BlockPos stem = cells.stream().filter(p -> l.getBlockState(p).is(blk(l, "moreminecrafts:heaven_giant_stem")) && p.getY() > second.getY())
                        .findFirst().orElseThrow();
                l.destroyBlock(stem, true, s.getPlayerList().getPlayers().getFirst());
            });
            context.waitTicks(40);
            int clientLeft = context.computeOnClient(c -> (int) cells.stream().filter(p -> !c.level.getBlockState(p).isAir()).count());
            report.put("break_after_reload", Map.of("cells", cells.size(), "client_left", clientLeft, "removal", String.valueOf(HeavenPlants.lastRemoval())));
        }
        System.out.println("HEAVEN_P3_CLIENT " + report);
        System.out.println("HEAVEN_P3_CAPTURE_COMPLETE " + shot[0]);
    }

    private static Block blk(ServerLevel l, String id) {
        return l.registryAccess().lookupOrThrow(Registries.BLOCK).getValue(Identifier.parse(id));
    }

    private static String place(ServerLevel level, Site site) {
        StructureTemplate t = template(level, "moreminecrafts:heaven_p3/meadow/" + site.name());
        load(level, site.at(), t.getSize().getX(), t.getSize().getZ());
        if (!t.placeInWorld(level, site.at(), site.at(), new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS))
            throw new AssertionError("place " + site.name());
        int finals = 0;
        if (site.attach() != null) {
            StructureTemplate piece = template(level, site.attach());
            BlockPos o = site.at().offset(site.attachOffset());
            if (!piece.placeInWorld(level, o, o, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS)) throw new AssertionError("attach " + site.attach());
        }
        // jigsaws -> final_state, as jigsaw assembly does
        var blocks = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        for (BlockPos p : BlockPos.betweenClosed(site.at(), site.at().offset(t.getSize().getX() + 30, t.getSize().getY() + 30, t.getSize().getZ() + 30))) {
            BlockState s = level.getBlockState(p);
            if (!s.is(Blocks.JIGSAW)) continue;
            var be = level.getBlockEntity(p);
            String fs = be instanceof net.minecraft.world.level.block.entity.JigsawBlockEntity j ? j.getFinalState() : "minecraft:air";
            try {
                level.setBlock(p, BlockStateParser.parseForBlock(blocks, fs, false).blockState(), Block.UPDATE_CLIENTS);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            finals++;
        }
        return "ok jigsaws_to_final_state=" + finals;
    }

    private static StructureTemplate template(ServerLevel level, String id) {
        return level.getStructureTemplateManager().get(Identifier.parse(id)).orElseThrow(() -> new AssertionError("missing " + id));
    }

    private static void load(ServerLevel level, BlockPos at, int sx, int sz) {
        for (int cx = (at.getX() - 16) >> 4; cx <= (at.getX() + sx + 16) >> 4; cx++)
            for (int cz = (at.getZ() - 16) >> 4; cz <= (at.getZ() + sz + 16) >> 4; cz++) level.getChunk(cx, cz);
    }

    /** All giant-part cells of the flower rooted at {@code root} (layout match from any stem above the root). */
    private static List<BlockPos> giantCells(ServerLevel level, BlockPos root) {
        for (int dy = 1; dy < 4; dy++) {
            BlockState s = level.getBlockState(root.above(dy));
            if (s.getBlock() instanceof HeavenPartBlock) {
                var inst = HeavenPlants.find(level, root.above(dy), s, true, level::isLoaded);
                if (inst != null) return new ArrayList<>(inst.cells());
            }
        }
        throw new AssertionError("no giant flower at " + root);
    }

    private static String fingerprint(ServerLevel level, Site site) {
        StructureTemplate t = template(level, "moreminecrafts:heaven_p3/meadow/" + site.name());
        long h = 1125899906842597L; int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(site.at(), site.at().offset(t.getSize().getX() - 1, t.getSize().getY() - 1, t.getSize().getZ() - 1))) {
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            h = 31 * h + p.hashCode() * 17L + s.hashCode(); n++;
        }
        return n + ":" + Long.toHexString(h);
    }

    private static void cmd(MinecraftServer s, String c) {
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c);
    }

    private static void orbit(ClientGameTestContext context, TestServerContext server, BlockPos at, double tx, double ty, double tz,
                              double yawDeg, double pitchDeg, double dist, int wait) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double px = tx + dist * Math.cos(pitch) * Math.sin(yaw), py = ty + dist * Math.sin(pitch), pz = tz + dist * Math.cos(pitch) * Math.cos(yaw);
        double dx = tx - px, dz = tz - pz;
        float fy = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float fp = (float) -Math.toDegrees(Math.atan2(ty - py, Math.hypot(dx, dz)));
        double ax = at.getX() + px, ay = at.getY() + py, az = at.getZ() + pz;
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), ax, ay, az, Set.of(), fy, fp, true));
        BlockPos target = BlockPos.containing(at.getX() + tx, at.getY() + ty, at.getZ() + tz);
        context.waitFor(c -> c.level != null && c.level.hasChunkAt(target), 600);
        context.waitTicks(wait);
    }

    private static void fps(ClientGameTestContext context, Map<String, Object> report, String view) {
        context.runOnClient(c -> report.put("fps_" + view, c.getFps()));
    }

    private static void snap(ClientGameTestContext context, int[] shot, String name) {
        context.runOnClient(c -> c.gui.toastManager().clear());
        System.out.println("HEAVEN_P3_CAPTURE_SCREENSHOT " + context.takeScreenshot("heaven-p3-%02d-%s".formatted(shot[0]++, name)));
    }
}
