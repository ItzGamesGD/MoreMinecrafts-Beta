package dev.itzgamesgd.moreminecrafts.pyramid;

import dev.itzgamesgd.moreminecrafts.boss.TombGuardianEncounters;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * D13-03-I06 runtime for the two royal reward chests of a pyramid instance. It is structure-independent: the (future)
 * ST-01 integration calls {@link #register} once per generated pyramid with a stable instance id and the two chest
 * positions; nothing here depends on structure NBT, worldgen or coordinates beyond locating those chests.
 *
 * <p>Access contract: sealed until the Tomb Guardian is normally defeated; PRIMARY's first sealed interaction keeps
 * driving the existing encounter; rewards are generated once on the first valid direct player opening after defeat;
 * the shared inventory is withdraw-only; hoppers/minecarts, mining and explosions can never touch a registered chest.
 */
public final class RoyalChestRuntime {
    private RoyalChestRuntime() {}

    public enum OpenResult {
        /** The position is not a registered royal chest; vanilla behaviour applies. */
        NOT_ROYAL_CHEST,
        /** The actor is not a real player (fake player, spectator): nothing is evaluated. */
        NOT_A_PLAYER,
        /** Sealed: PRIMARY started or continued the guardian encounter. */
        SEALED_ENCOUNTER_STARTED,
        /** Sealed: no reward evaluated (encounter already running, retry pending, or SECONDARY). */
        SEALED,
        /** Opened an already generated chest. */
        OPENED,
        /** Opened and generated this chest's rewards just now. */
        OPENED_AND_GENERATED
    }

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS; // the client never knows registrations; the server decides
            }
            OpenResult result = tryOpen(serverLevel, serverPlayer, hit.getBlockPos());
            return result == OpenResult.NOT_ROYAL_CHEST ? InteractionResult.PASS : InteractionResult.SUCCESS;
        });
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !isGuarded(level, pos));
    }

    // ---- registration (integration interface) ----

    public static RoyalChestData data(ServerLevel level) { return level.getDataStorage().computeIfAbsent(RoyalChestData.TYPE); }

    /**
     * Registers a pyramid instance. {@code tombAnchor} is the key the existing Tomb Guardian encounter uses (the
     * PRIMARY chest position by default); {@code bossSpawn} is the structure's prevalidated emergence point.
     * Re-registering the same {@code structureInstanceId} (repair, NBT replacement, chunk reload) relocates the chests
     * and keeps its catalyst assignment, results and remaining items. The catalyst is decided here, once, from
     * (world seed, instance id), before either chest can be opened, so opening order cannot influence it.
     */
    public static Optional<RoyalChestData.Instance> register(ServerLevel level, String structureInstanceId, BlockPos tombAnchor,
                                                              BlockPos bossSpawn, BlockPos primary, BlockPos secondary) {
        if (structureInstanceId == null || structureInstanceId.isBlank()) return Optional.empty();
        Optional<RoyalChestRole> fresh = RoyalChestRewardModel.assignCatalyst(catalystRandom(level.getSeed(), structureInstanceId));
        return data(level).register(structureInstanceId, tombAnchor, bossSpawn, primary, secondary, fresh);
    }

    /** Administrative / test cleanup: forgets an instance with its results. Never part of normal play. */
    public static boolean discardInstance(ServerLevel level, String structureInstanceId) { return data(level).discard(structureInstanceId); }

    public static boolean isGuarded(@org.jetbrains.annotations.Nullable Level level, BlockPos pos) {
        return level instanceof ServerLevel serverLevel && data(serverLevel).linkAt(pos).isPresent();
    }

    // ---- deterministic randomness: a pure function of (seed, instance, role) ----

    static long mix(long worldSeed, String instanceId, long salt) {
        long h = 0xCBF29CE484222325L;
        for (byte b : instanceId.getBytes(StandardCharsets.UTF_8)) { h ^= b & 0xFF; h *= 0x100000001B3L; }
        long x = worldSeed ^ h ^ (salt * 0x9E3779B97F4A7C15L);
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    public static RandomGenerator catalystRandom(long worldSeed, String instanceId) { return new SplittableRandom(mix(worldSeed, instanceId, 0x1L)); }

    public static RandomGenerator chestRandom(long worldSeed, String instanceId, RoyalChestRole role) {
        return new SplittableRandom(mix(worldSeed, instanceId, 0x10L + role.ordinal()));
    }

    // ---- opening ----

    private static boolean sealed(ServerLevel level, RoyalChestData.Instance instance) {
        return TombGuardianEncounters.isRoyalChestSealed(level, instance.tombAnchor());
    }

    /** Evaluates a direct player interaction with {@code pos}. Opens the shared menu when the chest is unsealed. */
    public static OpenResult tryOpen(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Optional<RoyalChestData.Link> link = data(level).linkAt(pos);
        if (link.isEmpty()) return OpenResult.NOT_ROYAL_CHEST;
        if (player instanceof FakePlayer || player.isSpectator()) return OpenResult.NOT_A_PLAYER;
        RoyalChestData.Instance instance = link.get().instance();
        RoyalChestRole role = link.get().role();
        if (sealed(level, instance)) {
            if (role != RoyalChestRole.PRIMARY) return OpenResult.SEALED;
            // Existing PRIMARY trigger, unchanged: STARTED / RESTARTED spawn the guardian; the rest are no-ops.
            TombGuardianEncounters.StartResult started =
                    TombGuardianEncounters.onRoyalChestInteraction(level, instance.tombAnchor(), instance.spawn(), player);
            return started.spawned() ? OpenResult.SEALED_ENCOUNTER_STARTED : OpenResult.SEALED;
        }
        RoyalChestData data = data(level);
        boolean generatedNow = !data.isGenerated(instance.id(), role);
        RoyalChestData.Chest chest = generatedNow ? generate(level, data, instance, role) : data.chest(instance.id(), role).orElseThrow();
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new RoyalChestMenu(id, inventory, chest.container(), level, pos),
                Component.translatable("container.moreminecrafts.royal_chest." + role.name().toLowerCase(java.util.Locale.ROOT))));
        return generatedNow ? OpenResult.OPENED_AND_GENERATED : OpenResult.OPENED;
    }

    /** Draws and installs one chest's rewards. Runs on the server thread, so a second opener sees the finished chest. */
    static RoyalChestData.Chest generate(ServerLevel level, RoyalChestData data, RoyalChestData.Instance instance, RoyalChestRole role) {
        RandomGenerator random = chestRandom(level.getSeed(), instance.id(), role);
        boolean holdsCatalyst = instance.catalyst().filter(role::equals).isPresent();
        List<RoyalChestRewardModel.Reward> rewards = RoyalChestRewardModel.chestRewards(role, random, holdsCatalyst);
        List<RoyalChestRewardModel.Stack> stacks = RoyalChestRewardModel.split(rewards, RoyalChestRuntime::maxStackSize);
        return data.install(instance.id(), role, stacks);
    }

    static int maxStackSize(String itemId) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(itemId))).getMaxStackSize();
    }
}
