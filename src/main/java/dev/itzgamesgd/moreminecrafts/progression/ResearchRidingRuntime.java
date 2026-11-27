package dev.itzgamesgd.moreminecrafts.progression;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;

/** Transient mounted bonuses preserve the horse's own bred/spawned base attributes. */
public final class ResearchRidingRuntime {
    public static final Identifier SPEED_ID = ModIds.id("research_riding_speed");
    public static final Identifier JUMP_ID = ModIds.id("research_riding_jump");
    public static final Identifier HEALTH_ID = ModIds.id("research_riding_health");

    private ResearchRidingRuntime() {}

    public static void update(Horse horse) {
        if (horse.level().isClientSide()) return;
        int level = horse.getControllingPassenger() instanceof ServerPlayer rider
                ? ResearchBatch2Runtime.level(rider, "v3.research.50") : 0;
        setBonus(horse, Attributes.MOVEMENT_SPEED, SPEED_ID, ResearchRemainingRules.horseSpeedBonus(level));
        setBonus(horse, Attributes.JUMP_STRENGTH, JUMP_ID, ResearchRemainingRules.horseJumpBonus(level));
        setBonus(horse, Attributes.MAX_HEALTH, HEALTH_ID, ResearchRemainingRules.horseHealthBonus(level));
    }

    private static void setBonus(Horse horse, Holder<Attribute> attribute, Identifier id, double amount) {
        var instance = horse.getAttribute(attribute);
        if (instance == null) return;
        var current = instance.getModifier(id);
        if (current != null && (amount == 0.0D || current.amount() != amount)) instance.removeModifier(id);
        if (amount > 0.0D && (current == null || current.amount() != amount))
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }
}
