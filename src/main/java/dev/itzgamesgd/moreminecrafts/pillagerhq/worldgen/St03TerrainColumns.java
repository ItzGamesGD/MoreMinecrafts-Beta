package dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen;

import dev.itzgamesgd.moreminecrafts.mixin.NoiseBasedChunkGeneratorAccessor;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.ScopedDensityBuffer;
import org.slf4j.LoggerFactory;

/** Same vanilla column algorithm as ST-01's verified batching, obtaining both WG heightmaps in one density volume. */
public final class St03TerrainColumns {
    private St03TerrainColumns() {}
    private static final AtomicBoolean DISABLED=new AtomicBoolean();
    public record Columns(int[] surface,int[] floor) {
        public int surfaceAt(int x,int z){return surface[(x&15)*16+(z&15)];}
        public int floorAt(int x,int z){return floor[(x&15)*16+(z&15)];}
    }
    public static Columns read(ChunkGenerator generator,LevelHeightAccessor level,RandomState random,int minX,int minZ) {
        if(generator.getClass()!=NoiseBasedChunkGenerator.class||DISABLED.get()||Boolean.getBoolean("mm.st03.terrain.legacy"))
            return legacy(generator,level,random,minX,minZ);
        var columns=batch((NoiseBasedChunkGenerator)generator,level,random,minX,minZ);
        for(int[] offset:new int[][]{{0,0},{15,15},{8,8},{0,15},{15,0}}) {
            int x=minX+offset[0],z=minZ+offset[1];
            if(columns.surfaceAt(x,z)!=generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,level,random)
                    ||columns.floorAt(x,z)!=generator.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,level,random)) {
                DISABLED.set(true);LoggerFactory.getLogger("moreminecrafts/st03-terrain").warn("ST03 dense WG height mismatch at {},{}; reverting to exact per-column queries",x,z);
                return legacy(generator,level,random,minX,minZ);
            }
        }
        return columns;
    }
    private static Columns legacy(ChunkGenerator generator,LevelHeightAccessor level,RandomState random,int minX,int minZ) {
        int[] surface=new int[256],floor=new int[256];
        for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            surface[x*16+z]=generator.getBaseHeight(minX+x,minZ+z,Heightmap.Types.WORLD_SURFACE_WG,level,random);
            floor[x*16+z]=generator.getBaseHeight(minX+x,minZ+z,Heightmap.Types.OCEAN_FLOOR_WG,level,random);
        }
        return new Columns(surface,floor);
    }
    private static Columns batch(NoiseBasedChunkGenerator generator,LevelHeightAccessor level,RandomState random,int minX,int minZ) {
        var settings=generator.generatorSettings().value();var noise=settings.noiseSettings().clampToHeightAccessor(level);
        int[] surface=new int[256],floor=new int[256];Arrays.fill(surface,level.getMinY());Arrays.fill(floor,level.getMinY());
        if(noise.height()<=0)return new Columns(surface,floor);
        var picker=((NoiseBasedChunkGeneratorAccessor)(Object)generator).moreminecrafts$globalFluidPicker();
        var volume=new DensityVolume(16,noise.height(),16,minX,noise.minY(),minZ);
        try(var chunk=new NoiseChunk(random,null,settings,picker.get(),Blender.empty(),volume);
            ScopedDensityBuffer density=chunk.cachingSamplers().get(settings.noiseRouter().finalDensity()).sampleVolume(chunk.volume())) {
            var aquifer=chunk.aquifer();var opaque=Heightmap.Types.WORLD_SURFACE_WG.isOpaque();var solid=Heightmap.Types.OCEAN_FLOOR_WG.isOpaque();
            for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
                boolean surfaceFound=false;
                for(int iy=volume.sizeY()-1;iy>=0;iy--) {
                    int y=volume.blockY(iy);var substance=aquifer.computeSubstance(minX+x,y,minZ+z,density.get(volume.indexUnchecked(x,iy,z)));
                    var state=substance==null?settings.defaultBlock():substance;
                    if(!surfaceFound&&opaque.test(state)){surface[x*16+z]=y+1;surfaceFound=true;}
                    if(solid.test(state)){floor[x*16+z]=y+1;break;}
                }
            }
        }
        return new Columns(surface,floor);
    }
}
