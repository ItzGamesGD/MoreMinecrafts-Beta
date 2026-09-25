package dev.itzgamesgd.moreminecrafts.fishing;

import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class FishItem extends Item {
    private final FishSpecies species;
    public FishItem(FishSpecies species, Properties properties) { super(properties); this.species=species; }
    public FishSpecies species() { return species; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> text, TooltipFlag flag) {
        int rarity = Math.max(1, FishingLogic.LIVE_TEST_SPECIES.indexOf(species) + 1);
        int grade = stack.getOrDefault(ModDataComponents.FISH_GRADE, 1);
        int length = stack.getOrDefault(ModDataComponents.FISH_LENGTH, species.minLengthCm());
        text.accept(Component.literal("Species: " + species.id()));
        text.accept(Component.literal("Rarity: " + FishingGrade.roman(Math.min(3, rarity))));
        text.accept(Component.literal("Grade: " + FishingGrade.roman(Math.min(3, grade))));
        text.accept(Component.literal("Length: " + length + " cm"));
        super.appendHoverText(stack, context, display, text, flag);
    }
}
