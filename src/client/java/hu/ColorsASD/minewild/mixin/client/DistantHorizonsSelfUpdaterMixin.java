package hu.ColorsASD.minewild.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.jar.updater.SelfUpdater")
public class DistantHorizonsSelfUpdaterMixin {
    @Inject(
            method = "onStart()Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void minewild$disableDhAutoUpdateCheck(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
