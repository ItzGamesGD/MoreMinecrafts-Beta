package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Server-owned knowledge state; permanent and free, independent from paid research. */
public final class KnowledgeRuntime {
    public static final AttachmentType<ResearchProgressState> STATE = AttachmentRegistry.create(
            ModIds.id("research_progress_v4"), builder -> builder
                    .initializer(ResearchProgressState::empty)
                    .persistent(ResearchProgressState.CODEC)
                    .copyOnDeath());

    private KnowledgeRuntime() {}
    private static List<Item> logItems = List.of();
    private static List<Item> plantItems = List.of();
    private static List<Item> seedItems = List.of();
    private static List<Item> foodItems = List.of();
    private static final PickaxeDamageSnapshotTracker PICKAXE_DAMAGE = new PickaxeDamageSnapshotTracker();
    private static final java.util.Set<java.util.UUID> SILENT_RECONCILIATION = new java.util.HashSet<>();

    private static void cacheTrackedItems() {
        var logs = new java.util.ArrayList<Item>();
        var plants = new java.util.ArrayList<Item>();
        var seeds = new java.util.ArrayList<Item>();
        var foods = new java.util.ArrayList<Item>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack probe = new ItemStack(item);
            if (probe.is(ItemTags.LOGS)) logs.add(item);
            if (probe.is(ItemTags.SAPLINGS)) plants.add(item);
            if (probe.has(net.minecraft.core.component.DataComponents.FOOD)) foods.add(item);
        }
        plants.addAll(List.of(Items.DANDELION, Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM,
                Items.AZURE_BLUET, Items.RED_TULIP, Items.ORANGE_TULIP, Items.WHITE_TULIP,
                Items.PINK_TULIP, Items.OXEYE_DAISY, Items.CORNFLOWER, Items.LILY_OF_THE_VALLEY,
                Items.SUNFLOWER, Items.LILAC, Items.ROSE_BUSH, Items.PEONY));
        seeds.addAll(List.of(Items.WHEAT_SEEDS, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS,
                Items.BEETROOT_SEEDS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD));
        plants.addAll(seeds);
        logItems = List.copyOf(logs);
        plantItems = List.copyOf(plants);
        seedItems = List.copyOf(seeds);
        foodItems = List.copyOf(foods);
    }

    public static void initialize() {
        K5NaturalTreeProvenance.initialize();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            ResearchProgressLookup.playerJoined(server, player.getUUID());
            SILENT_RECONCILIATION.add(player.getUUID());
            try {
                ResearchProgressState beforeMigration = state(player);
                ResearchProgressState migrated = ResearchRootMigration.migrate(beforeMigration);
                if (!migrated.equals(beforeMigration)) updateState(player, migrated);
                observeExploration(player);
                observeEffectTypes(player);
                refresh(player);
            } finally {
                SILENT_RECONCILIATION.remove(player.getUUID());
            }
        });
        // Registry item components are not bound during the mod entrypoint. Cache tags only
        // after the server has completed datapack/registry startup.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> cacheTrackedItems());
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            String blockId = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            String blockPath = blockId.substring(blockId.indexOf(':') + 1);
            if (blockPath.endsWith("_ore") || blockPath.equals("ancient_debris")) {
                // Deepslate variants are still the same ore type for the general survey.
                recordDistinctCondition(serverPlayer, "ore_types_mined", blockId.replace("deepslate_", ""));
                if (blockId.contains("deepslate_")) recordDistinctCondition(serverPlayer, "deepslate_ore_types_mined", blockId);
                recordCondition(serverPlayer, "ore_items_mined", value(serverPlayer,"condition.ore_items_mined") + 1);
            }
            if (!serverPlayer.gameMode.isCreative())
                KnowledgeEventCollectors.rockTypeMined(serverPlayer, state);
            if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop
                    && state.getValue(net.minecraft.world.level.block.CropBlock.AGE) >= crop.getMaxAge())
                recordCondition(serverPlayer, "crops_harvested", value(serverPlayer,"condition.crops_harvested") + 1);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ResearchProgressLookup.playerDisconnected(server, handler.player);
            PICKAXE_DAMAGE.playerDisconnected(handler.player.getUUID());
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            String entityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
            String path = entityId.substring(entityId.indexOf(':') + 1);
            ServerPlayer player = damageSource.getEntity() instanceof ServerPlayer p ? p : null;
            if (player != null) {
                String killKey = switch (path) {
                    case "zombie" -> "zombie_kills";
                    case "skeleton" -> "skeleton_kills";
                    case "creeper" -> "creeper_kills";
                    case "spider", "cave_spider" -> "spider_kills";
                    case "enderman" -> "enderman_kills";
                    case "slime" -> "slime_kills";
                    case "hoglin" -> "hoglin_kills";
                    case "pillager", "vindicator" -> "pillager_and_vindicator_kills";
                    case "drowned", "guardian" -> "drowned_and_guardian_kills";
                    case "ender_dragon", "wither" -> "ender_dragon_or_wither_killed";
                    default -> null;
                };
                if (killKey != null) {
                    int amount = killKey.equals("pillager_and_vindicator_kills") || killKey.equals("drowned_and_guardian_kills")
                            ? 1 : value(player, "condition." + killKey) + 1;
                    if (killKey.equals("pillager_and_vindicator_kills") || killKey.equals("drowned_and_guardian_kills"))
                        recordDistinctCondition(player, killKey, path);
                    else recordCondition(player, killKey, amount);
                }
                if (entity instanceof net.minecraft.world.entity.monster.Monster) {
                    if (path.equals("piglin"))
                        recordCondition(player, "piglin_kills", value(player,"condition.piglin_kills") + 1);
                    if (player.getMainHandItem().is(ItemTags.SWORDS))
                        recordCondition(player, "sword_hostile_kills", value(player,"condition.sword_hostile_kills") + 1);
                    if (player.getMainHandItem().is(ItemTags.AXES))
                        recordCondition(player, "axe_hostile_kills", value(player,"condition.axe_hostile_kills") + 1);
                    if (player.isSprinting())
                        recordCondition(player, "running_hostile_kills", value(player,"condition.running_hostile_kills") + 1);
                }
            }
            var direct = damageSource.getDirectEntity();
            if (entity instanceof net.minecraft.world.entity.monster.Monster
                    && direct instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident trident
                    && trident.getWeaponItem().is(Items.TRIDENT)
                    && trident.getOwner() instanceof ServerPlayer thrower)
                recordCondition(thrower, "trident_hostile_kills",
                        value(thrower, "condition.trident_hostile_kills") + 1);
            if (entity instanceof net.minecraft.world.entity.monster.Monster
                    && direct instanceof net.minecraft.world.entity.projectile.Projectile projectile)
                KnowledgeEventCollectors.projectileKill(projectile);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean refreshStatistics = server.getTickCount() % 100 == 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                observeExploration(player);
                observeEffectTypes(player);
                if (refreshStatistics) refresh(player);
            }
        });
    }

    private static void set(ServerPlayer player, String key, int value) {
        ResearchProgressState old = player.getAttachedOrCreate(STATE);
        if (old.get(key) != value) updateState(player, old.with(key, value));
    }

    public static ResearchProgressState state(ServerPlayer player) {
        return player.getAttachedOrCreate(STATE);
    }

    static void updateState(ServerPlayer player, ResearchProgressState state) {
        ResearchProgressState before = player.getAttachedOrCreate(STATE);
        if (state.equals(before)) return;
        ResearchProgressState after = state.with("progression.revision",
                Math.max(0, before.get("progression.revision")) + 1);
        player.setAttached(STATE, after);
        ProgressionSyncRuntime.delta(player, before, after);
    }

    private static int value(ServerPlayer player, String key) {
        return player.getAttachedOrCreate(STATE).get(key);
    }

    public static boolean knows(ServerPlayer player, int number) {
        return value(player, "knowledge." + number) != 0;
    }

    public static boolean researched(ServerPlayer player, String id) {
        return value(player, "research." + id) != 0;
    }

    public static void grantKnowledge(ServerPlayer player, int number) {
        if (number < 1 || number > 70 || knows(player, number)) return;
        boolean first = player.getAttachedOrCreate(STATE).counters().keySet().stream()
                .noneMatch(key -> key.startsWith("knowledge.") && value(player, key) != 0);
        set(player, "knowledge." + number, 1);
        if (SILENT_RECONCILIATION.contains(player.getUUID())) return;
        var entry = KnowledgeCatalog.entries().stream().filter(e -> e.number() == number).findFirst().orElse(null);
        if (entry == null) return;
        var title = net.minecraft.network.chat.Component.translatable(entry.titleKey()).withStyle(net.minecraft.ChatFormatting.GREEN);
        var message = net.minecraft.network.chat.Component.translatable("moreminecrafts.chat.knowledge_unlocked",
                player.getDisplayName(), title);
        if (player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SHOW_ADVANCEMENT_MESSAGES))
            player.level().getServer().getPlayerList().broadcastSystemMessage(message, false);
        if (first) player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "moreminecrafts.chat.first_knowledge_guidance", net.minecraft.network.chat.Component.keybind("key.advancements")));
    }

    /** Event adapters report durable, server-observed condition progress through this API. */
    public static void recordCondition(ServerPlayer player, String conditionKey, int totalProgress) {
        if (conditionKey == null || totalProgress <= 0) return;
        List<KnowledgeCatalog.Entry> matching = KnowledgeCatalog.entries().stream()
                .filter(entry -> entry.conditionKey().equals(conditionKey)).toList();
        if (matching.isEmpty()) return;
        int maximumTarget = matching.stream().mapToInt(KnowledgeCatalog.Entry::target).max().orElse(0);
        String key = "condition." + conditionKey;
        int next = KnowledgeConditionProgress.advance(value(player, key), totalProgress, maximumTarget);
        if (next != value(player, key)) set(player, key, next);
        for (KnowledgeCatalog.Entry entry : matching) {
            if (KnowledgeRules.conditionMet(next, entry.target())) grantKnowledge(player, entry.number());
        }
    }

    /** Opens a synchronous per-break frame so nested Vein Mining calls retain their own baseline. */
    public static PickaxeDamageSnapshotTracker.Scope beginPickaxeDamageTracking(ServerPlayer player) {
        ItemStack tool = player.getMainHandItem();
        Item item = tool.is(ItemTags.PICKAXES) && tool.getMaxDamage() > 0 ? tool.getItem() : null;
        return PICKAXE_DAMAGE.begin(player.getUUID(), item, tool.getDamageValue(), tool.getMaxDamage());
    }

    /** Completes one frame only after the enclosing server block-break call actually removed its block. */
    public static void finishPickaxeDamageTracking(ServerPlayer player,
            PickaxeDamageSnapshotTracker.Scope scope, boolean successfulRemoval) {
        PickaxeDamageSnapshotTracker.Snapshot before = scope.complete();
        if (!successfulRemoval || before == null) return;
        ItemStack after = player.getMainHandItem();
        int spent = 0;
        if (after.getItem() == before.item()) {
            spent = Math.max(0, after.getDamageValue() - before.damage());
        } else if (after.isEmpty()) {
            spent = before.maximumDamage() - before.damage() + 1;
        }
        if (spent > 0) recordCondition(player, "pickaxe_durability_used",
                value(player, "condition.pickaxe_durability_used") + spent);
    }

    /** Called only after the server has removed one marked natural vanilla log. */
    public static void recordK5NaturalLogMined(ServerPlayer player) {
        if (player.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL) return;
        recordCondition(player, "natural_vanilla_logs_mined",
                value(player, "condition.natural_vanilla_logs_mined") + 1);
    }

    /** Record one unique discovered ID (species, biome, structure, item type, etc.) exactly once. */
    public static void recordDistinctCondition(ServerPlayer player, String conditionKey, String discoveredId) {
        if (conditionKey == null || discoveredId == null || discoveredId.isBlank()) return;
        String key = KnowledgeConditionSemantics.distinctObservationKey(conditionKey, discoveredId);
        if (KnowledgeConditionSemantics.alreadyObserved(
                player.getAttachedOrCreate(STATE).counters(), conditionKey, discoveredId)) return;
        set(player, key, 1);
        int total = (int) player.getAttachedOrCreate(STATE).counters().keySet().stream()
                .filter(counter -> counter.startsWith("condition." + conditionKey + ".seen.")).count();
        recordCondition(player, conditionKey, total);
    }

    /** Knowledge-gated recipes are never allowed from recipe-book appearance alone. */
    public static boolean canCraft(ServerPlayer player, RecipeHolder<?> recipe) {
        return ProgressionRecipeLocks.allowed(player, recipe);
    }

    private static int crafted(ServerPlayer player, Item item) {
        return player.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
    }

    private static int acquired(ServerPlayer player, Item item) {
        return player.getStats().getValue(Stats.ITEM_PICKED_UP.get(item))
                + player.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
    }

    private static int countCrafted(ServerPlayer player, List<Item> items) {
        int count = 0;
        for (Item item : items) if (crafted(player, item) > 0) count++;
        return count;
    }

    private static int distinctCount(ServerPlayer player, String condition) {
        String prefix = "condition." + condition + ".seen.";
        return (int) player.getAttachedOrCreate(STATE).counters().keySet().stream()
                .filter(key -> key.startsWith(prefix)).count();
    }

    private static void observeTypes(ServerPlayer player, String condition, List<Item> items) {
        for (Item item : items) if (acquired(player, item) > 0)
            recordDistinctCondition(player, condition, String.valueOf(BuiltInRegistries.ITEM.getKey(item)));
    }

    private static void observeEffectTypes(ServerPlayer player) {
        for (var effect : player.getActiveEffects()) {
            var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            if (id != null) recordDistinctCondition(player, "effect_types_experienced", id.toString());
        }
    }

    private static void observeExploration(ServerPlayer p) {
        var biome = p.level().getBiome(p.blockPosition()).unwrapKey();
        biome.ifPresent(key -> {
            String biomeId = key.identifier().toString();
            recordDistinctCondition(p, "biomes_visited", biomeId);
            if (biomeId.endsWith(":deep_dark") || biomeId.endsWith(":lush_caves")
                    || biomeId.endsWith(":dripstone_caves"))
                recordDistinctCondition(p, "cave_biomes_visited", biomeId);
        });
        if (p.level().dimension().identifier().equals(net.minecraft.world.level.Level.NETHER.identifier()))
            recordDistinctCondition(p, "nether_and_end_visited", "minecraft:the_nether");
        if (p.level().dimension().identifier().equals(net.minecraft.world.level.Level.END.identifier()))
            recordDistinctCondition(p, "nether_and_end_visited", "minecraft:the_end");
        for (var structure : p.level().structureManager().getAllStructuresAt(p.blockPosition()).keySet()) {
            var id = p.level().registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
            if (id != null) recordDistinctCondition(p, "structures_visited", id.toString());
        }
        long structures = p.getAttachedOrCreate(STATE).counters().keySet().stream()
                .filter(key -> key.startsWith("condition.structures_visited.seen.")).count();
        if (structures >= 5) grantKnowledge(p, 60);
        if (structures >= 10) grantKnowledge(p, 62);
    }

    /** Only the 13 recipe-bearing knowledge entries are sampled here, not a fake full 70-entry unlock. */
    private static void refresh(ServerPlayer p) {
        observeTypes(p, "log_types_acquired", logItems);
        observeTypes(p, "plant_types_acquired", plantItems);
        observeTypes(p, "seed_types_acquired", seedItems);
        observeTypes(p, "rare_ores_acquired", List.of(Items.DIAMOND, Items.EMERALD, Items.ANCIENT_DEBRIS));
        observeTypes(p, "soil_types_acquired", List.of(Items.DIRT, Items.COARSE_DIRT, Items.PODZOL, Items.MUD));
        observeTypes(p, "brewing_ingredients_acquired", List.of(Items.NETHER_WART, Items.BLAZE_POWDER,
                Items.REDSTONE, Items.GLOWSTONE_DUST, Items.GHAST_TEAR, Items.MAGMA_CREAM,
                Items.SPIDER_EYE, Items.PUFFERFISH, Items.GOLDEN_CARROT, Items.RABBIT_FOOT));
        // K5 advances only from successful direct mining of persisted natural tree markers.
        recordCondition(p, "chests_crafted", crafted(p, Items.CHEST));
        for (Item food : foodItems) {
            if (crafted(p, food) > 0)
                recordDistinctCondition(p, "foods_crafted_or_cooked",
                        String.valueOf(BuiltInRegistries.ITEM.getKey(food)));
        }
        recordCondition(p, "netherite_ingot_crafted", crafted(p, Items.NETHERITE_INGOT));
        recordCondition(p, "farm_automation_parts_crafted",
                countCrafted(p, List.of(Items.OBSERVER, Items.PISTON, Items.HOPPER)));
        recordCondition(p, "villager_trades", p.getStats().getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER)));
        recordCondition(p, "animal_breedings", p.getStats().getValue(Stats.CUSTOM.get(Stats.ANIMALS_BRED)));
        boolean fullArmor = !p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()
                && !p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()
                && !p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).isEmpty()
                && !p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).isEmpty();
        recordCondition(p, "full_armor_worn", fullArmor ? 1 : 0);
        long kinds = p.getAttachedOrCreate(STATE).counters().keySet().stream()
                .filter(key -> key.startsWith("log.")).count();
        if (kinds >= 6) grantKnowledge(p, 4); // migrate legacy per-log observations
        if (!knows(p, 17) && crafted(p, Items.CHEST) >= 10) grantKnowledge(p, 17);
        if (!knows(p, 57) && p.getStats().getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM)) >= 200000)
            grantKnowledge(p, 57);
        recordCondition(p, "boat_distance", p.getStats().getValue(Stats.CUSTOM.get(Stats.BOAT_ONE_CM)) / 100);
        recordCondition(p, "horse_distance", p.getStats().getValue(Stats.CUSTOM.get(Stats.HORSE_ONE_CM)) / 100);
        recordCondition(p, "elytra_distance", p.getStats().getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM)) / 100);
    }
}
