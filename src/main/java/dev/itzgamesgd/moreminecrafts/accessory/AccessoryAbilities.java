package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Automatic accessory abilities. Cooldowns live on the player; short-lived trigger state does not. */
public final class AccessoryAbilities implements ModInitializer {
    private static final Map<UUID, EmberProtection> EMBER = new HashMap<>();
    private static final Map<UUID, Stillness> PEARL = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(AccessoryAbilities::tick));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            EMBER.remove(handler.player.getUUID());
            PEARL.remove(handler.player.getUUID());
        });
    }

    public static ItemStack equipped(ServerPlayer player, AccessoryDefinition ability) {
        int slot = ability.category().ordinal();
        ItemStack stack = new AccessoryEquipmentContainer(player).getItem(slot);
        return AccessorySlotRules.accepts(slot, stack)
                && stack.getItem() == ModItems.ACCESSORIES.get(ability) ? stack : ItemStack.EMPTY;
    }

    /** An equipment mutation ends active protection and resets unfinished stillness immediately. */
    public static void clearTransientState(ServerPlayer player, ItemStack previous) {
        if (previous.getItem() == ModItems.ACCESSORIES.get(AccessoryDefinition.EMBER_RING))
            EMBER.remove(player.getUUID());
        if (previous.getItem() == ModItems.ACCESSORIES.get(AccessoryDefinition.DEEP_SEA_PEARL))
            PEARL.remove(player.getUUID());
    }

    private static double stat(AccessoryDefinition ability, String name, ItemStack stack) {
        return AccessoryEnhancementCatalog.valueAt(ability, name,
                stack.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL, 0)).orElse(0);
    }

    public static void tick(ServerPlayer player) {
        AccessoryCooldownRuntime.tick(player);
        UUID id = player.getUUID();
        EmberProtection protection = EMBER.get(id);
        if (protection != null && (!player.isAlive() || equipped(player, AccessoryDefinition.EMBER_RING).isEmpty()
                || protection.remainingTicks() <= 1)) EMBER.remove(id);
        else if (protection != null) EMBER.put(id, new EmberProtection(protection.remainingTicks() - 1, protection.reduction()));

        ItemStack life = equipped(player, AccessoryDefinition.RING_OF_LIFE);
        if (player.isAlive() && !life.isEmpty() && player.getHealth() <= player.getMaxHealth() * 0.25F
                && AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.RING_OF_LIFE) == 0) {
            int duration = (int) Math.round(stat(AccessoryDefinition.RING_OF_LIFE,
                    "regeneration_i_duration_seconds", life) * 20);
            int cooldown = (int) Math.round(stat(AccessoryDefinition.RING_OF_LIFE,
                    "cooldown_seconds", life) * 20);
            if (duration > 0 && cooldown > 0 && AccessoryCooldownRuntime.start(player, AccessoryDefinition.RING_OF_LIFE, cooldown))
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
        }

        ItemStack pearl = equipped(player, AccessoryDefinition.DEEP_SEA_PEARL);
        boolean underwater = player.level().getFluidState(BlockPos.containing(player.getEyePosition())).is(FluidTags.WATER);
        if (!player.isAlive() || pearl.isEmpty() || !underwater || !player.isShiftKeyDown()) {
            PEARL.remove(id);
            return;
        }
        Vec3 position = player.position();
        Stillness previous = PEARL.get(id);
        int ticks = previous != null && previous.position().equals(position) ? previous.ticks() + 1 : 1;
        PEARL.put(id, new Stillness(position, ticks));
        if (ticks >= 40 && AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.DEEP_SEA_PEARL) == 0) {
            int duration = (int) Math.round(stat(AccessoryDefinition.DEEP_SEA_PEARL, "water_breathing_seconds", pearl) * 20);
            int cooldown = (int) Math.round(stat(AccessoryDefinition.DEEP_SEA_PEARL, "cooldown_seconds", pearl) * 20);
            if (duration > 0 && cooldown > 0 && AccessoryCooldownRuntime.start(player, AccessoryDefinition.DEEP_SEA_PEARL, cooldown))
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0));
        }
    }

    /** Called only after vanilla accepts a fire-damage event; the triggering hit is unchanged. */
    public static void afterDamage(ServerPlayer player, DamageSource source) {
        if (!player.isAlive() || !source.is(DamageTypeTags.IS_FIRE)) return;
        ItemStack ember = equipped(player, AccessoryDefinition.EMBER_RING);
        if (ember.isEmpty() || AccessoryCooldownRuntime.remaining(player, AccessoryDefinition.EMBER_RING) > 0) return;
        int duration = (int) Math.round(stat(AccessoryDefinition.EMBER_RING, "protection_duration_seconds", ember) * 20);
        double reduction = stat(AccessoryDefinition.EMBER_RING, "post_fire_hit_damage_reduction_percent", ember) / 100.0D;
        if (duration > 0 && AccessoryCooldownRuntime.start(player, AccessoryDefinition.EMBER_RING, 600))
            EMBER.put(player.getUUID(), new EmberProtection(duration, reduction));
    }

    public static float reduceFireDamage(ServerPlayer player, DamageSource source, float amount) {
        EmberProtection protection = EMBER.get(player.getUUID());
        return source.is(DamageTypeTags.IS_FIRE) && protection != null && player.isAlive()
                && !equipped(player, AccessoryDefinition.EMBER_RING).isEmpty()
                ? (float) (amount * (1.0D - protection.reduction())) : amount;
    }

    private record EmberProtection(int remainingTicks, double reduction) {}
    private record Stillness(Vec3 position, int ticks) {}
}
