package dev.itzgamesgd.moreminecrafts.ability;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class AbilityAttachments {
    public static final AttachmentType<AbilityCooldowns> COOLDOWNS = AttachmentRegistry.create(
            ModIds.id("ability_cooldowns"),
            builder -> builder
                    .initializer(() -> AbilityCooldowns.EMPTY)
                    .persistent(AbilityCooldowns.CODEC)
                    .copyOnDeath()
    );

    private AbilityAttachments() {
    }

    public static void initialize() {
        // Class loading performs attachment registration.
    }
}
