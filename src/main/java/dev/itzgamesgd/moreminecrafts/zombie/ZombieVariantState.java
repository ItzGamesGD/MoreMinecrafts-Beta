package dev.itzgamesgd.moreminecrafts.zombie;

public interface ZombieVariantState {
    ZombieVariant moreminecrafts$getVariant();

    void moreminecrafts$setVariant(ZombieVariant variant);

    boolean moreminecrafts$hasLeapFallImmunity(long gameTick);
}
