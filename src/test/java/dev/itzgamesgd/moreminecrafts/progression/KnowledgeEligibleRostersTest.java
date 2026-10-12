package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

final class KnowledgeEligibleRostersTest {
    @Test void rockRosterIsExplicitRegisteredAndHasSurvivalAcquisitionEvidence() {
        assertTrue(KnowledgeEligibleRosters.rockTypes().size() >= 8);
        for (var rock : KnowledgeEligibleRosters.rockTypes()) {
            assertNotNull(BuiltInRegistries.BLOCK.getKey(rock.block()));
            assertEquals(rock.id(), BuiltInRegistries.BLOCK.getKey(rock.block()));
            assertFalse(rock.acquisition().isBlank());
            assertFalse(rock.classification().isBlank());
            assertFalse(rock.block() == Blocks.AIR);
        }
        assertFalse(KnowledgeEligibleRosters.isEligibleRock(Blocks.STONE_BRICKS));
        assertFalse(KnowledgeEligibleRosters.isEligibleRock(Blocks.IRON_ORE));
    }

    @Test void toolRosterIncludesEveryRegisteredApprovedVanillaMaterialFamilyPair() {
        Set<String> actual = KnowledgeEligibleRosters.vanillaToolItems().stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .collect(Collectors.toSet());
        for (String material : new String[]{"wooden", "stone", "copper", "iron", "golden", "diamond", "netherite"}) {
            for (String family : new String[]{"pickaxe", "axe", "shovel", "hoe", "sword"}) {
                Identifier id = Identifier.parse("minecraft:" + material + "_" + family);
                boolean registered = BuiltInRegistries.ITEM.containsKey(id);
                assertEquals(registered, actual.contains(id.toString()), "registry-aware roster for " + id);
            }
        }
    }

    @Test void toolRosterExcludesOtherEquipmentAndModdedItems() {
        assertFalse(KnowledgeEligibleRosters.isEligibleVanillaTool(Items.BOW));
        assertFalse(KnowledgeEligibleRosters.isEligibleVanillaTool(Items.CROSSBOW));
        assertFalse(KnowledgeEligibleRosters.isEligibleVanillaTool(Items.TRIDENT));
        assertFalse(KnowledgeEligibleRosters.isEligibleVanillaTool(Items.MACE));
        assertFalse(KnowledgeEligibleRosters.isEligibleVanillaToolId(Identifier.parse("moreminecrafts:mythril_pickaxe")));
        assertFalse(KnowledgeEligibleRosters.isEligibleVanillaToolId(Identifier.parse("minecraft:brush")));
    }
}
