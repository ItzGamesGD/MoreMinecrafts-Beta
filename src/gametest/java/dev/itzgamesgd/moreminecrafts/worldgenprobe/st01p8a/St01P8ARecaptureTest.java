package dev.itzgamesgd.moreminecrafts.worldgenprobe.st01p8a;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/**
 * ST-01 P8-A visual-first recapture (validation only; gated by {@code -Dmm.st01.p8a.r2=true}).
 *
 * <p>No automated walking. Cameras are placed deterministically with a look-at target, the client pose is measured and
 * recorded next to the commanded pose, and the manifest is rewritten after every capture so a later failure cannot erase
 * earlier evidence. Night vision and a hidden HUD are temporary viewing aids only; the structure is not altered.
 * A final step teleports the player next to the PRIMARY chest and right-clicks it to inspect the opened container.
 */
public final class St01P8ARecaptureTest implements FabricClientGameTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier TEMPLATE_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "pyramid/royal_hall");
    private static final BlockPos ORIGIN = new BlockPos(-40, -24, 2);
    private static final BlockPos CHEST_LEFT = new BlockPos(-21, -20, 33);
    private static final BlockPos CHEST_RIGHT = new BlockPos(-20, -20, 33);

    /** A camera: name, camera (eye-level feet position) and the point it looks at. Yaw/pitch are derived. */
    private record View(String name, double x, double y, double z, double tx, double ty, double tz) {
        float yaw() {
            return (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        }

        float pitch() {
            return (float) -Math.toDegrees(Math.atan2(ty - (y + 1.62), Math.hypot(tx - x, tz - z)));
        }
    }

    private static final List<View> VIEWS = List.of(
            new View("R2-01-exterior-north-front", -21, -8, -38, -21, -14, 20),
            new View("R2-02-exterior-oblique-northwest", -66, 2, -24, -21, -14, 20),
            new View("R2-03-exterior-oblique-northeast", 24, 2, -24, -21, -14, 20),
            new View("R2-04-exterior-roof-overview", -21, 24, -8, -21, -14, 20),
            new View("R2-05-entrance-approach-exterior", -21, -20, -14, -21, -21, 6),
            new View("R2-06-doorway-looking-in", -21, -22, 0, -21, -21, 25),
            new View("R2-07-hall-from-entrance", -21, -22, 4, -21, -19, 30),
            new View("R2-08-mid-hall-toward-dais", -21, -22, 14, -21, -19, 33),
            new View("R2-09-chest-and-dais-front", -21, -22, 26, -20.5, -19.5, 33.5),
            new View("R2-10-chest-closeup-from-dais", -20.5, -19.4, 27.5, -20.5, -19.5, 33.5),
            new View("R2-11-guardian-arena-from-north", -21, -22, 10, -21, -22, 26),
            new View("R2-12-guardian-arena-from-west", -35, -22, 24, -21, -22, 26),
            new View("R2-13-sarcophagus-dais-west", -34, -19, 36, -22, -20, 33),
            new View("R2-14-frontal-stairs-low", -21, -22, 24, -21, -21, 31),
            new View("R2-15-stairs-from-side", -27, -21, 29, -21, -21, 31),
            new View("R2-16-side-depth-east-interior", -9, -22, 22, -30, -20, 22),
            new View("R2-17-side-depth-west-interior", -35, -19, 10, -10, -19, 26),
            new View("R2-18-exterior-rear-south", -21, -8, 60, -21, -14, 20),
            new View("R2-19-exterior-side-west", -64, -12, 20, -21, -14, 20));

    /** ST-01 P8-A-D1 view set (floor detailing and dais stairs); select with -Dmm.st01.p8a.r2.set=d1. Same poses for before/after. */
    private static final List<View> D1_VIEWS = List.of(
            new View("D1-01-chamber-floor-elevated-north", -21, -12, 6, -21, -22, 23),
            new View("D1-02-field-motif-oblique-east", -9, -15, 10, -21, -22, 22),
            new View("D1-03-field-motif-oblique-west", -33, -15, 10, -21, -22, 22),
            new View("D1-04-runner-from-doorway-floor-level", -21, -22, 4, -21, -22, 19),
            new View("D1-05-guardian-arena-and-west-aisle", -35, -14, 30, -23, -22, 18),
            new View("D1-06-east-side-aisle-low", -6, -20, 7, -6, -22, 31),
            new View("D1-07-stairs-front-low", -21, -22, 24, -21, -20.5, 31),
            new View("D1-08-stairs-side", -30, -21, 30.5, -21, -21, 31),
            new View("D1-09-final-stair-to-dais-connection", -21, -22, 28.5, -21, -20.2, 31.7),
            new View("D1-10-primary-chest-and-dais-front", -21, -22, 26, -20.5, -19.5, 33.5),
            new View("D1-11-dais-from-above", -21, -12, 27, -21, -20, 34),
            new View("D1-12-hall-floor-from-rear-high", -15, -12, 35, -21, -22, 12));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("mm.st01.p8a.r2")) return;
        String out = System.getProperty("mm.st01.p8a.r2.out", "build/st01-p8a-client/manifest_r2.json");
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("minecraft", "26.3");
        manifest.put("structure_origin", String.valueOf(ORIGIN));
        manifest.put("pose_semantics", "commanded = what the test asked for; client_measured = read from the rendering client after the wait");
        manifest.put("viewing_aids", "HUD hidden; temporary night vision on the player. Structure and world unchanged.");
        manifest.put("walk_executed", false);
        List<Map<String, Object>> captures = new ArrayList<>();
        manifest.put("captures", captures);
        manifest.put("status", "started");
        flush(out, manifest);

        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("level-type", "minecraft:flat");
        properties.setProperty("generate-structures", "false");
        properties.setProperty("spawn-protection", "0");
        properties.setProperty("view-distance", "8");
        properties.setProperty("simulation-distance", "6");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            double[] ground = new double[1];
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                StructureTemplate template;
                String templateFile = System.getProperty("mm.st01.p8a.r2.template.file", "");
                if (templateFile.isEmpty()) {
                    template = level.getStructureTemplateManager().get(TEMPLATE_ID).orElseThrow();
                } else { // "before" captures: the approved P8-A baseline asset, read from a file, placed identically
                    try (InputStream in = Files.newInputStream(Path.of(templateFile))) {
                        CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
                        template = new StructureTemplate();
                        template.load(s.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
                    } catch (IOException e) {
                        throw new AssertionError("could not read baseline template " + templateFile, e);
                    }
                }
                if (!template.placeInWorld(level, ORIGIN, ORIGIN, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS)) {
                    throw new AssertionError("template placement returned false");
                }
                ground[0] = level.getHeight(Heightmap.Types.WORLD_SURFACE, -80, -20);
                player.setGameMode(GameType.SPECTATOR); // camera views: no gravity, so the commanded height is kept
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 3600, 0, false, false, false));
            });
            manifest.put("flat_world_surface_height_at_x-80_z-20", ground[0]);
            context.runOnClient(c -> c.gui.hud.toggle()); // HUD starts visible: one toggle hides it
            context.waitTicks(40);
            connection.waitForClientboundPackets();

            String prefix = System.getProperty("mm.st01.p8a.r2.prefix", "R2");
            String only = System.getProperty("mm.st01.p8a.r2.only", "");
            manifest.put("run_prefix", prefix);
            String templateFileProp = System.getProperty("mm.st01.p8a.r2.template.file", "");
            manifest.put("template_source", templateFileProp.isEmpty() ? "mod resource moreminecrafts:pyramid/royal_hall" : "file " + templateFileProp);
            List<View> viewSet = "d1".equals(System.getProperty("mm.st01.p8a.r2.set", "r2")) ? D1_VIEWS : VIEWS;
            for (View v0 : viewSet) {
                if (!only.isEmpty() && java.util.Arrays.stream(only.split(",")).noneMatch(v0.name()::startsWith)) continue;
                View v = new View(v0.name().replaceFirst("^[A-Za-z0-9]+(?=-)", prefix), v0.x(), v0.y(), v0.z(), v0.tx(), v0.ty(), v0.tz());
                Map<String, Object> cap = new LinkedHashMap<>();
                cap.put("name", v.name());
                cap.put("commanded", Map.of("feet", List.of(v.x(), v.y(), v.z()), "yaw", round(v.yaw()), "pitch", round(v.pitch()),
                        "look_at", List.of(v.tx(), v.ty(), v.tz())));
                captures.add(cap);
                try {
                    server.runOnServer(s -> {
                        ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                        player.teleportTo(s.overworld(), v.x(), v.y(), v.z(), Set.of(), v.yaw(), v.pitch(), true);
                    });
                    boolean reached = true;
                    try {
                        context.waitFor(c -> c.player.position().distanceTo(new Vec3(v.x(), v.y(), v.z())) < 0.6, 400);
                    } catch (RuntimeException e) {
                        reached = false;
                    }
                    context.waitTicks(100);
                    connection.waitForClientboundPackets();
                    context.waitTicks(10);
                    cap.put("client_measured", context.computeOnClient(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        Vec3 p = c.player.position();
                        m.put("feet", List.of(round(p.x), round(p.y), round(p.z)));
                        m.put("yaw", round(c.player.getYRot()));
                        m.put("pitch", round(c.player.getXRot()));
                        m.put("eye_block", blockId(c.level.getBlockState(BlockPos.containing(c.player.getEyePosition()))));
                        m.put("client_wall_west_(-40,-15,20)", blockId(c.level.getBlockState(new BlockPos(-40, -15, 20))));
                        m.put("client_floor_(-21,-23,10)", blockId(c.level.getBlockState(new BlockPos(-21, -23, 10))));
                        return m;
                    }));
                    cap.put("position_reached", reached);
                    cap.put("screenshot", String.valueOf(context.takeScreenshot(v.name())));
                    cap.put("status", reached ? "captured" : "captured_but_client_pose_differs_from_command");
                } catch (RuntimeException | Error e) {
                    cap.put("status", "failed: " + e);
                }
                flush(out, manifest);
            }

            // ---- Optional short, stall-detected walks (hypothesis test: the central base blocks the centre line).
            if (Boolean.getBoolean("mm.st01.p8a.r2.walk")) {
                Map<String, Object> walks = new LinkedHashMap<>();
                manifest.put("walks", walks);
                if (Boolean.getBoolean("mm.st01.p8a.r2.walk.full")) {
                    walks.put("centre_line_x-20.5", walk(context, server, connection, -20.5, -22.0, 3.5));
                    flush(out, manifest);
                    walks.put("beside_base_x-25.5", walk(context, server, connection, -25.5, -22.0, 12.5));
                    flush(out, manifest);
                }
                // Past the central base, on the centre line: tests the stair + dais climb by itself.
                walks.put("anchor_x-20.5_z24.5_to_dais", walk(context, server, connection, -20.5, -22.0, 24.5));
                flush(out, manifest);
            }

            // ---- Independent PRIMARY B check: direct positioning near the chest, no navigation.
            Map<String, Object> chest = new LinkedHashMap<>();
            manifest.put("primary_b_chest", chest);
            try {
                server.runOnServer(s -> {
                    ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL); // an ordinary player opens chests; no flight or creative flags involved
                    // stand on the lowest stair tread (top face y=-21), 3 blocks in front of the PRIMARY chest
                    player.teleportTo(s.overworld(), -20.5, -21.0, 30.5, Set.of(), 0f, 0f, true);
                });
                context.waitTicks(60);
                connection.waitForClientboundPackets();
                chest.put("client_state_left", context.computeOnClient(c -> describe(c.level.getBlockState(CHEST_LEFT))));
                chest.put("client_state_right", context.computeOnClient(c -> describe(c.level.getBlockState(CHEST_RIGHT))));
                chest.put("client_player_feet", context.computeOnClient(c -> List.of(round(c.player.getX()), round(c.player.getY()), round(c.player.getZ()))));
                context.getInput().lookAt(CHEST_LEFT);
                context.waitTicks(5);
                chest.put("crosshair_target", context.computeOnClient(c -> {
                    if (c.hitResult instanceof net.minecraft.world.phys.BlockHitResult b) {
                        return b.getType() + " pos=" + b.getBlockPos() + " dir=" + b.getDirection() + " block=" + blockId(c.level.getBlockState(b.getBlockPos()));
                    }
                    return String.valueOf(c.hitResult);
                }));
                chest.put("client_modes", context.computeOnClient(c -> "spectator=" + c.player.isSpectator() + " creative=" + c.player.isCreative()));
                chest.put("eye_pos", context.computeOnClient(c -> String.valueOf(c.player.getEyePosition())));
                context.getInput().pressMouse(1);
                chest.put("method", "mouse_button_1");
                try {
                    context.waitFor(c -> c.gui.screen() instanceof AbstractContainerScreen<?>, 100);
                } catch (AssertionError | RuntimeException e) {
                    // fall back to the client's own use-item-on path (same packet a right-click sends)
                    chest.put("mouse_press_result", "no container screen opened: " + e.getMessage());
                    chest.put("method", "MultiPlayerGameMode.useItemOn with the crosshair hit");
                    chest.put("use_item_on_result", context.computeOnClient(c -> {
                        if (c.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr) {
                            return String.valueOf(c.gameMode.useItemOn(c.player, net.minecraft.world.InteractionHand.MAIN_HAND, bhr));
                        }
                        return "no block hit";
                    }));
                    context.waitFor(c -> c.gui.screen() instanceof AbstractContainerScreen<?>, 200);
                }
                context.waitTicks(10);
                connection.waitForClientboundPackets();
                chest.put("opened", context.computeOnClient(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    var menu = c.player.containerMenu;
                    m.put("screen", c.gui.screen() == null ? "null" : c.gui.screen().getClass().getSimpleName());
                    m.put("menu", menu.getClass().getSimpleName());
                    m.put("total_slots", menu.slots.size());
                    m.put("container_slots", menu.slots.size() - 36);
                    if (menu instanceof ChestMenu cm) m.put("rows", cm.getRowCount());
                    return m;
                }));
                context.runOnClient(c -> c.gui.hud.toggle()); // restore the HUD for the chest-screen frame
                chest.put("screenshot", String.valueOf(context.takeScreenshot("R2-20-primary-chest-open-54-slot")));
                chest.put("status", "tested");
            } catch (RuntimeException | Error e) {
                chest.put("status", "failed: " + e);
            }
            flush(out, manifest);
            manifest.put("client", context.computeOnClient(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("window", c.getWindow().getWidth() + "x" + c.getWindow().getHeight());
                m.put("gui_scale", c.getWindow().getGuiScale());
                m.put("fov", c.options.fov().get());
                m.put("render_distance", c.options.renderDistance().get());
                return m;
            }));
        } finally {
            manifest.put("status", "finished");
            flush(out, manifest);
        }
        System.out.println("ST01_P8A_R2_DONE captures=" + captures.size());
    }

    /** Survival walk with real held keys; stops on arrival at the dais top, a stall, or the tick budget. */
    private static Map<String, Object> walk(ClientGameTestContext context, TestDedicatedServerContext server,
                                            TestDedicatedServerConnection connection, double sx, double sy, double sz) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("start", List.of(sx, sy, sz));
        List<Object> trace = new ArrayList<>();
        r.put("trace", trace);
        server.runOnServer(s -> {
            ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(s.overworld(), sx, sy, sz, Set.of(), 0f, 0f, true);
        });
        context.waitTicks(40);
        connection.waitForClientboundPackets();
        context.getInput().holdKey(o -> o.keyUp);
        boolean jump = !Boolean.getBoolean("mm.st01.p8a.r2.walk.nojump");
        r.put("jump_key_held", jump);
        if (jump) context.getInput().holdKey(o -> o.keyJump);
        double[] last = null;
        int lastMoveTick = 0;
        String outcome = "tick_budget_exhausted";
        int t;
        for (t = 0; t < 600; t++) {
            context.waitTick();
            double[] p = context.computeOnClient(c -> new double[]{c.player.getX(), c.player.getY(), c.player.getZ()});
            if (t % 30 == 0) trace.add(List.of(t, round(p[0]), round(p[1]), round(p[2])));
            if (p[1] > -20.2 && p[2] > 31.8) { outcome = "reached_dais_top"; last = p; break; }
            if (last == null || Math.hypot(p[0] - last[0], Math.hypot(p[1] - last[1], p[2] - last[2])) > 0.05) { last = p; lastMoveTick = t; }
            else if (t - lastMoveTick >= 60) { outcome = "stalled"; break; }
        }
        context.getInput().releaseKey(o -> o.keyUp);
        if (jump) context.getInput().releaseKey(o -> o.keyJump);
        final double[] end = context.computeOnClient(c -> new double[]{c.player.getX(), c.player.getY(), c.player.getZ()});
        r.put("outcome", outcome);
        r.put("ticks", t);
        r.put("end", List.of(round(end[0]), round(end[1]), round(end[2])));
        r.put("blocks_ahead_of_end_position", context.computeOnClient(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            for (double dy : new double[]{0.0, 1.0, 1.8}) {
                m.put("dy+" + dy, blockId(c.level.getBlockState(BlockPos.containing(end[0], end[1] + dy, end[2] + 0.45))));
            }
            return m;
        }));
        return r;
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static String describe(BlockState state) {
        if (state.getBlock() instanceof ChestBlock) {
            return blockId(state) + " type=" + state.getValue(ChestBlock.TYPE) + " facing=" + state.getValue(ChestBlock.FACING);
        }
        return blockId(state);
    }

    /** Atomic rewrite so a crash never leaves a half-written manifest. */
    private static void flush(String out, Map<String, Object> manifest) {
        try {
            Path p = Path.of(out);
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(manifest), StandardCharsets.UTF_8);
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("could not write recapture manifest: " + e);
        }
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
