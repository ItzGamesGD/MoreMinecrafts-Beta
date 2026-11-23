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

    @GameTest(maxTicks = 20)
    public void featherCharmIsMappedAndNowRegistered(GameTestHelper ctx) {
        var entry = ArtifactCoreMapping.ALL.stream().filter(e -> "feather_charm".equals(e.accessoryId())).findFirst();
        ctx.assertTrue(entry.isPresent(), "Feather Charm mapping record exists");
        ctx.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(dev.itzgamesgd.moreminecrafts.core.ModIds.id("feather_charm")).isPresent(),
                "Feather Charm item is registered (owner-decided id feather_charm)");
        ctx.succeed();
    }

    @GameTest(maxTicks = 20)
    public void approvedDropRollCanBeReplayedFromRandomGenerator(GameTestHelper ctx) {
        java.util.random.RandomGenerator seeded = new java.util.SplittableRandom(20261116L);
        int first = ArtifactCoreDropRules.rollCount(ArtifactCoreDropRules.Tier.BOSS, seeded);
        int replay = ArtifactCoreDropRules.rollCount(ArtifactCoreDropRules.Tier.BOSS,
                new java.util.SplittableRandom(20261116L));
        ctx.assertTrue(first == replay && (first == 1 || first == 2), "boss drop roll is reproducible and yields one or two");
        ctx.succeed();
    }

    @GameTest(maxTicks = 20)
    public void bioMobRankExamplesUseTheirDocumentedDropTiers(GameTestHelper ctx) {
        var ordinary = net.minecraft.world.entity.EntityTypes.ENDERMAN.create(ctx.getLevel(),
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var elite = net.minecraft.world.entity.EntityTypes.RAVAGER.create(ctx.getLevel(),
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var boss = net.minecraft.world.entity.EntityTypes.ELDER_GUARDIAN.create(ctx.getLevel(),
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        ctx.assertTrue(ordinary != null && ArtifactCoreDropRules.tier(ordinary) == ArtifactCoreDropRules.Tier.GENERAL_STRONG,
                "BIO §2.2 ordinary example uses general rate");
        ctx.assertTrue(elite != null && ArtifactCoreDropRules.tier(elite) == ArtifactCoreDropRules.Tier.HIGH_ELITE,
                "BIO §2.2 high elite uses elite rate");
        ctx.assertTrue(boss != null && ArtifactCoreDropRules.tier(boss) == ArtifactCoreDropRules.Tier.BOSS,
                "BIO §2.2 boss example uses guaranteed rate");
        ctx.succeed();
    }
}
