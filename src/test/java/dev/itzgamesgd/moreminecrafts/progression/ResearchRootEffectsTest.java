package dev.itzgamesgd.moreminecrafts.progression;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ResearchRootEffectsTest {
    @Test void allSevenRootsHaveOneSingleLevelPurchaseGatedPassive() {
        for (String id : new String[]{"resources_root", "production_root", "living_root", "combat_root",
                "ecology_root", "world_root", "alchemy_root"}) {
            var root = ResearchCatalog.byId(id);
            assertNotNull(root, id);
            assertEquals(1, root.maximumLevel(), id);
            assertEquals(1, ResearchRewards.effects(id).size(), id);
            assertEquals(1, ResearchRewards.effects(id).getFirst().maximumLevel(), id);
        }
        assertEquals(0.01D, ResearchRewards.effects("combat_root").getFirst().valuePerLevel());
        assertEquals(0.01D, ResearchRewards.effects("ecology_root").getFirst().valuePerLevel());
    }

    @Test void allFourAuditedLowComplexityEffectsUseTheApprovedPerLevelValues() {
        assertEquals(0.02D, ResearchRewards.effects("v3.research.27").getFirst().valuePerLevel());
        assertEquals(0.10D, ResearchRewards.effects("v3.research.30").getFirst().valuePerLevel());
        assertEquals(0.05D, ResearchRewards.effects("v3.research.31").getFirst().valuePerLevel());
        assertEquals(0.06D, ResearchRewards.effects("v3.research.32").getFirst().valuePerLevel());
    }

    @Test void fractionalResearchXpRemainderSurvivesProgressStateSerialization() {
        var original = ResearchProgressState.empty()
                .with("research.xp_fraction.resources_ore", 63)
                .with("research.xp_fraction.production_smelt", 17)
                .with("research.xp_fraction.ecology_hostile", 99);
        var encoded = ResearchProgressState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, original)
                .getOrThrow();
        var restored = ResearchProgressState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(original, restored);
    }
}
