package dev.itzgamesgd.moreminecrafts.heaven;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * P3.12 opt-in real-client rendering of NATURALLY generated sky islands ({@code -Dmoreminecrafts.heaven.p312render=true}): a real client joins a dedicated server
 * (seed 42, the production jar, no extra data pack) and looks at each listed natural site ({@code -Dmm.heaven.sitefile}, {@code -Dmm.heaven.p312.sites}) at noon and midnight.
 * Screenshots: heaven-p312-NN-<site>-<day|night>. Software renderer: appearance only, no performance measurement.
 */
public final class HeavenP312ClientRender implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        if (!Boolean.getBoolean("moreminecrafts.heaven.p312render")) return;
        int[] shot = {0};
        try {
            JsonObject sites = JsonParser.parseString(Files.readString(Path.of(System.getProperty("mm.heaven.sitefile")))).getAsJsonObject();
            String[] want = System.getProperty("mm.heaven.p312.sites", "meadow,meadow#2,meadow#3,mountain_meadow,mountain_meadow#4,forest,forest#1,forest#3").split(",");
            Properties props = new Properties();
            props.setProperty("online-mode", "false"); props.setProperty("view-distance", "10"); props.setProperty("simulation-distance", "6"); props.setProperty("max-tick-time", "-1");
            props.setProperty("spawn-animals", "false"); props.setProperty("spawn-monsters", "false");
            try (TestDedicatedServerContext server = ctx.worldBuilder().setUseConsistentSettings(false).adjustSettings(st -> st.setSeed("42")).createServer(props);
                 var connection = server.connect()) {
                ctx.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.renderDistance().set(12); });
                server.runOnServer(s -> { for (String c : new String[]{"gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear", "gamerule randomTickSpeed 0"}) cmd(s, c);
                    s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR); });
                for (String target : want) {
                    if (!sites.has(target)) { System.out.println("HEAVEN_P312_RENDER " + target + " site missing"); continue; }
                    String[] xz = sites.get(target).getAsString().split(","); int sx = Integer.parseInt(xz[0]), sz = Integer.parseInt(xz[1]); String base = target.split("#")[0];
                    server.runOnServer(s -> cmd(s, "time set noon"));
                    server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), sx + 40.5, 220, sz + 0.5, Set.of(), 0f, 0f, true));
                    ctx.waitTicks(100);
                    int[] b = server.computeOnServer(s -> box(s, base, sx, sz));
                    if (b == null) { System.out.println("HEAVEN_P312_RENDER " + target + " no start"); continue; }
                    BoundingBox bb = new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
                    double cx = (bb.minX() + bb.maxX()) / 2.0, cy = (bb.minY() + bb.maxY()) / 2.0, cz = (bb.minZ() + bb.maxZ()) / 2.0, d = Math.max(bb.getXSpan(), bb.getZSpan()) * 0.8 + 18;
                    for (String when : new String[]{"day", "night"}) {
                        server.runOnServer(s -> cmd(s, when.equals("day") ? "time set noon" : "time set midnight"));
                        look(ctx, server, new BlockPos((int) (cx - d * 0.5), (int) (cy + d * 0.45), (int) (cz - d * 0.9)), new BlockPos((int) cx, (int) (cy - bb.getYSpan() * 0.2), (int) cz));
                        ctx.runOnClient(c -> c.gui.toastManager().clear());
                        System.out.println("HEAVEN_P312_RENDER " + target + " box=" + bb.getXSpan() + "x" + bb.getYSpan() + "x" + bb.getZSpan() + " "
                                + ctx.takeScreenshot("heaven-p312-%02d-%s-%s".formatted(shot[0]++, target.replace('#', '_'), when)));
                    }
                }
            }
        } catch (Throwable t) { System.out.println("HEAVEN_P312_RENDER error " + t); t.printStackTrace(); }
    }

    private static int[] box(MinecraftServer s, String base, int sx, int sz) {
        var level = s.overworld(); var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE); Identifier sid = Identifier.parse("moreminecrafts:sky_island_" + base);
        int cx = sx >> 4, cz = sz >> 4; level.getChunk(cx, cz, ChunkStatus.FULL, true);
        for (var st : level.structureManager().startsForStructure(cx, cz, x -> registry.getKey(x) != null && registry.getKey(x).equals(sid))) {
            BoundingBox b = st.getBoundingBox();
            for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++) for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++) level.getChunk(x, z, ChunkStatus.FULL, true);
            return new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()};
        }
        return null;
    }

    private static void cmd(MinecraftServer s, String c) { s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c); }

    private static void look(ClientGameTestContext ctx, TestDedicatedServerContext server, BlockPos eye, BlockPos target) {
        double px = eye.getX() + 0.5, py = eye.getY() + 0.5, pz = eye.getZ() + 0.5, dx = target.getX() + 0.5 - px, dy = target.getY() + 0.5 - py, dz = target.getZ() + 0.5 - pz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), px, py, pz, Set.of(), yaw, pitch, true));
        ctx.waitFor(c -> c.level != null && c.level.hasChunkAt(target), 600);
        ctx.waitTicks(120);
    }
}
