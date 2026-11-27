package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** T2 accessory registration, the 13 shaped recipes through real recipe matching, and the wired drop sources. */
public final class T2AccessoryGameTests {
    private static final String M = "moreminecrafts:";
    private static final Map<String, List<String>> RECIPES = new LinkedHashMap<>();
    static {
        RECIPES.put("star_covenant", List.of("minecraft:diamond", M + "void_star_shard", "minecraft:diamond", M + "blazing_relic_core", M + "etherium_shard", M + "blazing_relic_core", "minecraft:gold_ingot", M + "mythril", "minecraft:gold_ingot"));
        RECIPES.put("blood_covenant", List.of("minecraft:diamond", M + "gladiator_blood_crest", "minecraft:diamond", M + "blazing_relic_core", M + "etherium_ingot", M + "blazing_relic_core", M + "adamantium_ingot", M + "mythril", M + "adamantium_ingot"));
        RECIPES.put("phantom_mirror", List.of("minecraft:diamond", M + "shimmering_soul_glass", "minecraft:diamond", M + "earthen_relic_core", M + "etherium_shard", M + "earthen_relic_core", "minecraft:lapis_lazuli", M + "mythril", "minecraft:lapis_lazuli"));
        RECIPES.put("ring_of_time", List.of("minecraft:diamond", M + "ancient_clock_spring", "minecraft:diamond", M + "glacial_relic_core", M + "etherium_ingot", M + "glacial_relic_core", "minecraft:echo_shard", M + "glacial_relic_core", M + "mythril"));
        RECIPES.put("heart_of_gravity", List.of("minecraft:diamond", M + "void_gravity_core", "minecraft:diamond", M + "abyssal_relic_core", M + "etherium_ingot", M + "abyssal_relic_core", "minecraft:obsidian", M + "abyssal_relic_core", M + "mythril"));
        RECIPES.put("soul_chain", List.of("minecraft:diamond", M + "skeleton_king_chain", "minecraft:diamond", M + "earthen_relic_core", M + "etherium_shard", M + "earthen_relic_core", M + "titanium_ingot", M + "mythril", M + "titanium_ingot"));
        RECIPES.put("ember_ring", List.of("minecraft:diamond", M + "lava_heartstone", "minecraft:diamond", M + "blazing_relic_core", M + "etherium_shard", M + "blazing_relic_core", "minecraft:gold_ingot", M + "mythril", "minecraft:gold_ingot"));
        RECIPES.put("ring_of_life", List.of("minecraft:diamond", "minecraft:totem_of_undying", "minecraft:diamond", M + "blazing_relic_core", M + "etherium_shard", M + "blazing_relic_core", "minecraft:golden_apple", M + "mythril", "minecraft:golden_apple"));
        RECIPES.put("seed_of_life", List.of("minecraft:diamond", M + "ancient_world_tree_sap", "minecraft:diamond", M + "abyssal_relic_core", M + "etherium_shard", M + "abyssal_relic_core", "minecraft:gold_ingot", M + "mythril", "minecraft:gold_ingot"));
        RECIPES.put("snow_charm", List.of("minecraft:diamond", M + "permafrost_crystal", "minecraft:diamond", M + "glacial_relic_core", M + "etherium_shard", M + "glacial_relic_core", M + "titanium_ingot", M + "mythril", M + "titanium_ingot"));
        RECIPES.put("deep_sea_pearl", List.of("minecraft:diamond", "minecraft:heart_of_the_sea", "minecraft:diamond", M + "tidal_relic_core", M + "etherium_shard", M + "tidal_relic_core", M + "abyssal_pearl_core", M + "mythril", M + "abyssal_pearl_core"));
        RECIPES.put("whisper_of_the_sea", List.of("minecraft:diamond", M + "mermaid_scale", "minecraft:diamond", M + "tidal_relic_core", M + "etherium_shard", M + "tidal_relic_core", "minecraft:lapis_lazuli", M + "mythril", "minecraft:lapis_lazuli"));
        RECIPES.put("feather_charm", List.of(M + "sky_feather", M + "gale_shard", M + "sky_feather", M + "glacial_relic_core", "minecraft:diamond", M + "glacial_relic_core", "minecraft:diamond", M + "mythril", "minecraft:diamond"));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(Identifier.parse(id)).orElseThrow(() -> new IllegalStateException("missing item " + id)).value();
    }

    private static ItemStack assemble(GameTestHelper ctx, List<String> ids) {
        List<ItemStack> grid = new ArrayList<>();
        for (String id : ids) grid.add(new ItemStack(item(id)));
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return ctx.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, ctx.getLevel())
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    @GameTest(maxTicks = 20)
    public void allThirteenT2AccessoriesAndElevenMaterialsAreRegistered(GameTestHelper ctx) {
        long t2 = AccessoryDefinition.ALL.stream().filter(d -> !d.isT1()).count();
        ctx.assertTrue(t2 == 13, "13 T2 accessories registered, found " + t2);
        for (AccessoryDefinition d : AccessoryDefinition.ALL) {
            ctx.assertTrue(BuiltInRegistries.ITEM.get(ModIds.id(d.registryId())).isPresent(), d.registryId() + " registered");
            ctx.assertTrue(ModItems.ACCESSORIES.get(d) != null, d.registryId() + " in ModItems.ACCESSORIES");
        }
        ctx.assertTrue(BuiltInRegistries.ITEM.get(ModIds.id("feather_charm")).isPresent(), "feather_charm registered");
        for (String m : List.of("skeleton_king_chain", "gladiator_blood_crest", "ancient_clock_spring", "ancient_world_tree_sap", "lava_heartstone",
                "permafrost_crystal", "abyssal_pearl_core", "mermaid_scale", "void_star_shard", "void_gravity_core", "shimmering_soul_glass"))
            ctx.assertTrue(BuiltInRegistries.ITEM.get(ModIds.id(m)).isPresent(), m + " registered");
        ctx.succeed();
    }

