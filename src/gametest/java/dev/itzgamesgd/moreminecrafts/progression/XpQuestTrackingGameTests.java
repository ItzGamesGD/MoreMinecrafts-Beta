package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Q1-A: persistent progress, uncapped counters, distinct sets, stat reads and retroactive reconciliation. */
public final class XpQuestTrackingGameTests implements CustomTestMethodInvoker {
    private static XpQuestCatalogData.Spec activate(String id) {
        XpQuestCatalogData.Spec spec = XpQuestCatalogData.catalog().quests().stream()
                .filter(q -> q.id().equals(id)).findFirst().orElseThrow();
        XpQuestTracking.register(spec);
        return spec;
    }

    private static ResearchProgressState state(ServerPlayer player) { return KnowledgeRuntime.state(player); }

    @GameTest
    public void vanillaStatisticsReconcileRetroactivelyAndIdempotently(GameTestHelper ctx) {
        activate("xpq.frm.breed_200");
        activate("xpq.cmb.hostile_100");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        // History recorded before the quest system evaluated this player.
        player.awardStat(Stats.ANIMALS_BRED, 150);
        player.awardStat(Stats.ENTITY_KILLED.get(EntityTypes.ZOMBIE), 60);
        player.awardStat(Stats.ENTITY_KILLED.get(EntityTypes.COW), 500); // not a monster
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.breed_200") == 150
                && !state(player).questComplete("xpq.frm.breed_200"), "retroactive partial from vanilla stats");
        ctx.assertTrue(state(player).questProgress("xpq.cmb.hostile_100") == 60, "only monster kills count");
        player.awardStat(Stats.ANIMALS_BRED, 50);
        player.awardStat(Stats.ENTITY_KILLED.get(EntityTypes.SKELETON), 40);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.breed_200")
                && state(player).questComplete("xpq.cmb.hostile_100"), "completes at target");
        ResearchProgressState settled = state(player);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player) == settled, "re-reconciliation writes nothing (no revision bump)");
        ctx.succeed();
    }

    @GameTest
    public void knowledgeObservationKeysGiveFullRetroactiveDistinctProgress(GameTestHelper ctx) {
        activate("xpq.fsh.full_catalogue");
        activate("xpq.fsh.ocean_trio");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ResearchProgressState s = state(player);
        for (String fish : new String[]{"crucian_carp", "common_carp", "amur_catfish", "rainbow_trout", "largemouth_bass",
                "northern_pike", "atlantic_salmon", "yellowfin_tuna", "arapaima", "swordfish", "blue_marlin"})
            s = s.with(KnowledgeConditionSemantics.distinctObservationKey("fish_species_caught", fish), 1);
        KnowledgeRuntime.updateState(player, s);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.fsh.full_catalogue") == 11
                && !state(player).questComplete("xpq.fsh.full_catalogue"), "11 of 12 species");
        ctx.assertTrue(state(player).questComplete("xpq.fsh.ocean_trio"), "named subset completes");
        KnowledgeRuntime.recordDistinctCondition(player, "fish_species_caught", "beluga_sturgeon");
        KnowledgeRuntime.recordDistinctCondition(player, "fish_species_caught", "beluga_sturgeon");
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.fsh.full_catalogue")
                && state(player).questProgress("xpq.fsh.full_catalogue") == 12, "duplicate observation counted once");
        ctx.succeed();
    }

    @GameTest
    public void cappedKnowledgeCounterSeedsAndUncappedCounterContinues(GameTestHelper ctx) {
        activate("xpq.frm.harvest_1000");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        // Collectors report "stored + 1" per event (Q1-B: the delta past the Knowledge cap is live evidence).
        for (int i = 1; i <= 150; i++)
            KnowledgeRuntime.recordCondition(player, "crops_harvested", state(player).conditionProgress("crops_harvested") + 1);
        ctx.assertTrue(state(player).conditionProgress("crops_harvested") == 100, "Knowledge counter stays capped at 100");
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.harvest_1000") == 150,
                "baseline + live counter follows every recorded event: " + state(player).questProgress("xpq.frm.harvest_1000"));
        XpQuestTracking.addCounter(player, "crops_harvested", 849);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.harvest_1000") == 999
                && !state(player).questComplete("xpq.frm.harvest_1000"), "counter passes the Knowledge cap");
        XpQuestTracking.addCounter(player, "crops_harvested", 1);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.harvest_1000"), "completes at 1,000");
        ctx.assertTrue(state(player).conditionProgress("crops_harvested") == 100, "Knowledge semantics unchanged");
        ctx.succeed();
    }

    @GameTest
    public void ownedResearchValueAndCountsAreRetroactive(GameTestHelper ctx) {
        activate("xpq.grw.spent_10k");
        activate("xpq.grw.seven_roots");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ResearchProgressState s = state(player);
        long value = 0;
        for (ResearchDefinition d : ResearchCatalog.allCatalogEntries()) {
            if (value >= 10_000) break;
            s = s.with("research." + d.id(), d.maximumLevel());
            for (int l = 1; l <= d.maximumLevel(); l++) value += d.cost(l).rawXp();
        }
        for (ResearchDefinition d : ResearchCatalog.allCatalogEntries())
            if (d.id().endsWith("_root")) s = s.with("research." + d.id(), 1);
        KnowledgeRuntime.updateState(player, s);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.grw.spent_10k"), "owned value " + value + " >= 10,000");
        ctx.assertTrue(state(player).questComplete("xpq.grw.seven_roots"), "all seven roots owned");
        ctx.succeed();
    }

    @GameTest
    public void successfulBlockPlacementCountsAndCancelledDoesNot(GameTestHelper ctx) {
        activate("xpq.bld.place_1000");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos floor = new BlockPos(1, 1, 1);
        ctx.setBlock(floor, Blocks.STONE);
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor, Direction.UP);
        ctx.assertBlockPresent(Blocks.OAK_PLANKS, floor.above());
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor.above(), Direction.UP);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.bld.place_1000") == 2, "two successful placements: "
                + state(player).questProgress("xpq.bld.place_1000"));
        player.setGameMode(GameType.ADVENTURE); // placement is refused in Adventure mode
        ctx.placeAt(player, new ItemStack(Items.OAK_PLANKS), floor.above(2), Direction.UP);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.bld.place_1000") == 2, "refused placement adds nothing");
        ctx.succeed();
    }

    @GameTest
    public void progressSurvivesSaveLoadDeathCopyAndStaysPerPlayer(GameTestHelper ctx) {
        activate("xpq.frm.harvest_1000");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        XpQuestTracking.addCounter(player, "crops_harvested", 1_000);
        KnowledgeRuntime.updateState(player, QuestRules.observeDistinct(state(player), "xpq.test.distinct", "x"));
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.harvest_1000"), "complete before save");

        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess());
        player.saveWithoutId(out);
        CompoundTag saved = out.buildResult();
        ServerPlayer restarted = ctx.makeMockServerPlayerInLevel();
        restarted.load(TagValueInput.create(ProblemReporter.DISCARDING, ctx.getLevel().registryAccess(), saved));
        ctx.assertTrue(state(restarted).questComplete("xpq.frm.harvest_1000")
                && QuestRules.counter(state(restarted), "crops_harvested") == 1_000
                && QuestRules.distinctCount(state(restarted), "xpq.test.distinct") == 1
                && state(restarted).get(XpQuestTracking.SCHEMA_KEY) == XpQuestTracking.SCHEMA_VERSION,
                "save/load keeps completion, counters, distinct sets and schema");

        ResearchProgressState beforeDeath = state(player);
        ServerPlayer respawned = ctx.getLevel().getServer().getPlayerList().respawn(
                player, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        var diff = new java.util.TreeSet<String>();
        beforeDeath.counters().forEach((k, v) -> { if (!k.equals("progression.revision") && !v.equals(state(respawned).counters().get(k))) diff.add(k + "=" + v + "/" + state(respawned).counters().get(k)); });
        ctx.assertTrue(diff.isEmpty(), "death copy keeps the whole progression state: " + diff);

        ctx.assertTrue(state(respawned).questComplete("xpq.frm.harvest_1000"), "death keeps completion");
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        XpQuestTracking.reconcile(other);
        ctx.assertTrue(!state(other).questComplete("xpq.frm.harvest_1000")
                && QuestRules.counter(state(other), "crops_harvested") == 0, "another player is unaffected");
        ctx.succeed();
    }

    @GameTest
    public void claimedQuestIsFrozenAndUnregisteredQuestsAreIgnored(GameTestHelper ctx) {
        activate("xpq.frm.trades_200");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.awardStat(Stats.TRADED_WITH_VILLAGER, 200);
        XpQuestTracking.reconcile(player);
        KnowledgeRuntime.updateState(player, state(player).claimQuest("xpq.frm.trades_200"));
        ResearchProgressState claimed = state(player);
        player.awardStat(Stats.TRADED_WITH_VILLAGER, 500);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questClaimed("xpq.frm.trades_200")
                && state(player).questProgress("xpq.frm.trades_200") == claimed.questProgress("xpq.frm.trades_200"),
                "claimed quest is never rewritten");
        player.awardStat(Stats.CUSTOM.get(Stats.BOAT_ONE_CM), 3_000_000);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.exp.biomes_all_overworld") == 0, "unregistered quest is not evaluated");
        ctx.assertTrue(QuestCatalog.byId("xpq.boss.monument_clear") == null, "VERIFY quest is not registered");
        ctx.succeed();
    }

    @GameTest
    public void travel250kExcludesFlyAndFallStats(GameTestHelper ctx) {
        activate("xpq.exp.travel_250k");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.awardStat(Stats.CUSTOM.get(Stats.FLY_ONE_CM), 30_000_000);
        player.awardStat(Stats.CUSTOM.get(Stats.FALL_ONE_CM), 30_000_000);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.exp.travel_250k") == 0, "fly and fall are excluded");
        player.awardStat(Stats.CUSTOM.get(Stats.WALK_ONE_CM), 25_000_000);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.exp.travel_250k"), "walk counts toward 250k");
        ctx.succeed();
    }

    @GameTest
    public void inventoryScannerTracksEquipmentT2CoresNetheriteToolsAndRodModules(GameTestHelper ctx) {
        activate("xpq.eqp.enhance_5");
        activate("xpq.eqp.enhance_trio");
        activate("xpq.eqp.enhance_20_armor");
        activate("xpq.eqp.first_t2_core");
        activate("xpq.eqp.all_t2_cores");
        activate("xpq.cft.netherite_tools");
        activate("xpq.fsh.first_module");
        activate("xpq.fsh.tier3_rig");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();

        ItemStack sword10 = new ItemStack(Items.DIAMOND_SWORD);
        sword10.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ENHANCEMENT_LEVEL, 10);
        ItemStack pick10 = new ItemStack(Items.DIAMOND_PICKAXE);
        pick10.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ENHANCEMENT_LEVEL, 10);
        ItemStack helm20 = new ItemStack(Items.NETHERITE_HELMET);
        helm20.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ENHANCEMENT_LEVEL, 20);
        ItemStack chest20 = new ItemStack(Items.NETHERITE_CHESTPLATE);
        chest20.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ENHANCEMENT_LEVEL, 20);
        ItemStack legs20 = new ItemStack(Items.NETHERITE_LEGGINGS);
        legs20.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ENHANCEMENT_LEVEL, 20);
        ItemStack boots20 = new ItemStack(Items.NETHERITE_BOOTS);
        boots20.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ENHANCEMENT_LEVEL, 20);

        player.getInventory().setItem(0, sword10);
        player.getInventory().setItem(1, pick10);
        player.getInventory().setItem(36, boots20);
        player.getInventory().setItem(37, legs20);
        player.getInventory().setItem(38, chest20);
        player.getInventory().setItem(39, helm20);

        player.getInventory().setItem(2, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.EYE_OF_TYPHOON));
        player.getInventory().setItem(3, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.HEART_OF_THUNDER));
        player.getInventory().setItem(4, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.BURNING_HEART));
        player.getInventory().setItem(5, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.HEART_OF_CURRENTS));
        player.getInventory().setItem(6, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.ANCIENT_HEART));

        player.getInventory().setItem(7, new ItemStack(Items.NETHERITE_SWORD));
        player.getInventory().setItem(8, new ItemStack(Items.NETHERITE_PICKAXE));
        player.getInventory().setItem(9, new ItemStack(Items.NETHERITE_AXE));
        player.getInventory().setItem(10, new ItemStack(Items.NETHERITE_SHOVEL));
        player.getInventory().setItem(11, new ItemStack(Items.NETHERITE_HOE));

        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        rod.set(dev.itzgamesgd.moreminecrafts.registry.ModDataComponents.ROD_EQUIPMENT,
                new dev.itzgamesgd.moreminecrafts.fishing.RodEquipment(
                        new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.equipmentItem(
                                dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentFamily.REEL, 3)),
                        new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.equipmentItem(
                                dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentFamily.CRANKBAIT, 3)),
                        new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.equipmentItem(
                                dev.itzgamesgd.moreminecrafts.fishing.FishingEquipmentFamily.MINNOW, 3))));
        player.getInventory().setItem(40, rod);

        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.eqp.enhance_5"), "enhance 5");
        ctx.assertTrue(state(player).questComplete("xpq.eqp.enhance_trio"), "simultaneous trio");
        ctx.assertTrue(state(player).questComplete("xpq.eqp.enhance_20_armor"), "simultaneous 4 armor +20");
        ctx.assertTrue(state(player).questComplete("xpq.eqp.first_t2_core"), "first t2 core");
        ctx.assertTrue(state(player).questComplete("xpq.eqp.all_t2_cores"), "all 5 t2 cores");
        ctx.assertTrue(state(player).questComplete("xpq.cft.netherite_tools"), "all 5 netherite tools");
        ctx.assertTrue(state(player).questComplete("xpq.fsh.first_module"), "module t2+");
        ctx.assertTrue(state(player).questComplete("xpq.fsh.tier3_rig"), "tier3 rig");
        ctx.succeed();
    }

    @GameTest
    public void fishingGradesAndMultiCatchTrackCorrectly(GameTestHelper ctx) {
        activate("xpq.fsh.grade_four");
        activate("xpq.fsh.grade_five");
        activate("xpq.fsh.grade_five_5");
        activate("xpq.fsh.multi_catch");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();

        XpQuestHooks.fishCaught(player, "crucian_carp", 3);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(!state(player).questComplete("xpq.fsh.grade_four"), "grade III does not complete grade IV");

        XpQuestHooks.fishCaught(player, "crucian_carp", 4);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.fsh.grade_four")
                && !state(player).questComplete("xpq.fsh.grade_five"), "grade IV completes only grade_four");

        for (String species : new String[]{"crucian_carp", "common_carp", "amur_catfish", "rainbow_trout", "largemouth_bass"}) {
            XpQuestHooks.fishCaught(player, species, 5);
        }
        XpQuestHooks.fishingMultiCatch(player);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.fsh.grade_five"), "grade V completes");
        ctx.assertTrue(state(player).questComplete("xpq.fsh.grade_five_5"), "5 species grade V completes");
        ctx.assertTrue(state(player).questComplete("xpq.fsh.multi_catch"), "multi catch completes");
        ctx.succeed();
    }

    @GameTest
    public void storageAndCopperAgesPlacementTrackConfirmedPlacements(GameTestHelper ctx) {
        activate("xpq.bld.storage_50");
        activate("xpq.bld.copper_ages");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos base = new BlockPos(1, 1, 1);
        for (BlockPos target : new BlockPos[]{base, base.east(), base.south(), base.south().east(), base.south(2), base.south(2).east()}) {
            ctx.setBlock(target, Blocks.STONE);
        }
        ctx.placeAt(player, new ItemStack(Items.CHEST), base, Direction.UP);
        ctx.placeAt(player, new ItemStack(Items.BARREL), base.east(), Direction.UP);
        ctx.placeAt(player, new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:copper_block")).orElseThrow()), base.south(), Direction.UP);
        ctx.placeAt(player, new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:exposed_copper")).orElseThrow()), base.south().east(), Direction.UP);
        ctx.placeAt(player, new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:weathered_copper")).orElseThrow()), base.south(2), Direction.UP);
        ctx.placeAt(player, new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:oxidized_copper")).orElseThrow()), base.south(2).east(), Direction.UP);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.bld.storage_50") == 2, "chest and barrel counted");
        ctx.assertTrue(state(player).questComplete("xpq.bld.copper_ages"), "4 copper stages complete");
        ctx.succeed();
    }

    @GameTest
    public void alchemyAndNonOreSmeltingProvenanceRejectManualInsertion(GameTestHelper ctx) {
        activate("xpq.alc.shock_potion");
        activate("xpq.alc.splash_lingering");
        activate("xpq.alc.enhanced_3");
        activate("xpq.cft.smelt_1000");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();

        BlockPos furnacePos = ctx.absolutePos(new BlockPos(2, 2, 2));
        ctx.setBlock(new BlockPos(2, 2, 2), Blocks.FURNACE);
        var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity) ctx.getLevel().getBlockEntity(furnacePos);
        furnace.setItem(2, new ItemStack(Items.STONE, 8));
        new net.minecraft.world.inventory.Slot(furnace, 2, 0, 0).onTake(player, furnace.removeItem(2, 8));
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.cft.smelt_1000") == 0, "manual furnace insert gives 0");

        furnace.setItem(2, new ItemStack(Items.STONE, 5));
        SmeltingOutputProvenance.record(furnace, new ItemStack(Items.COBBLESTONE), new ItemStack(Items.STONE, 5));
        KnowledgeEventCollectors.smeltingCollected(player, furnace, new ItemStack(Items.STONE, 5));
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.cft.smelt_1000") == 5, "verified non-ore smelt counted");

        ItemStack shock = net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                Items.POTION, dev.itzgamesgd.moreminecrafts.alchemy.ModAlchemy.SHOCK_POTION);
        XpQuestHooks.potionCollected(player, shock, 1);

        var splashPotions = new ItemStack[]{
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS),
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.HEALING),
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.STRENGTH),
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.LEAPING)
        };
        for (ItemStack s : splashPotions) XpQuestHooks.potionCollected(player, s, 1);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.alc.splash_lingering") == 3,
                "4 splash types cap at 3 without lingering");

        var lingeringPotions = new ItemStack[]{
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.LINGERING_POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS),
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.LINGERING_POTION, net.minecraft.world.item.alchemy.Potions.HEALING),
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.LINGERING_POTION, net.minecraft.world.item.alchemy.Potions.STRENGTH)
        };
        for (ItemStack l : lingeringPotions) XpQuestHooks.potionCollected(player, l, 1);

        XpQuestHooks.potionCollected(player, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.STRONG_SWIFTNESS), 1);
        XpQuestHooks.potionCollected(player, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.LONG_SWIFTNESS), 1);
        XpQuestHooks.potionCollected(player, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.STRONG_STRENGTH), 1);
        XpQuestHooks.potionCollected(player, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.LONG_STRENGTH), 1);
        XpQuestHooks.potionCollected(player, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.STRONG_LEAPING), 1);
        XpQuestHooks.potionCollected(player, net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.LONG_LEAPING), 1);

        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.alc.shock_potion"), "shock potion completes");
        ctx.assertTrue(state(player).questComplete("xpq.alc.splash_lingering"), "3 splash + 3 lingering completes");
        ctx.assertTrue(state(player).questComplete("xpq.alc.enhanced_3"), "3 enhanced+extended pairs complete");
        ctx.succeed();
    }

    @GameTest
    public void survivalCraftingTracksItemsRecipesAndShulkersWhileCreativeIsExcluded(GameTestHelper ctx) {
        activate("xpq.cft.sleeping_bag");
        activate("xpq.cft.mm_blocks");
        activate("xpq.dim.shulker_boxes");
        activate("xpq.cft.recipes_200");
        ServerPlayer creative = ctx.makeMockServerPlayerInLevel();
        creative.setGameMode(GameType.CREATIVE);
        XpQuestHooks.crafted(creative,
                new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("moreminecrafts:exploration_sleeping_bag")).orElseThrow()),
                "moreminecrafts:exploration_sleeping_bag");
        XpQuestTracking.reconcile(creative);
        ctx.assertTrue(!state(creative).questComplete("xpq.cft.sleeping_bag"), "creative craft is ignored");

        ServerPlayer survival = ctx.makeMockServerPlayerInLevel();
        survival.setGameMode(GameType.SURVIVAL);
        var menu = new net.minecraft.world.inventory.CraftingMenu(1, survival.getInventory(),
                net.minecraft.world.inventory.ContainerLevelAccess.create(ctx.getLevel(), ctx.absolutePos(new BlockPos(1, 1, 1))));
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
        menu.slotsChanged(menu.getSlot(1).container);
        survival.containerMenu = menu;
        var resultSlot = menu.getSlot(0);
        ItemStack crafted = resultSlot.remove(4);
        ctx.assertTrue(!crafted.isEmpty(), "oak log crafts planks");
        resultSlot.onTake(survival, crafted);
        ctx.assertTrue(QuestRules.distinctCount(state(survival), XpQuestHooks.CRAFTED_RECIPES_SET) >= 1,
                "real crafting result slot take records recipe ID");

        XpQuestHooks.crafted(survival,
                new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("moreminecrafts:exploration_sleeping_bag")).orElseThrow()),
                "moreminecrafts:exploration_sleeping_bag");
        XpQuestHooks.crafted(survival, new ItemStack(Items.SHULKER_BOX), "minecraft:shulker_box");
        XpQuestHooks.crafted(survival,
                new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:white_shulker_box")).orElseThrow()),
                "minecraft:white_shulker_box");
        XpQuestHooks.crafted(survival,
                new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:red_shulker_box")).orElseThrow()),
                "minecraft:red_shulker_box");
        XpQuestHooks.crafted(survival,
                new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:blue_shulker_box")).orElseThrow()),
                "minecraft:blue_shulker_box");
        XpQuestHooks.crafted(survival,
                new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.parse("minecraft:green_shulker_box")).orElseThrow()),
                "minecraft:green_shulker_box");
        XpQuestTracking.reconcile(survival);
        ctx.assertTrue(state(survival).questComplete("xpq.cft.sleeping_bag"), "survival sleeping bag craft completes");
        ctx.assertTrue(state(survival).questComplete("xpq.dim.shulker_boxes"), "4 dyed shulker colors complete");
        ctx.succeed();
    }

    @GameTest
    public void villagerTradeHookTracksMasterProfessionsAndMendingBook(GameTestHelper ctx) {
        activate("xpq.frm.master_trade");
        activate("xpq.frm.mending_book");
        activate("xpq.frm.professions_10");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var professions = ctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION);
        var villager = EntityTypes.VILLAGER.create(ctx.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        villager.setTradingPlayer(player);
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(professions.getOrThrow(net.minecraft.world.entity.npc.villager.VillagerProfession.LIBRARIAN))
                .withLevel(5));

        ItemStack mendingBook = new ItemStack(Items.ENCHANTED_BOOK);
        var mendingHolder = T1Operations.enchantment(player, "minecraft:mending").orElseThrow();
        net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(mendingBook, e -> e.set(mendingHolder, 1));
        var offer = new net.minecraft.world.item.trading.MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(Items.EMERALD, 16),
                java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(Items.BOOK)),
                mendingBook, 12, 1, 0.0F);
        villager.notifyTrade(offer);

        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.frm.master_trade"), "master villager trade completes");
        ctx.assertTrue(state(player).questComplete("xpq.frm.mending_book"), "librarian mending trade completes");
        ctx.assertTrue(state(player).questProgress("xpq.frm.professions_10") == 1, "1 profession counted");
        ctx.succeed();
    }

    @GameTest
    public void batch5ExplorationAndLootProvenanceQuestsWorkAndRejectDrops(GameTestHelper ctx) {
        activate("xpq.exp.height_extremes");
        activate("xpq.exp.sherds_15");
        activate("xpq.dim.blaze_rods_32");
        activate("xpq.exp.explorer_maps");
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();

        player.setPos(player.getX(), 305.0D, player.getZ());
        XpQuestHooks.checkOverworldHeightExtremes(player);
        player.setPos(player.getX(), -62.0D, player.getZ());
        XpQuestHooks.checkOverworldHeightExtremes(player);

        String[] sherds = {
                "angler", "archer", "arms_up", "blade", "brewer", "burn", "danger", "explorer",
                "flow", "friend", "guster", "heart", "heartbreak", "howl", "miner"
        };
        for (String s : sherds) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getOptional(net.minecraft.resources.Identifier.parse("minecraft:" + s + "_pottery_sherd"))
                    .orElseThrow();
            XpQuestHooks.archaeologyItemProduced(player, new ItemStack(item));
        }

        // Player-dropped blaze rod without genuine Blaze death provenance gives 0.
        var nonGenuine = new net.minecraft.world.entity.item.ItemEntity(
                ctx.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.BLAZE_ROD, 16));
        nonGenuine.setNoPickUpDelay();
        nonGenuine.playerTouch(player);
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.dim.blaze_rods_32") == 0, "dropped blaze rod does not count");

        // Genuine Blaze death-loot pickup counts actual transferred quantity.
        var genuine = new net.minecraft.world.entity.item.ItemEntity(
                ctx.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.BLAZE_ROD, 32));
        genuine.setAttached(QuestWitherSkullProvenance.GENUINE_BLAZE_ROD, Boolean.TRUE);
        genuine.setNoPickUpDelay();
        genuine.playerTouch(player);

        var decorationsRegistry = ctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.MAP_DECORATION_TYPE);
        var cartographer = EntityTypes.VILLAGER.create(ctx.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var professions = ctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION);
        cartographer.setTradingPlayer(player);
        cartographer.setVillagerData(cartographer.getVillagerData()
                .withProfession(professions.getOrThrow(net.minecraft.world.entity.npc.villager.VillagerProfession.CARTOGRAPHER)));
        for (String typeId : new String[]{
                "minecraft:monument",
                "minecraft:mansion",
                "minecraft:trial_chambers"}) {
            var typeHolder = decorationsRegistry.get(
                    ResourceKey.create(Registries.MAP_DECORATION_TYPE, Identifier.parse(typeId))).orElseThrow();
            ItemStack map = new ItemStack(Items.FILLED_MAP);
            map.set(net.minecraft.core.component.DataComponents.MAP_DECORATIONS,
                    new net.minecraft.world.item.component.MapDecorations(java.util.Map.of(
                            "+", new net.minecraft.world.item.component.MapDecorations.Entry(
                                    typeHolder, 0.0D, 0.0D, 0.0F))));
            cartographer.notifyTrade(new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(Items.EMERALD, 12),
                    java.util.Optional.empty(), map, 12, 1, 0.0F));
        }

        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.exp.height_extremes"), "height extremes complete");
        ctx.assertTrue(state(player).questComplete("xpq.exp.sherds_15"), "15 sherds complete");
        ctx.assertTrue(state(player).questComplete("xpq.dim.blaze_rods_32"), "32 genuine blaze rods complete");
        ctx.assertTrue(state(player).questComplete("xpq.exp.explorer_maps"), "3 cartographer explorer maps complete");
        ctx.succeed();
    }

    @GameTest
    public void batch5MiningFarmingHoneyAncientEnchantingConduitAndMetalsWork(GameTestHelper ctx) {
        activate("xpq.min.amethyst_spyglass");
        activate("xpq.min.all_wood");
        activate("xpq.frm.harvest_all_crops");
        activate("xpq.frm.honey_32");
        activate("xpq.frm.ancient_harvest");
        activate("xpq.cft.enchanting_30");
        activate("xpq.cft.conduit");
        activate("xpq.cft.mm_metallurgy");

        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        // Amethyst + Spyglass
        BlockPos clusterPos = new BlockPos(1, 2, 1);
        ctx.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        ctx.setBlock(clusterPos, Blocks.AMETHYST_CLUSTER);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        for (int i = 0; i < 8; i++) {
            ctx.setBlock(clusterPos, Blocks.AMETHYST_CLUSTER);
            player.gameMode.destroyBlock(ctx.absolutePos(clusterPos));
        }
        XpQuestHooks.crafted(player, new ItemStack(Items.SPYGLASS), "minecraft:spyglass");

        // All 11 wood logs/stems
        Block[] woods = {
                Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG,
                Blocks.PALE_OAK_LOG, Blocks.CRIMSON_STEM, Blocks.WARPED_STEM
        };
        for (Block wood : woods) {
            ctx.setBlock(clusterPos, wood);
            player.gameMode.destroyBlock(ctx.absolutePos(clusterPos));
        }

        // Player-placed melon must NOT count; stem-grown melon counts.
        BlockPos fruitPos = new BlockPos(3, 2, 3);
        ctx.setBlock(new BlockPos(3, 1, 3), Blocks.FARMLAND);
        ctx.setBlock(fruitPos, Blocks.MELON);
        player.gameMode.destroyBlock(ctx.absolutePos(fruitPos));
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.harvest_all_crops") == 0, "standalone melon ignored");

        ctx.setBlock(new BlockPos(2, 1, 3), Blocks.FARMLAND);
        ctx.setBlock(fruitPos, Blocks.MELON);
        ctx.setBlock(new BlockPos(2, 2, 3), Blocks.ATTACHED_MELON_STEM.defaultBlockState()
                .setValue(net.minecraft.world.level.block.AttachedStemBlock.FACING, Direction.EAST));
        player.gameMode.destroyBlock(ctx.absolutePos(fruitPos));

        // Player-placed torchflower must NOT count; crop-matured torchflower counts.
        BlockPos tfPos = new BlockPos(4, 2, 3);
        ctx.setBlock(new BlockPos(4, 1, 3), Blocks.FARMLAND);
        ctx.setBlock(tfPos, Blocks.TORCHFLOWER);
        player.gameMode.destroyBlock(ctx.absolutePos(tfPos));
        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questProgress("xpq.frm.ancient_harvest") == 0, "placed torchflower ignored");

        ctx.setBlock(tfPos, Blocks.TORCHFLOWER);
        QuestNaturalCropProvenance.mark(ctx.getLevel(), ctx.absolutePos(tfPos));
        player.gameMode.destroyBlock(ctx.absolutePos(tfPos));
        ctx.setBlock(tfPos, Blocks.PITCHER_CROP.defaultBlockState()
                .setValue(net.minecraft.world.level.block.PitcherCropBlock.AGE,
                        net.minecraft.world.level.block.PitcherCropBlock.MAX_AGE));
        player.gameMode.destroyBlock(ctx.absolutePos(tfPos));

        // Remaining crops + honey + enchanting + conduit + 4 MM metals
        for (String cropId : new String[]{
                "minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots",
                "minecraft:pumpkin", "minecraft:nether_wart", "minecraft:cocoa", "minecraft:sweet_berry_bush"}) {
            XpQuestHooks.cropHarvested(player, cropId);
        }
        for (int i = 0; i < 32; i++) XpQuestHooks.honeyBottleHarvested(player);
        for (int i = 0; i < 30; i++) XpQuestHooks.enchantedWithBookshelves(player);
        XpQuestHooks.conduitActivated(player);
        XpQuestHooks.smeltedCollected(player, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.TITANIUM_INGOT), 1);
        XpQuestHooks.crafted(player, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.MYTHRIL), "moreminecrafts:mythril_from_mythril_block");
        XpQuestHooks.crafted(player, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.ETHERIUM_INGOT), "moreminecrafts:etherium_ingot");
        XpQuestHooks.crafted(player, new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.ADAMANTIUM_INGOT), "moreminecrafts:adamantium_ingot_from_adamantium_block");

        XpQuestTracking.reconcile(player);
        ctx.assertTrue(state(player).questComplete("xpq.min.amethyst_spyglass"), "amethyst + spyglass complete");
        ctx.assertTrue(state(player).questComplete("xpq.min.all_wood"), "11 wood types complete");
        ctx.assertTrue(state(player).questComplete("xpq.frm.harvest_all_crops"), "all 9 crops complete");
        ctx.assertTrue(state(player).questComplete("xpq.frm.honey_32"), "32 honey bottles complete");
        ctx.assertTrue(state(player).questComplete("xpq.frm.ancient_harvest"), "torchflower + pitcher complete");
        ctx.assertTrue(state(player).questComplete("xpq.cft.enchanting_30"), "30 bookshelf enchantments complete");
        ctx.assertTrue(state(player).questComplete("xpq.cft.conduit"), "conduit activation complete");
        ctx.assertTrue(state(player).questComplete("xpq.cft.mm_metallurgy"), "4 MM metals complete");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
