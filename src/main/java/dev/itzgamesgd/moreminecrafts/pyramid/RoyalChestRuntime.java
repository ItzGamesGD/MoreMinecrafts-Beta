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
 * ST-01 controlled placement adapter calls {@link #register} with transformed world positions. Natural worldgen
 * registration is deliberately outside this integration.
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
        /** Registration is retained, but the physical chest is temporarily absent. No rewards are evaluated. */
        CHEST_UNAVAILABLE,
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

    /** Dimension and transformed world-space anchor are the complete deterministic identity. */
    public static String canonicalId(ServerLevel level, BlockPos tombAnchor) {
        return level.dimension().identifier() + "/" + tombAnchor.getX() + "," + tombAnchor.getY() + "," + tombAnchor.getZ();
    }

    /** Both physical PRIMARY halves route to one role. ST-01 single PRIMARY registration (2026-11-12 override). */
    public static Optional<RoyalChestData.Instance> register(ServerLevel level, BlockPos tombAnchor,
            BlockPos bossSpawn, BlockPos primaryLeft, BlockPos primaryRight) {
        return register(level, tombAnchor, bossSpawn, primaryLeft, primaryRight, Optional.empty());
    }

    /** Both physical PRIMARY halves route to one role. Retains optional secondary for backward compatibility. */
    public static Optional<RoyalChestData.Instance> register(ServerLevel level, BlockPos tombAnchor,
            BlockPos bossSpawn, BlockPos primaryLeft, BlockPos primaryRight, Optional<BlockPos> secondary) {
        if (!tombAnchor.equals(primaryLeft) || !validPrimary(level, primaryLeft, primaryRight)) return Optional.empty();
        if (secondary.filter(pos -> !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CHEST)).isPresent())
            return Optional.empty();
        String id = canonicalId(level, tombAnchor);
        // Existing assignment (including no catalyst) is authoritative; never redraw at re-registration.
        Optional<RoyalChestRole> catalyst = data(level).instance(id).map(RoyalChestData.Instance::catalyst)
                .orElseGet(() -> RoyalChestRewardModel.assignCatalyst(catalystRandom(level.getSeed(), id)));
        return data(level).register(id, tombAnchor, bossSpawn, primaryLeft, Optional.of(primaryRight), secondary, catalyst);
    }

    static boolean validPrimary(ServerLevel level, BlockPos left, BlockPos right) {
        var a = level.getBlockState(left);
        var b = level.getBlockState(right);
        if (!a.is(net.minecraft.world.level.block.Blocks.CHEST) || !b.is(net.minecraft.world.level.block.Blocks.CHEST)) return false;
        var typeA = a.getValue(net.minecraft.world.level.block.ChestBlock.TYPE);
        var typeB = b.getValue(net.minecraft.world.level.block.ChestBlock.TYPE);
        return typeA != net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                && typeB != net.minecraft.world.level.block.state.properties.ChestType.SINGLE && typeA != typeB
                && a.getValue(net.minecraft.world.level.block.ChestBlock.FACING) == b.getValue(net.minecraft.world.level.block.ChestBlock.FACING)
                && left.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(a)).equals(right)
                && right.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(b)).equals(left);
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
        if (!level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CHEST)) return OpenResult.CHEST_UNAVAILABLE;
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

    /** Test seam; always null in production. */
    static volatile java.util.function.Function<RoyalChestRole, List<RoyalChestRewardModel.Reward>> rewardsOverride;

    /** Draws and installs one chest's rewards. Runs on the server thread, so a second opener sees the finished chest. */
    static RoyalChestData.Chest generate(ServerLevel level, RoyalChestData data, RoyalChestData.Instance instance, RoyalChestRole role) {
        if (instance.chestPos(role).isEmpty()) throw new IllegalStateException("Royal chest role is not placed: " + role);
        Optional<RoyalChestData.Chest> existing = data.chest(instance.id(), role);
        if (existing.isPresent()) return existing.get();
        RandomGenerator random = chestRandom(level.getSeed(), instance.id(), role);
        boolean holdsCatalyst = instance.catalyst().filter(c -> c == role || (role == RoyalChestRole.PRIMARY && instance.secondary().isEmpty())).isPresent();
        List<RoyalChestRewardModel.Reward> rewards = RoyalChestRewardModel.chestRewards(role, random, holdsCatalyst);
        java.util.function.Function<RoyalChestRole, List<RoyalChestRewardModel.Reward>> forced = rewardsOverride;
        if (forced != null) rewards = forced.apply(role); // tests only: pins whether an accessory is drawn
        List<RoyalChestRewardModel.Stack> stacks = RoyalChestRewardModel.split(rewards, RoyalChestRuntime::maxStackSize);
        RoyalChestData.Chest installed = data.install(instance.id(), role, stacks);
        // D13-03 Q39-Q41: tied to this one install (the persisted "generated" state), so reopening, reload and restart,
        // which find the chest already generated and return above, can never replay it.
        if (RoyalChestRewardModel.hasAccessory(rewards)) RoyalChestRevealFx.play(level, instance, role);
        return installed;
    }

    static int maxStackSize(String itemId) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(itemId))).getMaxStackSize();
    }
}
