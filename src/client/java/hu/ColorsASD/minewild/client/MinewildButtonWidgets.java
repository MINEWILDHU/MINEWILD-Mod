package hu.ColorsASD.minewild.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class MinewildButtonWidgets {
    private static final Identifier BUTTON_NORMAL = ClientCompat.id("minewild", "runtime/widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED = ClientCompat.id("minewild", "runtime/widget/button_highlighted");
    private static final Identifier BUTTON_DISABLED = ClientCompat.id("minewild", "runtime/widget/button_disabled");
    private static final String BUTTON_NORMAL_RESOURCE = "/assets/minewild/textures/gui/widget/button.png";
    private static final String BUTTON_HIGHLIGHTED_RESOURCE = "/assets/minewild/textures/gui/widget/button_highlighted.png";
    private static final String BUTTON_DISABLED_RESOURCE = "/assets/minewild/textures/gui/widget/button_disabled.png";
    private static final Set<ButtonWidget> STYLED_12111_BUTTONS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static boolean runtimeTexturesReady;

    private MinewildButtonWidgets() {
    }

    public static ButtonWidget create(
            Text message,
            ButtonWidget.PressAction onPress,
            int x,
            int y,
            int width,
            int height
    ) {
        if (ClientCompat.isMinecraft12111()) {
            ButtonWidget button = ButtonWidget.builder(message, onPress)
                    .dimensions(x, y, width, height)
                    .build();
            STYLED_12111_BUTTONS.add(button);
            return button;
        }
        return Legacy.create(message, onPress, x, y, width, height);
    }

    public static boolean drawStyled12111Texture(ButtonWidget button, DrawContext context) {
        if (!ClientCompat.isMinecraft12111()
                || button == null
                || context == null
                || !button.visible
                || !STYLED_12111_BUTTONS.contains(button)) {
            return false;
        }

        ensureRuntimeTexturesLoaded();
        if (!runtimeTexturesReady) {
            return false;
        }

        Identifier texture = pickTexture(button);
        if (texture == null) {
            return false;
        }

        try {
            ClientCompat.drawTexture(
                    context,
                    texture,
                    button.getX(),
                    button.getY(),
                    button.getWidth(),
                    button.getHeight(),
                    200,
                    20
            );
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Identifier pickTexture(ButtonWidget button) {
        if (!button.active) {
            return BUTTON_DISABLED;
        }
        return button.isSelected() ? BUTTON_HIGHLIGHTED : BUTTON_NORMAL;
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
        try (InputStream in = MinewildButtonWidgets.class.getResourceAsStream(resourcePath)) {
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

    private static final class Legacy {
        private Legacy() {
        }

        private static ButtonWidget create(
                Text message,
                ButtonWidget.PressAction onPress,
                int x,
                int y,
                int width,
                int height
        ) {
            return MinewildButtonWidget.create(message, onPress, x, y, width, height);
        }
    }
}
