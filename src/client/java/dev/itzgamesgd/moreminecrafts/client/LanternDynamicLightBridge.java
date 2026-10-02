package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.equipment.EquipmentAttachments;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;

/** Optional, block-free LambDynamicLights bridge supporting its handler-registration API. */
public final class LanternDynamicLightBridge {
    private LanternDynamicLightBridge() {}

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded("lambdynlights")) {
            MoreMinecrafts.LOGGER.info("LambDynamicLights not installed; Miner Lantern dynamic rendering is dormant.");
            return;
        }
        try {
            Class<?> handler = Class.forName("dev.lambdaurora.lambdynlights.api.DynamicLightHandler");
            Object implementation = Proxy.newProxyInstance(handler.getClassLoader(), new Class<?>[]{handler},
                    (proxy, method, args) -> {
                        if (method.getReturnType() == int.class && args != null && args.length > 0
                                && args[0] instanceof Player player)
                            return player.getAttachedOrElse(EquipmentAttachments.LANTERN,
                                    net.minecraft.world.item.ItemStack.EMPTY).is(ModItems.MINER_LANTERN) ? 15 : 0;
                        if (method.getReturnType() == boolean.class) return true;
                        if (method.getReturnType() == int.class) return 0;
                        return null;
                    });
            for (var method : handler.getMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (Modifier.isStatic(method.getModifiers()) && parameters.length == 2
                        && parameters[0].isAssignableFrom(EntityType.class) && parameters[1] == handler) {
                    method.invoke(null, EntityTypes.PLAYER, implementation);
                    MoreMinecrafts.LOGGER.info("Miner Lantern registered with LambDynamicLights handler API.");
                    return;
                }
            }
            throw new NoSuchMethodException("No compatible DynamicLightHandler registration method");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Installed LambDynamicLights does not expose a compatible handler API", exception);
        }
    }
}
