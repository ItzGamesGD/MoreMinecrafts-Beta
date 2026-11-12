package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

/**
 * Server-authoritative provenance for {@code xpq.cmb.wither_skulls_3}.
 *
 * <p>Evidence lives on the dropped {@link ItemEntity} (a persistent attachment, so it survives chunk
 * unload and server restarts) from the moment a Wither Skeleton's death loot table spawns it until a
 * player first takes it. The flag means "every skull in this entity is an uncredited genuine drop".
 * Because the item entity is the only carrier, an item that leaves it any way other than a player
 * pickup (hopper, mob, container, despawn, drop-and-repick) simply has no provenance afterwards, which
 * fails closed. Genuine and non-genuine skull entities never merge, so a flagged entity never holds a
 * mixed stack and partial pickups or splits keep the flag exactly correct.</p>
 */
public final class QuestWitherSkullProvenance {
    /** Per-player quest counter read by the {@code xpq.cmb.wither_skulls_3} tracker. */
    static final String DROPS = "wither_skull_drops";

    public static final AttachmentType<Boolean> GENUINE_DROP = AttachmentRegistry.create(
            ModIds.id("quest_wither_skull_drop"), builder -> builder.persistent(Codec.BOOL));

    /** The Wither Skeleton currently running its death loot table, on this (server) thread only. */
    private static final ThreadLocal<Entity> DEATH_LOOT_SOURCE = new ThreadLocal<>();

    private QuestWitherSkullProvenance() {}

    public static void initialize() { java.util.Objects.requireNonNull(GENUINE_DROP); }

    /** Brackets {@code LivingEntity.dropFromLootTable(level, source, playerKilled)}; returns the previous source. */
    public static Entity enterDeathLoot(Entity source) {
        Entity previous = DEATH_LOOT_SOURCE.get();
        DEATH_LOOT_SOURCE.set(source);
        return previous;
    }

    public static void exitDeathLoot(Entity previous) {
        if (previous == null) DEATH_LOOT_SOURCE.remove(); else DEATH_LOOT_SOURCE.set(previous);
    }

    /** {@code spawner} spawned {@code item}; marks it only for a skull from a Wither Skeleton's own death loot. */
    public static void spawned(Entity spawner, ItemEntity item) {
        if (item == null || spawner.level().isClientSide()) return;
        if (spawner.getType() != EntityTypes.WITHER_SKELETON || DEATH_LOOT_SOURCE.get() != spawner) return;
        if (item.getItem().is(Items.WITHER_SKELETON_SKULL)) item.setAttached(GENUINE_DROP, Boolean.TRUE);
    }

    public static boolean isGenuine(ItemEntity item) {
        return !item.isRemoved() && item.getItem().is(Items.WITHER_SKELETON_SKULL) && Boolean.TRUE.equals(item.getAttached(GENUINE_DROP));
    }

    /** Entities may merge only when both or neither are genuine, so provenance is never diluted or inflated. */
    public static boolean mayMerge(ItemEntity a, ItemEntity b) { return isGenuine(a) == isGenuine(b); }

    /**
     * {@code transferred} skulls of a genuine drop actually entered {@code player}'s inventory. The flag is
     * consumed when nothing is left, so a re-delivered callback for the same pickup finds no provenance.
     */
    public static void pickedUp(ServerPlayer player, ItemEntity item, int transferred) {
        if (transferred <= 0) return;
        if (item.getItem().isEmpty()) item.removeAttached(GENUINE_DROP);
        XpQuestTracking.addCounter(player, DROPS, transferred);
        XpQuestTracking.reconcile(player);
    }
}
