package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Opt-in disposable-world voxel sampler; no gameplay or worldgen hooks. */
public final class SpiderColonyProbe implements ModInitializer {
    public void onInitialize() {
        String target=System.getProperty("mm.spider.probe.out");
        if(target==null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                var level=server.getLevel(Level.OVERWORLD);
                int side=Integer.getInteger("mm.spider.probe.chunks",8);
                Path out=Path.of(target); Files.createDirectories(out.getParent());
                var times=new ArrayList<Double>(); var chunkTimes=new ArrayList<Map<String,Object>>(); var structures=new TreeSet<String>();
                var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
                long start=System.nanoTime();
                // PROVISIONAL: one-chunk halo supplies references for a 16-block structure buffer.
                for(int x=-1;x<=side;x++) for(int z=-1;z<=side;z++) {
                    long t=System.nanoTime();
                    var chunk=level.getChunk(x,z,ChunkStatus.FULL,true);
                    double elapsedMs=(System.nanoTime()-t)/1e6;
                    times.add(elapsedMs); chunkTimes.add(Map.of("x",x,"z",z,"ms",elapsedMs));
                    for(var e:chunk.getAllStarts().entrySet()) if(e.getValue().isValid())
                        structures.add(registry.getKey(e.getKey())+" "+e.getValue().getBoundingBox());
                    for(var e:chunk.getAllReferences().entrySet()) for(long ref:e.getValue()) {
                        var cp=net.minecraft.world.level.ChunkPos.unpack(ref);
                        var st=level.getChunk(cp.x(),cp.z(),ChunkStatus.STRUCTURE_STARTS).getStartForStructure(e.getKey());
                        if(st!=null && st.isValid()) structures.add(registry.getKey(e.getKey())+" "+st.getBoundingBox());
                    }
                }
                long generated=System.nanoTime();
                long[] counts=new long[3];
                Path voxels=out.resolveSibling(out.getFileName()+".vox.gz");
                try(var stream=new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(voxels)))) {
                    var p=new BlockPos.MutableBlockPos(); int n=side*16;
                    for(int x=0;x<n;x++) for(int z=0;z<n;z++) for(int y=-48;y<=48;y++) {
                        var state=level.getBlockState(p.set(x,y,z));
                        int b=state.isAir()?1:!state.getFluidState().isEmpty()?2:0;
                        stream.write(b); counts[b]++;
                    }
                }
                Collections.sort(times);
                Map<String,Object> result=new LinkedHashMap<>();
                result.put("seed",level.getSeed()); result.put("origin_xz",List.of(0,0));
                result.put("side_blocks",side*16); result.put("y_inclusive",List.of(-48,48));
                result.put("voxel_order","x,z,y; bytes 0=other, 1=air, 2=fluid");
                result.put("structure_buffer_blocks_PROVISIONAL",16); result.put("chunk_halo",1);
                result.put("counts",counts); result.put("structure_starts",structures); result.put("chunk_times",chunkTimes);
                result.put("chunk_request_ms_p50",times.get(times.size()/2));
                result.put("chunk_request_ms_p95",times.get((times.size()-1)*95/100));
                result.put("chunk_request_ms_max",times.get(times.size()-1));
                result.put("chunk_request_total_ms",(generated-start)/1e6);
                result.put("total_ms",(System.nanoTime()-start)/1e6);
                result.put("caveats","structure references plus one-chunk halo; provisional buffer is applied offline; no tick latency measured; sample edges unknown");
                Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result));
                Runtime.getRuntime().halt(0);
            } catch(Throwable t) {t.printStackTrace();Runtime.getRuntime().halt(1);}
        });
    }
}
