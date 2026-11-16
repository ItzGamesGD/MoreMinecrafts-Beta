package dev.itzgamesgd.moreminecrafts.pyramid;

import com.google.gson.GsonBuilder;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01PyramidCompletion;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

/**
 * Natural pyramid verification (validation only; gated by {@code -Dmm.pyramid.natural.verify=true}).
 * Order: (1) normal world builder, same variable passed to createServer, generator class asserted
 * NoiseBasedChunkGenerator; (2) one /locate; (3) generate the located chunks and check the structure actually exists;
 * (4) capture exterior and interior at the located coordinates. Nothing is placed by the test.
 * Results: build/pyramid-natural-verify/result.json. Each step is PASS / FAIL / NOT RUN.
 */
public final class St01NaturalVerifyClientTest implements FabricClientGameTest {
    private record View(String name, double x, double y, double z, double tx, double ty, double tz) {}

    private static final List<View> VIEWS = List.of(
            new View("exterior-oblique", 66, 28, 92, 0, 10, 0),
            new View("exterior-entrance-approach", 0, 3, 70, 0, 6, 30),
            new View("exterior-aerial", -70, 75, -70, 0, 0, 0),
            new View("interior-entrance-hall", 0, 0, 39, 0, 4, 22),
            new View("interior-gallery", 0, 0, 17, 0, 4, -4),
            new View("interior-royal-hall", -21, -22, 8, -21, -20, 33),
            new View("interior-treasury-vault", 7, -30, 26, 25, -26, 26));

