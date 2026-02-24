package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import hu.ColorsASD.minewild.client.DisconnectedScreenLayoutBridge;
import hu.ColorsASD.minewild.client.MinewildButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen implements DisconnectedScreenLayoutBridge {
    private static final String MINEWILD_ADDRESS = "play.minewild.hu";
    private static final String MINEWILD_NAME = "MINEWILD";
    private static final String SUPPORT_URL = "https://www.minewild.hu/support/";
    private static final String KEY_TO_MENU = "gui.toMenu";
    private static final String KEY_TO_TITLE = "gui.toTitle";
    private static final Text SUPPORT_LABEL = Text.literal("Segítségkérés");
    private static final Text RECONNECT_LABEL = Text.literal("Újracsatlakozás");
    private static final Text LEGACY_RECONNECT_LABEL = Text.literal("Csatlakozás");
    private static final Text EXIT_LABEL = Text.literal("Kilépés");
    private static final int BUTTON_SPACING = 4;
    private static final int DISCONNECTED_TEXT_TO_BUTTON_GAP = 10;
    private static final int DEFAULT_BUTTON_WIDTH = 200;
    private static final int DEFAULT_BUTTON_HEIGHT = 20;

    @Unique
    private int minewild$lastViewportWidth = -1;
    @Unique
    private int minewild$lastViewportHeight = -1;
    @Unique
    private int minewild$lastWindowWidth = -1;
    @Unique
    private int minewild$lastWindowHeight = -1;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void minewild$layoutMinewildButtonsOnInit(CallbackInfo ci) {
        minewild$capture1202ViewportSize();
        minewild$layoutMinewildButtons();
    }

    @Override
    public void minewild$onExternalRenderTick(DrawContext context) {
        minewild$sync1202ViewportSize(context);
        minewild$layoutMinewildButtons();
    }

    @Unique
    private void minewild$sync1202ViewportSize(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int viewportWidth;
        int viewportHeight;
        int windowWidth = -1;
        int windowHeight = -1;

        if (client != null && client.getWindow() != null) {
            viewportWidth = client.getWindow().getScaledWidth();
            viewportHeight = client.getWindow().getScaledHeight();
            windowWidth = client.getWindow().getWidth();
            windowHeight = client.getWindow().getHeight();
        } else if (context != null) {
            viewportWidth = context.getScaledWindowWidth();
            viewportHeight = context.getScaledWindowHeight();
            windowWidth = viewportWidth;
            windowHeight = viewportHeight;
        } else {
            return;
        }

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return;
        }

        boolean sameViewport = viewportWidth == minewild$lastViewportWidth && viewportHeight == minewild$lastViewportHeight;
        boolean sameWindow = windowWidth == minewild$lastWindowWidth && windowHeight == minewild$lastWindowHeight;
        if (sameViewport && sameWindow) {
            return;
        }

        minewild$lastViewportWidth = viewportWidth;
        minewild$lastViewportHeight = viewportHeight;
        minewild$lastWindowWidth = windowWidth;
        minewild$lastWindowHeight = windowHeight;

        if (this.width != viewportWidth || this.height != viewportHeight) {
            this.width = viewportWidth;
            this.height = viewportHeight;
        }
    }

    @Unique
    private void minewild$capture1202ViewportSize() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int windowWidth = client.getWindow().getWidth();
        int windowHeight = client.getWindow().getHeight();
        if (width > 0 && height > 0) {
            minewild$lastViewportWidth = width;
            minewild$lastViewportHeight = height;
            minewild$lastWindowWidth = windowWidth;
            minewild$lastWindowHeight = windowHeight;
        }
    }

    private void minewild$layoutMinewildButtons() {
        ButtonWidget menuButton = findMenuButton();
        ButtonWidget supportButton = findButtonByLabel(SUPPORT_LABEL);
        ButtonWidget reconnectButton = findReconnectButton();
        ButtonWidget exitButton = findButtonByLabel(EXIT_LABEL);

        int fullWidth = minewild$resolveFullButtonWidth(menuButton, supportButton, reconnectButton, exitButton);
        int height = minewild$resolveButtonHeight(menuButton, supportButton, reconnectButton, exitButton);
        int viewportWidth = minewild$resolveViewportWidth();
        int x = (viewportWidth - fullWidth) / 2;
        int y = minewild$resolve1202TopRowY(height, menuButton, supportButton);
        int smallWidth = (fullWidth - BUTTON_SPACING) / 2;
        int topRowY = y;
        int bottomRowY = y + height + BUTTON_SPACING;
        int rightX = x + smallWidth + BUTTON_SPACING;

        // Hide vanilla "Back to menu/title" button; we replace it with custom layout.
        minewild$hideAllMenuButtons();

        if (supportButton == null) {
            supportButton = MinewildButtonWidget.create(
                    SUPPORT_LABEL,
                    button -> openSupportPage(),
                    x,
                    topRowY,
                    fullWidth,
                    height
            );
            this.addDrawableChild(supportButton);
        } else {
            placeButton(supportButton, x, topRowY, fullWidth);
        }

        if (reconnectButton == null) {
            reconnectButton = MinewildButtonWidget.create(
                    RECONNECT_LABEL,
                    button -> connectToMinewild(),
                    x,
                    bottomRowY,
                    smallWidth,
                    height
            );
            this.addDrawableChild(reconnectButton);
        } else {
            placeButton(reconnectButton, x, bottomRowY, smallWidth);
        }

        if (exitButton == null) {
            exitButton = MinewildButtonWidget.create(
                    EXIT_LABEL,
                    button -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null) {
                            client.scheduleStop();
                        }
                    },
                    rightX,
                    bottomRowY,
                    smallWidth,
                    height
            );
            this.addDrawableChild(exitButton);
        } else {
            placeButton(exitButton, rightX, bottomRowY, smallWidth);
        }
    }

    @Unique
    private int minewild$resolveViewportWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null) {
            int scaledWidth = client.getWindow().getScaledWidth();
            if (scaledWidth > 0) {
                return scaledWidth;
            }
        }
        return this.width;
    }

    @Unique
    private static int minewild$resolveFullButtonWidth(
            ButtonWidget menuButton,
            ButtonWidget supportButton,
            ButtonWidget reconnectButton,
            ButtonWidget exitButton
    ) {
        if (menuButton != null && menuButton.getWidth() > 0) {
            return menuButton.getWidth();
        }
        if (supportButton != null && supportButton.getWidth() > 0) {
            return supportButton.getWidth();
        }
        int reconnectWidth = reconnectButton == null ? 0 : reconnectButton.getWidth();
        int exitWidth = exitButton == null ? 0 : exitButton.getWidth();
        if (reconnectWidth > 0 && exitWidth > 0) {
            return reconnectWidth + BUTTON_SPACING + exitWidth;
        }
        return DEFAULT_BUTTON_WIDTH;
    }

    @Unique
    private static int minewild$resolveButtonHeight(
            ButtonWidget menuButton,
            ButtonWidget supportButton,
            ButtonWidget reconnectButton,
            ButtonWidget exitButton
    ) {
        if (menuButton != null && menuButton.getHeight() > 0) {
            return menuButton.getHeight();
        }
        if (supportButton != null && supportButton.getHeight() > 0) {
            return supportButton.getHeight();
        }
        if (reconnectButton != null && reconnectButton.getHeight() > 0) {
            return reconnectButton.getHeight();
        }
        if (exitButton != null && exitButton.getHeight() > 0) {
            return exitButton.getHeight();
        }
        return DEFAULT_BUTTON_HEIGHT;
    }

    @Unique
    private int minewild$resolveFallbackX(ButtonWidget menuButton, ButtonWidget supportButton, int fullWidth) {
        if (menuButton != null) {
            return menuButton.getX();
        }
        if (supportButton != null) {
            return supportButton.getX();
        }
        return (this.width - fullWidth) / 2;
    }

    @Unique
    private int minewild$resolveFallbackY(ButtonWidget menuButton, ButtonWidget supportButton, int height) {
        if (menuButton != null) {
            return menuButton.getY();
        }
        if (supportButton != null) {
            return supportButton.getY();
        }
        return (this.height - height) / 2;
    }

    @Unique
    private int minewild$resolve1202TopRowY(int height, ButtonWidget menuButton, ButtonWidget supportButton) {
        int textBottom = Integer.MIN_VALUE;
        int boundedTextBottom = Integer.MIN_VALUE;
        int centerX = minewild$resolveViewportWidth() / 2;
        final int centerTolerance = 14;
        int maxAllowedTextBottom = this.height
                - (DISCONNECTED_TEXT_TO_BUTTON_GAP + (height * 2) + BUTTON_SPACING);
        for (Element element : this.children()) {
            if (!(element instanceof ClickableWidget widget) || widget instanceof ButtonWidget) {
                continue;
            }
            if (!widget.visible) {
                continue;
            }
            int widgetCenterX = widget.getX() + (widget.getWidth() / 2);
            if (Math.abs(widgetCenterX - centerX) > centerTolerance) {
                continue;
            }
            int bottom = widget.getY() + widget.getHeight();
            if (bottom > textBottom) {
                textBottom = bottom;
            }
            if (bottom <= maxAllowedTextBottom && bottom > boundedTextBottom) {
                boundedTextBottom = bottom;
            }
        }
        int resolvedTextBottom = boundedTextBottom != Integer.MIN_VALUE ? boundedTextBottom : textBottom;
        if (resolvedTextBottom != Integer.MIN_VALUE) {
            return resolvedTextBottom + DISCONNECTED_TEXT_TO_BUTTON_GAP;
        }
        if (menuButton != null) {
            return menuButton.getY();
        }
        if (supportButton != null) {
            return supportButton.getY();
        }
        return (this.height - height) / 2;
    }

    private ButtonWidget findMenuButton() {
        for (Element element : this.children()) {
            if (element instanceof ButtonWidget button && isMenuButton(button.getMessage())) {
                return button;
            }
        }
        return null;
    }

    private ButtonWidget findButtonByLabel(Text label) {
        String expected = label.getString();
        ButtonWidget found = null;
        List<ButtonWidget> duplicates = new ArrayList<>();
        for (Element element : this.children()) {
            if (!(element instanceof ButtonWidget button)) {
                continue;
            }
            String actual = button.getMessage() == null ? "" : button.getMessage().getString();
            if (!expected.equals(actual)) {
                continue;
            }
            if (found == null) {
                found = button;
            } else {
                duplicates.add(button);
            }
        }
        for (ButtonWidget duplicate : duplicates) {
            minewild$removeElement(duplicate);
        }
        return found;
    }

    private ButtonWidget findReconnectButton() {
        if (RECONNECT_LABEL.getString().equals(LEGACY_RECONNECT_LABEL.getString())) {
            return findButtonByLabel(RECONNECT_LABEL);
        }
        ButtonWidget reconnectButton = findButtonByLabel(RECONNECT_LABEL);
        ButtonWidget legacyReconnectButton = findButtonByLabel(LEGACY_RECONNECT_LABEL);
        if (reconnectButton != null && legacyReconnectButton != null) {
            if (legacyReconnectButton != reconnectButton) {
                minewild$removeElement(legacyReconnectButton);
            }
            return reconnectButton;
        }
        if (reconnectButton != null) {
            return reconnectButton;
        }
        if (legacyReconnectButton != null) {
            legacyReconnectButton.setMessage(RECONNECT_LABEL);
        }
        return legacyReconnectButton;
    }

    private static void placeButton(ButtonWidget button, int x, int y, int width) {
        if (button == null) {
            return;
        }
        button.setX(x);
        button.setY(y);
        if (button.getWidth() != width) {
            button.setWidth(width);
        }
    }

    private static boolean isMenuButton(Text text) {
        if (text == null) {
            return false;
        }
        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            return KEY_TO_MENU.equals(key) || KEY_TO_TITLE.equals(key);
        }
        String label = text.getString();
        if (label == null || label.isBlank()) {
            return false;
        }
        if (label.equals(Text.translatable(KEY_TO_MENU).getString())) {
            return true;
        }
        return label.equals(Text.translatable(KEY_TO_TITLE).getString());
    }

    private static void openSupportPage() {
        try {
            Util.getOperatingSystem().open(URI.create(SUPPORT_URL));
        } catch (Exception ignored) {
            try {
                Util.getOperatingSystem().open(SUPPORT_URL);
            } catch (Exception ignoredAgain) {
            }
        }
    }

    @Unique
    private void minewild$hideAllMenuButtons() {
        for (Element element : this.children()) {
            if (!(element instanceof ButtonWidget button) || !isMenuButton(button.getMessage())) {
                continue;
            }
            button.visible = false;
            button.active = false;
        }
    }

    @Unique
    private void minewild$removeElement(Element element) {
        if (element == null) {
            return;
        }
        try {
            ((ScreenInvokerMixin) (Object) this).minewild$invokeRemove(element);
        } catch (RuntimeException ignored) {
            if (element instanceof ClickableWidget widget) {
                widget.visible = false;
                widget.active = false;
            }
        }
    }

    private void connectToMinewild() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        ClientCompat.connectToServer(this, client, MINEWILD_NAME, MINEWILD_ADDRESS);
    }

}
