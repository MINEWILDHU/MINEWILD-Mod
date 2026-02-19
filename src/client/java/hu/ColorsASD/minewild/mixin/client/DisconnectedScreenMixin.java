package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import hu.ColorsASD.minewild.client.MinewildButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    private static final String MINEWILD_ADDRESS = "play.minewild.hu";
    private static final String MINEWILD_NAME = "MINEWILD";
    private static final String SUPPORT_URL = "https://www.minewild.hu/support/";
    private static final String KEY_TO_MENU = "gui.toMenu";
    private static final String KEY_TO_TITLE = "gui.toTitle";
    private static final Text SUPPORT_LABEL = Text.literal("Segítségkérés");
    private static final Text RECONNECT_LABEL = Text.literal("Újracsatlakozás");
    private static final Text EXIT_LABEL = Text.literal("Kilépés");
    private static final int BUTTON_SPACING = 4;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void minewild$layoutMinewildButtonsOnInit(CallbackInfo ci) {
        minewild$layoutMinewildButtons();
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void minewild$layoutMinewildButtonsOnRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        minewild$layoutMinewildButtons();
    }

    private void minewild$layoutMinewildButtons() {
        ButtonWidget menuButton = findMenuButton();
        if (menuButton == null) {
            return;
        }

        int fullWidth = menuButton.getWidth();
        int height = menuButton.getHeight();
        int x = menuButton.getX();
        int y = menuButton.getY();
        int smallWidth = (fullWidth - BUTTON_SPACING) / 2;
        int topRowY = y;
        int bottomRowY = y + height + BUTTON_SPACING;
        int rightX = x + smallWidth + BUTTON_SPACING;

        // Elrejtjük az eredeti "Vissza a menübe/címképernyőre" gombot.
        menuButton.visible = false;
        menuButton.active = false;

        ButtonWidget supportButton = findButtonByLabel(SUPPORT_LABEL);
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

        ButtonWidget reconnectButton = findButtonByLabel(RECONNECT_LABEL);
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

        ButtonWidget exitButton = findButtonByLabel(EXIT_LABEL);
        if (exitButton == null) {
            exitButton = MinewildButtonWidget.create(
                    EXIT_LABEL,
                    button -> MinecraftClient.getInstance().scheduleStop(),
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
        for (Element element : this.children()) {
            if (!(element instanceof ButtonWidget button)) {
                continue;
            }
            String actual = button.getMessage() == null ? "" : button.getMessage().getString();
            if (expected.equals(actual)) {
                return button;
            }
        }
        return null;
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

    private void connectToMinewild() {
        ClientCompat.connectToServer(this, MinecraftClient.getInstance(), MINEWILD_NAME, MINEWILD_ADDRESS);
    }
}
