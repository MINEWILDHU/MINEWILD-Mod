package hu.ColorsASD.minewild.client;

import com.mojang.blaze3d.systems.RenderSystem;
import hu.ColorsASD.minewild.installer.ModInstaller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;


public class RestartRequiredScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("MinewildRestartScreen");
    private static final Text TITLE = Text.literal("Újraindítás szükséges");
    private static final String LOGO_RESOURCE_PATH = "/assets/minewild/textures/gui/restart_logo.png";
    private static final Identifier LOGO_TEXTURE = ClientCompat.id("minewild", "textures/gui/restart_logo.png");
    private static final int LOGO_TEXTURE_WIDTH = 965;
    private static final int LOGO_TEXTURE_HEIGHT = 965;
    private static final int LINE_HEIGHT = 9;
    private static final int LINE_SPACING = 3;
    private static final int LOGO_TOP_PADDING = 20;
    private static final int LOGO_GAP = 16;
    private static final int MESSAGE_GAP = 16;
    private static final Text BUTTON_DELETE = Text.literal("Törlés");
    private static final Text BUTTON_EXIT = Text.literal("Kilépés");
    private Text lineOne;
    private Text lineTwo;
    private ButtonWidget actionButton;
    private boolean lastDownloading;
    private boolean lastDownloadFailed;
    private boolean lastExtraModsDetected;
    private int messageY;
    private int logoX;
    private int logoY;
    private int logoWidth;
    private int logoHeight;
    private int logoTextureWidth;
    private int logoTextureHeight;
    private static boolean logoLoadAttempted;
    private static boolean logoAvailable;
    private static Method nativeImageReadMethod;
    private static Method nativeImageWriteMethod;
    private static Method nativeImageCopyPixelsMethod;
    private static boolean nativeImageMethodsChecked;
    private static Boolean nativeImageWriteArgb;

    public RestartRequiredScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        int maxLogoWidth = Math.max(1, Math.min(256, this.width - 40));
        int maxLogoHeight = Math.max(1, Math.min(128, this.height / 4));
        float scale = Math.min((float) maxLogoWidth / LOGO_TEXTURE_WIDTH, (float) maxLogoHeight / LOGO_TEXTURE_HEIGHT);
        logoWidth = Math.max(1, Math.round(LOGO_TEXTURE_WIDTH * scale));
        logoHeight = Math.max(1, Math.round(LOGO_TEXTURE_HEIGHT * scale));
        logoX = (this.width - logoWidth) / 2;
        logoY = LOGO_TOP_PADDING;
        logoTextureWidth = LOGO_TEXTURE_WIDTH;
        logoTextureHeight = LOGO_TEXTURE_HEIGHT;

        messageY = logoY + logoHeight + LOGO_GAP;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 2;
        int buttonY = messageY + (LINE_HEIGHT * 2 + LINE_SPACING) + MESSAGE_GAP;
        int maxButtonY = this.height - 40;
        if (buttonY > maxButtonY) {
            buttonY = maxButtonY;
            messageY = buttonY - (LINE_HEIGHT * 2 + LINE_SPACING) - MESSAGE_GAP;
        }

        actionButton = this.addDrawableChild(ButtonWidget.builder(getActionLabel(ModInstaller.hasExtraModsDetected()),
                        button -> handleAction())
                .dimensions(x, buttonY, buttonWidth, buttonHeight)
                .build());

        lastDownloading = !ModInstaller.isDownloadInProgress();
        lastDownloadFailed = ModInstaller.hasDownloadFailed();
        lastExtraModsDetected = ModInstaller.hasExtraModsDetected();
        updateMessage();

    }

    @Override
    public void tick() {
        super.tick();
        if (!ModInstaller.isRestartRequired()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == this) {
                client.setScreen(new TitleScreen());
            }
            return;
        }
        updateMessage();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ensureLogoLoaded();
        drawBackground(context);
        if (actionButton != null) {
            super.render(context, mouseX, mouseY, delta);
        }
        ClientCompat.resetScissor(context, this.width, this.height);
        renderMessage(context);
        ClientCompat.resetScissor(context, this.width, this.height);
        drawLogo(context);
    }

    @Override
    public void renderBackground(DrawContext context) {
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void drawBackground(DrawContext context) {
        ClientCompat.disablePanorama();
        ClientCompat.resetScissor(context, this.width, this.height);
        if (ClientCompat.isMinecraft1211OrBelow()) {
            ClientCompat.disableDepthTest();
            ClientCompat.depthMask(false);
            RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
            RenderSystem.clear(0x4000, MinecraftClient.IS_SYSTEM_MAC);
        }
        if (ClientCompat.isMinecraft1214OrBelow()) {
            ClientCompat.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            ClientCompat.enableBlend();
            ClientCompat.defaultBlendFunc();
        }
        context.fill(0, 0, this.width, this.height, 0xFF101010);
        if (ClientCompat.isMinecraft1211OrBelow()) {
            ClientCompat.depthMask(true);
            ClientCompat.enableDepthTest();
        }
    }

    private void drawLogo(DrawContext context) {
        if (!logoAvailable || LOGO_TEXTURE == null) {
            return;
        }
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
            if (ClientCompat.isMinecraft1216OrAbove() && ClientCompat.shouldScaleLogo()) {
                ClientCompat.forceNearestSampler(LOGO_TEXTURE);
            }
            if (ClientCompat.isMinecraft1211OrAbove() || ClientCompat.isMinecraft1201Through1210()) {
                ClientCompat.drawTexture(context, LOGO_TEXTURE, logoX, logoY, logoWidth, logoHeight,
                        logoTextureWidth, logoTextureHeight);
            } else {
                ClientCompat.prepareGuiTexture(LOGO_TEXTURE);
                boolean manualOk = ClientCompat.drawTextureManual(context, LOGO_TEXTURE, logoX, logoY, logoWidth, logoHeight);
                if (!manualOk) {
                    ClientCompat.drawTextureLegacy(context, LOGO_TEXTURE, logoX, logoY, logoWidth, logoHeight,
                            logoTextureWidth, logoTextureHeight);
                }
            }
        } finally {
            if (matrices != null) {
                matrices.pop();
            }
            ClientCompat.depthMask(true);
            ClientCompat.enableDepthTest();
        }
    }

    public void renderBackgroundTexture(DrawContext context) {
        drawBackground(context);
    }

    public void renderInGameBackground(DrawContext context) {
        drawBackground(context);
    }

    private void ensureLogoLoaded() {
        if (logoLoadAttempted) {
            return;
        }
        logoLoadAttempted = true;
        try (InputStream in = RestartRequiredScreen.class.getResourceAsStream(LOGO_RESOURCE_PATH)) {
            if (in == null) {
                LOGGER.warn("A logó erőforrás nem található: {}", LOGO_RESOURCE_PATH);
                return;
            }
            NativeImage image = NativeImage.read(in);
            if (ClientCompat.isMinecraft1216OrAbove() && ClientCompat.shouldScaleLogo()
                    && logoWidth > 0 && logoHeight > 0) {
                int targetWidth = Math.max(1, logoWidth);
                int targetHeight = Math.max(1, logoHeight);
                image = scaleNearest(image, targetWidth, targetHeight);
                logoTextureWidth = image.getWidth();
                logoTextureHeight = image.getHeight();
            }
            NativeImageBackedTexture texture = ClientCompat.createBackedTexture(image);
            if (texture != null && LOGO_TEXTURE != null) {
                MinecraftClient.getInstance().getTextureManager().registerTexture(LOGO_TEXTURE, texture);
                if (ClientCompat.isMinecraft1214OrBelow()) {
                    try {
                        texture.upload();
                    } catch (RuntimeException e) {
                        LOGGER.warn("A logó textúra feltöltése sikertelen.", e);
                    }
                }
                if (ClientCompat.shouldScaleLogo()) {
                    ClientCompat.applyNearestFilter(texture);
                }
                logoAvailable = true;
            } else {
                LOGGER.warn("A logó textúra létrehozása sikertelen. textúra={}, azonosító={}", texture, LOGO_TEXTURE);
            }
        } catch (IOException e) {
            LOGGER.warn("A logó betöltése sikertelen.", e);
        }
    }

    private static NativeImage scaleNearest(NativeImage source, int targetWidth, int targetHeight) {
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) {
            return source;
        }
        Method readMethod = resolveNativeImageReadMethod(source);
        Method writeMethod = resolveNativeImageWriteMethod(source);
        Method copyPixelsMethod = nativeImageCopyPixelsMethod;
        Boolean writeArgb = nativeImageWriteArgb;
        if (writeMethod == null) {
            LOGGER.warn("A NativeImage méretezése kihagyva: hiányzik a színelérési metódus.");
            return source;
        }
        NativeImage scaled = new NativeImage(targetWidth, targetHeight, true);
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        try {
            if (copyPixelsMethod != null && writeArgb != null) {
                int[] pixels = (int[]) copyPixelsMethod.invoke(source);
                if (pixels == null || pixels.length < srcWidth * srcHeight) {
                    LOGGER.warn("A NativeImage méretezése kihagyva: a copyPixels metódus érvénytelen adatot adott vissza.");
                    scaled.close();
                    return source;
                }
                boolean useArgb = Boolean.TRUE.equals(writeArgb);
                for (int y = 0; y < targetHeight; y++) {
                    int srcY = y * srcHeight / targetHeight;
                    for (int x = 0; x < targetWidth; x++) {
                        int srcX = x * srcWidth / targetWidth;
                        int color = pixels[srcY * srcWidth + srcX];
                        if (!useArgb) {
                            color = swapRedBlue(color);
                        }
                        writeMethod.invoke(scaled, x, y, color);
                    }
                }
            } else {
                if (readMethod == null || !verifyReadMethod(source, readMethod) || !verifyWriteMethod(writeMethod)) {
                    LOGGER.warn("A NativeImage méretezése kihagyva: a színelérés ellenőrzése sikertelen.");
                    scaled.close();
                    return source;
                }
                for (int y = 0; y < targetHeight; y++) {
                    int srcY = y * srcHeight / targetHeight;
                    for (int x = 0; x < targetWidth; x++) {
                        int srcX = x * srcWidth / targetWidth;
                        int color = (int) readMethod.invoke(source, srcX, srcY);
                        writeMethod.invoke(scaled, x, y, color);
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("A NativeImage méretezése sikertelen, visszatérés az eredetihez.", e);
            scaled.close();
            return source;
        }
        source.close();
        return scaled;
    }

    private static Method resolveNativeImageReadMethod(NativeImage image) {
        if (nativeImageMethodsChecked) {
            return nativeImageReadMethod;
        }
        nativeImageMethodsChecked = true;
        NativeImageMethods methods = resolveNativeImageMethods(image);
        nativeImageReadMethod = methods.read;
        nativeImageWriteMethod = methods.write;
        nativeImageCopyPixelsMethod = methods.copyPixels;
        nativeImageWriteArgb = methods.writeArgb;
        return nativeImageReadMethod;
    }

    private static Method resolveNativeImageWriteMethod(NativeImage image) {
        if (!nativeImageMethodsChecked) {
            resolveNativeImageReadMethod(image);
        }
        return nativeImageWriteMethod;
    }

    private static Method findNativeImageMethod(Class<?> imageClass, boolean read) {
        Method best = null;
        int bestScore = -1;
        Method[] methods = imageClass.getDeclaredMethods();
        best = selectNativeImageMethod(methods, read, best, bestScore);
        bestScore = best == null ? -1 : scoreNativeImageMethod(best, read);
        Method[] publicMethods = imageClass.getMethods();
        best = selectNativeImageMethod(publicMethods, read, best, bestScore);
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        return best;
    }

    private static NativeImageMethods resolveNativeImageMethods(NativeImage image) {
        Class<?> imageClass = image.getClass();
        Method[] declared = imageClass.getDeclaredMethods();
        Method[] publics = imageClass.getMethods();
        Method read = selectNativeImageMethod(declared, true, null, -1);
        int readScore = read == null ? -1 : scoreNativeImageMethod(read, true);
        read = selectNativeImageMethod(publics, true, read, readScore);
        Method write = selectNativeImageMethod(declared, false, null, -1);
        int writeScore = write == null ? -1 : scoreNativeImageMethod(write, false);
        write = selectNativeImageMethod(publics, false, write, writeScore);
        Method copyPixels = findCopyPixelsMethod(declared);
        if (copyPixels == null) {
            copyPixels = findCopyPixelsMethod(publics);
        }
        read = pickArgbReadMethod(image, read, copyPixels, declared, publics);
        if (read != null) {
            safeSetAccessible(read);
        }
        WriteMethodSelection writeSelection = pickMatchingWriteMethod(read, write, copyPixels, declared, publics);
        write = writeSelection.method;
        if (write != null) {
            safeSetAccessible(write);
        }
        if (copyPixels != null) {
            safeSetAccessible(copyPixels);
        }
        return new NativeImageMethods(read, write, copyPixels, writeSelection.writeArgb);
    }

    private static Method findCopyPixelsMethod(Method[] methods) {
        for (Method method : methods) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.getReturnType() != int[].class) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static Method pickArgbReadMethod(NativeImage image, Method fallback, Method copyPixels,
                                             Method[] declared, Method[] publics) {
        if (copyPixels == null) {
            return fallback;
        }
        try {
            safeSetAccessible(copyPixels);
            int[] pixels = (int[]) copyPixels.invoke(image);
            if (pixels == null || pixels.length == 0) {
                return fallback;
            }
            int width = image.getWidth();
            int sampleIndex = findSamplePixelIndex(pixels);
            if (sampleIndex < 0) {
                return fallback;
            }
            int expected = pixels[sampleIndex];
            int sampleX = sampleIndex % width;
            int sampleY = sampleIndex / width;
            Method argbRead = findReadMethodBySample(image, expected, sampleX, sampleY, declared);
            if (argbRead == null) {
                argbRead = findReadMethodBySample(image, expected, sampleX, sampleY, publics);
            }
            return argbRead != null ? argbRead : fallback;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return fallback;
    }

    private static Method findReadMethodBySample(NativeImage image, int expected, int x, int y, Method[] methods) {
        for (Method method : methods) {
            if (!matchesNativeImageMethod(method, true)) {
                continue;
            }
            try {
                safeSetAccessible(method);
                int value = (int) method.invoke(image, x, y);
                if (value == expected) {
                    return method;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static WriteMethodSelection pickMatchingWriteMethod(Method readMethod, Method fallback, Method copyPixels,
                                                                Method[] declared, Method[] publics) {
        if (readMethod == null) {
            return new WriteMethodSelection(fallback, null);
        }
        int testColor = 0xFF112233;
        Method match = findWriteMethodBySample(readMethod, testColor, declared);
        if (match == null) {
            match = findWriteMethodBySample(readMethod, testColor, publics);
        }
        if (match != null) {
            return new WriteMethodSelection(match, null);
        }
        if (copyPixels != null) {
            WriteMethodSelection pixelMatch = findWriteMethodByCopyPixels(copyPixels, declared);
            if (pixelMatch.method == null) {
                pixelMatch = findWriteMethodByCopyPixels(copyPixels, publics);
            }
            if (pixelMatch.method != null) {
                return pixelMatch;
            }
        }
        return new WriteMethodSelection(fallback, null);
    }

    private static Method findWriteMethodBySample(Method readMethod, int color, Method[] methods) {
        for (Method method : methods) {
            if (!matchesNativeImageMethod(method, false)) {
                continue;
            }
            NativeImage test = new NativeImage(1, 1, true);
            try {
                safeSetAccessible(method);
                safeSetAccessible(readMethod);
                method.invoke(test, 0, 0, color);
                int readBack = (int) readMethod.invoke(test, 0, 0);
                if (readBack == color) {
                    return method;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            } finally {
                test.close();
            }
        }
        return null;
    }

    private static WriteMethodSelection findWriteMethodByCopyPixels(Method copyPixels, Method[] methods) {
        int testColor = 0xFF112233;
        Method swapCandidate = null;
        for (Method method : methods) {
            if (!matchesNativeImageMethod(method, false)) {
                continue;
            }
            NativeImage test = new NativeImage(1, 1, true);
            try {
                safeSetAccessible(method);
                safeSetAccessible(copyPixels);
                method.invoke(test, 0, 0, testColor);
                int[] pixels = (int[]) copyPixels.invoke(test);
                if (pixels == null || pixels.length == 0) {
                    continue;
                }
                int value = pixels[0];
                if (value == testColor) {
                    return new WriteMethodSelection(method, Boolean.TRUE);
                }
                if (value == swapRedBlue(testColor)) {
                    if (swapCandidate == null) {
                        swapCandidate = method;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            } finally {
                test.close();
            }
        }
        if (swapCandidate != null) {
            return new WriteMethodSelection(swapCandidate, Boolean.FALSE);
        }
        return new WriteMethodSelection(null, null);
    }

    private static void safeSetAccessible(Method method) {
        try {
            method.setAccessible(true);
        } catch (RuntimeException ignored) {
        }
    }

    private record NativeImageMethods(Method read, Method write, Method copyPixels, Boolean writeArgb) {}

    private record WriteMethodSelection(Method method, Boolean writeArgb) {}

    private static Method selectNativeImageMethod(Method[] methods, boolean read, Method current, int currentScore) {
        Method best = current;
        int bestScore = currentScore;
        for (Method method : methods) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!matchesNativeImageMethod(method, read)) {
                continue;
            }
            int score = scoreNativeImageMethod(method, read);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static boolean matchesNativeImageMethod(Method method, boolean read) {
        Class<?>[] params = method.getParameterTypes();
        if (read) {
            return params.length == 2 && params[0] == int.class && params[1] == int.class
                    && method.getReturnType() == int.class;
        }
        return params.length == 3 && params[0] == int.class && params[1] == int.class && params[2] == int.class
                && method.getReturnType() == void.class;
    }

    private static int scoreNativeImageMethod(Method method, boolean read) {
        String name = method.getName().toLowerCase();
        int score = 10;
        if (read && name.contains("get")) {
            score += 3;
        }
        if (!read && name.contains("set")) {
            score += 3;
        }
        if (name.contains("pixel")) {
            score += 2;
        }
        if (name.contains("color")) {
            score += 2;
        }
        return score;
    }

    private static int findSamplePixelIndex(int[] pixels) {
        for (int i = 0; i < pixels.length; i++) {
            int value = pixels[i];
            int red = (value >> 16) & 0xFF;
            int blue = value & 0xFF;
            if (red != blue) {
                return i;
            }
        }
        return -1;
    }

    private static int swapRedBlue(int argb) {
        int alpha = argb & 0xFF000000;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return alpha | (blue << 16) | (green << 8) | red;
    }

    private static boolean verifyReadMethod(NativeImage image, Method readMethod) {
        try {
            readMethod.invoke(image, 0, 0);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("A NativeImage olvasási hozzáférése sikertelen.", e);
            return false;
        }
    }

    private static boolean verifyWriteMethod(Method writeMethod) {
        NativeImage test = new NativeImage(1, 1, true);
        try {
            writeMethod.invoke(test, 0, 0, 0xFFFFFFFF);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("A NativeImage írási hozzáférése sikertelen.", e);
            return false;
        } finally {
            test.close();
        }
    }

    private void updateMessage() {
        boolean downloading = ModInstaller.isDownloadInProgress();
        boolean downloadFailed = ModInstaller.hasDownloadFailed();
        boolean extraMods = ModInstaller.hasExtraModsDetected();
        if (downloading == lastDownloading && downloadFailed == lastDownloadFailed && extraMods == lastExtraModsDetected) {
            return;
        }
        lastDownloading = downloading;
        lastDownloadFailed = downloadFailed;
        lastExtraModsDetected = extraMods;

        if (downloading) {
            lineOne = Text.literal("Szükséges modok letöltése folyamatban...");
            lineTwo = Text.literal("Kérlek, várj...");
        } else if (extraMods) {
            lineOne = Text.literal("Érzékeltünk más modokat is,");
            lineTwo = Text.literal("így újra kell indítanod a játékot.");
        } else if (downloadFailed) {
            lineOne = Text.literal("A modok letöltése nem sikerült,");
            lineTwo = Text.literal("kérlek, ellenőrizd az internetkapcsolatot.");
        } else {
            lineOne = Text.literal("Telepítettük a modokat,");
            lineTwo = Text.literal("kérlek, indítsd újra a játékot.");
        }

        if (actionButton != null) {
            actionButton.setMessage(getActionLabel(extraMods));
            actionButton.visible = !downloading;
            actionButton.active = !downloading;
        }
    }

    private Text getActionLabel(boolean extraMods) {
        return extraMods ? BUTTON_DELETE : BUTTON_EXIT;
    }

    private void handleAction() {
        if (ModInstaller.hasExtraModsDetected()) {
            ModInstaller.requestExtraModDeletion();
        }
        MinecraftClient.getInstance().scheduleStop();
    }

    private void renderMessage(DrawContext context) {
        if (lineOne == null || lineTwo == null) {
            return;
        }
        context.drawCenteredTextWithShadow(this.textRenderer, lineOne, this.width / 2, messageY, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, lineTwo, this.width / 2,
                messageY + LINE_HEIGHT + LINE_SPACING, 0xFFFFFFFF);
    }
}
