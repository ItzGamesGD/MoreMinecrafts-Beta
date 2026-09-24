package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
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
    void vanillaItemsRetainExplicitVanillaDefinitions() {
        var sword = EnhancementCatalog.definition(Items.IRON_SWORD).orElseThrow();
        assertEquals(EnhancementCategory.VANILLA, sword.category());
        assertEquals(EnhancementProfile.SWORD, sword.profile());
        assertEquals(EnhancementProfile.MACE, EnhancementCatalog.definition(Items.MACE).orElseThrow().profile());
    }

    @Test
    void elementalSwordsResolveByRegistryIdWithoutInitializingModItems() {
        var flame = EnhancementCatalog.customDefinition(ModIds.id("flame_sword")).orElseThrow();
        var wind = EnhancementCatalog.customDefinition(ModIds.id("wind_sword")).orElseThrow();

        assertEquals(EnhancementCategory.ELEMENTAL, flame.category());
        assertEquals(EnhancementCategory.ELEMENTAL, wind.category());
        assertEquals(EnhancementProfile.SWORD, flame.profile());
        assertEquals(EnhancementProfile.SWORD, wind.profile());
        assertTrue(EnhancementCatalog.customDefinition(ModIds.id("not_registered")).isEmpty());
    }

    @Test
    void unsupportedItemsRemainRejected() {
        assertTrue(EnhancementCatalog.definition(Items.SHIELD).isEmpty());
        assertTrue(EnhancementCatalog.definition(Items.DIRT).isEmpty());
    }
}
