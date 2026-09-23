package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.network.FishingChallengePayload;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FishingChallenges {
    private static final Map<UUID, Challenge> ACTIVE = new HashMap<>();

    private FishingChallenges() {}

    public static void initialize() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                cancel(handler.player, false));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Challenge challenge = ACTIVE.get(player.getUUID());
                if (challenge == null) continue;
                boolean valid = player.isAlive()
                        && player.level() == challenge.level()
                        && !challenge.hook().isRemoved()
                        && challenge.hook().getOwner() == player
                        && player.fishing == challenge.hook()
                        && (player.getMainHandItem() == challenge.rod()
                            || player.getOffhandItem() == challenge.rod());
                if (!valid || FishingTiming.timedOut(player.level().getGameTime() - challenge.startTick()))
                    cancel(player, true);
            }
        });
    }

    public static boolean active(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }

    public static void start(ServerPlayer player, FishingHook hook, ItemStack rod) {
        if (active(player)) return;
        RodEquipment equipped = rod.getOrDefault(ModDataComponents.ROD_EQUIPMENT, RodEquipment.EMPTY);
        long startTick = player.level().getGameTime();
        Challenge challenge = new Challenge(startTick, equipped.grade(FishingEquipmentFamily.REEL),
                equipped.grade(FishingEquipmentFamily.CRANKBAIT),
                equipped.grade(FishingEquipmentFamily.MINNOW), measureDepth(hook), 0,
                hook, player.level(), rod);
        ACTIVE.put(player.getUUID(), challenge);
        RodEquipment worn = new RodEquipment(equipped.reel(), equipped.crankbait(), equipped.minnow());
        wear(worn);
        rod.set(ModDataComponents.ROD_EQUIPMENT, new RodEquipment(worn.reel(), worn.crankbait(), worn.minnow()));
        ServerPlayNetworking.send(player, new FishingChallengePayload(true, startTick));
    }

    public static void confirm(ServerPlayer player) {
        Challenge challenge = ACTIVE.get(player.getUUID());
        if (challenge == null) return;
        boolean valid = player.isAlive() && player.level() == challenge.level()
                && !challenge.hook().isRemoved() && challenge.hook().getOwner() == player
                && player.fishing == challenge.hook()
                && (player.getMainHandItem() == challenge.rod()
                    || player.getOffhandItem() == challenge.rod());
        long elapsed = player.level().getGameTime() - challenge.startTick();
        finish(player, valid && !FishingTiming.timedOut(elapsed) && FishingTiming.successful(elapsed), true);
    }

    private static void cancel(ServerPlayer player, boolean notify) { finish(player, false, notify); }

    private static void finish(ServerPlayer player, boolean success, boolean notify) {
        Challenge challenge = ACTIVE.remove(player.getUUID());
        if (challenge == null) return;
        if (!challenge.hook().isRemoved()) challenge.hook().discard();
        if (player.fishing == challenge.hook()) player.fishing = null;
        if (success) {
            List<FishSpecies> pool = FishingLogic.eligible(
                    challenge.depth(), challenge.reel(), challenge.minnow());
            if (!pool.isEmpty()) {
                FishingLogic.CatchRoll roll = FishingLogic.select(
                        pool, challenge.crankbait(), challenge.luck(), player.getRandom());
                FishSpecies species = roll.species();
                int length = species.lengthFromRoll(player.getRandom().nextInt());
                ItemStack fish = new ItemStack(ModItems.fishItem(species));
                fish.set(ModDataComponents.FISH_GRADE, roll.grade());
                fish.set(ModDataComponents.FISH_LENGTH, length);
                player.getInventory().add(fish);
                if (!fish.isEmpty()) {
                    player.level().addFreshEntity(new ItemEntity(player.level(),
                            player.getX(), player.getY(), player.getZ(), fish));
                }
            }
        }
        if (notify) ServerPlayNetworking.send(player, new FishingChallengePayload(false, 0));
    }

    private static int measureDepth(FishingHook hook) {
        BlockPos.MutableBlockPos pos = hook.blockPosition().mutable();
        int water = 0;
        for (int i = 0; i < 20 && hook.level().getFluidState(pos).is(FluidTags.WATER);
                i++, pos.move(0, -1, 0)) water++;
        return water >= 12 ? 5 : water >= 8 ? 4 : water >= 5 ? 3 : water >= 3 ? 2 : 1;
    }

    private static void wear(RodEquipment equipment) {
        for (int i = 0; i < 3; i++) {
            ItemStack stack = equipment.slot(i);
            if (!stack.isEmpty()) {
                stack.setDamageValue(stack.getDamageValue() + 1);
                if (stack.getDamageValue() >= stack.getMaxDamage()) stack.shrink(1);
            }
        }
    }

    public record Challenge(long startTick, int reel, int crankbait, int minnow,
                            int depth, int luck, FishingHook hook, Level level, ItemStack rod) {}
}
