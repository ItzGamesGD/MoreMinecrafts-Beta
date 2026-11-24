package dev.itzgamesgd.moreminecrafts.progression;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

/** Server runtime for `.52` Archaeology chest rewards and `.54`/`piglin_barter` Piglin bartering. */
public final class ResearchWorldLootRuntime {
    private static final Item[] ARCHAEOLOGY_THEMATIC_POOL = {
            Items.ANGLER_POTTERY_SHERD,
            Items.ARCHER_POTTERY_SHERD,
            Items.ARMS_UP_POTTERY_SHERD,
            Items.BLADE_POTTERY_SHERD,
            Items.EXPLORER_POTTERY_SHERD,
            Items.PRIZE_POTTERY_SHERD,
            Items.SHEAF_POTTERY_SHERD,
            Items.SKULL_POTTERY_SHERD,
            Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE
    };

    private static final Item[] ARCHAEOLOGY_RARE_POOL = {
            Items.DIAMOND,
            Items.EMERALD,
            Items.GOLD_INGOT,
            Items.SNIFFER_EGG
    };

    private static final Item[] PIGLIN_RARE_BARTER_POOL = {
            Items.ENDER_PEARL,
            Items.CRYING_OBSIDIAN,
            Items.OBSIDIAN,
            Items.QUARTZ,
            Items.SPECTRAL_ARROW,
            Items.FIRE_CHARGE
    };

    private ResearchWorldLootRuntime() {}

    /** Applies `.52` structure-chest rare/thematic rolls for the first player who opens the chest. */
    public static void applyArchaeologyChestBonus(ServerPlayer opener, ResourceKey<LootTable> table,
                                                  Container container) {
        if (opener == null || table == null || container == null) return;
        String path = table.identifier().getPath();
        if (!ResearchWorldLootRules.isArchaeologyStructureChest(path)) return;
        int level52 = ResearchRewards.level(opener, "v3.research.52", "archaeology_study");
        if (level52 <= 0) return;

        RandomSource random = opener.getRandom();
        if (ResearchWorldLootRules.rollsUnder(random.nextDouble(),
                ResearchWorldLootRules.archaeologyRareBonusChance(level52))) {
            Item rare = ARCHAEOLOGY_RARE_POOL[random.nextInt(ARCHAEOLOGY_RARE_POOL.length)];
            insertIntoFirstEmptySlot(container, new ItemStack(rare, 1));
        }
        if (ResearchWorldLootRules.rollsUnder(random.nextDouble(),
                ResearchWorldLootRules.archaeologyThematicExtraChance(level52))) {
            Item thematic = ARCHAEOLOGY_THEMATIC_POOL[random.nextInt(ARCHAEOLOGY_THEMATIC_POOL.length)];
            insertIntoFirstEmptySlot(container, new ItemStack(thematic, 1));
        }
    }

    /** Applies `.54` (3-level rare weight) and V4 `piglin_barter` (separate 1-level bonus) to barter drops. */
    public static List<ItemStack> applyPiglinBarterBonus(Piglin piglin, List<ItemStack> baseDrops) {
        if (baseDrops == null || baseDrops.isEmpty()) return baseDrops;
        ServerPlayer offerer = KnowledgeEventCollectors.peekPiglinOfferer(piglin);
        if (offerer == null) return baseDrops;

        int level54 = ResearchRewards.level(offerer, "v3.research.54", "piglin_barter_rare_weight");
        int levelV4 = ResearchRewards.level(offerer, "piglin_barter", "piglin_barter_improvement");
        if (level54 <= 0 && levelV4 <= 0) return baseDrops;

        List<ItemStack> result = new ArrayList<>(baseDrops);
        RandomSource random = offerer.getRandom();
        if (ResearchWorldLootRules.rollsUnder(random.nextDouble(),
                ResearchWorldLootRules.piglinBarterRareChance(level54))) {
            Item rare = PIGLIN_RARE_BARTER_POOL[random.nextInt(PIGLIN_RARE_BARTER_POOL.length)];
            result.add(new ItemStack(rare, 1));
        }
        if (levelV4 > 0 && ResearchWorldLootRules.rollsUnder(random.nextDouble(),
                ResearchWorldLootRules.V4_PIGLIN_BARTER_BONUS_CHANCE)) {
            ItemStack copy = baseDrops.getFirst().copyWithCount(1);
            if (!copy.isEmpty()) result.add(copy);
        }
        return result;
    }

    private static void insertIntoFirstEmptySlot(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack);
                container.setChanged();
                return;
            }
        }
    }
}
