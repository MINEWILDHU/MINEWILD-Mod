package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.class_434")
public abstract class DownloadingTerrainScreenBackgroundMixin extends Screen {
    private static final int CONNECT_BACKGROUND_COLOR = 0xFF111114;

    protected DownloadingTerrainScreenBackgroundMixin(Text title) {
        super(title);
    }

    @Redirect(
            method = "method_25394(Lnet/minecraft/class_332;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_434;method_25434(Lnet/minecraft/class_332;)V"
            ),
            remap = false,
            require = 0
    )
    private void minewild$replaceTerrainBackground(DownloadingTerrainScreen instance, DrawContext context) {
        minewild$fillSolidBackground(context);
    }

    private void minewild$fillSolidBackground(DrawContext context) {
        if (context == null || this.width <= 0 || this.height <= 0) {
            return;
        }
        ClientCompat.disablePanorama();
        context.fill(0, 0, this.width, this.height, CONNECT_BACKGROUND_COLOR);
    }
}
