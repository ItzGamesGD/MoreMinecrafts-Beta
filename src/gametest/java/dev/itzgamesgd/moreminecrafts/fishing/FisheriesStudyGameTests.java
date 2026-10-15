package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchRewards;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Server-path and fish-payload coverage for Fisheries Study. */
public final class FisheriesStudyGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void fisheriesStudyIsPlayerScopedAndPreservesFishPayloadAndTechniqueTiming(GameTestHelper ctx) {
        ServerPlayer unowned = ctx.makeMockServerPlayerInLevel();
        ServerPlayer researched = ctx.makeMockServerPlayerInLevel();
        researched.setAttached(KnowledgeRuntime.STATE, KnowledgeRuntime.state(researched)
                .with("research.v3.research.19", 4)
                .with("research.v3.research.23", 1));
        unowned.getRandom().setSeed(1_234_567L);
        researched.getRandom().setSeed(1_234_567L);

        ctx.assertTrue(FishingChallenges.fisheriesStudyLevel(unowned) == 0,
                "an unpurchased player resolves to the baseline grade table");
        ctx.assertTrue(FishingChallenges.fisheriesStudyLevel(researched) == 4,
                "the server resolves the paid Fisheries level for this player only");
        ctx.assertTrue(ResearchRewards.level(unowned, "v3.research.23", "fishing_technique_window") == 0
                        && ResearchRewards.level(researched, "v3.research.23", "fishing_technique_window") == 1,
                "the existing timing research remains separately player-gated");

        FishSpecies species = FishSpecies.COMMON_CARP;
        var baseline = FishingChallenges.rollFishForPlayer(unowned, species, 2);
        var improved = FishingChallenges.rollFishForPlayer(researched, species, 2);
        ctx.assertTrue(baseline.species() == improved.species()
                        && baseline.lengthTenthsCm() == improved.lengthTenthsCm()
                        && baseline.weightTenthsKg() == improved.weightTenthsKg(),
                "Fisheries changes grade odds without changing species, length, or weight");

        var baselineStack = FishingChallenges.createFishStack(baseline);
        var improvedStack = FishingChallenges.createFishStack(improved);
        ctx.assertTrue(baselineStack.is(ModItems.fishItem(species))
                        && improvedStack.is(ModItems.fishItem(species)),
                "both results retain the same species item");
        ctx.assertTrue(baselineStack.getOrDefault(ModDataComponents.FISH_LENGTH_TENTHS_CM, -1)
                        == improvedStack.getOrDefault(ModDataComponents.FISH_LENGTH_TENTHS_CM, -1)
                        && baselineStack.getOrDefault(ModDataComponents.FISH_WEIGHT_TENTHS_KG, -1)
                        == improvedStack.getOrDefault(ModDataComponents.FISH_WEIGHT_TENTHS_KG, -1),
                "the serialized fish length and weight payload remains unchanged");
        ctx.assertTrue(baselineStack.getOrDefault(ModDataComponents.FISH_GRADE, 0) == baseline.grade()
                        && improvedStack.getOrDefault(ModDataComponents.FISH_GRADE, 0) == improved.grade(),
                "only the grade component reflects the adjusted distribution");
        ctx.assertTrue(FishingTiming.zone(.415, 0) == FishingTiming.Zone.YELLOW
                        && FishingTiming.zone(.415, ResearchRewards.level(researched,
                                "v3.research.23", "fishing_technique_window")) == FishingTiming.Zone.LIGHT_GREEN,
                "the .23 timing window continues to work independently with Fisheries Study");
        ctx.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper ctx, Method method) throws ReflectiveOperationException {
        method.invoke(this, ctx);
    }
}
