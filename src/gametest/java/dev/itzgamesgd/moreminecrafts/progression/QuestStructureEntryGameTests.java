package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

/** C2 unique-structure entry: real StructureStart/StructureManager queries, per-player persisted evidence. */
public final class QuestStructureEntryGameTests implements CustomTestMethodInvoker {
    private static final String TYPES = QuestStructureEntry.TYPES;
    private static final String CITIES = QuestStructureEntry.END_CITIES;
    private static final List<String> VILLAGES = List.of("village_plains", "village_desert", "village_savanna",
            "village_snowy", "village_taiga");
    private static final List<String> GRAND = List.of("stronghold", "ancient_city", "monument", "mansion", "trial_chambers");

    private static int seen(ServerPlayer player, String key) {
        return QuestRules.distinctCount(KnowledgeRuntime.state(player), key);
    }

    private static boolean complete(ServerPlayer player, String quest) {
        return KnowledgeRuntime.state(player).questComplete(quest);
    }

    private static void stand(ServerPlayer player, BlockPos pos) {
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        QuestStructureEntry.observe(player);
    }

    /** Each call uses its own chunk column so tests never share structures. */
    private static ChunkPos chunk(int slot) { return new ChunkPos(300 + slot * 12, 300); }

    @GameTest
    public void entryNeedsAPieceNotTheBoundingBoxChunkOrAltitude(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var village = QuestStructureFixture.register(level, "minecraft:village_plains", chunk(0), new BlockPos(48, 0, 0));
        var box = village.start().getBoundingBox();
        BlockPos gap = new BlockPos((village.pieces().get(0).maxX() + village.pieces().get(1).minX()) / 2,
                village.inside().getY(), village.inside().getZ());
        ctx.assertTrue(box.isInside(gap) && village.pieces().stream().noneMatch(p -> p.isInside(gap)),
                "gap is inside the structure bounding box but in no piece");
        stand(player, gap);
        ctx.assertTrue(seen(player, TYPES) == 0, "bounding-box gap is not entry");
        stand(player, village.inside().above(80));
        ctx.assertTrue(seen(player, TYPES) == 0, "flying far above the piece is not entry");
        stand(player, new BlockPos(chunk(0).getMinBlockX() + 1, village.inside().getY(), chunk(0).getMinBlockZ() + 1));
        ctx.assertTrue(seen(player, TYPES) == 0, "same chunk but outside every piece is not entry");
        stand(player, village.inside());
        ctx.assertTrue(seen(player, TYPES) == 1, "standing in a piece records the structure type");
        ServerPlayer bystander = ctx.makeMockServerPlayerInLevel();
        QuestStructureEntry.observe(bystander);
        ctx.assertTrue(seen(bystander, TYPES) == 0, "a player who stayed outside has nothing");
        ctx.succeed();
    }

    @GameTest
    public void endCitiesAreDistinctByStartChunkAndRepeatsDoNotCount(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var first = QuestStructureFixture.register(level, "minecraft:end_city", chunk(1), new BlockPos(20, 0, 0));
        var second = QuestStructureFixture.register(level, "minecraft:end_city", chunk(2));
        var third = QuestStructureFixture.register(level, "minecraft:end_city", chunk(3));
        var notACity = QuestStructureFixture.register(level, "minecraft:village_plains", chunk(4));
        stand(player, notACity.inside());
        ctx.assertTrue(seen(player, CITIES) == 0, "another structure type is not an end city");
        stand(player, first.inside());
        stand(player, second.inside());
        stand(player, first.inside());
        stand(player, new BlockPos(first.pieces().get(1).minX() + 1, first.inside().getY(), first.inside().getZ() + 1));
        ctx.assertTrue(seen(player, CITIES) == 2 && !complete(player, "xpq.dim.end_cities_3"),
                "re-entering a city and entering another piece of it never adds a third city");
        stand(player, third.inside());
        ctx.assertTrue(seen(player, CITIES) == 3 && complete(player, "xpq.dim.end_cities_3"),
                "a genuinely different third city completes the quest (teleport-style jump included)");
        ctx.succeed();
    }

    @GameTest
    public void endCityIdentityIncludesTheDimension(GameTestHelper ctx) {
        ServerLevel overworld = ctx.getLevel();
        ServerLevel end = overworld.getServer().getLevel(Level.END);
        ctx.assertTrue(end != null, "the End level is loaded");
        ChunkPos shared = chunk(5);
        var inOverworld = QuestStructureFixture.register(overworld, "minecraft:end_city", shared);
        var inEnd = QuestStructureFixture.register(end, "minecraft:end_city", shared);
        ServerPlayer overworldPlayer = ctx.makeMockServerPlayerInLevel();
        stand(overworldPlayer, inOverworld.inside());
        ServerPlayer endPlayer = QuestPersistentPlayers.mock(end, GameType.SURVIVAL);
        ctx.assertTrue(endPlayer.level() == end, "the second player really is in the End level");
        stand(endPlayer, inEnd.inside());
        ctx.assertTrue(seen(overworldPlayer, CITIES) == 1 && seen(endPlayer, CITIES) == 1,
                "each dimension's city is its own identity");
        ctx.assertTrue(!KnowledgeRuntime.state(overworldPlayer).counters().keySet().stream()
                        .filter(k -> k.contains(CITIES)).anyMatch(k -> k.contains("minecraft:the_end")),
                "overworld evidence is keyed by the overworld, not the End");
        ctx.succeed();
    }

