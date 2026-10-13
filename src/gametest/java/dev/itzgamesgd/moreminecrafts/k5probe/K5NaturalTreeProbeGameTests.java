package dev.itzgamesgd.moreminecrafts.k5probe;

import dev.itzgamesgd.moreminecrafts.k5probe.K5NaturalTreeProbe.ChunkData;
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

/** Runtime-only checks for the diagnostic hooks. These tests never touch Knowledge state. */
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
        new ChunkPersistenceRun(ctx).start();
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
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
