package dev.itzgamesgd.moreminecrafts.pyramid;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidCompletion;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * P8-C2-B rendered-client capture of NATURALLY GENERATED pyramids (validation only; gated by
 * {@code -Dmm.st01.c2b.client=true}). A real 26.3 client connects to a dedicated server whose world uses the given seed
 * with normal world generation; the targets are starts found by the probe's scan phase
 * ({@code -Dmm.st01.c2b.client.scan=<scan.json>}). The server only generates the chunks around each start; it places
 * nothing. Views: exterior and terrain integration, entrance, interior, then the Guardian encounter and the royal chest
 * opened through the client's own use-item-on-block packets. Night vision keeps interiors legible.
 */
public final class St01NaturalPyramidClientCaptureTest implements FabricClientGameTest {
    private record View(String name, double x, double y, double z, double tx, double ty, double tz) {}

    private static final List<View> VIEWS = List.of(
            new View("exterior-oblique-natural-terrain", 66, 28, 92, 0, 10, 0),
            new View("entrance-approach-from-desert", 0, 3, 70, 0, 6, 30),
            new View("aerial-footprint-and-edges", -70, 75, -70, 0, 0, 0),
            new View("footprint-edge-terrain-west", -58, 6, 0, -40, 0, 10),
            new View("footprint-edge-terrain-north", 10, 6, -58, 0, 0, -40),
            new View("entrance-hall-interior", 0, 0, 39, 0, 4, 22),
            new View("gallery", 0, 0, 17, 0, 4, -4),
            new View("royal-hall-d1-toward-primary", -21, -22, 8, -21, -20, 33),
            new View("treasury-vault", 7, -30, 26, 25, -26, 26),
            new View("secret-crypt", 9, -30, -1, 16, -29, -1));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("mm.st01.c2b.client")) return;
        String out = System.getProperty("mm.st01.c2b.client.out", "build/st01-p8c2b/client/manifest.json");
        JsonObject scan;
        try {
            scan = JsonParser.parseString(Files.readString(Path.of(System.getProperty("mm.st01.c2b.client.scan")))).getAsJsonObject();
        } catch (Exception e) { throw new AssertionError(e); }
        long seed = scan.get("seed").getAsLong();
        int maxTargets = Integer.getInteger("mm.st01.c2b.client.max", 2);
        List<Object[]> targets = new ArrayList<>(); // ground, orientation, route, startChunk
        for (var e : scan.getAsJsonObject("starts").entrySet()) {
            if (targets.size() >= maxTargets) break;
            JsonObject s = e.getValue().getAsJsonObject(), a = s.getAsJsonObject("anchor");
            targets.add(new Object[]{new BlockPos(a.get("x").getAsInt(), a.get("y").getAsInt(), a.get("z").getAsInt()),
                    Orientation.valueOf(s.get("orientation").getAsString()), AccessRoute.valueOf(s.get("route").getAsString()), e.getKey()});
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        List<Map<String, Object>> captures = new ArrayList<>();
        manifest.put("minecraft", "26.3");
        manifest.put("seed", seed);
        manifest.put("world", "default noise world generation; nothing placed by the test");
        manifest.put("night_vision", true);
        manifest.put("captures", captures);
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("level-seed", Long.toString(seed));
        properties.setProperty("generate-structures", "true");
        properties.setProperty("spawn-protection", "0");
        properties.setProperty("spawn-monsters", "false");
        properties.setProperty("view-distance", "10");
        properties.setProperty("simulation-distance", "6");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.runOnClient(c -> { c.gui.hud.toggle(); c.options.renderDistance().set(12); });
            for (Object[] t : targets) {
                BlockPos ground = (BlockPos) t[0];
                Orientation o = (Orientation) t[1];
                AccessRoute route = (AccessRoute) t[2];
                String tag = ((String) t[3]).replace(',', '_');
                Map<String, Object> status = new LinkedHashMap<>();
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    var box = St01PyramidAssembly.completionBox(ground, o);
                    for (int cx = (box.minX() >> 4) - 6; cx <= (box.maxX() >> 4) + 6; cx++)
                        for (int cz = (box.minZ() >> 4) - 6; cz <= (box.maxZ() >> 4) + 6; cz++) level.getChunk(cx, cz, ChunkStatus.FULL);
                    St01PyramidCompletion.processPending(s);
                    var p = St01PyramidAssembly.describe(ground, o, route);
                    status.put("registered", RoyalChestRuntime.data(level).instance(RoyalChestRuntime.canonicalId(level, p.primaryLeft())).isPresent());
                    status.put("primary_left_block", String.valueOf(BuiltInRegistries.BLOCK.getKey(level.getBlockState(p.primaryLeft()).getBlock())));
                    ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SPECTATOR);
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 3600, 0, false, false, false));
                });
                manifest.put("target-" + tag, Map.of("ground", ground.toShortString(), "orientation", o.name(), "route", route.name(), "server", status));
                context.waitTicks(20);
                for (View v : VIEWS) captures.add(capture(context, server, connection, "st01-p8c2b-" + tag + "-" + v.name, ground, o, v, true));
                // Guardian encounter and royal chest through the client's own interaction packets
                var p = St01PyramidAssembly.describe(ground, o, route);
                BlockPos chest = p.primaryRight();
                server.runOnServer(s -> {
                    ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.CREATIVE);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                });
                View front = new View("primary-front", -20, -20, 31, -20, -20, 33);
                captures.add(capture(context, server, connection, "st01-p8c2b-" + tag + "-before-interaction", ground, o, front, false));
                Map<String, Object> interaction = new LinkedHashMap<>();
                interaction.put("first_use", use(context, chest));
                context.waitTicks(40);
                connection.waitForClientboundPackets();
                interaction.put("guardians_after_first_use", context.computeOnClient(c -> c.level.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class, c.player.getBoundingBox().inflate(32),
                        e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().contains("tomb_guardian")).size()));
                View guardianView = new View("guardian-encounter", -21, -20, 32, -21, -20, 26);
                captures.add(capture(context, server, connection, "st01-p8c2b-" + tag + "-guardian-encounter", ground, o, guardianView, false));
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                    var anchor = p.primaryLeft();
                    var gs = level.getEntities(dev.itzgamesgd.moreminecrafts.boss.TombGuardianEntities.TOMB_GUARDIAN, g -> anchor.equals(g.tombAnchor()));
                    interaction.put("server_guardians", gs.size());
                    for (var g : gs) { g.skipAwakening(); g.hurtServer(level, level.damageSources().playerAttack(player), 10000.0F); }
                    interaction.put("defeated", dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters.isDefeated(level, anchor));
                });
                context.waitTicks(40);
                captures.add(capture(context, server, connection, "st01-p8c2b-" + tag + "-after-defeat", ground, o, front, false));
                interaction.put("second_use", use(context, chest));
                context.waitTicks(20);
                interaction.put("client_screen", context.computeOnClient(c -> c.gui.screen() == null ? null : c.gui.screen().getClass().getSimpleName()
                        + " / " + c.gui.screen().getTitle().getString()));
                interaction.put("menu_screenshot", String.valueOf(context.takeScreenshot("st01-p8c2b-" + tag + "-royal-chest-menu")));
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    var id = RoyalChestRuntime.canonicalId(level, p.primaryLeft());
                    interaction.put("generated", RoyalChestRuntime.data(level).isGenerated(id, RoyalChestRole.PRIMARY));
                    s.getPlayerList().getPlayers().getFirst().closeContainer();
                    s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
                });
                context.runOnClient(c -> { if (c.gui.screen() != null) c.player.closeContainer(); });
                context.waitTicks(10);
                manifest.put("interaction-" + tag, interaction);
            }
            manifest.put("client", context.computeOnClient(c -> Map.of("window", c.getWindow().getWidth() + "x" + c.getWindow().getHeight(),
                    "fov", c.options.fov().get(), "render_distance", c.options.renderDistance().get())));
        }
        try {
            Path p = Path.of(out);
            Files.createDirectories(p.getParent());
            Files.writeString(p, new GsonBuilder().setPrettyPrinting().create().toJson(manifest), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new AssertionError(e); }
        System.out.println("ST01_P8C2B_CLIENT_CAPTURE_DONE captures=" + captures.size());
    }

    /** Right-click on the chest from the client: the real ServerboundUseItemOnPacket path. */
    private static String use(ClientGameTestContext context, BlockPos chest) {
        return context.computeOnClient(c -> {
            Vec3 hit = Vec3.atCenterOf(chest);
            Direction face = Direction.getApproximateNearest(c.player.getEyePosition().subtract(hit));
            var result = c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, chest, false));
            return result.toString();
        });
    }

    private static Map<String, Object> capture(ClientGameTestContext context, TestDedicatedServerContext server, TestDedicatedServerConnection connection,
            String name, BlockPos ground, Orientation o, View v, boolean longWait) {
        BlockPos feet = St01PyramidAssembly.worldPos(BlockPos.containing(v.x, v.y, v.z), ground, o);
        BlockPos look = St01PyramidAssembly.worldPos(BlockPos.containing(v.tx, v.ty, v.tz), ground, o);
        double x = feet.getX() + 0.5, y = feet.getY(), z = feet.getZ() + 0.5;
        double tx = look.getX() + 0.5, ty = look.getY() + 0.5, tz = look.getZ() + 0.5;
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty - (y + 1.62), Math.hypot(tx - x, tz - z)));
        Map<String, Object> cap = new LinkedHashMap<>();
        cap.put("name", name);
        cap.put("world_feet", List.of(x, y, z));
        cap.put("yaw", yaw);
        cap.put("pitch", pitch);
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), x, y, z, Set.of(), yaw, pitch, true));
        boolean reached = true;
        try {
            context.waitFor(c -> c.player.position().distanceTo(new Vec3(x, y, z)) < 0.6, 400);
        } catch (RuntimeException e) {
            reached = false;
        }
        context.waitTicks(longWait ? 100 : 40);
        connection.waitForClientboundPackets();
        context.waitTicks(10);
        cap.put("position_reached", reached);
        cap.put("client_eye_block", context.computeOnClient(c -> String.valueOf(
                BuiltInRegistries.BLOCK.getKey(c.level.getBlockState(BlockPos.containing(c.player.getEyePosition())).getBlock()))));
        cap.put("client_look_target_block", context.computeOnClient(c -> String.valueOf(
                BuiltInRegistries.BLOCK.getKey(c.level.getBlockState(look).getBlock()))));
        cap.put("screenshot", String.valueOf(context.takeScreenshot(name)));
        return cap;
    }
}
