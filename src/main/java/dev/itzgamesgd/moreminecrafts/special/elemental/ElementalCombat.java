package dev.itzgamesgd.moreminecrafts.special.elemental;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ElementalCombat {
    private ElementalCombat() {}

    public static boolean validTarget(ServerPlayer attacker, LivingEntity target) {
        return target != attacker && target.isAlive() && !target.isAlliedTo(attacker)
                && (!(target instanceof ServerPlayer player) || attacker.canHarmPlayer(player));
    }
}
