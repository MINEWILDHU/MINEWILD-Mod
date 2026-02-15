package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientExitOnDisconnect;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {
    @Inject(method = "disconnect", at = @At("HEAD"), require = 0)
    private void minewild$exitAfterDisconnect(CallbackInfo ci) {
        ClientExitOnDisconnect.request();
    }
}
