package dev.itzgamesgd.moreminecrafts.mob.sky;

import dev.itzgamesgd.moreminecrafts.skytower.SkyTowerTrialSpawners;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/** SKY-MOB §15.6: spawn placements (N1/N4/N5/P3/Q33) and a real vanilla Trial Spawner running an approved combo. */
public final class SkyMobSpawnGameTests {
    private static final List<EntityType<? extends Mob>> DARK = List.of(SkyNormalMobs.CLOUD_WRAITH,
            SkyNormalMobs.SKYGUARD_ARCHER, SkyNormalMobs.FALLEN_SENTINEL);
    private static final List<EntityType<? extends Mob>> ANY_LIGHT = List.of(SkyNormalMobs.CRYSTAL_BUG,
            SkyNormalMobs.CLOUD_SLIME);

    private static boolean rule(GameTestHelper ctx, EntityType<? extends Mob> type, EntitySpawnReason reason, BlockPos pos) {
        return SpawnPlacements.checkSpawnRules(type, ctx.getLevel(), reason, pos, ctx.getLevel().getRandom());
    }

    /** Stone box with a 1x2 interior at local (2,2,2); optional glowstone ceiling light inside the shell. */
    private static BlockPos shell(GameTestHelper ctx, boolean lit) {
        var level = ctx.getLevel();
        for (int x = 0; x <= 4; x++) for (int y = 1; y <= 5; y++) for (int z = 0; z <= 4; z++)
            level.setBlock(ctx.absolutePos(new BlockPos(x, y, z)), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(ctx.absolutePos(new BlockPos(2, 2, 2)), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ctx.absolutePos(new BlockPos(2, 3, 2)), Blocks.AIR.defaultBlockState(), 3);
        if (lit) level.setBlock(ctx.absolutePos(new BlockPos(2, 4, 2)), Blocks.GLOWSTONE.defaultBlockState(), 3);
        return ctx.absolutePos(new BlockPos(2, 2, 2));
    }

    @GameTest
    public void placementsAreOnGroundAndNaturalSpawnsNeedTheSkyIslandRegion(GameTestHelper ctx) {
        BlockPos pos = shell(ctx, false);
        ctx.assertTrue(SkyIslandRegion.classify(ctx.getLevel(), pos) == SkyIslandRegion.Kind.OUTSIDE,
                "a test floor without a generated sky-island start is outside the region");
        for (var type : SkyNormalMobs.allTypes()) {
            ctx.assertTrue(SpawnPlacements.getPlacementType(type) == SpawnPlacementTypes.ON_GROUND, type + " on ground");
            ctx.assertTrue(!rule(ctx, type, EntitySpawnReason.NATURAL, pos), type + ": N1 no natural spawn off the islands");
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void lightRuleFollowsQ29(GameTestHelper ctx) {
        BlockPos dark = shell(ctx, false);
        ctx.runAfterDelay(20, () -> {
            for (var type : SkyNormalMobs.allTypes())
                ctx.assertTrue(rule(ctx, type, EntitySpawnReason.EVENT, dark), type + " may spawn in the dark");
            ctx.getLevel().setBlock(dark.above(2), Blocks.GLOWSTONE.defaultBlockState(), 3);
            ctx.runAfterDelay(20, () -> {
                for (var type : DARK) {
                    ctx.assertTrue(!rule(ctx, type, EntitySpawnReason.EVENT, dark), type + ": Q29 dark only");
                    ctx.assertTrue(rule(ctx, type, EntitySpawnReason.TRIAL_SPAWNER, dark), type + ": Q29 not applied to trial spawners");
                }
                for (var type : ANY_LIGHT)
                    ctx.assertTrue(rule(ctx, type, EntitySpawnReason.EVENT, dark), type + ": Q29 any light");
                ctx.succeed();
            });
        });
    }

    @GameTest
    public void peacefulBlocksEveryPlacement(GameTestHelper ctx) {
        BlockPos pos = shell(ctx, true);
        var server = ctx.getLevel().getServer();
        Difficulty before = ctx.getLevel().getDifficulty();
        boolean[] allowed = new boolean[5];
        server.setDifficulty(Difficulty.PEACEFUL, true);
        try {
            var types = SkyNormalMobs.allTypes();
            for (int i = 0; i < types.size(); i++) allowed[i] = rule(ctx, types.get(i), EntitySpawnReason.TRIAL_SPAWNER, pos);
        } finally {
            server.setDifficulty(before, true);
        }
        for (int i = 0; i < 5; i++) ctx.assertTrue(!allowed[i], "N5/Q74 peaceful blocks " + SkyNormalMobs.allTypes().get(i));
        ctx.succeed();
    }

    /** P4/P6/O1: a real vanilla Trial Spawner with combo A (cloud wraith + cloud slime) spawns only those mobs,
     * slimes only medium (size 2) or large (size 4) with Q16 health, and no random equipment. */
    @GameTest(maxTicks = 600)
    public void comboATrialSpawnerSpawnsOnlyApprovedGroupCMobs(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++)
            level.setBlock(ctx.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState(), 3);
        BlockPos spawnerPos = ctx.absolutePos(new BlockPos(4, 2, 4));
        level.setBlock(spawnerPos, Blocks.TRIAL_SPAWNER.defaultBlockState(), 3);
        var spawner = (TrialSpawnerBlockEntity) level.getBlockEntity(spawnerPos);
        ctx.assertTrue(SkyTowerTrialSpawners.configure(level, spawner, 0), "combo A configured");
        ctx.assertTrue(spawner.getTrialSpawner().normalConfig().spawnPotentialsDefinition().unwrap().size() == 4,
                "combo A potentials: wraith + medium slime + large slime + azure slime");
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "sky-combo");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos stand = ctx.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(stand.getX() + .5, stand.getY(), stand.getZ() + .5);
        AABB box = new AABB(spawnerPos).inflate(8);
        Set<EntityType<?>> allowed = Set.of(SkyNormalMobs.CLOUD_WRAITH, SkyNormalMobs.CLOUD_SLIME, SkyVariantMobs.AZURE_SLIME);
        ctx.onEachTick(() -> {
            var mobs = level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive());
            for (Mob mob : mobs) {
                ctx.assertTrue(allowed.contains(mob.getType()), "only combo A mobs, got " + mob.getType());
                for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                        EquipmentSlot.FEET, EquipmentSlot.MAINHAND))
                    ctx.assertTrue(mob.getItemBySlot(slot).isEmpty(), mob.getType() + ": O1 no random equipment in " + slot);
                if (mob instanceof CloudSlime slime) {
                    ctx.assertTrue(slime.getSize() == 2 || slime.getSize() == 4, "P6 medium or large, got " + slime.getSize());
                    ctx.assertTrue(slime.getMaxHealth() == (slime.getSize() == 2 ? 12f : 32f), "Q16 health " + slime.getMaxHealth());
                }
            }
            if (mobs.size() >= 3) {
                mobs.forEach(m -> m.discard());
                level.getServer().getPlayerList().remove(player);
                ctx.succeed();
            }
        });
    }

    /** C2 existing worlds: a spawner that still holds a spawn pick from the vanilla zombie config drops it when the
     * approved combo is applied (chunk-load path), and a second reconcile is a no-op that keeps the new pick. */
    @GameTest
    public void reconfiguredSpawnerDropsStaleVanillaSpawnPick(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos spawnerPos = ctx.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(spawnerPos, Blocks.TRIAL_SPAWNER.defaultBlockState(), 3);
        var spawner = (TrialSpawnerBlockEntity) level.getBlockEntity(spawnerPos);
        var configs = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRIAL_SPAWNER_CONFIG);
        var zombie = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.TRIAL_SPAWNER_CONFIG,
                net.minecraft.resources.Identifier.withDefaultNamespace("trial_chamber/melee/zombie/normal"));
        var zombieOminous = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.TRIAL_SPAWNER_CONFIG,
                net.minecraft.resources.Identifier.withDefaultNamespace("trial_chamber/melee/zombie/ominous"));
        var accessor = (dev.itzgamesgd.moreminecrafts.mixin.TrialSpawnerConfigAccessor) (Object) spawner.getTrialSpawner();
        var old = accessor.moreminecrafts$config();
        accessor.moreminecrafts$setConfig(new net.minecraft.world.level.block.entity.trialspawner.TrialSpawner.FullConfig(
                configs.getOrThrow(zombie), configs.getOrThrow(zombieOminous), old.targetCooldownLength(), old.requiredPlayerRange()));
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", "minecraft:zombie");
        var state = (dev.itzgamesgd.moreminecrafts.mixin.TrialSpawnerStateDataAccessor) spawner.getTrialSpawner().getStateData();
        state.moreminecrafts$setNextSpawnData(java.util.Optional.of(
                new net.minecraft.world.level.SpawnData(tag, java.util.Optional.empty(), java.util.Optional.empty())));
        var chunk = level.getChunkAt(spawnerPos);
        ctx.assertTrue(SkyTowerTrialSpawners.configureLoading(level, chunk, spawner, 0), "zombie config replaced");
        ctx.assertTrue(accessor.moreminecrafts$config().normal().is(SkyTowerTrialSpawners.key("a", false)), "combo A normal");
        ctx.assertTrue(accessor.moreminecrafts$config().ominous().is(SkyTowerTrialSpawners.key("a", true)), "combo A ominous");
        ctx.assertTrue(state.moreminecrafts$nextSpawnData().isEmpty(), "stale zombie spawn pick dropped");
        ctx.assertTrue(chunk.isUnsaved(), "chunk marked for saving");
        var fresh = java.util.Optional.of(new net.minecraft.world.level.SpawnData());
        state.moreminecrafts$setNextSpawnData(fresh);
        ctx.assertFalse(SkyTowerTrialSpawners.configureLoading(level, chunk, spawner, 0), "second reconcile is a no-op");
        ctx.assertTrue(state.moreminecrafts$nextSpawnData() == fresh, "pick from the approved config kept");
        ctx.succeed();
    }
}
