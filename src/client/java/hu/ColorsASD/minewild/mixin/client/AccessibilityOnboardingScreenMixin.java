package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessibilityOnboardingScreen.class)
public class AccessibilityOnboardingScreenMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void minewild$skipOnboarding(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        GameOptions options = client.options;
        boolean changed = false;
        if (ClientCompat.setOnboardAccessibility(options, true)) {
            changed = true;
        }
        if (ClientCompat.setSkipMultiplayerWarning(options, true)) {
            changed = true;
        }
        if (changed) {
            options.write();
        }

        client.setScreen(new TitleScreen());
        ci.cancel();
    }
}
