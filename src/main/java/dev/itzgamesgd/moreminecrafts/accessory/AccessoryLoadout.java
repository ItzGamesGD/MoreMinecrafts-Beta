package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** In-memory five-slot category map with category and active-slot validation. */
public final class AccessoryLoadout {
    private final EnumMap<AccessoryCategory, AccessoryDefinition> equipped;

    private AccessoryLoadout(Map<AccessoryCategory, AccessoryDefinition> equipped) {
        this.equipped = new EnumMap<>(AccessoryCategory.class);
        this.equipped.putAll(equipped);
    }

    public static AccessoryLoadout empty() {
        return new AccessoryLoadout(Map.of());
    }

    /** Equips into the sole slot for this category; occupied slots are left unchanged. */
    public boolean equip(AccessoryDefinition accessory) {
        AccessoryCategory category = accessory.category();
        if (equipped.containsKey(category)) return false;
        equipped.put(category, accessory);
        return true;
    }

    public Optional<AccessoryDefinition> get(AccessoryCategory category) {
        return Optional.ofNullable(equipped.get(category));
    }

    public boolean canManuallyActivate(AccessoryDefinition accessory) {
        return accessory.category().allowsManualActivation()
                && equipped.get(accessory.category()) == accessory;
    }

    public int slotCount() {
        return AccessoryCategory.values().length;
    }

    public Map<AccessoryCategory, AccessoryDefinition> equipped() {
        return Map.copyOf(equipped);
    }
}
