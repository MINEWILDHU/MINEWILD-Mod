package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_3928")
public abstract class LevelLoadingScreenBackgroundMixin extends Screen {
    private static final int CONNECT_BACKGROUND_COLOR = 0xFF111114;

    protected LevelLoadingScreenBackgroundMixin(Text title) {
        super(title);
    }

    @Inject(
            method = "method_25394(Lnet/minecraft/class_332;IIF)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void minewild$fillBeforeLevelLoadingRender(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        if (!ClientCompat.isMinecraft1219Through12111()) {
            return;
        }
        minewild$fillSolidBackground(context);
    }

    @Inject(
            method = "method_25420(Lnet/minecraft/class_332;IIF)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$replaceLevelLoadingBackground(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        if (!ClientCompat.isMinecraft1219Through12111()) {
            return;
        }
        minewild$fillSolidBackground(context);
        ci.cancel();
    }

    private void minewild$fillSolidBackground(DrawContext context) {
        if (context == null) {
            return;
        }
        int targetWidth = this.width;
        int targetHeight = this.height;
        if (targetWidth <= 0 || targetHeight <= 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                targetWidth = client.getWindow().getScaledWidth();
                targetHeight = client.getWindow().getScaledHeight();
            }
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        ClientCompat.disablePanorama();
        context.fill(0, 0, targetWidth, targetHeight, CONNECT_BACKGROUND_COLOR);
    }
}
