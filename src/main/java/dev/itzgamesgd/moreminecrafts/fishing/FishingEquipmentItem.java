package dev.itzgamesgd.moreminecrafts.fishing;

import net.minecraft.world.item.Item;

public final class FishingEquipmentItem extends Item {
    private final FishingEquipmentFamily family;
    private final int grade;

    public FishingEquipmentItem(FishingEquipmentFamily family, int grade, Properties properties) {
        super(properties);
        if (grade < 1 || grade > 5) throw new IllegalArgumentException("Fishing equipment grade must be 1-5");
        this.family = family;
        this.grade = grade;
    }

    public FishingEquipmentFamily family() { return family; }
    public int grade() { return grade; }
}
