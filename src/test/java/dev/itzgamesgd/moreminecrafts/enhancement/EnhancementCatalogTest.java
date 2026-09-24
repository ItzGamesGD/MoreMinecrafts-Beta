package dev.itzgamesgd.moreminecrafts.enhancement;

import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnhancementCatalogTest {
    @BeforeAll static void bootstrapMinecraft() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    @Test void vanillaItemsRetainExplicitVanillaDefinitions() {
        var sword = EnhancementCatalog.definition(Items.IRON_SWORD).orElseThrow();
        assertEquals(EnhancementCategory.VANILLA, sword.category());
        assertEquals(EnhancementProfile.SWORD, sword.profile());
        assertEquals(EnhancementProfile.MACE, EnhancementCatalog.definition(Items.MACE).orElseThrow().profile());
    }
    @Test void elementalSwordsResolveByRegistryIdWithoutCatalogInitializationCycle() {
        var flame = EnhancementCatalog.definition(ModItems.FLAME_SWORD).orElseThrow();
        var wind = EnhancementCatalog.definition(ModItems.WIND_SWORD).orElseThrow();
        assertEquals(EnhancementCategory.ELEMENTAL, flame.category());
        assertEquals(EnhancementCategory.ELEMENTAL, wind.category());
        assertEquals(EnhancementProfile.SWORD, flame.profile());
        assertEquals(EnhancementProfile.SWORD, wind.profile());
    }
    @Test void unsupportedItemsRemainRejected() {
        assertTrue(EnhancementCatalog.definition(Items.SHIELD).isEmpty());
        assertTrue(EnhancementCatalog.definition(Items.DIRT).isEmpty());
    }
}
