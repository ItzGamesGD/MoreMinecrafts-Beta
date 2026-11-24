package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * P2.5 characterisation probes (test source set only; no product behaviour is changed).
 * They record what Minecraft 26.3 + Fabric actually do with the P1/P2 heaven blocks, next to vanilla controls,
 * and print one {@code HEAVEN_P25_PROBE} line per probe for the P2.5 policy tables. Assertions only guard the
 * controls (so a broken probe cannot report silently); heaven results are recorded, not judged.
 */
public final class HeavenP25PolicyProbes implements CustomTestMethodInvoker {
    private static Block b(String id) {
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        if (block == Blocks.AIR) throw new IllegalStateException("not registered: " + id);
        return block;
    }

    private static final String[] SUPPORTS = {"moreminecrafts:heaven_grass_block", "moreminecrafts:heaven_loam", "moreminecrafts:heaven_cream_stone",
            "minecraft:grass_block", "minecraft:dirt"};
    private static final String[] PLANTS = {"minecraft:dandelion", "minecraft:poppy", "minecraft:short_grass", "minecraft:fern", "minecraft:oak_sapling",
            "minecraft:cherry_sapling", "minecraft:pink_petals", "minecraft:sweet_berry_bush", "minecraft:azalea", "minecraft:wheat",
            "minecraft:dead_bush", "minecraft:brown_mushroom", "minecraft:bamboo_sapling", "minecraft:sugar_cane",
            "moreminecrafts:heaven_flower_red", "moreminecrafts:heaven_short_grass", "moreminecrafts:heaven_petals"};

    private static void log(String probe, Object result) {
        MoreMinecrafts.LOGGER.info("HEAVEN_P25_PROBE {} {}", probe, result);
    }

    @GameTest
    public void plantSurvivalMatrix(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        BlockPos base = ctx.absolutePos(new BlockPos(1, 1, 1));
        Map<String, Map<String, Boolean>> out = new LinkedHashMap<>();
        for (String support : SUPPORTS) {
            level.setBlock(base, b(support).defaultBlockState(), Block.UPDATE_ALL);
            Map<String, Boolean> row = new LinkedHashMap<>();
            for (String plant : PLANTS) row.put(plant.substring(plant.indexOf(':') + 1), b(plant).defaultBlockState().canSurvive(level, base.above()));
            out.put(support.substring(support.indexOf(':') + 1), row);
        }
        log("plant_survival", out);
        ctx.assertTrue(out.get("grass_block").get("dandelion") && out.get("grass_block").get("oak_sapling"), "control: vanilla grass supports flowers and saplings");
        ctx.succeed();
    }

    @GameTest
    public void boneMealToolsAndSnow(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Map<String, Object> out = new LinkedHashMap<>();
        int x = 1;
        for (String id : new String[]{"moreminecrafts:heaven_grass_block", "minecraft:grass_block", "moreminecrafts:heaven_loam", "minecraft:dirt"}) {
            String key = id.substring(id.indexOf(':') + 1);
            BlockPos pos = ctx.absolutePos(new BlockPos(x, 1, 1)); x += 2;
            level.setBlock(pos, b(id).defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            out.put(key + ".bonemeal_applies", BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos));
            for (var tool : List.of(Items.IRON_SHOVEL, Items.IRON_HOE)) {
                level.setBlock(pos, b(id).defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                var player = ctx.makeMockPlayer(GameType.SURVIVAL);
                ItemStack stack = new ItemStack(tool);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false)));
                out.put(key + "." + (tool == Items.IRON_SHOVEL ? "shovel" : "hoe") + "_result", BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
            }
            level.setBlock(pos, b(id).defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.above(), Blocks.SNOW.defaultBlockState(), Block.UPDATE_ALL);
            BlockState under = level.getBlockState(pos);
            out.put(key + ".snowy_property", under.getProperties().stream().anyMatch(p -> p.getName().equals("snowy"))
                    ? String.valueOf(under.getValue(net.minecraft.world.level.block.SnowyBlock.SNOWY)) : "absent");
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        log("bonemeal_tools_snow", out);
        ctx.assertTrue("minecraft:dirt_path".equals(out.get("grass_block.shovel_result")), "control: shovel makes a path from vanilla grass");
        ctx.succeed();
    }

