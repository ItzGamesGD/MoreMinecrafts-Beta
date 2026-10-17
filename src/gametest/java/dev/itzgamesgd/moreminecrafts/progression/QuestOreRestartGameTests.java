package dev.itzgamesgd.moreminecrafts.progression;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Ore provenance persistence. Without a phase property: a real in-process chunk save, Minecraft
 * unload and reload. With {@code moreminecrafts.questore.restart.phase=write|read}: the same fixture
 * saved by one JVM process and reopened from disk by a second one (a clean shutdown and restart, not a
 * JVM kill).
 */
public final class QuestOreRestartGameTests implements CustomTestMethodInvoker {
    private static final ChunkPos CHUNK = new ChunkPos(200, 200);
    private static final Path MANIFEST = Path.of("quest-ore-restart-fixture.txt");
    private static final int MAX_WAIT_TICKS = 600;
    /** Chunk instances Minecraft actually unloaded (identity), observed through the Fabric event. */
    private static final Set<LevelChunk> UNLOADED = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    static {
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            if (chunk.getPos().equals(CHUNK)) synchronized (UNLOADED) { UNLOADED.add(chunk); }
        });
    }

    private record Fixture(List<BlockPos> natural, BlockPos replaced) {}

    /** Natural decoration, then one natural ore broken and replaced (non-natural from then on). */
    private static Fixture write(GameTestHelper ctx, ServerLevel level) {
        List<BlockPos> ores = QuestOreFixture.decorateWithNaturalOres(level, CHUNK);
        ctx.assertTrue(ores.size() >= 4, "natural fixture produced ores: " + ores.size());
        BlockPos replaced = ores.get(0);
        level.setBlock(replaced, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(replaced, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
        ctx.assertTrue(QuestOreProvenance.verdict(level, replaced) == QuestOreProvenance.Verdict.NON_NATURAL, "replaced before save");
        return new Fixture(ores.subList(1, ores.size()), replaced);
    }

    private static void verifyRestored(GameTestHelper ctx, ServerLevel level, LevelChunk chunk, Fixture fixture) {
        QuestOreProvenance.ChunkData data = chunk.getAttached(QuestOreProvenance.Storage.ATTACHMENT);
        ctx.assertTrue(data != null && data.tracked() && data.schemaVersion() == QuestOreProvenance.SCHEMA_VERSION,
                "tracked attachment restored from disk");
        ctx.assertTrue(data.nonNatural().equals(Set.of(fixture.replaced().asLong())), "exact non-natural set restored");
        for (BlockPos ore : fixture.natural())
            ctx.assertTrue(QuestOreProvenance.verdict(level, ore) == QuestOreProvenance.Verdict.NATURAL, "natural after reload " + ore);
        ServerPlayer miner = ctx.makeMockServerPlayerInLevel();
        miner.setGameMode(GameType.SURVIVAL);
        int before = QuestRules.counter(KnowledgeRuntime.state(miner), "natural_ore_mined");
        ctx.assertTrue(miner.gameMode.destroyBlock(fixture.replaced()), "mined replaced ore");
        ctx.assertTrue(QuestRules.counter(KnowledgeRuntime.state(miner), "natural_ore_mined") == before,
                "replaced ore still does not count after reload");
        ctx.assertTrue(miner.gameMode.destroyBlock(fixture.natural().get(0)), "mined natural ore");
        ctx.assertTrue(QuestRules.counter(KnowledgeRuntime.state(miner), "natural_ore_mined") == before + 1,
                "natural ore still counts after reload");
    }

    @GameTest(maxTicks = 1300)
    public void oreProvenanceSurvivesRealSaveUnloadAndReload(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        String phase = System.getProperty("moreminecrafts.questore.restart.phase");
        try {
            if ("write".equals(phase)) {
                Fixture fixture = write(ctx, level);
                level.getChunkSource().save(true);
                Files.writeString(MANIFEST, encode(fixture));
                System.out.println("QUEST_ORE_RESTART_WRITE chunk=" + CHUNK + " natural=" + fixture.natural().size()
                        + " replaced=" + fixture.replaced().toShortString());
                ctx.succeed();
                return;
            }
            if ("read".equals(phase)) {
                Fixture fixture = decode(Files.readString(MANIFEST));
                level.getChunkSource().addTicketWithRadius(TicketType.FORCED, CHUNK, 0);
                verifyRestored(ctx, level, level.getChunk(CHUNK.x(), CHUNK.z()), fixture);
                System.out.println("QUEST_ORE_RESTART_READ chunk=" + CHUNK + " verified natural=" + fixture.natural().size()
                        + " replaced=" + fixture.replaced().toShortString());
                ctx.succeed();
                return;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("restart manifest unavailable", exception);
        }
        Fixture fixture = write(ctx, level);
        LevelChunk first = level.getChunk(CHUNK.x(), CHUNK.z());
        level.getChunkSource().save(true);
        level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, CHUNK, 0);
        // Synchronous loads (the chunk and the neighbours ore features spill into) leave FULL-level
        // UNKNOWN tickets that only expire in simulated chunks; release them so Minecraft can unload.
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                level.getChunkSource().removeTicketWithRadius(TicketType.UNKNOWN, new ChunkPos(CHUNK.x() + dx, CHUNK.z() + dz), 0);
        awaitUnload(ctx, level, first, fixture, 0);
    }

    private static void awaitUnload(GameTestHelper ctx, ServerLevel level, LevelChunk first, Fixture fixture, int waited) {
        boolean unloaded;
        synchronized (UNLOADED) { unloaded = UNLOADED.contains(first); }
        if (unloaded) {
            level.getChunkSource().addTicketWithRadius(TicketType.FORCED, CHUNK, 0);
            LevelChunk second = level.getChunk(CHUNK.x(), CHUNK.z());
            ctx.assertTrue(second != first, "reload produced a new chunk instance (real unload)");
            verifyRestored(ctx, level, second, fixture);
            level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, CHUNK, 0);
            ctx.succeed();
            return;
        }
        ctx.assertTrue(waited < MAX_WAIT_TICKS, "chunk did not unload after its ticket was removed");
        ctx.runAfterDelay(1, () -> awaitUnload(ctx, level, first, fixture, waited + 1));
    }

    private static String encode(Fixture fixture) {
        return fixture.replaced().asLong() + ";" + fixture.natural().stream().map(p -> Long.toString(p.asLong()))
                .collect(Collectors.joining(","));
    }

    private static Fixture decode(String text) {
        String[] parts = text.trim().split(";");
        List<BlockPos> natural = new TreeSet<>(java.util.Arrays.stream(parts[1].split(",")).map(Long::parseLong).toList())
                .stream().map(BlockPos::of).toList();
        return new Fixture(natural, BlockPos.of(Long.parseLong(parts[0])));
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
