package dev.itzgamesgd.moreminecrafts.k5probe;

import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance.ChunkData;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressLookup;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchState;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchStateAccess;
import dev.itzgamesgd.moreminecrafts.progression.ResearchXpAcquisition;
import dev.itzgamesgd.moreminecrafts.k5probe.K5CooperatingMixinProbe;
import java.lang.reflect.Method;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
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
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
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
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;

/** Runtime checks for production K5 provenance, direct mining, and diagnostic lifecycle evidence. */
public final class K5NaturalTreeProbeGameTests implements CustomTestMethodInvoker {
    /** Lowest Y the controlled forest fixtures leave untouched: the floor they place is y 1..3. */
    private static final int FIXTURE_DECORATION_MIN_Y = 4;

    /**
     * Returns a controlled decoration fixture to the state a fresh GameTest world has there: air above the floor and
     * no K5 provenance attachment, in the chunk and the eight neighbours its decoration writes into. The fixture chunks
     * are fixed for seed-0 determinism, so a reused GameTest world otherwise still holds the previous run's trees and
     * markers. A fresh world is unchanged by this.
     */
    static void restoreFreshDecorationVolume(ServerLevel level, ChunkPos center) {
        for (int cx = center.x() - 1; cx <= center.x() + 1; cx++) {
            for (int cz = center.z() - 1; cz <= center.z() + 1; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++)
                    for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++)
                        for (int y = FIXTURE_DECORATION_MIN_Y; y < level.getMaxY(); y++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (!chunk.getBlockState(pos).isAir())
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                        }
                chunk.removeAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS);
            }
        }
    }

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
            if (registeredFeature instanceof TreeFeature || registeredFeature instanceof FallenTreeFeature) {
                Identifier key = featureRegistry.getKey(registeredFeature);
                registeredTreeFeatureIds.add(key);
                if (registeredFeature instanceof TreeFeature && key != null && key.getNamespace().equals("minecraft")) {
                    ctx.assertTrue(K5NaturalTreeProbe.isKnownVanillaTreeFeatureId(key),
                            "every registered vanilla TreeFeature key is in the pinned allowlist: " + key);
                } else if (registeredFeature instanceof FallenTreeFeature && key != null
                        && key.getNamespace().equals("minecraft")) {
                    ctx.assertTrue(K5NaturalTreeProvenance.isKnownVanillaFallenTreeFeatureId(key),
                            "every registered vanilla FallenTreeFeature key is in the pinned allowlist: " + key);
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
        restoreFreshDecorationVolume(level, new ChunkPos(0, 0));
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
        long callsBefore = K5NaturalTreeProbe.currentThreadBiomeDecorationCalls();
        long callbacksBefore = K5NaturalTreeProbe.naturalTreeTrunkCallbacks();
        fixtureGenerator.applyBiomeDecoration(level, chunk, level.structureManager());

        ctx.assertTrue(K5NaturalTreeProbe.currentThreadBiomeDecorationCalls() == callsBefore + 1,
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
        K5CooperatingMixinProbe.reset();
        ctx.assertTrue(miner.gameMode.destroyBlock(first), "Survival player directly mined a natural marked log");
        ctx.assertTrue(K5CooperatingMixinProbe.blockBreakCount() == 1,
                "the GameTest-only destroyBlock wrapper and production mining wrapper both ran once");
        ctx.assertTrue(K5CooperatingMixinProbe.levelMutationCount() > 0,
                "the GameTest-only Level.setBlock wrapper composed with production marker invalidation");
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


    /**
     * Harness regression: the unload the persistence test waits for must stay observable even when an
     * unrelated chunk that also has K5 markers unloads in the same window (the observation used to be a single
     * last-writer-wins slot, so a second unload hid the first and the wait ran into its tick limit).
     */
    @GameTest
    public void k5UnloadObservationSurvivesUnrelatedMarkedChunkUnloads(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        LevelChunk awaited = level.getChunkAt(ctx.absolutePos(new BlockPos(1, 2, 1)));
        LevelChunk unrelated = level.getChunkAt(ctx.absolutePos(new BlockPos(1 + 48, 2, 1)));
        ctx.assertTrue(awaited != unrelated, "two different chunk instances");
        K5NaturalTreeProbe.recordMarkedChunkUnload(level, awaited);
        K5NaturalTreeProbe.recordMarkedChunkUnload(level, unrelated);
        ctx.assertTrue(K5NaturalTreeProbe.observedMarkedChunkUnload(level, awaited.getPos(), awaited),
                "the awaited unload is still observable after an unrelated marked chunk unloaded");
        ctx.assertTrue(K5NaturalTreeProbe.observedMarkedChunkUnload(level, unrelated.getPos(), unrelated),
                "the unrelated unload is observable too");
        ctx.assertTrue(!K5NaturalTreeProbe.observedMarkedChunkUnload(level, awaited.getPos(), unrelated),
                "an observation never matches a different chunk instance");
        ctx.succeed();
    }

    @GameTest(maxTicks = 1250)
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
        private static final BlockPos FURNACE_POS = new BlockPos((CHUNK.x() << 4) + 14, 100,
                (CHUNK.z() << 4) + 14);
        private static final FurnaceResearchState FURNACE_STATE = new FurnaceResearchState(
                java.util.Optional.of(java.util.UUID.fromString("829092e5-bc66-4b78-bafb-0e328995f9ef")),
                0.1875D, 0.8125D);
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
                level.setBlock(FURNACE_POS, Blocks.FURNACE.defaultBlockState(), 3);
                var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)
                        level.getBlockEntity(FURNACE_POS);
                ((FurnaceResearchStateAccess) furnace).moreminecrafts$setResearchState(FURNACE_STATE);
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
                player.setAttached(KnowledgeRuntime.STATE,
                        KnowledgeRuntime.state(player).with("condition.tool_types_crafted", 3)
                                .with("knowledge.14", 1)
                                .with("research.v3.research.64", 3)
                        .with("research.v3.research.06", 2)
                                .with("research.xp_fraction.v3.research.64", 63));
                dev.itzgamesgd.moreminecrafts.furnace.PortableFurnaceRestartFixture.write(ctx, player);
                player.setGameMode(GameType.CREATIVE);
                player.awardStat(net.minecraft.stats.Stats.ITEM_CRAFTED.get(Items.WOODEN_PICKAXE), 12);
                CraftingMenu menu = new CraftingMenu(1, player.getInventory(),
                        ContainerLevelAccess.create(level, ctx.absolutePos(new BlockPos(1, 1, 1))));
                setWoodenPickaxeRecipe(menu);
                ctx.assertTrue(menu.getSlot(0).getItem().is(Items.WOODEN_PICKAXE),
                        "Creative result slot has a real vanilla recipe output");
                menu.clicked(0, 0, ContainerInput.PICKUP, player);
                ctx.assertTrue(player.getStats().getValue(
                                net.minecraft.stats.Stats.ITEM_CRAFTED.get(Items.WOODEN_PICKAXE)) > 12,
                        "Creative crafting persists an ambiguous vanilla crafted statistic");
                ctx.runAfterDelay(110, () -> {
                    try {
                        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == 3
                                        && KnowledgeRuntime.knows(player, 14),
                                "periodic refresh preserves existing K14 progress and unlock after Creative crafting");
                        ctx.assertTrue(KnowledgeRuntime.state(player)
                                        .get("research.xp_fraction.v3.research.64") == 63,
                                "the per-player XP Acquisition fractional remainder is present before disk save");
                        level.getServer().getPlayerList().saveAll();
                        level.getChunkSource().save(true);
                        System.out.println("K5_RESTART_WRITE worldSeed=0 chunk=[64,64] player=" + PLAYER_ID
                                + " k5Progress=38 k14Progress=3 k14Unlocked=true creativeCraftStats="
                                + player.getStats().getValue(
                                        net.minecraft.stats.Stats.ITEM_CRAFTED.get(Items.WOODEN_PICKAXE))
                                + " xpAcquisitionFraction=63 surviving=" + surviving);
                        ctx.succeed();
                    } catch (RuntimeException exception) {
                        throw exception;
                    }
                });
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
                ctx.assertTrue(((FurnaceResearchStateAccess) level.getBlockEntity(FURNACE_POS))
                                .moreminecrafts$getResearchState().equals(FURNACE_STATE),
                        "fresh process restored furnace attribution and fractional state from chunk save");
                ctx.assertTrue(K5NaturalTreeProvenance.hasMarker(level, surviving)
                                && level.getBlockState(surviving).is(net.minecraft.tags.BlockTags.LOGS),
                        "fresh process restored a marker at its real surviving log position");
                ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, removed)
                                && !K5NaturalTreeProvenance.hasMarker(level, replaced)
                                && !K5NaturalTreeProvenance.hasMarker(level, mined),
                        "consumed natural markers did not reappear after process restart");
                ctx.assertTrue(level.getBlockState(replaced).is(Blocks.OAK_LOG),
                        "replacement log exists after reload without inherited provenance");
                ctx.assertTrue(ResearchProgressLookup.researchLevel(level.getServer(), PLAYER_ID,
                                "v3.research.06") == 2,
                        "server-thread offline lookup reads the purchased research level directly from player data");
                dev.itzgamesgd.moreminecrafts.furnace.PortableFurnaceRestartFixture.readOffline(ctx, PLAYER_ID);
                var player = player(true);
                ctx.assertTrue(KnowledgeRuntime.state(player).get("condition.natural_vanilla_logs_mined") == 38,
                        "Minecraft player data restored K5 progress into a fresh player instance");
                ctx.assertTrue(!KnowledgeRuntime.knows(player, 5), "K5 remains locked below 200 after restart");
                ctx.assertTrue(player.getStats().getValue(
                                net.minecraft.stats.Stats.ITEM_CRAFTED.get(Items.WOODEN_PICKAXE)) > 12,
                        "ambiguous Creative-crafted statistics remain present after restart");
                ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("tool_types_crafted") == 3
                                && KnowledgeRuntime.knows(player, 14),
                        "fresh-process JOIN refresh preserves K14 progress and unlock without importing stats");
                ctx.assertTrue(KnowledgeRuntime.state(player)
                                .get("research.xp_fraction.v3.research.64") == 63,
                        "fresh process reloads the per-player XP Acquisition fractional remainder");
                ctx.assertTrue(ResearchXpAcquisition.adjustedAmount(player, 5) == 6
                                && KnowledgeRuntime.state(player)
                                        .get("research.xp_fraction.v3.research.64") == 8,
                        "the reloaded remainder contributes to the next receiver's XP Acquisition award");
                System.out.println("K5_RESTART_READ chunk=[64,64] player=" + PLAYER_ID
                        + " k5Progress=38 k14Progress=3 k14Unlocked=true xpAcquisitionFraction=63"
                        + " nextXpAward=6 nextFraction=8 surviving=" + surviving);
                ctx.succeed();
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("could not read restart fixture manifest", exception);
            }
        }

        private void setWoodenPickaxeRecipe(CraftingMenu menu) {
            menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS));
            menu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS));
            menu.getSlot(3).set(new ItemStack(Items.OAK_PLANKS));
            menu.getSlot(5).set(new ItemStack(Items.STICK));
            menu.getSlot(8).set(new ItemStack(Items.STICK));
            menu.slotsChanged(menu.getSlot(1).container);
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
        private final BlockPos furnacePos = new BlockPos((chunkX << 4) + 15, 20, (chunkZ << 4) + 15);
        private final FurnaceResearchState furnaceState = new FurnaceResearchState(
                java.util.Optional.of(java.util.UUID.fromString("f66dbb41-2601-4c71-a5df-001c07f4b213")),
                0.375D, 0.625D);
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
            level.setBlock(furnacePos, Blocks.FURNACE.defaultBlockState(), 3);
            var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)
                    level.getBlockEntity(furnacePos);
            ((FurnaceResearchStateAccess) furnace).moreminecrafts$setResearchState(furnaceState);
            long savesBefore = K5NaturalTreeProbe.savedChunksWithMarkers();
            level.getChunkSource().save(true);
            ctx.assertTrue(K5NaturalTreeProbe.savedChunksWithMarkers() > savesBefore,
                    "ServerChunkCache.save(true) reached ChunkMap.save for a marked chunk");
            K5NaturalTreeProbe.clearMarkedChunkUnloads(level, targetPos);
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
                var restoredFurnace = (FurnaceResearchStateAccess) level.getBlockEntity(furnacePos);
                ctx.assertTrue(restoredFurnace.moreminecrafts$getResearchState().equals(furnaceState),
                        "real chunk unload and reload restored furnace attribution and both fractional remainders");
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
                K5NaturalTreeProbe.clearMarkedChunkUnloads(level, targetPos);
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
                ctx.assertTrue(((FurnaceResearchStateAccess) level.getBlockEntity(furnacePos))
                                .moreminecrafts$getResearchState().equals(furnaceState),
                        "a second chunk unload and reload retains furnace persistent state");
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
        var registries = level.registryAccess();
        var biomeRegistry = registries.lookupOrThrow(Registries.BIOME);
        Holder<Biome> biome = biomeRegistry.get(Identifier.fromNamespaceAndPath(
                "moreminecrafts-gametest", "k5_untrusted_oak_fixture")).orElseThrow();
        var placedRegistry = registries.lookupOrThrow(Registries.PLACED_FEATURE);
        var moddedPlaced = placedRegistry.get(Identifier.fromNamespaceAndPath(
                "moreminecrafts-gametest", "k5_untrusted_oak")).orElseThrow();
        boolean fixtureContainsPlacement = false;
        for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
            for (Holder<PlacedFeature> placed : step) {
                if (placed.value() == moddedPlaced.value()) fixtureContainsPlacement = true;
            }
        }
        ctx.assertTrue(fixtureContainsPlacement,
                "registered test biome contains the exact registered test-namespace oak placement");

        var noise = registries.lookupOrThrow(Registries.NOISE_SETTINGS)
                .get(Identifier.fromNamespaceAndPath("minecraft", "overworld")).orElseThrow();
        var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), noise);
        // Do not share chunk [128,128] with the positive fallen-tree provenance GameTest.
        // Its real natural markers can persist into this negative fixture between tests.
        int baseX = 160 << 4;
        int baseZ = 160 << 4;
        ChunkAccess chunk = level.getChunkAt(new BlockPos(baseX + 8, 4, baseZ + 8));
        ctx.assertTrue(chunk.getPos().equals(new ChunkPos(160, 160)),
                "isolated negative-provenance fixture uses distinct chunk [160,160]");
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> biome);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            level.setBlock(new BlockPos(baseX + x, 1, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(new BlockPos(baseX + x, 2, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(new BlockPos(baseX + x, 3, baseZ + z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }

        ChunkData before = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(before == null || before.positions().isEmpty(),
                "modded-feature target begins unmarked");
        long scopeCalls = K5NaturalTreeProbe.currentThreadBiomeDecorationCalls();
        generator.applyBiomeDecoration(level, chunk, level.structureManager());
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
        ctx.assertTrue(placedLogs > 0,
                "the registered test-namespace placed feature grew real oak trunks");
        ChunkData after = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(after == null || after.positions().isEmpty(),
                "a registered non-vanilla placed feature cannot create K5 provenance");

        var miner = ctx.makeMockServerPlayerInLevel();
        miner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(miner.gameMode.destroyBlock(sample),
                "player directly mined a modded-placement tree log");
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 0,
                "modded tree placement gives no K5 credit even for a vanilla oak log");
        ctx.succeed();
    }

    @GameTest
    public void k5RegisteredNonVanillaFallenTreeFeatureRemainsUnmarked(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(level.getSeed() == 0L, "registered modded-fallen fixture uses pinned seed 0");
        var registries = level.registryAccess();
        var biomes = registries.lookupOrThrow(Registries.BIOME);
        Holder<Biome> biome = biomes.get(Identifier.fromNamespaceAndPath(
                "moreminecrafts-gametest", "k5_untrusted_fallen_fixture")).orElseThrow();
        var placedRegistry = registries.lookupOrThrow(Registries.PLACED_FEATURE);
        Identifier placedId = Identifier.fromNamespaceAndPath("moreminecrafts-gametest", "k5_untrusted_fallen");
        PlacedFeature placed = placedRegistry.get(placedId).orElseThrow().value();
        var featureRegistry = registries.lookupOrThrow(Registries.FEATURE);
        Feature configured = featureRegistry.get(Identifier.fromNamespaceAndPath(
                "moreminecrafts-gametest", "k5_untrusted_fallen")).orElseThrow().value();
        ctx.assertTrue(configured instanceof FallenTreeFeature,
                "the non-vanilla registry entry is a genuine registered FallenTreeFeature instance");
        ctx.assertTrue(!K5NaturalTreeProvenance.isKnownVanillaFallenTreeFeatureId(featureRegistry.getKey(configured)),
                "the non-vanilla FallenTreeFeature identity is outside the allowlist");
        var noise = registries.lookupOrThrow(Registries.NOISE_SETTINGS)
                .get(Identifier.fromNamespaceAndPath("minecraft", "overworld")).orElseThrow();
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome), noise);
        ChunkPos fixtureChunkPos = new ChunkPos(140, 140);
        int baseX = fixtureChunkPos.getMinBlockX();
        int baseZ = fixtureChunkPos.getMinBlockZ();
        ChunkAccess chunk = level.getChunkAt(new BlockPos(baseX + 8, 4, baseZ + 8));
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> biome);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            level.setBlock(new BlockPos(baseX + x, 1, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(new BlockPos(baseX + x, 2, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(new BlockPos(baseX + x, 3, baseZ + z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
        long decorationBefore = K5NaturalTreeProbe.currentThreadBiomeDecorationCalls();
        generator.applyBiomeDecoration(level, chunk, level.structureManager());
        ctx.assertTrue(K5NaturalTreeProbe.currentThreadBiomeDecorationCalls() == decorationBefore + 1,
                "the registered non-vanilla fallen feature runs inside natural biome-decoration scope");
        var runs = K5NaturalTreeProbe.lastNaturalTreeFeaturePlacements().stream()
                .filter(run -> placedId.equals(run.placedFeatureId())
                        && featureRegistry.getKey(configured).equals(run.featureId())
                        && !run.successfulLogPositions().isEmpty())
                .toList();
        ctx.assertTrue(!runs.isEmpty(),
                "the genuine registered non-vanilla FallenTreeFeature successfully writes real log states");
        var sample = runs.stream().flatMap(run -> run.successfulLogPositions().stream())
                .map(BlockPos::of).filter(pos -> level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LOGS))
                .findFirst().orElseThrow();
        ChunkData markerData = K5NaturalTreeProbe.data(chunk);
        ctx.assertTrue(markerData == null || markerData.positions().isEmpty(),
                "a non-vanilla FallenTreeFeature receives no production natural-log markers");
        var miner = ctx.makeMockServerPlayerInLevel();
        miner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(miner.gameMode.destroyBlock(sample),
                "Survival directly mines a real log from the registered non-vanilla fallen tree");
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 0,
                "non-vanilla FallenTreeFeature logs cannot grant K5 credit");
        System.out.println("K5_UNTRUSTED_FALLEN_TEST biome=" + biomes.getKey(biome.value())
                + " placedFeature=" + placedId + " feature=" + featureRegistry.getKey(configured)
                + " chunk=" + fixtureChunkPos + " generatedLog=" + sample + " markerCount=0 progress=0");
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

    @GameTest
    public void k5NestedUntrustedPlacedFeatureCannotInheritVanillaIdentity(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var registries = level.registryAccess();
        var placedRegistry = registries.lookupOrThrow(Registries.PLACED_FEATURE);
        var vanilla = placedRegistry.get(Identifier.fromNamespaceAndPath(
                "minecraft", "trees_birch_and_oak_leaf_litter")).orElseThrow();
        var untrusted = placedRegistry.get(Identifier.fromNamespaceAndPath(
                "moreminecrafts-gametest", "k5_untrusted_oak")).orElseThrow();
        Feature oak = registries.lookupOrThrow(Registries.FEATURE)
                .get(Identifier.fromNamespaceAndPath("minecraft", "oak_bees_0002_leaf_litter")).orElseThrow().value();
        BlockPos pos = ctx.absolutePos(new BlockPos(3, 4, 3));
        level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);

        K5NaturalTreeProvenance.enterNaturalBiomeDecoration(registries);
        var vanillaScope = K5NaturalTreeProvenance.enterPlacedFeature(vanilla.value());
        var untrustedScope = K5NaturalTreeProvenance.enterPlacedFeature(untrusted.value());
        try {
            ctx.assertTrue(vanillaScope.eligible() && !untrustedScope.eligible(),
                    "nested placement identities are individually classified");
            K5NaturalTreeProvenance.recordTrunkWrites((Feature) oak, level,
                    java.util.List.of(new K5NaturalTreeProvenance.TrunkWrite(pos, level.getBlockState(pos))), true);
        } finally {
            K5NaturalTreeProvenance.exitPlacedFeature(untrustedScope);
            K5NaturalTreeProvenance.exitPlacedFeature(vanillaScope);
            K5NaturalTreeProvenance.exitNaturalBiomeDecoration();
        }
        ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, pos),
                "an untrusted nested placed feature cannot inherit the outer vanilla feature identity");
        ctx.succeed();
    }

    @GameTest
    public void k5NaturalVanillaFallenTreeFeaturesUseExplicitFailClosedAllowlist(GameTestHelper ctx) {
        var registries = ctx.getLevel().registryAccess();
        var biomeRegistry = registries.lookupOrThrow(Registries.BIOME);
        var placedRegistry = registries.lookupOrThrow(Registries.PLACED_FEATURE);
        var featureRegistry = registries.lookupOrThrow(Registries.FEATURE);
        Set<String> naturalUses = new TreeSet<>();
        Set<Identifier> vanillaFallenFeatureIds = new TreeSet<>();
        Set<Identifier> nonVanillaFallenFeatureIds = new TreeSet<>();
        for (Biome biome : biomeRegistry) {
            for (HolderSet<PlacedFeature> step : biome.getGenerationSettings().features()) {
                for (Holder<PlacedFeature> placedHolder : step) {
                    PlacedFeature placed = placedHolder.value();
                    for (Holder<Feature> featureHolder : placed.getFeatures().toList()) {
                        Feature feature = featureHolder.value();
                        if (feature instanceof FallenTreeFeature) {
                            Identifier biomeId = biomeRegistry.getKey(biome);
                            Identifier placedId = placedRegistry.getKey(placed);
                            Identifier featureId = featureRegistry.getKey(feature);
                            naturalUses.add(biomeId + " -> " + placedId + " -> " + featureId);
                            if (featureId.getNamespace().equals("minecraft")) vanillaFallenFeatureIds.add(featureId);
                            else nonVanillaFallenFeatureIds.add(featureId);
                        }
                    }
                }
            }
        }
        System.out.println("K5_FALLEN_TREE_NATURAL_BIOME_USES " + naturalUses);
        ctx.assertTrue(!naturalUses.isEmpty(), "vanilla biome decoration registers natural fallen-tree feature variants");
        for (Identifier featureId : vanillaFallenFeatureIds) {
            ctx.assertTrue(K5NaturalTreeProvenance.isKnownVanillaFallenTreeFeatureId(featureId),
                    "each runtime-observed natural FallenTreeFeature ID is allowlisted: " + featureId);
            ctx.assertTrue(K5NaturalTreeProvenance.isEligibleTreeIdentity(
                            Identifier.fromNamespaceAndPath("minecraft", "trees_birch"), featureId, true),
                    "a registered natural fallen-tree feature passes the placed-feature identity gate: " + featureId);
        }
        Identifier testNamespaceFallen = Identifier.fromNamespaceAndPath(
                "moreminecrafts-gametest", "k5_untrusted_fallen");
        ctx.assertTrue(nonVanillaFallenFeatureIds.contains(testNamespaceFallen),
                "registry scan observes the genuine test-namespace FallenTreeFeature control");
        for (Identifier featureId : nonVanillaFallenFeatureIds) {
            ctx.assertTrue(!K5NaturalTreeProvenance.isKnownVanillaFallenTreeFeatureId(featureId),
                    "non-vanilla FallenTreeFeature identities are outside the allowlist: " + featureId);
        }
        ctx.assertTrue(!K5NaturalTreeProvenance.isEligibleTreeIdentity(
                        Identifier.fromNamespaceAndPath("minecraft", "trees_birch"),
                        Identifier.fromNamespaceAndPath("minecraft", "fallen_unknown_tree"), true),
                "unknown fallen-tree feature IDs remain fail-closed");
        ctx.succeed();
    }

    @GameTest
    public void k5ControlledNaturalDecorationGeneratesAndCreditsRegisteredFallenTree(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ctx.assertTrue(level.getSeed() == 0L, "fallen-tree fixture uses the pinned GameTest seed 0");

        var registries = level.registryAccess();
        var biomes = registries.lookupOrThrow(Registries.BIOME);
        Holder<Biome> forest = biomes.get(Identifier.fromNamespaceAndPath("minecraft", "forest")).orElseThrow();
        var placedRegistry = registries.lookupOrThrow(Registries.PLACED_FEATURE);
        PlacedFeature naturalFallenPlacement = placedRegistry.get(Identifier.fromNamespaceAndPath(
                "minecraft", "trees_birch_and_oak_leaf_litter")).orElseThrow().value();
        ctx.assertTrue(naturalFallenPlacement.getFeatures().anyMatch(feature ->
                        feature.value() instanceof FallenTreeFeature
                                && K5NaturalTreeProvenance.isKnownVanillaFallenTreeFeatureId(
                                        registries.lookupOrThrow(Registries.FEATURE).getKey(feature.value()))),
                "registered vanilla forest placed feature contains an allowlisted FallenTreeFeature variant");
        var noise = registries.lookupOrThrow(Registries.NOISE_SETTINGS)
                .get(Identifier.fromNamespaceAndPath("minecraft", "overworld")).orElseThrow();
        NoiseBasedChunkGenerator fixtureGenerator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(forest), noise);

        ChunkPos fixtureChunkPos = new ChunkPos(128, 128);
        int baseX = fixtureChunkPos.getMinBlockX();
        int baseZ = fixtureChunkPos.getMinBlockZ();
        BlockPos chunkCenter = new BlockPos(baseX + 8, 4, baseZ + 8);
        restoreFreshDecorationVolume(level, fixtureChunkPos);
        ChunkAccess chunk = level.getChunkAt(chunkCenter);
        ctx.assertTrue(chunk.getPos().equals(fixtureChunkPos), "fixture uses the pinned chunk [128,128]");
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ) -> forest);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            level.setBlock(new BlockPos(baseX + x, 1, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(new BlockPos(baseX + x, 2, baseZ + z), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(new BlockPos(baseX + x, 3, baseZ + z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }

        long decorationCallsBefore = K5NaturalTreeProbe.currentThreadBiomeDecorationCalls();
        Identifier forcedFeatureId = Identifier.fromNamespaceAndPath("minecraft", "fallen_birch_tree");
        K5NaturalTreeProbe.armDeterministicFallenBranch(forcedFeatureId,
                registries.lookupOrThrow(Registries.FEATURE));
        try {
            fixtureGenerator.applyBiomeDecoration(level, chunk, level.structureManager());
        } finally {
            K5NaturalTreeProbe.disarmDeterministicFallenBranch();
        }
        ctx.assertTrue(K5NaturalTreeProbe.currentThreadBiomeDecorationCalls() == decorationCallsBefore + 1,
                "fixture enters the production ChunkGenerator.applyBiomeDecoration scope exactly once");
        ctx.assertTrue(K5NaturalTreeProbe.forcedFallenBranchCount() == 1,
                "one deterministic random-selector value selected the first registered fallen-birch branch");
        ctx.assertTrue(!K5NaturalTreeProbe.isNaturalBiomeDecoration(),
                "production natural-decoration scope closes after the worldgen call");

        Set<Identifier> knownFallenIds = Set.of(
                Identifier.fromNamespaceAndPath("minecraft", "fallen_oak_tree"),
                Identifier.fromNamespaceAndPath("minecraft", "fallen_birch_tree"),
                Identifier.fromNamespaceAndPath("minecraft", "fallen_super_birch_tree"),
                Identifier.fromNamespaceAndPath("minecraft", "fallen_spruce_tree"),
                Identifier.fromNamespaceAndPath("minecraft", "fallen_jungle_tree"),
                Identifier.fromNamespaceAndPath("minecraft", "fallen_poplar_tree"));
        var featureRuns = K5NaturalTreeProbe.lastNaturalTreeFeaturePlacements();
        var fallenRuns = featureRuns.stream()
                .filter(run -> knownFallenIds.contains(run.featureId()) && !run.successfulLogPositions().isEmpty())
                .toList();
        ChunkData persisted = K5NaturalTreeProbe.data(chunk);
        Set<Long> markers = persisted == null ? Set.of() : persisted.positions();
        var fallenLogPositions = fallenRuns.stream().flatMap(run -> run.successfulLogPositions().stream())
                .filter(markers::contains).map(BlockPos::of).distinct().toList();
        var placedFeatureIds = Set.of(placedRegistry.getKey(naturalFallenPlacement));
        System.out.println("K5_FALLEN_TREE_POSITIVE seed=" + level.getSeed()
                + " biome=minecraft:forest controlledRandomSelector=firstChanceValue0.0"
                + " dimension=" + level.dimension().identifier() + " chunk=" + fixtureChunkPos
                + " placedFeatureIds=" + placedFeatureIds
                + " actualFeatureRuns=" + featureRuns
                + " successfulFallenRuns=" + fallenRuns
                + " markerCount=" + markers.size() + " fallenMarkedLogs=" + fallenLogPositions);
        ctx.assertTrue(!fallenRuns.isEmpty(),
                "natural decoration invoked an actual registered FallenTreeFeature and changed log blocks");
        ctx.assertTrue(!fallenLogPositions.isEmpty(),
                "production provenance marked actual log positions written by FallenTreeFeature");
        ctx.assertTrue(fallenRuns.stream().allMatch(run -> placedFeatureIds.contains(run.placedFeatureId())),
                "observed feature runs belong to exact registered vanilla placed-feature identities");

        BlockPos minedPos = fallenLogPositions.get(0);
        var originalLog = level.getBlockState(minedPos);
        ctx.assertTrue(originalLog.is(net.minecraft.tags.BlockTags.LOGS),
                "recorded fallen-tree position independently contains a real log block");
        ctx.assertTrue(level.getChunkAt(minedPos) == chunk && K5NaturalTreeProvenance.hasMarker(level, minedPos),
                "the marker is attached to this world and fixture chunk at the real log position");
        var miner = ctx.makeMockServerPlayerInLevel();
        miner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        int progressBefore = KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined");
        ctx.assertTrue(progressBefore == 0,
                "fallen-tree miner starts with zero K5 progress");
        ctx.assertTrue(miner.gameMode.destroyBlock(minedPos), "Survival player directly mines generated fallen-tree log");
        ctx.assertTrue(KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined") == 1,
                "successful direct mining of one generated fallen log grants exactly one K5 point");
        ctx.assertTrue(!K5NaturalTreeProvenance.containsMarker(level, minedPos),
                "successful mining consumes the original fallen-log marker");
        miner.gameMode.destroyBlock(minedPos);
        level.setBlock(minedPos, originalLog, 3);
        boolean replacementHasMarker = K5NaturalTreeProvenance.hasMarker(level, minedPos);
        ctx.assertTrue(!K5NaturalTreeProvenance.hasMarker(level, minedPos),
                "a replacement log at the consumed fallen-tree position remains unmarked");
        miner.gameMode.destroyBlock(minedPos);
        int progressAfterRepeat = KnowledgeRuntime.state(miner).get("condition.natural_vanilla_logs_mined");
        ctx.assertTrue(progressAfterRepeat == 1,
                "repeat mining and replacement cannot credit the same source position twice");
        System.out.println("K5_FALLEN_TREE_MINING position=" + minedPos + " initialMarker=true"
                + " progress=" + progressBefore + "->1 repeatAndReplacementProgress=" + progressAfterRepeat
                + " replacementMarker=" + replacementHasMarker);
        ctx.succeed();
    }

    @GameTest
    public void k5HighDensityProvenanceMicrobenchmarkPreservesCountsAndCodec(GameTestHelper ctx) throws Exception {
        ServerLevel level = ctx.getLevel();
        var registries = level.registryAccess();
        var placed = registries.lookupOrThrow(Registries.PLACED_FEATURE)
                .get(Identifier.fromNamespaceAndPath("minecraft", "trees_birch_and_oak_leaf_litter")).orElseThrow();
        var tree = registries.lookupOrThrow(Registries.FEATURE)
                .get(Identifier.fromNamespaceAndPath("minecraft", "oak_bees_0002_leaf_litter")).orElseThrow();
        ChunkAccess chunk = level.getChunkAt(ctx.absolutePos(new BlockPos(2, 2, 2)));
        int[] sizes = {100, 500, 1_000};
        for (int size : sizes) {
            ArrayList<BlockPos> positions = new ArrayList<>(size);
            ArrayList<K5NaturalTreeProvenance.TrunkWrite> writes = new ArrayList<>(size);
            int minX = chunk.getPos().getMinBlockX();
            int minZ = chunk.getPos().getMinBlockZ();
            int baseY = level.getMinY() + 48;
            for (int i = 0; i < size; i++) {
                BlockPos pos = new BlockPos(minX + (i & 15), baseY + (i >> 8), minZ + ((i >> 4) & 15));
                chunk.setBlockState(pos, Blocks.OAK_LOG.defaultBlockState(), 0);
                positions.add(pos);
                writes.add(new K5NaturalTreeProvenance.TrunkWrite(pos, Blocks.OAK_LOG.defaultBlockState()));
            }
            ArrayList<Long> insertTimes = new ArrayList<>();
            ArrayList<Long> lookupTimes = new ArrayList<>();
            ArrayList<Long> consumeTimes = new ArrayList<>();
            ArrayList<Long> codecTimes = new ArrayList<>();
            int encodedBytes = 0;
            for (int sample = 0; sample < 4; sample++) {
                chunk.removeAttached(K5NaturalTreeProvenance.NATURAL_TREE_LOGS);
                long started = System.nanoTime();
                K5NaturalTreeProvenance.enterNaturalBiomeDecoration(registries);
                var featureScope = K5NaturalTreeProvenance.enterPlacedFeature(placed.value());
                try {
                    K5NaturalTreeProvenance.recordTrunkWrites((Feature) tree.value(), level, writes, true);
                } finally {
                    K5NaturalTreeProvenance.exitPlacedFeature(featureScope);
                    K5NaturalTreeProvenance.exitNaturalBiomeDecoration();
                }
                long insertElapsed = System.nanoTime() - started;
                ChunkData data = K5NaturalTreeProvenance.data(chunk);
                if (sample == 0) {
                    System.out.println("K5_DENSITY_DIAGNOSTIC source=" + registries.lookupOrThrow(Registries.FEATURE).getKey(tree.value())
                            + " placed=" + registries.lookupOrThrow(Registries.PLACED_FEATURE).getKey(placed.value())
                            + " eligible=" + K5NaturalTreeProvenance.isEligibleTreeIdentity(
                                    registries.lookupOrThrow(Registries.PLACED_FEATURE).getKey(placed.value()),
                                    registries.lookupOrThrow(Registries.FEATURE).getKey(tree.value()), true)
                            + " chunkTarget=" + (chunk instanceof net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)
                            + " markerCount=" + (data == null ? -1 : data.positions().size()));
                }
                ctx.assertTrue(data != null && data.positions().size() == size,
                        "all synthetic marked log positions are retained at density " + size);
                if (sample > 0) insertTimes.add(insertElapsed);

                started = System.nanoTime();
                int foundMarkers = 0;
                for (BlockPos pos : positions) if (K5NaturalTreeProvenance.hasMarker(level, pos)) foundMarkers++;
                ctx.assertTrue(foundMarkers == size, "every dense marker is found at density " + size);
                long lookupElapsed = System.nanoTime() - started;
                if (sample > 0) lookupTimes.add(lookupElapsed);

                started = System.nanoTime();
                var encoded = K5NaturalTreeProvenance.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                NbtIo.writeAnyTag(encoded, new DataOutputStream(output));
                encodedBytes = output.size();
                var restored = K5NaturalTreeProvenance.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
                long codecElapsed = System.nanoTime() - started;
                ctx.assertTrue(restored.positions().equals(data.positions()),
                        "persistent attachment codec round trip retains every marker at density " + size);
                if (sample > 0) codecTimes.add(codecElapsed);

                started = System.nanoTime();
                int consumedMarkers = 0;
                for (BlockPos pos : positions) if (K5NaturalTreeProvenance.invalidatePosition(chunk, pos)) consumedMarkers++;
                long consumeElapsed = System.nanoTime() - started;
                ctx.assertTrue(consumedMarkers == size && K5NaturalTreeProvenance.data(chunk) == null,
                        "all dense markers are consumed at density " + size);
                if (sample > 0) consumeTimes.add(consumeElapsed);
            }
            long insertMedian = median(insertTimes);
            long lookupMedian = median(lookupTimes);
            long consumeMedian = median(consumeTimes);
            long codecMedian = median(codecTimes);
            System.out.println("K5_DENSITY_BENCHMARK markers=" + size + " insertMedianNs=" + insertMedian
                    + " lookupBatchMedianNs=" + lookupMedian + " consumeMedianNs=" + consumeMedian
                    + " codecRoundTripMedianNs=" + codecMedian
                    + " nbtPayloadBytes=" + encodedBytes + " repetitions=3 warmup=1");
        }
        ctx.succeed();
    }

    private static long median(ArrayList<Long> values) {
        ArrayList<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    /** Leftovers of an earlier run in a reused world are removed; the floor below the fixture is kept. */
    @GameTest
    public void k5FixtureResetRemovesLeftoverDecorationAndMarkers(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ChunkPos center = new ChunkPos(-300, -300);
        LevelChunk middle = level.getChunk(center.x(), center.z());
        LevelChunk neighbour = level.getChunk(center.x() + 1, center.z() - 1);
        BlockPos log = new BlockPos(center.getMinBlockX() + 5, 5, center.getMinBlockZ() + 5);
        BlockPos leaves = new BlockPos(neighbour.getPos().getMinBlockX() + 3, 10, neighbour.getPos().getMinBlockZ() + 3);
        BlockPos floor = new BlockPos(center.getMinBlockX() + 5, 3, center.getMinBlockZ() + 5);
        BlockPos outside = new BlockPos(center.getMinBlockX() + 40, 6, center.getMinBlockZ() + 5);
        level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
        level.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState(), 3);
        level.setBlock(floor, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(outside, Blocks.OAK_LOG.defaultBlockState(), 3);
        middle.setAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS, new ChunkData(1, Set.of(log.asLong())));
        neighbour.setAttached(K5NaturalTreeProbe.NATURAL_TREE_LOGS, new ChunkData(1, Set.of(leaves.asLong())));

        restoreFreshDecorationVolume(level, center);

        ctx.assertTrue(level.getBlockState(log).isAir() && level.getBlockState(leaves).isAir(),
                "decoration blocks above the floor in the chunk and its neighbours are cleared");
        ctx.assertTrue(K5NaturalTreeProbe.data(middle) == null && K5NaturalTreeProbe.data(neighbour) == null,
                "provenance markers left in the fixture chunk and a neighbour are removed");
        ctx.assertTrue(level.getBlockState(floor).is(Blocks.GRASS_BLOCK),
                "the floor below the fixture's decoration volume is kept");
        ctx.assertTrue(level.getBlockState(outside).is(Blocks.OAK_LOG),
                "blocks outside the 3x3 chunk neighbourhood are untouched");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
