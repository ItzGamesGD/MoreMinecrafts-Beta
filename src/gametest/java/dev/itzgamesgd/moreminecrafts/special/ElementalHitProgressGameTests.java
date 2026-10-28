package dev.itzgamesgd.moreminecrafts.special;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.gametest.framework.GameTestHelper;

/** Regression coverage for immediate, server-authoritative weapon-family counter synchronization. */
public final class ElementalHitProgressGameTests {
    @GameTest
    public void repeatedSameTickFamilySwitchesResetBeforeEachAttack(GameTestHelper ctx) {
        ServerPlayer player = ctx.makeMockServerPlayerInLevel();
        ElementalHitProgress.clear(player.getUUID());
        ElementalHitProgress.observeHeld(player.getUUID(), ElementalHitProgress.Weapon.FIRE);
        for (int i = 0; i < ElementalHitProgress.FIRE_THRESHOLD - 1; i++) {
            ElementalHitProgress.record(player.getUUID(), ElementalHitProgress.Weapon.FIRE);
        }

        attackWith(ctx, player, ModItems.WIND_SWORD, 2);
        ctx.assertTrue(ElementalHitProgress.progress(player.getUUID(), ElementalHitProgress.Weapon.FIRE) == 0
                        && ElementalHitProgress.progress(player.getUUID(), ElementalHitProgress.Weapon.WIND) == 1,
                "Fire progress is cleared before the first same-tick switch attack");
        attackWith(ctx, player, ModItems.FLAME_SWORD, 3);
        ctx.assertTrue(ElementalHitProgress.progress(player.getUUID(), ElementalHitProgress.Weapon.FIRE) == 1
                        && ElementalHitProgress.progress(player.getUUID(), ElementalHitProgress.Weapon.WIND) == 0,
                "Wind progress is cleared before switching back in the same tick");
        attackWith(ctx, player, ModItems.WIND_SWORD, 4);
        ctx.assertTrue(ElementalHitProgress.progress(player.getUUID(), ElementalHitProgress.Weapon.FIRE) == 0
                        && ElementalHitProgress.progress(player.getUUID(), ElementalHitProgress.Weapon.WIND) == 1,
                "Repeated same-tick switching never carries a previous family's counter");
        ctx.succeed();
    }

    private static void attackWith(GameTestHelper ctx, ServerPlayer player, net.minecraft.world.item.Item item, int x) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        var target = ctx.spawnMob(EntityTypes.HUSK, x, 2, 2).withNoFreeWill().spawn();
        target.setNoAi(true);
        AttackEntityCallback.EVENT.invoker().interact(player, ctx.getLevel(), InteractionHand.MAIN_HAND, target, null);
        float before = target.getHealth();
        player.attack(target);
        ctx.assertTrue(target.getHealth() < before, "the family-switch regression attack must hit successfully");
    }
}
