package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.item.DyeColor;
import dev.itzgamesgd.moreminecrafts.fishing.FishTrapBlock;

public final class ModBlocks {
    public static final Block DEEPSLATE_TITANIUM_ORE = ore("deepslate_titanium_ore", UniformInt.of(0, 0), 4.5F);
    public static final Block TITANIUM_ORE = ore("titanium_ore", UniformInt.of(0, 0), 3.0F);
    public static final Block DEEPSLATE_TUNGSTEN_ORE = ore("deepslate_tungsten_ore", UniformInt.of(0, 0), 5.0F);
    public static final Block TUNGSTEN_ORE = ore("tungsten_ore", UniformInt.of(0, 0), 3.5F);
    public static final Block DEEPSLATE_MYTHRIL_ORE = ore("deepslate_mythril_ore", UniformInt.of(3, 7), 5.5F);
    public static final Block MYTHRIL_ORE = ore("mythril_ore", UniformInt.of(3, 7), 4.0F);
    public static final Block DEEPSLATE_ETHERIUM_ORE = ore("deepslate_etherium_ore", UniformInt.of(3, 7), 5.5F);
    public static final Block ETHERIUM_ORE = ore("etherium_ore", UniformInt.of(3, 7), 4.0F);
    public static final Block ADAMANTIUM_DEBRIS = block("adamantium_debris", 30.0F, 1200.0F);
    public static final Block TITANIUM_BLOCK = block("titanium_block", 5.0F, 6.0F);
    public static final Block TUNGSTEN_BLOCK = block("tungsten_block", 5.0F, 6.0F);
    public static final Block MYTHRIL_BLOCK = block("mythril_block", 5.0F, 6.0F);
    public static final Block ETHERIUM_BLOCK = block("etherium_block", 5.0F, 6.0F);
    public static final Block ADAMANTIUM_BLOCK = block("adamantium_block", 50.0F, 1200.0F);
    public static final Block EXPLORATION_SLEEPING_BAG = sleepingBag();
    public static final Block FISH_TRAP = fishTrap();

    private ModBlocks() {}

    private static Block ore(String path, UniformInt experience, float strength) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModIds.id(path));
        Block block = new DropExperienceBlock(experience, BlockBehaviour.Properties.of()
                .strength(strength, 3.0F).requiresCorrectToolForDrops().setId(key));
        return register(path, key, block);
    }

    private static Block block(String path, float strength, float resistance) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModIds.id(path));
        return register(path, key, new Block(BlockBehaviour.Properties.of().strength(strength, resistance)
                .requiresCorrectToolForDrops().setId(key)));
    }

    private static Block sleepingBag() {
        String path = "exploration_sleeping_bag";
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModIds.id(path));
        return register(path, key, new BedBlock(DyeColor.GREEN,
                BlockBehaviour.Properties.of().strength(0.2F).noOcclusion().setId(key)));
    }

    private static Block fishTrap() {
        String path = "fish_trap";
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModIds.id(path));
        return register(path, key, new FishTrapBlock(
                BlockBehaviour.Properties.of().strength(2.0F).noOcclusion().setId(key)));
    }

    private static Block register(String path, ResourceKey<Block> key, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ModIds.id(path));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey)));
        return block;
    }

    public static void initialize() {
        MoreMinecrafts.LOGGER.info("Registered five Stage 6 mineral blocks.");
    }
}
