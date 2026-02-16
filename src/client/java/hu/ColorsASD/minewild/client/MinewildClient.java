package hu.ColorsASD.minewild.client;

import net.fabricmc.api.ClientModInitializer;
import hu.ColorsASD.minewild.installer.ModInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundCategory;
import java.lang.reflect.Field;

public class MinewildClient implements ClientModInitializer {
    private static final String LANGUAGE_CODE = "hu_hu";
    private static final int GUI_SCALE = 3;
    private static final int GUI_SCALE_MIN_FRAMEBUFFER_WIDTH = 960;
    private static final int GUI_SCALE_MIN_FRAMEBUFFER_HEIGHT = 720;
    private static final int GUI_SCALE_FALLBACK_WINDOW_WIDTH = 1280;
    private static final int GUI_SCALE_FALLBACK_WINDOW_HEIGHT = 720;
    private static final int VIEW_DISTANCE = 6;
    private static final int SIMULATION_DISTANCE = 5;
    private static final int GUI_SCALE_RETRY_MS = 2000;
    private static final int FPS_UNLIMITED_FALLBACK = 260;
    private static Integer cachedUnlimitedFps;

    @Override
    public void onInitializeClient() {
        applyAccessibilityDefaultsImmediately();
        ModInstaller.beginInstallIfNeeded();
        scheduleBaseSettings();
    }

    private void applyAccessibilityDefaultsImmediately() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        GameOptions options = client.options;
        boolean changed = false;
        if (ClientCompat.setOnboardAccessibility(options, true)) {
            changed = true;
        }
        if (ClientCompat.setSkipMultiplayerWarning(options, true)) {
            changed = true;
        }
        if (changed) {
            options.write();
        }
    }

    private void scheduleBaseSettings() {
        Thread worker = new Thread(() -> {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 10_000) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.options != null) {
                    client.execute(() -> {
                        applyBaseSettings(client);
                        scheduleGuiScaleRetry(client);
                    });
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "minewild-options-init");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyBaseSettings(MinecraftClient client) {
        GameOptions options = client.options;
        if (options == null) {
            return;
        }
        boolean languageChanged = false;
        boolean resolutionChanged = false;
        boolean changed = false;

        if (!LANGUAGE_CODE.equals(options.language)) {
            options.language = LANGUAGE_CODE;
            client.getLanguageManager().setLanguage(LANGUAGE_CODE);
            languageChanged = true;
            changed = true;
        }

        if (ClientCompat.setOnboardAccessibility(options, true)) {
            changed = true;
        }
        if (ClientCompat.setSkipMultiplayerWarning(options, true)) {
            changed = true;
        }

        GuiScaleResult guiScaleResult = enforceGuiScale(client, options);
        if (guiScaleResult.changed) {
            changed = true;
        }
        if (guiScaleResult.changed || guiScaleResult.windowResized) {
            resolutionChanged = true;
        }

        SimpleOption<Integer> viewDistance = options.getViewDistance();
        if (viewDistance.getValue() != VIEW_DISTANCE) {
            viewDistance.setValue(VIEW_DISTANCE);
            changed = true;
        }

        SimpleOption<Integer> simulationDistance = options.getSimulationDistance();
        if (simulationDistance.getValue() != SIMULATION_DISTANCE) {
            simulationDistance.setValue(SIMULATION_DISTANCE);
            changed = true;
        }

        SimpleOption<Double> musicVolume = options.getSoundVolumeOption(SoundCategory.MUSIC);
        if (musicVolume.getValue() != 0.0) {
            musicVolume.setValue(0.0);
            client.getSoundManager().updateSoundVolume(SoundCategory.MUSIC, 0.0f);
            changed = true;
        }

        SimpleOption<Integer> maxFps = options.getMaxFps();
        int unlimitedFps = getUnlimitedFps();
        if (maxFps.getValue() != unlimitedFps) {
            maxFps.setValue(unlimitedFps);
            changed = true;
        }

        if (resolutionChanged) {
            client.onResolutionChanged();
        }
        if (ClientCompat.isAccessibilityOnboardingScreen(client.currentScreen)) {
            AutoConnectController.connectNow(client);
        }
        if (languageChanged) {
            client.reloadResources();
        }
        if (changed) {
            options.write();
        }
    }

    private void scheduleGuiScaleRetry(MinecraftClient client) {
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(GUI_SCALE_RETRY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            MinecraftClient instance = MinecraftClient.getInstance();
            if (instance == null) {
                return;
            }
            instance.execute(() -> applyGuiScale(instance));
        }, "minewild-guiscale-retry");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyGuiScale(MinecraftClient client) {
        GameOptions options = client.options;
        if (options == null) {
            return;
        }
        GuiScaleResult result = enforceGuiScale(client, options);
        if (result.changed || result.windowResized) {
            client.onResolutionChanged();
        }
        if (result.changed) {
            options.write();
        }
    }

    private GuiScaleResult enforceGuiScale(MinecraftClient client, GameOptions options) {
        SimpleOption<Integer> guiScale = options.getGuiScale();
        int before = guiScale.getValue();
        boolean windowResized = false;

        if (before != GUI_SCALE) {
            guiScale.setValue(GUI_SCALE);
            if (guiScale.getValue() != GUI_SCALE) {
                windowResized = ensureWindowSizeForGuiScale(client);
                guiScale.setValue(GUI_SCALE);
            }
        }

        return new GuiScaleResult(guiScale.getValue() != before, windowResized);
    }

    private boolean ensureWindowSizeForGuiScale(MinecraftClient client) {
        Window window = client.getWindow();
        if (window == null || window.isFullscreen()) {
            return false;
        }

        if (window.getFramebufferWidth() >= GUI_SCALE_MIN_FRAMEBUFFER_WIDTH
                && window.getFramebufferHeight() >= GUI_SCALE_MIN_FRAMEBUFFER_HEIGHT) {
            return false;
        }

        int targetWidth = Math.max(window.getWidth(), GUI_SCALE_FALLBACK_WINDOW_WIDTH);
        int targetHeight = Math.max(window.getHeight(), GUI_SCALE_FALLBACK_WINDOW_HEIGHT);
        window.setWindowedSize(targetWidth, targetHeight);
        return true;
    }

    private static final class GuiScaleResult {
        private final boolean changed;
        private final boolean windowResized;

        private GuiScaleResult(boolean changed, boolean windowResized) {
            this.changed = changed;
            this.windowResized = windowResized;
        }
    }

    private static int getUnlimitedFps() {
        Integer cached = cachedUnlimitedFps;
        if (cached != null) {
            return cached;
        }
        int value = FPS_UNLIMITED_FALLBACK;
        try {
            Field field = GameOptions.class.getDeclaredField("MAX_FRAMERATE");
            if (field.getType() == int.class) {
                value = field.getInt(null);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        cachedUnlimitedFps = value;
        return value;
    }
}
