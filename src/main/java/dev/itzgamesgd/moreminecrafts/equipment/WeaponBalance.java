package dev.itzgamesgd.moreminecrafts.equipment;

import java.util.List;

/** Final implementation points chosen inside the approved basic-weapon balance windows. */
public final class WeaponBalance {
    public enum Tier { WOOD, STONE, COPPER, IRON, GOLD, DIAMOND, NETHERITE }
    public record Values(Tier tier, float swordDamage, float daggerDamage, float greatswordDamage) {}

    public static final float DAGGER_ATTACK_SPEED = 2.0F;
    public static final float GREATSWORD_ATTACK_SPEED = 0.8F;
    public static final float GAUNTLET_ATTACK_SPEED = 2.4F;
    public static final List<Values> VALUES = List.of(
            new Values(Tier.WOOD, 4, 2.5F, 5.5F),
            new Values(Tier.STONE, 5, 3.5F, 7),
            new Values(Tier.COPPER, 5, 3.5F, 7),
            new Values(Tier.IRON, 6, 4, 8.5F),
            new Values(Tier.GOLD, 4, 2.5F, 5.5F),
            new Values(Tier.DIAMOND, 7, 4.5F, 10),
            new Values(Tier.NETHERITE, 8, 5, 11)
    );

    private WeaponBalance() {}

    public static Values values(Tier tier) {
        return VALUES.stream().filter(value -> value.tier() == tier).findFirst().orElseThrow();
    }
}
