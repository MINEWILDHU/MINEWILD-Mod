package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import hu.ColorsASD.minewild.client.ClientExitOnDisconnect;
import hu.ColorsASD.minewild.client.ClientPauseMenuLinks;
import hu.ColorsASD.minewild.client.ResourcePackAutoAccept;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenBlurMixin {
    private static final int CONNECT_BACKGROUND_COLOR = 0xFF111114;

    @Inject(
            method = "method_25420(Lnet/minecraft/class_332;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$flatConnectBackgroundLegacy(DrawContext context, CallbackInfo ci) {
        if (!minewild$shouldForceBackground()) {
            return;
        }
        minewild$fillSolidBackground(context);
        ci.cancel();
    }

    @Inject(
            method = "method_25420(Lnet/minecraft/class_332;IIF)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$flatConnectBackgroundModern(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!minewild$shouldForceBackground()) {
            return;
        }
        minewild$fillSolidBackground(context);
        ci.cancel();
    }

    @Inject(
            method = "method_25434(Lnet/minecraft/class_332;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$flatTextureBackgroundLegacy(DrawContext context, CallbackInfo ci) {
        if (!minewild$shouldForceBackground()) {
            return;
        }
        minewild$fillSolidBackground(context);
        ci.cancel();
    }

    @Inject(
            method = "method_57728(Lnet/minecraft/class_332;F)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$blockPanoramaLegacy(DrawContext context, float delta, CallbackInfo ci) {
        if (!minewild$shouldForceBackground()) {
            return;
        }
        minewild$fillSolidBackground(context);
        ci.cancel();
    }

    @Inject(
            method = "method_57728(Lnet/minecraft/class_332;IIF)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$blockPanoramaModern(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!minewild$shouldForceBackground()) {
            return;
        }
        minewild$fillSolidBackground(context);
        ci.cancel();
    }

    @Inject(
            method = "method_57734()V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$skipPanoramaBlur(CallbackInfo ci) {
        if (!minewild$shouldForceBackground()) {
            return;
        }
        ClientCompat.disablePanorama();
        ci.cancel();
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void minewild$screenInitHooks(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        try {
            ResourcePackAutoAccept.handleScreenInit(screen);
        } catch (RuntimeException ignored) {
        }
        try {
            ClientExitOnDisconnect.handleScreenInit(screen);
        } catch (RuntimeException ignored) {
        }
        try {
            ClientPauseMenuLinks.handleScreenInit(screen);
        } catch (RuntimeException ignored) {
        }
    }

    private boolean minewild$shouldForceBackground() {
        Screen screen = (Screen) (Object) this;
        return ClientExitOnDisconnect.isShutdownRequested()
                || ClientCompat.isConnectScreen(screen)
                || screen instanceof DisconnectedScreen;
    }

    private void minewild$fillSolidBackground(DrawContext context) {
        Screen screen = (Screen) (Object) this;
        if (context == null || screen.width <= 0 || screen.height <= 0) {
            return;
        }
        ClientCompat.disablePanorama();
        context.fill(0, 0, screen.width, screen.height, CONNECT_BACKGROUND_COLOR);
    }
}
