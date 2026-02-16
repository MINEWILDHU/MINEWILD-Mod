package hu.ColorsASD.minewild.client;

import hu.ColorsASD.minewild.installer.ModInstaller;
import hu.ColorsASD.minewild.installer.ShaderPackInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;

public final class AutoConnectController {
    private static final String MINEWILD_ADDRESS = "play.minewild.hu";
    private static final String MINEWILD_NAME = "MINEWILD";
    private static final int FALLBACK_DELAY_TICKS = 60;
    private static boolean attempted;
    private static int titleTicks;

    private AutoConnectController() {
    }

    public static void tick(MinecraftClient client) {
        if (attempted || client == null) {
            return;
        }
        if (ModInstaller.isRestartRequired()) {
            titleTicks = 0;
            return;
        }
        if (!ShaderPackInstaller.hasUserPreference()) {
            titleTicks = 0;
            return;
        }
        if (client.world != null || client.getNetworkHandler() != null) {
            titleTicks = 0;
            return;
        }
        if (ClientCompat.isAccessibilityOnboardingScreen(client.currentScreen)) {
            connectNow(client);
            return;
        }
        if (!(client.currentScreen instanceof TitleScreen)) {
            titleTicks = 0;
            return;
        }
        ClientCompat.AutoConnectReadiness readiness = ClientCompat.getAutoConnectReadiness(client);
        if (readiness == ClientCompat.AutoConnectReadiness.NOT_READY) {
            titleTicks = 0;
            return;
        }
        if (readiness == ClientCompat.AutoConnectReadiness.UNKNOWN) {
            titleTicks++;
            if (titleTicks < FALLBACK_DELAY_TICKS) {
                return;
            }
        }
        connectNow(client);
    }

    public static boolean connectNow(MinecraftClient client) {
        if (client == null || attempted) {
            return false;
        }
        if (!ShaderPackInstaller.hasUserPreference()) {
            return false;
        }
        Screen parent = client.currentScreen;
        if (!ClientCompat.connectToServer(parent, client, MINEWILD_NAME, MINEWILD_ADDRESS)) {
            return false;
        }
        attempted = true;
        return true;
    }

}
