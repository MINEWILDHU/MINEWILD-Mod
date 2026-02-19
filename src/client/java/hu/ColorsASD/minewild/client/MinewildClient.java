package hu.ColorsASD.minewild.client;

import net.fabricmc.api.ClientModInitializer;
import hu.ColorsASD.minewild.installer.ModInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.Window;
import net.minecraft.sound.SoundCategory;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public class MinewildClient implements ClientModInitializer {
    private static final String LANGUAGE_CODE = "hu_hu";
    private static final int GUI_SCALE = 3;
    private static final int GUI_SCALE_MIN_FRAMEBUFFER_WIDTH = 960;
    private static final int GUI_SCALE_MIN_FRAMEBUFFER_HEIGHT = 720;
    private static final int GUI_SCALE_FALLBACK_WINDOW_WIDTH = 1280;
    private static final int GUI_SCALE_FALLBACK_WINDOW_HEIGHT = 720;
    private static final int VIEW_DISTANCE = 6;
    private static final int SIMULATION_DISTANCE = 5;
    private static final double GAMMA = 0.0;
    private static final boolean VSYNC_ENABLED = false;
    private static final ParticlesMode PARTICLES_MODE = ParticlesMode.DECREASED;
    private static final int BIOME_BLEND_RADIUS = 1;
    private static final int MIPMAP_LEVELS = 0;
    private static final int GUI_SCALE_RETRY_MS = 2000;
    private static final int WINDOW_ICON_RETRY_MS = 250;
    private static final int WINDOW_ICON_MAX_ATTEMPTS = 120;
    private static final int FPS_UNLIMITED_FALLBACK = 260;
    private static final String WINDOW_ICON_RESOURCE_PATH = "/assets/minewild/textures/gui/restart_logo.png";
    private static final String SODIUM_OPTIONS_CLASS_NAME =
            "me.jellysquid.mods.sodium.client.gui.SodiumGameOptions";
    private static Integer cachedUnlimitedFps;
    private static volatile boolean windowIconApplied;

    @Override
    public void onInitializeClient() {
        applyAccessibilityDefaultsImmediately();
        ModInstaller.beginInstallIfNeeded();
        scheduleWindowIconUpdate();
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
        if (ClientCompat.setTutorialStepNone(options)) {
            changed = true;
        }
        if (changed) {
            options.write();
        }
    }

    private void scheduleBaseSettings() {
        MinecraftClient immediateClient = MinecraftClient.getInstance();
        if (immediateClient != null && immediateClient.options != null) {
            immediateClient.execute(() -> {
                applyBaseSettings(immediateClient);
                scheduleGuiScaleRetry(immediateClient);
            });
            return;
        }

        Thread worker = new Thread(() -> {
            while (true) {
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

    private void scheduleWindowIconUpdate() {
        Thread worker = new Thread(() -> {
            int attempts = 0;
            while (!windowIconApplied && attempts < WINDOW_ICON_MAX_ATTEMPTS) {
                attempts++;
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    try {
                        client.execute(() -> applyWindowIcon(client));
                    } catch (RuntimeException ignored) {
                    }
                }
                if (windowIconApplied) {
                    return;
                }
                try {
                    Thread.sleep(WINDOW_ICON_RETRY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "minewild-window-icon");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyWindowIcon(MinecraftClient client) {
        if (windowIconApplied || client == null || client.getWindow() == null) {
            return;
        }
        try {
            long handle = client.getWindow().getHandle();
            if (handle == 0L) {
                return;
            }
            BufferedImage source = loadWindowIconSource();
            if (source == null) {
                return;
            }
            BufferedImage icon16 = scaleToSquare(source, 16);
            BufferedImage icon32 = scaleToSquare(source, 32);
            ByteBuffer pixels16 = imageToRgba(icon16);
            ByteBuffer pixels32 = imageToRgba(icon32);
            if (pixels16 == null || pixels32 == null) {
                return;
            }
            GLFWImage.Buffer icons = GLFWImage.malloc(2);
            try {
                icons.position(0);
                icons.width(16);
                icons.height(16);
                icons.pixels(pixels16);
                icons.position(1);
                icons.width(32);
                icons.height(32);
                icons.pixels(pixels32);
                icons.position(0);
                GLFW.glfwSetWindowIcon(handle, icons);
            } finally {
                icons.free();
            }
            windowIconApplied = true;
        } catch (RuntimeException ignored) {
        }
    }

    private static BufferedImage loadWindowIconSource() {
        try (InputStream in = MinewildClient.class.getResourceAsStream(WINDOW_ICON_RESOURCE_PATH)) {
            if (in == null) {
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static BufferedImage scaleToSquare(BufferedImage source, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, size, size, null);
        graphics.dispose();
        return scaled;
    }

    private static ByteBuffer imageToRgba(BufferedImage image) {
        if (image == null) {
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        int[] argb = new int[width * height];
        image.getRGB(0, 0, width, height, argb, 0, width);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int color : argb) {
            buffer.put((byte) ((color >> 16) & 0xFF));
            buffer.put((byte) ((color >> 8) & 0xFF));
            buffer.put((byte) (color & 0xFF));
            buffer.put((byte) ((color >> 24) & 0xFF));
        }
        buffer.flip();
        return buffer;
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
        if (ClientCompat.setTutorialStepNone(options)) {
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

        SimpleOption<Double> gamma = options.getGamma();
        if (gamma.getValue() != GAMMA) {
            gamma.setValue(GAMMA);
            changed = true;
        }

        SimpleOption<Boolean> enableVsync = options.getEnableVsync();
        if (enableVsync.getValue() != VSYNC_ENABLED) {
            enableVsync.setValue(VSYNC_ENABLED);
            changed = true;
        }

        SimpleOption<ParticlesMode> particles = options.getParticles();
        if (particles.getValue() != PARTICLES_MODE) {
            particles.setValue(PARTICLES_MODE);
            changed = true;
        }

        SimpleOption<Integer> biomeBlendRadius = options.getBiomeBlendRadius();
        if (biomeBlendRadius.getValue() != BIOME_BLEND_RADIUS) {
            biomeBlendRadius.setValue(BIOME_BLEND_RADIUS);
            changed = true;
        }

        SimpleOption<Integer> mipmapLevels = options.getMipmapLevels();
        if (mipmapLevels.getValue() != MIPMAP_LEVELS) {
            mipmapLevels.setValue(MIPMAP_LEVELS);
            changed = true;
        }

        if (applySodiumQualityDefaults()) {
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

    private boolean applySodiumQualityDefaults() {
        try {
            Class<?> sodiumOptionsClass = Class.forName(SODIUM_OPTIONS_CLASS_NAME);
            Object sodiumOptions = sodiumOptionsClass.getMethod("loadFromDisk").invoke(null);
            if (sodiumOptions == null) {
                return false;
            }

            Field qualityField = sodiumOptionsClass.getField("quality");
            Object qualitySettings = qualityField.get(sodiumOptions);
            if (qualitySettings == null) {
                return false;
            }

            boolean changed = false;

            Field weatherQuality = qualitySettings.getClass().getField("weatherQuality");
            if (setEnumField(weatherQuality, qualitySettings, "FAST")) {
                changed = true;
            }

            Field vignette = qualitySettings.getClass().getField("enableVignette");
            boolean vignetteEnabled = vignette.getBoolean(qualitySettings);
            if (vignetteEnabled) {
                vignette.setBoolean(qualitySettings, false);
                changed = true;
            }

            if (changed) {
                sodiumOptionsClass.getMethod("writeToDisk", sodiumOptionsClass).invoke(null, sodiumOptions);
            }
            return changed;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean setEnumField(Field field, Object target, String enumName) throws IllegalAccessException {
        Class<?> enumType = field.getType();
        if (!Enum.class.isAssignableFrom(enumType)) {
            return false;
        }
        Enum desired = Enum.valueOf((Class<? extends Enum>) enumType.asSubclass(Enum.class), enumName);
        Object current = field.get(target);
        if (desired.equals(current)) {
            return false;
        }
        field.set(target, desired);
        return true;
    }
}
