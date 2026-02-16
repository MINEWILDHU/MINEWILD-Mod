package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientPauseMenuLinks;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonWidget.class)
public class ButtonWidgetMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true, require = 0)
    private void minewild$openPauseMenuLinks(CallbackInfo ci) {
        if (ClientPauseMenuLinks.handleButtonPress((ButtonWidget) (Object) this)) {
            ci.cancel();
        }
    }
}
