package dev.itzgamesgd.moreminecrafts.mixin;
import java.util.Map;
import dev.itzgamesgd.moreminecrafts.client.T1AnvilScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MenuScreens.class)
abstract class T1AnvilScreenFactoryMixin {
    @Shadow @Final private static Map<MenuType<?>,MenuScreens.ScreenConstructor<?,?>> SCREENS;
    @Inject(method="<clinit>",at=@At("RETURN"))
    private static void moreminecrafts$anvilFactory(CallbackInfo ci){
        SCREENS.put(MenuType.ANVIL,(MenuScreens.ScreenConstructor<AnvilMenu,T1AnvilScreen>)T1AnvilScreen::new);
    }
}
