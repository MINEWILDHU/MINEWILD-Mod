package hu.ColorsASD.minewild.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Util;

import java.net.URI;
import java.text.Normalizer;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class ClientPauseMenuLinks {
    private static final String RETURN_TO_GAME_KEY = "menu.returnToGame";
    private static final String FEEDBACK_KEY = "menu.sendFeedback";
    private static final String FEEDBACK_KEY_ALT = "menu.feedback";
    private static final String BUG_REPORT_KEY = "menu.reportBugs";
    private static final String PLAYER_REPORTING_KEY = "menu.playerReporting";
    private static final String DISCONNECT_KEY = "menu.disconnect";
    private static final String RETURN_TO_MENU_KEY = "menu.returnToMenu";
    private static final String OPTIONS_KEY = "menu.options";
    private static final String ADVANCEMENTS_KEY = "gui.advancements";
    private static final String STATS_KEY = "gui.stats";

    private static final String WEBSTORE_LABEL = "Webáruház";
    private static final String WEBSITE_LABEL = "Weboldal";
    private static final String DISCORD_LABEL = "Discord";
    private static final String SUPPORT_LABEL = "Segítségkérés";

    private static final String WEBSTORE_URL = "https://shop.minewild.hu/";
    private static final String WEBSITE_URL = "https://www.minewild.hu/";
    private static final String DISCORD_URL = "https://discord.minewild.hu/";
    private static final String SUPPORT_URL = "https://www.minewild.hu/support/";

    private static final Map<ButtonWidget, Target> TARGET_BUTTONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ClientPauseMenuLinks() {
    }

    public static void handleScreenInit(Screen screen) {
        if (!(screen instanceof GameMenuScreen)) {
            return;
        }
        for (Element element : screen.children()) {
            if (!(element instanceof ButtonWidget button)) {
                continue;
            }
            Target target = resolveTarget(button.getMessage());
            if (target == null) {
                continue;
            }
            applyTarget(button, target);
        }
    }

    public static ButtonWidget createWebstoreButton(int x, int y, int width, int height) {
        ButtonWidget button = ButtonWidget.builder(
                        Text.literal(WEBSTORE_LABEL),
                        pressed -> openExternalUrl(WEBSTORE_URL)
                )
                .dimensions(x, y, width, height)
                .build();
        TARGET_BUTTONS.put(button, Target.WEBSTORE);
        return button;
    }

    public static void resetReturnToGameLabel(ButtonWidget button) {
        if (button == null) {
            return;
        }
        button.setMessage(Text.translatable(RETURN_TO_GAME_KEY));
    }

    public static void placeButton(ButtonWidget button, int x, int y, int width) {
        if (button == null) {
            return;
        }
        button.setX(x);
        button.setY(y);
        if (width > 0 && button.getWidth() != width) {
            button.setWidth(width);
        }
    }

    public static boolean isReturnToGameButton(Text text) {
        if (matchesKey(text, RETURN_TO_GAME_KEY)) {
            return true;
        }
        String normalized = normalize(text == null ? "" : text.getString());
        return normalized.contains("vissza a jatekba")
                || normalized.contains("folytatas")
                || normalized.contains("return to game")
                || normalized.contains("back to game")
                || normalized.contains("vissza a minewild szerverre");
    }

    public static boolean isOptionsButton(Text text) {
        if (matchesKey(text, OPTIONS_KEY)) {
            return true;
        }
        String normalized = normalize(text == null ? "" : text.getString());
        return normalized.contains("beallitas")
                || normalized.contains("options");
    }

    public static boolean isAdvancementsButton(Text text) {
        if (matchesKey(text, ADVANCEMENTS_KEY)) {
            return true;
        }
        String normalized = normalize(text == null ? "" : text.getString());
        return normalized.contains("elorelepes")
                || normalized.contains("advancements");
    }

    public static boolean isStatsButton(Text text) {
        if (matchesKey(text, STATS_KEY)) {
            return true;
        }
        String normalized = normalize(text == null ? "" : text.getString());
        return normalized.contains("statisztik")
                || normalized.contains("statistics")
                || normalized.equals("stats");
    }

    public static boolean isWebsiteButton(Text text) {
        return Target.WEBSITE == resolveTarget(text);
    }

    public static boolean isDiscordButton(Text text) {
        return Target.DISCORD == resolveTarget(text);
    }

    public static boolean isSupportButton(Text text) {
        return Target.SUPPORT == resolveTarget(text);
    }

    public static boolean isWebstoreButton(Text text) {
        if (text == null) {
            return false;
        }
        return Target.WEBSTORE == resolveTarget(text);
    }

    public static boolean isDisconnectButton(Text text) {
        if (text == null) {
            return false;
        }
        if (matchesKey(text, DISCONNECT_KEY, RETURN_TO_MENU_KEY)) {
            return true;
        }
        String label = text.getString();
        if (label == null || label.isBlank()) {
            return false;
        }
        if (label.equals(Text.translatable(DISCONNECT_KEY).getString())) {
            return true;
        }
        return label.equals(Text.translatable(RETURN_TO_MENU_KEY).getString());
    }

    private static void applyTarget(ButtonWidget button, Target target) {
        if (button == null || target == null) {
            return;
        }
        TARGET_BUTTONS.put(button, target);
        if (!target.label.equals(button.getMessage().getString())) {
            button.setMessage(Text.literal(target.label));
        }
    }

    public static boolean handleButtonPress(ButtonWidget button) {
        if (button == null) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !(client.currentScreen instanceof GameMenuScreen)) {
            return false;
        }
        Target target = TARGET_BUTTONS.get(button);
        if (target == null) {
            target = resolveTarget(button.getMessage());
        }
        if (target == null) {
            return false;
        }
        openExternalUrl(target.url);
        return true;
    }

    private static Target resolveTarget(Text text) {
        if (text == null) {
            return null;
        }
        TextContent content = text.getContent();
        if (content instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            Target byKey = resolveByKey(key);
            if (byKey != null) {
                return byKey;
            }
        }
        return resolveByLabel(text.getString());
    }

    private static Target resolveByKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (FEEDBACK_KEY.equals(key) || FEEDBACK_KEY_ALT.equals(key)) {
            return Target.WEBSITE;
        }
        if (BUG_REPORT_KEY.equals(key)) {
            return Target.DISCORD;
        }
        if (PLAYER_REPORTING_KEY.equals(key)) {
            return Target.SUPPORT;
        }
        return null;
    }

    private static Target resolveByLabel(String label) {
        String normalized = normalize(label);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.contains("visszajelzes")
                || normalized.contains("feedback")
                || normalized.contains("give feedback")
                || normalized.contains("weboldal")) {
            return Target.WEBSITE;
        }
        if (normalized.contains("webaruhaz")
                || normalized.contains("webshop")) {
            return Target.WEBSTORE;
        }
        if (normalized.contains("hibajelentes")
                || normalized.contains("report bugs")
                || normalized.contains("bug report")
                || normalized.contains("discord")) {
            return Target.DISCORD;
        }
        if (normalized.contains("jatekos jelentese")
                || normalized.contains("player reporting")
                || normalized.contains("segitsegkeres")) {
            return Target.SUPPORT;
        }
        return null;
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String noMarks = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noMarks.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean matchesKey(Text text, String... keys) {
        if (text == null || keys == null || keys.length == 0) {
            return false;
        }
        TextContent content = text.getContent();
        if (!(content instanceof TranslatableTextContent translatable)) {
            return false;
        }
        String key = translatable.getKey();
        if (key == null || key.isBlank()) {
            return false;
        }
        for (String candidate : keys) {
            if (candidate != null && candidate.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static void openExternalUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(rawUrl);
            Util.getOperatingSystem().open(uri);
        } catch (Exception ignored) {
            try {
                Util.getOperatingSystem().open(rawUrl);
            } catch (Exception ignoredAgain) {
            }
        }
    }

    private enum Target {
        WEBSTORE(WEBSTORE_LABEL, WEBSTORE_URL),
        WEBSITE(WEBSITE_LABEL, WEBSITE_URL),
        DISCORD(DISCORD_LABEL, DISCORD_URL),
        SUPPORT(SUPPORT_LABEL, SUPPORT_URL);

        private final String label;
        private final String url;

        Target(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }
}
