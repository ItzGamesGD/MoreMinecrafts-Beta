package dev.itzgamesgd.moreminecrafts.registry;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.fishing.FishingRodMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

public final class ModMenuTypes {
    public static final MenuType<FishingRodMenu> FISHING_ROD = Registry.register(
            BuiltInRegistries.MENU, ModIds.id("fishing_rod"),
            new MenuType<>(FishingRodMenu::new, FeatureFlagSet.of()));

    public static final MenuType<dev.itzgamesgd.moreminecrafts.progression.EnchantingProgressionMenu> ENCHANTING_PROGRESSION = Registry.register(
            BuiltInRegistries.MENU, ModIds.id("enchanting_progression"),
            new MenuType<>(dev.itzgamesgd.moreminecrafts.progression.EnchantingProgressionMenu::new, FeatureFlagSet.of()));
    private ModMenuTypes() {}

    public static void initialize() {}
}
