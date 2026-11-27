package dev.itzgamesgd.moreminecrafts.pyramid.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.AccessRoute;
import dev.itzgamesgd.moreminecrafts.pyramid.St01PyramidAssembly.Orientation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Completion protocol state: complete only after every chunk of the box is committed, in any order; persisted exactly. */
class St01PyramidCompletionTest {
    @BeforeAll static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }

    private static St01PyramidCompletion.Meta meta(BlockPos ground, Orientation o) {
        BoundingBox box = St01PyramidAssembly.completionBox(ground, o);
        return new St01PyramidCompletion.Meta(ground, o, AccessRoute.WEST, box);
    }

    @Test void boxOfAChunkCentredStartSpansExactly36Chunks() {
        for (Orientation o : Orientation.values()) {
            BlockPos ground = new BlockPos(-9128 + 8, 70, -8008 + 8);
            assertEquals(36, St01PyramidCompletion.requiredChunks(meta(ground, o).box()).length, o.name());
        }
    }

    @Test void completeOnlyWithMetadataAndEveryChunkInNormalReverseAndRandomOrder() {
        BlockPos ground = new BlockPos(16 * 37 + 8, 66, -16 * 5 + 8);
        long start = ChunkPos.pack(37, -5);
        for (int mode = 0; mode < 3; mode++) {
            var m = meta(ground, Orientation.EAST);
            List<Long> order = new ArrayList<>();
            for (long c : St01PyramidCompletion.requiredChunks(m.box())) order.add(c);
            if (mode == 1) Collections.reverse(order);
            if (mode == 2) Collections.shuffle(order, new Random(mode));
            var data = new St01PyramidCompletion();
            for (int i = 0; i < order.size(); i++) {
                data.commit(start, order.get(i));
                if (i == order.size() / 2) data.describe(start, m);
                assertEquals(i == order.size() - 1 && i >= order.size() / 2, St01PyramidCompletion.isComplete(data.entry(start).orElseThrow()), "mode " + mode + " step " + i);
            }
            assertFalse(data.commit(start, order.getFirst()), "re-commit is a no-op");
            assertFalse(data.describe(start, meta(ground.above(), Orientation.NORTH)), "metadata is never replaced");
            assertEquals(m, data.entry(start).orElseThrow().meta().orElseThrow());
        }
        var noMeta = new St01PyramidCompletion();
        for (long c : St01PyramidCompletion.requiredChunks(meta(ground, Orientation.EAST).box())) noMeta.commit(start, c);
        assertFalse(St01PyramidCompletion.isComplete(noMeta.entry(start).orElseThrow()), "never complete without the start's own metadata");
    }

    @Test void stateSurvivesTheSavedDataCodec() {
        BlockPos ground = new BlockPos(-8, 71, 520);
        long start = ChunkPos.pack(-1, 32);
        var data = new St01PyramidCompletion();
        var m = meta(ground, Orientation.WEST);
        long[] req = St01PyramidCompletion.requiredChunks(m.box());
        for (int i = 0; i < req.length - 1; i++) data.commit(start, req[i]);
        data.describe(start, m);
        var codec = St01PyramidCompletion.TYPE.codec();
        var restored = codec.parse(NbtOps.INSTANCE, codec.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
        var e = restored.entry(start).orElseThrow();
        assertEquals(m, e.meta().orElseThrow());
        assertEquals(req.length - 1, e.committedCount());
        assertEquals(St01PyramidCompletion.State.PENDING, e.state());
        assertFalse(St01PyramidCompletion.isComplete(e));
        restored.commit(start, req[req.length - 1]);
        assertTrue(St01PyramidCompletion.isComplete(restored.entry(start).orElseThrow()), "the persisted partial set completes after restart");
    }
}
