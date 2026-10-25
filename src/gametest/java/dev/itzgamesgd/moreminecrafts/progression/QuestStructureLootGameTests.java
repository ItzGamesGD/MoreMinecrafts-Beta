package dev.itzgamesgd.moreminecrafts.progression;

import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

/** C1 live entrypoint coverage; catalog promotion remains per quest. */
public final class QuestStructureLootGameTests implements CustomTestMethodInvoker {
    private static ResourceKey<LootTable> table(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/" + name));
    }

    private static ResourceKey<LootTable> modTable(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("moreminecrafts:chests/" + name));
    }

    private static int seen(ServerPlayer player, String key) {
        return QuestRules.distinctCount(KnowledgeRuntime.state(player), key);
    }

    private static RandomizableContainerBlockEntity chest(GameTestHelper ctx, BlockPos local, String name) {
        ctx.setBlock(local, Blocks.CHEST);
        var blockEntity = ctx.getLevel().getBlockEntity(ctx.absolutePos(local));
        ctx.assertTrue(blockEntity instanceof RandomizableContainerBlockEntity, "chest has a loot container");
        var container = (RandomizableContainerBlockEntity) blockEntity;
        container.setLootTable(table(name));
        return container;
    }

    @GameTest
    public void blockLootCreditsOnlyFirstGeneratingPlayer(GameTestHelper ctx) {
        ServerPlayer opener = ctx.makeMockServerPlayerInLevel();
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        var chest = chest(ctx, new BlockPos(1, 1, 1), "simple_dungeon");
        chest.unpackLootTable(opener);
        ctx.assertTrue(seen(opener, "set.structure_loot_any") == 1
                && seen(opener, "set.structure_loot_dungeon") == 1, "actual opener credited");
        ctx.assertTrue(KnowledgeRuntime.state(opener).questComplete("xpq.exp.first_structure_loot")
                && KnowledgeRuntime.state(opener).questComplete("xpq.exp.dungeon_loot"),
                "first loot and dungeon quests complete for the generating player");
        chest.unpackLootTable(other);
        ctx.assertTrue(seen(other, "set.structure_loot_any") == 0, "reopening gives other player nothing");
        ctx.assertTrue(!KnowledgeRuntime.state(other).questComplete("xpq.exp.first_structure_loot"),
                "bystander cannot complete the first-loot quest");
        chest.setLootTable(table("simple_dungeon"));
        chest.unpackLootTable(opener);
        ctx.assertTrue(seen(opener, "set.structure_loot_any") == 1, "same container is deduplicated");
        ctx.succeed();
    }

    @GameTest
    public void entityLootAndNullGenerationKeepAttribution(GameTestHelper ctx) {
        ServerPlayer opener = ctx.makeMockServerPlayerInLevel();
        ServerPlayer other = ctx.makeMockServerPlayerInLevel();
        var cart = EntityTypes.CHEST_MINECART.create(ctx.getLevel(), EntitySpawnReason.COMMAND);
        ctx.assertTrue(cart instanceof ContainerEntity, "chest minecart is a container entity");
        BlockPos cartPos = ctx.absolutePos(new BlockPos(2, 1, 2));
        cart.setPos(cartPos.getX() + 0.5D, cartPos.getY(), cartPos.getZ() + 0.5D);
        ctx.getLevel().addFreshEntity(cart);
        ContainerEntity container = (ContainerEntity) cart;
        container.setContainerLootTable(table("abandoned_mineshaft"));
        container.unpackChestVehicleLootTable(opener);
        ctx.assertTrue(seen(opener, "set.structure_loot_mineshaft") == 1, "minecart opener credited");
        ctx.assertTrue(KnowledgeRuntime.state(opener).questComplete("xpq.exp.first_structure_loot")
                && KnowledgeRuntime.state(opener).questComplete("xpq.exp.mineshaft_loot"),
                "minecart opener completes only supported loot quests");
        container.unpackChestVehicleLootTable(other);
        ctx.assertTrue(seen(other, "set.structure_loot_any") == 0, "other player gets no minecart credit");
        ctx.assertTrue(!KnowledgeRuntime.state(other).questComplete("xpq.exp.mineshaft_loot"),
                "other player cannot complete mineshaft loot quest on reopening");
        var hopperChest = chest(ctx, new BlockPos(3, 1, 2), "simple_dungeon");
        hopperChest.unpackLootTable(null);
        ctx.assertTrue(seen(opener, "set.structure_loot_dungeon") == 0
                && seen(other, "set.structure_loot_any") == 0, "null player invents no credit");
        ctx.succeed();
    }

    @GameTest
    public void invalidTableAndUniqueContainersFailClosed(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var invalid = chest(ctx, new BlockPos(1, 1, 1), "not_a_real_table");
        invalid.unpackLootTable(player);
        ctx.assertTrue(seen(player, "set.structure_loot_any") == 0, "unknown loot table fails closed");
        var first = chest(ctx, new BlockPos(2, 1, 1), "ancient_city");
        first.unpackLootTable(player);
        first.setLootTable(table("ancient_city"));
        first.unpackLootTable(player);
        var second = chest(ctx, new BlockPos(3, 1, 1), "ancient_city_ice_box");
        second.unpackLootTable(player);
        ctx.assertTrue(seen(player, "set.structure_loot_ancient_city") == 2,
                "different ancient city containers count, reopened first does not");
        ctx.succeed();
    }

    @GameTest
    public void samePathInAnotherNamespaceDoesNotImpersonateVanillaTable(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var chest = chest(ctx, new BlockPos(1, 1, 1), "simple_dungeon");
        chest.setLootTable(modTable("simple_dungeon"));
        chest.unpackLootTable(player);
        ctx.assertTrue(seen(player, "set.structure_loot_any") == 1,
                "real mod loot table passes generic structure loot classification");
        ctx.assertTrue(seen(player, "set.structure_loot_dungeon") == 0,
                "mod namespace table does not count as vanilla dungeon loot");
        ctx.succeed();
    }

    private static void open(GameTestHelper ctx, ServerPlayer player, int x, String name) {
        chest(ctx, new BlockPos(x, 1, 4), name).unpackLootTable(player);
    }

    @GameTest
    public void twoTempleTablesAreRequired(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        open(ctx, player, 1, "desert_pyramid");
        ctx.assertTrue(!KnowledgeRuntime.state(player).questComplete("xpq.exp.temples"),
                "one temple type is insufficient");
        open(ctx, player, 2, "desert_pyramid");
        ctx.assertTrue(!KnowledgeRuntime.state(player).questComplete("xpq.exp.temples"),
                "another chest of the same temple type is insufficient");
        open(ctx, player, 3, "jungle_temple");
        ctx.assertTrue(seen(player, "set.structure_loot_tables") == 2,
                "both approved temple tables are distinct");
        ctx.assertTrue(KnowledgeRuntime.state(player).questComplete("xpq.exp.temples"),
                "two temple types complete the temple quest");
        ctx.succeed();
    }

    @GameTest
    public void oceanRelicsRequireThreeDistinctTableGroups(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        open(ctx, player, 1, "shipwreck_supply");
        open(ctx, player, 2, "shipwreck_treasure");
        open(ctx, player, 3, "underwater_ruin_big");
        ctx.assertTrue(!KnowledgeRuntime.state(player).questComplete("xpq.exp.ocean_relics"),
                "two shipwreck tables still represent one required group");
        open(ctx, player, 4, "buried_treasure");
        ctx.assertTrue(seen(player, "set.structure_loot_tables") == 4,
                "all four generated table identities are retained");
        ctx.assertTrue(KnowledgeRuntime.state(player).questComplete("xpq.exp.ocean_relics"),
                "shipwreck, ruin, and buried treasure complete ocean relics");
        ctx.succeed();
    }

    @GameTest
    public void fortressLootNeedsTheFortressTable(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        open(ctx, player, 1, "simple_dungeon");
        ctx.assertTrue(!KnowledgeRuntime.state(player).questComplete("xpq.dim.fortress_loot"),
                "unrelated loot cannot complete fortress quest");
        open(ctx, player, 2, "nether_bridge");
        ctx.assertTrue(seen(player, "set.structure_loot_fortress") == 1,
                "fortress table records exactly one source observation");
        ctx.assertTrue(KnowledgeRuntime.state(player).questComplete("xpq.dim.fortress_loot"),
                "fortress table completes fortress loot quest");
        ctx.succeed();
    }

    @GameTest
    public void ancientCityNeedsThreeDifferentContainers(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        var first = chest(ctx, new BlockPos(1, 1, 4), "ancient_city");
        first.unpackLootTable(player);
        first.setLootTable(table("ancient_city"));
        first.unpackLootTable(player);
        open(ctx, player, 2, "ancient_city_ice_box");
        ctx.assertTrue(seen(player, "set.structure_loot_ancient_city") == 2,
                "reopening the first chest does not produce a third container");
        open(ctx, player, 3, "ancient_city");
        ctx.assertTrue(seen(player, "set.structure_loot_ancient_city") == 3,
                "third distinct container counts");
        ctx.assertTrue(KnowledgeRuntime.state(player).questComplete("xpq.dim.ancient_city_loot"),
                "third container completes ancient city loot quest");
        ctx.succeed();
    }

    @GameTest
    public void bastionNeedsAllFourTableTypes(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        open(ctx, player, 1, "bastion_bridge");
        open(ctx, player, 2, "bastion_hoglin_stable");
        open(ctx, player, 3, "bastion_other");
        open(ctx, player, 4, "bastion_other");
        ctx.assertTrue(!KnowledgeRuntime.state(player).questComplete("xpq.dim.bastion_all"),
                "four chests with only three bastion tables are insufficient");
        open(ctx, player, 5, "bastion_treasure");
        ctx.assertTrue(seen(player, "set.structure_loot_tables") == 4,
                "all four bastion table types are recorded");
        ctx.assertTrue(KnowledgeRuntime.state(player).questComplete("xpq.dim.bastion_all"),
                "all four table types complete bastion quest");
        ctx.succeed();
    }

    @GameTest
    public void endCityNeedsTenDifferentContainers(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        for (int x = 1; x <= 9; x++) open(ctx, player, x, "end_city_treasure");
        ctx.assertTrue(!KnowledgeRuntime.state(player).questComplete("xpq.dim.end_city_loot_10"),
                "nine containers are insufficient");
        open(ctx, player, 10, "end_city_treasure");
        ctx.assertTrue(seen(player, "set.structure_loot_end_city") == 10,
                "ten different containers are counted");
        ctx.assertTrue(KnowledgeRuntime.state(player).questComplete("xpq.dim.end_city_loot_10"),
                "tenth container completes end city loot quest");
        ctx.succeed();
    }

    @Override public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
