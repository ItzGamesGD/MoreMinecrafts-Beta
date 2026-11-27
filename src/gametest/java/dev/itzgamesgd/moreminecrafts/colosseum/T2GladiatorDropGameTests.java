package dev.itzgamesgd.moreminecrafts.colosseum;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/** 2026-11-13 T2 record U-08: the Immortal Gladiator drops exactly one Gladiator's Bloodied Crest (100%) and nothing else. */
public final class T2GladiatorDropGameTests {
    @GameTest(maxTicks = 60)
    public void immortalGladiatorDropsExactlyOneBloodiedCrestOnKill(GameTestHelper ctx) {
        var gladiator = ctx.spawn(GladiatorEntities.TYPE, new BlockPos(1, 2, 1));
        gladiator.kill(ctx.getLevel());
        ctx.runAfterDelay(2, () -> {
            int crests = 0, others = 0;
            for (ItemEntity e : ctx.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(ctx.absolutePos(new BlockPos(1, 2, 1))).inflate(6))) {
                if (e.getItem().is(ModItems.GLADIATOR_BLOOD_CREST)) crests += e.getItem().getCount();
                else if (!BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getPath().endsWith("_relic_core")) others++;
            }
            ctx.assertTrue(crests == 1, "exactly one crest, found " + crests);
            ctx.assertTrue(others == 0, "sword/shield equipment and every other drop (besides the separately approved boss relic core) stay suppressed, found " + others);
            ctx.succeed();
        });
    }
}
