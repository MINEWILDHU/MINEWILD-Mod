package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_412")
public abstract class ConnectScreenBackgroundMixin extends Screen {
    private static final int CONNECT_BACKGROUND_COLOR = 0xFF111114;
    private static final String KEY_CANCEL = "gui.cancel";
    private static final String EXIT_LABEL = "Kilépés";
    private static final Identifier BUTTON_NORMAL = ClientCompat.id("minewild", "textures/gui/widget/button.png");
    private static final Identifier BUTTON_HIGHLIGHTED = ClientCompat.id("minewild", "textures/gui/widget/button_highlighted.png");
    private static final Identifier BUTTON_DISABLED = ClientCompat.id("minewild", "textures/gui/widget/button_disabled.png");

    protected ConnectScreenBackgroundMixin(Text title) {
        super(title);
    }

    @Redirect(
            method = "method_25394(Lnet/minecraft/class_332;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_412;method_25420(Lnet/minecraft/class_332;)V"
            ),
            remap = false,
            require = 0
    )
    private void minewild$replaceConnectBackground(ConnectScreen instance, DrawContext context) {
        minewild$fillSolidBackground(context);
    }

    @Inject(
            method = "method_25394(Lnet/minecraft/class_332;IIF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_332;method_27534(Lnet/minecraft/class_327;Lnet/minecraft/class_2561;III)V",
                    shift = At.Shift.BEFORE
            ),
            remap = false,
            require = 0
    )
    private void minewild$forceConnectBackgroundBeforeText(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        minewild$fillSolidBackground(context);
        minewild$drawConnectExitButtonFallback(context, mouseX, mouseY);
    }

    private void minewild$fillSolidBackground(DrawContext context) {
        if (context == null || this.width <= 0 || this.height <= 0) {
            return;
        }
        ClientCompat.disablePanorama();
        context.fill(0, 0, this.width, this.height, CONNECT_BACKGROUND_COLOR);
    }

    private void minewild$drawConnectExitButtonFallback(DrawContext context, int mouseX, int mouseY) {
        if (context == null) {
            return;
        }
        for (Element element : this.children()) {
            if (!(element instanceof ButtonWidget button) || !button.visible) {
                continue;
            }
            if (!minewild$isConnectExitButton(button.getMessage())) {
                continue;
            }
            int x = button.getX();
            int y = button.getY();
            int w = button.getWidth();
            int h = button.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            int fillColor = 0xFF4F4539;
            int topBorder = 0xFF9E8F7A;
            int sideBorder = 0xFF8A7C67;
            int bottomBorder = 0xFF2A241D;

            context.fill(x, y, x + w, y + h, fillColor);
            context.fill(x, y, x + w, y + 1, topBorder);
            context.fill(x, y, x + 1, y + h, sideBorder);
            context.fill(x + w - 1, y, x + w, y + h, sideBorder);
            context.fill(x, y + h - 1, x + w, y + h, bottomBorder);

            Identifier texture = minewild$pickButtonTexture(button, mouseX, mouseY);
            ClientCompat.drawTexture(context, texture, x, y, w, h, 200, 20);
            minewild$drawExitText1218(context, button, x, y, w, h);
            return;
        }
    }

    private void minewild$drawExitText1218(
            DrawContext context,
            ButtonWidget button,
            int x,
            int y,
            int width,
            int height
    ) {
        if (!ClientCompat.isMinecraft1218()
                && !ClientCompat.isMinecraft1217()
                && !ClientCompat.isMinecraft1216()) {
            return;
        }
        if (context == null || button == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        int textColor = button.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        int fontHeight = client.textRenderer.fontHeight;
        int textY = y + ((height - fontHeight) / 2);
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.literal(EXIT_LABEL),
                x + (width / 2),
                textY,
                textColor
        );
    }

    private Identifier minewild$pickButtonTexture(ButtonWidget button, int mouseX, int mouseY) {
        if (button == null || !button.active) {
            return BUTTON_DISABLED;
        }
        if (button.isSelected()) {
            return BUTTON_HIGHLIGHTED;
        }
        boolean hovered = mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
        return hovered ? BUTTON_HIGHLIGHTED : BUTTON_NORMAL;
    }

    private boolean minewild$isConnectExitButton(Text text) {
        if (text == null) {
            return false;
        }
        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatable && KEY_CANCEL.equals(translatable.getKey())) {
            return true;
        }
        String label = text.getString();
        if (label == null || label.isEmpty()) {
            return false;
        }
        return EXIT_LABEL.equals(label) || label.equals(Text.translatable(KEY_CANCEL).getString());
    }

}
