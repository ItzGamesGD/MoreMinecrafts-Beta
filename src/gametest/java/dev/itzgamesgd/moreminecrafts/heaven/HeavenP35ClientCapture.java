package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumData;
import dev.itzgamesgd.moreminecrafts.colosseum.ColosseumRuntime;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Opt-in real-client P3.5 check ({@code -Dmoreminecrafts.heaven.p35capture=true}).
 * <ol>
 *   <li>Rotation: the three giant flowers x four structure rotations on one platform, photographed; a rotated stem is
 *       broken on the server and read back from the CLIENT; the world is saved, closed and reopened, the grid compared
 *       block by block and a second rotated flower broken after the reload.</li>
 *   <li>Textures: the representative flower (giant lily) and a P3 island photographed with the production textures and
 *       with the gametest-only packs T1 / T2 / T3 (same cameras, noon; island also at midnight).</li>
 *   <li>Save-error control: a fresh world with the P3 capture's seed and NO heaven placement; the player waits at the
 *       natural ST05 colosseum the P3 run constructed, then the world is saved and reopened. Colosseum state size and
 *       persistence are logged (the UTF error itself goes to the log).</li>
 * </ol>
 * FPS under the CI software renderer is not a GPU measurement.
 */
public final class HeavenP35ClientCapture implements FabricClientGameTest {
    private static final String P3_SEED = "-7815959961086380509";        // client_p3.log [ST05-NATURAL] started seed=...
    private static final BlockPos GRID = new BlockPos(0, 200, 0), LILY = new BlockPos(120, 200, 0), ISLAND = new BlockPos(180, 200, 0);
    private static final String[] SPECIES = {"giant_starlotus", "giant_lily", "giant_daffodil"};
    private static final Map<String, BlockPos> PIVOT = Map.of("giant_starlotus", new BlockPos(3, 1, 3), "giant_lily", new BlockPos(3, 1, 2),
            "giant_daffodil", new BlockPos(4, 1, 4));
    private static final Rotation[] ROT = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean(HeavenP35TexturePacks.GATE)) return;
        int[] shot = {0};
        Map<String, Object> report = new LinkedHashMap<>();
        TestWorldSave save;
        // ------------------------------------------------------------ world A: rotation + textures
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
            save = world.getWorldSave();
            TestServerContext server = world.getServer();
            setup(context, server);
            server.runOnServer(s -> {
                ServerLevel l = s.overworld();
                for (int si = 0; si < 3; si++) for (int r = 0; r < 4; r++) {
                    BlockPos root = root(si, r);
                    for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                        l.setBlock(root.offset(x, -2, z), HeavenPrototypeBlocks.HEAVEN_LOAM.defaultBlockState(), Block.UPDATE_CLIENTS);
                        l.setBlock(root.offset(x, -1, z), HeavenPrototypeBlocks.HEAVEN_GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    placeSpecies(l, SPECIES[si], root, r);
                }
            });
            context.waitTicks(40);
            look(context, server, GRID.offset(22, 22, -34), GRID.offset(22, 6, 22), 200);
            snap(context, shot, "rotation-grid-front");
            look(context, server, GRID.offset(-26, 34, -20), GRID.offset(22, 4, 22), 120);
            snap(context, shot, "rotation-grid-oblique");
            for (int si = 0; si < 3; si++) for (int r = 0; r < 4; r++) {
                BlockPos root = root(si, r);
                double yaw = Math.toRadians(30 + 90 * r);              // camera turns with the flower: every shot shows the same side
                look(context, server, root.offset((int) Math.round(-16 * Math.sin(yaw)), 8, (int) Math.round(-16 * Math.cos(yaw))), root.above(5), 80);
                snap(context, shot, "rotation-" + SPECIES[si] + "-r" + 90 * r);
            }
            // break a rotated flower; the client must see every cell gone
            List<BlockPos> lily90 = server.computeOnServer(s -> cells(s.overworld(), root(1, 1)));
            server.runOnServer(s -> breakStem(s, lily90, root(1, 1)));
            context.waitTicks(40);
            report.put("rotated_break_sync", Map.of("cells", lily90.size(),
                    "client_left", context.computeOnClient(c -> (int) lily90.stream().filter(p -> !c.level.getBlockState(p).isAir()).count()),
                    "removal", String.valueOf(HeavenPlants.lastRemoval())));
            look(context, server, GRID.offset(22, 22, -34), GRID.offset(22, 6, 22), 80);
            snap(context, shot, "rotation-grid-after-break-lily-r90");
            // textures: representative lily + P3 island, production then T1 / T2 / T3
            server.runOnServer(s -> {
                ServerLevel l = s.overworld();
                for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                    l.setBlock(LILY.offset(x, -2, z), HeavenPrototypeBlocks.HEAVEN_LOAM.defaultBlockState(), Block.UPDATE_CLIENTS);
                    l.setBlock(LILY.offset(x, -1, z), HeavenPrototypeBlocks.HEAVEN_GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                placeSpecies(l, "giant_lily", LILY, 0);
                StructureTemplate t = template(l, "moreminecrafts:heaven_p3/meadow/plain_medium_a");
                if (!t.placeInWorld(l, ISLAND, ISLAND, new StructurePlaceSettings(), l.getRandom(), Block.UPDATE_CLIENTS)) throw new AssertionError("island");
            });
            context.waitTicks(40);
            var repo = context.computeOnClient(c -> { c.getResourcePackRepository().reload(); return List.copyOf(c.getResourcePackRepository().getAvailableIds()); });
            List<String> packs = repo.stream().filter(id -> id.contains("heaven_tex_t")).sorted().toList();
            report.put("texture_packs", packs);
            List<String> variants = new ArrayList<>(List.of("P3"));
            variants.addAll(packs);
            for (String v : variants) {
                String tag = v.equals("P3") ? "P3" : "T" + v.charAt(v.length() - 1);
                context.runOnClient(c -> {
                    var r = c.getResourcePackRepository();
                    for (String p : packs) r.removePack(p);
                    if (!v.equals("P3")) r.addPack(v);
                });
                reload(context);
                server.runOnServer(s -> cmd(s, "time set noon"));
                look(context, server, LILY.offset(-9, 9, -17), LILY.offset(0, 6, 1), 80);
                snap(context, shot, "tex-" + tag + "-1-lily-whole");
                look(context, server, LILY.offset(-4, 10, -6), LILY.offset(0, 8, 1), 60);
                snap(context, shot, "tex-" + tag + "-2-petals");
                look(context, server, LILY.offset(4, 3, -5), LILY.offset(0, 2, 1), 60);
                snap(context, shot, "tex-" + tag + "-3-stem-leaves");
                look(context, server, LILY.offset(-1, 14, -2), LILY.offset(0, 10, 1), 60);
                snap(context, shot, "tex-" + tag + "-4-stamens");
                look(context, server, ISLAND.offset(-14, 34, -24), ISLAND.offset(22, 13, 17), 100);
                snap(context, shot, "tex-" + tag + "-5-island");
                server.runOnServer(s -> cmd(s, "time set midnight"));
                context.waitTicks(60);
                snap(context, shot, "tex-" + tag + "-6-island-night");
            }
            context.runOnClient(c -> { for (String p : packs) c.getResourcePackRepository().removePack(p); });
            reload(context);
            report.put("grid_fingerprint_before", server.computeOnServer(s -> fingerprint(s.overworld())));
            server.runCommand("save-all flush");
            context.waitTicks(20);
        }
        try (TestSingleplayerContext world = save.open()) {
            TestServerContext server = world.getServer();
            setup(context, server);
            look(context, server, GRID.offset(22, 22, -34), GRID.offset(22, 6, 22), 160);
            snap(context, shot, "rotation-grid-after-reload");
            report.put("grid_fingerprint_after", server.computeOnServer(s -> fingerprint(s.overworld())));
            List<BlockPos> daf270 = server.computeOnServer(s -> cells(s.overworld(), root(2, 3)));
            server.runOnServer(s -> breakStem(s, daf270, root(2, 3)));
            context.waitTicks(40);
            report.put("rotated_break_after_reload", Map.of("cells", daf270.size(),
                    "client_left", context.computeOnClient(c -> (int) daf270.stream().filter(p -> !c.level.getBlockState(p).isAir()).count()),
                    "removal", String.valueOf(HeavenPlants.lastRemoval())));
        }
        System.out.println("HEAVEN_P35_CLIENT " + report);
        // ------------------------------------------------------------ world B: save-error control (no heaven)
        Map<String, Object> control = new LinkedHashMap<>();
        TestWorldSave controlSave;
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> s.setSeed(P3_SEED)).create()) {
            controlSave = world.getWorldSave();
            TestServerContext server = world.getServer();
            setup(context, server);
            server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), 206, 120, -164, Set.of(), 0, 60, true));
            try {
                control.put("ticks_until_colosseum", server.waitFor(s -> !ColosseumRuntime.data(s).sites().isEmpty(), 20 * 60 * 6));
            } catch (RuntimeException | AssertionError timeout) {
                control.put("ticks_until_colosseum", "no colosseum within 6 min: " + timeout.getMessage());
            }
            control.put("state_before_save", server.computeOnServer(HeavenP35ClientCapture::colosseumState));
            server.runCommand("save-all flush");
            context.waitTicks(40);
            Path dat = controlSave.getSaveDirectory().resolve("data/moreminecrafts/colosseum_arenas.dat");
            control.put("colosseum_file_after_save", Files.exists(dat) ? dat.toFile().length() + " bytes" : "missing");
        }
        try (TestSingleplayerContext world = controlSave.open()) {
            control.put("state_after_reopen", world.getServer().computeOnServer(HeavenP35ClientCapture::colosseumState));
        }
        System.out.println("HEAVEN_P35_SAVE_CONTROL " + control);
        System.out.println("HEAVEN_P35_CAPTURE_COMPLETE " + shot[0]);
    }

    /** Reload resources while letting the client tick (joining the future on the test thread would stall the client). */
    private static void reload(ClientGameTestContext context) {
        var future = context.computeOnClient(c -> c.reloadResourcePacks());
        context.waitFor(c -> future.isDone(), 20 * 120);
        context.waitTicks(40);
    }

    private static String colosseumState(MinecraftServer s) {
        ColosseumData d = ColosseumRuntime.data(s);
        StringBuilder b = new StringBuilder("sites=" + d.sites().size());
        for (String id : d.sites().keySet()) {
            var c = d.controller(id);
            b.append(" ").append(id).append(":json_chars=").append(c == null ? -1 : c.toJson().length());
        }
        return b.toString();
    }

    private static BlockPos root(int si, int r) { return GRID.offset(r * 15, 0, si * 16); }

    private static void setup(ClientGameTestContext context, TestServerContext server) {
        context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.renderDistance().set(12); });
        server.runOnServer(s -> {
            cmd(s, "gamerule doDaylightCycle false"); cmd(s, "gamerule doWeatherCycle false"); cmd(s, "weather clear"); cmd(s, "time set noon");
            cmd(s, "gamerule doMobSpawning false");
            s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
        });
    }

    private static StructureTemplate template(ServerLevel level, String id) {
        return level.getStructureTemplateManager().get(Identifier.parse(id)).orElseThrow(() -> new AssertionError("missing " + id));
    }

    private static void placeSpecies(ServerLevel level, String sp, BlockPos root, int r) {
        BlockPos pivot = PIVOT.get(sp);
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(ROT[r]).setRotationPivot(pivot);
        BlockPos origin = root.subtract(pivot);
        for (int cx = (root.getX() - 16) >> 4; cx <= (root.getX() + 16) >> 4; cx++) for (int cz = (root.getZ() - 16) >> 4; cz <= (root.getZ() + 16) >> 4; cz++) level.getChunk(cx, cz);
        if (!template(level, "moreminecrafts:heaven_flora/" + sp).placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS))
            throw new AssertionError("place " + sp);
    }

    private static List<BlockPos> cells(ServerLevel level, BlockPos root) {
        for (int dy = 1; dy < 5; dy++) {
            BlockState s = level.getBlockState(root.above(dy));
            if (s.getBlock() instanceof HeavenPartBlock) {
                var inst = HeavenPlants.find(level, root.above(dy), s, true, level::isLoaded);
                if (inst != null) return new ArrayList<>(inst.cells());
            }
        }
        throw new AssertionError("no flower at " + root);
    }

    private static void breakStem(MinecraftServer s, List<BlockPos> cells, BlockPos root) {
        ServerLevel l = s.overworld();
        Block stem = BuiltInRegistries.BLOCK.getValue(Identifier.parse("moreminecrafts:heaven_giant_stem"));
        BlockPos p = cells.stream().filter(c -> c.getY() > root.getY() && l.getBlockState(c).is(stem)).findFirst().orElseThrow();
        l.destroyBlock(p, true, s.getPlayerList().getPlayers().getFirst());
    }

    private static String fingerprint(ServerLevel level) {
        long h = 1125899906842597L; int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(GRID.offset(-8, -3, -8), GRID.offset(60, 14, 44))) {
            BlockState s = level.getBlockState(p);
            if (s.isAir()) continue;
            h = 31 * h + p.hashCode() * 17L + s.hashCode(); n++;
        }
        return n + ":" + Long.toHexString(h);
    }

    private static void cmd(MinecraftServer s, String c) { s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c); }

    private static void look(ClientGameTestContext context, TestServerContext server, BlockPos eye, BlockPos target, int wait) {
        double px = eye.getX() + 0.5, py = eye.getY() + 0.5, pz = eye.getZ() + 0.5;
        double dx = target.getX() + 0.5 - px, dy = target.getY() + 0.5 - py, dz = target.getZ() + 0.5 - pz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), px, py, pz, Set.of(), yaw, pitch, true));
        context.waitFor(c -> c.level != null && c.level.hasChunkAt(target), 600);
        context.waitTicks(wait);
    }

    private static void snap(ClientGameTestContext context, int[] shot, String name) {
        context.runOnClient(c -> c.gui.toastManager().clear());
        System.out.println("HEAVEN_P35_CAPTURE_SCREENSHOT " + context.takeScreenshot("heaven-p35-%02d-%s".formatted(shot[0]++, name)));
    }
}
