package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.progression.ResearchBatch2Runtime;
import dev.itzgamesgd.moreminecrafts.progression.ResearchEcologyAiRules;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies `.36` Zombie reinforcement suppression and `.42` Illager movement speed reduction via transient modifiers. */
@Mixin(Mob.class)
abstract class ResearchEcologyMobMixin {
    @Unique
    private static final Identifier ZOMBIE_REINFORCEMENT_ID = ModIds.id("research_zombie_reinforcement_reduction");
    @Unique
    private static final Identifier ILLAGER_SPEED_ID = ModIds.id("research_illager_speed_reduction");

    @Inject(method = "tick", at = @At("HEAD"))
    private void moreminecrafts$applyEcologyAttributeEffects(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (mob.level().isClientSide()) return;

        LivingEntity attributed = mob.getTarget() instanceof ServerPlayer ? mob.getTarget() : mob.getLastHurtByMob();
        if (mob instanceof Zombie) {
            AttributeInstance attr = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
            if (attr != null) {
                int level36 = ResearchBatch2Runtime.level(attributed, "v3.research.36");
                double reduction = ResearchEcologyAiRules.zombieReinforcementReduction(level36);
                AttributeModifier existing = attr.getModifier(ZOMBIE_REINFORCEMENT_ID);
                if (reduction > 0.0D) {
                    if (existing == null || Math.abs(existing.amount() + reduction) > 1.0E-6D) {
                        attr.removeModifier(ZOMBIE_REINFORCEMENT_ID);
                        attr.addTransientModifier(new AttributeModifier(
                                ZOMBIE_REINFORCEMENT_ID, -reduction, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }
                } else if (existing != null) {
                    attr.removeModifier(ZOMBIE_REINFORCEMENT_ID);
                }
            }
        }
        if (mob instanceof AbstractIllager) {
            AttributeInstance attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) {
                int level42 = ResearchBatch2Runtime.level(attributed, "v3.research.42");
                double delta = ResearchEcologyAiRules.illagerSpeedModifier(level42);
                AttributeModifier existing = attr.getModifier(ILLAGER_SPEED_ID);
                if (delta < 0.0D) {
                    if (existing == null) {
                        attr.addTransientModifier(new AttributeModifier(
                                ILLAGER_SPEED_ID, delta, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                    }
                } else if (existing != null) {
                    attr.removeModifier(ILLAGER_SPEED_ID);
                }
            }
        }
    }
}
