package dev.itzgamesgd.moreminecrafts.client.skyflyer;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.resources.Identifier;

/**
 * The only place that names the Sky Flyer entity textures. Both currently point at the vanilla Allay and Phantom
 * textures (design: reuse vanilla model and texture first, swap the resource later). To give an entity its own skin,
 * add {@code assets/moreminecrafts/textures/entity/<id>.png} and change the matching constant to {@code OWN_*}; no
 * renderer or model code has to change.
 */
public final class SkyFlyerTextures {
    private SkyFlyerTextures() {}

    /** Own-texture slots, unused until art exists. */
    public static final Identifier OWN_SKY_SPIRIT = ModIds.id("textures/entity/sky_spirit.png");
    public static final Identifier OWN_STORM_HUNTER = ModIds.id("textures/entity/storm_hunter.png");

    /** Active textures (vanilla reuse). */
    public static final Identifier SKY_SPIRIT = Identifier.withDefaultNamespace("textures/entity/allay/allay.png");
    public static final Identifier STORM_HUNTER = Identifier.withDefaultNamespace("textures/entity/phantom/phantom.png");
}
