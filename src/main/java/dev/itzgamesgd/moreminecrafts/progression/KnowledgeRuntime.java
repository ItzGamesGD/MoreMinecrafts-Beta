package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
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

    public static void initialize() {
        // Record five distinct actually feedable species across normal right-clicks.
        // An observer callback never consumes or cancels the original interaction.
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register(
                (player, level, hand, entity, hitResult) -> {
                    if (player instanceof ServerPlayer serverPlayer
                            && entity instanceof net.minecraft.world.entity.animal.Animal animal
                            && animal.isFood(player.getItemInHand(hand))
                            && (animal.canFallInLove() || animal.isBaby())
                            && !knows(serverPlayer, 25)) {
                        String type = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
                        set(serverPlayer, "fed_species." + type, 1);
                        long count = serverPlayer.getAttachedOrCreate(STATE).counters().keySet().stream()
                                .filter(s -> s.startsWith("fed_species.")).count();
                        set(serverPlayer, "fed_species", (int) count);
                    }
                    return net.minecraft.world.InteractionResult.PASS;
                });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) refresh(player);
        });
    }

    private static void set(ServerPlayer player, String key, int value) {
        ResearchProgressState old = player.getAttachedOrCreate(STATE);
        if (old.get(key) != value) player.setAttached(STATE, old.with(key, value));
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
        if (number >= 1 && number <= 70) set(player, "knowledge." + number, 1);
    }

    /** Used by the future research purchase/menu server-side path. */
    public static void recordResearchPurchase(ServerPlayer player, String id) {
        set(player, "research." + id, 1);
    }

    /** Knowledge-gated recipes are never allowed from recipe-book appearance alone. */
    public static boolean canCraft(ServerPlayer player, RecipeHolder<?> recipe) {
        if (recipe == null) return true;
        var id = recipe.id().identifier();
        if (!id.getNamespace().equals("moreminecrafts") || !id.getPath().startsWith("knowledge/knowledge"))
            return true;
        String path = id.getPath().substring("knowledge/knowledge".length());
        if (path.length() < 2 || !Character.isDigit(path.charAt(0)) || !Character.isDigit(path.charAt(1)))
            return false;
        return knows(player, Integer.parseInt(path.substring(0, 2)));
    }

    private static int crafted(ServerPlayer player, Item item) {
        return player.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
    }

    private static int used(ServerPlayer player, Item item) {
        return player.getStats().getValue(Stats.ITEM_USED.get(item));
    }

    private static int craftedSum(ServerPlayer player, List<Item> items) {
        int count = 0;
        for (Item item : items) count += crafted(player, item);
        return count;
    }

    /** Only the 13 recipe-bearing knowledge entries are sampled here, not a fake full 70-entry unlock. */
    private static void refresh(ServerPlayer p) {
        if (!knows(p, 4)) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack stack = p.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(ItemTags.LOGS)) {
                    set(p, "log." + BuiltInRegistries.ITEM.getKey(stack.getItem()), 1);
                }
            }
            long kinds = p.getAttachedOrCreate(STATE).counters().keySet().stream()
                    .filter(key -> key.startsWith("log.")).count();
            if (kinds >= 6) grantKnowledge(p, 4);
        }
        if (!knows(p, 14) && craftedSum(p, List.of(
                Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE,
                Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE,
                Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.DIAMOND_SHOVEL,
                Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD)) >= 5)
            grantKnowledge(p, 14);
        if (!knows(p, 15)) {
            int total = 0;
            for (Item item : BuiltInRegistries.ITEM) total += crafted(p, item);
            if (total >= 50) grantKnowledge(p, 15);
        }
        if (!knows(p, 17) && crafted(p, Items.CHEST) >= 10) grantKnowledge(p, 17);
        if (!knows(p, 18) && used(p, Items.HOPPER) >= 5) grantKnowledge(p, 18);
        if (!knows(p, 19) && (used(p, Items.REDSTONE) + used(p, Items.REPEATER)
                + used(p, Items.COMPARATOR) + used(p, Items.OBSERVER)
                + used(p, Items.PISTON) + used(p, Items.STICKY_PISTON)) >= 20) grantKnowledge(p, 19);
        if (!knows(p, 22) && (used(p, Items.WOODEN_HOE) + used(p, Items.STONE_HOE)
                + used(p, Items.IRON_HOE) + used(p, Items.GOLDEN_HOE)
                + used(p, Items.DIAMOND_HOE) + used(p, Items.NETHERITE_HOE)) >= 100) grantKnowledge(p, 22);
        if (!knows(p, 25) && value(p, "fed_species") >= 5) grantKnowledge(p, 25);
        if (!knows(p, 30) && craftedSum(p, List.of(
                Items.BREAD, Items.COOKIE, Items.CAKE, Items.PUMPKIN_PIE,
                Items.MUSHROOM_STEW, Items.RABBIT_STEW, Items.BEETROOT_SOUP,
                Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN,
                Items.COOKED_MUTTON, Items.COOKED_RABBIT, Items.COOKED_COD,
                Items.COOKED_SALMON, Items.BAKED_POTATO)) >= 10) grantKnowledge(p, 30);
        var skeleton = BuiltInRegistries.ENTITY_TYPE.getValue(ModIds.id("skeleton"));
        var spider = BuiltInRegistries.ENTITY_TYPE.getValue(ModIds.id("spider"));
        if (!knows(p, 42) && skeleton != null
                && p.getStats().getValue(Stats.ENTITY_KILLED.get(skeleton)) >= 30) grantKnowledge(p, 42);
        if (!knows(p, 44) && spider != null
                && p.getStats().getValue(Stats.ENTITY_KILLED.get(spider)) >= 30) grantKnowledge(p, 44);
        if (!knows(p, 57) && p.getStats().getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM)) >= 200000)
            grantKnowledge(p, 57);
        if (!knows(p, 60)) {
            for (var structure : p.level().structureManager().getAllStructuresAt(p.blockPosition()).keySet()) {
                var id = p.level().registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
                if (id != null) set(p, "visited_structure." + id, 1);
            }
            long unique = p.getAttachedOrCreate(STATE).counters().keySet().stream()
                    .filter(key -> key.startsWith("visited_structure.")).count();
            if (unique >= 5) grantKnowledge(p, 60);
        }
    }
}
