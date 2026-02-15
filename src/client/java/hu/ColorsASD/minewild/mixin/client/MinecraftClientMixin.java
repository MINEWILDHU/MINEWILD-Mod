package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.AutoConnectController;
import hu.ColorsASD.minewild.client.ClientExitOnDisconnect;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    private static final String WINDOW_TITLE = "MINEWILD";

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void minewild$windowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(WINDOW_TITLE);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void minewild$autoConnect(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        AutoConnectController.tick(client);
        ClientExitOnDisconnect.handleTick(client);
    }
}
