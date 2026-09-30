package dev.itzgamesgd.moreminecrafts.enchantment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;

/** Queues the two follow-up shots of a barrage without changing ordinary charge speed. */
public final class CrossbowBarrageRuntime {
    private record Shot(long tick, ServerPlayer player, InteractionHand hand, ItemStack crossbow, int remaining) {}
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static boolean followUp;
    private CrossbowBarrageRuntime() {}

    public static void afterShot(net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity shooter,
            InteractionHand hand, ItemStack crossbow) {
        if (followUp || !(shooter instanceof ServerPlayer player) || level.isClientSide()) return;
        if (ModEnchantments.level(level, crossbow, ModEnchantments.CROSSBOW_BARRAGE) > 0)
            SHOTS.add(new Shot(level.getGameTime() + 4, player, hand, crossbow, 2));
    }

    public static void tick(long time) {
        Iterator<Shot> iterator = SHOTS.iterator();
        List<Shot> next = new ArrayList<>();
        while (iterator.hasNext()) {
            Shot shot = iterator.next();
            if (shot.tick > time) continue;
            iterator.remove();
            if (!shot.player.isAlive() || shot.crossbow.isEmpty()) continue;
            ItemStack ammo = shot.player.getProjectile(shot.crossbow);
            if (ammo.isEmpty()) continue;
            ItemStack loaded = ammo.copyWithCount(1);
            if (!shot.player.getAbilities().instabuild) ammo.shrink(1);
            shot.crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(List.of(loaded)));
            followUp = true;
            try {
                ((CrossbowItem) shot.crossbow.getItem()).performShooting(shot.player.level(), shot.player,
                        shot.hand, shot.crossbow, 3.15F, 1.0F, null);
            } finally { followUp = false; }
            if (shot.remaining > 1) next.add(new Shot(time + 4, shot.player, shot.hand, shot.crossbow, shot.remaining - 1));
        }
        SHOTS.addAll(next);
    }
}
