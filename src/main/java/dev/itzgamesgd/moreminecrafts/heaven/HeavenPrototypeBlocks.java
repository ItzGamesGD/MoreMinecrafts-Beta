package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Heaven sky island P1 prototype blocks (design: docs/design/heaven_sky_island_*_2026-11-19).
 *
 * <p>IDs, strengths, sounds and drops are PROVISIONAL (owner approval pending, see
 * docs/design/heaven_sky_island_p1_prototype_2026-11-20/README.md). None of these blocks is
 * placed by world generation: they only appear through the prototype template
 * {@code moreminecrafts:heaven_prototype/plain_medium_d_v5} placed with {@code /place template}.
 *
 * <p>P3 (owner-approved F1-F4, F9, F11): heaven grass spreads onto / decays into heaven loam and takes bone meal
 * ({@link HeavenGrassBlock}); shovel path, hoe farmland and vanilla plant survival are individual vanilla tags
 * (never {@code #minecraft:dirt}); IDs unchanged; cream stone drops itself.
 */
public final class HeavenPrototypeBlocks {
    /** A-grass top, A fringe on B-loam side, B-loam bottom. P3 (F1/F4): spreads onto heaven loam, decays to it, bone meal. */
    public static final Block HEAVEN_GRASS_BLOCK = natural("heaven_grass_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).randomTicks().strength(0.6F).sound(SoundType.GRASS), HeavenGrassBlock::new);
    /** B loam (subsurface; replaces dirt and coarse dirt). */
    public static final Block HEAVEN_LOAM = natural("heaven_loam",
            BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL));
    /** B2 natural cream rock (replaces stone). */
    public static final Block HEAVEN_CREAM_STONE = natural("heaven_cream_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    /** B2 pale cream rock (replaces andesite and tuff). */
    public static final Block HEAVEN_CREAM_STONE_PALE = natural("heaven_cream_stone_pale",
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    /** Baked V5 vegetation parts (one state per unique quarter-block voxel cell); no item, no drops. */
    public static final Block HEAVEN_FLORA_PROTOTYPE = flora();

    private HeavenPrototypeBlocks() {}

    private static Block natural(String path, BlockBehaviour.Properties properties) {
        return natural(path, properties, Block::new);
    }

    private static Block natural(String path, BlockBehaviour.Properties properties, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModIds.id(path));
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ModIds.id(path));
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
        return block;
    }

    private static Block flora() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModIds.id("heaven_flora_prototype"));
        return Registry.register(BuiltInRegistries.BLOCK, key, new HeavenFloraPrototypeBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.AZALEA_LEAVES).noOcclusion().noLootTable()
                .pushReaction(PushReaction.POPPED).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p, box) -> false)
                .isRedstoneConductor((s, l, p) -> false).isValidSpawn((s, l, p, t) -> false).setId(key)));
    }

    public static void initialize() {
        MoreMinecrafts.LOGGER.info("Registered heaven sky island prototype blocks ({} flora parts).",
                HeavenFloraPrototypeBlock.PART_COUNT);
    }
}
