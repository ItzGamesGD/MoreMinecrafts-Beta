package dev.itzgamesgd.moreminecrafts.core;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.minecraft.resources.ResourceLocation;

public final class ModIds {
    private ModIds() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MoreMinecrafts.MOD_ID, path);
    }
}
