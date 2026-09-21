package dev.itzgamesgd.moreminecrafts.core;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import net.minecraft.resources.Identifier;

public final class ModIds {
    private ModIds() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MoreMinecrafts.MOD_ID, path);
    }
}
