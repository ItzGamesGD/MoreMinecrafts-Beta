package dev.itzgamesgd.moreminecrafts.colosseum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Single-file write-ahead state. Entity/chunk/player saves remain independent: an interrupted effect is quarantined, never replayed blindly. */
final class EffectJournal {
    record Record(String state, String checksum, boolean inFlight, boolean cleanup) {}
    static Path path(Path root, String arena) { return root.resolve(ArenaController.checksum(arena) + ".json"); }
    static void write(Path root, String arena, String state, boolean inFlight) throws IOException {
        write(root,arena,state,inFlight,false);
    }
    static void write(Path root,String arena,String state,boolean inFlight,boolean cleanup) throws IOException {
        Files.createDirectories(root);
        Path target = path(root, arena), temp = target.resolveSibling(target.getFileName() + ".tmp");
        byte[] bytes = ArenaController.GSON.toJson(new Record(state, ArenaController.checksum(state), inFlight, cleanup)).getBytes(StandardCharsets.UTF_8);
        try (FileChannel f = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer b = ByteBuffer.wrap(bytes); while (b.hasRemaining()) f.write(b); f.force(true);
        }
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        try (FileChannel directory = FileChannel.open(root, StandardOpenOption.READ)) { directory.force(true); }
    }
    static Record read(Path root, String arena) throws IOException {
        Path p = path(root, arena);
        if (!Files.exists(p)) return null;
        try {
            Record r = ArenaController.GSON.fromJson(Files.readString(p), Record.class);
            if (r == null || r.state == null || !ArenaController.checksum(r.state).equals(r.checksum)) throw new IOException("journal checksum mismatch");
            return r;
        } catch (RuntimeException e) { throw new IOException("unreadable journal", e); }
    }
}
