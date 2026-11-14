package dev.itzgamesgd.moreminecrafts.mob.skyflyer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Rendered-client capture of the Sky Spirit and Storm Hunter on a real dedicated server with a real survival player.
 * Active only with {@code -Dmoreminecrafts.skyflyer.capture=true}. It takes front, side and back screenshots of each
 * mob (static, no AI), then runs the genuine behaviour with the real player: spirit retaliation and chase end, and
 * hunter acquisition plus the full circle, shoot, dive, rise loop. Behaviour is asserted on the server; perceptual
 * quality of the screenshots is a human check.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SkyFlyerClientCaptureTest implements FabricClientGameTest {
    private static final int FLOOR_Y = 150;
    private static final double CAMERA_DISTANCE = 3.2D;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.skyflyer.capture")) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("view-distance", "6");
        properties.setProperty("simulation-distance", "6");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.getInput().pressKey(options -> options.keyToggleGui); // F1: hide the HUD for clean captures
            server.runOnServer(this::buildStage);
            context.waitTicks(20);

            UUID[] spirit = new UUID[1];
            UUID[] hunter = new UUID[1];
            server.runOnServer(s -> {
                spirit[0] = spawnStatic(s, dev.itzgamesgd.moreminecrafts.mob.skyflyer.SkyFlyers.SKY_SPIRIT, 0.5D, FLOOR_Y + 2.0D, 0.5D).getUUID();
                hunter[0] = spawnStatic(s, dev.itzgamesgd.moreminecrafts.mob.skyflyer.SkyFlyers.STORM_HUNTER, 20.5D, FLOOR_Y + 2.0D, 0.5D).getUUID();
            });
            context.waitTicks(20);
            connection.waitForClientboundPackets();
            context.runOnClient(c -> {
                boolean spiritRenderer = false, hunterRenderer = false;
                for (var e : c.level.entitiesForRendering()) {
                    var renderer = c.getEntityRenderDispatcher().getRenderer(e);
                    if (e instanceof SkySpirit && renderer.getClass().getName().endsWith("skyflyer.SkySpiritRenderer")) spiritRenderer = true;
                    if (e instanceof StormHunter && renderer.getClass().getName().endsWith("skyflyer.StormHunterRenderer")) hunterRenderer = true;
                }
                if (!spiritRenderer || !hunterRenderer) {
                    throw new AssertionError("dedicated renderers are not in use: spirit=" + spiritRenderer + " hunter=" + hunterRenderer);
                }
            });

            // --- static front / side / back captures
            for (var mob : List.of(new Object[] {"sky-spirit", spirit, 0.5D}, new Object[] {"storm-hunter", hunter, 20.5D})) {
                String name = (String) mob[0];
                double mx = (Double) mob[2];
                capture(context, server, connection, name + "-front", mx, 0.5D + CAMERA_DISTANCE, 180.0F, mx, 0.5D);
                capture(context, server, connection, name + "-side", mx + CAMERA_DISTANCE, 0.5D, 90.0F, mx, 0.5D);
                capture(context, server, connection, name + "-back", mx, 0.5D - CAMERA_DISTANCE, 0.0F, mx, 0.5D);
            }
            server.runOnServer(s -> {
                s.overworld().getEntity(spirit[0]).discard();
                s.overworld().getEntity(hunter[0]).discard();
            });

            // --- real-player behaviour: Sky Spirit retaliation, then end of chase
            UUID[] live = new UUID[1];
            server.runOnServer(s -> {
                ServerPlayer p = player(s);
                p.teleportTo(s.overworld(), 0.5D, FLOOR_Y + 1.0D, 10.5D, Set.of(), 180.0F, 0.0F, true);
                SkySpirit sp = (SkySpirit) SkyFlyers.SKY_SPIRIT.create(s.overworld(), EntitySpawnReason.COMMAND);
                sp.setPos(0.5D, FLOOR_Y + 2.5D, 0.5D);
                sp.setPersistenceRequired();
                s.overworld().addFreshEntity(sp);
                live[0] = sp.getUUID();
            });
            context.waitTicks(40);
            server.runOnServer(s -> {
                SkySpirit sp = (SkySpirit) s.overworld().getEntity(live[0]);
                if (sp.getTarget() != null) throw new AssertionError("spirit is neutral: no target before being hit");
                sp.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(player(s)), 1.0F);
            });
            boolean[] retaliated = {false};
            boolean[] sawWind = {false};
            for (int i = 0; i < 60; i++) {
                context.waitTicks(2);
                server.runOnServer(s -> {
                    SkySpirit sp = (SkySpirit) s.overworld().getEntity(live[0]);
                    if (sp.getTarget() == player(s)) retaliated[0] = true;
                    if (!s.overworld().getEntitiesOfClass(BreezeWindCharge.class, sp.getBoundingBox().inflate(30.0D), w -> w.getOwner() == sp).isEmpty()) sawWind[0] = true;
                });
                if (retaliated[0] && sawWind[0]) break;
            }
            if (!retaliated[0] || !sawWind[0]) throw new AssertionError("real-player retaliation failed: targeted=" + retaliated[0] + " wind=" + sawWind[0]);
            connection.waitForClientboundPackets();
            context.takeScreenshot("sky-spirit-retaliation");
            // Hide from sight and move away: the chase must end.
            server.runOnServer(s -> {
                ServerPlayer p = player(s);
                p.teleportTo(s.overworld(), 0.5D, FLOOR_Y + 1.0D, 0.5D + SkyFlyerTuning.SPIRIT_DISENGAGE_DISTANCE + 8.0D, Set.of(), 0.0F, 0.0F, true);
            });
            boolean ended = false;
            for (int i = 0; i < 60 && !ended; i++) {
                context.waitTicks(5);
                boolean[] holder = {false};
                server.runOnServer(s -> holder[0] = ((SkySpirit) s.overworld().getEntity(live[0])).getTarget() == null);
                ended = holder[0];
            }
            if (!ended) throw new AssertionError("spirit kept chasing a far attacker");
            server.runOnServer(s -> s.overworld().getEntity(live[0]).discard());

            // --- real-player behaviour: Storm Hunter acquires, loops, and dives
            server.runOnServer(s -> {
                ServerPlayer p = player(s);
                p.teleportTo(s.overworld(), 0.5D, FLOOR_Y + 1.0D, 0.5D, Set.of(), 180.0F, 0.0F, true);
                StormHunter h = (StormHunter) SkyFlyers.STORM_HUNTER.create(s.overworld(), EntitySpawnReason.COMMAND);
                h.setPos(0.5D, FLOOR_Y + 9.0D, 12.5D);
                h.setPersistenceRequired();
                s.overworld().addFreshEntity(h);
                live[0] = h.getUUID();
            });
            List<HunterPattern.Phase> seen = new ArrayList<>();
            boolean[] acquired = {false};
            boolean[] wind = {false};
            boolean[] hit = {false};
            boolean[] diveShot = {false};
            for (int i = 0; i < 400; i++) {
                context.waitTicks(3);
                HunterPattern.Phase[] phase = new HunterPattern.Phase[1];
                server.runOnServer(s -> {
                    ServerPlayer p = player(s);
                    p.setHealth(p.getMaxHealth());
                    StormHunter h = (StormHunter) s.overworld().getEntity(live[0]);
                    if (h.getTarget() == p) acquired[0] = true;
                    phase[0] = h.pattern().phase();
                    if (!s.overworld().getEntitiesOfClass(BreezeWindCharge.class, h.getBoundingBox().inflate(40.0D), w -> w.getOwner() == h).isEmpty()) wind[0] = true;
                    var src = p.getLastDamageSource();
                    if (src != null && src.getEntity() == h && src.getDirectEntity() == h) hit[0] = true;
                });
                if (seen.isEmpty() || seen.getLast() != phase[0]) seen.add(phase[0]);
                if (phase[0] == HunterPattern.Phase.DIVE && !diveShot[0]) {
                    connection.waitForClientboundPackets();
                    context.takeScreenshot("storm-hunter-dive");
                    diveShot[0] = true;
                }
                if (acquired[0] && wind[0] && hit[0] && diveShot[0] && seen.size() >= 5) break;
            }
            if (!acquired[0]) throw new AssertionError("hunter never targeted the real survival player on its own");
            if (!wind[0]) throw new AssertionError("hunter never fired a wind shot; phases " + seen);
            if (!hit[0]) throw new AssertionError("hunter dive never connected; phases " + seen);
            System.out.println("SKY_FLYER_CAPTURE_PASS phases=" + seen);
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
        if (p.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) p.setGameMode(GameType.SURVIVAL);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0D);
        return p;
    }

    private void buildStage(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (int x = -30; x <= 40; x++) for (int z = -45; z <= 30; z++) {
            level.setBlock(new BlockPos(x, FLOOR_Y, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 18);
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 24; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 18);
        }
        ServerPlayer p = player(server);
        p.setNoGravity(true);
        p.getAbilities().mayfly = true;
        p.onUpdateAbilities();
    }

    private static Mob spawnStatic(MinecraftServer server, EntityType<? extends Mob> type, double x, double y, double z) {
        Mob mob = type.create(server.overworld(), EntitySpawnReason.COMMAND);
        mob.setPos(x, y, z);
        mob.setYRot(0.0F);
        mob.yBodyRot = 0.0F;
        mob.yHeadRot = 0.0F;
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.setPersistenceRequired();
        server.overworld().addFreshEntity(mob);
        return mob;
    }

    /** Camera: mob centre is at y = FLOOR_Y + 2.0 + half height; the player's eye is placed level with the mob. */
    private void capture(ClientGameTestContext context, TestDedicatedServerContext server, TestDedicatedServerConnection connection,
                         String name, double cx, double cz, float yaw, double mx, double mz) {
        server.runOnServer(s -> {
            ServerPlayer p = player(s);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(s.overworld(), cx, FLOOR_Y + 2.0D + 0.25D - 1.62D + 0.0D, cz, Set.of(), yaw, 0.0F, true);
        });
        context.waitTicks(8);
        connection.waitForClientboundPackets();
        context.takeScreenshot(name);
    }
}
