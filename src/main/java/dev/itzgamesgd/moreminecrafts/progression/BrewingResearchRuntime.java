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
}
