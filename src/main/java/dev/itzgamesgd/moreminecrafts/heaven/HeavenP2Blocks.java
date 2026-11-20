package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Heaven sky island P2 vegetation blocks: reusable model families generated from the V5 design
 * ({@code tools/heaven-p2/build_p2.py}). IDs, strengths and sounds are PROVISIONAL; there are no items,
 * drops, survival rules or growth yet (owner policy pending). Nothing here is placed by world generation.
 */
public final class HeavenP2Blocks {
    private static final List<String> FAMILIES = List.of(
            "heaven_giant_stem", "heaven_giant_leaf", "heaven_giant_bloom",
            "heaven_lavender", "heaven_hydrangea", "heaven_phlox",
            "heaven_flower_red", "heaven_flower_yellow", "heaven_flower_white", "heaven_flower_pink", "heaven_flower_blue", "heaven_flower_violet",
            "heaven_short_grass", "heaven_tall_grass", "heaven_vine", "heaven_wisteria");
    private static final List<Block> PART_BLOCKS = new ArrayList<>();
    /** White canopy of the V5 white-blossom tree: vanilla leaves behaviour, cutout texture, no falling particles. */
    public static final Block HEAVEN_WHITE_BLOSSOM_LEAVES;

    static {
        for (String name : FAMILIES) PART_BLOCKS.add(part(name));
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModIds.id("heaven_white_blossom_leaves"));
        HEAVEN_WHITE_BLOSSOM_LEAVES = Registry.register(BuiltInRegistries.BLOCK, key, new UntintedParticleLeavesBlock(0.0F,
                ParticleTypes.CHERRY_LEAVES, AmbientLeavesBlockSoundPlayer.noAmbientSound(),
                BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.SNOW).setId(key)));
    }

    private HeavenP2Blocks() {}

    private static Block part(String name) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModIds.id(name));
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noOcclusion().noLootTable()
                .pushReaction(PushReaction.POPPED).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos, box) -> false)
                .isRedstoneConductor((s, l, pos) -> false).isValidSpawn((s, l, pos, t) -> false);
        if (name.startsWith("heaven_giant_")) p = p.strength(0.4F).sound(name.endsWith("stem") ? SoundType.BAMBOO : SoundType.AZALEA_LEAVES);
        else p = p.instabreak().noCollision().sound(SoundType.GRASS);
        return Registry.register(BuiltInRegistries.BLOCK, key, HeavenPartBlock.create(name, p.setId(key)));
    }

    public static List<Block> partBlocks() { return Collections.unmodifiableList(PART_BLOCKS); }
    public static List<String> families() { return FAMILIES; }

    public static void initialize() {
        int parts = PART_BLOCKS.stream().mapToInt(b -> ((HeavenPartBlock) b).partCount()).sum();
        MoreMinecrafts.LOGGER.info("Registered heaven P2 vegetation: {} part families ({} models) + white blossom leaves.", PART_BLOCKS.size(), parts);
    }
}
