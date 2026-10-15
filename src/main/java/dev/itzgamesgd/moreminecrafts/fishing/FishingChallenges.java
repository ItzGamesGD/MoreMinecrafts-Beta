package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.network.FishingChallengePayload;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FishingChallenges {
    private static final Map<UUID, Challenge> ACTIVE = new HashMap<>();
    private FishingChallenges() {}

    public static void initialize() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancel(handler.player, false));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Challenge challenge = ACTIVE.get(player.getUUID());
                if (challenge != null && (!valid(player, challenge)
                        || FishingTiming.timedOut(player.level().getGameTime() - challenge.startTick())))
                    cancel(player, true);
            }
        });
    }

    public static boolean active(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }

    /** @return true only when custom fishing replaced vanilla retrieval. */
    public static boolean start(ServerPlayer player, FishingHook hook, ItemStack rod) {
        if (active(player)) return true;
        RodEquipment equipment = rod.getOrDefault(ModDataComponents.ROD_EQUIPMENT, RodEquipment.EMPTY);
        int reelTier = equipment.tier(FishingEquipmentFamily.REEL);
        if (reelTier == 0 || !WaterBodyValidator.isEligible(player.level(), hook.blockPosition())) return false;

        var pool = FishingLogic.accessibleSpecies(reelTier); // Ecology metadata is scaffolded; broad live validation bypasses it.
        if (pool.isEmpty()) return false;
        FishSpecies species = FishingLogic.selectSpecies(pool, player.getRandom());
        RodEquipment worn = equipment.withWear(FishingEquipmentFamily.REEL);
        rod.set(ModDataComponents.ROD_EQUIPMENT, worn);

        long startTick = player.level().getGameTime();
        int fishingTechniqueLevel = dev.itzgamesgd.moreminecrafts.progression.ResearchRewards.level(
                player, "v3.research.23", "fishing_technique_window");
        ACTIVE.put(player.getUUID(), new Challenge(startTick, species, List.copyOf(pool),
                worn.tier(FishingEquipmentFamily.CRANKBAIT), worn.tier(FishingEquipmentFamily.MINNOW),
                hook, player.level(), rod, fishingTechniqueLevel));
        ServerPlayNetworking.send(player, new FishingChallengePayload(true, startTick, species.fishingDifficulty(),
                species.ordinal(), FishingLogic.canCapture(species, worn.tier(FishingEquipmentFamily.MINNOW)),
                species.requiredMinnowTier(), fishingTechniqueLevel));
        return true;
    }

    public static void confirm(ServerPlayer player) {
        Challenge challenge = ACTIVE.get(player.getUUID());
        if (challenge == null) return;
        long elapsed = player.level().getGameTime() - challenge.startTick();
        FishingTiming.Zone zone = FishingTiming.zone(FishingTiming.position(elapsed, challenge.species().fishingDifficulty()),
                challenge.fishingTechniqueLevel());
        boolean connected = player.connection != null;
        boolean valid = valid(player, challenge);
        boolean success = !FishingTiming.timedOut(elapsed) && FishingTiming.captures(zone, player.getRandom());
        finish(player, FishingChallengeRules.shouldAward(success, connected, valid), true);
    }

    private static boolean valid(ServerPlayer player, Challenge challenge) {
        return FishingChallengeRules.valid(player.connection != null, player.isAlive(),
                player.level() == challenge.level(), !challenge.hook().isRemoved(),
                challenge.hook().getOwner() == player, player.fishing == challenge.hook(),
                player.getMainHandItem() == challenge.rod() || player.getOffhandItem() == challenge.rod());
    }

    private static void cancel(ServerPlayer player, boolean notify) { finish(player, false, notify); }

    private static void finish(ServerPlayer player, boolean minigameSuccess, boolean notify) {
        Challenge challenge = ACTIVE.remove(player.getUUID());
        if (challenge == null) return;
        if (!challenge.hook().isRemoved()) challenge.hook().discard();
        if (player.fishing == challenge.hook()) player.fishing = null;
        if (minigameSuccess) awardIfPermitted(player, challenge);
        if (notify) ServerPlayNetworking.send(player, new FishingChallengePayload(false, 0, 1, 0, false, 1, 0));
    }

    private static void awardIfPermitted(ServerPlayer player, Challenge challenge) {
        List<FishSpecies> speciesRolls = new ArrayList<>(2);
        speciesRolls.add(challenge.species());
        int multiCatchLevel = dev.itzgamesgd.moreminecrafts.progression.T1Operations.enchantments(challenge.rod())
                .getOrDefault("moreminecrafts:multi_catch", 0);
        for (int i = 1; i < FishingChallengeRules.catchCount(multiCatchLevel > 0); i++) {
            speciesRolls.add(FishingLogic.selectSpecies(challenge.speciesPool(), player.getRandom()));
        }
        int deniedRequiredTier = 0;
        boolean caught = false;
        for (FishSpecies species : speciesRolls) {
            RodEquipment equipment = challenge.rod().getOrDefault(ModDataComponents.ROD_EQUIPMENT, RodEquipment.EMPTY);
            int minnowTier = equipment.tier(FishingEquipmentFamily.MINNOW);
            if (!FishingLogic.canCapture(species, minnowTier)) {
                deniedRequiredTier = Math.max(deniedRequiredTier, species.requiredMinnowTier());
                continue;
            }
            int crankbaitTier = equipment.tier(FishingEquipmentFamily.CRANKBAIT);
            int fisheriesStudyLevel = dev.itzgamesgd.moreminecrafts.progression.ResearchRewards.level(
                    player, "v3.research.19", "fisheries_grade");
            FishingLogic.FishRoll roll = FishingLogic.rollFish(species, crankbaitTier,
                    fisheriesStudyLevel, player.getRandom());
            ItemStack fish = new ItemStack(ModItems.fishItem(roll.species()));
            fish.set(ModDataComponents.FISH_GRADE, roll.grade());
            fish.set(ModDataComponents.FISH_LENGTH_TENTHS_CM, roll.lengthTenthsCm());
            fish.set(ModDataComponents.FISH_WEIGHT_TENTHS_KG, roll.weightTenthsKg());
            equipment = captureWear(equipment, true);
            challenge.rod().set(ModDataComponents.ROD_EQUIPMENT, equipment);
            deliver(player, fish);
            caught = true;
            dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime.recordDistinctCondition(
                    player, "fish_species_caught", roll.species().id());
            var biome = player.level().getBiome(player.blockPosition()).unwrapKey();
            biome.ifPresent(key -> dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime.recordDistinctCondition(
                    player, "water_biomes_fished", key.identifier().toString()));
        }
        if (caught) dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime.recordCondition(player, "fishing_successes",
                dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime.state(player)
                        .conditionProgress("fishing_successes") + 1);
        if (deniedRequiredTier > 0) {
            player.sendSystemMessage(Component.translatable("message.moreminecrafts.fishing.minnow_required",
                    FishingGrade.roman(deniedRequiredTier)), false);
        }
    }

    private static void deliver(ServerPlayer player, ItemStack fish) {
        player.getInventory().add(fish);
        if (!fish.isEmpty()) player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), fish));
    }

    static RodEquipment captureWear(RodEquipment equipment, boolean capturePermitted) {
        return capturePermitted
                ? equipment.withWear(FishingEquipmentFamily.MINNOW).withWear(FishingEquipmentFamily.CRANKBAIT)
                : equipment;
    }

    public record Challenge(long startTick, FishSpecies species, List<FishSpecies> speciesPool,
                            int crankbaitTier, int minnowTier,
                            FishingHook hook, Level level, ItemStack rod, int fishingTechniqueLevel) {}
}
