package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;

/**
 * 구름 슬라임 (Q15-Q18, D1, D6): Slime AI with size-dependent health 4 / 12 / 32 and +50 % attack on damaging sizes.
 *
 * <p>Splitting (large -> 2-4 medium, medium -> 2-4 small, small never) and "tiny slimes deal no damage" are the vanilla
 * AbstractCubeMob rules and are inherited unchanged; split children use {@code getType()} and so stay Cloud Slimes.
 * Slimes cannot be babies in 26.3 ({@code canBeABaby() == false}), which satisfies D1.
 */
public final class CloudSlime extends Slime {
    public CloudSlime(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        SkyMobTraits.applyFixedLoadout(this, ItemStack.EMPTY); // Q55
        return result;
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return false;
    }

    /** Q16: 4 / 12 / 32 HP for the small / medium / large sizes (1 / 2 / 4). */
    @Override
    protected void setCubeMobHealth(int size) {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(SkyMobTraits.cloudSlimeHealth(size));
    }

    /** Q16 +50 % attack for sizes that deal damage; D6: the tiny size keeps the vanilla value and still deals 0. */
    @Override
    public void setSize(int size, boolean updateHealth) {
        super.setSize(size, updateHealth);
        if (!isTiny()) {
            AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(attack.getBaseValue() * SkyMobTraits.CLOUD_SLIME_ATTACK_MULTIPLIER);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return SkyMobTraits.isImmuneDamage(source) || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }
}
