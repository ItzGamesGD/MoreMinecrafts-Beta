package dev.itzgamesgd.moreminecrafts.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Small identity-based binding table for the concrete special items implemented by the mod. */
public final class EquipmentAbilityBindings {
    private static final InputBindingTable<Item, ActiveEquipmentAbility> BINDINGS = new InputBindingTable<>();

    private EquipmentAbilityBindings() {
    }

    public static void bind(Item item, EquipmentSkillInput input, ActiveEquipmentAbility ability) {
        BINDINGS.bind(item, input, ability);
    }

    public static ActiveEquipmentAbility resolve(ItemStack stack, EquipmentSkillInput input) {
        if (stack.isEmpty() || input == null) {
            return null;
        }
        return BINDINGS.resolve(stack.getItem(), input);
    }

    /** Resolves both item and ability from server-owned state; packet data supplies only the input. */
    public static AbilityActivation.Result activate(ServerPlayer player, EquipmentSkillInput input) {
        if (player == null || !player.isAlive()) {
            return AbilityActivation.Result.INELIGIBLE;
        }
        ItemStack held = player.getMainHandItem();
        ActiveEquipmentAbility ability = resolve(held, input);
        return ability == null ? AbilityActivation.Result.INELIGIBLE
                : AbilityActivation.tryActivate(player, held, ability);
    }
}