    @GameTest
    public void overlappingStructuresEachRecordTheirOwnType(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var village = QuestStructureFixture.register(level, "minecraft:village_taiga", chunk(6));
        var stronghold = QuestStructureFixture.register(level, "minecraft:stronghold", chunk(6));
        stand(player, village.inside());
        ctx.assertTrue(stronghold.firstPiece().isInside(village.inside()), "pieces overlap at the player's position");
        ctx.assertTrue(seen(player, TYPES) == 2
                && KnowledgeRuntime.state(player).get(QuestRules.seenKey(TYPES, "minecraft:village_taiga")) != 0
                && KnowledgeRuntime.state(player).get(QuestRules.seenKey(TYPES, "minecraft:stronghold")) != 0,
                "both overlapping structures are recorded once each");
        ctx.succeed();
    }

    @GameTest
    public void villageQuestNeedsTheFiveApprovedVillageTypes(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var outpost = QuestStructureFixture.register(level, "minecraft:pillager_outpost", chunk(7));
        stand(player, outpost.inside());
        for (int i = 0; i < 4; i++) {
            var village = QuestStructureFixture.register(level, "minecraft:" + VILLAGES.get(i), chunk(8 + i));
            stand(player, village.inside());
            stand(player, village.inside());
        }
        ctx.assertTrue(!complete(player, "xpq.exp.villages_5"), "four village types plus an outpost are insufficient");
        var last = QuestStructureFixture.register(level, "minecraft:" + VILLAGES.get(4), chunk(12));
        stand(player, last.inside());
        ctx.assertTrue(complete(player, "xpq.exp.villages_5"), "the fifth village type completes the quest");
        ctx.succeed();
    }

    @GameTest
    public void expeditionQuestsNeedFifteenTypesAndTheFiveLandmarks(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        List<String> ids = new java.util.ArrayList<>(GRAND);
        ids.addAll(VILLAGES);
        ids.addAll(List.of("end_city", "fortress", "bastion_remnant", "desert_pyramid", "jungle_pyramid", "igloo"));
        for (int i = 0; i < GRAND.size() - 1; i++) {
            stand(player, QuestStructureFixture.register(level, "minecraft:" + ids.get(i), chunk(13 + i)).inside());
        }
        ctx.assertTrue(!complete(player, "xpq.exp.grand_expedition"), "four landmarks are insufficient");
        stand(player, QuestStructureFixture.register(level, "minecraft:" + ids.get(4), chunk(13 + 4)).inside());
        ctx.assertTrue(complete(player, "xpq.exp.grand_expedition") && !complete(player, "xpq.exp.structures_15"),
                "the five approved landmarks complete the expedition quest only");
        for (int i = 5; i < 14; i++)
            stand(player, QuestStructureFixture.register(level, "minecraft:" + ids.get(i), chunk(13 + i)).inside());
        ctx.assertTrue(seen(player, TYPES) == 14 && !complete(player, "xpq.exp.structures_15"),
                "fourteen distinct types are insufficient");
        stand(player, QuestStructureFixture.register(level, "minecraft:" + ids.get(14), chunk(13 + 14)).inside());
        ctx.assertTrue(seen(player, TYPES) == 15 && complete(player, "xpq.exp.structures_15"),
                "the fifteenth distinct type completes the structure quest");
        ctx.succeed();
    }

    @GameTest
    public void trackerIdsAreRealStructureRegistryEntries(GameTestHelper ctx) {
        var registry = ctx.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String id : List.of("village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga",
                "stronghold", "ancient_city", "monument", "mansion", "trial_chambers", "end_city"))
            ctx.assertTrue(registry.containsKey(Identifier.parse("minecraft:" + id)), id + " is a vanilla structure id");
        ctx.assertTrue(!registry.containsKey(Identifier.parse("minecraft:ocean_monument")),
                "ocean_monument is not a structure id (the monument's id is minecraft:monument)");
        ctx.succeed();
    }

    @GameTest(maxTicks = 100)
    public void serverTickObservesRealPlayersButNotSpectators(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var village = QuestStructureFixture.register(level, "minecraft:village_snowy", chunk(30));
        ServerPlayer player = QuestPersistentPlayers.mock(level, GameType.SURVIVAL);
        ServerPlayer spectator = QuestPersistentPlayers.mock(level, GameType.SPECTATOR);
        BlockPos inside = village.inside();
        player.setPos(inside.getX() + 0.5D, inside.getY(), inside.getZ() + 0.5D);
        spectator.setPos(inside.getX() + 0.5D, inside.getY(), inside.getZ() + 0.5D);
        ctx.runAfterDelay(25, () -> {
            ctx.assertTrue(seen(player, TYPES) == 1, "the real server tick recorded the entry");
            ctx.assertTrue(seen(spectator, TYPES) == 0, "spectators never record structure entry");
            ctx.succeed();
        });
    }

    @GameTest
    public void evidenceSurvivesPlayerSaveAndLoad(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        for (int i = 0; i < 2; i++)
            stand(player, QuestStructureFixture.register(level, "minecraft:end_city", chunk(31 + i)).inside());
        stand(player, QuestStructureFixture.register(level, "minecraft:village_desert", chunk(33)).inside());
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        player.saveWithoutId(out);
        ServerPlayer reloaded = ctx.makeMockServerPlayerInLevel();
        reloaded.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
        ctx.assertTrue(seen(reloaded, CITIES) == 2 && seen(reloaded, TYPES) == 2, "reload restores both evidence sets");
        stand(reloaded, QuestStructureFixture.register(level, "minecraft:end_city", chunk(34)).inside());
        ctx.assertTrue(complete(reloaded, "xpq.dim.end_cities_3"), "restored evidence plus one new city completes the quest");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
