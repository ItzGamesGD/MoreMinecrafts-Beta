package dev.itzgamesgd.moreminecrafts.ability;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class FoundationProbeAbility implements ActiveEquipmentAbility {
    public static final FoundationProbeAbility INSTANCE = new FoundationProbeAbility();
    public static final Identifier ID = ModIds.id("foundation_probe_active");
    public static final int COOLDOWN_TICKS = 40;

    private FoundationProbeAbility() {
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack source) {
        player.displayClientMessage(Component.literal("Foundation ability activated"), true);
        return true;
    }
}
