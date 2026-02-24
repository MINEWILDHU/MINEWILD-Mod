package hu.ColorsASD.minewild.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.gui.updater.UpdateModScreen",
        "com.seibel.distanthorizons.common.wrappers.gui.updater.UpdateModScreen"
})
public abstract class DistantHorizonsUpdateScreenMixin extends Screen {
    protected DistantHorizonsUpdateScreenMixin(Text title) {
        super(title);
    }

    @Inject(
            method = "method_25426()V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$skipDhUpdatePopup(CallbackInfo ci) {
        this.close();
        ci.cancel();
    }
}
