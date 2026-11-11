package dev.itzgamesgd.moreminecrafts;

import dev.itzgamesgd.moreminecrafts.registry.ModRegistries;
import dev.itzgamesgd.moreminecrafts.network.ModNetworking;
import dev.itzgamesgd.moreminecrafts.zombie.BombZombieEffects;
import dev.itzgamesgd.moreminecrafts.progression.ResearchAxeCombatRuntime;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingCommand;
import dev.itzgamesgd.moreminecrafts.skeleton.SkeletonKingLifecycle;
import dev.itzgamesgd.moreminecrafts.test.LiveTestCommand;
import dev.itzgamesgd.moreminecrafts.worldgen.structure.StructurePoolContributions;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MoreMinecrafts implements ModInitializer {
    public static final String MOD_ID = "moreminecrafts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        dev.itzgamesgd.moreminecrafts.worldgen.structure.AdaptiveJigsawStructure.initialize();
        StructurePoolContributions.initialize();
        dev.itzgamesgd.moreminecrafts.accessory.RelicCatalystSources.initialize();
        dev.itzgamesgd.moreminecrafts.accessory.T2StructureChestAccessories.initialize();
        dev.itzgamesgd.moreminecrafts.progression.DeepSeaPearlContribution.initialize();
        dev.itzgamesgd.moreminecrafts.accessory.TombGuardianGravityHeart.initialize();
        dev.itzgamesgd.moreminecrafts.pyramid.RoyalChestRuntime.initialize();
        dev.itzgamesgd.moreminecrafts.pyramid.worldgen.St01Worldgen.initialize();
        dev.itzgamesgd.moreminecrafts.worldgen.structure.ArenaWorldgenProbe.initialize();
        dev.itzgamesgd.moreminecrafts.colosseum.ColosseumRuntime.initialize();
        dev.itzgamesgd.moreminecrafts.worldgenprobe.stage0f.St05NaturalGeneration.initialize();
        dev.itzgamesgd.moreminecrafts.skytower.SkyTowerIntegration.initialize();
        ModRegistries.initialize();
        ModNetworking.initialize();
        BombZombieEffects.initialize();
        ResearchAxeCombatRuntime.initialize();
        SkeletonKingCommand.initialize();
        SkeletonKingLifecycle.initialize();
        LiveTestCommand.initialize();
        LOGGER.info("MoreMinecrafts common initialization complete.");
    }
}
