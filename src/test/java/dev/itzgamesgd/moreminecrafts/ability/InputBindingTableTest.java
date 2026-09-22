package dev.itzgamesgd.moreminecrafts.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputBindingTableTest {
    @Test
    void resolvesSeparateInputsAndSafelyMisses() {
        var table = new InputBindingTable<Object, String>();
        Object windSword = new Object();
        table.bind(windSword, EquipmentSkillInput.PRIMARY, "wind_blade");
        table.bind(windSword, EquipmentSkillInput.SECONDARY, "wind_step");

        assertEquals("wind_blade", table.resolve(windSword, EquipmentSkillInput.PRIMARY));
        assertEquals("wind_step", table.resolve(windSword, EquipmentSkillInput.SECONDARY));
        assertNull(table.resolve(new Object(), EquipmentSkillInput.PRIMARY));
        assertNull(table.resolve(windSword, null));
    }

    @Test
    void networkIdsRejectUnexpectedValues() {
        assertSame(EquipmentSkillInput.PRIMARY, EquipmentSkillInput.fromNetworkId(0));
        assertSame(EquipmentSkillInput.SECONDARY, EquipmentSkillInput.fromNetworkId(1));
        assertNull(EquipmentSkillInput.fromNetworkId(-1));
        assertNull(EquipmentSkillInput.fromNetworkId(2));
    }
}
