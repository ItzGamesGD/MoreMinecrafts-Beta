package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Applies Star Covenant's conditional melee bonus from the authoritative equipped stack. */
public final class StarCovenantAbility implements ModInitializer {
    private static final Identifier BONUS_ID = ModIds.id("star_covenant_melee_bonus");
    private static final int COMBAT_SLOT = AccessoryCategory.COMBAT.ordinal();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(StarCovenantAbility::refresh));
    }

    /** Recomputes the transient modifier; no extra ability data is stored on the player. */
    public static void refresh(ServerPlayer player) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return;
        double bonus = activeBonus(player);
        AttributeModifier current = attack.getModifier(BONUS_ID);
        if (current != null && current.amount() == bonus) return;
        if (current != null) attack.removeModifier(BONUS_ID);
        if (bonus > 0.0D) {
            attack.addTransientModifier(new AttributeModifier(
                    BONUS_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static double activeBonus(ServerPlayer player) {
        if (!player.isAlive()) return 0.0D;
        ItemStack stack = new AccessoryEquipmentContainer(player).getItem(COMBAT_SLOT);
        if (!AccessorySlotRules.accepts(COMBAT_SLOT, stack)
                || stack.getItem() != ModItems.ACCESSORIES.get(AccessoryDefinition.STAR_COVENANT)) return 0.0D;
        int level = stack.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL, 0);
        var threshold = AccessoryEnhancementCatalog.valueAt(
                AccessoryDefinition.STAR_COVENANT, "hp_activation_threshold_percent", level);
        var damage = AccessoryEnhancementCatalog.valueAt(
                AccessoryDefinition.STAR_COVENANT, "melee_damage_bonus_percent", level);
        if (threshold.isEmpty() || damage.isEmpty()
                || player.getHealth() > player.getMaxHealth() * threshold.getAsDouble() / 100.0D) return 0.0D;
        return damage.getAsDouble() / 100.0D;
    }
}
