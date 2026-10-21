package dev.itzgamesgd.moreminecrafts.accessory;

import net.minecraft.world.item.Item;

/** A registered accessory with immutable category metadata. */
public final class AccessoryItem extends Item {
    private final AccessoryDefinition definition;

    public AccessoryItem(AccessoryDefinition definition, Properties properties) {
        super(properties);
        this.definition = definition;
    }

    public AccessoryDefinition definition() { return definition; }
    public AccessoryCategory category() { return definition.category(); }

}
