package dev.itzgamesgd.moreminecrafts.heaven;

import java.util.Map;
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

/**
 * Opt-in real-client check of the production T1 flower textures ({@code -Dmoreminecrafts.heaven.p36capture=true}):
 * every flower family close up, the P3 medium and large islands from mid and far range, at noon and midnight.
 * Software renderer: not a GPU measurement.
 */
public final class HeavenP36ClientCapture implements FabricClientGameTest {
    private static final BlockPos ROW = new BlockPos(0, 200, 0), MED = new BlockPos(0, 200, 60), LARGE = new BlockPos(80, 190, 40);
    private static final String[] PLANTS = {"giant_starlotus", "giant_lily", "giant_daffodil", "medium_lavender_0", "medium_hydrangea_0", "medium_phlox_0",
            "tree_blossom", "tree_white"};
    private static final Map<String, BlockPos> PIVOT = Map.of("giant_starlotus", new BlockPos(3, 1, 3), "giant_lily", new BlockPos(3, 1, 2),
            "giant_daffodil", new BlockPos(4, 1, 4), "medium_lavender_0", new BlockPos(1, 0, 1), "medium_hydrangea_0", new BlockPos(1, 0, 1),
            "medium_phlox_0", new BlockPos(1, 0, 1), "tree_blossom", new BlockPos(3, 1, 2), "tree_white", new BlockPos(3, 1, 4));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.heaven.p36capture")) return;
        int[] shot = {0};
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
            TestServerContext server = world.getServer();
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); c.options.renderDistance().set(12); });
            server.runOnServer(s -> {
                for (String c : new String[]{"gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear", "time set noon", "gamerule doMobSpawning false"}) cmd(s, c);
                s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SPECTATOR);
                ServerLevel l = s.overworld();
                for (int x = -6; x < PLANTS.length * 14; x++) for (int z = -8; z <= 8; z++) {
                    l.setBlock(ROW.offset(x, -2, z), HeavenPrototypeBlocks.HEAVEN_LOAM.defaultBlockState(), Block.UPDATE_CLIENTS);
                    l.setBlock(ROW.offset(x, -1, z), HeavenPrototypeBlocks.HEAVEN_GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                for (int i = 0; i < PLANTS.length; i++) {
                    BlockPos root = ROW.offset(i * 14, 0, 0), o = root.subtract(PIVOT.get(PLANTS[i]));
                    tpl(l, "moreminecrafts:heaven_flora/" + PLANTS[i]).placeInWorld(l, o, o, new StructurePlaceSettings(), l.getRandom(), Block.UPDATE_CLIENTS);
                }
                tpl(l, "moreminecrafts:heaven_p3/meadow/plain_medium_a").placeInWorld(l, MED, MED, new StructurePlaceSettings(), l.getRandom(), Block.UPDATE_CLIENTS);
                tpl(l, "moreminecrafts:heaven_p3/meadow/large_a").placeInWorld(l, LARGE, LARGE, new StructurePlaceSettings(), l.getRandom(), Block.UPDATE_CLIENTS);
            });
            context.waitTicks(60);
            for (String time : new String[]{"noon", "midnight"}) {
                server.runOnServer(s -> cmd(s, "time set " + time));
                for (int i = 0; i < PLANTS.length; i++) {
                    BlockPos root = ROW.offset(i * 14, 0, 0);
                    int h = PLANTS[i].startsWith("medium") ? 1 : 5;
                    look(context, server, root.offset(-4, h + 2, -7), root.above(h), 50);
                    snap(context, shot, time + "-near-" + PLANTS[i]);
                }
                look(context, server, MED.offset(14, 20, -6), MED.offset(22, 14, 17), 60);
                snap(context, shot, time + "-near-island-ground");
                look(context, server, MED.offset(-20, 34, -30), MED.offset(22, 12, 17), 80);
                snap(context, shot, time + "-mid-island");
                look(context, server, LARGE.offset(-60, 80, -70), LARGE.offset(48, 28, 48), 120);
                snap(context, shot, time + "-far-large-island");
            }
            System.out.println("HEAVEN_P36_CAPTURE_COMPLETE " + shot[0]);
        }
    }

    private static net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate tpl(ServerLevel l, String id) {
        return l.getStructureTemplateManager().get(Identifier.parse(id)).orElseThrow(() -> new AssertionError("missing " + id));
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
        System.out.println("HEAVEN_P36_CAPTURE_SCREENSHOT " + context.takeScreenshot("heaven-p36-%02d-%s".formatted(shot[0]++, name)));
    }
}
