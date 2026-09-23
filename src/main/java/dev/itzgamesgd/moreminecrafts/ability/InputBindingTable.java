package dev.itzgamesgd.moreminecrafts.ability;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Package-sized data structure separated from Minecraft bootstrap so binding behavior is testable. */
public final class InputBindingTable<K, V> {
    private final Map<K, EnumMap<EquipmentSkillInput, V>> bindings = new IdentityHashMap<>();

    public void bind(K key, EquipmentSkillInput input, V value) {
        bindings.computeIfAbsent(key, ignored -> new EnumMap<>(EquipmentSkillInput.class)).put(input, value);
    }

    public V resolve(K key, EquipmentSkillInput input) {
        if (key == null || input == null) return null;
        var inputs = bindings.get(key);
        return inputs == null ? null : inputs.get(input);
    }
}
