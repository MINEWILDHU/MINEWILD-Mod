package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    private static final String MINEWILD_ADDRESS = "play.minewild.hu";
    private static final String MINEWILD_NAME = "MINEWILD";
    private static final String KEY_TO_MENU = "gui.toMenu";
    private static final String KEY_TO_TITLE = "gui.toTitle";
    private static final Text RECONNECT_LABEL = Text.literal("Újracsatlakozás");
    private static final Text EXIT_LABEL = Text.literal("Kilépés");
    private boolean minewild$reconnectReady;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @ModifyArgs(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/ButtonWidget;builder(Lnet/minecraft/text/Text;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)Lnet/minecraft/client/gui/widget/ButtonWidget$Builder;"
            ),
            require = 0
    )
    private void minewild$replaceMenuButton(Args args) {
        Text label = args.get(0);
        if (!isMenuButton(label)) {
            return;
        }
        args.set(0, RECONNECT_LABEL);
        args.set(1, (ButtonWidget.PressAction) button -> connectToMinewild());
        minewild$reconnectReady = true;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void minewild$addExitButton(CallbackInfo ci) {
        ensureReconnectButton();
        for (Element element : this.children()) {
            if (element instanceof ButtonWidget button
                    && EXIT_LABEL.getString().equals(button.getMessage().getString())) {
                return;
            }
        }
        int buttonWidth = 200;
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 2;
        int y = findExitButtonY();
        ButtonWidget exitButton = ButtonWidget.builder(EXIT_LABEL, button -> MinecraftClient.getInstance().scheduleStop())
                .dimensions(x, y, buttonWidth, buttonHeight)
                .build();
        this.addDrawableChild(exitButton);
    }

    private void ensureReconnectButton() {
        if (minewild$reconnectReady) {
            return;
        }
        if (hasReconnectButton()) {
            minewild$reconnectReady = true;
            return;
        }
        ButtonWidget menuButton = findMenuButton();
        if (menuButton == null) {
            return;
        }
        ButtonWidget.PressAction action = button -> connectToMinewild();
        if (updateButtonAction(menuButton, action)) {
            minewild$reconnectReady = true;
            return;
        }
        replaceMenuButton(menuButton, action);
        minewild$reconnectReady = true;
    }

    private boolean hasReconnectButton() {
        for (Element element : this.children()) {
            if (element instanceof ButtonWidget button
                    && RECONNECT_LABEL.getString().equals(button.getMessage().getString())) {
                return true;
            }
        }
        return false;
    }

    private ButtonWidget findMenuButton() {
        for (Element element : this.children()) {
            if (element instanceof ButtonWidget button && isMenuButton(button.getMessage())) {
                return button;
            }
        }
        return null;
    }

    private boolean updateButtonAction(ButtonWidget button, ButtonWidget.PressAction action) {
        if (button == null) {
            return false;
        }
        if (!setPressAction(button, action)) {
            return false;
        }
        button.setMessage(RECONNECT_LABEL);
        return true;
    }

    private void replaceMenuButton(ButtonWidget menuButton, ButtonWidget.PressAction action) {
        int defaultWidth = 200;
        int defaultHeight = 20;
        int x = readWidgetInt(menuButton, "getX", "x", (this.width - defaultWidth) / 2);
        int y = readWidgetInt(menuButton, "getY", "y", this.height / 2 - 10);
        int width = readWidgetInt(menuButton, "getWidth", "width", defaultWidth);
        int height = readWidgetInt(menuButton, "getHeight", "height", defaultHeight);
        disableWidget(menuButton);
        ButtonWidget reconnectButton = ButtonWidget.builder(RECONNECT_LABEL, action)
                .dimensions(x, y, width, height)
                .build();
        this.addDrawableChild(reconnectButton);
    }

    private static int readWidgetInt(Object target, String methodName, String fieldName, int fallback) {
        Integer value = callIntMethod(target, methodName);
        if (value != null) {
            return value;
        }
        value = readIntField(target, fieldName);
        return value != null ? value : fallback;
    }

    private static Integer callIntMethod(Object target, String name) {
        if (target == null || name == null || name.isBlank()) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name);
            if (method.getReturnType() != int.class || method.getParameterCount() != 0) {
                return null;
            }
            return (Integer) method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Integer readIntField(Object target, String name) {
        Field field = findField(target, name, int.class);
        if (field == null) {
            return null;
        }
        try {
            return field.getInt(target);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    private static void disableWidget(ButtonWidget button) {
        setBooleanField(button, "active", false);
        setBooleanField(button, "visible", false);
    }

    private static void setBooleanField(Object target, String name, boolean value) {
        Field field = findField(target, name, boolean.class);
        if (field == null) {
            return;
        }
        try {
            field.setBoolean(target, value);
        } catch (IllegalAccessException | RuntimeException ignored) {
        }
    }

    private static Field findField(Object target, String name, Class<?> type) {
        if (target == null || name == null || name.isBlank()) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                if (type == null || type.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException | RuntimeException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean setPressAction(ButtonWidget button, ButtonWidget.PressAction action) {
        if (button == null || action == null) {
            return false;
        }
        Field field = findPressActionField(button.getClass());
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

    private int findExitButtonY() {
        for (Element element : this.children()) {
            if (element instanceof ButtonWidget button
                    && RECONNECT_LABEL.getString().equals(button.getMessage().getString())) {
                return button.getY() + 24;
            }
        }
        return this.height / 2 + 32;
    }

    private void connectToMinewild() {
        ClientCompat.connectToServer(this, MinecraftClient.getInstance(), MINEWILD_NAME, MINEWILD_ADDRESS);
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
        return false;
    }
}
