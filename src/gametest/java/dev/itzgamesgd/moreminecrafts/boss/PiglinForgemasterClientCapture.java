package dev.itzgamesgd.moreminecrafts.boss;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Real-client frame sequences of the Lava Forgemaster performing each skill (opt-in via
 * {@code -Dmoreminecrafts.forgemaster.capture=true}). One screenshot every {@link #STEP} ticks per clip, named
 * {@code forgemaster-<clip>-NNN}; the capture script turns each clip into an mp4. Appearance/animation review only.
 */
public final class PiglinForgemasterClientCapture implements FabricClientGameTest {
    private static final int Y = 100;
    private static final int STEP = 2;
    private static final Vec3 BOSS = new Vec3(0.5, Y, 0.5);
    // Boss faces +Z; camera sits to its right-front for a three-quarter view that shows the axe arm and the sagittal swing.
    private static final Vec3 CAM = new Vec3(-5.2, Y + 2.3, 4.4);

    private record Clip(String name, PiglinForgemaster.Skill skill, int ticks) {}

    private static final List<Clip> CLIPS = List.of(
            new Clip("attack_overhead", PiglinForgemaster.Skill.ATTACK_OVERHEAD, 30),
            new Clip("attack_cleave", PiglinForgemaster.Skill.ATTACK_CLEAVE, 26),
            new Clip("skill_slash", PiglinForgemaster.Skill.SLASH, 56),
            new Clip("skill_quake", PiglinForgemaster.Skill.QUAKE, 74),
            new Clip("skill_furnace", PiglinForgemaster.Skill.FURNACE, 88),
            new Clip("overheat", PiglinForgemaster.Skill.OVERHEAT, 66),
            new Clip("stagger", PiglinForgemaster.Skill.STAGGER, 56),
            new Clip("leap_slam", PiglinForgemaster.Skill.LEAP, 54));

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.forgemaster.capture")) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "normal");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> buildRoom(s.overworld()));
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            connection.waitForClientboundPackets();
            context.waitTicks(40);

            shoot(context, connection, server, "idle", 60, s -> { });
            for (Clip clip : CLIPS) {
                resetBoss(server, connection, context);
                shoot(context, connection, server, clip.name(), clip.ticks(), s -> {
                    PiglinForgemaster boss = boss(s.overworld());
                    boss.force(clip.skill(), CAM);
                });
            }
            resetBoss(server, connection, context);
            shoot(context, connection, server, "walk", 60, s -> {
                PiglinForgemaster boss = boss(s.overworld());
                boss.getNavigation().moveTo(BOSS.x + 6.0, Y, BOSS.z + 4.0, 1.0);
            });
            resetBoss(server, connection, context);
            shoot(context, connection, server, "death", 60, s -> boss(s.overworld()).kill(s.overworld()));
            System.out.println("FORGEMASTER_CAPTURE_COMPLETE");
        }
    }

    private static void shoot(ClientGameTestContext context, TestDedicatedServerConnection connection,
                              TestDedicatedServerContext server, String clip, int ticks,
                              java.util.function.Consumer<net.minecraft.server.MinecraftServer> action) {
        server.runOnServer(action::accept);
        connection.waitForClientboundPackets();
        int index = 0;
        for (int t = 0; t < ticks; t += STEP) {
            context.runOnClient(c -> c.gui.toastManager().clear());
            context.takeScreenshot("forgemaster-%s-%03d".formatted(clip, index++));
            context.waitTicks(STEP);
        }
        System.out.println("FORGEMASTER_CLIP " + clip + " frames=" + index);
    }

    private static PiglinForgemaster boss(ServerLevel level) {
        return level.getEntitiesOfClass(PiglinForgemaster.class, new net.minecraft.world.phys.AABB(-20, Y - 5, -20, 20, Y + 20, 20)).getFirst();
    }

    private static void resetBoss(TestDedicatedServerContext server, TestDedicatedServerConnection connection, ClientGameTestContext context) {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            for (PiglinForgemaster old : level.getEntitiesOfClass(PiglinForgemaster.class, new net.minecraft.world.phys.AABB(-20, Y - 5, -20, 20, Y + 20, 20))) {
                old.discard();
            }
            spawnBoss(level);
        });
        connection.waitForClientboundPackets();
        context.waitTicks(20);
    }

    private static void spawnBoss(ServerLevel level) {
        PiglinForgemaster boss = PiglinForgemasterEntities.PIGLIN_FORGEMASTER.create(level, EntitySpawnReason.COMMAND);
        if (boss == null) throw new AssertionError("forgemaster entity could not be created");
        boss.snapTo(BOSS.x, BOSS.y, BOSS.z, 0.0F, 0.0F);
        boss.setScripted(true);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);
    }

    private static void buildRoom(ServerLevel level) {
        for (int x = -14; x <= 14; x++) {
            for (int z = -10; z <= 14; z++) {
                level.setBlockAndUpdate(new BlockPos(x, Y - 1, z), Blocks.DEEPSLATE_TILES.defaultBlockState());
                for (int y = Y; y <= Y + 9; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        spawnBoss(level);
        var player = level.getServer().getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SPECTATOR);
        Vec3 d = BOSS.add(0, 1.6, 0).subtract(CAM);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.hypot(d.x, d.z)));
        player.teleportTo(level, CAM.x, CAM.y, CAM.z, Set.of(), yaw, pitch, true);
    }
}
