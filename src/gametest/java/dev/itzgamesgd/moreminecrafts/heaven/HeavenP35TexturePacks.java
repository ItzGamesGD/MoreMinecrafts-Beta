package dev.itzgamesgd.moreminecrafts.heaven;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/**
 * P3.5-D: registers the flower texture proposals T1 / T2 / T3 (src/gametest/resources/resourcepacks/heaven_tex_t*) as
 * built-in resource packs of the gametest mod, OFF by default, and only under {@code -Dmoreminecrafts.heaven.p35capture=true}.
 * Production textures are untouched; nothing outside the opt-in client capture ever enables these packs.
 */
public final class HeavenP35TexturePacks implements ModInitializer {
    static final String GATE = "moreminecrafts.heaven.p35capture";

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(GATE)) return;
        var mod = FabricLoader.getInstance().getModContainer("moreminecrafts-gametest").orElseThrow();
        for (int t = 1; t <= 3; t++) {
            boolean ok = ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath("moreminecrafts-gametest", "heaven_tex_t" + t), mod, PackActivationType.NORMAL);
            MoreMinecrafts.LOGGER.info("HEAVEN_P35_PACK heaven_tex_t{} registered={}", t, ok);
        }
    }
}
