package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnhancementOperationTest {
    @BeforeAll
    static void bootstrapMinecraftAndMod() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ModDataComponents.initialize();
        ModItems.initialize();
    }

    @Test
    void advancesSequentiallyAndPreservesStackState() {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(7);
        var first = EnhancementOperation.preview(sword, stone()).orElseThrow();
        assertEquals(1, first.targetLevel());
        assertEquals(1, first.experienceLevelCost());
        assertEquals(7, first.stack().getDamageValue());
        assertEquals(0, EnhancementOperation.level(sword));

        var second = EnhancementOperation.preview(first.stack(), stone()).orElseThrow();
        assertEquals(2, second.targetLevel());
        assertEquals(2, EnhancementOperation.level(second.stack()));
        assertEquals(0.16D, EnhancementOperation.attackDamageBonus(second.stack()), 0.000001D);
    }

    @Test
    void vanillaStopsAtThirty() {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(ModDataComponents.ENHANCEMENT_LEVEL, 29);
        assertEquals(30, EnhancementOperation.preview(sword, stone()).orElseThrow().targetLevel());
        sword.set(ModDataComponents.ENHANCEMENT_LEVEL, 30);
        assertTrue(EnhancementOperation.preview(sword, stone()).isEmpty());
    }

    @Test
    void unsupportedAndShieldAreRejected() {
        assertTrue(EnhancementOperation.preview(new ItemStack(Items.DIRT), stone()).isEmpty());
        assertTrue(EnhancementOperation.preview(new ItemStack(Items.SHIELD), stone()).isEmpty());
    }

    @Test
    void materialMustBeOneEnhancementStone() {
        assertTrue(EnhancementOperation.preview(new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY).isEmpty());
        assertEquals(1, EnhancementOperation.preview(new ItemStack(Items.IRON_SWORD), stone())
                .orElseThrow().materialCost());
    }

    private static ItemStack stone() {
        return new ItemStack(dev.itzgamesgd.moreminecrafts.registry.ModItems.ENHANCEMENT_STONE);
    }
}
