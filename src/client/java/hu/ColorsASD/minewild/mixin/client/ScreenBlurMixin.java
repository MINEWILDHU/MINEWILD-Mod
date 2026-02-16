package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import hu.ColorsASD.minewild.client.ClientExitOnDisconnect;
import hu.ColorsASD.minewild.client.RestartRequiredScreen;
import hu.ColorsASD.minewild.client.ShaderPreferenceScreen;
import hu.ColorsASD.minewild.client.ResourcePackAutoAccept;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenBlurMixin {
    @Inject(method = "method_57734", at = @At("HEAD"), cancellable = true, require = 0)
    private void minewild$skipRestartBlur(CallbackInfo ci) {
        if (ClientCompat.isMinecraft1215OrBelow() && ((Object) this instanceof RestartRequiredScreen
                || (Object) this instanceof ShaderPreferenceScreen)) {
            ci.cancel();
        }
    }

    @Inject(method = "method_57728", at = @At("HEAD"), cancellable = true, require = 0)
    private void minewild$skipPanorama(DrawContext context, float delta, CallbackInfo ci) {
        if ((Object) this instanceof RestartRequiredScreen || (Object) this instanceof ShaderPreferenceScreen) {
            ci.cancel();
        }
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void minewild$screenInitHooks(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ResourcePackAutoAccept.handleScreenInit(screen);
        ClientExitOnDisconnect.handleScreenInit(screen);
    }
}
