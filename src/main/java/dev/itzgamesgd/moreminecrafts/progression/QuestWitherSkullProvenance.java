package dev.itzgamesgd.moreminecrafts.progression;

import com.mojang.serialization.Codec;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

/**
 * Server-authoritative provenance for {@code xpq.cmb.wither_skulls_3}.
 *
 * <p>Evidence lives on the dropped {@link ItemEntity} via a persistent random origin token
 * attached at death-loot drop. The token must be unconsumed and unblocked in the world ledger
 * to earn quest credit.
 *
 * <p>Cancels merging for genuine skull entities (genuine with genuine, and genuine with ordinary)
 * so each genuine entity carries exactly one skull and one origin token.
 */
public final class QuestWitherSkullProvenance {
    /** Per-player quest counter read by the {@code xpq.cmb.wither_skulls_3} tracker. */
    static final String DROPS = "wither_skull_drops";

    public static final AttachmentType<String> ORIGIN_TOKEN = AttachmentRegistry.create(
            ModIds.id("quest_wither_skull_token"), builder -> builder.persistent(Codec.STRING));

    /** The Wither Skeleton currently running its death loot table, on this (server) thread only. */
    private static final ThreadLocal<Entity> DEATH_LOOT_SOURCE = new ThreadLocal<>();

    private QuestWitherSkullProvenance() {}

    public static void initialize() {
        java.util.Objects.requireNonNull(ORIGIN_TOKEN);
        QuestWitherSkullAttachments.initialize();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                QuestWitherSkullTransactions.recover(handler.getPlayer()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTickCount();
            if (tick % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (QuestWitherSkullTransactions.needsRecovery(player)) {
                    QuestWitherSkullTransactions.recover(player);
                }
            }
        });
    }

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
        if (item.getItem().is(Items.WITHER_SKELETON_SKULL)) {
            String token = UUID.randomUUID().toString();
            item.setAttached(ORIGIN_TOKEN, token);
        }
    }

    public static String getToken(ItemEntity item) {
        if (item == null || item.isRemoved() || !item.getItem().is(Items.WITHER_SKELETON_SKULL)) return null;
        return item.getAttached(ORIGIN_TOKEN);
    }

    public static boolean isGenuine(ItemEntity item) {
        String token = getToken(item);
        if (token == null) return false;
        if (item.level() instanceof ServerLevel level) {
            return !QuestWitherSkullLedger.of(level).blocked(token);
        }
        return true;
    }

    /**
     * Wither skull entities with tokens never merge (genuine with genuine, or genuine with ordinary).
     * This ensures each genuine entity holds exactly one skull and one token.
     */
    public static boolean mayMerge(ItemEntity a, ItemEntity b) {
        return getToken(a) == null && getToken(b) == null;
    }

    public static void consumeToken(ItemEntity item) {
        item.removeAttached(ORIGIN_TOKEN);
    }
}
