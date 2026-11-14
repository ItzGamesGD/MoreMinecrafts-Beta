package dev.itzgamesgd.moreminecrafts.mob.pyramid;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Native client captures of the pyramid variant textures, in a test-only lit room. */
public final class PyramidMobClientCapture implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("moreminecrafts.pyramid.mob.capture")) return;
        var types = List.of(PyramidMobs.BURIED_MUMMY, PyramidMobs.CRYPT_ARCHER,
                PyramidMobs.FUNERAL_SERVANT, PyramidMobs.GILDED_MUMMY);
        var names = List.of("buried-mummy", "crypt-archer", "funeral-servant", "gilded-mummy");
        Properties properties = new Properties();
        properties.setProperty("online-mode", "false");
        try (TestDedicatedServerContext server = context.worldBuilder().createServer(properties);
             TestDedicatedServerConnection connection = server.connect()) {
            context.waitFor(c -> c.player != null && c.level != null);
            server.runOnServer(s -> {
                var level = s.overworld();
                var player = s.getPlayerList().getPlayers().getFirst();
                for (int x = -4; x <= 19; x++) for (int z = -3; z <= 8; z++) {
                    level.setBlock(new BlockPos(x, 99, z), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(x, 105, z), Blocks.GLOWSTONE.defaultBlockState(), 3);
                    for (int y = 100; y < 105; y++) {
                        level.setBlock(new BlockPos(x, y, z), z == -3 ? Blocks.CUT_SANDSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                player.setGameMode(GameType.SPECTATOR);
                for (int i = 0; i < types.size(); i++) {
                    Mob mob = types.get(i).create(level, EntitySpawnReason.COMMAND);
                    if (mob == null) throw new AssertionError("Could not create " + names.get(i));
                    mob.setNoAi(true);
                    mob.snapTo(i * 5 + 0.5, 100, 0.5, 0, 0);
                    if (i == 1) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    if (!level.addFreshEntity(mob)) throw new AssertionError("Could not add " + names.get(i));
                }
            });
            for (int i = 0; i < types.size(); i++) {
                final int index = i;
                server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().teleportTo(
                        s.overworld(), index * 5 + 0.5, 100, 4.5, Set.of(), 180, 0, true));
                connection.waitForClientboundPackets();
                context.waitTicks(40);
                EntityType<? extends Mob> expected = types.get(i);
                context.runOnClient(c -> {
                    boolean found = false;
                    for (var entity : c.level.entitiesForRendering()) if (entity.getType() == expected) found = true;
                    if (!found) throw new AssertionError("Client did not receive " + names.get(index));
                });
                System.out.println("PYRAMID_MOB_SCREENSHOT " + context.takeScreenshot("pyramid-variant-" + names.get(i)));
                System.out.println("PYRAMID_MOB_CAPTURE " + names.get(i));
            }
            System.out.println("PYRAMID_MOB_CAPTURE_COMPLETE");
        }
    }
}
