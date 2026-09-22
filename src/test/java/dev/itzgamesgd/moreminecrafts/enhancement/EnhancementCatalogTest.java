package dev.itzgamesgd.moreminecrafts.enhancement;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnhancementCatalogTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void resolvesEverySupportedFamily() {
        assertProfile(EnhancementProfile.SWORD, Items.WOODEN_SWORD, Items.IRON_SWORD, Items.NETHERITE_SWORD);
        assertProfile(EnhancementProfile.MELEE, Items.MACE);
        assertProfile(EnhancementProfile.RANGED, Items.BOW, Items.CROSSBOW);
        assertProfile(EnhancementProfile.SPEAR, Items.TRIDENT);
        assertProfile(EnhancementProfile.ARMOR, Items.LEATHER_BOOTS, Items.CHAINMAIL_CHESTPLATE,
                Items.IRON_HELMET, Items.GOLDEN_LEGGINGS, Items.DIAMOND_CHESTPLATE,
                Items.NETHERITE_BOOTS, Items.TURTLE_HELMET);
        assertProfile(EnhancementProfile.TOOL, Items.WOODEN_PICKAXE, Items.STONE_SHOVEL,
                Items.IRON_HOE, Items.GOLDEN_PICKAXE, Items.DIAMOND_SHOVEL, Items.NETHERITE_HOE);
        assertProfile(EnhancementProfile.AXE, Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
                Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
        assertProfile(EnhancementProfile.ELYTRA, Items.ELYTRA);
    }

    @Test
    void ironSwordRetainsTheGeneralizedStage3Contract() {
        var definition = EnhancementCatalog.definition(Items.IRON_SWORD).orElseThrow();
        assertEquals(EnhancementCategory.VANILLA, definition.category());
        assertEquals(EnhancementProfile.SWORD, definition.profile());
        assertEquals(0.08D,
                definition.profile().valuePerLevel(EnhancementEffect.Target.ATTACK_DAMAGE),
                0.000001D);
    }

    @Test
    void rejectsShieldAndUnsupportedItems() {
        assertTrue(EnhancementCatalog.definition(Items.SHIELD).isEmpty());
        assertTrue(EnhancementCatalog.definition(Items.DIRT).isEmpty());
    }

    @Test
    void profilesContainPaperValues() {
        assertEquals(0.08D, EnhancementProfile.SWORD.valuePerLevel(EnhancementEffect.Target.ATTACK_DAMAGE));
        assertEquals(0.10D, EnhancementProfile.MELEE.valuePerLevel(EnhancementEffect.Target.ATTACK_DAMAGE));
        assertEquals(0.08D, EnhancementProfile.RANGED.valuePerLevel(EnhancementEffect.Target.RANGED_DAMAGE));
        assertEquals(0.09D, EnhancementProfile.SPEAR.valuePerLevel(EnhancementEffect.Target.ATTACK_DAMAGE));
        assertEquals(0.001D, EnhancementProfile.ARMOR.valuePerLevel(EnhancementEffect.Target.DAMAGE_REDUCTION));
        assertEquals(0.65D, EnhancementProfile.TOOL.valuePerLevel(EnhancementEffect.Target.TOOL_EFFICIENCY));
        assertEquals(0.65D, EnhancementProfile.AXE.valuePerLevel(EnhancementEffect.Target.TOOL_EFFICIENCY));
        assertEquals(0.10D, EnhancementProfile.AXE.valuePerLevel(EnhancementEffect.Target.ATTACK_DAMAGE));
        assertEquals(0.0D, EnhancementProfile.ELYTRA.valuePerLevel(EnhancementEffect.Target.NONE));
    }

    private static void assertProfile(EnhancementProfile expected, Item... items) {
        for (Item item : items) {
            var definition = EnhancementCatalog.definition(item).orElseThrow();
            assertEquals(EnhancementCategory.VANILLA, definition.category());
            assertEquals(expected, definition.profile());
        }
    }
}
