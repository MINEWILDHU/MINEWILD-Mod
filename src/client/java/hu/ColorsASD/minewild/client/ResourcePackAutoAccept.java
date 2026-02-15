package hu.ColorsASD.minewild.client;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ResourcePackAutoAccept {
    private static final String MINEWILD_ADDRESS = "play.minewild.hu";
    private static final long AUTO_ACCEPT_TTL_MS = 60_000;
    private static final String[] RESOURCE_PACK_KEYWORDS = {
            "erőforrás-csomag",
            "erőforráscsomag",
            "erőforrás csomag",
            "erőforrás"
    };

    private static volatile long autoAcceptUntilMs = 0L;
    private static volatile int lastAcceptedScreenId = 0;

    private ResourcePackAutoAccept() {
    }

    public static void notifyConnect(String address) {
        if (isMinewildAddress(address)) {
            autoAcceptUntilMs = System.currentTimeMillis() + AUTO_ACCEPT_TTL_MS;
        }
    }

    public static void handleScreenInit(Screen screen) {
        if (screen == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (!shouldAutoAccept(client)) {
            return;
        }
        if (!looksLikeResourcePackPrompt(screen)) {
            return;
        }
        int screenId = System.identityHashCode(screen);
        if (screenId == lastAcceptedScreenId) {
            return;
        }
        if (tryAccept(screen)) {
            lastAcceptedScreenId = screenId;
            autoAcceptUntilMs = 0L;
        }
    }

    private static boolean shouldAutoAccept(MinecraftClient client) {
        if (client == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now <= autoAcceptUntilMs) {
            return true;
        }
        String current = ClientCompat.tryGetCurrentServerAddress(client);
        return isMinewildAddress(current);
    }

    private static boolean looksLikeResourcePackPrompt(Screen screen) {
        Text title = screen.getTitle();
        if (containsResourcePackKeyword(title)) {
            return true;
        }
        for (Field field : getAllFields(screen.getClass())) {
            Object value = readFieldValue(screen, field);
            if (value instanceof Text text && containsResourcePackKeyword(text)) {
                return true;
            }
            if (value instanceof String text && containsResourcePackKeyword(text)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsResourcePackKeyword(Text text) {
        if (text == null) {
            return false;
        }
        return containsResourcePackKeyword(text.getString());
    }

    private static boolean containsResourcePackKeyword(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : RESOURCE_PACK_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryAccept(Screen screen) {
        for (Field field : getAllFields(screen.getClass())) {
            Object value = readFieldValue(screen, field);
            if (value == null) {
                continue;
            }
            if (value instanceof BooleanConsumer consumer) {
                consumer.accept(true);
                return true;
            }
            if (value instanceof Consumer<?> consumer) {
                invokeConsumer(consumer, true);
                return true;
            }
            if (invokeAcceptMethod(value, true)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void invokeConsumer(Consumer<?> consumer, boolean value) {
        try {
            ((Consumer<Object>) consumer).accept(Boolean.valueOf(value));
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean invokeAcceptMethod(Object target, boolean value) {
        for (Method method : target.getClass().getMethods()) {
            if (!"accept".equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param != boolean.class && param != Boolean.class) {
                continue;
            }
            try {
                method.invoke(target, value);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        return false;
    }

    private static Object readFieldValue(Object target, Field field) {
        if (target == null || field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            Field[] declared = current.getDeclaredFields();
            if (declared.length == 0) {
                continue;
            }
            for (Field field : declared) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static boolean isMinewildAddress(String address) {
        if (address == null) {
            return false;
        }
        String host = normalizeHost(address);
        return MINEWILD_ADDRESS.equals(host);
    }

    private static String normalizeHost(String address) {
        String trimmed = address.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith("[")) {
            int end = trimmed.indexOf(']');
            if (end > 1) {
                return trimmed.substring(1, end);
            }
            return trimmed.substring(1);
        }
        int colon = trimmed.indexOf(':');
        if (colon > 0) {
            return trimmed.substring(0, colon);
        }
        return trimmed;
    }
}
