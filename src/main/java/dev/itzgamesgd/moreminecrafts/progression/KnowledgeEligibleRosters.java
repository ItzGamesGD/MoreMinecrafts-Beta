package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Explicit, version-checked registries for K8 rocks and K14 vanilla tools. */
public final class KnowledgeEligibleRosters {
    public record RockType(Identifier id, Block block, String acquisition, String classification) {}

    private static final List<RockType> ROCK_TYPES = List.of(
            rock("stone", Blocks.STONE, "Mine exposed stone with a pickaxe; drops cobblestone without Silk Touch.", "Common natural rock substrate."),
            rock("granite", Blocks.GRANITE, "Mine in Overworld granite deposits with a pickaxe.", "Naturally generated igneous rock variant."),
            rock("diorite", Blocks.DIORITE, "Mine in Overworld diorite deposits with a pickaxe.", "Naturally generated igneous rock variant."),
            rock("andesite", Blocks.ANDESITE, "Mine in Overworld andesite deposits with a pickaxe.", "Naturally generated igneous rock variant."),
            rock("tuff", Blocks.TUFF, "Mine in Overworld underground tuff deposits with a pickaxe.", "Naturally generated volcanic rock."),
            rock("deepslate", Blocks.DEEPSLATE, "Mine below the deepslate transition with a pickaxe.", "Naturally generated deep-crust rock."),
            rock("calcite", Blocks.CALCITE, "Mine exposed amethyst-geode calcite with a pickaxe.", "Naturally generated crystalline mineral rock."),
            rock("dripstone_block", Blocks.DRIPSTONE_BLOCK, "Mine dripstone cave deposits with a pickaxe.", "Naturally generated cave limestone."),
            rock("basalt", Blocks.BASALT, "Mine basalt deltas or naturally generated basalt formations with a pickaxe.", "Naturally generated volcanic rock."),
            rock("blackstone", Blocks.BLACKSTONE, "Mine in Nether basalt-delta and bastion terrain with a pickaxe.", "Naturally generated Nether rock."),
            rock("netherrack", Blocks.NETHERRACK, "Mine exposed Nether terrain with a pickaxe.", "Common naturally generated Nether rock."),
            rock("end_stone", Blocks.END_STONE, "Mine exposed End-island terrain with a pickaxe.", "Common naturally generated End rock.")
    );

    private static final Set<String> TOOL_MATERIALS = Set.of(
            "wooden", "stone", "copper", "iron", "golden", "diamond", "netherite");
    private static final Set<String> TOOL_FAMILIES = Set.of("pickaxe", "axe", "shovel", "hoe", "sword");

    private KnowledgeEligibleRosters() {}

    private static RockType rock(String path, Block block, String acquisition, String classification) {
        return new RockType(Identifier.withDefaultNamespace(path), block, acquisition, classification);
    }

    public static List<RockType> rockTypes() { return ROCK_TYPES; }

    public static boolean isEligibleRock(Block block) {
        return ROCK_TYPES.stream().anyMatch(type -> type.block() == block);
    }

    /** Only registered vanilla tools from the approved material/family cross-product are eligible. */
    public static List<Item> vanillaToolItems() {
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (isEligibleVanillaToolId(id)) items.add(item);
        }
        items.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return List.copyOf(items);
    }

    public static boolean isEligibleVanillaTool(Item item) {
        return item != null && item != net.minecraft.world.item.Items.AIR
                && isEligibleVanillaToolId(BuiltInRegistries.ITEM.getKey(item));
    }

    public static boolean isEligibleVanillaToolId(Identifier id) {
        if (id == null || !id.getNamespace().equals("minecraft")) return false;
        String path = id.getPath();
        for (String family : TOOL_FAMILIES) {
            String suffix = "_" + family;
            if (!path.endsWith(suffix)) continue;
            return TOOL_MATERIALS.contains(path.substring(0, path.length() - suffix.length()));
        }
        return false;
    }

    public static Set<String> rockIds() {
        return ROCK_TYPES.stream().map(type -> type.id().toString()).collect(Collectors.toUnmodifiableSet());
    }
}