    @GameTest
    public void spreadingAndDecay(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Map<String, Object> out = new LinkedHashMap<>();
        // decay: grass covered by a full block, many random ticks
        for (String id : new String[]{"moreminecrafts:heaven_grass_block", "minecraft:grass_block"}) {
            BlockPos pos = ctx.absolutePos(new BlockPos(id.startsWith("minecraft") ? 1 : 3, 1, 4));
            level.setBlock(pos, b(id).defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            for (int i = 0; i < 400; i++) level.getBlockState(pos).randomTick(level, pos, level.getRandom());
            out.put(id.substring(id.indexOf(':') + 1) + ".covered_after_400_random_ticks", BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        // spread: grass next to a soil block in daylight, many random ticks on the grass
        String[][] pairs = {{"minecraft:grass_block", "minecraft:dirt"}, {"minecraft:grass_block", "moreminecrafts:heaven_loam"},
                {"moreminecrafts:heaven_grass_block", "moreminecrafts:heaven_loam"}, {"moreminecrafts:heaven_grass_block", "minecraft:dirt"}};
        int z = 6;
        for (String[] p : pairs) {
            BlockPos g = ctx.absolutePos(new BlockPos(1, 1, z)), s = g.east();
            level.setBlock(g, b(p[0]).defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(s, b(p[1]).defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(g.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(s.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            for (int i = 0; i < 400; i++) level.getBlockState(g).randomTick(level, g, level.getRandom());
            out.put(p[0].substring(p[0].indexOf(':') + 1) + "->" + p[1].substring(p[1].indexOf(':') + 1),
                    BuiltInRegistries.BLOCK.getKey(level.getBlockState(s).getBlock()).toString());
            z += 2;
        }
        log("spread_decay", out);
        ctx.assertTrue("minecraft:dirt".equals(out.get("grass_block.covered_after_400_random_ticks")), "control: covered vanilla grass decays to dirt");
        ctx.succeed();
    }

    @GameTest
    public void miningDropsAndSolidity(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Map<String, Object> out = new LinkedHashMap<>();
        var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        BlockPos pos = ctx.absolutePos(new BlockPos(1, 1, 1));
        for (String id : new String[]{"moreminecrafts:heaven_grass_block", "moreminecrafts:heaven_loam", "moreminecrafts:heaven_cream_stone",
                "moreminecrafts:heaven_cream_stone_pale", "minecraft:grass_block", "minecraft:stone"}) {
            String k = id.substring(id.indexOf(':') + 1);
            BlockState s = b(id).defaultBlockState();
            level.setBlock(pos, s, Block.UPDATE_ALL);
            ItemStack shovel = new ItemStack(Items.IRON_SHOVEL), pick = new ItemStack(Items.IRON_PICKAXE), silkPick = new ItemStack(Items.IRON_PICKAXE);
            silkPick.enchant(silk, 1);
            out.put(k + ".hardness", s.getDestroySpeed(level, pos));
            out.put(k + ".correct_tool", shovel.isCorrectToolForDrops(s) ? "shovel" : pick.isCorrectToolForDrops(s) ? "pickaxe" : "any/none");
            out.put(k + ".requires_tool", s.requiresCorrectToolForDrops());
            out.put(k + ".drops_plain_tool", Block.getDrops(s, level, pos, null, null, pick).stream().map(st -> BuiltInRegistries.ITEM.getKey(st.getItem()).getPath()).toList());
            out.put(k + ".drops_silk_touch", Block.getDrops(s, level, pos, null, null, silkPick).stream().map(st -> BuiltInRegistries.ITEM.getKey(st.getItem()).getPath()).toList());
        }
        // which P1/P2 blocks count as "solid" for SkyIslandHabitat's template surface, and fluid / fire behaviour
        for (String id : new String[]{"moreminecrafts:heaven_grass_block", "moreminecrafts:heaven_flora_prototype", "moreminecrafts:heaven_giant_stem",
                "moreminecrafts:heaven_giant_bloom", "moreminecrafts:heaven_flower_red", "moreminecrafts:heaven_short_grass", "moreminecrafts:heaven_petals",
                "moreminecrafts:heaven_white_blossom_leaves", "minecraft:cherry_leaves", "minecraft:oak_log", "minecraft:dandelion"}) {
            String k = id.substring(id.indexOf(':') + 1);
            BlockState s = b(id).defaultBlockState();
            out.put(k + ".isSolid", s.isSolid());
            out.put(k + ".water_can_replace", s.canBeReplaced(Fluids.WATER));
            out.put(k + ".fire_ignite_burn_odds", FlammableBlockRegistry.getDefaultInstance().get(s.getBlock()) == null ? "none"
                    : FlammableBlockRegistry.getDefaultInstance().get(s.getBlock()).getIgniteOdds() + "/" + FlammableBlockRegistry.getDefaultInstance().get(s.getBlock()).getBurnOdds());
            out.put(k + ".explosion_resistance", s.getBlock().getExplosionResistance());
        }
        log("mining_drops_solidity", out);
        ctx.assertTrue(Boolean.TRUE.equals(out.get("oak_log.isSolid")), "control: logs are solid");
        ctx.succeed();
    }

    @GameTest
    public void heavenFloraWithoutSupport(GameTestHelper ctx) {
        ServerLevel level = ctx.getLevel();
        Map<String, Object> out = new LinkedHashMap<>();
        BlockPos ground = ctx.absolutePos(new BlockPos(2, 1, 2));
        for (String id : new String[]{"moreminecrafts:heaven_flower_red", "moreminecrafts:heaven_short_grass", "minecraft:dandelion"}) {
            level.setBlock(ground, b("minecraft:grass_block").defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(ground.above(), b(id).defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(ground, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);      // remove the support with a normal update
            out.put(id.substring(id.indexOf(':') + 1) + ".after_support_removed", BuiltInRegistries.BLOCK.getKey(level.getBlockState(ground.above()).getBlock()).toString());
            level.setBlock(ground.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        log("support_removal", out);
        ctx.assertTrue("minecraft:air".equals(out.get("dandelion.after_support_removed")), "control: a vanilla flower pops when its support goes");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}
