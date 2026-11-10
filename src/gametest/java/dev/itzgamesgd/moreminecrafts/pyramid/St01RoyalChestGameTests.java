package dev.itzgamesgd.moreminecrafts.pyramid;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** Transform coverage uses the actual approved D1 NBT. Data-policy tests explicitly use synthetic records. */
public final class St01RoyalChestGameTests {
    private static RoyalChestData roundTrip(RoyalChestData data) {
        return RoyalChestData.TYPE.codec().parse(NbtOps.INSTANCE,
                RoyalChestData.TYPE.codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
    }

    @GameTest(maxTicks = 200)
    public void actualD1NbtRegistersEveryRotationMirrorAndTranslation(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var template = level.getStructureTemplateManager().get(St01RoyalChestPlacement.TEMPLATE).orElseThrow();
        int index = 0;
        for (Mirror mirror : Mirror.values()) for (Rotation rotation : Rotation.values()) {
            // Nonzero pivot exercises the same semantics as StructureTemplate.placeInWorld.
            var settings = St01RoyalChestPlacement.placementSettings(new StructurePlaceSettings().setMirror(mirror).setRotation(rotation)
                    .setRotationPivot(new BlockPos(4, 0, 7)));
            BlockPos origin = ctx.absolutePos(new BlockPos(4096 + 160 * index++, 8, 4096));
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
                level.getChunk((origin.getX() >> 4) + x, (origin.getZ() >> 4) + z);
            ctx.assertTrue(template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS), "D1 placed");
            var registered = St01RoyalChestPlacement.registerPlaced(level, origin, settings);
            BlockPos probeLeft = St01RoyalChestPlacement.worldPos(St01RoyalChestPlacement.REFERENCE_ANCHOR, origin, settings);
            BlockPos probeRight = St01RoyalChestPlacement.worldPos(St01RoyalChestPlacement.REFERENCE_RIGHT, origin, settings);
            ctx.assertTrue(registered.isPresent(), "registration " + mirror + "/" + rotation + " left=" + level.getBlockState(probeLeft) + " right=" + level.getBlockState(probeRight));
            var instance = registered.orElseThrow();
            BlockPos left = instance.primary(), right = instance.primaryRight().orElseThrow();
            var data = RoyalChestRuntime.data(level);
            ctx.assertTrue(data.linkAt(left).orElseThrow().instance().id().equals(data.linkAt(right).orElseThrow().instance().id()), "one canonical instance");
            ctx.assertTrue(data.linkAt(right).orElseThrow().role() == RoyalChestRole.PRIMARY, "right is PRIMARY");
            ctx.assertTrue(instance.secondary().isEmpty(), "no artificial SECONDARY");
            ctx.assertTrue(instance.id().equals(RoyalChestRuntime.canonicalId(level, left)), "world anchor identity");
            ctx.assertTrue(instance.spawn().equals(St01RoyalChestPlacement.worldPos(St01RoyalChestPlacement.REFERENCE_SPAWN, origin, settings)), "spawn transformed");
            ctx.assertTrue(St01RoyalChestPlacement.registerPlaced(level, origin, settings).orElseThrow().equals(instance), "idempotent registration");
            var restored = roundTrip(data);
            ctx.assertTrue(restored.linkAt(right).orElseThrow().instance().equals(instance), "both aliases survive codec");
            RoyalChestRuntime.discardInstance(level, instance.id());
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 40)
    public void syntheticDeferredCatalystEntitlementAndRegistrationConflicts(GameTestHelper ctx) {
        for (RoyalChestRole assigned : RoyalChestRole.values()) {
            var data = new RoyalChestData();
            BlockPos anchor = new BlockPos(11, 20, 30), right = anchor.east(), spawn = anchor.north(7).below(2);
            var instance = data.register("synthetic-" + assigned, anchor, spawn, anchor, Optional.of(right), Optional.empty(), Optional.of(assigned)).orElseThrow();
            var primary = RoyalChestRuntime.generate(ctx.getLevel(), data, instance, RoyalChestRole.PRIMARY).container();
            int expected = assigned == RoyalChestRole.PRIMARY ? 1 : 0;
            ctx.assertTrue(countCatalysts(primary) == expected, "only PRIMARY assignment delivered via PRIMARY");
            primary.removeItemNoUpdate(0);
            data = roundTrip(data);
            ctx.assertTrue(data.instance(instance.id()).orElseThrow().secondary().isEmpty(), "absent SECONDARY stays absent");
            ctx.assertTrue(data.instance(instance.id()).orElseThrow().catalyst().equals(Optional.of(assigned)), "entitlement survived reload");
            ctx.assertTrue(data.linkAt(right).orElseThrow().role() == RoyalChestRole.PRIMARY, "alias survived reload");
            ctx.assertTrue(data.register("conflict", anchor, spawn, right, Optional.empty(), Optional.empty(), Optional.empty()).isEmpty(), "conflicting alias refused");
            ctx.assertTrue(data.register(instance.id(), anchor, spawn.east(), anchor, Optional.of(right), Optional.empty(), Optional.empty()).isEmpty(), "conflicting geometry refused");
            BlockPos secondary = anchor.south(10);
            var extended = data.register(instance.id(), anchor, spawn, anchor, Optional.of(right), Optional.of(secondary), Optional.empty()).orElseThrow();
            ctx.assertTrue(extended.catalyst().equals(Optional.of(assigned)), "future registration retains original assignment");
            var secondaryChest = RoyalChestRuntime.generate(ctx.getLevel(), data, extended, RoyalChestRole.SECONDARY).container();
            ctx.assertTrue(countCatalysts(secondaryChest) == 1 - expected, "SECONDARY receives only its own entitlement");
            for (int slot = 0; slot < 54; slot++) secondaryChest.removeItemNoUpdate(slot);
            ctx.assertTrue(RoyalChestRuntime.generate(ctx.getLevel(), data, extended, RoyalChestRole.SECONDARY).container().isEmpty(), "repeat generation never duplicates catalyst");
            ctx.assertTrue(data.chest(instance.id(), RoyalChestRole.PRIMARY).orElseThrow().container().getItem(0).isEmpty(), "withdrawal persisted through extension");
        }
        ctx.succeed();
    }

    /** Three independent JVM phases on a dedicated test world; actual D1 NBT, normal save boundaries. */
    @GameTest(maxTicks = 100)
    public void royalInventorySurvivesNormalServerProcessRestart(GameTestHelper ctx) {
        String phase = System.getProperty("mm.royal.restart.phase");
        if (phase == null) { ctx.succeed(); return; }
        ServerLevel level = ctx.getLevel();
        BlockPos origin = new BlockPos(-300, 80, -300);
        var settings = St01RoyalChestPlacement.placementSettings(new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90).setMirror(Mirror.FRONT_BACK));
        // Deterministic fixture selection, not a probability test: exercise a real deferred SECONDARY entitlement.
        for (int i = 0; i < 100000; i++) {
            origin = new BlockPos(-300 - 64 * i, 80, -300);
            BlockPos candidate = St01RoyalChestPlacement.worldPos(St01RoyalChestPlacement.REFERENCE_ANCHOR, origin, settings);
            String candidateId = RoyalChestRuntime.canonicalId(level, candidate);
            if (RoyalChestRewardModel.assignCatalyst(RoyalChestRuntime.catalystRandom(level.getSeed(), candidateId))
                    .equals(Optional.of(RoyalChestRole.SECONDARY))) break;
            if (i == 99999) throw new IllegalStateException("No SECONDARY restart fixture found");
        }
        BlockPos anchor = St01RoyalChestPlacement.worldPos(St01RoyalChestPlacement.REFERENCE_ANCHOR, origin, settings);
        String id = RoyalChestRuntime.canonicalId(level, anchor);
        var data = RoyalChestRuntime.data(level);
        var player = ctx.makeMockServerPlayerInLevel();
        if (phase.equals("write")) {
            ctx.assertTrue(data.instance(id).isEmpty(), "fresh restart fixture");
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) level.getChunk((origin.getX() >> 4) + x, (origin.getZ() >> 4) + z);
            var template = level.getStructureTemplateManager().get(St01RoyalChestPlacement.TEMPLATE).orElseThrow();
            ctx.assertTrue(template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS), "actual D1 placed");
            var instance = St01RoyalChestPlacement.registerPlaced(level, origin, settings).orElseThrow();
            ctx.assertTrue(RoyalChestRuntime.tryOpen(level, player, instance.primaryRight().orElseThrow()) == RoyalChestRuntime.OpenResult.SEALED_ENCOUNTER_STARTED, "right starts encounter");
            var guardians = level.getEntities(dev.itzgamesgd.moreminecrafts.boss.TombGuardianEntities.TOMB_GUARDIAN, g -> anchor.equals(g.tombAnchor()));
            ctx.assertTrue(guardians.size() == 1, "one guardian");
            var guardian = guardians.getFirst(); guardian.skipAwakening();
            guardian.hurtServer(level, level.damageSources().playerAttack(player), 10000.0F);
            ctx.assertTrue(RoyalChestRuntime.tryOpen(level, player, anchor) == RoyalChestRuntime.OpenResult.OPENED_AND_GENERATED, "first valid open");
            ctx.assertTrue(countCatalysts(data.chest(id, RoyalChestRole.PRIMARY).orElseThrow().container()) == 0, "deferred SECONDARY catalyst never redistributed");
            data.chest(id, RoyalChestRole.PRIMARY).orElseThrow().container().removeItemNoUpdate(0);
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("mm.royal.restart.file")),
                        RoyalChestData.TYPE.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data).getOrThrow().toString());
            } catch (java.io.IOException e) { throw new RuntimeException(e); }
        } else {
            var instance = data.instance(id).orElseThrow();
            ctx.assertTrue(instance.catalyst().equals(Optional.of(RoyalChestRole.SECONDARY)) && instance.secondary().isEmpty(), "deferred entitlement survived process restart");
            ctx.assertTrue(!dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters.isRoyalChestSealed(level, anchor), "defeat survived restart");
            ctx.assertTrue(dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters.onRoyalChestInteraction(level, anchor, instance.spawn(), player)
                    == dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters.StartResult.ALREADY_DEFEATED, "restart never resurrects defeated Guardian");
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) level.getChunk((origin.getX() >> 4) + x, (origin.getZ() >> 4) + z);
            ctx.assertTrue(St01RoyalChestPlacement.registerPlaced(level, origin, settings).orElseThrow().equals(instance), "re-registration preserves record");
            ctx.assertTrue(RoyalChestRuntime.tryOpen(level, player, instance.primaryRight().orElseThrow()) == RoyalChestRuntime.OpenResult.OPENED, "right reopens without generation");
            try {
                var expected = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("mm.royal.restart.file"))));
                var actual = RoyalChestData.TYPE.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data).getOrThrow();
                ctx.assertTrue(actual.equals(expected), "all aliases, catalyst and partially withdrawn contents match saved fixture exactly");
            } catch (java.io.IOException e) { throw new RuntimeException(e); }
        }
        player.closeContainer();
        level.getServer().getPlayerList().remove(player);
        level.getServer().saveEverything(true, true, true);
        System.out.println("ROYAL_CHEST_NORMAL_RESTART_" + phase.toUpperCase() + "_PASS");
        ctx.succeed();
    }

    private static int countCatalysts(net.minecraft.world.Container container) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(RoyalChestRewardModel.CATALYST_ID)) count += stack.getCount();
        }
        return count;
    }

    @GameTest(maxTicks = 40)
    public void syntheticLegacySchemaPreservesIdsContentsAndAssignment(GameTestHelper ctx) {
        CompoundTag record = new CompoundTag();
        record.putString("id", "legacy-unrelated-id");
        record.putLong("tomb_anchor", new BlockPos(1, 2, 3).asLong());
        record.putLong("spawn", new BlockPos(1, 0, 10).asLong());
        record.putLong("primary", new BlockPos(1, 2, 3).asLong());
        record.putLong("secondary", new BlockPos(7, 2, 3).asLong());
        record.putString("catalyst", "SECONDARY");
        ListTag instances = new ListTag(); instances.add(record);
        CompoundTag stack = new CompoundTag(); stack.putInt("slot", 2); stack.putString("item", "minecraft:gold_ingot"); stack.putInt("count", 17);
        ListTag stacks = new ListTag(); stacks.add(stack);
        CompoundTag chest = new CompoundTag(); chest.putString("instance", "legacy-unrelated-id"); chest.putString("role", "PRIMARY"); chest.put("stacks", stacks);
        ListTag chests = new ListTag(); chests.add(chest);
        CompoundTag root = new CompoundTag(); root.put("instances", instances); root.put("chests", chests);
        var data = RoyalChestData.TYPE.codec().parse(NbtOps.INSTANCE, root).getOrThrow();
        data = roundTrip(data);
        var instance = data.instance("legacy-unrelated-id").orElseThrow();
        ctx.assertTrue(instance.primaryRight().isEmpty() && instance.secondary().isPresent(), "legacy geometry retained without invented alias");
        ctx.assertTrue(instance.catalyst().equals(Optional.of(RoyalChestRole.SECONDARY)), "legacy entitlement retained");
        ctx.assertTrue(data.chest(instance.id(), RoyalChestRole.PRIMARY).orElseThrow().container().getItem(2).getCount() == 17, "legacy items retained");
        ctx.assertTrue(data.register("canonical-new-id", instance.tombAnchor(), instance.spawn(), instance.primary(), Optional.of(instance.primary().east()), instance.secondary(), Optional.empty()).isEmpty(), "legacy anchor cannot fork new reward state");
        ctx.succeed();
    }

    @GameTest(maxTicks = 40)
    public void dimensionsAndIndependentTombsDoNotShareState(GameTestHelper ctx) {
        ServerLevel a = ctx.getLevel(), b = a.getServer().getLevel(Level.NETHER);
        BlockPos anchor = ctx.absolutePos(new BlockPos(1, 1, 1));
        ctx.assertTrue(!RoyalChestRuntime.canonicalId(a, anchor).equals(RoyalChestRuntime.canonicalId(b, anchor)), "dimension is part of identity");
        ctx.assertTrue(RoyalChestRuntime.data(a) != RoyalChestRuntime.data(b), "dimension-scoped SavedData");
        ctx.assertTrue(!RoyalChestRuntime.canonicalId(a, anchor).equals(RoyalChestRuntime.canonicalId(a, anchor.east(100))), "different tombs independent");
        ctx.assertTrue(RoyalChestRuntime.register(a, anchor, anchor.north(7), anchor, anchor.east(), Optional.empty()).isEmpty(), "missing physical double chest rejected");
        ctx.succeed();
    }
}
