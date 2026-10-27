package dev.itzgamesgd.moreminecrafts.progression;

import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Checks the two vanilla 26.3 behaviours that the restorable wither-skull protocol (docs/audits/wave13b) depends on. These are
 * assumptions about Minecraft, not tests of a skull feature: nothing here registers or implements the XP quest.
 */
public final class SkullProtocolAssumptionGameTests {
    /** A recovery that re-creates a removed skull entity with its original UUID is idempotent: vanilla refuses a second entity with that UUID. */
    @GameTest
    public void secondEntityWithTheSameUuidIsRefused(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        Vec3 at = context.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        UUID uuid = UUID.nameUUIDFromBytes("wave13b-skull-restore".getBytes());
        ItemEntity first = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.WITHER_SKELETON_SKULL));
        first.setUUID(uuid);
        context.assertTrue(level.addFreshEntity(first), "first entity with the UUID is added");
        ItemEntity second = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.WITHER_SKELETON_SKULL));
        second.setUUID(uuid);
        context.assertTrue(!level.addFreshEntity(second), "vanilla must refuse a second entity with the same UUID");
        context.assertTrue(level.getEntity(uuid) == first, "the original entity remains the one registered under the UUID");
        int count = level.getEntitiesOfClass(ItemEntity.class, first.getBoundingBox().inflate(4.0), e -> e.getUUID().equals(uuid)).size();
        context.assertTrue(count == 1, "exactly one entity carries the UUID, found " + count);
        first.discard();
        context.succeed();
    }

    /** Recovery can ask whether a chunk's entities have been loaded before deciding that a skull entity is absent. */
    @GameTest
    public void entityLoadStatusIsQueryablePerChunk(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos pos = context.absolutePos(new BlockPos(1, 2, 1));
        context.assertTrue(level.areEntitiesLoaded(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4)), "entities of the test chunk are loaded");
        context.assertTrue(!level.areEntitiesLoaded(ChunkPos.pack(1_000_000, 1_000_000)), "entities of a never-loaded chunk are not loaded");
        context.succeed();
    }
}
