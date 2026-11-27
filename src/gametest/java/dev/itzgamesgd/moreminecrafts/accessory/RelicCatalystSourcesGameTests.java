package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.accessory.RelicCatalystSources.Source;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * D13-02: structure-instance catalyst assignment against genuine vanilla-generated ancient city and bastion starts.
 * Deterministic: instances are chosen by the pure assignment function, never by repeating until a 5% roll succeeds.
 */
public final class RelicCatalystSourcesGameTests {
    private static int nextOrigin = 0;

    private record Instance(StructureStart start, List<BlockPos> targets, int assigned) {}

    private static synchronized ChunkPos origin(int base) {
        ChunkPos pos = new ChunkPos(-9000 - 37 * (nextOrigin % 400), base + 37 * (nextOrigin / 400));
        nextOrigin++;
        return pos;
    }

    private static Instance generate(GameTestHelper ctx, Source source, ChunkPos origin) {
        ServerLevel level = ctx.getLevel();
        var holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, source.structure()));
        var generator = level.getChunkSource().getGenerator();
        var randomState = level.getChunkSource().randomState();
        StructureStart start = holder.value().generate(holder, level.dimension(), level.registryAccess(), generator,
                generator.getBiomeSource(), randomState.createClimateSampler(
                        net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED),
                randomState, level.getServer().getStructureTemplateManager(), level.getSeed(), origin, 0, level, biome -> true);
        if (!start.isValid()) return null;
        var box = start.getBoundingBox();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++)
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                var chunk = level.getChunk(cx, cz);
                chunk.addReferenceForStructure(holder.value(), origin.pack());
                if (cx == origin.x() && cz == origin.z()) chunk.setStartForStructure(holder.value(), start);
            }
        List<BlockPos> targets = RelicCatalystSources.targetChests(level, start, source);
        return new Instance(start, targets,
                RelicCatalystSources.assignedIndex(level.getSeed(), origin, source, targets.size()));
    }

    private static List<ItemStack> loot(ServerLevel level, Source source, BlockPos pos, long seed) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(source.lootTable());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).create(LootContextParamSets.CHEST);
        return table.getRandomItems(params, seed);
    }

    private static int catalysts(List<ItemStack> stacks) {
        int count = 0;
        for (ItemStack stack : stacks) if (stack.is(ModItems.RELIC_CATALYST)) count += stack.getCount();
        return count;
    }

    private static Instance find(GameTestHelper ctx, Source source, boolean wantAssigned, int base) {
        int generated = 0;
        for (int attempt = 0; attempt < 20000 && generated < 300; attempt++) {
            ChunkPos origin = origin(base);
            // Cheap deterministic pre-filter by the same pure function; the real chest count is re-checked after generation.
            if ((RelicCatalystSources.assignedIndex(ctx.getLevel().getSeed(), origin, source, 1) >= 0) != wantAssigned
                    && source == Source.BASTION_TREASURE) continue;
            generated++;
            Instance instance = generate(ctx, source, origin);
            if (instance == null || instance.targets().isEmpty()) continue;
            if ((instance.assigned() >= 0) == wantAssigned) return instance;
        }
        ctx.fail("no " + (wantAssigned ? "assigned" : "unassigned") + " " + source + " instance found");
        return null;
    }

    private static void assertInstanceBehaviour(GameTestHelper ctx, Source source, Instance instance) {
        ServerLevel level = ctx.getLevel();
        int total = 0;
        for (int i = 0; i < instance.targets().size(); i++) {
            BlockPos pos = instance.targets().get(i);
            for (long seed = 0; seed < 25; seed++) {
                List<ItemStack> items = loot(level, source, pos, 31L * seed + i);
                int found = catalysts(items);
                ctx.assertTrue(found == (i == instance.assigned() ? 1 : 0),
                        source + " chest " + i + " catalysts=" + found + " assigned=" + instance.assigned());
                if (i == instance.assigned()) {
                    ctx.assertTrue(items.size() > 1 || source == Source.BASTION_TREASURE,
                            "catalyst is added alongside the existing loot, not instead of it");
                    ctx.assertTrue(items.stream().anyMatch(s -> !s.is(ModItems.RELIC_CATALYST)), "existing loot kept");
                }
            }
            total += catalysts(loot(level, source, pos, 7L + i)) > 0 ? 1 : 0;
        }
        ctx.assertTrue(total == (instance.assigned() >= 0 ? 1 : 0), "at most one catalyst chest per structure instance");
    }

    @GameTest(maxTicks = 600)
    public void ancientCityAssignsExactlyOneChestWhenSuccessful(GameTestHelper ctx) {
        Instance instance = find(ctx, Source.ANCIENT_CITY, true, 12000);
        if (instance == null) return;
        ctx.assertTrue(instance.targets().size() >= 2, "a real city has several normal chests (n=" + instance.targets().size() + ")");
        assertInstanceBehaviour(ctx, Source.ANCIENT_CITY, instance);
        ctx.succeed();
    }

    @GameTest(maxTicks = 600)
    public void ancientCityWithoutSuccessGeneratesNoCatalyst(GameTestHelper ctx) {
        Instance instance = find(ctx, Source.ANCIENT_CITY, false, 14000);
        if (instance == null) return;
        assertInstanceBehaviour(ctx, Source.ANCIENT_CITY, instance);
        ctx.succeed();
    }

    @GameTest(maxTicks = 1200)
    public void bastionTreasureRoomAssignsAtMostOneCoreChest(GameTestHelper ctx) {
        Instance hit = find(ctx, Source.BASTION_TREASURE, true, 16000);
        if (hit == null) return;
        assertInstanceBehaviour(ctx, Source.BASTION_TREASURE, hit);
        Instance miss = find(ctx, Source.BASTION_TREASURE, false, 18000);
        if (miss == null) return;
        assertInstanceBehaviour(ctx, Source.BASTION_TREASURE, miss);
        ctx.succeed();
    }

    @GameTest
    public void nonCoreBastionAndLooseChestsNeverReceiveCatalyst(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos loose = ctx.absolutePos(new BlockPos(1, 1, 1));
        for (long seed = 0; seed < 50; seed++) {
            for (Source source : Source.values())
                ctx.assertTrue(catalysts(loot(level, source, loose, seed)) == 0, "chest outside the structure has no catalyst");
            // A bastion bridge/housing chest uses a different table, so it is never touched.
            LootTable other = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.parse("minecraft:chests/bastion_other")));
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(loose))
                    .create(LootContextParamSets.CHEST);
            ctx.assertTrue(catalysts(other.getRandomItems(params, seed)) == 0, "bastion_other has no catalyst pool");
            LootTable iceBox = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.parse("minecraft:chests/ancient_city_ice_box")));
            ctx.assertTrue(catalysts(iceBox.getRandomItems(params, seed)) == 0, "ice box chests are not catalyst targets");
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 600)
    public void assignedChestGeneratesOnceAndNothingIsRedrawnOnReopenOrSave(GameTestHelper ctx) {
        Instance instance = find(ctx, Source.ANCIENT_CITY, true, 20000);
        if (instance == null) return;
        ServerLevel level = ctx.getLevel();
        BlockPos pos = instance.targets().get(instance.assigned());
        level.getChunk(pos);
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ctx.assertTrue(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity, "chest block entity");
        RandomizableContainerBlockEntity chest = (RandomizableContainerBlockEntity) level.getBlockEntity(pos);
        chest.setLootTable(Source.ANCIENT_CITY.lootTable());
        ServerPlayer opener = ctx.makeMockServerPlayerInLevel();
        chest.unpackLootTable(opener);
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) contents.add(chest.getItem(i));
        ctx.assertTrue(catalysts(contents) == 1, "first open yields exactly one catalyst");
        // reopen / second player / repeated unpack: the loot table is spent, nothing is drawn again
        chest.unpackLootTable(ctx.makeMockServerPlayerInLevel());
        chest.unpackLootTable(opener);
        contents.clear();
        for (int i = 0; i < chest.getContainerSize(); i++) contents.add(chest.getItem(i));
        ctx.assertTrue(catalysts(contents) == 1, "reopening never re-rolls");
        ctx.assertTrue(!chest.saveWithFullMetadata(level.registryAccess()).contains("LootTable"),
                "saved chest has no pending loot table, so a restart cannot draw again");
        // Sibling target chests in the same city stay catalyst-free after being opened.
        for (int i = 0; i < instance.targets().size(); i++) {
            if (i == instance.assigned()) continue;
            BlockPos other = instance.targets().get(i);
            level.getChunk(other);
            level.setBlockAndUpdate(other, Blocks.CHEST.defaultBlockState());
            var sibling = (RandomizableContainerBlockEntity) level.getBlockEntity(other);
            sibling.setLootTable(Source.ANCIENT_CITY.lootTable());
            sibling.unpackLootTable(opener);
            List<ItemStack> siblingItems = new ArrayList<>();
            for (int slot = 0; slot < sibling.getContainerSize(); slot++) siblingItems.add(sibling.getItem(slot));
            ctx.assertTrue(catalysts(siblingItems) == 0, "other chests of the same city never hold a catalyst");
        }
        ctx.succeed();
    }
}
