package dev.itzgamesgd.moreminecrafts.boss;

import java.util.ArrayList;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Real-client screenshots of the Tomb Guardian appearance for review against the confirmed reference image (opt-in via
 * {@code -Dmoreminecrafts.tomb.capture=true}). A normal and an enraged guardian stand in a lit sandstone room; each
 * attack animation is triggered through the same entity event the server sends and photographed at its key pose, and
 * the red spike floor telegraph is shot mid-charge. Test-only room; no gameplay numbers are exercised.
 */
public final class TombGuardianClientCapture implements FabricClientGameTest {
    private static final int Y = 100;
    private static final double NORMAL_X = 0.5;
    private static final double ENRAGED_X = 12.5;
    private static final double SPIKE_X = -12.5;

    /** One screenshot: camera, which guardian, its body yaw, and an optional animation event with a tick delay. */
    private record Shot(String name, double camX, double camY, double camZ, float yaw, float pitch,
                        double guardianX, float guardianYaw, int event, int ticksAfterEvent) {}

    private static Shot still(String name, double gx, double dist, double height, float camYaw, float pitch, float gYaw) {
        return new Shot(name, gx, Y + height, dist + 0.5, camYaw, pitch, gx, gYaw, -1, 0);
    }

    private static Shot anim(String name, double gx, int event, int ticks) {
        // Camera 4.5 blocks to the side and 9 in front, looking back at the guardian (yaw 153 faces it).
        return new Shot(name, gx + 4.5, Y + 2.4, 9.5, 153, 8, gx, 25, event, ticks);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.tomb.capture")) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "easy");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> buildRoomAndGuardians(s.overworld()));
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            context.waitTicks(40); // enrage tick on the second guardian
            // Freeze the appearance guardians so look-at AI cannot turn them between the pose and the shot; the spike
            // guardian keeps its AI because it runs the real attack for the telegraph shots.
            server.runOnServer(s -> {
                for (var g : s.overworld().getEntities(TombGuardianEntities.TOMB_GUARDIAN, e -> true))
                    if (Math.abs(g.getX() - SPIKE_X) > 0.1) g.setNoAi(true);
            });

            List<Shot> shots = new ArrayList<>();
            // Appearance, normal state: front, three-quarter, side, back, and a close-up of head and chest.
            shots.add(still("normal-front", NORMAL_X, 7.0, 2.2, 180, 4, 0));
            shots.add(new Shot("normal-hero-low-angle", NORMAL_X + 2.5, Y + 0.9, 7.5, 160, -6, NORMAL_X, -20, -1, 0));
            shots.add(still("normal-three-quarter", NORMAL_X, 7.0, 2.2, 180, 4, 38));
            shots.add(still("normal-side", NORMAL_X, 7.0, 2.2, 180, 4, 90));
            shots.add(still("normal-back", NORMAL_X, 7.0, 2.2, 180, 4, 180));
            shots.add(still("normal-closeup", NORMAL_X, 4.6, 3.1, 180, 4, 0));
            // Enraged: cracks widened and the chest core overheated to red gold.
            shots.add(still("enraged-front", ENRAGED_X, 7.0, 2.2, 180, 4, 0));
            shots.add(new Shot("enraged-hero-low-angle", ENRAGED_X + 2.5, Y + 0.9, 7.5, 160, -6, ENRAGED_X, -20, -1, 0));
            shots.add(still("enraged-closeup", ENRAGED_X, 4.6, 3.1, 180, 4, 0));
            // Attack animations at their key poses (the tick counts follow each animation's keyframes).
            shots.add(anim("anim-sweep-cocked", NORMAL_X, 70, 11));
            shots.add(anim("anim-sweep-strike", NORMAL_X, 70, 15));
            shots.add(anim("anim-slam-raised", NORMAL_X, 71, 20));
            shots.add(anim("anim-slam-impact", NORMAL_X, 71, 27));
            shots.add(anim("anim-spikes-stomp-lift", NORMAL_X, 72, 10));
            shots.add(anim("anim-summon-roar", NORMAL_X, 75, 26));
            shots.add(anim("anim-ultimate-raised", NORMAL_X, 76, 34));
            shots.add(anim("anim-ultimate-strike", NORMAL_X, 76, 42));

            int index = 0;
            for (Shot shot : shots) {
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    for (var g : level.getEntities(TombGuardianEntities.TOMB_GUARDIAN, e -> true)) {
                        float yaw = Math.abs(g.getX() - shot.guardianX()) < 0.1 ? shot.guardianYaw() : 0;
                        g.setYRot(yaw);
                        g.setYBodyRot(yaw);
                        g.setYHeadRot(yaw);
                    }
                    s.getPlayerList().getPlayers().getFirst().teleportTo(level, shot.camX(), shot.camY(), shot.camZ(),
                            Set.of(), shot.yaw(), shot.pitch(), true);
                });
                connection.waitForClientboundPackets();
                // The client eases the body yaw toward its target, so a big turn (e.g. front to back) needs time.
                context.waitTicks(shot.event() >= 0 ? 12 : 45);
                if (shot.event() >= 0) {
                    server.runOnServer(s -> {
                        ServerLevel level = s.overworld();
                        for (var g : level.getEntities(TombGuardianEntities.TOMB_GUARDIAN, e -> true))
                            if (Math.abs(g.getX() - shot.guardianX()) < 0.1) level.broadcastEntityEvent(g, (byte) shot.event());
                    });
                    context.waitTicks(shot.ticksAfterEvent());
                }
                context.runOnClient(c -> c.gui.toastManager().clear());
                System.out.println("TOMB_GUARDIAN_SCREENSHOT " + context.takeScreenshot(
                        "tomb-guardian-%02d-%s".formatted(index++, shot.name())));
            }

            // Floor telegraphs, each from the real attack run against a villager: gold circle for spikes, red area plus
            // charge gauge for the direct attacks (sweep fan, ground slam zone, ultimate zone). Clearing the target
            // right after the start keeps the AI from chaining other patterns while the shot is taken.
            record Tell(String name, TombGuardianCombat.Attack attack, int ticks, double camY, double camZ, float pitch) {}
            List<Tell> tells = List.of(
                    new Tell("telegraph-spikes-gold-circle", TombGuardianCombat.Attack.SPIKES, 10, Y + 8.0, 8.0, 52),
                    new Tell("telegraph-sweep-red-fan", TombGuardianCombat.Attack.SWEEP, 11, Y + 8.5, 8.5, 60),
                    new Tell("telegraph-slam-red-zone", TombGuardianCombat.Attack.SLAM, 15, Y + 9.0, 14.0, 40),
                    new Tell("telegraph-ultimate-red-zone", TombGuardianCombat.Attack.ULTIMATE, 24, Y + 9.0, 14.0, 40));
            for (Tell tell : tells) {
                server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(s.overworld(), SPIKE_X + 0.5,
                        tell.camY(), tell.camZ(), Set.of(), 180, tell.pitch(), true));
                connection.waitForClientboundPackets();
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    TombGuardian guardian = level.getEntities(TombGuardianEntities.TOMB_GUARDIAN,
                            e -> Math.abs(e.getX() - SPIKE_X) < 0.1).getFirst();
                    Villager villager = level.getEntities(EntityTypes.VILLAGER, e -> true).getFirst();
                    guardian.forceAttack(level, tell.attack(), villager);
                    guardian.setTarget(null);
                });
                context.waitTicks(tell.ticks());
                context.runOnClient(c -> c.gui.toastManager().clear());
                System.out.println("TOMB_GUARDIAN_SCREENSHOT " + context.takeScreenshot(
                        "tomb-guardian-%02d-%s".formatted(index++, tell.name())));
                context.waitTicks(130); // let the attack and its displays finish before the next one
            }
            System.out.println("TOMB_GUARDIAN_CAPTURE_COMPLETE " + index);
        }
    }

    private static void buildRoomAndGuardians(ServerLevel level) {
        for (int x = -22; x <= 22; x++) {
            for (int z = -10; z <= 20; z++) {
                level.setBlock(new BlockPos(x, Y - 1, z), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, Y + 10, z), Blocks.GLOWSTONE.defaultBlockState(), 3);
                for (int y = Y; y < Y + 10; y++) {
                    level.setBlock(new BlockPos(x, y, z), z == -10
                            ? Blocks.CUT_SANDSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        var player = level.getServer().getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SPECTATOR);
        spawn(level, NORMAL_X, false);
        spawn(level, ENRAGED_X, true);
        spawn(level, SPIKE_X, false);
        Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        if (villager == null) throw new AssertionError("Could not create the telegraph target");
        villager.setNoAi(true);
        villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0D);
        villager.setHealth(1000.0F);
        villager.setPersistenceRequired();
        villager.snapTo(SPIKE_X, Y, 5.5, 0, 0);
        level.addFreshEntity(villager);
    }

    private static void spawn(ServerLevel level, double x, boolean enraged) {
        TombGuardian guardian = TombGuardianEntities.TOMB_GUARDIAN.create(level, EntitySpawnReason.COMMAND);
        if (guardian == null) throw new AssertionError("Could not create the Tomb Guardian");
        guardian.setPersistenceRequired();
        guardian.snapTo(x, Y, 0.5, 0, 0);
        if (enraged) guardian.setHealth(guardian.getMaxHealth() * 0.4F);
        level.addFreshEntity(guardian);
    }
}
