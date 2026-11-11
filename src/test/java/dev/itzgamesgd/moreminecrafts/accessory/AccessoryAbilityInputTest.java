package dev.itzgamesgd.moreminecrafts.accessory;

import net.minecraft.world.entity.player.Input;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessoryAbilityInputTest {
    @Test
    void onlyDirectionalKeysCountAsHorizontalInput() {
        assertFalse(AccessoryAbilities.hasHorizontalInput(Input.EMPTY));
        assertFalse(AccessoryAbilities.hasHorizontalInput(new Input(false, false, false, false, true, true, true)),
                "jump, crouch and sprint alone are not horizontal movement input");
        assertTrue(AccessoryAbilities.hasHorizontalInput(new Input(true, false, false, false, false, true, false)));
        assertTrue(AccessoryAbilities.hasHorizontalInput(new Input(false, true, false, false, false, true, false)));
        assertTrue(AccessoryAbilities.hasHorizontalInput(new Input(false, false, true, false, false, true, false)));
        assertTrue(AccessoryAbilities.hasHorizontalInput(new Input(false, false, false, true, false, true, false)));
    }
}
