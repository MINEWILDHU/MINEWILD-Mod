package hu.ColorsASD.minewild.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.InputStream;

public class MinewildButtonWidget extends ButtonWidget {
    private static final Identifier BUTTON_NORMAL = ClientCompat.id("minewild", "runtime/widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED = ClientCompat.id("minewild", "runtime/widget/button_highlighted");
    private static final Identifier BUTTON_DISABLED = ClientCompat.id("minewild", "runtime/widget/button_disabled");
    private static final String BUTTON_NORMAL_RESOURCE = "/assets/minewild/textures/gui/widget/button.png";
    private static final String BUTTON_HIGHLIGHTED_RESOURCE = "/assets/minewild/textures/gui/widget/button_highlighted.png";
    private static final String BUTTON_DISABLED_RESOURCE = "/assets/minewild/textures/gui/widget/button_disabled.png";
    private static boolean runtimeTexturesReady;

    protected MinewildButtonWidget(
            int x,
            int y,
            int width,
            int height,
            Text message,
            PressAction onPress,
            NarrationSupplier narrationSupplier
    ) {
        super(x, y, width, height, message, onPress, narrationSupplier);
    }

    public static MinewildButtonWidget create(Text message, PressAction onPress, int x, int y, int width, int height) {
        return new MinewildButtonWidget(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        ensureRuntimeTexturesLoaded();
        if (!runtimeTexturesReady) {
            super.renderButton(context, mouseX, mouseY, delta);
            return;
        }
        Identifier texture = pickTexture();
        if (texture == null) {
            super.renderButton(context, mouseX, mouseY, delta);
            return;
        }

        try {
            ClientCompat.drawTexture(
                context,
                texture,
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight(),
                200,
                20
            );
        } catch (RuntimeException ignored) {
            super.renderButton(context, mouseX, mouseY, delta);
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null) {
            int textColor = this.active ? 0xFFFFFF : 0xA0A0A0;
            int withAlpha = textColor | (MathHelper.ceil(this.alpha * 255.0f) << 24);
            this.drawMessage(context, client.textRenderer, withAlpha);
        }
    }

    private Identifier pickTexture() {
        if (!this.active) {
            return BUTTON_DISABLED;
        }
        return this.isSelected() ? BUTTON_HIGHLIGHTED : BUTTON_NORMAL;
    }

    private static void ensureRuntimeTexturesLoaded() {
        if (runtimeTexturesReady) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getTextureManager() == null) {
            return;
        }

        boolean normalLoaded = registerRuntimeTexture(client, BUTTON_NORMAL, BUTTON_NORMAL_RESOURCE);
        boolean highlightedLoaded = registerRuntimeTexture(client, BUTTON_HIGHLIGHTED, BUTTON_HIGHLIGHTED_RESOURCE);
        boolean disabledLoaded = registerRuntimeTexture(client, BUTTON_DISABLED, BUTTON_DISABLED_RESOURCE);
        runtimeTexturesReady = normalLoaded && highlightedLoaded && disabledLoaded;
    }

    private static boolean registerRuntimeTexture(MinecraftClient client, Identifier id, String resourcePath) {
        if (client == null || id == null || resourcePath == null || resourcePath.isBlank()) {
            return false;
        }
        try (InputStream in = MinewildButtonWidget.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return false;
            }
            NativeImage image = NativeImage.read(in);
            NativeImageBackedTexture texture = ClientCompat.createBackedTexture(image);
            if (texture == null) {
                image.close();
                return false;
            }
            client.getTextureManager().registerTexture(id, texture);
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }
}
