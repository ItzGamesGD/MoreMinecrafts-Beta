package dev.itzgamesgd.moreminecrafts.progression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/** Server runtime for Brewing Basics (.59), Potion Study (.60), and Advanced Brewing (.66). */
public final class BrewingResearchRuntime {
    private BrewingResearchRuntime() {}

    public static void opened(BrewingStandBlockEntity stand, ServerPlayer player) {
        if (!(stand instanceof BrewingResearchStateAccess access)) return;
        var current = access.moreminecrafts$getResearchAttribution();
        var next = current.openedBy(player.getUUID());
        if (!next.equals(current)) access.moreminecrafts$setResearchAttribution(next);
    }

    public static void interacted(BrewingStandBlockEntity stand, ServerPlayer player) {
        if (!(stand instanceof BrewingResearchStateAccess access)) return;
        var current = access.moreminecrafts$getResearchAttribution();
        var next = current.interactedBy(player.getUUID());
        if (!next.equals(current)) access.moreminecrafts$setResearchAttribution(next);
    }

    public static List<ItemStack> snapshot(BrewingStandBlockEntity stand) {
        return List.of(
                stand.getItem(0).copy(),
                stand.getItem(1).copy(),
                stand.getItem(2).copy(),
                stand.getItem(3).copy(),
                stand.getItem(4).copy());
    }

    public static boolean inventoryChanged(BrewingStandBlockEntity stand, List<ItemStack> before) {
        if (before.size() != 5) return false;
        for (int slot = 0; slot < 5; slot++) {
            if (!ItemStack.matches(before.get(slot), stand.getItem(slot))) return true;
        }
        return false;
    }

    public static int level(ServerLevel level, BrewingStandBlockEntity stand, String researchId) {
        if (level == null || !(stand instanceof BrewingResearchStateAccess access)) return 0;
        return BlockEntityResearchAttribution.resolveLevel(
                level.getServer(), access.moreminecrafts$getResearchAttribution(), researchId);
    }

    public static int accelerateBrewTime(ServerLevel level, BrewingStandBlockEntity stand, int currentBrewTime) {
        if (!(stand instanceof BrewingResearchStateAccess access)) return currentBrewTime;
        int level59 = level(level, stand, "v3.research.59");
        var step = BrewingResearchRules.advanceBrewTime(
                currentBrewTime, level59, access.moreminecrafts$getBrewProgressRemainder());
        if (step.nextRemainder() != access.moreminecrafts$getBrewProgressRemainder()) {
            access.moreminecrafts$setBrewProgressRemainder(step.nextRemainder());
        }
        return step.nextBrewTime();
    }

    public static int fuelUsesPerBlazePowder(ServerLevel level, BrewingStandBlockEntity stand) {
        return BrewingResearchRules.fuelUsesPerBlazePowder(level(level, stand, "v3.research.66"));
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> BREWING_CATALYSTS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("moreminecrafts", "research/brewing_catalysts"));

    /** Preserves 1 unit of an eligible brewing catalyst at 5/10/15% for `v3.research.65`. */
    public static void tryPreserveCatalyst(ServerLevel level, BrewingStandBlockEntity stand,
                                           ItemStack ingredientBefore) {
        if (ingredientBefore.isEmpty() || !ingredientBefore.is(BREWING_CATALYSTS)) return;
        int level65 = level(level, stand, "v3.research.65");
        if (!ResearchStage3HarvestRules.catalystPreserved(level65, level.getRandom().nextDouble())) return;
        ItemStack after = stand.getItem(3);
        if (after.isEmpty()) {
            stand.setItem(3, ingredientBefore.copyWithCount(1));
            stand.setChanged();
        } else if (ItemStack.isSameItemSameComponents(after, ingredientBefore)
                && after.getCount() < ingredientBefore.getCount()
                && after.getCount() < after.getMaxStackSize()) {
            ItemStack restored = after.copy();
            restored.grow(1);
            stand.setItem(3, restored);
            stand.setChanged();
        }
    }

