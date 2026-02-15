package hu.ColorsASD.minewild.mixin.client;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class ClientBrandRetrieverMixin {
    private static final String CLIENT_BRAND = "minewild";

    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true)
    private static void minewild$overrideClientBrand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(CLIENT_BRAND);
    }
}
