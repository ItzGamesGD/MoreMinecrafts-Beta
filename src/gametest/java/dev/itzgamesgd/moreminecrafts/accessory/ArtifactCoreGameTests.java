package dev.itzgamesgd.moreminecrafts.accessory;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ArtifactCoreGameTests {
    @GameTest(maxTicks = 20)
    public void allDocumentedArtifactCoreItemsAreRegistered(GameTestHelper ctx) {
        for (ArtifactCore core : ArtifactCore.values()) {
            var item = BuiltInRegistries.ITEM.get(ModIds.id(core.id()));
            ctx.assertTrue(item.isPresent(), core.id() + " is registered");
            ctx.assertTrue(!item.orElseThrow().value().getDefaultInstance().isEmpty(), core.id() + " creates an item stack");
        }
        ctx.succeed();
    }

    @GameTest(maxTicks = 20)
    public void worldBiomeAdapterClassifiesTheActualTestPosition(GameTestHelper ctx) {
        var core = ArtifactCoreBiomeRules.classify(ctx.getLevel(), ctx.absolutePos(new net.minecraft.core.BlockPos(1, 250, 1)));
        ctx.assertTrue(core != null, "overworld test position falls into a documented biome class");
        ctx.succeed();
    }
}
