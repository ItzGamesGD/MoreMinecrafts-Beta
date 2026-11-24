package dev.itzgamesgd.moreminecrafts.heaven;

import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** Opt-in real-client forest comparison ({@code -Dmoreminecrafts.heaven.p37capture=true}): 4 variants x medium / large island, overview + eye level. */
public final class HeavenP37ClientCapture implements FabricClientGameTest {
    private static final String[] V = HeavenP37GameTests.VARIANTS;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.heaven.p37capture")) return;
        int[] shot = {0};
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
            TestServerContext server = world.getServer();
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.renderDistance().set(12); });
            server.runOnServer(s -> {
                for (String c : new String[]{"gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear", "time set noon", "gamerule doMobSpawning false"}) cmd(s, c);
                s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
                ServerLevel l = s.overworld();
                for (int i = 0; i < 4; i++) {
                    place(l, "plain_medium_d", V[i], medium(i));
                    place(l, "large_a", V[i], large(i));
                }
            });
            context.waitTicks(80);
            for (int i = 0; i < 4; i++) {
                BlockPos m = medium(i);
                look(context, server, m.offset(-16, 30, -26), m.offset(22, 14, 18), 100);
                snap(context, shot, V[i] + "-medium-overview");
                BlockPos eye = server.computeOnServer(s -> ground(s.overworld(), m.offset(22, 0, 18)));
                look(context, server, eye, eye.offset(8, 0, 6), 100);
                snap(context, shot, V[i] + "-medium-eye-level");
                look(context, server, eye, eye.offset(-8, 0, 8), 60);
                snap(context, shot, V[i] + "-medium-eye-level-2");
                BlockPos g = large(i);
                look(context, server, g.offset(-14, 62, -44), g.offset(48, 20, 48), 420);
                snap(context, shot, V[i] + "-large-overview");
                BlockPos le = server.computeOnServer(s -> ground(s.overworld(), g.offset(48, 0, 48)));
                look(context, server, le, le.offset(10, 0, 4), 200);
                snap(context, shot, V[i] + "-large-eye-level");
            }
            System.out.println("HEAVEN_P37_CAPTURE_COMPLETE " + shot[0]);
        }
    }

    private static BlockPos medium(int i) { return new BlockPos(i * 70, 200, 0); }
    private static BlockPos large(int i) { return new BlockPos(i * 140, 190, 200); }

    private static void place(ServerLevel l, String name, String v, BlockPos at) {
        var t = l.getStructureTemplateManager().get(Identifier.parse("moreminecrafts-gametest:heaven_forest_cmp/" + v + "/" + name)).orElseThrow();
        if (!t.placeInWorld(l, at, at, new StructurePlaceSettings(), l.getRandom(), Block.UPDATE_CLIENTS)) throw new AssertionError(name);
    }

    /** Standing position (eye) on the highest solid block of the column, 2 above its top. */
    private static BlockPos ground(ServerLevel l, BlockPos col) {
        for (int y = col.getY() + 40; y > col.getY() - 40; y--)
            if (!l.getBlockState(new BlockPos(col.getX(), y, col.getZ())).isAir()) return new BlockPos(col.getX(), y + 2, col.getZ());
        return col;
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
        System.out.println("HEAVEN_P37_CAPTURE_SCREENSHOT " + context.takeScreenshot("heaven-p37-%02d-%s".formatted(shot[0]++, name)));
    }
}
