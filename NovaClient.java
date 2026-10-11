package nova;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import nova.core.Config;
import nova.core.ModuleManager;
import nova.gui.SplashScreen;
import nova.modules.ClientModules;

public class NovaClient implements ClientModInitializer {
    private static boolean splashShown;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();
        ClientTickEvents.END_CLIENT_TICK.register(c -> { ModuleManager.tick(); showSplash(c); });
        HudRenderCallback.EVENT.register((ctx, tc) -> ModuleManager.hud(ctx));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());
    }

    /** Loading screen: once, as soon as the title screen is up and the game's own loading overlay is gone. */
    private static void showSplash(MinecraftClient c) {
        if (splashShown || c.getOverlay() != null || !(c.currentScreen instanceof TitleScreen)) return;
        splashShown = true;
        if (ClientModules.Gui.splashOn()) c.setScreen(new SplashScreen(c.currentScreen));
    }
}
