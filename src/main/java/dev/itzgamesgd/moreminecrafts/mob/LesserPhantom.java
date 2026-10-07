package dev.itzgamesgd.moreminecrafts.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;

/** Phantom variant whose approved damage multiplier survives vanilla size-derived rewrites. */
public final class LesserPhantom extends Phantom {
    private static final double ATTACK_MULTIPLIER = 0.72;

    public LesserPhantom(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
    }

    @Override
    public void setPhantomSize(int size) {
        super.setPhantomSize(size);
        var damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue((6.0 + getPhantomSize()) * ATTACK_MULTIPLIER);
        }
    }
}
