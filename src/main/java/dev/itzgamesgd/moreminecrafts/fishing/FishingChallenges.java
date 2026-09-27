package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.network.FishingChallengePayload;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.HashMap;
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
        RodEquipment worn = new RodEquipment(equipment.reel(), equipment.crankbait(), equipment.minnow());
        ModuleDurability.wear(worn.reel(), FishingEquipmentFamily.REEL);
        rod.set(ModDataComponents.ROD_EQUIPMENT, worn);

        long startTick = player.level().getGameTime();
        ACTIVE.put(player.getUUID(), new Challenge(startTick, species,
                worn.tier(FishingEquipmentFamily.CRANKBAIT), worn.tier(FishingEquipmentFamily.MINNOW),
                hook, player.level(), rod));
        ServerPlayNetworking.send(player, new FishingChallengePayload(true, startTick, species.fishingDifficulty(), species.ordinal()));
        return true;
    }

    public static void confirm(ServerPlayer player) {
        Challenge challenge = ACTIVE.get(player.getUUID());
        if (challenge == null) return;
        long elapsed = player.level().getGameTime() - challenge.startTick();
        FishingTiming.Zone zone = FishingTiming.zone(FishingTiming.position(elapsed, challenge.species().fishingDifficulty()));
        finish(player, valid(player, challenge) && !FishingTiming.timedOut(elapsed)
                && FishingTiming.captures(zone, player.getRandom()), true);
    }

    private static boolean valid(ServerPlayer player, Challenge challenge) {
        return player.isAlive() && player.level() == challenge.level() && !challenge.hook().isRemoved()
                && challenge.hook().getOwner() == player && player.fishing == challenge.hook()
                && (player.getMainHandItem() == challenge.rod() || player.getOffhandItem() == challenge.rod());
    }

    private static void cancel(ServerPlayer player, boolean notify) { finish(player, false, notify); }

    private static void finish(ServerPlayer player, boolean minigameSuccess, boolean notify) {
        Challenge challenge = ACTIVE.remove(player.getUUID());
        if (challenge == null) return;
        if (!challenge.hook().isRemoved()) challenge.hook().discard();
        if (player.fishing == challenge.hook()) player.fishing = null;
        if (minigameSuccess) awardIfPermitted(player, challenge);
        if (notify) ServerPlayNetworking.send(player, new FishingChallengePayload(false, 0, 1, 0));
    }

    private static void awardIfPermitted(ServerPlayer player, Challenge challenge) {
        RodEquipment equipment = challenge.rod().getOrDefault(ModDataComponents.ROD_EQUIPMENT, RodEquipment.EMPTY);
        int minnowTier = equipment.tier(FishingEquipmentFamily.MINNOW);
        if (!FishingLogic.canCapture(challenge.species(), minnowTier)) {
            player.sendSystemMessage(Component.translatable("message.moreminecrafts.fishing.minnow_required",
                    FishingGrade.roman(challenge.species().requiredMinnowTier())), false);
            return;
        }
        int crankbaitTier = equipment.tier(FishingEquipmentFamily.CRANKBAIT);
        int grade = FishingLogic.rollGrade(crankbaitTier, player.getRandom());
        int length = FishingLogic.rollLength(challenge.species(), player.getRandom());
        int weight = FishingLogic.rollWeight(challenge.species(), player.getRandom());
        ItemStack fish = new ItemStack(ModItems.fishItem(challenge.species()));
        fish.set(ModDataComponents.FISH_GRADE, grade);
        fish.set(ModDataComponents.FISH_LENGTH_TENTHS_CM, length);
        fish.set(ModDataComponents.FISH_WEIGHT_TENTHS_KG, weight);
        ModuleDurability.wear(equipment.minnow(), FishingEquipmentFamily.MINNOW);
        ModuleDurability.wear(equipment.crankbait(), FishingEquipmentFamily.CRANKBAIT);
        challenge.rod().set(ModDataComponents.ROD_EQUIPMENT, equipment);
        player.getInventory().add(fish);
        if (!fish.isEmpty()) player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), fish));
    }

    public record Challenge(long startTick, FishSpecies species, int crankbaitTier, int minnowTier,
                            FishingHook hook, Level level, ItemStack rod) {}
}
