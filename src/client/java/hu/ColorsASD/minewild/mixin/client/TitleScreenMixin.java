package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.RestartRequiredScreen;
import hu.ColorsASD.minewild.installer.ModInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
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
    }
}
