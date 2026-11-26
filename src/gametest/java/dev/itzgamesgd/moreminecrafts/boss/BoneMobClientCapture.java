package dev.itzgamesgd.moreminecrafts.boss;

import dev.itzgamesgd.moreminecrafts.mob.bone.BoneMobs;
import dev.itzgamesgd.moreminecrafts.mob.bone.BoneSkillMonster;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Real-client frame sequences of the Deep Sea Poseidon (in a water tank), Sky Wyvern and Floating Jellyfish (in the air)
 * for animation review. Opt-in: {@code -Dmoreminecrafts.bonemob.capture=true}. Frames are named
 * {@code bonemob-<mob>-<clip>-NNN}, one every 2 ticks.
 */
public final class BoneMobClientCapture implements FabricClientGameTest {
    private static final int Y = 100;
    private static final int STEP = 2;

    private record Clip(String name, int ticks, BiConsumer<ServerLevel, BoneSkillMonster> action) {}

    private record Scene(String id, EntityType<? extends BoneSkillMonster> type, Vec3 at, Vec3 cam, double lookY, boolean water, List<Clip> clips) {}

    private static Clip skill(String name, int ticks, Vec3 toward) {
        return new Clip(name, ticks, (l, m) -> m.forceSkill(name, toward));
    }

    private static List<Scene> scenes() {
        Vec3 p = new Vec3(0.5, Y + 3.0, 0.5);
        Vec3 w = new Vec3(60.5, Y + 4.0, 0.5);
        Vec3 j = new Vec3(120.5, Y + 1.5, 0.5);
        Vec3 pCam = p.add(-4.6, 1.2, 4.2), wCam = w.add(-5.0, 0.8, 4.6), jCam = j.add(-3.0, 0.6, 2.8);
        return List.of(
                new Scene("deep_sea_poseidon", BoneMobs.DEEP_SEA_POSEIDON, p, pCam, 1.4, true, List.of(
                        new Clip("idle", 80, (l, m) -> { }),
                        new Clip("swim", 70, (l, m) -> m.forceMove(new Vec3(0.12, 0, -0.06), 70)),
                        skill("attack_thrust", 26, pCam),
                        skill("attack_swing", 30, pCam),
                        skill("skill_pull", 66, pCam),
                        skill("skill_charge", 58, p.add(10, 0, -2)),
                        skill("skill_throw", 62, p.add(10, 0, 3)),
                        new Clip("death", 56, (l, m) -> m.kill(l)))),
                new Scene("sky_wyvern", BoneMobs.SKY_WYVERN, w, wCam, 0.7, false, List.of(
                        new Clip("fly", 50, (l, m) -> { }),
                        skill("dive", 40, w.add(6, -4, 2)),
                        skill("breath", 56, wCam),
                        skill("wing_buffet", 26, wCam),
                        new Clip("hurt", 20, (l, m) -> m.hurtServer(l, l.damageSources().generic(), 1.0F)),
                        new Clip("death", 46, (l, m) -> m.kill(l)))),
                new Scene("floating_jellyfish", BoneMobs.FLOATING_JELLYFISH, j, jCam, 0.7, false, List.of(
                        new Clip("idle", 80, (l, m) -> { }),
                        new Clip("move", 80, (l, m) -> m.forceMove(new Vec3(0.03, 0, -0.015), 80)),
                        skill("sting", 26, jCam),
                        new Clip("death", 36, (l, m) -> m.kill(l)))));
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.bonemob.capture")) return;
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        properties.setProperty("difficulty", "normal");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            context.runOnClient(c -> { if (!c.gui.hud.isHidden()) c.gui.hud.toggle(); });
            String only = System.getProperty("moreminecrafts.bonemob.only", "");
            for (Scene scene : scenes()) {
                if (!only.isEmpty() && !only.contains(scene.id())) continue;
                server.runOnServer(s -> {
                    ServerLevel level = s.overworld();
                    buildStage(level, scene);
                    var player = s.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SPECTATOR);
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0, false, false));
                    // cam is the eye position; teleportTo takes the feet (eye height 1.62).
                    Vec3 d = scene.at().add(0, scene.lookY(), 0).subtract(scene.cam());
                    float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
                    float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.hypot(d.x, d.z)));
                    player.teleportTo(level, scene.cam().x, scene.cam().y - 1.62, scene.cam().z, Set.of(), yaw, pitch, true);
                });
                connection.waitForClientboundPackets();
                context.waitTicks(60);
                for (Clip clip : scene.clips()) {
                    respawn(server, scene);
                    connection.waitForClientboundPackets();
                    context.waitTicks(20);
                    server.runOnServer(s -> {
                        BoneSkillMonster mob = mob(s.overworld(), scene);
                        clip.action().accept(s.overworld(), mob);
                    });
                    connection.waitForClientboundPackets();
                    int index = 0;
                    for (int t = 0; t < clip.ticks(); t += STEP) {
                        context.runOnClient(c -> c.gui.toastManager().clear());
                        context.takeScreenshot("bonemob-%s-%s-%03d".formatted(scene.id(), clip.name(), index++));
                        context.waitTicks(STEP);
                    }
                    System.out.println("BONEMOB_CLIP " + scene.id() + " " + clip.name() + " frames=" + index);
                }
            }
            System.out.println("BONEMOB_CAPTURE_COMPLETE");
        }
    }

    private static BoneSkillMonster mob(ServerLevel level, Scene scene) {
        return level.getEntitiesOfClass(BoneSkillMonster.class, AABB.ofSize(scene.at(), 40, 30, 40)).getFirst();
    }

    private static void respawn(TestDedicatedServerContext server, Scene scene) {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            for (BoneSkillMonster old : level.getEntitiesOfClass(BoneSkillMonster.class, AABB.ofSize(scene.at(), 40, 30, 40))) old.discard();
            for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, AABB.ofSize(scene.at(), 60, 40, 60))) e.discard();
            BoneSkillMonster mob = scene.type().create(level, EntitySpawnReason.COMMAND);
            if (mob == null) throw new AssertionError("could not create " + scene.id());
            mob.snapTo(scene.at().x, scene.at().y, scene.at().z, 0.0F, 0.0F);
            mob.setScripted(true);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        });
    }

    private static void buildStage(ServerLevel level, Scene scene) {
        int cx = (int) Math.floor(scene.at().x), cz = (int) Math.floor(scene.at().z);
        for (int x = cx - 14; x <= cx + 14; x++) {
            for (int z = cz - 12; z <= cz + 12; z++) {
                level.setBlockAndUpdate(new BlockPos(x, Y - 1, z), scene.water() ? Blocks.PRISMARINE_BRICKS.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
                boolean wall = x == cx - 14 || x == cx + 14 || z == cz - 12 || z == cz + 12;
                for (int y = Y; y <= Y + 10; y++) {
                    var state = scene.water()
                            ? (wall ? Blocks.GLASS.defaultBlockState() : y <= Y + 8 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState())
                            : Blocks.AIR.defaultBlockState();
                    level.setBlockAndUpdate(new BlockPos(x, y, z), state);
                }
                if (scene.water() && (x + z) % 7 == 0) level.setBlockAndUpdate(new BlockPos(x, Y - 1, z), Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
    }
}
