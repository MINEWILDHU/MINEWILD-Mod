package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import hu.ColorsASD.minewild.client.ClientExitOnDisconnect;
import hu.ColorsASD.minewild.client.OwnModUpdateScreen;
import hu.ColorsASD.minewild.client.OutdatedModsScreen;
import hu.ColorsASD.minewild.client.OutdatedShaderScreen;
import hu.ColorsASD.minewild.client.RestartRequiredScreen;
import hu.ColorsASD.minewild.client.ShaderPreferenceScreen;
import hu.ColorsASD.minewild.installer.ModInstaller;
import hu.ColorsASD.minewild.installer.OwnModUpdater;
import hu.ColorsASD.minewild.installer.ShaderPackInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final int EXIT_BACKGROUND_COLOR = 0xFF111114;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void minewild$init(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (OwnModUpdater.hasUpdateAvailable()) {
            if (!(client.currentScreen instanceof OwnModUpdateScreen)) {
                client.setScreen(new OwnModUpdateScreen());
            }
            ci.cancel();
            return;
        }
        if (ModInstaller.isRestartRequired()) {
            minewild$openRequiredScreen(client);
            ci.cancel();
            return;
        }
        if (ShaderPackInstaller.hasOutdatedShaderDetected()) {
            if (!(client.currentScreen instanceof OutdatedShaderScreen)) {
                client.setScreen(new OutdatedShaderScreen());
            }
            ci.cancel();
            return;
        }
        if (!ShaderPackInstaller.hasUserPreference()) {
            if (!(client.currentScreen instanceof ShaderPreferenceScreen)) {
                client.setScreen(new ShaderPreferenceScreen());
            }
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void minewild$showRequiredScreenWhenReady(CallbackInfo ci) {
        if (OwnModUpdater.hasUpdateAvailable()) {
            if (!(MinecraftClient.getInstance().currentScreen instanceof OwnModUpdateScreen)) {
                MinecraftClient.getInstance().setScreen(new OwnModUpdateScreen());
            }
            return;
        }
        if (!ModInstaller.isRestartRequired()) {
            if (!ShaderPackInstaller.hasOutdatedShaderDetected()) {
                return;
            }
            if (!(MinecraftClient.getInstance().currentScreen instanceof OutdatedShaderScreen)) {
                MinecraftClient.getInstance().setScreen(new OutdatedShaderScreen());
            }
            return;
        }
        minewild$openRequiredScreen(MinecraftClient.getInstance());
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void minewild$hidePanoramaDuringShutdown(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!ClientExitOnDisconnect.isShutdownRequested()) {
            return;
        }
        if (context != null && this.width > 0 && this.height > 0) {
            context.fill(0, 0, this.width, this.height, EXIT_BACKGROUND_COLOR);
        }
        ClientCompat.disablePanorama();
        ci.cancel();
    }

    private void minewild$openRequiredScreen(MinecraftClient client) {
        if (client == null) {
            return;
        }
        if (ModInstaller.hasOutdatedModsDetected() && !ModInstaller.hasExtraModsDetected()) {
            if (!(client.currentScreen instanceof OutdatedModsScreen)) {
                client.setScreen(new OutdatedModsScreen());
            }
            return;
        }
        if (!(client.currentScreen instanceof RestartRequiredScreen)) {
            client.setScreen(new RestartRequiredScreen());
        }
    }
}
