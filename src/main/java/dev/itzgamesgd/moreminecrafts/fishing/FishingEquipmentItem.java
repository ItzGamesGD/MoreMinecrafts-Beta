package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.world.item.Item;

public final class FishingEquipmentItem extends Item {
    private final FishingEquipmentFamily family;
    private final int tier;

    public FishingEquipmentItem(FishingEquipmentFamily family, int tier, Properties properties) {
        super(properties);
        this.family = family;
        this.tier = FishingEquipmentTier.requireValid(tier);
    }

    public FishingEquipmentFamily family() { return family; }
    public int tier() { return tier; }
}
