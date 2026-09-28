package dev.itzgamesgd.moreminecrafts.special;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.itzgamesgd.moreminecrafts.special.elemental.ElementalSwordItem;
import dev.itzgamesgd.moreminecrafts.special.flame.FlameSwordItem;
import dev.itzgamesgd.moreminecrafts.special.wind.WindSwordItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-runtime, player- and weapon-family-owned counters. Only primary hit callbacks record. */
public final class ElementalHitProgress {
    public enum Weapon { FIRE, WIND, LIGHTNING }

    public static final int FIRE_THRESHOLD = 10;
    public static final int WIND_THRESHOLD = 10;
    public static final int LIGHTNING_THRESHOLD = 5;
    public static final float LIGHTNING_PROC_BONUS_DAMAGE = 4.0F;

    private static final Map<UUID, EnumMap<Weapon, SuccessfulHitCounter>> PROGRESS = new HashMap<>();
    private static final Map<UUID, Weapon> HELD_FAMILY = new HashMap<>();

    private ElementalHitProgress() {}

    /** Registers server-side lifetime cleanup and main-hand family-switch detection. */
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(ElementalHitProgress::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(ElementalHitProgress::onServerStopped);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Set<UUID> online = new HashSet<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID playerId = player.getUUID();
                online.add(playerId);
                if (!player.isAlive()) {
                    clear(playerId);
                    continue;
                }
                observeHeld(playerId, heldFamily(player.getMainHandItem()));
            }
            PROGRESS.keySet().removeIf(playerId -> !online.contains(playerId));
            HELD_FAMILY.keySet().removeIf(playerId -> !online.contains(playerId));
        });
    }

    static void onServerStarting(MinecraftServer server) {
        clearRuntimeState();
    }

    static void onServerStopped(MinecraftServer server) {
        clearRuntimeState();
    }

    static void clearRuntimeState() {
        PROGRESS.clear();
        HELD_FAMILY.clear();
    }

    static boolean hasRuntimeState() {
        return !PROGRESS.isEmpty() || !HELD_FAMILY.isEmpty();
    }

    static float primaryHitBonusDamage(UUID playerId, Weapon weapon) {
        if (weapon != Weapon.LIGHTNING) return 0.0F;
        return progress(playerId, weapon) == LIGHTNING_THRESHOLD - 1
                ? LIGHTNING_PROC_BONUS_DAMAGE : 0.0F;
    }

    public static boolean record(UUID playerId, Weapon weapon) {
        int threshold = threshold(weapon);
        EnumMap<Weapon, SuccessfulHitCounter> playerProgress =
                PROGRESS.computeIfAbsent(playerId, ignored -> new EnumMap<>(Weapon.class));
        SuccessfulHitCounter.Advance advance = playerProgress
                .getOrDefault(weapon, SuccessfulHitCounter.EMPTY)
                .recordSuccessfulHit(threshold);
        playerProgress.put(weapon, advance.counter());
        return advance.triggered();
    }

    static void observeHeld(UUID playerId, Weapon currentFamily) {
        if (currentFamily == null) {
            clear(playerId);
            return;
        }
        Weapon previousFamily = HELD_FAMILY.put(playerId, currentFamily);
        if (previousFamily != null && previousFamily != currentFamily) {
            PROGRESS.remove(playerId);
        }
    }

    private static Weapon heldFamily(ItemStack stack) {
        if (stack.getItem() instanceof FlameSwordItem) return Weapon.FIRE;
        if (stack.getItem() instanceof WindSwordItem) return Weapon.WIND;
        if (stack.getItem() == ModItems.LIGHTNING_SWORD
                && stack.getItem() instanceof ElementalSwordItem) return Weapon.LIGHTNING;
        return null;
    }

    static void clear(UUID playerId) {
        PROGRESS.remove(playerId);
        HELD_FAMILY.remove(playerId);
    }

    static int progress(UUID playerId, Weapon weapon) {
        return PROGRESS.getOrDefault(playerId, new EnumMap<>(Weapon.class))
                .getOrDefault(weapon, SuccessfulHitCounter.EMPTY).hitsTowardTrigger();
    }

    static int threshold(Weapon weapon) {
        return switch (weapon) {
            case FIRE -> FIRE_THRESHOLD;
            case WIND -> WIND_THRESHOLD;
            case LIGHTNING -> LIGHTNING_THRESHOLD;
        };
    }
}
