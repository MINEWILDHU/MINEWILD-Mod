package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import hu.ColorsASD.minewild.client.ClientExitOnDisconnect;
import hu.ColorsASD.minewild.client.RestartRequiredScreen;
import hu.ColorsASD.minewild.client.ShaderPreferenceScreen;
import hu.ColorsASD.minewild.installer.ModInstaller;
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
        if (ModInstaller.isRestartRequired()) {
            if (!(client.currentScreen instanceof RestartRequiredScreen)) {
                client.setScreen(new RestartRequiredScreen());
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
}
