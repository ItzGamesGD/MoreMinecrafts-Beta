package dev.itzgamesgd.moreminecrafts.accessory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;

/** Technical mod-mob criterion: vanilla Enemy or NeutralMob contracts, never passive animals. */
public final class T1CombatTargets {
    private T1CombatTargets() {}
    public static boolean eligible(ServerPlayer player, LivingEntity target, boolean shockwave) {
        if (target == player || !target.isAlive()) return false;
        if (target instanceof ServerPlayer other) {
            return !other.isSpectator() && player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP)
                    && (shockwave || !player.isAlliedTo(other));
        }
        return (target instanceof Enemy || target instanceof NeutralMob)
                && !target.isAlliedTo(player) && !player.isAlliedTo(target);
    }
}
