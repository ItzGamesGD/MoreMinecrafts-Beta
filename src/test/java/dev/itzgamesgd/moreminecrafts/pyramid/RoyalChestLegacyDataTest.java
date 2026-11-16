package dev.itzgamesgd.moreminecrafts.pyramid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoyalChestLegacyDataTest {
    @Test
    void savedRoyalChestWithAnUnregisteredItemLoadsAndPreservesRemainingContents() {
        var saved = new RoyalChestData.Saved(List.of(), List.of(new RoyalChestData.ChestSave(
                "legacy-pyramid", RoyalChestRole.PRIMARY, List.of(
                        new RoyalChestData.StackSave(0, "moreminecrafts:retired_metal_ingot", 32)))));

        RoyalChestData loaded = RoyalChestData.fromSaved(saved);
        var chest = loaded.chest("legacy-pyramid", RoyalChestRole.PRIMARY).orElseThrow().container();

        assertTrue(loaded.isGenerated("legacy-pyramid", RoyalChestRole.PRIMARY));
        assertTrue(chest.getItem(0).isEmpty(), "unknown legacy item is skipped without aborting the saved-data load");
        assertEquals(RoyalChestRewardModel.SLOTS, chest.getContainerSize(), "the chest remains available to continue loading the world");
    }
}
