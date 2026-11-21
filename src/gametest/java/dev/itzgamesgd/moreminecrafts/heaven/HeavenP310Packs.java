package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/**
 * P3.10: registers the option-D pending pools (copies of heaven_worldgen_pending/, byte-identical, checked by HeavenP310ResourceTest) as
 * built-in DATA packs of the gametest mod, enabled by default ONLY for the themes named in {@code -Dmoreminecrafts.heaven.p310packs=meadow,forest}.
 * Without the property nothing is registered; production pools and data/ are never touched.
 */
public final class HeavenP310Packs implements ModInitializer {
    @Override
    public void onInitialize() {
        String themes = System.getProperty("moreminecrafts.heaven.p310packs");
        if (themes == null || themes.isBlank()) return;
        var mod = FabricLoader.getInstance().getModContainer("moreminecrafts-gametest").orElseThrow();
        for (String t : themes.split(",")) {
            boolean ok = ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath("moreminecrafts-gametest", "heaven_pending_" + t.trim()), mod, PackActivationType.DEFAULT_ENABLED);
            MoreMinecrafts.LOGGER.info("HEAVEN_P310_PACK heaven_pending_{} registered={}", t.trim(), ok);
        }
    }
}
