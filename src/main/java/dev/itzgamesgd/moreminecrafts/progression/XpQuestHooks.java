package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Set;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Q1-B live evidence for XP Quests. Every method runs on the server thread, attributes progress to
 * the acting player, and writes only {@code quest.counter.*}, {@code quest.seen.*} and
 * {@code quest.baseline.*} keys through the single {@link KnowledgeRuntime#updateState} path.
 *
 * <p>Retroactive history uses a <em>baseline</em> captured once per key, before the first live event:
 * progress = baseline + live counter. A live event is never also part of its baseline, so history is
 * recognised without double counting (owner policy 3).</p>
 */
public final class XpQuestHooks {
    /** Knowledge counters whose collectors report "stored + n" per event; their deltas are exact events. */
    static final Set<String> INCREMENT_CONDITIONS = Set.of(
            "archaeology_items_brushed", "shield_blocks", "trident_hostile_kills", "natural_vanilla_logs_mined",
            "pickaxe_durability_used", "crops_harvested", "farmland_tilled", "piglin_barters", "fishing_successes",
            "anvil_repairs", "redstone_components_placed", "potions_brewed");
    static final String BLOCKS_PLACED = "blocks_placed";
    static final String BLOCK_ITEMS_USED_BASELINE = "block_items_used";
    static final String PLACED_BLOCK_SET = "set.blocks_placed";
    static final String NATURAL_ORE_SET = "set.natural_ore_mined";
    static final String TAMED_SET = "set.tamed";
    static final String COPPER_AGES_SET = "set.copper_ages";
    static final Set<String> COPPER_AGE_BLOCKS = Set.of(
            "minecraft:copper_block", "minecraft:exposed_copper",
            "minecraft:weathered_copper", "minecraft:oxidized_copper");
    static final String FISH_GRADE_5_SET = "set.fish_grade_5_species";
    static final String POTION_ENHANCED_PAIRS_SET = "set.potion_enhanced_pairs";
    static final String CRAFTED_QUEST_ITEMS_SET = "set.crafted_quest_items";
    static final String CRAFTED_SHULKER_COLORS_SET = "set.crafted_shulker_colors";
    static final String CRAFTED_RECIPES_SET = "set.crafted_recipes";
    static final String VILLAGER_PROFESSIONS_SET = "set.villager_professions";
    static final String EXPLORER_MAPS_SET = "set.explorer_maps";
    static final String HEIGHT_EXTREMES_SET = "set.overworld_height_extremes";
    static final String ARCHAEOLOGY_SHERDS_SET = "set.archaeology_sherds";
    static final String ALL_WOOD_SET = "set.all_wood_mined";
    static final String HARVEST_CROPS_SET = "set.harvest_all_crops";
    static final String ANCIENT_HARVEST_SET = "set.ancient_harvest";
    static final String MM_METALLURGY_SET = "set.mm_metallurgy";
    static final Set<String> EXPLORER_MAP_DECORATIONS = Set.of(
            "minecraft:monument", "minecraft:mansion", "minecraft:trial_chambers");
    static final Set<String> ALL_WOOD_BLOCKS = Set.of(
            "minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log",
            "minecraft:jungle_log", "minecraft:acacia_log", "minecraft:dark_oak_log",
            "minecraft:mangrove_log", "minecraft:cherry_log", "minecraft:pale_oak_log",
            "minecraft:crimson_stem", "minecraft:warped_stem");
    static final Set<String> MM_METALLURGY_ITEMS = Set.of(
            "moreminecrafts:titanium_ingot", "moreminecrafts:mythril",
            "moreminecrafts:etherium_ingot", "moreminecrafts:adamantium_ingot");
    static final Set<String> TRACKED_CRAFT_ITEMS = Set.of(
            "minecraft:cake", "minecraft:pumpkin_pie", "minecraft:cookie", "minecraft:bread", "minecraft:rabbit_stew",
            "moreminecrafts:exploration_sleeping_bag", "moreminecrafts:parachute",
            "moreminecrafts:wood_dagger", "moreminecrafts:stone_dagger", "moreminecrafts:copper_dagger",
            "moreminecrafts:iron_dagger", "moreminecrafts:gold_dagger", "moreminecrafts:diamond_dagger",
            "moreminecrafts:netherite_dagger",
            "moreminecrafts:wood_greatsword", "moreminecrafts:stone_greatsword", "moreminecrafts:copper_greatsword",
            "moreminecrafts:iron_greatsword", "moreminecrafts:gold_greatsword", "moreminecrafts:diamond_greatsword",
            "moreminecrafts:netherite_greatsword",
            "moreminecrafts:titanium_block", "moreminecrafts:mythril_block",
            "moreminecrafts:etherium_block", "moreminecrafts:adamantium_block");
    /** Bound for per-player distinct sets; the largest catalog target is 200. */
    static final int DISTINCT_CAP = 200;
    private record PendingBreak(Level level, BlockPos pos, BlockState state, boolean naturalOre,
                                boolean stemGrownFruit, boolean naturalTorchflower) {}
    private static final Map<ServerPlayer, Deque<PendingBreak>> PENDING_BREAK = new WeakHashMap<>();

    private XpQuestHooks() {}

    /** Registered before XP Quest reconciliation so baselines exist before the first evaluation. */
    static void initialize() {
        QuestOreProvenance.initialize();
        QuestNaturalCropProvenance.initialize();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> captureBaselines(handler.player));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                boolean naturalOre = QuestOreProvenance.isTrackedOre(state)
                        && QuestOreProvenance.verdict(level, pos) == QuestOreProvenance.Verdict.NATURAL;
                boolean stemFruit = hasAttachedStemFacing(level, pos, state);
                boolean naturalTorchflower = state.is(Blocks.TORCHFLOWER)
                        && QuestNaturalCropProvenance.isNatural(level, pos);
                PENDING_BREAK.computeIfAbsent(serverPlayer, ignored -> new ArrayDeque<>())
                        .push(new PendingBreak(level, pos.immutable(), state, naturalOre, stemFruit, naturalTorchflower));
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                PendingBreak pending = removePending(serverPlayer, level, pos, state);
                if (pending != null && pending.naturalOre()) oreMined(serverPlayer, state);
                handleBatch5BlockBreak(serverPlayer, level, state, pending);
            }
        });
        PlayerBlockBreakEvents.CANCELED.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) removePending(serverPlayer, level, pos, state);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PENDING_BREAK.remove(handler.player));
    }

    private static boolean hasAttachedStemFacing(Level level, BlockPos pos, BlockState state) {
        Block expectedStem = state.is(Blocks.MELON) ? Blocks.ATTACHED_MELON_STEM
                : state.is(Blocks.PUMPKIN) ? Blocks.ATTACHED_PUMPKIN_STEM : null;
        if (expectedStem == null) return false;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.is(expectedStem) && neighbor.hasProperty(AttachedStemBlock.FACING)
                    && neighborPos.relative(neighbor.getValue(AttachedStemBlock.FACING)).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /** Match the most recent frame; nested Vein Mining breaks can occur before an outer AFTER. */
    private static PendingBreak removePending(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        Deque<PendingBreak> frames = PENDING_BREAK.get(player);
        if (frames == null) return null;
        for (Iterator<PendingBreak> iterator = frames.iterator(); iterator.hasNext();) {
            PendingBreak pending = iterator.next();
            if (pending.level() == level && pending.pos().equals(pos)
                    && pending.state().getBlock() == state.getBlock()) {
                iterator.remove();
                if (frames.isEmpty()) PENDING_BREAK.remove(player);
                return pending;
            }
        }
        return null;
    }

    static String baselineKey(String key) { return "quest.baseline." + key; }

    static boolean hasBaseline(ResearchProgressState state, String key) {
        return state.counters().containsKey(baselineKey(key));
    }

    /** Captures every not-yet-captured baseline from persisted evidence. Idempotent. */
    public static void captureBaselines(ServerPlayer player) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        for (String condition : INCREMENT_CONDITIONS)
            if (!hasBaseline(next, condition)) next = next.with(baselineKey(condition), next.conditionProgress(condition));
        next = capturePlacementBaseline(player, next);
        if (!next.equals(before)) KnowledgeRuntime.updateState(player, next);
    }

    private static ResearchProgressState capturePlacementBaseline(ServerPlayer player, ResearchProgressState state) {
        if (hasBaseline(state, BLOCK_ITEMS_USED_BASELINE)) return state;
        long used = 0;
        ResearchProgressState next = state;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem blockItem)) continue;
            int count = player.getStats().getValue(Stats.ITEM_USED.get(item));
            if (count <= 0) continue;
            used += count;
            next = observeBounded(next, PLACED_BLOCK_SET, BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());
        }
        return next.with(baselineKey(BLOCK_ITEMS_USED_BASELINE), (int) Math.min(Integer.MAX_VALUE, used));
    }

    private static ResearchProgressState observeBounded(ResearchProgressState state, String set, String entry) {
        if (QuestRules.distinctCount(state, set) >= DISTINCT_CAP) return state;
        return QuestRules.observeDistinct(state, set, entry);
    }

    private static void write(ServerPlayer player, ResearchProgressState before, ResearchProgressState after) {
        if (after != before && !after.equals(before)) KnowledgeRuntime.updateState(player, after);
    }

    /**
     * Called by {@link KnowledgeRuntime#recordCondition} before it stores a new total. For the
     * increment-style collectors the difference between the reported total and the stored (capped)
     * value is exactly this event's amount, even after the Knowledge counter reached its cap.
     */
    static void knowledgeConditionReported(ServerPlayer player, String condition, int reportedTotal) {
        if (!INCREMENT_CONDITIONS.contains(condition)) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        if (!hasBaseline(next, condition)) next = next.with(baselineKey(condition), next.conditionProgress(condition));
        long delta = (long) reportedTotal - next.conditionProgress(condition);
        if (delta > 0) next = QuestRules.addCounter(next, condition, delta);
        write(player, before, next);
    }

    /** BlockItem.place returned Success for this player: one confirmed placement. */
    public static void blockPlaced(ServerPlayer player, BlockItem item) {
        Block block = item.getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = capturePlacementBaseline(player, before);
        next = QuestRules.addCounter(next, BLOCKS_PLACED, 1);
        next = observeBounded(next, PLACED_BLOCK_SET, blockId);
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST
                || block == Blocks.BARREL || block instanceof ShulkerBoxBlock) {
            next = QuestRules.addCounter(next, "storage_placed", 1);
        }
        if (COPPER_AGE_BLOCKS.contains(blockId)) {
            next = QuestRules.observeDistinct(next, COPPER_AGES_SET, blockId);
        }
        write(player, before, next);
    }

    /** Called only after a successful break whose pre-break origin was positively verified. */
    private static void oreMined(ServerPlayer player, BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = QuestRules.addCounter(before, "natural_ore_mined", 1);
        next = QuestRules.addCounter(next, "natural_mined." + id, 1);
        next = observeBounded(next, NATURAL_ORE_SET, id);
        if (id.equals("moreminecrafts:mythril_ore") || id.equals("moreminecrafts:deepslate_mythril_ore")) {
            next = QuestRules.observeDistinct(next, MM_METALLURGY_SET, "moreminecrafts:mythril");
        }
        write(player, before, next);
    }

    private static void handleBatch5BlockBreak(ServerPlayer player, Level level, BlockState state, PendingBreak pending) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (mode == GameType.CREATIVE || mode == GameType.SPECTATOR) return;
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;

        if (ALL_WOOD_BLOCKS.contains(blockId)) {
            next = QuestRules.observeDistinct(next, ALL_WOOD_SET, blockId);
        }

        if (state.is(Blocks.AMETHYST_CLUSTER)) {
            var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            if (EnchantmentHelper.getItemEnchantmentLevel(silk, player.getMainHandItem()) == 0) {
                int shards = player.getMainHandItem().is(ItemTags.PICKAXES) ? 4 : 2;
                next = QuestRules.addCounter(next, "amethyst_shards_mined", shards);
                if (QuestRules.counter(next, "amethyst_shards_mined") >= 32) {
                    next = QuestRules.seedCounter(next, "amethyst_32_done", 1);
                }
            }
        }

        if ((blockId.equals("minecraft:wheat") || blockId.equals("minecraft:carrots")
                || blockId.equals("minecraft:potatoes") || blockId.equals("minecraft:beetroots"))
                && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            next = QuestRules.observeDistinct(next, HARVEST_CROPS_SET, blockId);
        } else if (state.getBlock() instanceof NetherWartBlock
                && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
            next = QuestRules.observeDistinct(next, HARVEST_CROPS_SET, "minecraft:nether_wart");
        } else if (state.getBlock() instanceof CocoaBlock
                && state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE) {
            next = QuestRules.observeDistinct(next, HARVEST_CROPS_SET, "minecraft:cocoa");
        } else if (state.getBlock() instanceof SweetBerryBushBlock
                && state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE) {
            next = QuestRules.observeDistinct(next, HARVEST_CROPS_SET, "minecraft:sweet_berry_bush");
        } else if (pending != null && pending.stemGrownFruit()
                && (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN))) {
            next = QuestRules.observeDistinct(next, HARVEST_CROPS_SET, blockId);
        }

        if (pending != null && pending.naturalTorchflower() && state.is(Blocks.TORCHFLOWER)) {
            next = QuestRules.observeDistinct(next, ANCIENT_HARVEST_SET, "minecraft:torchflower");
        } else if (state.getBlock() instanceof PitcherCropBlock
                && state.getValue(PitcherCropBlock.AGE) >= PitcherCropBlock.MAX_AGE) {
            next = QuestRules.observeDistinct(next, ANCIENT_HARVEST_SET, "minecraft:pitcher_crop");
        }

        write(player, before, next);
    }

    /** Vanilla TAME_ANIMAL fired for this player (TamableAnimal.tame / AbstractHorse.tameWithName). */
    public static void tamed(ServerPlayer player, Entity animal) {
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, observeBounded(before, TAMED_SET, type.toString()));
    }

    /** A movement statistic (centimetres) awarded while the player is in the Nether. */
    public static void netherMovement(ServerPlayer player, int centimetres) {
        if (centimetres <= 0 || player.level().dimension() != Level.NETHER) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.addCounter(before, "nether_travel_cm", centimetres));
    }

    /** One fish sold to a Fisherman for {@code emeralds} emeralds. */
    public static void fishSold(ServerPlayer player, int emeralds) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = QuestRules.addCounter(before, "fish_sold", 1);
        next = QuestRules.addCounter(next, "fish_sale_emeralds", Math.max(0, emeralds));
        write(player, before, next);
    }

    /** Confirmed custom fishing capture with rolled grade (1-5). */
    public static void fishCaught(ServerPlayer player, String speciesId, int grade) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        if (grade >= 4) {
            next = QuestRules.addCounter(next, "fish_grade_4_plus", 1);
        }
        if (grade >= 5) {
            next = QuestRules.addCounter(next, "fish_grade_5", 1);
            next = QuestRules.observeDistinct(next, FISH_GRADE_5_SET, speciesId);
        }
        write(player, before, next);
    }

    /** Confirmed multi-catch fishing challenge yielding two or more captured fish. */
    public static void fishingMultiCatch(ServerPlayer player) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.addCounter(before, "fishing_multi_catch", 1));
    }

    /** Verified smelted output items taken by the player from a furnace, blast furnace, or smoker. */
    public static void smeltedCollected(ServerPlayer player, ItemStack taken, int count) {
        if (count <= 0) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = QuestRules.addCounter(before, "smelted_items", count);
        if (!taken.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(taken.getItem()).toString();
            if (MM_METALLURGY_ITEMS.contains(itemId)) {
                next = QuestRules.observeDistinct(next, MM_METALLURGY_SET, itemId);
            }
        }
        write(player, before, next);
    }

    /** Verified brewed potion taken by the player from a brewing stand. */
    public static void potionCollected(ServerPlayer player, ItemStack result, int brewedCount) {
        if (brewedCount <= 0 || result.isEmpty()) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        var typeOpt = KnowledgeEventRules.potionType(result);
        if (typeOpt.isPresent()) {
            String type = typeOpt.get();
            if (type.equals("moreminecrafts:shock")
                    || type.startsWith("moreminecrafts:strong_shock")
                    || type.startsWith("moreminecrafts:long_shock")) {
                next = QuestRules.addCounter(next, "shock_potions_brewed", brewedCount);
            }
            if (result.getItem() instanceof SplashPotionItem) {
                next = observeBounded(next, "alc.splash_types", type);
                next = QuestRules.seedCounter(next, "splash_types_3",
                        Math.min(3, QuestRules.distinctCount(next, "alc.splash_types")));
            }
            if (result.getItem() instanceof LingeringPotionItem) {
                next = observeBounded(next, "alc.lingering_types", type);
                next = QuestRules.seedCounter(next, "lingering_types_3",
                        Math.min(3, QuestRules.distinctCount(next, "alc.lingering_types")));
            }
            var family = KnowledgeEventRules.potionFamilyBase(type);
            if (family.isPresent()) {
                String base = family.get();
                if (KnowledgeEventRules.isStrengthenedPotionId(type)) {
                    next = observeBounded(next, "alc.strong_families", base);
                } else if (KnowledgeEventRules.isExtendedPotionId(type)) {
                    next = observeBounded(next, "alc.long_families", base);
                }
                if (next.get(QuestRules.seenKey("alc.strong_families", base)) != 0
                        && next.get(QuestRules.seenKey("alc.long_families", base)) != 0) {
                    next = QuestRules.observeDistinct(next, POTION_ENHANCED_PAIRS_SET, base);
                }
            }
        }
        write(player, before, next);
    }

    /** Confirmed server-side crafting result take; excludes Creative and Spectator modes (K14 parity). */
    public static void crafted(ServerPlayer player, ItemStack crafted, String recipeId) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (mode == GameType.CREATIVE || mode == GameType.SPECTATOR || crafted.isEmpty()) return;
        String itemId = BuiltInRegistries.ITEM.getKey(crafted.getItem()).toString();
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        if (TRACKED_CRAFT_ITEMS.contains(itemId)) {
            next = QuestRules.observeDistinct(next, CRAFTED_QUEST_ITEMS_SET, itemId);
        }
        if (MM_METALLURGY_ITEMS.contains(itemId)) {
            next = QuestRules.observeDistinct(next, MM_METALLURGY_SET, itemId);
        }
        if (crafted.is(Items.SPYGLASS)) {
            next = QuestRules.seedCounter(next, "spyglass_crafted", 1);
        }
        if (crafted.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock
                && !crafted.is(Items.SHULKER_BOX)) {
            next = QuestRules.observeDistinct(next, CRAFTED_SHULKER_COLORS_SET, itemId);
        }
        if (recipeId != null && !recipeId.isBlank()) {
            next = observeBounded(next, CRAFTED_RECIPES_SET, recipeId);
        }
        write(player, before, next);
    }

    /** Confirmed server-side villager trade. */
    public static void villagerTraded(ServerPlayer player, Villager villager, MerchantOffer offer) {
        VillagerData data = villager.getVillagerData();
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        if (data.level() >= 5) {
            next = QuestRules.addCounter(next, "master_villager_trades", 1);
        }
        var professionKey = data.profession().unwrapKey();
        if (professionKey.isPresent()
                && !professionKey.get().equals(VillagerProfession.NONE)
                && !professionKey.get().equals(VillagerProfession.NITWIT)) {
            next = QuestRules.observeDistinct(next, VILLAGER_PROFESSIONS_SET,
                    professionKey.get().identifier().toString());
        }
        if (data.profession().is(VillagerProfession.LIBRARIAN) && offer != null) {
            ItemStack result = offer.getResult();
            if (result.is(Items.ENCHANTED_BOOK)
                    && T1Operations.enchantments(result).getOrDefault("minecraft:mending", 0) > 0) {
                next = QuestRules.addCounter(next, "librarian_mending_trades", 1);
            }
        }
        if (data.profession().is(VillagerProfession.CARTOGRAPHER) && offer != null) {
            ItemStack result = offer.getResult();
            if (result.is(Items.FILLED_MAP)) {
                MapDecorations decorations = result.get(DataComponents.MAP_DECORATIONS);
                if (decorations != null) {
                    for (MapDecorations.Entry entry : decorations.decorations().values()) {
                        var typeKey = entry.type().unwrapKey();
                        if (typeKey.isPresent()) {
                            String id = typeKey.get().identifier().toString();
                            if (EXPLORER_MAP_DECORATIONS.contains(id)) {
                                next = QuestRules.observeDistinct(next, EXPLORER_MAPS_SET, id);
                            }
                        }
                    }
                }
            }
        }
        write(player, before, next);
    }

    /** Observes Overworld Y>=300 and Y<=-60 standing positions without extra tick overhead on normal heights. */
    public static void checkOverworldHeightExtremes(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        double y = player.getY();
        if (y < 300.0D && y > -60.0D) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        if (y >= 300.0D) next = QuestRules.observeDistinct(next, HEIGHT_EXTREMES_SET, "high");
        if (y <= -60.0D) next = QuestRules.observeDistinct(next, HEIGHT_EXTREMES_SET, "low");
        write(player, before, next);
    }

    /** Confirmed archaeology brush loot produced on the server. */
    public static void archaeologyItemProduced(ServerPlayer player, ItemStack result) {
        if (result.isEmpty()) return;
        Identifier id = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (!id.getNamespace().equals("minecraft") || !id.getPath().endsWith("_pottery_sherd")) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.observeDistinct(before, ARCHAEOLOGY_SHERDS_SET, id.toString()));
    }

    /** Confirmed genuine Blaze Rod death-loot pickup into inventory. */
    public static void blazeRodPickedUp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.addCounter(before, "blaze_rod_drops", amount));
    }

    /** Confirmed honey bottle harvest from a full beehive. */
    public static void honeyBottleHarvested(ServerPlayer player) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.addCounter(before, "honey_bottles_harvested", 1));
    }

    /** Confirmed right-click or break harvest of a tracked crop ID. */
    public static void cropHarvested(ServerPlayer player, String cropId) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.observeDistinct(before, HARVEST_CROPS_SET, cropId));
    }

    /** Confirmed T1 enchantment purchase at an Enchanting Table with at least 15 valid bookshelves. */
    public static void enchantedWithBookshelves(ServerPlayer player) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.addCounter(before, "enchantments_with_bookshelves", 1));
    }

    /** Confirmed Conduit activation for nearby server players. */
    public static void conduitActivated(ServerPlayer player) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        write(player, before, QuestRules.addCounter(before, "conduits_activated", 1));
    }
}
