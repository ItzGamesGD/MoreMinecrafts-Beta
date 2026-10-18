package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/** A filtered two-process clean save/restart check for C1 block-container evidence. */
public final class QuestLootRestartGameTests implements CustomTestMethodInvoker {
    private static final UUID PLAYER_ID = UUID.fromString("92d47785-6563-40d8-ad5b-0d3765b20b43");
    private static final UUID CART_ID = UUID.fromString("92d47785-6563-40d8-ad5b-0d3765b20b44");
    private static final ChunkPos CHUNK = new ChunkPos(220, 220);
    private static final BlockPos CHEST = new BlockPos((CHUNK.x() << 4) + 4, 100, (CHUNK.z() << 4) + 4);
    private static final BlockPos CART = CHEST.offset(2, 0, 0);
    private static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.parse("minecraft:chests/simple_dungeon"));
    private static final ResourceKey<LootTable> CART_TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.parse("minecraft:chests/abandoned_mineshaft"));

    private static ServerPlayer player(GameTestHelper ctx, boolean loadFromDisk) {
        ServerLevel level = ctx.getLevel();
        GameProfile profile = new GameProfile(PLAYER_ID, "xpq-loot-restart");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        if (loadFromDisk) {
            var saved = level.getServer().getPlayerList().loadPlayerData(player.nameAndId());
            ctx.assertTrue(saved.isPresent(), "fixed player data was saved by the first JVM");
            player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), saved.orElseThrow()));
        }
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    @GameTest(maxTicks = 400)
    public void structureLootPersistsAcrossCleanJvms(GameTestHelper ctx) {
        String phase = System.getProperty("moreminecrafts.questloot.restart.phase");
        if (phase == null) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, CHUNK, 0);
        level.getChunk(CHUNK.x(), CHUNK.z());
        if (phase.equals("write")) {
            level.setBlock(CHEST, Blocks.CHEST.defaultBlockState(), 3);
            var chest = (RandomizableContainerBlockEntity) level.getBlockEntity(CHEST);
            ctx.assertTrue(chest != null, "chest exists in fixed chunk");
            chest.setLootTable(TABLE);
            ServerPlayer opener = player(ctx, false);
            chest.unpackLootTable(opener);
            ctx.assertTrue(QuestRules.distinctCount(KnowledgeRuntime.state(opener), "set.structure_loot_any") == 1,
                    "real loot generation recorded for fixed player");
            var cart = EntityTypes.CHEST_MINECART.create(level, EntitySpawnReason.COMMAND);
            ctx.assertTrue(cart instanceof ContainerEntity, "fixed chest minecart was created");
            cart.setUUID(CART_ID);
            cart.setPos(CART.getX() + 0.5D, CART.getY(), CART.getZ() + 0.5D);
            ctx.assertTrue(level.addFreshEntity(cart), "fixed chest minecart was spawned");
            ContainerEntity cartContainer = (ContainerEntity) cart;
            cartContainer.setContainerLootTable(CART_TABLE);
            cartContainer.unpackChestVehicleLootTable(opener);
            ctx.assertTrue(QuestRules.distinctCount(KnowledgeRuntime.state(opener), "set.structure_loot_mineshaft") == 1,
                    "entity container records its own classified evidence");
            level.getServer().getPlayerList().saveAll();
            level.getChunkSource().save(true);
            System.out.println("QUEST_LOOT_RESTART_WRITE chunk=" + CHUNK + " player=" + PLAYER_ID);
            ctx.succeed();
            return;
        }
        if (phase.equals("read")) {
            ServerPlayer opener = player(ctx, true);
            ctx.assertTrue(QuestRules.distinctCount(KnowledgeRuntime.state(opener), "set.structure_loot_any") == 1
                    && QuestRules.distinctCount(KnowledgeRuntime.state(opener), "set.structure_loot_mineshaft") == 1,
                    "block and entity loot evidence restored from disk");
            var chest = (RandomizableContainerBlockEntity) level.getBlockEntity(CHEST);
            ctx.assertTrue(chest != null && chest.getLootTable() == null,
                    "container has no loot table after generation and restart");
            var carts = level.getEntitiesOfClass(Entity.class, new AABB(CART).inflate(3),
                    entity -> CART_ID.equals(entity.getUUID()));
            ctx.assertTrue(carts.size() == 1 && carts.getFirst() instanceof ContainerEntity,
                    "same loot-bearing chest minecart restored after restart");
            ContainerEntity cartContainer = (ContainerEntity) carts.getFirst();
            ServerPlayer other = ctx.makeMockServerPlayerInLevel();
            chest.unpackLootTable(other);
            cartContainer.unpackChestVehicleLootTable(other);
            ctx.assertTrue(QuestRules.distinctCount(KnowledgeRuntime.state(other), "set.structure_loot_any") == 0,
                    "another player gains no credit from reopening after restart");
            System.out.println("QUEST_LOOT_RESTART_READ chunk=" + CHUNK + " player=" + PLAYER_ID);
            ctx.succeed();
            return;
        }
        throw new IllegalArgumentException("Unknown questloot restart phase: " + phase);
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
