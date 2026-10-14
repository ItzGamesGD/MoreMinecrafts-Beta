package dev.itzgamesgd.moreminecrafts.k5probe;

import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance.ChunkData;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.TreeSet;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Runtime checks for production K5 provenance, direct mining, and diagnostic lifecycle evidence. */
public final class K5NaturalTreeProbeGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void k5FeatureIdentityUsesRegistryAllowlistAndFailsClosed(GameTestHelper ctx) {
        Identifier vanillaForestPlacement = Identifier.fromNamespaceAndPath("minecraft", "trees_birch_and_oak_leaf_litter");
        Identifier vanillaOakVariant = Identifier.fromNamespaceAndPath("minecraft", "oak_bees_0002_leaf_litter");
        Identifier vanillaLargeSpruce = Identifier.fromNamespaceAndPath("minecraft", "mega_spruce");
        Identifier moddedFeature = Identifier.fromNamespaceAndPath("examplemod", "custom_tree");
        Identifier unknownVanillaFeature = Identifier.fromNamespaceAndPath("minecraft", "custom_tree");

        ctx.assertTrue(K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, vanillaOakVariant, true),
                "the runtime-observed vanilla forest tree identity is allowlisted");
        ctx.assertTrue(K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, vanillaLargeSpruce, true),
                "a registered vanilla large-tree identity is allowlisted");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, moddedFeature, true),
                "a mod namespace cannot authorize a tree feature");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(
                        Identifier.fromNamespaceAndPath("examplemod", "trees"), vanillaOakVariant, true),
                "a modded placed feature cannot authorize a vanilla tree source");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, unknownVanillaFeature, true),
                "an unknown minecraft feature key is rejected even in the vanilla namespace");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, null, true),
                "an unregistered feature with no registry key fails closed");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(vanillaForestPlacement, vanillaOakVariant, false),
                "a non-TreeFeature implementation is rejected");
        ctx.assertTrue(!K5NaturalTreeProbe.isEligibleTreeIdentity(null, vanillaOakVariant, true),
                "an unknown placed-feature identity fails closed");
        ctx.succeed();
    }

    @GameTest
    public void k5DeterministicNaturalForestDecorationMarksRealVanillaTrunkLogs(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(level.getSeed() == 0L, "GameTest fixture runs with its pinned seed 0");

        var biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        var forest = biomeRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "forest")).orElseThrow();
        var noiseRegistry = level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS);
        var overworldNoise = noiseRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "overworld")).orElseThrow();
        var placedFeatureRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        Set<Identifier> registeredTreeFeatureIds = new TreeSet<>();
        var featureRegistry = level.registryAccess().lookupOrThrow(Registries.FEATURE);
        for (Feature registeredFeature : featureRegistry) {
            if (registeredFeature instanceof TreeFeature) {
                Identifier key = featureRegistry.getKey(registeredFeature);
                registeredTreeFeatureIds.add(key);
                if (key != null && key.getNamespace().equals("minecraft")) {
                    ctx.assertTrue(K5NaturalTreeProbe.isKnownVanillaTreeFeatureId(key),
                            "every registered vanilla TreeFeature key is in the pinned allowlist: " + key);
                }
            }
        }
        Set<Identifier> vanillaForestTreePlacements = new TreeSet<>();
        for (HolderSet<PlacedFeature> step : forest.value().getGenerationSettings().features()) {
            for (Holder<PlacedFeature> placedHolder : step) {
                PlacedFeature placed = placedHolder.value();
                if (placed.getFeatures().anyMatch(feature -> feature.value() instanceof TreeFeature)) {
                    Identifier key = placedFeatureRegistry.getKey(placed);
                    if (key != null && key.getNamespace().equals("minecraft")) {
                        vanillaForestTreePlacements.add(key);
                    }
                }
            }
        }
        ctx.assertTrue(!vanillaForestTreePlacements.isEmpty(),
                "the registered minecraft:forest generation settings contain registered vanilla TreeFeature placements");

        NoiseBasedChunkGenerator fixtureGenerator =
                new NoiseBasedChunkGenerator(new FixedBiomeSource(forest), overworldNoise);
        BlockPos fixedChunkProbe = new BlockPos(8, 4, 8);
        ChunkAccess chunk = level.getChunkAt(fixedChunkProbe);
        ctx.assertTrue(chunk.getPos().equals(new ChunkPos(0, 0)),
                "fixture chunk coordinates are fixed at (0,0)");
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> forest);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 2, z), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 3, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }

        ChunkData before = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(before == null || before.positions().isEmpty(),
                "the controlled target chunk starts without provenance markers");
        long callsBefore = K5NaturalTreeProbe.biomeDecorationCalls();
        long callbacksBefore = K5NaturalTreeProbe.naturalTreeTrunkCallbacks();
        fixtureGenerator.applyBiomeDecoration(level, chunk, level.structureManager());

        ctx.assertTrue(K5NaturalTreeProbe.biomeDecorationCalls() == callsBefore + 1,
                "the verified natural ChunkGenerator.applyBiomeDecoration hook was entered");
        ctx.assertTrue(!K5NaturalTreeProbe.isNaturalBiomeDecoration(),
                "natural-generation scope is cleared after biome decoration returns");
        Set<Identifier> observedTreeFeatureIds = K5NaturalTreeProbe.lastNaturalTreeFeatureIds();
        Set<Long> observedTrunkPositions = K5NaturalTreeProbe.lastNaturalTreeCallbackPositions();
        ChunkData data = K5NaturalTreeProbe.data(chunk);
        var positions = new TreeSet<>(data == null ? Set.<Long>of() : data.positions());
        System.out.println("K5_DETERMINISTIC_FIXTURE seed=0 dimension=" + level.dimension().identifier()
                + " generator=NoiseBasedChunkGenerator"
                + " noiseSettings=minecraft:overworld biome=minecraft:forest chunk=[0,0]"
                + " configuredTreeFeatureIds=" + registeredTreeFeatureIds
                + " treePlacements=" + vanillaForestTreePlacements
                + " observedTreeFeatureIds=" + observedTreeFeatureIds
                + " callbackCount=" + (K5NaturalTreeProbe.naturalTreeTrunkCallbacks() - callbacksBefore)
                + " callbackPositions=" + new TreeSet<>(observedTrunkPositions)
                + " markerCount=" + positions.size()
                + " markers=" + positions);
        ctx.assertTrue(K5NaturalTreeProbe.naturalTreeTrunkCallbacks() > callbacksBefore,
                "natural biome decoration invoked an actual registered TreeFeature trunk setter");
        ctx.assertTrue(observedTreeFeatureIds.contains(
                        Identifier.fromNamespaceAndPath("minecraft", "oak_bees_0002_leaf_litter")),
                "the deterministic forest invokes the runtime-observed registered vanilla Feature identity");
        ctx.assertTrue(!observedTrunkPositions.isEmpty(),
                "the runtime trace identifies actual trunk callback positions");
        Set<Long> actualLogPositions = new TreeSet<>();
        var callbackStates = new java.util.TreeMap<String, String>();
        for (long packed : observedTrunkPositions) {
            BlockPos callbackPos = BlockPos.of(packed);
            var state = level.getBlockState(callbackPos);
            callbackStates.put(callbackPos.toString(), state.toString());
            if (state.is(net.minecraft.tags.BlockTags.LOGS)) actualLogPositions.add(packed);
        }
        System.out.println("K5_CALLBACK_FINAL_BLOCK_STATES " + callbackStates);
        ctx.assertTrue(!actualLogPositions.isEmpty(),
                "at least one natural TreeFeature callback position remains a real log block after decoration");
        ctx.assertTrue(data != null && !data.positions().isEmpty(),
                "the natural TreeFeature marker predicate records at least one actual trunk position");
        ctx.assertTrue(actualLogPositions.containsAll(positions),
                "every marker was independently observed as a final log at a trunk callback position");
        for (long packed : positions) {
            BlockPos markedPos = BlockPos.of(packed);
            var state = level.getBlockState(markedPos);
            ctx.assertTrue(level.getChunkAt(markedPos) == chunk,
                    "each marker is attached to the exact chunk and world that contain its position");
            ctx.assertTrue(state.is(Blocks.OAK_LOG) || state.is(Blocks.BIRCH_LOG),
                    "every marked position contains an expected vanilla forest trunk log: " + markedPos + " = " + state);
            ctx.assertTrue(data.positions().contains(markedPos.asLong()),
                    "the independently read block position is present in the persistent chunk marker set");
        }
        // The same deterministic natural fixture now exercises the production collector.
        ctx.assertTrue(positions.size() >= 6, "natural fixture has enough distinct logs for collector controls");
        var marked = positions.stream().map(BlockPos::of).toList();
        var miner = ctx.makeMockServerPlayerInLevel();
        miner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos first = marked.get(0);
        ctx.assertTrue(miner.gameMode.destroyBlock(first), "Survival player directly mined a natural marked log");
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 1,
                "successful direct mining awards exactly one K5 progress point");
        ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, first), "mined marker was consumed");
        miner.gameMode.destroyBlock(first);
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 1,
                "the same original log cannot be counted twice");

        var other = ctx.makeMockServerPlayerInLevel();
        other.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(other.gameMode.destroyBlock(marked.get(1)), "second player mined a separate natural log");
        ctx.assertTrue(KnowledgeRuntime.state(other).get("condition.natural_vanilla_logs_mined") == 1
                        && KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 1,
                "separate players have independent progress");

        var creative = ctx.makeMockServerPlayerInLevel();
        creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        creative.gameMode.destroyBlock(marked.get(2));
        ctx.assertTrue(KnowledgeRuntime.state(creative).get("condition.natural_vanilla_logs_mined") == 0,
                "Creative removal never awards K5");
        var spectator = ctx.makeMockServerPlayerInLevel();
        spectator.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        spectator.gameMode.destroyBlock(marked.get(3));
        ctx.assertTrue(KnowledgeRuntime.state(spectator).get("condition.natural_vanilla_logs_mined") == 0,
                "Spectator removal never awards K5");

        var thresholdMiner = ctx.makeMockServerPlayerInLevel();
        thresholdMiner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        KnowledgeRuntime.recordCondition(thresholdMiner, "natural_vanilla_logs_mined", 199);
        ctx.assertTrue(!KnowledgeRuntime.knows(thresholdMiner, 5), "K5 remains locked at 199");
        ctx.assertTrue(thresholdMiner.gameMode.destroyBlock(marked.get(4)),
                "threshold log was directly mined from the verified natural fixture");
        ctx.assertTrue(KnowledgeRuntime.state(thresholdMiner).get("condition.natural_vanilla_logs_mined") == 200
                        && KnowledgeRuntime.knows(thresholdMiner, 5), "K5 unlocks at 200");
        thresholdMiner.gameMode.destroyBlock(marked.get(4));
        ctx.assertTrue(KnowledgeRuntime.state(thresholdMiner).get("condition.natural_vanilla_logs_mined") == 200,
                "post-unlock duplicate mining does not increase progress");

        level.setBlock(first, Blocks.OAK_LOG.defaultBlockState(), 3);
        ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, first),
                "replacement at a consumed position has no natural provenance");
        miner.gameMode.destroyBlock(first);
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 1,
                "a mined replacement log cannot restore progress");

        BlockPos indirect = marked.get(5);
        ctx.assertTrue(level.destroyBlock(indirect, true), "server destroyed a natural log without a player mining it");
        ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, indirect)
                        && KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 1,
                "indirect removal consumes provenance without awarding a player");

        BlockPos blocked = marked.get(6);
        var cancel = new java.util.concurrent.atomic.AtomicBoolean(true);
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register(
                (world, actor, breakingPos, breakingState, blockEntity) ->
                        !(cancel.get() && world == level && breakingPos.equals(blocked)));
        boolean cancelledResult = miner.gameMode.destroyBlock(blocked);
        cancel.set(false);
        ctx.assertTrue(!cancelledResult && K5NaturalTreeProvenance.hasMarker(level, blocked),
                "cancelled direct break leaves both block and marker intact");
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 1,
                "cancelled direct break gives no progress");

        BlockPos raced = marked.get(7);
        ctx.assertTrue(miner.gameMode.destroyBlock(raced), "first player mined the shared position");
        other.gameMode.destroyBlock(raced);
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 2
                        && KnowledgeRuntime.state(other).get("condition.natural_vanilla_logs_mined") == 1,
                "only the first successful miner of one physical log receives credit");

        miner.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG, 64));
        other.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG, 64));
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 2
                        && KnowledgeRuntime.state(other).get("condition.natural_vanilla_logs_mined") == 1,
                "inventory receipt and gifted log stacks never grant direct-mining progress");
        ctx.succeed();
    }

    @GameTest
    public void k5ChunkAttachmentStoresPackedPositionData(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos logPos = ctx.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);
        ChunkAccess chunk = level.getChunkAt(logPos);
        var target = (net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) chunk;
        target.setAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS,
                new ChunkData(K5NaturalTreeProbe.SCHEMA_VERSION, Set.of(logPos.asLong())));

        ChunkData data = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(data != null && data.schemaVersion() == K5NaturalTreeProbe.SCHEMA_VERSION,
                "persistent Fabric chunk attachment is available and uses the declared schema");
        ctx.assertTrue(data.positions().equals(Set.of(logPos.asLong())),
                "the attachment codec stores a packed position");
        ctx.succeed();
    }


    @GameTest
    public void k5NaturalChunkProvenanceSurvivesRealSaveUnloadReloadAndInvalidation(GameTestHelper ctx) {
        String restartPhase = System.getProperty("moreminecrafts.k5.restart.phase");
        if ("write".equals(restartPhase)) {
            new RestartPersistenceRun(ctx).write();
            return;
        }
        if ("read".equals(restartPhase)) {
            new RestartPersistenceRun(ctx).read();
            return;
        }
        new ChunkPersistenceRun(ctx).start();
    }

    /** A filtered GameTest runs in two fresh JVM processes sharing the same saved world. */
    private static final class RestartPersistenceRun {
        private static final java.util.UUID PLAYER_ID =
                java.util.UUID.fromString("45d315ee-09d7-40d9-81c9-6c04438da132");
        private static final java.nio.file.Path MANIFEST = java.nio.file.Path.of("k5-restart-fixture.txt");
        private static final ChunkPos CHUNK = new ChunkPos(64, 64);
        private final GameTestHelper ctx;
        private final ServerLevel level;

        private RestartPersistenceRun(GameTestHelper ctx) {
            this.ctx = ctx;
            this.level = ctx.getLevel();
        }

        private net.minecraft.server.level.ServerPlayer player(boolean loadFromDisk) {
            var profile = new com.mojang.authlib.GameProfile(PLAYER_ID, "k5-restart-player");
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
            var player = new net.minecraft.server.level.ServerPlayer(
                    level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
            if (loadFromDisk) {
                var saved = level.getServer().getPlayerList().loadPlayerData(player.nameAndId());
                ctx.assertTrue(saved.isPresent(), "Minecraft PlayerDataStorage loaded the fixed player UUID");
                player.load(net.minecraft.world.level.storage.TagValueInput.create(
                        net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess(), saved.orElseThrow()));
            }
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            return player;
        }

        private void write() {
            try {
                ctx.assertTrue(level.getSeed() == 0L, "restart fixture uses the pinned seed 0");
                level.getChunkSource().addTicketWithRadius(TicketType.FORCED, CHUNK, 0);
                LevelChunk chunk = level.getChunk(CHUNK.x(), CHUNK.z());
                ChunkPersistenceRun fixture = new ChunkPersistenceRun(ctx);
                fixture.generateDeterministicNaturalTree(chunk);
                var marked = fixture.originalMarkers.stream().sorted().map(BlockPos::of).toList();
                ctx.assertTrue(marked.size() >= 4, "restart fixture generated four real natural trunk logs");
                BlockPos removed = marked.get(0);
                BlockPos replaced = marked.get(1);
                BlockPos mined = marked.get(2);
                BlockPos surviving = marked.get(3);
                ctx.assertTrue(level.destroyBlock(removed, true), "indirectly removed original log before save");
                level.setBlock(replaced, Blocks.OAK_LOG.defaultBlockState(), 3);
                ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, removed)
                                && !K5NaturalTreeProvenance.hasMarker(level, replaced),
                        "removed and replaced logs lose provenance before disk save");
                var player = player(false);
                KnowledgeRuntime.recordCondition(player, "natural_vanilla_logs_mined", 37);
                ctx.assertTrue(player.gameMode.destroyBlock(mined), "fixed player directly mined a natural marked log");
                ctx.assertTrue(KnowledgeRuntime.state(player).get("condition.natural_vanilla_logs_mined") == 38,
                        "K5 progress was recorded before player save");
                ctx.assertTrue(K5NaturalTreeProvenance.hasMarker(level, surviving),
                        "surviving natural marker remains before save");
                java.nio.file.Files.writeString(MANIFEST,
                        removed.asLong() + "," + replaced.asLong() + "," + mined.asLong() + ","
                                + surviving.asLong());
                level.getServer().getPlayerList().saveAll();
                level.getChunkSource().save(true);
                System.out.println("K5_RESTART_WRITE worldSeed=0 chunk=[64,64] player=" + PLAYER_ID
                        + " progress=38 surviving=" + surviving);
                ctx.succeed();
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("could not write restart fixture manifest", exception);
            }
        }

        private void read() {
            try {
                ctx.assertTrue(level.getSeed() == 0L, "restarted process reopened the pinned world seed");
                var parts = java.nio.file.Files.readString(MANIFEST).trim().split(",");
                ctx.assertTrue(parts.length == 4, "process A recorded four fixture positions");
                BlockPos removed = BlockPos.of(Long.parseLong(parts[0]));
                BlockPos replaced = BlockPos.of(Long.parseLong(parts[1]));
                BlockPos mined = BlockPos.of(Long.parseLong(parts[2]));
                BlockPos surviving = BlockPos.of(Long.parseLong(parts[3]));
                LevelChunk chunk = level.getChunk(CHUNK.x(), CHUNK.z());
                ctx.assertTrue(chunk.getPos().equals(CHUNK), "Minecraft loaded the correct persisted chunk");
                ctx.assertTrue(K5NaturalTreeProvenance.hasMarker(level, surviving)
                                && level.getBlockState(surviving).is(net.minecraft.tags.BlockTags.LOGS),
                        "fresh process restored a marker at its real surviving log position");
                ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, removed)
                                && !K5NaturalTreeProvenance.hasMarker(level, replaced)
                                && !K5NaturalTreeProvenance.hasMarker(level, mined),
                        "consumed natural markers did not reappear after process restart");
                ctx.assertTrue(level.getBlockState(replaced).is(Blocks.OAK_LOG),
                        "replacement log exists after reload without inherited provenance");
                var player = player(true);
                ctx.assertTrue(KnowledgeRuntime.state(player).get("condition.natural_vanilla_logs_mined") == 38,
                        "Minecraft player data restored K5 progress into a fresh player instance");
                ctx.assertTrue(!KnowledgeRuntime.knows(player, 5), "K5 remains locked below 200 after restart");
                System.out.println("K5_RESTART_READ chunk=[64,64] player=" + PLAYER_ID
                        + " progress=38 surviving=" + surviving);
                ctx.succeed();
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("could not read restart fixture manifest", exception);
            }
        }
    }

    @GameTest
    public void k5MalformedAndCrossChunkAttachmentDataFailsClosed(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos origin = ctx.absolutePos(new BlockPos(2, 4, 2));
        ChunkAccess chunk = level.getChunkAt(origin);
        var target = (net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) chunk;

        var malformed = K5NaturalTreeProbe.CODEC.parse(
                net.minecraft.nbt.NbtOps.INSTANCE, new net.minecraft.nbt.CompoundTag());
        ctx.assertTrue(malformed.error().isPresent(), "missing required persisted fields are rejected by the codec");
        target.setAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS,
                new ChunkData(K5NaturalTreeProbe.SCHEMA_VERSION + 1, Set.of(origin.asLong())));
        ctx.assertTrue(K5NaturalTreeProbe.data(chunk) == null
                        && target.getAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS) == null,
                "unknown attachment schema is removed and fails closed");

        BlockPos foreignChunkPos = origin.offset(16, 0, 0);
        target.setAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS,
                new ChunkData(K5NaturalTreeProbe.SCHEMA_VERSION, Set.of(foreignChunkPos.asLong())));
        ctx.assertTrue(K5NaturalTreeProbe.data(chunk) == null
                        && target.getAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS) == null,
                "a position encoded in the wrong chunk is discarded");
        ctx.succeed();
    }

    private static final class ChunkPersistenceRun {
        private static final int MAX_WAIT_TICKS = 240;
        private final GameTestHelper ctx;
        private final ServerLevel level;
        private final ChunkPos targetPos = new ChunkPos(64, 64);
        private final int chunkX = 64;
        private final int chunkZ = 64;
        private CompletableFuture<ChunkResult<ChunkAccess>> pendingLoad;
        private LevelChunk firstInstance;
        private LevelChunk secondInstance;
        private Set<Long> originalMarkers = Set.of();
        private BlockPos replacedPos;
        private int phaseTicks;
        private int phase;

        private ChunkPersistenceRun(GameTestHelper ctx) {
            this.ctx = ctx;
            this.level = ctx.getLevel();
        }

        private void start() {
            ctx.assertTrue(level.getSeed() == 0L, "persistence fixture is pinned to seed 0");
            requestChunk();
        }

        private void requestChunk() {
            level.getChunkSource().addTicketWithRadius(TicketType.FORCED, targetPos, 0);
            pendingLoad = level.getChunkSource().getChunkFuture(
                    chunkX, chunkZ, ChunkStatus.FULL, true);
            phaseTicks = 0;
            ctx.runAfterDelay(1, this::pollInitialLoad);
        }

        private void pollInitialLoad() {
            ChunkAccess access = completedChunk();
            if (access == null) {
                if (++phaseTicks >= MAX_WAIT_TICKS) {
                    ctx.assertTrue(false, "forced remote chunk did not reach FULL status");
                    return;
                }
                ctx.runAfterDelay(1, this::pollInitialLoad);
                return;
            }
            if (!(access instanceof LevelChunk levelChunk)) {
                ctx.assertTrue(false, "FULL chunk load returned a LevelChunk");
                return;
            }
            firstInstance = levelChunk;
            generateDeterministicNaturalTree(firstInstance);
            if (originalMarkers.size() < 2) {
                ctx.assertTrue(false, "fixture generated multiple real eligible trunk logs for lifecycle checks");
                return;
            }
            long savesBefore = K5NaturalTreeProbe.savedChunksWithMarkers();
            level.getChunkSource().save(true);
            ctx.assertTrue(K5NaturalTreeProbe.savedChunksWithMarkers() > savesBefore,
                    "ServerChunkCache.save(true) reached ChunkMap.save for a marked chunk");
            K5NaturalTreeProbe.clearLastMarkedChunkUnload();
            level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, targetPos, 0);
            phaseTicks = 0;
            ctx.runAfterDelay(1, this::pollFirstUnload);
        }

        private void generateDeterministicNaturalTree(LevelChunk chunk) {
            var biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
            var forest = biomeRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "forest")).orElseThrow();
            var noiseRegistry = level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS);
            var overworldNoise = noiseRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "overworld")).orElseThrow();
            chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> forest);
            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    level.setBlock(new BlockPos(baseX + x, 1, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(baseX + x, 2, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(baseX + x, 3, baseZ + z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                }
            }
            NoiseBasedChunkGenerator fixtureGenerator =
                    new NoiseBasedChunkGenerator(new FixedBiomeSource(forest), overworldNoise);
            fixtureGenerator.applyBiomeDecoration(level, chunk, level.structureManager());
            ctx.assertTrue(!K5NaturalTreeProbe.isNaturalBiomeDecoration(),
                    "natural biome-decoration scope was cleared after generation");
            ChunkData data = K5NaturalTreeProbe.data(chunk);
            if (data == null || data.positions().isEmpty()) {
                ctx.assertTrue(false, "controlled natural biome decoration generated and marked vanilla tree logs");
                return;
            }
            Set<Long> actualLogs = new TreeSet<>();
            for (long packed : data.positions()) {
                BlockPos pos = BlockPos.of(packed);
                ctx.assertTrue(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4).equals(targetPos),
                        "natural marker belongs to the target chunk");
                ctx.assertTrue(level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS),
                        "marked position independently contains a real vanilla log: " + pos);
                actualLogs.add(packed);
            }
            originalMarkers = Set.copyOf(actualLogs);
            System.out.println("K5_PERSISTENCE_FIXTURE seed=0 biome=minecraft:forest chunk=[64,64]"
                    + " dimension=" + level.dimension().identifier() + " markerCount=" + originalMarkers.size()
                    + " markers=" + new TreeSet<>(originalMarkers));
        }

        private void pollFirstUnload() {
            if (K5NaturalTreeProbe.observedMarkedChunkUnload(level, targetPos, firstInstance)) {
                ctx.assertTrue(!level.getChunkSource().hasChunk(chunkX, chunkZ),
                        "the original marked chunk is unavailable at FULL status after Minecraft unload");
                requestReload(1);
                return;
            }
            if (++phaseTicks >= MAX_WAIT_TICKS) {
                ctx.assertTrue(false, "removing the FORCED ticket did not reach ServerLevel.unload");
                return;
            }
            ctx.runAfterDelay(1, this::pollFirstUnload);
        }

        private void requestReload(int reloadPhase) {
            phase = reloadPhase;
            level.getChunkSource().addTicketWithRadius(TicketType.FORCED, targetPos, 0);
            pendingLoad = level.getChunkSource().getChunkFuture(
                    chunkX, chunkZ, ChunkStatus.FULL, true);
            phaseTicks = 0;
            ctx.runAfterDelay(1, this::pollReload);
        }

        private void pollReload() {
            ChunkAccess access = completedChunk();
            if (access == null) {
                if (++phaseTicks >= MAX_WAIT_TICKS) {
                    ctx.assertTrue(false, "Minecraft chunk loading did not return the persisted chunk");
                    return;
                }
                ctx.runAfterDelay(1, this::pollReload);
                return;
            }
            if (!(access instanceof LevelChunk levelChunk)) {
                ctx.assertTrue(false, "reloaded FULL chunk is a LevelChunk");
                return;
            }
            if (phase == 1) {
                secondInstance = levelChunk;
                ctx.assertTrue(secondInstance != firstInstance,
                        "reload created a new chunk instance after the original was unloaded");
                ChunkData restored = K5NaturalTreeProbe.data(secondInstance);
                ctx.assertTrue(restored != null && restored.positions().equals(originalMarkers),
                        "disk save and Minecraft load restored the exact marker set");
                for (long packed : originalMarkers) {
                    BlockPos pos = BlockPos.of(packed);
                    ctx.assertTrue(level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS),
                            "reloaded marker still points to an eligible log: " + pos);
                }
                verifyWorldAndChunkIsolation();
                replacedPos = BlockPos.of(originalMarkers.iterator().next());
                level.setBlock(replacedPos, Blocks.AIR.defaultBlockState(), 3);
                ctx.assertTrue(!K5NaturalTreeProbe.data(secondInstance).positions().contains(replacedPos.asLong()),
                        "legitimate removal consumes the marker immediately");
                level.setBlock(replacedPos, Blocks.OAK_LOG.defaultBlockState(), 3);
                ctx.assertTrue(!K5NaturalTreeProbe.data(secondInstance).positions().contains(replacedPos.asLong()),
                        "a replacement log at the same position does not inherit provenance");
                int boundedCount = originalMarkers.size() - 1;
                for (int i = 0; i < 24; i++) {
                    level.setBlock(replacedPos, Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(replacedPos, Blocks.OAK_LOG.defaultBlockState(), 3);
                }
                ChunkData afterReplacements = K5NaturalTreeProbe.data(secondInstance);
                ctx.assertTrue(afterReplacements.positions().size() == boundedCount,
                        "repeated replacement does not grow stored marker quantity");
                long savesBefore = K5NaturalTreeProbe.savedChunksWithMarkers();
                level.getChunkSource().save(true);
                ctx.assertTrue(K5NaturalTreeProbe.savedChunksWithMarkers() > savesBefore,
                        "mutated marker set was written through the real ChunkMap save path");
                K5NaturalTreeProbe.clearLastMarkedChunkUnload();
                level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, targetPos, 0);
                phaseTicks = 0;
                ctx.runAfterDelay(1, this::pollSecondUnload);
            } else {
                Set<Long> expectedRemaining = new HashSet<>(originalMarkers);
                expectedRemaining.remove(replacedPos.asLong());
                ChunkData restored = K5NaturalTreeProbe.data(levelChunk);
                ctx.assertTrue(levelChunk != secondInstance,
                        "second unload/reload created a fresh LevelChunk instance");
                ctx.assertTrue(restored != null && restored.positions().equals(expectedRemaining),
                        "consumed provenance stays absent after another save and reload");
                ctx.assertTrue(level.getBlockState(replacedPos).is(Blocks.OAK_LOG),
                        "replacement log remains after persistence reload");
                ctx.assertTrue(!restored.positions().contains(replacedPos.asLong()),
                        "replacement log cannot recover the consumed marker after reload");
                level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, targetPos, 0);
                ctx.succeed();
            }
        }

        private void pollSecondUnload() {
            if (K5NaturalTreeProbe.observedMarkedChunkUnload(level, targetPos, secondInstance)) {
                requestReload(2);
                return;
            }
            if (++phaseTicks >= MAX_WAIT_TICKS) {
                ctx.assertTrue(false, "second mutation save did not unload the marked chunk");
                return;
            }
            ctx.runAfterDelay(1, this::pollSecondUnload);
        }

        private ChunkAccess completedChunk() {
            if (pendingLoad == null || !pendingLoad.isDone() || pendingLoad.isCompletedExceptionally()) return null;
            ChunkResult<ChunkAccess> result = pendingLoad.getNow(null);
            if (result == null) return null;
            AtomicReference<ChunkAccess> loaded = new AtomicReference<>();
            result.ifSuccess(loaded::set);
            return loaded.get();
        }

        private void verifyWorldAndChunkIsolation() {
            ChunkAccess neighboring = level.getChunkAt(new BlockPos((chunkX + 1) << 4, 64, chunkZ << 4));
            ChunkData neighboringData = K5NaturalTreeProbe.data(neighboring);
            ctx.assertTrue(neighboringData == null || neighboringData.positions().isEmpty(),
                    "adjacent chunk has no provenance leakage");
            ServerLevel nether = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            ctx.assertTrue(nether != null, "test server has the vanilla Nether level");
            if (nether != null) {
                ChunkAccess otherDimension = nether.getChunkAt(new BlockPos(chunkX << 4, 64, chunkZ << 4));
                ChunkData dimensionData = K5NaturalTreeProbe.data(otherDimension);
                ctx.assertTrue(dimensionData == null || dimensionData.positions().isEmpty(),
                        "same coordinates in another dimension have independent attachments");
            }
        }
    }

    @GameTest
    public void k5PlayerGrownTwoByTwoSpruceTreeIsNotTagged(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos origin = ctx.absolutePos(new BlockPos(2, 1, 2));
        // mega_pine is the registered feature selected by the spruce TreeGrower. Remove
        // ambient terrain/structures in a fixed clearance volume so placement does not
        // depend on the GameTest runner's randomly assigned world coordinates.
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = 0; y <= 48; y++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                level.setBlock(origin.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 3);
            }
        }

        SaplingBlock sapling = (SaplingBlock) Blocks.SPRUCE_SAPLING;
        var matureSapling = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        BlockPos[] saplingPositions = {origin, origin.east(), origin.south(), origin.east().south()};
        for (BlockPos saplingPos : saplingPositions) {
            level.setBlock(saplingPos, matureSapling, 3);
        }
        for (BlockPos saplingPos : saplingPositions) {
            ctx.assertTrue(level.getBlockState(saplingPos).is(Blocks.SPRUCE_SAPLING)
                            && level.getBlockState(saplingPos.below()).is(Blocks.DIRT),
                    "each of four actual mature spruce saplings is planted on dirt before growth: " + saplingPos);
        }

        K5NaturalTreeProbe.beginTreeGrowerTrace();
        sapling.advanceTree(level, origin, matureSapling, RandomSource.create(0L));

        int generatedLogCount = 0;
        for (int x = -1; x <= 5; x++) {
            for (int z = -1; z <= 5; z++) {
                for (int y = 1; y <= 32; y++) {
                    if (level.getBlockState(origin.offset(x, y - 1, z)).is(net.minecraft.tags.BlockTags.LOGS)) {
                        generatedLogCount++;
                    }
                }
            }
        }
        int baseTrunkLogs = 0;
        for (BlockPos trunkPos : saplingPositions) {
            if (level.getBlockState(trunkPos).is(net.minecraft.tags.BlockTags.LOGS)) baseTrunkLogs++;
        }
        System.out.println("K5_SAPLING_2X2 baseTrunkLogs=" + baseTrunkLogs
                + " generatedLogCount=" + generatedLogCount
                + " featureAttempts=" + K5NaturalTreeProbe.treeGrowerFeatureAttempts());
        ctx.assertTrue(baseTrunkLogs == 4 && generatedLogCount >= 4,
                "the real SaplingBlock.advanceTree -> TreeGrower.growTree path grew a trunk from all four sapling positions");
        ChunkData data = K5NaturalTreeProbe.data(level.getChunkAt(origin));
        ctx.assertTrue(data == null || data.positions().isEmpty(),
                "sapling TreeGrower direct Feature.place calls did not enter natural-biome scope");
        var miner = ctx.makeMockServerPlayerInLevel();
        miner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(miner.gameMode.destroyBlock(origin), "player directly mined a grown spruce trunk log");
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 0,
                "direct mining a player-grown 2x2 tree awards no K5 progress");
        ctx.succeed();
    }

    @GameTest
    public void k5BonemealGrownTreeIsNotTagged(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos soil = ctx.absolutePos(new BlockPos(2, 1, 2));
        BlockPos saplingPos = soil.above();
        level.setBlock(soil, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(saplingPos, Blocks.OAK_SAPLING.defaultBlockState()
                .setValue(SaplingBlock.STAGE, 1), 3);
        var player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL, 64));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(saplingPos),
                net.minecraft.core.Direction.UP, saplingPos, false);

        for (int attempt = 0; attempt < 64 && level.getBlockState(saplingPos).is(Blocks.OAK_SAPLING); attempt++) {
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                    player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        }
        int logs = 0;
        for (int x = -1; x <= 4; x++) {
            for (int z = -1; z <= 4; z++) {
                for (int y = 1; y <= 40; y++) {
                    if (level.getBlockState(saplingPos.offset(x, y, z)).is(net.minecraft.tags.BlockTags.LOGS)) logs++;
                }
            }
        }
        ctx.assertTrue(logs > 0, "vanilla bone-meal use grew a real oak sapling tree");
        ChunkData data = K5NaturalTreeProbe.data(level.getChunkAt(saplingPos));
        ctx.assertTrue(data == null || data.positions().isEmpty(), "bonemeal TreeGrower placement is outside natural decoration");
        ctx.assertTrue(player.gameMode.destroyBlock(saplingPos), "player directly mined a bone-meal-grown log");
        ctx.assertTrue(KnowledgeRuntime.state(player).get("condition.natural_vanilla_logs_mined") == 0,
                "bone-meal-grown trunk awards no K5 progress");
        ctx.succeed();
    }

    @GameTest
    public void k5PlayerPlacedLogIsNotTagged(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos support = ctx.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        var player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG, 1));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(support),
                net.minecraft.core.Direction.UP, support, false);
        var result = player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        BlockPos placedPos = support.above();
        ctx.assertTrue(result.consumesAction() && level.getBlockState(placedPos).is(Blocks.OAK_LOG),
                "ordinary Survival item use placed a real oak log");
        ChunkData data = K5NaturalTreeProbe.data(level.getChunkAt(placedPos));
        ctx.assertTrue(data == null || data.positions().isEmpty(), "a player item placement has no natural worldgen scope");
        ctx.assertTrue(player.gameMode.destroyBlock(placedPos), "player directly mined their placed log");
        ctx.assertTrue(KnowledgeRuntime.state(player).get("condition.natural_vanilla_logs_mined") == 0,
                "placed log cannot award K5 progress");
        ctx.succeed();
    }

    @GameTest
    public void k5PistonMovementConsumesPositionMarkerWithoutMiningCredit(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos log = ctx.absolutePos(new BlockPos(5, 3, 5));
        BlockPos piston = log.east();
        for (int distance = 1; distance <= 3; distance++)
            level.setBlock(log.west(distance), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
        var chunk = level.getChunkAt(log);
        ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) chunk).setAttached(
                K5NaturalTreeProvenance.NATURAL_TREE_LOGS,
                new ChunkData(K5NaturalTreeProvenance.SCHEMA_VERSION, Set.of(log.asLong())));
        var observer = ctx.makeMockServerPlayerInLevel();
        observer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        level.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(
                net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,
                net.minecraft.core.Direction.WEST), 3);
        level.setBlock(piston.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        ctx.runAfterDelay(5, () -> {
            ctx.assertTrue(level.getBlockState(log.west()).is(Blocks.OAK_LOG),
                    "vanilla piston actually moved the marked log");
            ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, log)
                            && !K5NaturalTreeProvenance.hasMarker(level, log.west()),
                    "piston movement neither preserves nor transfers natural provenance");
            ctx.assertTrue(KnowledgeRuntime.state(observer).get("condition.natural_vanilla_logs_mined") == 0,
                    "piston movement cannot grant K5 mining credit");
            ctx.succeed();
        });
    }

    @GameTest
    public void k5ExplosionConsumesPositionMarkerWithoutMiningCredit(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos log = ctx.absolutePos(new BlockPos(5, 3, 5));
        level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
        var chunk = level.getChunkAt(log);
        ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) chunk).setAttached(
                K5NaturalTreeProvenance.NATURAL_TREE_LOGS,
                new ChunkData(K5NaturalTreeProvenance.SCHEMA_VERSION, Set.of(log.asLong())));
        var observer = ctx.makeMockServerPlayerInLevel();
        observer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        level.explode(null, log.getX() + 0.5, log.getY() + 0.5, log.getZ() + 0.5, 4.0F,
                net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        ctx.assertTrue(!level.getBlockState(log).is(net.minecraft.tags.BlockTags.LOGS),
                "vanilla explosion actually destroyed the marked log");
        ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, log),
                "explosion destruction consumes the original position marker");
        ctx.assertTrue(KnowledgeRuntime.state(observer).get("condition.natural_vanilla_logs_mined") == 0,
                "explosion destruction cannot grant direct-mining credit");
        ctx.succeed();
    }

    @GameTest
    public void k5ProgressSurvivesVanillaRespawn(GameTestHelper ctx) {
        var original = ctx.makeMockServerPlayerInLevel();
        original.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        KnowledgeRuntime.recordCondition(original, "natural_vanilla_logs_mined", 37);
        ctx.assertTrue(KnowledgeRuntime.state(original).get("condition.natural_vanilla_logs_mined") == 37,
                "K5 condition is attached to the original server player");
        var respawned = ctx.getLevel().getServer().getPlayerList().respawn(
                original, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        ctx.assertTrue(respawned != original, "vanilla respawn constructed a new server player");
        ctx.assertTrue(KnowledgeRuntime.state(respawned).get("condition.natural_vanilla_logs_mined") == 37,
                "persistent Knowledge attachment copied K5 progress across death");
        ctx.succeed();
    }

    @GameTest
    public void k5StructureTemplateLogHasNoNaturalMiningCredit(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos source = ctx.absolutePos(new BlockPos(2, 3, 2));
        BlockPos structureLog = ctx.absolutePos(new BlockPos(6, 3, 6));
        level.setBlock(source, Blocks.OAK_LOG.defaultBlockState(), 3);
        var template = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
        template.fillFromWorld(level, source, new net.minecraft.core.Vec3i(1, 1, 1), false, java.util.List.of());
        level.setBlock(structureLog, Blocks.AIR.defaultBlockState(), 3);
        boolean placed = template.placeInWorld(level, structureLog, structureLog,
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                RandomSource.create(0L), 3);
        ctx.assertTrue(placed && level.getBlockState(structureLog).is(Blocks.OAK_LOG),
                "Minecraft StructureTemplate placed an actual vanilla log");
        ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, structureLog),
                "structure placement did not inherit natural biome-decoration provenance");
        var miner = ctx.makeMockServerPlayerInLevel();
        miner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(miner.gameMode.destroyBlock(structureLog), "player directly mined the structure log");
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 0,
                "structure-generated log cannot advance K5");
        ctx.succeed();
    }

    @GameTest
    public void k5RegisteredNonVanillaPlacedTreeIsRejectedDuringNaturalDecoration(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(level.getSeed() == 0L, "modded placed-feature fixture uses pinned seed 0");
        var biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        var forest = biomeRegistry.get(Identifier.fromNamespaceAndPath("minecraft", "forest")).orElseThrow();
        var placedRegistry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        var moddedPlaced = placedRegistry.get(Identifier.fromNamespaceAndPath(
                "moreminecrafts-gametest", "k5_untrusted_oak")).orElseThrow();
        var generation = new net.minecraft.world.level.biome.BiomeGenerationSettings.PlainBuilder()
                .addFeature(net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION,
                        moddedPlaced)
                .build();
        var syntheticBiome = new Biome.BiomeBuilder().temperature(0.7F).downfall(0.8F)
                .specialEffects(forest.value().getSpecialEffects())
                .generationSettings(generation).build();
        Holder<Biome> biome = Holder.direct(syntheticBiome);
        var noise = level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                .get(Identifier.fromNamespaceAndPath("minecraft", "overworld")).orElseThrow();
        var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), noise);
        int baseX = 128 << 4;
        int baseZ = 128 << 4;
        ChunkAccess chunk = level.getChunkAt(new BlockPos(baseX + 8, 4, baseZ + 8));
        ctx.assertTrue(chunk.getPos().equals(new ChunkPos(128, 128)), "isolated test chunk is fixed at [128,128]");
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> biome);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            level.setBlock(new BlockPos(baseX + x, 1, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(new BlockPos(baseX + x, 2, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(new BlockPos(baseX + x, 3, baseZ + z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
        ChunkData before = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(before == null || before.positions().isEmpty(), "modded-feature target begins unmarked");
        long scopeCalls = K5NaturalTreeProbe.currentThreadBiomeDecorationCalls();
        generator.applyBiomeDecoration(level, chunk, level.structureManager());
        System.out.println("K5_MODDED_PLACEMENT_SCOPE before=" + scopeCalls
                + " after=" + K5NaturalTreeProbe.currentThreadBiomeDecorationCalls());
        ctx.assertTrue(K5NaturalTreeProbe.currentThreadBiomeDecorationCalls() == scopeCalls + 1,
                "actual natural decoration executed the registered non-vanilla placed feature");
        int placedLogs = 0;
        BlockPos sample = null;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 4; y <= 20; y++) {
            BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
            if (level.getBlockState(pos).is(Blocks.OAK_LOG)) {
                placedLogs++;
                sample = pos;
            }
        }
        ctx.assertTrue(placedLogs > 0, "the registered test-namespace placed feature grew real oak trunks");
        ChunkData after = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(after == null || after.positions().isEmpty(),
                "a registered non-vanilla placed feature cannot create K5 provenance");
        var miner = ctx.makeMockServerPlayerInLevel();
        miner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(miner.gameMode.destroyBlock(sample), "player directly mined a modded-placement tree log");
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 0,
                "modded tree placement gives no K5 credit even for a vanilla oak log");
        ctx.succeed();
    }

    @GameTest
    public void k5PickupAndHopperContainerTransferDoNotCountAsMining(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var recipient = ctx.makeMockServerPlayerInLevel();
        recipient.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var drop = new net.minecraft.world.entity.item.ItemEntity(level,
                recipient.getX(), recipient.getY(), recipient.getZ(),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG, 32));
        drop.setNoPickUpDelay();
        level.addFreshEntity(drop);
        drop.playerTouch(recipient);
        ctx.assertTrue(recipient.getInventory().countItem(net.minecraft.world.item.Items.OAK_LOG) >= 32,
                "ordinary ItemEntity pickup delivered a vanilla log stack");
        ctx.assertTrue(KnowledgeRuntime.state(recipient).get("condition.natural_vanilla_logs_mined") == 0,
                "item pickup cannot advance the direct-mining condition");

        BlockPos hopperPos = ctx.absolutePos(new BlockPos(4, 2, 4));
        BlockPos chestPos = hopperPos.above();
        level.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState(), 3);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var chest = (net.minecraft.world.Container) level.getBlockEntity(chestPos);
        chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG, 16));
        ctx.runAfterDelay(16, () -> {
            var hopper = (net.minecraft.world.Container) level.getBlockEntity(hopperPos);
            ctx.assertTrue(!hopper.getItem(0).isEmpty(), "vanilla hopper actually moved logs out of the chest");
            var extracted = hopper.removeItem(0, 1);
            recipient.getInventory().add(extracted);
            ctx.assertTrue(KnowledgeRuntime.state(recipient).get("condition.natural_vanilla_logs_mined") == 0,
                    "chest and hopper transfers followed by player receipt cannot advance K5");
            ctx.succeed();
        });
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
