package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior.BoundingBox;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Optional LambDynamicLights 26.3 API integration for the socketed Miner Lantern. */
public final class LanternDynamicLightBridge implements DynamicLightsInitializer {
    private DynamicLightBehaviorManager manager;
    private final Map<UUID, LanternBehavior> active = new HashMap<>();

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        manager = context.dynamicLightBehaviorManager();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (client.level == null) {
            active.values().forEach(manager::remove);
            active.clear();
            return;
        }

        Set<UUID> equippedPlayers = new HashSet<>();
        for (Player player : client.level.players()) {
            if (!player.isAlive() || !player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY)
                    .is(ModItems.MINER_LANTERN)) continue;
            equippedPlayers.add(player.getUUID());
            LanternBehavior existing = active.get(player.getUUID());
            if (existing != null && existing.player() != player) {
                manager.remove(existing);
                active.remove(player.getUUID());
            }
            active.computeIfAbsent(player.getUUID(), ignored -> {
                LanternBehavior behavior = new LanternBehavior(player);
                manager.add(behavior);
                return behavior;
            });
        }

        active.entrySet().removeIf(entry -> {
            if (equippedPlayers.contains(entry.getKey())) return false;
            manager.remove(entry.getValue());
            return true;
        });
    }

    private static final class LanternBehavior implements DynamicLightBehavior {
        private final Player player;
        private double lastX = Double.NaN, lastY = Double.NaN, lastZ = Double.NaN;

        LanternBehavior(Player player) { this.player = player; }
        Player player() { return player; }

        @Override public double lightAtPos(BlockPos pos, double falloffRatio) {
            if (!player.isAlive() || !player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY)
                    .is(ModItems.MINER_LANTERN)) return 0.0D;
            double dx = pos.getX() + 0.5D - player.getX();
            double dy = pos.getY() + 0.5D - (player.getY() + player.getBbHeight() * 0.85D);
            double dz = pos.getZ() + 0.5D - player.getZ();
            return Math.max(15.0D - Math.sqrt(dx * dx + dy * dy + dz * dz) * falloffRatio, 0.0D);
        }

        @Override public BoundingBox getBoundingBox() {
            BlockPos pos = BlockPos.containing(player.getX(), player.getY() + player.getBbHeight() * 0.85D, player.getZ());
            return new BoundingBox(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        }

        @Override public boolean hasChanged() {
            double x = player.getX(), y = player.getY(), z = player.getZ();
            boolean changed = x != lastX || y != lastY || z != lastZ;
            lastX = x; lastY = y; lastZ = z;
            return changed;
        }

        @Override public boolean isRemoved() {
            return !player.isAlive() || !player.getAttachedOrElse(EquipmentAttachments.LANTERN, ItemStack.EMPTY)
                    .is(ModItems.MINER_LANTERN);
        }
    }
}