    private static final Pattern COORDS = Pattern.compile("\\[(-?\\d+), (~|-?\\d+), (-?\\d+)]");

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("mm.pyramid.natural.verify")) return;
        Map<String, Object> result = new LinkedHashMap<>();
        for (String step : List.of("generator", "locate", "structure", "capture")) result.put(step, "NOT RUN");
        Map<String, Object> detail = new LinkedHashMap<>();
        result.put("detail", detail);
        String seed = System.getProperty("mm.pyramid.natural.verify.seed", "1004");
        detail.put("seed", seed);
        Throwable failure = null;

        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("level-seed", seed);
        properties.setProperty("generate-structures", "true");
        properties.setProperty("spawn-protection", "0");
        properties.setProperty("spawn-monsters", "false");
        properties.setProperty("spawn-animals", "false");
        properties.setProperty("view-distance", "10");
        properties.setProperty("simulation-distance", "6");
        properties.setProperty("max-tick-time", "-1");

        // Ordinary (non-consistent-settings) world builder, kept in one variable and passed to createServer.
        var builder = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(st -> st.setSeed(seed));
        try (TestDedicatedServerContext server = builder.createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);

            // ---- 1. generator ---------------------------------------------------------------------------------------
            String generatorClass = server.computeOnServer(s -> s.overworld().getChunkSource().getGenerator().getClass().getName());
            boolean noise = server.computeOnServer(s -> s.overworld().getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator);
            detail.put("generator_class", generatorClass);
            System.out.println("PYRAMID_VERIFY generator_class=" + generatorClass + " noise_based=" + noise);
            result.put("generator", noise ? "PASS" : "FAIL");
            if (!noise) throw new AssertionError("Generator is not NoiseBasedChunkGenerator: " + generatorClass);

            // ---- 2. one /locate -------------------------------------------------------------------------------------
            List<String> messages = new ArrayList<>();
            String located = server.computeOnServer(s -> {
                CommandSource sink = new CommandSource() {
                    @Override public void sendSystemMessage(Component message) { messages.add(message.getString()); }
                    @Override public boolean acceptsSuccess() { return true; }
                    @Override public boolean acceptsFailure() { return true; }
                    @Override public boolean shouldInformAdmins() { return false; }
                };
                var source = s.createCommandSourceStack().withSource(sink).withLevel(s.overworld())
                        .withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER);
                s.getCommands().performPrefixedCommand(source, "locate structure " + St01Worldgen.STRUCTURE_ID);
                return String.join(" | ", messages);
            });
            detail.put("locate_output", located);
            System.out.println("PYRAMID_VERIFY locate_output=" + located);
            Matcher m = COORDS.matcher(located);
            if (!m.find()) {
                result.put("locate", "FAIL");
                throw new AssertionError("/locate returned no coordinates: " + located);
            }
            int lx = Integer.parseInt(m.group(1)), lz = Integer.parseInt(m.group(3));
            detail.put("locate_xz", List.of(lx, lz));
            result.put("locate", "PASS");

            // ---- 3. structure exists --------------------------------------------------------------------------------
            Map<String, Object> structure = new LinkedHashMap<>();
            detail.put("structure", structure);
            Object[] found = new Object[4]; // ground, orientation, route
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(St01Worldgen.STRUCTURE_ID).orElseThrow();
                ChunkPos around = new ChunkPos(lx >> 4, lz >> 4);
                StructureStart start = null;
                // the located column is a start chunk; look in a small neighbourhood in case /locate reports an offset
                for (int dx = -1; dx <= 1 && start == null; dx++)
                    for (int dz = -1; dz <= 1 && start == null; dz++) {
                        StructureStart st = level.getChunk(around.x() + dx, around.z() + dz, ChunkStatus.STRUCTURE_STARTS)
                                .getStartForStructure(holder.value());
                        if (st != null && st.isValid()) start = st;
                    }
                structure.put("valid_start_found", start != null);
                if (start == null) return;
                var meta = St01PyramidCompletion.metaOf(start).orElse(null);
                structure.put("meta_present", meta != null);
                structure.put("start_piece_count", start.getPieces().size());
                if (meta == null) return;
                found[3] = meta;
            });
            if (found[3] instanceof St01PyramidCompletion.Meta meta0) {
                // generate in small batches so the client keeps receiving keep-alives (one big task timed the client out)
                var box = meta0.box();
                // a player at the pyramid holds its chunks loaded (as in normal play); registration is chunk-load driven
                BlockPos at = meta0.ground();
                server.runOnServer(s -> {
                    ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(s.overworld(), at.getX() + 0.5, at.getY() + 25, at.getZ() + 0.5, Set.of(), 0f, 0f, true);
                });
                context.waitTicks(40);
                List<int[]> chunks = new ArrayList<>();
                for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++)
                    for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) chunks.add(new int[]{cx, cz});
                for (int i = 0; i < chunks.size(); i += 4) {
                    int from = i;
                    server.runOnServer(s -> {
                        for (int k = from; k < Math.min(from + 4, chunks.size()); k++)
                            s.overworld().getChunk(chunks.get(k)[0], chunks.get(k)[1], ChunkStatus.FULL);
                    });
                    context.waitTicks(1);
                }
                // the completion handoff registers the royal chest asynchronously: poll up to ~30 s
                var pl = St01PyramidAssembly.describe(meta0.ground(), meta0.orientation(), meta0.route());
                for (int i = 0; i < 120; i++) {
                    server.runOnServer(s -> St01PyramidCompletion.processPending(s));
                    boolean reg = server.computeOnServer(s -> RoyalChestRuntime.data(s.overworld())
                            .instance(RoyalChestRuntime.canonicalId(s.overworld(), pl.primaryLeft())).isPresent());
                    detail.put("registration_polls", i + 1);
                    if (reg) break;
                    context.waitTicks(10);
                }
            }
            server.runOnServer(s -> {
                ServerLevel level = s.overworld();
                if (!(found[3] instanceof St01PyramidCompletion.Meta meta)) return;
                var p = St01PyramidAssembly.describe(meta.ground(), meta.orientation(), meta.route());
                BlockPos g = meta.ground();
                found[0] = g; found[1] = meta.orientation(); found[2] = meta.route();
                structure.put("ground", g.toShortString());
                structure.put("orientation", meta.orientation().name());
                structure.put("route", meta.route().name());
                structure.put("biome_at_ground", level.getBiome(g).unwrapKey().map(k -> k.identifier().toString()).orElse("?"));
                structure.put("biome_at_located_xz", level.getBiome(new BlockPos(lx, g.getY(), lz)).unwrapKey()
                        .map(k -> k.identifier().toString()).orElse("?"));
                String primary = String.valueOf(BuiltInRegistries.BLOCK.getKey(level.getBlockState(p.primaryLeft()).getBlock()));
                structure.put("primary_left_block", primary);
                structure.put("royal_chest_registered",
                        RoyalChestRuntime.data(level).instance(RoyalChestRuntime.canonicalId(level, p.primaryLeft())).isPresent());
                int solid = 0, total = 0;
                var cb = p.completionBounds();
                for (int x = cb.minX(); x <= cb.maxX(); x += 3)
                    for (int y = cb.minY(); y <= cb.maxY(); y += 3)
                        for (int z = cb.minZ(); z <= cb.maxZ(); z += 3) {
                            total++;
                            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) solid++;
                        }
                structure.put("sampled_non_air_blocks", solid + "/" + total);
            });
            boolean placed = Boolean.TRUE.equals(structure.get("valid_start_found"))
                    && Boolean.TRUE.equals(structure.get("meta_present"))
                    && Boolean.TRUE.equals(structure.get("royal_chest_registered"))
                    && structure.get("primary_left_block") instanceof String b && !b.equals("minecraft:air");
            result.put("structure", placed ? "PASS" : "FAIL");
            System.out.println("PYRAMID_VERIFY structure=" + structure);
            if (!placed) throw new AssertionError("Pyramid structure not confirmed: " + structure);

            // ---- 4. client captures at the located coordinates ------------------------------------------------------
            BlockPos ground = (BlockPos) found[0];
            Orientation o = (Orientation) found[1];
            AccessRoute route = (AccessRoute) found[2];
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.renderDistance().set(12); });
            server.runOnServer(s -> {
                ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SPECTATOR);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 3600, 0, false, false, false));
            });
            context.waitTicks(20);
            List<Map<String, Object>> captures = new ArrayList<>();
            detail.put("captures", captures);
            int index = 0;
            for (View v : VIEWS) captures.add(capture(context, server, connection,
                    "pyramid-natural-%02d-%s".formatted(index++, v.name()), ground, o, v));
            long reached = captures.stream().filter(c -> Boolean.TRUE.equals(c.get("position_reached"))).count();
            boolean ok = reached == VIEWS.size();
            result.put("capture", ok ? "PASS" : "FAIL");
            if (!ok) throw new AssertionError("Only " + reached + "/" + VIEWS.size() + " capture positions reached");
        } catch (Throwable t) {
            failure = t;
            detail.put("failure", t.toString());
            System.out.println("PYRAMID_VERIFY failure=" + t);
        } finally {
            try {
                Path out = Path.of(System.getProperty("mm.pyramid.natural.verify.out", "build/pyramid-natural-verify/result.json"));
                Files.createDirectories(out.getParent());
                Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(result), StandardCharsets.UTF_8);
            } catch (Exception e) { System.out.println("PYRAMID_VERIFY result write failed: " + e); }
            System.out.println("PYRAMID_VERIFY_RESULT " + result);
        }
        if (failure != null) throw new AssertionError(failure);
        System.out.println("PYRAMID_VERIFY_DONE");
    }

    private static Map<String, Object> capture(ClientGameTestContext context, TestDedicatedServerContext server,
            TestDedicatedServerConnection connection, String name, BlockPos ground, Orientation o, View v) {
        BlockPos feet = St01PyramidAssembly.worldPos(BlockPos.containing(v.x, v.y, v.z), ground, o);
        BlockPos look = St01PyramidAssembly.worldPos(BlockPos.containing(v.tx, v.ty, v.tz), ground, o);
        double x = feet.getX() + 0.5, y = feet.getY(), z = feet.getZ() + 0.5;
        double tx = look.getX() + 0.5, ty = look.getY() + 0.5, tz = look.getZ() + 0.5;
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty - (y + 1.62), Math.hypot(tx - x, tz - z)));
        Map<String, Object> cap = new LinkedHashMap<>();
        cap.put("name", name);
        cap.put("world_feet", List.of(x, y, z));
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), x, y, z, Set.of(), yaw, pitch, true));
        boolean reached = true;
        try {
            context.waitFor(c -> c.player.position().distanceTo(new Vec3(x, y, z)) < 0.6, 400);
        } catch (RuntimeException e) {
            reached = false;
        }
        context.waitTicks(100);
        connection.waitForClientboundPackets();
        context.waitTicks(10);
        cap.put("position_reached", reached);
        cap.put("client_eye_block", context.computeOnClient(c -> String.valueOf(
                BuiltInRegistries.BLOCK.getKey(c.level.getBlockState(BlockPos.containing(c.player.getEyePosition())).getBlock()))));
        cap.put("screenshot", String.valueOf(context.takeScreenshot(name)));
        return cap;
    }
}
