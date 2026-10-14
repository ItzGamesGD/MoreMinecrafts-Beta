package dev.itzgamesgd.moreminecrafts.equipment;

import net.minecraft.world.item.Item;

public class BasicWeaponItem extends Item {
    public enum Kind { DAGGER, GREATSWORD, GAUNTLET }
    private final Kind kind;
    public BasicWeaponItem(Kind kind, Properties properties) { super(properties); this.kind = kind; }
    public Kind kind() { return kind; }
}
