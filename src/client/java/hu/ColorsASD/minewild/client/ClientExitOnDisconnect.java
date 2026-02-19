package hu.ColorsASD.minewild.client;

import hu.ColorsASD.minewild.mixin.client.ScreenInvokerMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class ClientExitOnDisconnect {
    private static final String KEY_DISCONNECT = "menu.disconnect";
    private static final String KEY_RETURN_TO_MENU = "menu.returnToMenu";
    private static final String KEY_CANCEL = "gui.cancel";
    private static final Text EXIT_LABEL = Text.literal("Kilépés");
    private static final Set<ButtonWidget> WRAPPED_BUTTONS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<ButtonWidget> PATCHED_CONNECT_CANCELS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile boolean pendingExit = false;
    private static volatile boolean shutdownRequested = false;

    private ClientExitOnDisconnect() {
    }

    public static void request() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.isInSingleplayer()) {
            return;
        }
        pendingExit = true;
    }

    public static void markShutdownRequested() {
        shutdownRequested = true;
        ClientCompat.disablePanorama();
    }

    public static boolean isShutdownRequested() {
        return shutdownRequested;
    }

    public static void handleDisconnect(MinecraftClient client) {
        if (!pendingExit) {
            return;
        }
        if (client == null || client.isInSingleplayer()) {
            pendingExit = false;
            return;
        }
        if (client.getNetworkHandler() != null || client.world != null) {
            return;
        }
        pendingExit = false;
        client.execute(() -> {
            markShutdownRequested();
            client.scheduleStop();
        });
    }

    public static void handleTick(MinecraftClient client) {
        handleDisconnect(client);
        if (client == null || client.isInSingleplayer()) {
            return;
        }
        replaceConnectCancelButton(client.currentScreen);
    }

    public static void handleScreenInit(Screen screen) {
        if (screen == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.isInSingleplayer()) {
            return;
        }
        if (screen instanceof GameMenuScreen) {
            wrapDisconnectButton(screen);
        }
        replaceConnectCancelButton(screen);
    }

    private static void wrapDisconnectButton(Screen screen) {
        for (Element element : screen.children()) {
            if (element instanceof ButtonWidget button && isDisconnectButton(button.getMessage())) {
                wrapPressAction(button);
                return;
            }
        }
    }

    private static void replaceConnectCancelButton(Screen screen) {
        if (!ClientCompat.isConnectScreen(screen)) {
            return;
        }
        for (Element element : screen.children()) {
            if (!(element instanceof ButtonWidget button) || !isCancelButton(button.getMessage())) {
                continue;
            }
            if (PATCHED_CONNECT_CANCELS.contains(button)) {
                return;
            }
            ButtonWidget.PressAction exitAction = createExitAction();
            if (writePressAction(button, exitAction)) {
                button.setMessage(EXIT_LABEL);
                PATCHED_CONNECT_CANCELS.add(button);
                return;
            }
            if (replaceButtonWithExit(screen, button, exitAction)) {
                PATCHED_CONNECT_CANCELS.add(button);
            }
            return;
        }
    }

    private static ButtonWidget.PressAction createExitAction() {
        return pressed -> {
            MinecraftClient current = MinecraftClient.getInstance();
            if (current != null) {
                markShutdownRequested();
                current.scheduleStop();
            }
        };
    }

    private static boolean replaceButtonWithExit(Screen screen, ButtonWidget original, ButtonWidget.PressAction action) {
        if (screen == null || original == null || action == null) {
            return false;
        }
        ButtonWidget replacement = MinewildButtonWidget.create(
                EXIT_LABEL,
                action,
                original.getX(),
                original.getY(),
                original.getWidth(),
                original.getHeight()
        );
        original.visible = false;
        original.active = false;
        try {
            ((ScreenInvokerMixin) (Object) screen).minewild$invokeAddDrawableChild(replacement);
            return true;
        } catch (RuntimeException ignored) {
            original.visible = true;
            original.active = true;
            return false;
        }
    }

    private static boolean isDisconnectButton(Text text) {
        if (text == null) {
            return false;
        }
        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            return KEY_DISCONNECT.equals(key) || KEY_RETURN_TO_MENU.equals(key);
        }
        String label = text.getString();
        if (label == null || label.isEmpty()) {
            return false;
        }
        if (label.equals(Text.translatable(KEY_DISCONNECT).getString())) {
            return true;
        }
        return label.equals(Text.translatable(KEY_RETURN_TO_MENU).getString());
    }

    private static boolean isCancelButton(Text text) {
        if (text == null) {
            return false;
        }
        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatable) {
            return KEY_CANCEL.equals(translatable.getKey());
        }
        String label = text.getString();
        if (label == null || label.isEmpty()) {
            return false;
        }
        return label.equals(Text.translatable(KEY_CANCEL).getString());
    }

    private static void wrapPressAction(ButtonWidget button) {
        if (button == null || WRAPPED_BUTTONS.contains(button)) {
            return;
        }
        ButtonWidget.PressAction original = readPressAction(button);
        if (original == null) {
            return;
        }
        ButtonWidget.PressAction wrapped = pressed -> {
            ClientExitOnDisconnect.request();
            original.onPress(pressed);
        };
        if (writePressAction(button, wrapped)) {
            WRAPPED_BUTTONS.add(button);
        }
    }

    private static ButtonWidget.PressAction readPressAction(ButtonWidget button) {
        Field field = findPressActionField(button == null ? null : button.getClass());
        if (field == null) {
            return null;
        }
        try {
            Object value = field.get(button);
            if (value instanceof ButtonWidget.PressAction action) {
                return action;
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
        }
        return null;
    }

    private static boolean writePressAction(ButtonWidget button, ButtonWidget.PressAction action) {
        Field field = findPressActionField(button == null ? null : button.getClass());
        if (field == null) {
            return false;
        }
        try {
            field.set(button, action);
            return true;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return false;
        }
    }

    private static Field findPressActionField(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (ButtonWidget.PressAction.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                    } catch (RuntimeException ignored) {
                    }
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
