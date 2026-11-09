package dev.itzgamesgd.moreminecrafts.worldgen;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public final class AncientCityArenaGameTests implements CustomTestMethodInvoker {
    private static final Identifier TARGET = Identifier.parse("minecraft:ancient_city/structures");
    private static final Identifier ADDITIONS = Identifier.parse("moreminecrafts:ancient_city/arena_additions");

    @GameTest
    public void loadedPoolRetainsVanillaAndUsesConfiguredArenaWeight(GameTestHelper context) {
        var pools = context.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var addition = pools.get(ResourceKey.create(Registries.TEMPLATE_POOL, ADDITIONS));
        context.assertTrue(addition.isPresent(), "Arena contribution pool must load from production resources");
        var source = addition.orElseThrow().value();
        var target = pools.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, TARGET)).value();
        context.assertTrue(source.getTemplates().size() == 1, "Prototype must contribute exactly one arena");
        var arena = source.getTemplates().getFirst().getFirst();
        int weight = source.getTemplates().stream().mapToInt(entry -> entry.getSecond()).sum();
        context.assertTrue(weight > 0 && weight <= 150, "Configured production weight must be eligible");
        context.assertTrue(target.size() == 46 + weight, "All 46 vanilla weighted slots must be preserved");
        long copies = target.getShuffledTemplates(RandomSource.create(1)).stream().filter(element -> element == arena).count();
        context.assertTrue(copies == weight, "Configured contribution weight must reach the real expanded pool");
        int randomArena = 0;
        int shuffledArena = 0;
        int vanilla = 0;
        for (int seed = 0; seed < 4096; seed++) {
            var selected = target.getRandomTemplate(RandomSource.create(seed));
            if (selected == arena) randomArena++;
            else if (selected != EmptyPoolElement.INSTANCE) vanilla++;
            if (target.getShuffledTemplates(RandomSource.create(seed)).getFirst() == arena) shuffledArena++;
        }
        context.assertTrue(randomArena > 0 && randomArena < 4096, "Arena random selection must be reachable and non-universal");
        context.assertTrue(shuffledArena > 0 && shuffledArena < 4096, "Real jigsaw candidate ordering must sometimes lead with arena");
        context.assertTrue(vanilla > 0, "Ordinary Ancient City structures must remain selectable");
        MoreMinecrafts.LOGGER.info("ARENA_SELECTION_VERIFIED seeds=4096 configured_weight={} total_weight={} random_arena={} shuffled_first_arena={} vanilla={}",
                weight, target.size(), randomArena, shuffledArena, vanilla);
        context.succeed();
    }

    @GameTest
    public void arenaTemplateLoadsAndAttachesToVanillaCityWall(GameTestHelper context) {
        var pools = context.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var addition = pools.get(ResourceKey.create(Registries.TEMPLATE_POOL, ADDITIONS));
        context.assertTrue(addition.isPresent(), "Arena contribution pool must exist");
        var arena = addition.orElseThrow().value().getTemplates().getFirst().getFirst();
        var manager = context.getLevel().getStructureTemplateManager();
        var size = arena.getSize(manager, Rotation.NONE);
        context.assertTrue(size.getX() == 37 && size.getY() == 13 && size.getZ() == 37, "Actual arena template must resolve at its authored size");
        var walls = pools.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, Identifier.parse("minecraft:ancient_city/walls"))).value();
        boolean attached = false;
        for (var entry : walls.getTemplates()) {
            for (var wallJigsaw : entry.getFirst().getShuffledJigsawBlocks(manager, BlockPos.ZERO, Rotation.NONE, RandomSource.create(1))) {
                if (!wallJigsaw.pool().identifier().equals(TARGET)) continue;
                for (var rotation : Rotation.values()) {
                    for (var arenaJigsaw : arena.getShuffledJigsawBlocks(manager, BlockPos.ZERO, rotation, RandomSource.create(1))) {
                        attached |= JigsawBlock.canAttach(wallJigsaw, arenaJigsaw);
                    }
                }
            }
        }
        context.assertTrue(attached, "Vanilla city-wall connectors must accept the arena using the actual 26.3 attachment rules");
        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
