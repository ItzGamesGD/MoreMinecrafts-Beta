package dev.itzgamesgd.moreminecrafts.pyramid;

import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01AssemblyPiece;
import dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01RoyalHallRegion;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

/** Exercises the natural-spawn hook against a registered ST-01 structure start. */
public final class St01RoyalHallSpawnGameTests {
    @GameTest(maxTicks = 100)
    public void royalHallRejectsNaturalMonsterSpawns(GameTestHelper ctx) throws Exception {
        var level = ctx.getLevel();
        var ground = new BlockPos(320008, 150, 320008);
        var piece = new St01AssemblyPiece(level.getStructureTemplateManager(), ground,
                St01PyramidAssembly.Orientation.SOUTH, St01PyramidAssembly.AccessRoute.EAST, 1);
        var box = piece.getBoundingBox();
        // SOUTH, relative to ground: D1 is inclusive X=-40..-2, Y=-24..-5, Z=2..40.
        ctx.assertTrue(box.minX() == ground.getX()-40 && box.maxX() == ground.getX()-2
                && box.minY() == ground.getY()-24 && box.maxY() == ground.getY()-5
                && box.minZ() == ground.getZ()+2 && box.maxZ() == ground.getZ()+40, "D1 box bounds changed: " + box);
        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(
                ResourceKey.create(Registries.STRUCTURE, Identifier.parse("moreminecrafts:st01_pyramid")));
        var chunk = new ChunkPos(ground.getX() >> 4, ground.getZ() >> 4);
        var start = new StructureStart(structure, chunk, 0, new PiecesContainer(List.of(piece)));
        level.getChunk(chunk.x(), chunk.z()).setStartForStructure(structure, start);
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++)
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++)
                level.getChunk(x, z).addReferenceForStructure(structure, chunk.pack());
        Method hook = java.util.Arrays.stream(NaturalSpawner.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("isValidSpawnPostitionForType")).findFirst().orElseThrow();
        hook.setAccessible(true);
        int allowed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            ctx.assertTrue(St01RoyalHallRegion.inRoyalHall(level, pos), "unrecognized hall position " + pos);
            if ((boolean) hook.invoke(null, level, MobCategory.MONSTER, level.structureManager(),
                    level.getChunkSource().getGenerator(), null, pos.mutable(), 0.0D)) allowed++;
        }
        ctx.assertTrue(allowed == 0, "expected 0 natural monster spawn positions, got " + allowed);
        ctx.assertTrue(!St01RoyalHallRegion.inRoyalHall(level, new BlockPos(box.minX()-1, box.minY(), box.minZ())), "outside box is not hall");
        System.out.println("ST01_ROYAL_HALL_NATURAL_MONSTER_SPAWN box=" + box + " expected=0 allowed=" + allowed);
        ctx.succeed();
    }
}
