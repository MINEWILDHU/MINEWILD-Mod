package hu.ColorsASD.minewild.mixin.client;

import hu.ColorsASD.minewild.client.ClientCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    private static final Identifier RESTART_LOGO = ClientCompat.id("minewild", "runtime/gui/restart_logo");
    private static final String RESTART_LOGO_RESOURCE = "/assets/minewild/textures/gui/restart_logo.png";
    private static final int LOGO_TEXTURE_WIDTH = 965;
    private static final int LOGO_TEXTURE_HEIGHT = 965;
    private static final int MIN_LOGO_WIDTH = 120;
    private static final int MIN_LOGO_HEIGHT = 70;
    private static final int MAX_LOGO_WIDTH = 320;
    private static final int MAX_LOGO_HEIGHT = 180;
    private static final int TOP_MARGIN = 20;
    private static final int BACKGROUND_COLOR = 0xFF111114;
    private static final int BAR_BORDER_COLOR = 0xFF72655B;
    private static final int BAR_TRACK_COLOR = 0xFF2D2723;
    private static final int BAR_PROGRESS_COLOR = 0xFFC67C12;
    private static boolean logoLoadAttempted;
    private static boolean logoAvailable;
    private static Field progressField;
    private static boolean progressFieldChecked;

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void minewild$drawRestartLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (RESTART_LOGO == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        ensureLogoLoaded(client);

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        try {
            context.fill(0, 0, width, height, BACKGROUND_COLOR);

            int maxLogoWidth = Math.max(MIN_LOGO_WIDTH, Math.min(MAX_LOGO_WIDTH, width - 40));
            int maxLogoHeight = Math.max(MIN_LOGO_HEIGHT, Math.min(MAX_LOGO_HEIGHT, height / 3));
            float scale = Math.min((float) maxLogoWidth / LOGO_TEXTURE_WIDTH, (float) maxLogoHeight / LOGO_TEXTURE_HEIGHT);
            int logoWidth = Math.max(1, Math.round(LOGO_TEXTURE_WIDTH * scale));
            int logoHeight = Math.max(1, Math.round(LOGO_TEXTURE_HEIGHT * scale));
            int logoX = (width - logoWidth) / 2;
            int logoY = Math.max(TOP_MARGIN, (height - logoHeight) / 2 - 22);
            int barWidth = Math.max(200, Math.min(440, width - 80));
            int barHeight = 8;
            int barX = (width - barWidth) / 2;
            int barY = Math.min(height - 28, logoY + logoHeight + 28);
            drawProgressBar(context, barX, barY, barWidth, barHeight, readProgressValue((Object) this));

            ClientCompat.disableDepthTest();
            ClientCompat.depthMask(false);
            ClientCompat.enableBlend();
            ClientCompat.defaultBlendFunc();
            ClientCompat.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            MatrixStack matrices = ClientCompat.getMatrixStack(context);
            if (matrices != null) {
                matrices.push();
                matrices.translate(0.0, 0.0, 200.0);
            }
            try {
                if (logoAvailable) {
                    if (ClientCompat.isMinecraft1216OrAbove() && ClientCompat.shouldScaleLogo()) {
                        ClientCompat.forceNearestSampler(RESTART_LOGO);
                    }
                    if (ClientCompat.isMinecraft1211OrAbove() || ClientCompat.isMinecraft1201Through1210()) {
                        ClientCompat.drawTexture(context, RESTART_LOGO, logoX, logoY, logoWidth, logoHeight,
                                LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
                    } else {
                        ClientCompat.prepareGuiTexture(RESTART_LOGO);
                        boolean manualOk = ClientCompat.drawTextureManual(context, RESTART_LOGO, logoX, logoY, logoWidth, logoHeight);
                        if (!manualOk) {
                            ClientCompat.drawTextureLegacy(context, RESTART_LOGO, logoX, logoY, logoWidth, logoHeight,
                                    LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);
                        }
                    }
                }
            } finally {
                if (matrices != null) {
                    matrices.pop();
                }
                ClientCompat.depthMask(true);
                ClientCompat.enableDepthTest();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static void drawProgressBar(DrawContext context, int x, int y, int width, int height, float progress) {
        context.fill(x - 2, y - 2, x + width + 2, y + height + 2, BAR_BORDER_COLOR);
        context.fill(x, y, x + width, y + height, BAR_TRACK_COLOR);
        if (progress < 0.0f) {
            return;
        }
        int fill = Math.max(0, Math.min(width, Math.round(width * progress)));
        if (fill > 0) {
            context.fill(x, y, x + fill, y + height, BAR_PROGRESS_COLOR);
        }
    }

    private static float readProgressValue(Object overlay) {
        Field field = getProgressField();
        if (field == null) {
            return -1.0f;
        }
        try {
            Object target = Modifier.isStatic(field.getModifiers()) ? null : overlay;
            Object value = field.get(target);
            if (value instanceof Number number) {
                return clamp(number.floatValue());
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
        }
        return -1.0f;
    }

    private static Field getProgressField() {
        if (progressFieldChecked) {
            return progressField;
        }
        progressFieldChecked = true;
        Class<?> target = SplashOverlay.class;
        Field fallback = null;
        int fallbackScore = Integer.MIN_VALUE;
        while (target != null && target != Object.class) {
            for (Field field : target.getDeclaredFields()) {
                Class<?> type = field.getType();
                if (type != float.class && type != double.class) {
                    continue;
                }
                int score = Modifier.isStatic(field.getModifiers()) ? 0 : 5;
                String name = field.getName().toLowerCase();
                if (name.contains("progress")) {
                    score += 20;
                }
                if (score > fallbackScore) {
                    fallback = field;
                    fallbackScore = score;
                }
                if (!name.contains("progress")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (RuntimeException ignored) {
                }
                progressField = field;
                return progressField;
            }
            target = target.getSuperclass();
        }
        if (fallback != null) {
            try {
                fallback.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
            progressField = fallback;
        }
        return progressField;
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return -1.0f;
        }
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static void ensureLogoLoaded(MinecraftClient client) {
        if (logoLoadAttempted) {
            return;
        }
        logoLoadAttempted = true;
        if (client == null || client.getTextureManager() == null) {
            return;
        }
        try (InputStream in = SplashOverlayMixin.class.getResourceAsStream(RESTART_LOGO_RESOURCE)) {
            if (in == null) {
                return;
            }
            NativeImage image = NativeImage.read(in);
            NativeImageBackedTexture texture = ClientCompat.createBackedTexture(image);
            if (texture == null) {
                image.close();
                return;
            }
            client.getTextureManager().registerTexture(RESTART_LOGO, texture);
            logoAvailable = true;
        } catch (IOException | RuntimeException ignored) {
        }
    }
}
