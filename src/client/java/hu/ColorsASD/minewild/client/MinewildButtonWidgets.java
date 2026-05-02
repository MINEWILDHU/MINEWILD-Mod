package hu.ColorsASD.minewild.client;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class MinewildButtonWidgets {
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
            return ButtonWidget.builder(message, onPress)
                    .dimensions(x, y, width, height)
                    .build();
        }
        return Legacy.create(message, onPress, x, y, width, height);
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
