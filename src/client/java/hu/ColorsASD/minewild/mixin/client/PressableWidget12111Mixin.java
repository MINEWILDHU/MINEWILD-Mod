package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.MinewildButtonWidgets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class PressableWidget12111Mixin {
    @Inject(
            method = "method_75794(Lnet/minecraft/class_332;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void minewild$drawStyled12111Texture(DrawContext context, CallbackInfo ci) {
        Object self = this;
        if (self instanceof ButtonWidget button && MinewildButtonWidgets.drawStyled12111Texture(button, context)) {
            ci.cancel();
        }
    }
}