    @GameTest(maxTicks = 20)
    public void everyArtifactCoreMappingPointsAtARegisteredItem(GameTestHelper ctx) {
        ctx.assertTrue(ArtifactCoreMapping.ALL.size() == 20, "20 mapping entries");
        for (var entry : ArtifactCoreMapping.ALL)
            ctx.assertTrue(BuiltInRegistries.ITEM.get(ModIds.id(entry.accessoryId())).isPresent(), entry.accessoryId() + " mapped item is registered");
        ctx.succeed();
    }

    @GameTest(maxTicks = 40)
    public void thirteenRecipesCraftTheRightAccessoryAndRejectAWrongEtheriumForm(GameTestHelper ctx) {
        for (var entry : RECIPES.entrySet()) {
            ItemStack result = assemble(ctx, entry.getValue());
            ctx.assertTrue(result.is(item(M + entry.getKey())) && result.getCount() == 1, entry.getKey() + " layout yields exactly one " + entry.getKey());
            // swapping the centre etherium form (shard <-> ingot) or a mirrored layout must not craft the accessory
            List<String> swapped = new ArrayList<>(entry.getValue());
            for (int i = 0; i < swapped.size(); i++) {
                if (swapped.get(i).equals(M + "etherium_shard")) swapped.set(i, M + "etherium_ingot");
                else if (swapped.get(i).equals(M + "etherium_ingot")) swapped.set(i, M + "etherium_shard");
            }
            if (!swapped.equals(entry.getValue()))
                ctx.assertTrue(!assemble(ctx, swapped).is(item(M + entry.getKey())), entry.getKey() + " rejects the wrong etherium form");
            List<String> reversed = new ArrayList<>(entry.getValue());
            java.util.Collections.reverse(reversed);
            if (!reversed.equals(entry.getValue()))
                ctx.assertTrue(!assemble(ctx, reversed).is(item(M + entry.getKey())), entry.getKey() + " is not matched when rotated 180 degrees");
        }
        ctx.succeed();
    }

    private static int count(ServerLevel level, String table, Item item, LootParams params, int trials) {
        LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table)));
        int total = 0;
        for (int i = 0; i < trials; i++)
            for (ItemStack stack : loot.getRandomItems(params, 777L + i)) if (stack.is(item)) total += stack.getCount();
        return total;
    }

    @GameTest(maxTicks = 200)
    public void ancientCityChestRollsClockSpringAtTwelvePercentAndWardenAlwaysDropsIt(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        LootParams chest = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        int trials = 20000;
        int hits = count(level, "minecraft:chests/ancient_city", ModItems.ANCIENT_CLOCK_SPRING, chest, trials);
        ctx.assertTrue(Math.abs(hits / (double) trials - 0.12D) <= 0.015D, "ancient city clock spring rate " + hits / (double) trials);
        var zombie = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        LootParams entity = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, zombie)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic()).create(LootContextParamSets.ENTITY);
        ctx.assertTrue(count(level, "minecraft:entities/warden", ModItems.ANCIENT_CLOCK_SPRING, entity, 200) == 200, "warden drops exactly one every time");
        ctx.succeed();
    }

    @GameTest(maxTicks = 200)
    public void tombGuardianAlwaysDropsSapAndKeepsAncientHeartAndGravityHeartPools(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        var zombie = ctx.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        LootParams entity = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, zombie)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic()).create(LootContextParamSets.ENTITY);
        int trials = 4000;
        String table = TombGuardianGravityHeart.LOOT_TABLE;
        ctx.assertTrue(count(level, table, ModItems.ANCIENT_WORLD_TREE_SAP, entity, trials) == trials, "sap exactly one every kill");
        ctx.assertTrue(count(level, table, ModItems.ANCIENT_HEART, entity, trials) == trials, "ancient heart pool unchanged");
        double gravity = count(level, table, ModItems.ACCESSORIES.get(AccessoryDefinition.HEART_OF_GRAVITY), entity, trials) / (double) trials;
        ctx.assertTrue(Math.abs(gravity - 0.20D) <= 0.03D, "heart of gravity 20% drop unchanged, rate " + gravity);
        ctx.succeed();
    }

    private static int itemsNear(GameTestHelper ctx, Item item) {
        AABB box = new AABB(ctx.absolutePos(new BlockPos(1, 2, 1))).inflate(6);
        int n = 0;
        for (ItemEntity e : ctx.getLevel().getEntitiesOfClass(ItemEntity.class, box)) if (e.getItem().is(item)) n += e.getItem().getCount();
        return n;
    }

    @GameTest(maxTicks = 40)
    public void skeletonKingDropsChainOnceAndOrdinarySkeletonsNever(GameTestHelper ctx) {
        var king = ctx.spawn(EntityTypes.SKELETON, new BlockPos(1, 2, 1));
        ((SkeletonKingState) king).moreminecrafts$makeSkeletonKing();
        king.kill(ctx.getLevel());
        var plain = ctx.spawn(EntityTypes.SKELETON, new BlockPos(2, 2, 1));
        plain.kill(ctx.getLevel());
        ctx.runAfterDelay(2, () -> {
            ctx.assertTrue(itemsNear(ctx, ModItems.SKELETON_KING_CHAIN) == 1, "exactly one chain from the king only, found " + itemsNear(ctx, ModItems.SKELETON_KING_CHAIN));
            ctx.succeed();
        });
    }
}
