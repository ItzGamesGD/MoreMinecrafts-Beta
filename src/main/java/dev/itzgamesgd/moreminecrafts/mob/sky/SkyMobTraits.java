package dev.itzgamesgd.moreminecrafts.mob.sky;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Shared, side-effect-free rules for the five Sky Realm normal mobs (SKY-MOB Issue #230 Q01-Q87, P1 D1-D6).
 * Every constant here is an owner-approved value; nothing in this class is a new design decision.
 */
public final class SkyMobTraits {
    /** Q23 / Q76: Fallen Sentinel melee hit applies Weakness I with this chance, on every difficulty. */
    public static final float SENTINEL_WEAKNESS_CHANCE = 0.40F;
    /** Q21 / Q23: Weakness I lasts 3 seconds. */
    public static final int SENTINEL_WEAKNESS_TICKS = 60;
    /** Q08: Skyguard Archer deals 40 % more base arrow damage than a vanilla Skeleton. */
    public static final double ARCHER_ARROW_DAMAGE_MULTIPLIER = 1.4D;
    /** Q25: Crystal Bug is 2.5x the size of a vanilla Silverfish. */
    public static final double CRYSTAL_BUG_SCALE = 2.5D;
    /** Q16: damaging Cloud Slime sizes deal 50 % more than the matching vanilla slime. */
    public static final double CLOUD_SLIME_ATTACK_MULTIPLIER = 1.5D;

    private SkyMobTraits() {}

    /** Q69 (lightning) and Q68 (powder-snow freezing). Fire/lava/burning (Q49, Q65) use EntityType.fireImmune(). */
    public static boolean isImmuneDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_LIGHTNING) || source.is(DamageTypeTags.IS_FREEZING);
    }

    /** Pure roll used by the Fallen Sentinel so the probability can be verified with a seeded source. */
    public static boolean rollWeakness(RandomSource random) {
        return random.nextFloat() < SENTINEL_WEAKNESS_CHANCE;
    }

    /** Q16: Cloud Slime max health per size. Sizes outside the three vanilla spawn sizes keep vanilla size^2. */
    public static int cloudSlimeHealth(int size) {
        return switch (size) {
            case 1 -> 4;
            case 2 -> 12;
            case 4 -> 32;
            default -> size * size;
        };
    }

    /**
     * Q55: no random equipment, no equipment pickup, no extra equipment drops. {@code keep} (may be empty) is the
     * only item left in the main hand, e.g. the Skyguard Archer's fixed bow.
     */
    public static void applyFixedLoadout(Mob mob, ItemStack keep) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setItemSlot(slot, slot == EquipmentSlot.MAINHAND ? keep.copy() : ItemStack.EMPTY);
            mob.setDropChance(slot, 0.0F);
        }
        mob.setCanPickUpLoot(false);
    }
}