    /** Applies `.60` Redstone (+8/+16/+24%) and Glowstone (10/20/30% penalty mitigation) after `doBrew`. */
    public static void adjustBrewedPotions(ServerLevel level, BrewingStandBlockEntity stand,
                                           ItemStack ingredientBefore, ItemStack[] before) {
        if (ingredientBefore.isEmpty()) return;
        boolean redstone = ingredientBefore.is(Items.REDSTONE);
        boolean glowstone = ingredientBefore.is(Items.GLOWSTONE_DUST);
        if (!redstone && !glowstone) return;

        int level60 = level(level, stand, "v3.research.60");
        if (level60 <= 0) return;

        for (int slot = 0; slot < 3; slot++) {
            ItemStack afterStack = stand.getItem(slot);
            if (afterStack.isEmpty() || ItemStack.matches(before[slot], afterStack)) continue;
            PotionContents afterContents = afterStack.get(DataComponents.POTION_CONTENTS);
            if (afterContents == null) continue;

            PotionContents updated = redstone
                    ? applyRedstoneBonus(afterContents, level60)
                    : applyGlowstoneMitigation(before[slot].get(DataComponents.POTION_CONTENTS), afterContents, level60);
            if (!updated.equals(afterContents)) {
                ItemStack copy = afterStack.copy();
                copy.set(DataComponents.POTION_CONTENTS, updated);
                stand.setItem(slot, copy);
            }
        }
    }

