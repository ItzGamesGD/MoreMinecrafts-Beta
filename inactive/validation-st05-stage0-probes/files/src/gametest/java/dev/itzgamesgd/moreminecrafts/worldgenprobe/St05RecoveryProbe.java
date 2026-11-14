package dev.itzgamesgd.moreminecrafts.worldgenprobe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Disposable two-marker WAL experiment, NOT a worldgen/recovery implementation. */
public final class St05RecoveryProbe implements ModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> IDS = List.of("resume", "complete_gap", "conflict", "corrupt", "materialize", "all_written", "erased");
    private record Write(int x, int y, int z, String before, String after) {
        BlockPos pos() { return new BlockPos(x,y,z); }
    }
    private record Plan(int schema, long seed, String dimension, String id, List<Write> writes) {}
    private record Journal(Plan plan, String planHash, String state) {}
    private record Envelope(String payload, String sha256) {}

    @Override public void onInitialize() {
        String out = System.getProperty("mm.st05fix.out");
        if (out == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            int code=0;
            try { run(server, Path.of(out), Path.of(System.getProperty("mm.st05fix.store"))); }
            catch (Throwable t) { t.printStackTrace(); code=1; }
            Runtime.getRuntime().halt(code);
        });
    }
    private static String hash(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }
    // Immutable plan + mutable status, checksummed and published by same-directory atomic rename.
    private static void publish(Path path, Journal j) throws Exception {
        String payload=JSON.toJson(j), data=JSON.toJson(new Envelope(payload,hash(payload)));
        Path tmp=Files.createTempFile(path.getParent(),"wal-",".tmp");
        try {
            try (var f=FileChannel.open(tmp,StandardOpenOption.WRITE)) {
                ByteBuffer bytes=ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) f.write(bytes);
                f.force(true);
            }
            Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            try (var directory=FileChannel.open(path.getParent(),StandardOpenOption.READ)) { directory.force(true); }
        } finally { Files.deleteIfExists(tmp); }
    }
    private static Journal read(Path path, String id, ServerLevel level) throws Exception {
        var e=JSON.fromJson(Files.readString(path),Envelope.class);
        if (e==null || e.payload==null || !hash(e.payload).equals(e.sha256)) throw new IllegalStateException("journal checksum");
        var j=JSON.fromJson(e.payload,Journal.class);
        if (j.plan.schema!=1 || j.plan.seed!=level.getSeed() || !j.plan.dimension.equals("minecraft:overworld")
                || !j.plan.id.equals(id) || !hash(JSON.toJson(j.plan)).equals(j.planHash)
                || !Set.of("PREPARED","APPLYING","COMPLETE","CONFLICT").contains(j.state))
            throw new IllegalStateException("journal identity/schema");
        // Only this exact two-marker fixture is accepted. No arbitrary positions or states.
        int row=IDS.indexOf(id);
        if (j.plan.writes.size()!=2) throw new IllegalStateException("write count");
        for (int n=0;n<2;n++) {
            var w=j.plan.writes.get(n);
            if (!w.pos().equals(pos(row,n)) || !w.after.equals(Blocks.EMERALD_BLOCK.defaultBlockState().toString()))
                throw new IllegalStateException("fixture position/state");
        }
        return j;
    }
    private static BlockPos pos(int row,int n) { return new BlockPos(n==0?496:528,100,row*32); }
    private static Journal prepare(Path store, String id, ServerLevel level) throws Exception {
        int row=IDS.indexOf(id);
        var writes=new ArrayList<Write>();
        for (int n=0;n<2;n++) {
            var p=pos(row,n);
            writes.add(new Write(p.getX(),p.getY(),p.getZ(),level.getBlockState(p).toString(),Blocks.EMERALD_BLOCK.defaultBlockState().toString()));
        }
        var p=new Plan(1,level.getSeed(),"minecraft:overworld",id,List.copyOf(writes));
        var j=new Journal(p,hash(JSON.toJson(p)),"PREPARED");
        publish(store.resolve(id+".json"),j);
        return j;
    }
    private static void state(Path store,Journal j,String status) throws Exception {
        publish(store.resolve(j.plan.id+".json"),new Journal(j.plan,j.planHash,status));
    }
    private static void save(MinecraftServer s) {
        if (!s.saveEverything(false,true,true)) throw new AssertionError("save failed");
    }
    private static void set(ServerLevel level,BlockPos p,BlockState b) {
        if (!level.getBlockState(p).equals(b) && !level.setBlock(p,b,3)) throw new AssertionError("write failed");
    }
    private static Map<String,Object> recover(MinecraftServer server,Path store,String id,boolean reverse,boolean interrupt) throws Exception {
        var level=server.getLevel(Level.OVERWORLD);
        Journal j;
        try { j=read(store.resolve(id+".json"),id,level); }
        catch (Exception e) { return Map.of("state","INVALID","writes",0,"reason",e.getClass().getSimpleName()); }
        // COMPLETE is only a hint: inspect actual blocks on EVERY restart and retry.
        for (var w:j.plan.writes) {
            String actual=level.getBlockState(w.pos()).toString();
            if (!actual.equals(w.before) && !actual.equals(w.after)) {
                state(store,j,"CONFLICT");
                return Map.of("state","CONFLICT","writes",0,"plan_hash",j.planHash);
            }
        }
        state(store,j,"APPLYING");
        var ordered=new ArrayList<>(j.plan.writes); if (reverse) Collections.reverse(ordered);
        int changed=0;
        for (var w:ordered) if (!level.getBlockState(w.pos()).toString().equals(w.after)) {
            set(level,w.pos(),Blocks.EMERALD_BLOCK.defaultBlockState()); changed++;
        }
        save(server);
        if (!interrupt) state(store,j,"COMPLETE");
        for (var w:ordered) if (!level.getBlockState(w.pos()).is(Blocks.EMERALD_BLOCK)) throw new AssertionError("incomplete");
        return Map.of("state",interrupt?"APPLYING":"COMPLETE","writes",changed,"previous_state",j.state,"plan_hash",j.planHash);
    }
    private static void run(MinecraftServer server,Path out,Path store) throws Exception {
        long start=System.nanoTime();
        var level=server.getLevel(Level.OVERWORLD);
        Files.createDirectories(store);
        String phase=System.getProperty("mm.st05fix.phase");
        var rows=new TreeMap<String,Object>();
        if (phase.equals("prepare")) {
            for (String id:IDS) {
                int row=IDS.indexOf(id);
                // Force full generation before capturing immutable 'before' snapshots.
                for (int n=0;n<2;n++) level.getChunk(pos(row,n).getX()>>4,pos(row,n).getZ()>>4);
                if (id.equals("materialize")) {
                    Files.createDirectory(store.resolve(id+".json"));
                    boolean rejected=false;
                    var before=level.getBlockState(pos(row,0));
                    try { prepare(store,id,level); } catch (java.io.IOException expected) { rejected=true; }
                    if (!rejected || !level.getBlockState(pos(row,0)).equals(before)) throw new AssertionError("materialization failure");
                    rows.put(id,Map.of("failure_before_mutation",true)); continue;
                }
                var beforeB=level.getBlockState(pos(row,1));
                var j=prepare(store,id,level);
                state(store,j,"APPLYING");
                if (!id.equals("corrupt")) set(level,pos(row,0),Blocks.EMERALD_BLOCK.defaultBlockState());
                if (Set.of("complete_gap","all_written","erased").contains(id)) set(level,pos(row,1),Blocks.EMERALD_BLOCK.defaultBlockState());
                save(server);
                if (id.equals("complete_gap")) { state(store,j,"COMPLETE"); set(level,pos(row,1),beforeB); }
                if (id.equals("erased")) set(level,pos(row,1),beforeB); // simulated player edit indistinguishable from not-yet-written
                if (id.equals("conflict")) set(level,pos(row,1),Blocks.OAK_PLANKS.defaultBlockState());
                if (id.equals("corrupt")) Files.writeString(store.resolve(id+".json"),"{partial");
                rows.put(id,Map.of("plan_hash",j.planHash,"first_marker",level.getBlockState(pos(row,0)).toString(),"second_marker",level.getBlockState(pos(row,1)).toString()));
            }
            save(server);
        } else if (phase.equals("interrupt")) {
            rows.put("resume",recover(server,store,"resume",true,true)); // flushed blocks, no COMPLETE record, then halt
        } else if (phase.equals("recover") || phase.equals("verify")) {
            var order=new ArrayList<>(IDS); if (phase.equals("recover")) Collections.reverse(order);
            for (String id:order) {
                var r=recover(server,store,id,phase.equals("recover"),false);
                var again=recover(server,store,id,false,false);
                if (!again.get("writes").equals(0)) throw new AssertionError("duplicate replay wrote blocks");
                if (phase.equals("verify") && !r.get("writes").equals(0)) throw new AssertionError("restart wrote blocks");
                rows.put(id,Map.of("first",r,"duplicate",again));
            }
            if (!level.getBlockState(pos(2,1)).is(Blocks.OAK_PLANKS)) throw new AssertionError("edit overwritten");
            rows.put("erased_edit_negative_control",Map.of("overwritten",level.getBlockState(pos(6,1)).is(Blocks.EMERALD_BLOCK),"safe_for_player_world",false));
        } else if (phase.equals("order")) {
            for (String id:List.of("all_written","complete_gap","resume")) {
                prepare(store,id,level);
                rows.put(id,recover(server,store,id,true,false));
            }
        } else throw new IllegalArgumentException("unknown phase");
        var result=new TreeMap<String,Object>();
        result.put("phase",phase); result.put("seed",level.getSeed()); result.put("rows",rows);
        result.put("elapsed_ms",(System.nanoTime()-start)/1_000_000);
        result.put("used_heap_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        save(server);
        Files.writeString(out,JSON.toJson(result));
    }
}
