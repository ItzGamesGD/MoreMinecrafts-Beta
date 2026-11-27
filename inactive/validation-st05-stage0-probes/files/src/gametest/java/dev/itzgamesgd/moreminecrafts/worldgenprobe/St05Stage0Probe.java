package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Opt-in, disposable-world counterexample probe. Air components are NOT qualified cavern IDs. */
public final class St05Stage0Probe implements ModInitializer {
    private static final BlockPos A = new BlockPos(496, 100, 0), B = new BlockPos(528, 100, 0);
    @Override public void onInitialize() {
        String out = System.getProperty("mm.st05.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int exit = 0;
            try { run(server, Path.of(out)); }
            catch (Throwable e) { e.printStackTrace(); exit = 1; }
            // Intentional process loss after explicit save, not a graceful shutdown claim.
            Runtime.getRuntime().halt(exit);
        });
    }

    private static void run(MinecraftServer server, Path out) throws Exception {
        long start = System.nanoTime();
        var level = server.getLevel(Level.OVERWORLD);
        var chunks = new ArrayList<int[]>();
        for (int x = 30; x <= 33; x++) for (int z = 0; z <= 1; z++) chunks.add(new int[]{x,z});
        if (Boolean.getBoolean("mm.st05.reverse")) Collections.reverse(chunks);
        for (var c : chunks) level.getChunk(c[0], c[1]);
        var air = new TreeSet<Integer>();
        var digest = MessageDigest.getInstance("SHA-256");
        // Fixed operation budget: 64*32*31 samples; x=512 crosses an Anvil region boundary.
        for (int x=0; x<64; x++) for (int y=0; y<31; y++) for (int z=0; z<32; z++) {
            var state = level.getBlockState(new BlockPos(x+480,y-40,z));
            digest.update((state.toString()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (state.isAir()) air.add((x*31+y)*32+z);
        }
        var remaining = new HashSet<>(air);
        var components = new ArrayList<Map<String,Object>>();
        while (!remaining.isEmpty()) {
            int root = Collections.min(remaining), count=0; boolean frontier=false;
            var q = new ArrayDeque<Integer>(); q.add(root); remaining.remove(root);
            while (!q.isEmpty()) {
                int p=q.remove(), z=p%32, y=(p/32)%31, x=p/(32*31); count++;
                frontier |= x==0 || x==63 || y==0 || y==30 || z==0 || z==31;
                int[][] ns={{x-1,y,z},{x+1,y,z},{x,y-1,z},{x,y+1,z},{x,y,z-1},{x,y,z+1}};
                for (var n:ns) if (n[0]>=0 && n[0]<64 && n[1]>=0 && n[1]<31 && n[2]>=0 && n[2]<32) {
                    int k=(n[0]*31+n[1])*32+n[2]; if (remaining.remove(k)) q.add(k);
                }
            }
            components.add(Map.of("local_min",root,"air_blocks",count,"open_frontier",frontier));
        }
        components.sort(Comparator.comparingInt(c -> (Integer)c.get("local_min")));
        var result = new LinkedHashMap<String,Object>();
        result.put("seed",level.getSeed()); result.put("reverse",Boolean.getBoolean("mm.st05.reverse"));
        result.put("sampled_blocks",64*32*31); result.put("block_sha256",HexFormat.of().formatHex(digest.digest()));
        result.put("components",components); result.put("qualified_cavern_ids","UNKNOWN: finite air scan only; no Q09 segmentation adopted");
        String phase=System.getProperty("mm.st05.phase","scan"); result.put("phase",phase);
        if (phase.equals("write")) {
            if (level.getBlockState(A).is(Blocks.EMERALD_BLOCK) || level.getBlockState(B).is(Blocks.EMERALD_BLOCK))
                throw new IllegalStateException("requires fresh disposable world");
            Path plan=out.resolveSibling("marker-plan.json");
            // Complete two-chunk fixture plan exists BEFORE any block writes. Not arena geometry.
            Files.writeString(plan,"{\"id\":\"stage0-fixture\",\"writes\":[[496,100,0],[528,100,0]]}");
            Path invalid=out.resolveSibling("unmaterializable-plan"); Files.createDirectories(invalid);
            boolean rejected=false;
            try { Files.writeString(invalid,"plan"); } catch (java.io.IOException expected) { rejected=true; }
            if (!rejected) throw new AssertionError("failed plan materialization was accepted");
            if (level.getBlockState(A).is(Blocks.EMERALD_BLOCK) || level.getBlockState(B).is(Blocks.EMERALD_BLOCK))
                throw new AssertionError("materialization failure changed marker states");
            result.put("materialization_failure_before_writes",true);
            if (!level.setBlock(A,Blocks.EMERALD_BLOCK.defaultBlockState(),3))
                throw new AssertionError("first marker write failed");
            if (!server.saveEverything(false,true,true)) throw new AssertionError("fixture save failed");
            result.put("complete_plan_present",Files.isRegularFile(plan));
            result.put("injected_failure","after first chunk write + save, before second write");
        }
        result.put("marker_a",level.getBlockState(A).is(Blocks.EMERALD_BLOCK));
        result.put("marker_b",level.getBlockState(B).is(Blocks.EMERALD_BLOCK));
        result.put("elapsed_ms",(System.nanoTime()-start)/1_000_000);
        result.put("heap_used_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        Files.createDirectories(out.getParent());
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result));
        if (!server.saveEverything(false,true,true)) throw new AssertionError("final save failed");
    }
}