    private static PotionContents applyRedstoneBonus(PotionContents contents, int level60) {
        PotionContents updated = contents;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().value().isInstantaneous() || effect.isInfiniteDuration()) continue;
            int extended = BrewingResearchRules.redstoneExtendedDuration(effect.getDuration(), level60);
            if (extended > effect.getDuration()) {
                updated = updated.withEffectAdded(new MobEffectInstance(
                        effect.getEffect(), extended, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        }
        return updated;
    }

    private static PotionContents applyGlowstoneMitigation(PotionContents beforeContents,
                                                           PotionContents afterContents, int level60) {
        if (beforeContents == null) return afterContents;
        Map<Holder<MobEffect>, Integer> beforeDurations = new HashMap<>();
        for (MobEffectInstance beforeEffect : beforeContents.getAllEffects()) {
            if (!beforeEffect.getEffect().value().isInstantaneous() && !beforeEffect.isInfiniteDuration()) {
                beforeDurations.merge(beforeEffect.getEffect(), beforeEffect.getDuration(), Math::max);
            }
        }
        if (beforeDurations.isEmpty()) return afterContents;

        PotionContents updated = afterContents;
        for (MobEffectInstance afterEffect : afterContents.getAllEffects()) {
            if (afterEffect.getEffect().value().isInstantaneous() || afterEffect.isInfiniteDuration()) continue;
            Integer prevDuration = beforeDurations.get(afterEffect.getEffect());
            if (prevDuration == null) continue;
            int mitigated = BrewingResearchRules.glowstoneMitigatedDuration(
                    prevDuration, afterEffect.getDuration(), level60);
            if (mitigated > afterEffect.getDuration()) {
                updated = updated.withEffectAdded(new MobEffectInstance(
                        afterEffect.getEffect(), mitigated, afterEffect.getAmplifier(),
                        afterEffect.isAmbient(), afterEffect.isVisible(), afterEffect.showIcon()));
            }
        }
        return updated;
    }

    public static void syncToPlayer(BrewingStandBlockEntity stand, ServerPlayer player) {
        if (!(stand instanceof BrewingResearchStateAccess access)) return;
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(
                player, dev.itzgamesgd.moreminecrafts.network.BrewingResearchSyncPayload.TYPE)) {
            int queued = (int) access.moreminecrafts$getInputQueue().stream().filter(s -> !s.isEmpty()).count();
            int outputs = (int) access.moreminecrafts$getOutputBuffer().stream().filter(s -> !s.isEmpty()).count();
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                    new dev.itzgamesgd.moreminecrafts.network.BrewingResearchSyncPayload(
                            access.moreminecrafts$isAutoProcessEnabled(), queued, outputs));
        }
    }

    public static boolean handleAction(ServerPlayer player,
                                       dev.itzgamesgd.moreminecrafts.network.BrewingResearchActionPayload payload) {
        if (player == null || !(player.containerMenu instanceof net.minecraft.world.inventory.BrewingStandMenu menu)
                || menu.slots.isEmpty()) return false;
        if (!(menu.slots.getFirst().container instanceof BrewingStandBlockEntity stand)
                || !(stand instanceof BrewingResearchStateAccess access)) return false;

        boolean changed = switch (payload.action()) {
            case dev.itzgamesgd.moreminecrafts.network.BrewingResearchActionPayload.ACTION_MIX_OR_CONCENTRATE ->
                    mixOrConcentrate(player, stand);
            case dev.itzgamesgd.moreminecrafts.network.BrewingResearchActionPayload.ACTION_TOGGLE_AUTO -> {
                if (ResearchRewards.level(player, "v3.research.69", "brewing_process_control") <= 0) yield false;
                access.moreminecrafts$setAutoProcessEnabled(!access.moreminecrafts$isAutoProcessEnabled());
                interacted(stand, player);
                yield true;
            }
            case dev.itzgamesgd.moreminecrafts.network.BrewingResearchActionPayload.ACTION_QUEUE_CARRIED -> {
                if (ResearchRewards.level(player, "v3.research.69", "brewing_process_control") <= 0) yield false;
                ItemStack carried = menu.getCarried();
                if (carried.isEmpty()) yield false;
                var queue = access.moreminecrafts$getInputQueue();
                for (int i = 0; i < queue.size(); i++) {
                    if (queue.get(i).isEmpty()) {
                        queue.set(i, carried.copy());
                        menu.setCarried(ItemStack.EMPTY);
                        stand.setChanged();
                        interacted(stand, player);
                        yield true;
                    }
                }
                yield false;
            }
            case dev.itzgamesgd.moreminecrafts.network.BrewingResearchActionPayload.ACTION_COLLECT_OUTPUTS -> {
                if (ResearchRewards.level(player, "v3.research.69", "brewing_process_control") <= 0) yield false;
                var outputs = access.moreminecrafts$getOutputBuffer();
                boolean moved = false;
                for (int i = 0; i < outputs.size(); i++) {
                    ItemStack stack = outputs.get(i);
                    if (stack.isEmpty()) continue;
                    if (player.getInventory().add(stack)) {
                        outputs.set(i, ItemStack.EMPTY);
                        moved = true;
                    }
                }
                if (moved) stand.setChanged();
                yield moved;
            }
            default -> false;
        };
        syncToPlayer(stand, player);
        return changed;
    }

    /** Executes `.68` Potion Mixing or `.70` Potion Concentration on bottle slots 0 and 1. */
    public static boolean mixOrConcentrate(ServerPlayer player, BrewingStandBlockEntity stand) {
        if (player == null || stand == null) return false;
        ItemStack stackA = stand.getItem(0);
        ItemStack stackB = stand.getItem(1);
        String deliveryA = deliveryType(stackA);
        String deliveryB = deliveryType(stackB);
        if (deliveryA == null || !deliveryA.equals(deliveryB)) return false;

        List<PotionMixingRules.EffectSpec> specsA = toEffectSpecs(stackA);
        List<PotionMixingRules.EffectSpec> specsB = toEffectSpecs(stackB);
        boolean concA = isConcentrated(stackA);
        boolean concB = isConcentrated(stackB);
        PotionMixingRules.Result result = PotionMixingRules.resolve(
                deliveryA, specsA, concA, deliveryB, specsB, concB);
        if (result.kind() == PotionMixingRules.OperationKind.REJECTED) return false;

        if (result.kind() == PotionMixingRules.OperationKind.MIX
                && ResearchRewards.level(player, "v3.research.68", "potion_mixing") <= 0) {
            return false;
        }
        if (result.kind() == PotionMixingRules.OperationKind.CONCENTRATE
                && ResearchRewards.level(player, "v3.research.70", "potion_concentration") <= 0) {
            return false;
        }

        PotionContents contents = PotionContents.EMPTY;
        for (PotionMixingRules.EffectSpec spec : result.effects()) {
            var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .get(net.minecraft.resources.Identifier.parse(spec.effectId())).orElse(null);
            if (holder == null) return false;
            contents = contents.withEffectAdded(new MobEffectInstance(
                    holder, spec.durationTicks(), spec.amplifier()));
        }

        ItemStack out = new ItemStack(stackA.getItem());
        out.set(DataComponents.POTION_CONTENTS, contents);
        if (result.kind() == PotionMixingRules.OperationKind.CONCENTRATE || concA || concB) {
            net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, out,
                    tag -> tag.putBoolean("MoreMinecraftsConcentrated", true));
        }

        stackA.shrink(1);
        stackB.shrink(1);
        if (stackA.isEmpty()) {
            stand.setItem(0, out);
        } else if (stand.getItem(2).isEmpty()) {
            stand.setItem(2, out);
        } else if (!player.getInventory().add(out)) {
            player.drop(out, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        stand.setChanged();
        interacted(stand, player);
        return true;
    }

    /** `.69` 9+9 process automation: refills bottle/ingredient slots from inputQueue when idle and moves finished potions. */
    public static void tickAutoProcess(ServerLevel level, BrewingStandBlockEntity stand, int currentBrewTime) {
        if (level == null || currentBrewTime > 0 || !(stand instanceof BrewingResearchStateAccess access)) return;
        if (!access.moreminecrafts$isAutoProcessEnabled()) return;
        if (level(level, stand, "v3.research.69") <= 0) return;

        var queue = access.moreminecrafts$getInputQueue();
        var outputs = access.moreminecrafts$getOutputBuffer();
        boolean changed = false;

        // If ingredient slot is empty and bottle slots hold finished potions, move them to outputBuffer.
        if (stand.getItem(3).isEmpty()) {
            for (int slot = 0; slot < 3; slot++) {
                ItemStack bottle = stand.getItem(slot);
                if (bottle.isEmpty()) continue;
                PotionContents contents = bottle.get(DataComponents.POTION_CONTENTS);
                if (contents != null && contents.getAllEffects().iterator().hasNext()) {
                    for (int outIdx = 0; outIdx < outputs.size(); outIdx++) {
                        if (outputs.get(outIdx).isEmpty()) {
                            outputs.set(outIdx, bottle.copy());
                            stand.setItem(slot, ItemStack.EMPTY);
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }

        // Pull bottles or ingredients from inputQueue into empty stand slots.
        for (int q = 0; q < queue.size(); q++) {
            ItemStack queued = queue.get(q);
            if (queued.isEmpty()) continue;
            if (deliveryType(queued) != null) {
                for (int slot = 0; slot < 3; slot++) {
                    if (stand.getItem(slot).isEmpty()) {
                        stand.setItem(slot, queued.copyWithCount(1));
                        queued.shrink(1);
                        if (queued.isEmpty()) queue.set(q, ItemStack.EMPTY);
                        changed = true;
                        break;
                    }
                }
            } else if (queued.is(Items.BLAZE_POWDER) && stand.getItem(4).isEmpty()) {
                stand.setItem(4, queued.copy());
                queue.set(q, ItemStack.EMPTY);
                changed = true;
            } else if (stand.getItem(3).isEmpty()) {
                stand.setItem(3, queued.copy());
                queue.set(q, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) stand.setChanged();
    }

    private static String deliveryType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.is(Items.POTION)) return "drinkable";
        if (stack.is(Items.SPLASH_POTION)) return "splash";
        if (stack.is(Items.LINGERING_POTION)) return "lingering";
        return null;
    }

    private static boolean isConcentrated(ItemStack stack) {
        var custom = stack.getOrDefault(DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        return custom.copyTag().getBooleanOr("MoreMinecraftsConcentrated", false);
    }

    private static List<PotionMixingRules.EffectSpec> toEffectSpecs(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return List.of();
        List<PotionMixingRules.EffectSpec> specs = new java.util.ArrayList<>();
        for (MobEffectInstance eff : contents.getAllEffects()) {
            String id = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .getKey(eff.getEffect().value()));
            specs.add(new PotionMixingRules.EffectSpec(
                    id, eff.getDuration(), eff.getAmplifier(),
                    eff.getEffect().value().isInstantaneous(), 2));
        }
        return specs;
    }
}
