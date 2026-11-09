package dev.itzgamesgd.moreminecrafts.furnace;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.ResearchProgressLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Server-authoritative rules for permanent furnace entities. */
public final class FurnaceResearchRuntime {
    private static final TagKey<Item> MINERAL_INPUTS = TagKey.create(Registries.ITEM,
            ModIds.id("research/mineral_smelting_inputs"));
    private static final TagKey<Item> MINERAL_OUTPUTS = TagKey.create(Registries.ITEM,
            ModIds.id("research/mineral_smelting_outputs"));

    private FurnaceResearchRuntime() {}

    public static int level(ServerLevel level, AbstractFurnaceBlockEntity furnace, String researchId) {
        if (!(furnace instanceof FurnaceResearchStateAccess access)) return 0;
        return access.moreminecrafts$getResearchState().attributedPlayer()
                .map(playerId -> ResearchProgressLookup.researchLevel(level.getServer(), playerId, researchId))
                .orElse(0);
    }

    public static int fuelDuration(ServerLevel level, AbstractFurnaceBlockEntity furnace, int baseTicks) {
        int purchased = level(level, furnace, "v3.research.06");
        if (purchased == 0 || baseTicks <= 0 || !(furnace instanceof FurnaceResearchStateAccess access))
            return baseTicks;
        FurnaceResearchState state = access.moreminecrafts$getResearchState();
        var result = FurnaceResearchRules.fuelDuration(baseTicks, purchased, state.fuelSavingRemainder());
        if (result.remainder() != state.fuelSavingRemainder())
            access.moreminecrafts$setResearchState(state.withFuelRemainder(result.remainder()));
        return result.burnTicks();
    }

    public static int cookingTimer(ServerLevel level, AbstractFurnaceBlockEntity furnace,
                                   int vanillaTimer, int totalTime) {
        if (!(furnace instanceof FurnaceResearchStateAccess access)) return vanillaTimer;
        FurnaceResearchState state = access.moreminecrafts$getResearchState();
        int purchased = level(level, furnace, "v3.research.11");
        if (purchased == 0 && state.cookingCredit() == 0) return vanillaTimer;
        var result = FurnaceResearchRules.cookingTick(vanillaTimer, totalTime, purchased,
                state.cookingProgressRemainder(), state.cookingCredit());
        if (result.remainder() != state.cookingProgressRemainder()
                || result.credit() != state.cookingCredit())
            access.moreminecrafts$setResearchState(state.withCooking(result.remainder(), result.credit()));
        return result.timer();
    }

    public static boolean eligibleMineral(AbstractFurnaceBlockEntity furnace, ItemStack ingredient,
                                           ItemStack result) {
        return eligibleMineral(ingredient, result);
    }

    /** Shared recipe-context eligibility for permanent and item-backed furnace processing. */
    public static boolean eligibleMineral(ItemStack ingredient, ItemStack result) {
        return !ingredient.isEmpty() && !result.isEmpty()
                && ingredient.is(MINERAL_INPUTS) && result.is(MINERAL_OUTPUTS);
    }

    public static boolean canAcceptPossibleBonus(ServerLevel level, AbstractFurnaceBlockEntity furnace,
                                                  NonNullList<ItemStack> items, ItemStack result,
                                                  int maxStack) {
        if (level(level, furnace, "v3.research.07") == 0
                || !eligibleMineral(furnace, items.get(0), result)) return true;
        ItemStack output = items.get(2);
        return FurnaceResearchRules.fitsPossibleBonus(output.getCount(), result.getCount(),
                Math.min(maxStack, result.getMaxStackSize()));
    }

    public static ItemStack completedOutput(ServerLevel level, AbstractFurnaceBlockEntity furnace,
                                            ItemStack ingredient, ItemStack result) {
        int purchased = level(level, furnace, "v3.research.07");
        if (purchased == 0 || !eligibleMineral(furnace, ingredient, result)) return result;
        if (!FurnaceResearchRules.precisionBonus(purchased, level.getRandom().nextInt(10000))) return result;
        return result.copyWithCount(result.getCount() + 1);
    }
}
