package hu.ColorsASD.minewild.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Locale;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

public final class ClientCompat {
    public enum AutoConnectReadiness {
        READY,
        NOT_READY,
        UNKNOWN
    }
    private static final String PIPELINE_CLASS = "com.mojang.blaze3d.pipeline.RenderPipeline";
    private static final String PIPELINES_CLASS = "net.minecraft.client.gl.RenderPipelines";
    private static final String CONNECT_SCREEN_CLASS = "net.minecraft.client.gui.screen.multiplayer.ConnectScreen";
    private static final String CONNECT_SCREEN_CLASS_INTERMEDIARY = "net.minecraft.class_412";
    private static final String CONNECT_SCREEN_FALLBACK = "net.minecraft.client.gui.screen.ConnectScreen";
    private static final String ACCESSIBILITY_ONBOARDING_SCREEN_CLASS = "net.minecraft.client.gui.screen.AccessibilityOnboardingScreen";
    private static final String ACCESSIBILITY_ONBOARDING_SCREEN_CLASS_INTERMEDIARY = "net.minecraft.class_8032";
    private static final String FINISHED_LOADING_METHOD = "method_53466";
    private static final String FINISHED_LOADING_METHOD_NAMED = "isFinishedLoading";
    private static final String FINISHED_LOADING_FIELD = "field_45900";
    private static final String FINISHED_LOADING_FIELD_NAMED = "finishedLoading";
    private static final String RESOURCE_RELOAD_FUTURE_FIELD = "field_18174";
    private static final String RESOURCE_RELOAD_FUTURE_FIELD_NAMED = "resourceReloadFuture";
    private static final String ONBOARD_ACCESSIBILITY_FIELD = "onboardAccessibility";
    private static final String ONBOARD_ACCESSIBILITY_FIELD_INTERMEDIARY = "field_41785";
    private static final String SKIP_MULTIPLAYER_WARNING_FIELD = "skipMultiplayerWarning";
    private static final String SKIP_MULTIPLAYER_WARNING_FIELD_INTERMEDIARY = "field_21840";
    private static final String TUTORIAL_STEP_FIELD = "tutorialStep";
    private static final String COOKIE_STORAGE_CLASS = "net.minecraft.client.network.CookieStorage";
    private static final String RENDER_LAYER_CLASS = "net.minecraft.class_1921";
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final Logger LOGGER = LoggerFactory.getLogger("MinewildCompat");
    private static final int MIN_LAYER_SCORE = 40;

    private static final Method DRAW_TEXTURE_PIPELINE = findDrawTextureMethod(true);
    private static final Method DRAW_TEXTURE_ID = findDrawTextureMethod(false);
    private static final Method DRAW_TEXTURE_FUNCTION = findDrawTextureFunctionMethod();
    private static final Method DRAW_TEXTURE_QUAD = findDrawTexturedQuadMethod();
    private static final Method DRAW_TEXTURE_QUAD_ID = findDrawTexturedQuadIdMethod();
    private static final Object GUI_TEXTURED_PIPELINE = findGuiTexturedPipeline();
    private static final Method CONNECT_METHOD = findConnectMethod();
    private static boolean pipelineDrawFailedLogged = false;
    private static boolean idDrawFailedLogged = false;
    private static boolean functionDrawFailedLogged = false;
    private static volatile Object guiTextureFunction = null;
    private static boolean guiTextureFunctionChecked = false;
    private static Method textureFilterMethod;
    private static boolean textureFilterChecked;
    private static Method gpuTextureGetterMethod;
    private static boolean gpuTextureGetterChecked;
    private static Method gpuFilterMethod;
    private static boolean gpuFilterChecked;
    private static Object filterNearest;
    private static boolean filterNearestChecked;
    private static String minecraftVersion;
    private static boolean minecraftVersionChecked;
    private static boolean minecraftVersionPartsChecked;
    private static boolean minecraftVersionPartsValid;
    private static int minecraftVersionMajor;
    private static int minecraftVersionMinor;
    private static int minecraftVersionPatch;
    private static Method guiTexturedMethodLegacy;
    private static boolean guiTexturedMethodChecked;
    private static Method setPanoramaMethod;
    private static boolean setPanoramaMethodChecked;
    private static Method legacyDrawTextureMethod;
    private static boolean legacyDrawTextureChecked;
    private static Method positionTexShaderMethod;
    private static boolean positionTexShaderChecked;
    private static Method disableScissorMethod;
    private static boolean disableScissorChecked;
    private static Method setScissorMethod;
    private static boolean setScissorChecked;
    private static Field scissorStackField;
    private static boolean scissorStackFieldChecked;
    private static Field scissorDequeField;
    private static boolean scissorDequeFieldChecked;
    private static Method drawGuiTextureMethod;
    private static boolean drawGuiTextureChecked;
    private static Method drawGuiTextureNamedMethod;
    private static boolean drawGuiTextureNamedChecked;
    private static Method bufferRendererDrawMethod;
    private static boolean bufferRendererDrawChecked;
    private static Method depthTestToggleMethod;
    private static boolean depthTestToggleChecked;
    private static Method blendToggleMethod;
    private static boolean blendToggleChecked;
    private static Method defaultBlendFuncMethod;
    private static boolean defaultBlendFuncChecked;
    private static Method blendFuncMethod;
    private static boolean blendFuncChecked;
    private static Method blendFuncSeparateMethod;
    private static boolean blendFuncSeparateChecked;
    private static Method shaderColorMethod;
    private static boolean shaderColorChecked;
    private static Method drawContextMatricesMethod;
    private static boolean drawContextMatricesChecked;
    private static Constructor<?> cookieStorageConstructor;
    private static boolean cookieStorageChecked;
    private static Method finishedLoadingMethod;
    private static boolean finishedLoadingMethodChecked;
    private static Field finishedLoadingField;
    private static boolean finishedLoadingFieldChecked;
    private static Field resourceReloadFutureField;
    private static boolean resourceReloadFutureFieldChecked;
    private static Field onboardAccessibilityField;
    private static boolean onboardAccessibilityFieldChecked;
    private static Field skipMultiplayerWarningField;
    private static boolean skipMultiplayerWarningFieldChecked;
    private static Field tutorialStepField;
    private static boolean tutorialStepFieldChecked;
    private static Method currentServerEntryMethod;
    private static boolean currentServerEntryMethodChecked;
    private static Field serverInfoAddressField;
    private static boolean serverInfoAddressFieldChecked;

    private ClientCompat() {
    }

    public static Identifier id(String namespace, String path) {
        Identifier resolved = tryIdentifierOf(namespace, path);
        if (resolved != null) {
            return resolved;
        }
        resolved = tryIdentifierConstructor(namespace, path);
        if (resolved != null) {
            return resolved;
        }
        return tryIdentifierParse(namespace + ":" + path);
    }

    public static boolean isMinecraft1215() {
        return isMinecraftAtLeast(1, 21, 5) && isMinecraftAtMost(1, 21, 5);
    }

    public static boolean isMinecraft1211OrBelow() {
        return isMinecraftAtMost(1, 21, 1);
    }

    public static boolean isMinecraft1216OrAbove() {
        return isMinecraftAtLeast(1, 21, 6);
    }

    public static boolean isMinecraft1211OrAbove() {
        return isMinecraftAtLeast(1, 21, 1);
    }

    public static boolean isConnectScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        Class<?> connectScreenClass = findConnectScreenClass();
        if (connectScreenClass != null && connectScreenClass.isInstance(screen)) {
            return true;
        }
        String className = screen.getClass().getName();
        if (CONNECT_SCREEN_CLASS.equals(className)
                || CONNECT_SCREEN_CLASS_INTERMEDIARY.equals(className)
                || CONNECT_SCREEN_FALLBACK.equals(className)
                || className.endsWith(".ConnectScreen")
                || className.endsWith(".class_412")
                || className.endsWith(".DownloadingTerrainScreen")
                || className.endsWith(".class_434")
                || className.endsWith(".ReconfiguringScreen")
                || className.endsWith(".class_8671")) {
            return true;
        }
        TextContent content = screen.getTitle().getContent();
        if (content instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            return "connect.connecting".equals(key)
                    || "connect.authorizing".equals(key)
                    || "connect.joining".equals(key)
                    || "multiplayer.downloadingTerrain".equals(key)
                    || "connect.reconfiguring".equals(key)
                    || "connect.reconfiging".equals(key);
        }
        return false;
    }

    public static boolean isMinecraft1201Through1210() {
        return isMinecraftAtLeast(1, 20, 1) && isMinecraftAtMost(1, 21, 0);
    }


    public static boolean isMinecraft1215OrBelow() {
        return isMinecraftAtMost(1, 21, 5);
    }

    public static boolean isMinecraft1214OrBelow() {
        return isMinecraftAtMost(1, 21, 4);
    }

    public static AutoConnectReadiness getAutoConnectReadiness(MinecraftClient client) {
        if (client == null) {
            return AutoConnectReadiness.NOT_READY;
        }
        Boolean finished = readFinishedLoading(client);
        if (Boolean.FALSE.equals(finished)) {
            return AutoConnectReadiness.NOT_READY;
        }
        Boolean reloadDone = readResourceReloadDone(client);
        if (Boolean.FALSE.equals(reloadDone)) {
            return AutoConnectReadiness.NOT_READY;
        }
        if (finished == null && reloadDone == null) {
            return AutoConnectReadiness.UNKNOWN;
        }
        return AutoConnectReadiness.READY;
    }

    public static boolean isClientReadyForAutoConnect(MinecraftClient client) {
        return getAutoConnectReadiness(client) == AutoConnectReadiness.READY;
    }

    public static boolean isAccessibilityOnboardingScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        Class<?> current = screen.getClass();
        while (current != null && current != Object.class) {
            if (isAccessibilityOnboardingClassName(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    public static boolean setOnboardAccessibility(GameOptions options, boolean enabled) {
        return setGameOptionsBoolean(getOnboardAccessibilityField(), options, enabled);
    }

    public static boolean setSkipMultiplayerWarning(GameOptions options, boolean enabled) {
        return setGameOptionsBoolean(getSkipMultiplayerWarningField(), options, enabled);
    }

    public static boolean setTutorialStepNone(GameOptions options) {
        if (options == null) {
            return false;
        }
        Field field = getTutorialStepField(options);
        if (field == null) {
            return false;
        }
        try {
            if (field.getType().isEnum()) {
                Object noneValue = findEnumConstant(field.getType().getEnumConstants(), "NONE");
                if (noneValue == null) {
                    return false;
                }
                Object current = field.get(options);
                if (noneValue.equals(current)) {
                    return false;
                }
                field.set(options, noneValue);
                return true;
            }

            Object value = field.get(options);
            if (value instanceof SimpleOption<?> option) {
                return setSimpleOptionEnumToNone(option);
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
        }
        return false;
    }

    public static String tryGetCurrentServerAddress(MinecraftClient client) {
        ServerInfo info = tryGetCurrentServerInfo(client);
        if (info == null) {
            return null;
        }
        return tryReadServerInfoAddress(info);
    }

    private static ServerInfo tryGetCurrentServerInfo(MinecraftClient client) {
        if (client == null) {
            return null;
        }
        Method method = getCurrentServerEntryMethod();
        if (method != null) {
            try {
                Object value = method.invoke(client);
                if (value instanceof ServerInfo serverInfo) {
                    return serverInfo;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static boolean isAccessibilityOnboardingClassName(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        if (ACCESSIBILITY_ONBOARDING_SCREEN_CLASS.equals(className)
                || ACCESSIBILITY_ONBOARDING_SCREEN_CLASS_INTERMEDIARY.equals(className)) {
            return true;
        }
        int separator = className.lastIndexOf('.');
        String simpleName = separator >= 0 ? className.substring(separator + 1) : className;
        return "AccessibilityOnboardingScreen".equals(simpleName) || "class_8032".equals(simpleName);
    }

    private static boolean setGameOptionsBoolean(Field field, GameOptions options, boolean enabled) {
        if (field == null || options == null) {
            return false;
        }
        try {
            boolean current = field.getBoolean(options);
            if (current == enabled) {
                return false;
            }
            field.setBoolean(options, enabled);
            return true;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return false;
        }
    }

    private static Field getOnboardAccessibilityField() {
        if (onboardAccessibilityFieldChecked) {
            return onboardAccessibilityField;
        }
        onboardAccessibilityFieldChecked = true;
        onboardAccessibilityField = findGameOptionsBooleanField(
                ONBOARD_ACCESSIBILITY_FIELD,
                ONBOARD_ACCESSIBILITY_FIELD_INTERMEDIARY
        );
        return onboardAccessibilityField;
    }

    private static Field getSkipMultiplayerWarningField() {
        if (skipMultiplayerWarningFieldChecked) {
            return skipMultiplayerWarningField;
        }
        skipMultiplayerWarningFieldChecked = true;
        skipMultiplayerWarningField = findGameOptionsBooleanField(
                SKIP_MULTIPLAYER_WARNING_FIELD,
                SKIP_MULTIPLAYER_WARNING_FIELD_INTERMEDIARY
        );
        return skipMultiplayerWarningField;
    }

    private static Field getTutorialStepField(GameOptions options) {
        if (tutorialStepFieldChecked) {
            return tutorialStepField;
        }
        tutorialStepFieldChecked = true;
        tutorialStepField = findTutorialStepField(options);
        return tutorialStepField;
    }

    private static Field findGameOptionsBooleanField(String... names) {
        if (names == null || names.length == 0) {
            return null;
        }
        for (String name : names) {
            Field field = findBooleanField(GameOptions.class, name);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    private static Field findBooleanField(Class<?> owner, String name) {
        if (owner == null || name == null || name.isBlank()) {
            return null;
        }
        Class<?> current = owner;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException | RuntimeException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Field findTutorialStepField(GameOptions options) {
        Field named = findDeclaredField(GameOptions.class, TUTORIAL_STEP_FIELD);
        if (named != null && looksLikeTutorialField(named, options)) {
            return named;
        }

        Field best = null;
        int bestScore = -1;
        Class<?> current = GameOptions.class;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                int score = scoreTutorialField(field, options);
                if (score > bestScore) {
                    bestScore = score;
                    best = field;
                }
            }
            current = current.getSuperclass();
        }
        return bestScore >= 3 ? best : null;
    }

    private static int scoreTutorialField(Field field, GameOptions options) {
        if (field == null) {
            return -1;
        }
        boolean accessible = trySetAccessible(field);
        if (!accessible) {
            return -1;
        }

        int score = 0;
        String fieldName = field.getName().toLowerCase(Locale.ROOT);
        if (fieldName.contains("tutorial")) {
            score += 4;
        }

        Class<?> type = field.getType();
        if (type.isEnum()) {
            int enumScore = scoreTutorialEnum(type);
            if (enumScore < 0) {
                return -1;
            }
            return score + enumScore;
        }

        if (!SimpleOption.class.isAssignableFrom(type) || options == null) {
            return -1;
        }

        try {
            Object value = field.get(options);
            if (!(value instanceof SimpleOption<?> option)) {
                return -1;
            }
            Object current = option.getValue();
            if (!(current instanceof Enum<?> currentEnum)) {
                return -1;
            }
            int enumScore = scoreTutorialEnum(currentEnum.getClass());
            if (enumScore < 0) {
                return -1;
            }
            return score + enumScore + 2;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return -1;
        }
    }

    private static int scoreTutorialEnum(Class<?> enumType) {
        if (enumType == null || !enumType.isEnum()) {
            return -1;
        }
        Object[] values = enumType.getEnumConstants();
        if (values == null || values.length == 0) {
            return -1;
        }
        if (findEnumConstant(values, "NONE") == null) {
            return -1;
        }

        int score = 1;
        String typeName = enumType.getSimpleName().toLowerCase(Locale.ROOT);
        if (typeName.contains("tutorial")) {
            score += 4;
        }
        if (findEnumConstant(values, "MOVEMENT") != null) {
            score += 3;
        }
        if (findEnumConstant(values, "OPEN_INVENTORY") != null) {
            score += 2;
        }
        if (findEnumConstant(values, "CRAFT_PLANKS") != null) {
            score += 1;
        }
        return score;
    }

    private static boolean looksLikeTutorialField(Field field, GameOptions options) {
        return scoreTutorialField(field, options) >= 3;
    }

    private static Field findDeclaredField(Class<?> owner, String name) {
        if (owner == null || name == null || name.isBlank()) {
            return null;
        }
        Class<?> current = owner;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                if (trySetAccessible(field)) {
                    return field;
                }
            } catch (NoSuchFieldException | RuntimeException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean trySetAccessible(Field field) {
        if (field == null) {
            return false;
        }
        try {
            field.setAccessible(true);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean setSimpleOptionEnumToNone(SimpleOption<?> option) {
        if (option == null) {
            return false;
        }
        Object current = option.getValue();
        if (!(current instanceof Enum<?> currentEnum)) {
            return false;
        }
        Object noneValue = findEnumConstant(currentEnum.getClass().getEnumConstants(), "NONE");
        if (noneValue == null || noneValue.equals(currentEnum)) {
            return false;
        }
        return setSimpleOptionValue(option, noneValue);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean setSimpleOptionValue(SimpleOption<?> option, Object value) {
        try {
            ((SimpleOption) option).setValue(value);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Method getCurrentServerEntryMethod() {
        if (currentServerEntryMethodChecked) {
            return currentServerEntryMethod;
        }
        currentServerEntryMethodChecked = true;
        for (Method method : MinecraftClient.class.getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (!ServerInfo.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            currentServerEntryMethod = method;
            break;
        }
        if (currentServerEntryMethod != null) {
            try {
                currentServerEntryMethod.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        return currentServerEntryMethod;
    }

    private static String tryReadServerInfoAddress(ServerInfo info) {
        if (info == null) {
            return null;
        }
        Field field = getServerInfoAddressField();
        if (field != null) {
            try {
                Object value = field.get(info);
                if (value instanceof String address) {
                    return address;
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        for (Method method : info.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != String.class) {
                continue;
            }
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("address") && !name.contains("ip") && !name.contains("host")) {
                continue;
            }
            try {
                Object value = method.invoke(info);
                if (value instanceof String address) {
                    return address;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        for (Field candidate : info.getClass().getDeclaredFields()) {
            if (candidate.getType() != String.class) {
                continue;
            }
            try {
                candidate.setAccessible(true);
                Object value = candidate.get(info);
                if (value instanceof String address && looksLikeServerAddress(address)) {
                    return address;
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static Field getServerInfoAddressField() {
        if (serverInfoAddressFieldChecked) {
            return serverInfoAddressField;
        }
        serverInfoAddressFieldChecked = true;
        Field field = null;
        try {
            field = ServerInfo.class.getDeclaredField("address");
        } catch (NoSuchFieldException | RuntimeException ignored) {
        }
        if (field == null) {
            try {
                field = ServerInfo.class.getDeclaredField("field_3761");
            } catch (NoSuchFieldException | RuntimeException ignored) {
            }
        }
        if (field != null) {
            try {
                field.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        serverInfoAddressField = field;
        return field;
    }

    private static boolean looksLikeServerAddress(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.indexOf(' ') >= 0) {
            return false;
        }
        return trimmed.contains(".") || trimmed.contains(":");
    }

    private static Boolean readFinishedLoading(MinecraftClient client) {
        Method method = getFinishedLoadingMethod();
        if (method != null) {
            try {
                Object value = method.invoke(client);
                if (value instanceof Boolean flag) {
                    return flag;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        Field field = getFinishedLoadingField();
        if (field != null) {
            try {
                Object value = field.get(client);
                if (value instanceof Boolean flag) {
                    return flag;
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static Boolean readResourceReloadDone(MinecraftClient client) {
        Field field = getResourceReloadFutureField();
        if (field == null) {
            return null;
        }
        try {
            Object value = field.get(client);
            if (value instanceof CompletableFuture<?> future) {
                return future.isDone();
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
        }
        return null;
    }

    private static Method getFinishedLoadingMethod() {
        if (finishedLoadingMethodChecked) {
            return finishedLoadingMethod;
        }
        finishedLoadingMethodChecked = true;
        Method method = findBooleanNoArgMethod(FINISHED_LOADING_METHOD);
        if (method == null) {
            method = findBooleanNoArgMethod(FINISHED_LOADING_METHOD_NAMED);
        }
        if (method != null) {
            finishedLoadingMethod = method;
        }
        return finishedLoadingMethod;
    }

    private static Field getFinishedLoadingField() {
        if (finishedLoadingFieldChecked) {
            return finishedLoadingField;
        }
        finishedLoadingFieldChecked = true;
        Field field = findField(FINISHED_LOADING_FIELD);
        if (field == null) {
            field = findField(FINISHED_LOADING_FIELD_NAMED);
        }
        finishedLoadingField = field;
        return field;
    }

    private static Field getResourceReloadFutureField() {
        if (resourceReloadFutureFieldChecked) {
            return resourceReloadFutureField;
        }
        resourceReloadFutureFieldChecked = true;
        Field field = findField(RESOURCE_RELOAD_FUTURE_FIELD);
        if (field == null) {
            field = findField(RESOURCE_RELOAD_FUTURE_FIELD_NAMED);
        }
        resourceReloadFutureField = field;
        return field;
    }

    private static Method findBooleanNoArgMethod(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            Method method = MinecraftClient.class.getDeclaredMethod(name);
            if (method.getParameterCount() == 0
                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                method.setAccessible(true);
                return method;
            }
        } catch (NoSuchMethodException | RuntimeException ignored) {
        }
        try {
            Method method = MinecraftClient.class.getMethod(name);
            if (method.getParameterCount() == 0
                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                return method;
            }
        } catch (NoSuchMethodException | RuntimeException ignored) {
        }
        return null;
    }

    private static Field findField(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            Field field = MinecraftClient.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | RuntimeException ignored) {
        }
        return null;
    }

    private static String getMinecraftVersion() {
        if (minecraftVersionChecked) {
            return minecraftVersion;
        }
        minecraftVersionChecked = true;
        try {
            FabricLoader.getInstance().getModContainer("minecraft")
                    .ifPresent(container -> minecraftVersion = container.getMetadata().getVersion().getFriendlyString());
        } catch (RuntimeException ignored) {
        }
        return minecraftVersion;
    }

    private static boolean loadMinecraftVersionParts() {
        if (minecraftVersionPartsChecked) {
            return minecraftVersionPartsValid;
        }
        minecraftVersionPartsChecked = true;
        int[] parts = parseVersionParts(getMinecraftVersion());
        if (parts == null) {
            return false;
        }
        minecraftVersionMajor = parts[0];
        minecraftVersionMinor = parts[1];
        minecraftVersionPatch = parts[2];
        minecraftVersionPartsValid = true;
        return true;
    }

    private static int[] parseVersionParts(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String[] raw = version.split("\\.");
        Integer major = parseVersionPart(raw, 0);
        if (major == null) {
            return null;
        }
        Integer minor = parseVersionPart(raw, 1);
        if (minor == null) {
            minor = 0;
        }
        Integer patch = parseVersionPart(raw, 2);
        if (patch == null) {
            patch = 0;
        }
        return new int[]{major, minor, patch};
    }

    private static Integer parseVersionPart(String[] raw, int index) {
        if (raw == null || index < 0 || index >= raw.length) {
            return null;
        }
        String part = raw[index];
        if (part == null || part.isBlank()) {
            return null;
        }
        int value = 0;
        boolean hasDigit = false;
        for (int i = 0; i < part.length(); i++) {
            char ch = part.charAt(i);
            if (ch >= '0' && ch <= '9') {
                hasDigit = true;
                value = value * 10 + (ch - '0');
            } else if (hasDigit) {
                break;
            }
        }
        return hasDigit ? value : null;
    }

    private static boolean isMinecraftAtLeast(int major, int minor, int patch) {
        if (!loadMinecraftVersionParts()) {
            return false;
        }
        if (minecraftVersionMajor != major) {
            return minecraftVersionMajor > major;
        }
        if (minecraftVersionMinor != minor) {
            return minecraftVersionMinor > minor;
        }
        return minecraftVersionPatch >= patch;
    }

    private static boolean isMinecraftAtMost(int major, int minor, int patch) {
        if (!loadMinecraftVersionParts()) {
            return true;
        }
        if (minecraftVersionMajor != major) {
            return minecraftVersionMajor < major;
        }
        if (minecraftVersionMinor != minor) {
            return minecraftVersionMinor < minor;
        }
        return minecraftVersionPatch <= patch;
    }

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height,
                                   int textureWidth, int textureHeight) {
        if (context == null || texture == null) {
            return;
        }
        boolean pipelineOk = false;
        if (DRAW_TEXTURE_PIPELINE != null && GUI_TEXTURED_PIPELINE != null) {
            pipelineOk = invokeDrawTexturePipeline(context, texture, x, y, width, height, textureWidth, textureHeight);
            if (!pipelineOk && !pipelineDrawFailedLogged) {
                pipelineDrawFailedLogged = true;
                LOGGER.warn("A textúrarajzoló pipeline hívása sikertelen, visszaállás az azonosító metódusra.");
            }
        }
        boolean functionOk = false;
        if (!pipelineOk && DRAW_TEXTURE_FUNCTION != null) {
            Object function = getGuiTextureFunction();
            if (function != null) {
                functionOk = invokeDrawTextureFunction(context, function, texture, x, y, width, height, textureWidth, textureHeight);
                if (!functionOk && DRAW_TEXTURE_QUAD != null && DRAW_TEXTURE_PIPELINE == null) {
                    functionOk = invokeDrawTextureQuad(context, function, texture, x, y, width, height);
                }
            }
            if (!functionOk && !functionDrawFailedLogged) {
                functionDrawFailedLogged = true;
                LOGGER.warn("A textúrarajzoló függvényhívás sikertelen, visszaállás az azonosító metódusra.");
            }
        }
        if (!pipelineOk && !functionOk) {
            boolean quadOk = false;
            if (isMinecraft1211OrBelow() && DRAW_TEXTURE_QUAD_ID != null) {
                quadOk = invokeDrawTextureQuadId(context, texture, x, y, width, height);
            }
            if (!quadOk) {
                boolean idOk = invokeDrawTextureId(context, texture, x, y, width, height, textureWidth, textureHeight);
                if (!idOk && !idDrawFailedLogged) {
                    idDrawFailedLogged = true;
                    LOGGER.warn("A textúrarajzoló azonosítóhívás sikertelen, a logó lehet, hogy nem jelenik meg.");
                }
            }
        }
    }

    public static boolean drawGuiTextureCompat(DrawContext context, Identifier texture, int x, int y, int width, int height,
                                               int textureWidth, int textureHeight) {
        if (context == null || texture == null) {
            return false;
        }
        Method method = getDrawGuiTextureNamedMethod();
        if (method == null) {
            method = getDrawGuiTextureMethod();
        }
        if (method == null) {
            return false;
        }
        Object function = getGuiTextureFunction();
        if (function == null) {
            return false;
        }
        Object[] args = buildDrawGuiTextureArgs(method, function, texture, x, y, width, height, textureWidth, textureHeight);
        if (args == null) {
            return false;
        }
        try {
            method.invoke(context, args);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean drawTextureManual(DrawContext context, Identifier texture,
                                            int x, int y, int width, int height) {
        if (context == null || texture == null) {
            return false;
        }
        try {
            ShaderProgram program = resolvePositionTexShader();
            if (program != null) {
                RenderSystem.setShader(() -> program);
            }
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            float x1 = x;
            float y1 = y;
            float x2 = x + width;
            float y2 = y + height;
            buffer.vertex(matrix, x1, y2, 0.0f).texture(0.0f, 1.0f).next();
            buffer.vertex(matrix, x2, y2, 0.0f).texture(1.0f, 1.0f).next();
            buffer.vertex(matrix, x2, y1, 0.0f).texture(1.0f, 0.0f).next();
            buffer.vertex(matrix, x1, y1, 0.0f).texture(0.0f, 0.0f).next();
            Object built;
            try {
                built = buffer.end();
            } catch (RuntimeException e) {
                return false;
            }
            return drawBuiltBuffer(built);
        } catch (NoSuchMethodError e) {
            return false;
        }
    }

    public static void drawTextureLegacy(DrawContext context, Identifier texture, int x, int y, int width, int height,
                                         int textureWidth, int textureHeight) {
        if (context == null || texture == null) {
            return;
        }
        Method method = getLegacyDrawTextureMethod();
        if (method == null) {
            drawTexture(context, texture, x, y, width, height, textureWidth, textureHeight);
            return;
        }
        Object[] args = buildLegacyDrawArgs(method, texture, x, y, width, height, textureWidth, textureHeight);
        if (args == null) {
            drawTexture(context, texture, x, y, width, height, textureWidth, textureHeight);
            return;
        }
        try {
            method.invoke(context, args);
            if (shouldTryLegacyAltArgs(method)) {
                Object[] altArgs = buildLegacyDrawArgsAlt(method, texture, x, y, width, height, textureWidth, textureHeight);
                if (altArgs != null) {
                    method.invoke(context, altArgs);
                }
            }
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            drawTexture(context, texture, x, y, width, height, textureWidth, textureHeight);
        }
    }

    public static void prepareGuiTexture(Identifier texture) {
        if (texture == null) {
            return;
        }
        ShaderProgram program = resolvePositionTexShader();
        if (program != null) {
            try {
                RenderSystem.setShader(() -> program);
            } catch (NoSuchMethodError | RuntimeException ignored) {
            }
        }
        try {
            RenderSystem.setShaderTexture(0, texture);
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
        enableBlend();
        defaultBlendFunc();
    }

    public static MatrixStack getMatrixStack(DrawContext context) {
        if (context == null) {
            return null;
        }
        Method method = getDrawContextMatricesMethod(context.getClass());
        if (method == null) {
            return null;
        }
        try {
            return (MatrixStack) method.invoke(context);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    public static void disableDepthTest() {
        try {
            RenderSystem.disableDepthTest();
            return;
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
        invokeDepthTestToggle(false);
    }

    public static void enableDepthTest() {
        try {
            RenderSystem.enableDepthTest();
            return;
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
        invokeDepthTestToggle(true);
    }

    public static void depthMask(boolean enabled) {
        try {
            RenderSystem.depthMask(enabled);
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
    }

    public static void enableBlend() {
        try {
            RenderSystem.enableBlend();
            return;
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
        invokeBlendToggle(true);
    }

    public static void defaultBlendFunc() {
        try {
            RenderSystem.defaultBlendFunc();
            return;
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
        Method method = getDefaultBlendFuncMethod();
        if (method != null) {
            try {
                method.invoke(null);
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        invokeBlendFuncFallback();
    }

    public static void setShaderColor(float red, float green, float blue, float alpha) {
        try {
            RenderSystem.setShaderColor(red, green, blue, alpha);
            return;
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
        Method method = getShaderColorMethod();
        if (method == null) {
            return;
        }
        try {
            method.invoke(null, red, green, blue, alpha);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static void disableScissor(DrawContext context) {
        if (context == null) {
            disableScissorFallback();
            return;
        }
        Method method = getDisableScissorMethod(context.getClass());
        if (method == null) {
            disableScissorFallback();
            return;
        }
        try {
            method.invoke(context);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            disableScissorFallback();
        }
    }

    public static void resetScissor(DrawContext context, int width, int height) {
        clearScissorStack(context);
        invokeDisableScissor(context);
        disableScissorFallback();
    }

    private static void disableScissorFallback() {
        try {
            RenderSystem.disableScissor();
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
    }

    private static boolean invokeDisableScissor(DrawContext context) {
        try {
            Method method = getDisableScissorMethod(context.getClass());
            if (method == null) {
                return false;
            }
            method.invoke(context);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            disableScissorFallback();
            return true;
        }
    }

    private static boolean invokeSetScissor(DrawContext context, int width, int height) {
        Method method = getSetScissorMethod(context.getClass());
        if (method == null) {
            return false;
        }
        try {
            method.invoke(context, 0, 0, width, height);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static void invokeRenderSystemScissor(int width, int height) {
        try {
            RenderSystem.enableScissor(0, 0, width, height);
        } catch (NoSuchMethodError | RuntimeException ignored) {
        }
    }

    private static void invokeDepthTestToggle(boolean enabled) {
        Method method = getDepthTestToggleMethod();
        if (method == null) {
            return;
        }
        try {
            method.invoke(null, enabled);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void invokeBlendToggle(boolean enabled) {
        Method method = getBlendToggleMethod();
        if (method == null) {
            return;
        }
        try {
            method.invoke(null, enabled);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void invokeBlendFuncFallback() {
        Method separate = getBlendFuncSeparateMethod();
        if (separate != null) {
            try {
                separate.invoke(null, 770, 771, 1, 0);
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        Method func = getBlendFuncMethod();
        if (func == null) {
            return;
        }
        try {
            func.invoke(null, 770, 771);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static Method getBlendToggleMethod() {
        if (blendToggleChecked) {
            return blendToggleMethod;
        }
        blendToggleChecked = true;
        Method best = null;
        int bestScore = -1;
        for (Method method : RenderSystem.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || params[0] != boolean.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("blend")) {
                continue;
            }
            int score = 0;
            if (name.contains("blend")) {
                score += 3;
            }
            if (name.contains("enable")) {
                score += 2;
            }
            if (name.contains("set")) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        blendToggleMethod = best;
        return best;
    }

    private static Method getDefaultBlendFuncMethod() {
        if (defaultBlendFuncChecked) {
            return defaultBlendFuncMethod;
        }
        defaultBlendFuncChecked = true;
        Method best = null;
        int bestScore = -1;
        for (Method method : RenderSystem.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class || method.getParameterCount() != 0) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("blend")) {
                continue;
            }
            if (!name.contains("default")) {
                continue;
            }
            int score = 0;
            if (name.contains("default")) {
                score += 3;
            }
            if (name.contains("blend")) {
                score += 2;
            }
            if (name.contains("func")) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        defaultBlendFuncMethod = best;
        return best;
    }

    private static Method getBlendFuncMethod() {
        if (blendFuncChecked) {
            return blendFuncMethod;
        }
        blendFuncChecked = true;
        Method best = null;
        int bestScore = -1;
        for (Method method : RenderSystem.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 2 || params[0] != int.class || params[1] != int.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("blend")) {
                continue;
            }
            if (name.contains("separate")) {
                continue;
            }
            int score = 0;
            if (name.contains("blendfunc")) {
                score += 3;
            }
            if (name.contains("blend")) {
                score += 2;
            }
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        blendFuncMethod = best;
        return best;
    }

    private static Method getBlendFuncSeparateMethod() {
        if (blendFuncSeparateChecked) {
            return blendFuncSeparateMethod;
        }
        blendFuncSeparateChecked = true;
        Method best = null;
        int bestScore = -1;
        for (Method method : RenderSystem.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 4
                    || params[0] != int.class || params[1] != int.class
                    || params[2] != int.class || params[3] != int.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("blend") || !name.contains("separate")) {
                continue;
            }
            int score = 0;
            if (name.contains("blendfuncseparate")) {
                score += 4;
            }
            if (name.contains("blend")) {
                score += 2;
            }
            if (name.contains("separate")) {
                score += 2;
            }
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        blendFuncSeparateMethod = best;
        return best;
    }

    private static Method getShaderColorMethod() {
        if (shaderColorChecked) {
            return shaderColorMethod;
        }
        shaderColorChecked = true;
        Method best = null;
        int bestScore = -1;
        for (Method method : RenderSystem.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 4
                    || params[0] != float.class || params[1] != float.class
                    || params[2] != float.class || params[3] != float.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("color")) {
                continue;
            }
            int score = 0;
            if (name.contains("shader")) {
                score += 3;
            }
            if (name.contains("color")) {
                score += 2;
            }
            if (name.contains("set")) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        shaderColorMethod = best;
        return best;
    }

    private static Method getDepthTestToggleMethod() {
        if (depthTestToggleChecked) {
            return depthTestToggleMethod;
        }
        depthTestToggleChecked = true;
        Method best = null;
        int bestScore = -1;
        for (Method method : RenderSystem.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || params[0] != boolean.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("depth") || !name.contains("test")) {
                continue;
            }
            int score = 0;
            if (name.contains("depthtest")) {
                score += 6;
            }
            if (name.contains("depth")) {
                score += 3;
            }
            if (name.contains("test")) {
                score += 3;
            }
            if (name.contains("set")) {
                score += 2;
            }
            if (name.contains("enable")) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        depthTestToggleMethod = best;
        return best;
    }

    private static Method getDrawContextMatricesMethod(Class<?> contextClass) {
        if (drawContextMatricesChecked) {
            return drawContextMatricesMethod;
        }
        drawContextMatricesChecked = true;
        Method best = null;
        int bestScore = -1;
        Method[] declared = contextClass.getDeclaredMethods();
        Method[] publics = contextClass.getMethods();
        best = pickDrawContextMatricesMethod(declared, best, bestScore);
        bestScore = best == null ? -1 : scoreDrawContextMatricesMethod(best);
        best = pickDrawContextMatricesMethod(publics, best, bestScore);
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        drawContextMatricesMethod = best;
        return best;
    }

    private static Method pickDrawContextMatricesMethod(Method[] methods, Method current, int currentScore) {
        Method best = current;
        int bestScore = currentScore;
        for (Method method : methods) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.getReturnType() != MatrixStack.class) {
                continue;
            }
            int score = scoreDrawContextMatricesMethod(method);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static int scoreDrawContextMatricesMethod(Method method) {
        String name = method.getName().toLowerCase();
        int score = 0;
        if (name.contains("matrix")) {
            score += 3;
        }
        if (name.contains("matrices")) {
            score += 2;
        }
        if (name.contains("get")) {
            score += 1;
        }
        return score;
    }

    private static void clearScissorStack(DrawContext context) {
        if (context == null) {
            return;
        }
        Object stack = getScissorStack(context);
        if (stack == null) {
            return;
        }
        Object deque = getScissorDeque(stack);
        if (deque instanceof java.util.Deque<?> scissorDeque) {
            scissorDeque.clear();
        }
    }

    private static Object getScissorStack(DrawContext context) {
        if (scissorStackFieldChecked) {
            return getFieldValue(scissorStackField, context);
        }
        scissorStackFieldChecked = true;
        Field best = null;
        for (Field field : context.getClass().getDeclaredFields()) {
            String name = field.getName().toLowerCase();
            if (!name.contains("scissor")) {
                continue;
            }
            best = field;
            break;
        }
        if (best == null) {
            for (Field field : context.getClass().getDeclaredFields()) {
                String typeName = field.getType().getName().toLowerCase();
                if (typeName.contains("scissor")) {
                    best = field;
                    break;
                }
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        scissorStackField = best;
        return getFieldValue(scissorStackField, context);
    }

    private static Object getScissorDeque(Object stack) {
        if (stack == null) {
            return null;
        }
        if (scissorDequeFieldChecked) {
            return getFieldValue(scissorDequeField, stack);
        }
        scissorDequeFieldChecked = true;
        Field best = null;
        for (Field field : stack.getClass().getDeclaredFields()) {
            if (java.util.Deque.class.isAssignableFrom(field.getType())) {
                best = field;
                break;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        scissorDequeField = best;
        return getFieldValue(scissorDequeField, stack);
    }

    private static Object getFieldValue(Field field, Object target) {
        if (field == null || target == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method getDrawGuiTextureMethod() {
        if (drawGuiTextureChecked) {
            return drawGuiTextureMethod;
        }
        drawGuiTextureChecked = true;
        Method best = null;
        int bestScore = -1;
        Method[] declared = DrawContext.class.getDeclaredMethods();
        Method[] publics = DrawContext.class.getMethods();
        best = pickDrawGuiTextureMethod(declared, best, bestScore);
        bestScore = best == null ? -1 : scoreDrawGuiTextureMethod(best);
        best = pickDrawGuiTextureMethod(publics, best, bestScore);
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        drawGuiTextureMethod = best;
        return best;
    }

    private static Method getDrawGuiTextureNamedMethod() {
        if (drawGuiTextureNamedChecked) {
            return drawGuiTextureNamedMethod;
        }
        drawGuiTextureNamedChecked = true;
        String[] names = {"drawGuiTexture", "method_52706", "method_52707", "method_52708"};
        Method[] declared = DrawContext.class.getDeclaredMethods();
        Method[] publics = DrawContext.class.getMethods();
        Method best = findDrawGuiTextureByName(declared, names);
        if (best == null) {
            best = findDrawGuiTextureByName(publics, names);
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        drawGuiTextureNamedMethod = best;
        return best;
    }

    private static Method findDrawGuiTextureByName(Method[] methods, String[] names) {
        for (Method method : methods) {
            String name = method.getName();
            if (!matchesName(name, names)) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length < 6 || params.length > 10) {
                continue;
            }
            if (!Function.class.isAssignableFrom(params[0])) {
                continue;
            }
            if (params[1] != Identifier.class) {
                continue;
            }
            if (!allInts(params, 2)) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static boolean matchesName(String name, String[] names) {
        for (String target : names) {
            if (target.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Method pickDrawGuiTextureMethod(Method[] methods, Method current, int currentScore) {
        Method best = current;
        int bestScore = currentScore;
        for (Method method : methods) {
            String name = method.getName().toLowerCase();
            if (!(name.contains("draw") && name.contains("texture")) && !name.contains("method_527")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length < 6 || params.length > 10) {
                continue;
            }
            if (!Function.class.isAssignableFrom(params[0])) {
                continue;
            }
            if (params[1] != Identifier.class) {
                continue;
            }
            if (!allInts(params, 2)) {
                continue;
            }
            int score = scoreDrawGuiTextureMethod(method);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static int scoreDrawGuiTextureMethod(Method method) {
        int count = method.getParameterCount();
        int score;
        if (count == 6) {
            score = 100;
        } else if (count == 7) {
            score = 90;
        } else if (count == 10) {
            score = 80;
        } else {
            score = 0;
        }
        String name = method.getName().toLowerCase();
        if (name.contains("drawguitexture")) {
            score += 5;
        }
        return score;
    }

    private static boolean drawBuiltBuffer(Object built) {
        if (built == null) {
            return false;
        }
        Method method = getBufferRendererDrawMethod(built.getClass());
        if (method == null) {
            return false;
        }
        try {
            method.invoke(null, built);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Method getBufferRendererDrawMethod(Class<?> builtClass) {
        if (bufferRendererDrawChecked && bufferRendererDrawMethod != null) {
            if (builtClass == null || bufferRendererDrawMethod.getParameterTypes()[0].isAssignableFrom(builtClass)) {
                return bufferRendererDrawMethod;
            }
        }
        if (bufferRendererDrawChecked && builtClass == null) {
            return bufferRendererDrawMethod;
        }
        bufferRendererDrawChecked = true;
        Method best = null;
        String[] names = {"drawWithGlobalProgram", "drawWithShader", "draw"};
        for (Method method : BufferRenderer.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            String name = method.getName();
            if (!matchesName(name, names)) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (builtClass != null && !param.isAssignableFrom(builtClass)) {
                continue;
            }
            best = method;
            break;
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        bufferRendererDrawMethod = best;
        return best;
    }

    private static Object[] buildDrawGuiTextureArgs(Method method, Object function, Identifier texture,
                                                    int x, int y, int width, int height,
                                                    int textureWidth, int textureHeight) {
        int count = method.getParameterCount();
        if (count == 6) {
            return new Object[]{function, texture, x, y, width, height};
        }
        if (count == 7) {
            return new Object[]{function, texture, x, y, width, height, COLOR_WHITE};
        }
        if (count == 10) {
            return new Object[]{function, texture, textureWidth, textureHeight, 0, 0, x, y, width, height};
        }
        return null;
    }

    private static boolean allInts(Class<?>[] params, int startIndex) {
        for (int i = startIndex; i < params.length; i++) {
            if (!isInt(params[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean shouldScaleLogo() {
        return DRAW_TEXTURE_PIPELINE == null && (DRAW_TEXTURE_FUNCTION != null || DRAW_TEXTURE_ID != null);
    }

    public static String getMinecraftVersionString() {
        String version = getMinecraftVersion();
        if (version == null || version.isBlank()) {
            return "ismeretlen";
        }
        return version;
    }

    public static String getDrawTexturePipelineName() {
        return DRAW_TEXTURE_PIPELINE == null ? "nincs" : DRAW_TEXTURE_PIPELINE.getName();
    }

    public static String getDrawTextureFunctionName() {
        return DRAW_TEXTURE_FUNCTION == null ? "nincs" : DRAW_TEXTURE_FUNCTION.getName();
    }

    public static String getDrawTextureIdName() {
        return DRAW_TEXTURE_ID == null ? "nincs" : DRAW_TEXTURE_ID.getName();
    }

    public static String getDrawTextureQuadName() {
        return DRAW_TEXTURE_QUAD == null ? "nincs" : DRAW_TEXTURE_QUAD.getName();
    }

    public static String getLegacyDrawTextureName() {
        Method method = getLegacyDrawTextureMethod();
        return method == null ? "nincs" : method.getName();
    }

    public static String getGuiTexturePipelineName() {
        return GUI_TEXTURED_PIPELINE == null ? "nincs" : GUI_TEXTURED_PIPELINE.getClass().getName();
    }

    public static String getGuiTextureFunctionName() {
        Object function = getGuiTextureFunction();
        return function == null ? "nincs" : function.getClass().getName();
    }

    public static void applyNearestFilter(NativeImageBackedTexture texture) {
        applyNearestFilterUnsafe(texture);
    }

    public static void applyNearestFilter(Object texture) {
        applyNearestFilterUnsafe(texture);
    }

    public static void disablePanorama() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        Method method = getSetPanoramaMethod(client.getClass());
        if (method == null) {
            return;
        }
        try {
            method.invoke(client, false);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void applyNearestFilterUnsafe(Object texture) {
        if (texture == null) {
            return;
        }
        Method filterMethod = getTextureFilterMethod(texture.getClass());
        if (filterMethod == null) {
            return;
        }
        try {
            filterMethod.invoke(texture, false, false);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static void forceNearestSampler(Identifier textureId) {
        if (textureId == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        Object texture = client.getTextureManager().getTexture(textureId);
        if (texture == null) {
            return;
        }
        if (texture instanceof NativeImageBackedTexture nativeTexture) {
            try {
                nativeTexture.upload();
            } catch (RuntimeException ignored) {
            }
            applyNearestFilter(nativeTexture);
        } else {
            applyNearestFilter(texture);
        }
        applyGpuNearestFilter(texture);
    }

    private static void applyGpuNearestFilter(Object texture) {
        if (texture == null) {
            return;
        }
        Method getter = getGpuTextureGetter(texture.getClass());
        if (getter == null) {
            return;
        }
        Object gpuTexture;
        try {
            gpuTexture = getter.invoke(texture);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return;
        }
        if (gpuTexture == null) {
            return;
        }
        Method filterMethod = getGpuFilterMethod(gpuTexture.getClass());
        if (filterMethod == null) {
            return;
        }
        Object nearest = getFilterNearest();
        if (nearest == null) {
            return;
        }
        try {
            if (filterMethod.getParameterCount() == 2) {
                filterMethod.invoke(gpuTexture, nearest, false);
            } else {
                filterMethod.invoke(gpuTexture, nearest, nearest, false);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static NativeImageBackedTexture createBackedTexture(NativeImage image) {
        if (image == null) {
            return null;
        }
        try {
            Constructor<NativeImageBackedTexture> ctor =
                    NativeImageBackedTexture.class.getConstructor(Supplier.class, NativeImage.class);
            return ctor.newInstance((Supplier<String>) () -> "minewild_restart_logo", image);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Constructor<NativeImageBackedTexture> ctor = NativeImageBackedTexture.class.getConstructor(NativeImage.class);
            return ctor.newInstance(image);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Constructor<NativeImageBackedTexture> ctor =
                    NativeImageBackedTexture.class.getConstructor(NativeImage.class, boolean.class);
            return ctor.newInstance(image, false);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static boolean invokeFilterMethod(NativeImageBackedTexture texture, String name, boolean blur, boolean mipmap) {
        try {
            Method method = texture.getClass().getMethod(name, boolean.class, boolean.class);
            method.invoke(texture, blur, mipmap);
            return true;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method method = texture.getClass().getDeclaredMethod(name, boolean.class, boolean.class);
            method.setAccessible(true);
            method.invoke(texture, blur, mipmap);
            return true;
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private static Method getTextureFilterMethod(Class<?> textureClass) {
        if (textureFilterChecked) {
            return textureFilterMethod;
        }
        textureFilterChecked = true;
        Method best = null;
        int bestScore = -1;
        Class<?> current = textureClass;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] != boolean.class || params[1] != boolean.class) {
                    continue;
                }
                if (method.getReturnType() != void.class) {
                    continue;
                }
                int score = scoreFilterMethod(method);
                if (score > bestScore) {
                    bestScore = score;
                    best = method;
                }
            }
            current = current.getSuperclass();
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        textureFilterMethod = best;
        return best;
    }

    private static Method getGpuTextureGetter(Class<?> textureClass) {
        if (gpuTextureGetterChecked) {
            return gpuTextureGetterMethod;
        }
        gpuTextureGetterChecked = true;
        Method best = null;
        Class<?> current = textureClass;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                if (returnType == null) {
                    continue;
                }
                if (!"com.mojang.blaze3d.textures.GpuTexture".equals(returnType.getName())) {
                    continue;
                }
                best = method;
                break;
            }
            if (best != null) {
                break;
            }
            current = current.getSuperclass();
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        gpuTextureGetterMethod = best;
        return best;
    }

    private static Method getSetPanoramaMethod(Class<?> clientClass) {
        if (setPanoramaMethodChecked) {
            return setPanoramaMethod;
        }
        setPanoramaMethodChecked = true;
        Method method = findPanoramaSetter(clientClass, "setRenderingPanorama");
        if (method == null) {
            method = findPanoramaSetter(clientClass, "method_35770");
        }
        if (method == null) {
            for (Method candidate : clientClass.getDeclaredMethods()) {
                if (candidate.getParameterCount() != 1 || candidate.getReturnType() != void.class) {
                    continue;
                }
                if (candidate.getParameterTypes()[0] != boolean.class) {
                    continue;
                }
                if (!candidate.getName().toLowerCase().contains("panorama")) {
                    continue;
                }
                method = candidate;
                break;
            }
        }
        if (method != null) {
            try {
                method.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        setPanoramaMethod = method;
        return method;
    }

    private static Method findPanoramaSetter(Class<?> clientClass, String name) {
        try {
            Method method = clientClass.getDeclaredMethod(name, boolean.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            return clientClass.getMethod(name, boolean.class);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Method getGpuFilterMethod(Class<?> gpuTextureClass) {
        if (gpuFilterChecked) {
            return gpuFilterMethod;
        }
        gpuFilterChecked = true;
        Method best = null;
        for (Method method : gpuTextureClass.getMethods()) {
            if (!"setTextureFilter".equals(method.getName())) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 || params.length == 3) {
                best = method;
                break;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        gpuFilterMethod = best;
        return best;
    }

    private static Object getFilterNearest() {
        if (filterNearestChecked) {
            return filterNearest;
        }
        filterNearestChecked = true;
        try {
            Class<?> filterMode = Class.forName("com.mojang.blaze3d.textures.FilterMode");
            if (filterMode.isEnum()) {
                Object[] values = filterMode.getEnumConstants();
                for (Object value : values) {
                    if (value instanceof Enum<?> enumValue && "NEAREST".equals(enumValue.name())) {
                        filterNearest = value;
                        break;
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return filterNearest;
    }

    private static int scoreFilterMethod(Method method) {
        String name = method.getName().toLowerCase();
        int score = 0;
        if (name.contains("filter")) {
            score += 5;
        }
        if (name.contains("blur")) {
            score += 3;
        }
        if (name.contains("mipmap")) {
            score += 2;
        }
        if (name.startsWith("set")) {
            score += 1;
        }
        return score;
    }

    public static boolean connectToServer(Screen parent, MinecraftClient client, String name, String address) {
        if (CONNECT_METHOD == null || client == null || address == null || name == null) {
            return false;
        }
        ResourcePackAutoAccept.notifyConnect(address);
        ServerInfo info = createServerInfo(name, address);
        if (info == null) {
            return false;
        }
        applyResourcePackPolicy(info);
        ServerAddress parsed = ServerAddress.parse(address);
        Class<?>[] params = CONNECT_METHOD.getParameterTypes();
        Object[] args = new Object[params.length];
        Object cookieStorage = null;
        String host = resolveServerHost(parsed, address);
        int port = resolveServerPort(parsed, address);
        boolean usedAddress = false;
        boolean usedName = false;
        for (int i = 0; i < args.length; i++) {
            Class<?> type = params[i];
            if (Screen.class.isAssignableFrom(type)) {
                args[i] = parent;
            } else if (MinecraftClient.class.isAssignableFrom(type)) {
                args[i] = client;
            } else if (ServerAddress.class.isAssignableFrom(type)) {
                args[i] = parsed;
            } else if (ServerInfo.class.isAssignableFrom(type)) {
                args[i] = info;
            } else if (isCookieStorageType(type)) {
                if (cookieStorage == null) {
                    cookieStorage = createCookieStorage();
                }
                args[i] = cookieStorage;
            } else if (type == String.class) {
                if (!usedAddress) {
                    args[i] = address;
                    usedAddress = true;
                } else if (!usedName) {
                    args[i] = name;
                    usedName = true;
                } else {
                    args[i] = address;
                }
            } else if (type == int.class || type == Integer.class) {
                args[i] = port;
            } else if (type == boolean.class || type == Boolean.class) {
                args[i] = false;
            } else if (InetSocketAddress.class.isAssignableFrom(type)) {
                args[i] = new InetSocketAddress(host, port);
            } else {
                args[i] = null;
            }
        }
        try {
            CONNECT_METHOD.invoke(null, args);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Identifier tryIdentifierOf(String namespace, String path) {
        try {
            Method of = Identifier.class.getMethod("of", String.class, String.class);
            return (Identifier) of.invoke(null, namespace, path);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Identifier tryIdentifierConstructor(String namespace, String path) {
        try {
            Constructor<Identifier> ctor = Identifier.class.getDeclaredConstructor(String.class, String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(namespace, path);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Identifier tryIdentifierParse(String value) {
        try {
            Method parse = Identifier.class.getMethod("tryParse", String.class);
            return (Identifier) parse.invoke(null, value);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Method findDrawTextureMethod(boolean pipeline) {
        Method candidate = null;
        int bestScore = -1;
        for (Method method : DrawContext.class.getMethods()) {
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length < 1) {
                continue;
            }
            if (pipeline) {
                if (params.length < 2 || !PIPELINE_CLASS.equals(params[0].getName()) || params[1] != Identifier.class) {
                    continue;
                }
            } else if (params[0] != Identifier.class) {
                continue;
            }
            int score = scoreDrawTextureMethod(params.length, pipeline);
            if (score > bestScore) {
                bestScore = score;
                candidate = method;
            }
        }
        if (candidate != null) {
            candidate.setAccessible(true);
        }
        return candidate;
    }

    private static Method findDrawTextureFunctionMethod() {
        Method candidate = null;
        int bestScore = -1;
        for (Method method : DrawContext.class.getMethods()) {
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length < 2) {
                continue;
            }
            if (!Function.class.isAssignableFrom(params[0]) || params[1] != Identifier.class) {
                continue;
            }
            int score = scoreDrawTextureFunctionMethod(method);
            if (score > bestScore) {
                bestScore = score;
                candidate = method;
            }
        }
        if (candidate != null) {
            candidate.setAccessible(true);
        }
        return candidate;
    }

    private static Method findDrawTexturedQuadMethod() {
        Method candidate = findDrawTexturedQuadMethod(DrawContext.class.getDeclaredMethods());
        if (candidate == null) {
            candidate = findDrawTexturedQuadMethod(DrawContext.class.getMethods());
        }
        if (candidate != null) {
            try {
                candidate.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        return candidate;
    }

    private static Method findDrawTexturedQuadMethod(Method[] methods) {
        for (Method method : methods) {
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 11) {
                continue;
            }
            if (!Function.class.isAssignableFrom(params[0]) || params[1] != Identifier.class) {
                continue;
            }
            if (!isInt(params[2]) || !isInt(params[3]) || !isInt(params[4]) || !isInt(params[5])) {
                continue;
            }
            if (!isFloat(params[6]) || !isFloat(params[7]) || !isFloat(params[8]) || !isFloat(params[9])) {
                continue;
            }
            if (!isInt(params[10])) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static Method findDrawTexturedQuadIdMethod() {
        Method candidate = findDrawTexturedQuadIdMethod(DrawContext.class.getDeclaredMethods());
        if (candidate == null) {
            candidate = findDrawTexturedQuadIdMethod(DrawContext.class.getMethods());
        }
        if (candidate != null) {
            try {
                candidate.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        return candidate;
    }

    private static Method findDrawTexturedQuadIdMethod(Method[] methods) {
        Method best = null;
        int bestScore = -1;
        for (Method method : methods) {
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length < 9 || params.length > 15) {
                continue;
            }
            if (params[0] != Identifier.class) {
                continue;
            }
            int intCount = 0;
            int floatCount = 0;
            boolean valid = true;
            for (int i = 1; i < params.length; i++) {
                Class<?> type = params[i];
                if (isInt(type)) {
                    intCount++;
                } else if (isFloat(type)) {
                    floatCount++;
                } else {
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                continue;
            }
            int score = scoreIdTexturedQuadSignature(intCount, floatCount);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static boolean isCookieStorageType(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (COOKIE_STORAGE_CLASS.equals(type.getName())) {
            return true;
        }
        Class<?> cookieClass = getCookieStorageClass();
        return cookieClass != null && cookieClass == type;
    }

    private static Class<?> getCookieStorageClass() {
        try {
            return Class.forName(COOKIE_STORAGE_CLASS);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Object createCookieStorage() {
        Constructor<?> ctor = getCookieStorageConstructor();
        if (ctor == null) {
            return null;
        }
        Class<?>[] params = ctor.getParameterTypes();
        Object[] args = new Object[params.length];
        if (params.length == 1) {
            if (Map.class.isAssignableFrom(params[0])) {
                args[0] = new HashMap<>();
            } else {
                return null;
            }
        }
        try {
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Constructor<?> getCookieStorageConstructor() {
        if (cookieStorageChecked) {
            return cookieStorageConstructor;
        }
        cookieStorageChecked = true;
        Class<?> cookieClass = getCookieStorageClass();
        if (cookieClass == null) {
            return null;
        }
        Constructor<?> best = null;
        int bestScore = -1;
        for (Constructor<?> ctor : cookieClass.getDeclaredConstructors()) {
            int score = scoreCookieStorageConstructor(ctor);
            if (score > bestScore) {
                bestScore = score;
                best = ctor;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        cookieStorageConstructor = best;
        return best;
    }

    private static int scoreCookieStorageConstructor(Constructor<?> ctor) {
        Class<?>[] params = ctor.getParameterTypes();
        if (params.length == 0) {
            return 2;
        }
        if (params.length == 1 && Map.class.isAssignableFrom(params[0])) {
            return 3;
        }
        return -1;
    }

    private static int scoreIdTexturedQuadSignature(int intCount, int floatCount) {
        if (floatCount == 4 && intCount == 4) {
            return 100;
        }
        if (floatCount == 4 && intCount == 5) {
            return 95;
        }
        if (floatCount == 4 && intCount >= 6) {
            return 80;
        }
        if (floatCount == 8 && intCount >= 5) {
            return 70;
        }
        return 0;
    }

    private static int scoreDrawTextureMethod(int count, boolean pipeline) {
        if (pipeline) {
            if (count == 10) {
                return 100;
            }
            if (count == 11) {
                return 90;
            }
            if (count == 12) {
                return 80;
            }
            if (count == 13) {
                return 70;
            }
            return 0;
        }
        if (count == 10) {
            return 100;
        }
        if (count == 9) {
            return 90;
        }
        if (count == 11) {
            return 80;
        }
        if (count == 6) {
            return 70;
        }
        return 0;
    }

    private static int scoreDrawTextureFunctionMethod(int count) {
        if (count == 13) {
            return 100;
        }
        if (count == 12) {
            return 95;
        }
        if (count == 11) {
            return 90;
        }
        if (count == 10) {
            return 80;
        }
        if (count == 7) {
            return 70;
        }
        if (count == 6) {
            return 60;
        }
        return 0;
    }

    private static int scoreDrawTextureFunctionMethod(Method method) {
        int score = scoreDrawTextureFunctionMethod(method.getParameterCount());
        if (isMinecraft1214OrBelow()) {
            if (hasFloatParam(method)) {
                score += 30;
            } else {
                score -= 30;
            }
        }
        return score;
    }

    private static Object findGuiTexturedPipeline() {
        if (DRAW_TEXTURE_PIPELINE == null) {
            return null;
        }
        try {
            Class<?> pipelines = Class.forName(PIPELINES_CLASS);
            Field field = pipelines.getField("GUI_TEXTURED");
            return field.get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static boolean invokeDrawTexturePipeline(DrawContext context, Identifier texture, int x, int y,
                                                     int width, int height, int textureWidth, int textureHeight) {
        Object[] args = buildPipelineArgs(texture, x, y, width, height, textureWidth, textureHeight);
        if (args == null) {
            return false;
        }
        try {
            DRAW_TEXTURE_PIPELINE.invoke(context, args);
            return true;
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private static boolean invokeDrawTextureFunction(DrawContext context, Object function, Identifier texture, int x, int y,
                                                     int width, int height, int textureWidth, int textureHeight) {
        if (DRAW_TEXTURE_FUNCTION == null || function == null) {
            return false;
        }
        int count = DRAW_TEXTURE_FUNCTION.getParameterCount();
        Object[] args = buildFunctionArgs(count, function, texture, x, y, width, height, textureWidth, textureHeight);
        if (args == null) {
            return false;
        }
        try {
            DRAW_TEXTURE_FUNCTION.invoke(context, args);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
        }
        return false;
    }

    private static boolean invokeDrawTextureQuad(DrawContext context, Object function, Identifier texture, int x, int y,
                                                 int width, int height) {
        if (DRAW_TEXTURE_QUAD == null || function == null) {
            return false;
        }
        int x2 = x + width;
        int y2 = y + height;
        Object[] args = new Object[]{function, texture, x, x2, y, y2, 0.0f, 1.0f, 0.0f, 1.0f, COLOR_WHITE};
        try {
            DRAW_TEXTURE_QUAD.invoke(context, args);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
        }
        return false;
    }

    private static boolean invokeDrawTextureQuadId(DrawContext context, Identifier texture, int x, int y,
                                                   int width, int height) {
        if (DRAW_TEXTURE_QUAD_ID == null || context == null || texture == null) {
            return false;
        }
        Object[] args = buildIdTexturedQuadArgs(DRAW_TEXTURE_QUAD_ID, texture, x, y, width, height);
        if (args == null) {
            return false;
        }
        try {
            DRAW_TEXTURE_QUAD_ID.invoke(context, args);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean invokeDrawTextureId(DrawContext context, Identifier texture, int x, int y,
                                               int width, int height, int textureWidth, int textureHeight) {
        if (DRAW_TEXTURE_ID == null) {
            return false;
        }
        Object[] args = buildIdArgs(texture, x, y, width, height, textureWidth, textureHeight);
        if (args == null) {
            return false;
        }
        try {
            DRAW_TEXTURE_ID.invoke(context, args);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
        }
        return false;
    }

    private static Object[] buildPipelineArgs(Identifier texture, int x, int y, int width, int height,
                                              int textureWidth, int textureHeight) {
        if (GUI_TEXTURED_PIPELINE == null) {
            return null;
        }
        if (DRAW_TEXTURE_PIPELINE == null) {
            return null;
        }
        if (hasFloatParam(DRAW_TEXTURE_PIPELINE)) {
            return buildArgsWithFloats(DRAW_TEXTURE_PIPELINE, GUI_TEXTURED_PIPELINE, texture, x, y, width, height,
                    textureWidth, textureHeight);
        }
        return buildArgsWithoutFloats(DRAW_TEXTURE_PIPELINE, GUI_TEXTURED_PIPELINE, texture, x, y, width, height,
                textureWidth, textureHeight);
    }

    private static Object[] buildIdArgs(Identifier texture, int x, int y, int width, int height,
                                        int textureWidth, int textureHeight) {
        if (DRAW_TEXTURE_ID == null) {
            return null;
        }
        if (hasFloatParam(DRAW_TEXTURE_ID)) {
            return buildArgsWithFloats(DRAW_TEXTURE_ID, null, texture, x, y, width, height, textureWidth, textureHeight);
        }
        return buildArgsWithoutFloats(DRAW_TEXTURE_ID, null, texture, x, y, width, height, textureWidth, textureHeight);
    }

    private static Object[] buildFunctionArgs(int count, Object function, Identifier texture, int x, int y, int width,
                                              int height, int textureWidth, int textureHeight) {
        if (count == 10 && DRAW_TEXTURE_FUNCTION != null) {
            Class<?>[] params = DRAW_TEXTURE_FUNCTION.getParameterTypes();
            if (params.length == 10 && isInt(params[2]) && isInt(params[3]) && isInt(params[4])
                    && isInt(params[5]) && isInt(params[6]) && isInt(params[7]) && isInt(params[8]) && isInt(params[9])) {
                return new Object[]{function, texture, textureWidth, textureHeight, 0, 0, x, y, width, height};
            }
        }
        float u = 0.0f;
        float v = 0.0f;
        int regionWidth = textureWidth;
        int regionHeight = textureHeight;
        if (count == 13) {
            return new Object[]{function, texture, x, y, u, v, width, height,
                    regionWidth, regionHeight, textureWidth, textureHeight, COLOR_WHITE};
        }
        if (count == 12) {
            return new Object[]{function, texture, x, y, u, v, width, height,
                    regionWidth, regionHeight, textureWidth, textureHeight};
        }
        if (count == 11) {
            return new Object[]{function, texture, x, y, u, v, width, height, textureWidth, textureHeight, COLOR_WHITE};
        }
        if (count == 10) {
            return new Object[]{function, texture, x, y, u, v, width, height, textureWidth, textureHeight};
        }
        if (count == 7) {
            return new Object[]{function, texture, x, y, width, height, COLOR_WHITE};
        }
        if (count == 6) {
            return new Object[]{function, texture, x, y, width, height};
        }
        return null;
    }

    private static Object[] buildArgsWithFloats(Method method, Object pipeline, Identifier texture,
                                                int x, int y, int width, int height,
                                                int textureWidth, int textureHeight) {
        Class<?>[] params = method.getParameterTypes();
        if (isTexturedQuadSignature(params, pipeline != null)) {
            return buildTexturedQuadArgs(params.length, pipeline, texture, x, y, width, height);
        }
        Object[] args = new Object[params.length];
        int index = 0;
        if (pipeline != null) {
            args[index++] = pipeline;
        }
        if (index >= params.length || params[index] != Identifier.class) {
            return null;
        }
        args[index++] = texture;

        int intBeforeFloats = countIntsBeforeFloat(params, index);
        int[] beforeValues = buildBeforeValues(intBeforeFloats, x, y, width, height);
        int[] afterValues = buildAfterValues(intBeforeFloats, width, height, textureWidth, textureHeight);
        int beforeIndex = 0;
        int afterIndex = 0;
        boolean seenFloat = false;
        int floatIndex = 0;

        for (int i = index; i < params.length; i++) {
            Class<?> type = params[i];
            if (type == float.class || type == Float.class) {
                args[i] = defaultFloatValue(floatIndex);
                seenFloat = true;
                floatIndex++;
            } else if (type == int.class || type == Integer.class) {
                if (!seenFloat && beforeIndex < beforeValues.length) {
                    args[i] = beforeValues[beforeIndex++];
                } else {
                    int safeIndex = Math.min(afterIndex, afterValues.length - 1);
                    args[i] = afterValues[Math.max(0, safeIndex)];
                    afterIndex++;
                }
            } else if (type == boolean.class || type == Boolean.class) {
                args[i] = false;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private static boolean isTexturedQuadSignature(Class<?>[] params, boolean hasPipeline) {
        int offset = hasPipeline ? 2 : 1;
        if (params.length != offset + 8 && params.length != offset + 9) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            Class<?> type = params[offset + i];
            if (type != int.class && type != Integer.class) {
                return false;
            }
        }
        for (int i = 4; i < 8; i++) {
            Class<?> type = params[offset + i];
            if (type != float.class && type != Float.class) {
                return false;
            }
        }
        if (params.length == offset + 9) {
            Class<?> type = params[offset + 8];
            return type == int.class || type == Integer.class;
        }
        return true;
    }

    private static Object[] buildTexturedQuadArgs(int paramCount, Object pipeline, Identifier texture,
                                                  int x, int y, int width, int height) {
        Object[] args = new Object[paramCount];
        int index = 0;
        if (pipeline != null) {
            args[index++] = pipeline;
        }
        args[index++] = texture;
        args[index++] = x;
        args[index++] = y;
        args[index++] = x + width;
        args[index++] = y + height;
        args[index++] = 0.0f;
        args[index++] = 1.0f;
        args[index++] = 0.0f;
        args[index++] = 1.0f;
        if (index < args.length) {
            args[index] = COLOR_WHITE;
        }
        return args;
    }

    private static Object[] buildIdTexturedQuadArgs(Method method, Identifier texture,
                                                    int x, int y, int width, int height) {
        if (method == null || texture == null) {
            return null;
        }
        Class<?>[] params = method.getParameterTypes();
        Object[] args = new Object[params.length];
        int floatIndex = 0;
        int coordIndex = 0;
        int[] coords = new int[]{x, x + width, y, y + height};
        boolean seenFloat = false;
        for (int i = 0; i < params.length; i++) {
            Class<?> type = params[i];
            if (i == 0) {
                if (type != Identifier.class) {
                    return null;
                }
                args[i] = texture;
                continue;
            }
            if (isInt(type)) {
                if (!seenFloat) {
                    if (coordIndex < coords.length) {
                        args[i] = coords[coordIndex++];
                    } else {
                        args[i] = 0;
                    }
                } else {
                    args[i] = COLOR_WHITE;
                }
            } else if (isFloat(type)) {
                seenFloat = true;
                args[i] = quadFloatValue(floatIndex++);
            } else {
                return null;
            }
        }
        return args;
    }

    private static float quadFloatValue(int index) {
        return (index % 2 == 1) ? 1.0f : 0.0f;
    }

    private static float defaultFloatValue(int index) {
        if (index == 2 || index == 3) {
            return 1.0f;
        }
        return 0.0f;
    }

    private static Object[] buildArgsWithoutFloats(Method method, Object pipeline, Identifier texture,
                                                   int x, int y, int width, int height,
                                                   int textureWidth, int textureHeight) {
        int count = method.getParameterCount();
        if (pipeline != null) {
            if (count == 10) {
                return new Object[]{pipeline, texture, x, y, 0, 0, width, height, textureWidth, textureHeight};
            }
            if (count == 11) {
                return new Object[]{pipeline, texture, x, y, 0, 0, width, height, textureWidth, textureHeight, COLOR_WHITE};
            }
            return null;
        }
        if (count == 7) {
            return new Object[]{texture, x, y, 0, 0, width, height};
        }
        if (count == 8) {
            return new Object[]{texture, x, y, 0, 0, width, height, COLOR_WHITE};
        }
        if (count == 9) {
            return new Object[]{texture, x, y, 0, 0, width, height, textureWidth, textureHeight};
        }
        if (count == 10) {
            return new Object[]{texture, x, y, 0, 0, width, height, textureWidth, textureHeight, COLOR_WHITE};
        }
        return null;
    }

    private static Method getLegacyDrawTextureMethod() {
        if (legacyDrawTextureChecked) {
            return legacyDrawTextureMethod;
        }
        legacyDrawTextureChecked = true;
        Method best = null;
        int bestScore = -1;
        Method[] declared = DrawContext.class.getDeclaredMethods();
        Method[] publics = DrawContext.class.getMethods();
        best = pickLegacyDrawTextureMethod(declared, best, bestScore);
        bestScore = best == null ? -1 : scoreLegacyDrawTextureMethod(best);
        best = pickLegacyDrawTextureMethod(publics, best, bestScore);
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        legacyDrawTextureMethod = best;
        return best;
    }

    private static Method pickLegacyDrawTextureMethod(Method[] methods, Method current, int currentScore) {
        Method best = current;
        int bestScore = currentScore;
        for (Method method : methods) {
            if (method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length < 7 || params.length > 10) {
                continue;
            }
            if (params[0] != Identifier.class) {
                continue;
            }
            if (!matchesLegacyDrawParams(params)) {
                continue;
            }
            int score = scoreLegacyDrawTextureMethod(method);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static boolean matchesLegacyDrawParams(Class<?>[] params) {
        for (int i = 1; i < params.length; i++) {
            Class<?> type = params[i];
            if (type == int.class || type == Integer.class) {
                continue;
            }
            if (type == float.class || type == Float.class) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static int scoreLegacyDrawTextureMethod(Method method) {
        int count = method.getParameterCount();
        if (count == 9) {
            return 100;
        }
        if (count == 10) {
            return 90;
        }
        if (count == 7) {
            return 80;
        }
        if (count == 8) {
            return 70;
        }
        return 0;
    }

    private static Object[] buildLegacyDrawArgs(Method method, Identifier texture, int x, int y,
                                                int width, int height, int textureWidth, int textureHeight) {
        if (method == null) {
            return null;
        }
        if (hasFloatParam(method)) {
            return buildArgsWithFloats(method, null, texture, x, y, width, height, textureWidth, textureHeight);
        }
        return buildArgsWithoutFloats(method, null, texture, x, y, width, height, textureWidth, textureHeight);
    }

    private static Object[] buildLegacyDrawArgsAlt(Method method, Identifier texture, int x, int y,
                                                   int width, int height, int textureWidth, int textureHeight) {
        if (method == null || hasFloatParam(method)) {
            return null;
        }
        int count = method.getParameterCount();
        if (count == 7) {
            return new Object[]{texture, x, y, width, height, 0, 0};
        }
        if (count == 8) {
            return new Object[]{texture, x, y, width, height, 0, 0, COLOR_WHITE};
        }
        if (count == 9) {
            return new Object[]{texture, x, y, width, height, 0, 0, textureWidth, textureHeight};
        }
        if (count == 10) {
            return new Object[]{texture, x, y, width, height, 0, 0, textureWidth, textureHeight, COLOR_WHITE};
        }
        return null;
    }

    private static boolean shouldTryLegacyAltArgs(Method method) {
        if (method == null) {
            return false;
        }
        if (hasFloatParam(method)) {
            return false;
        }
        int count = method.getParameterCount();
        if (count < 7 || count > 10) {
            return false;
        }
        return isMinecraftAtLeast(1, 20, 1) && isMinecraftAtMost(1, 21, 0);
    }

    private static ShaderProgram resolvePositionTexShader() {
        Method method = getPositionTexShaderMethod();
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(null);
            if (value instanceof ShaderProgram program) {
                return program;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }

    private static Method getPositionTexShaderMethod() {
        if (positionTexShaderChecked) {
            return positionTexShaderMethod;
        }
        positionTexShaderChecked = true;
        Method best = null;
        int bestScore = -1;
        Method[] declared = net.minecraft.client.render.GameRenderer.class.getDeclaredMethods();
        Method[] publics = net.minecraft.client.render.GameRenderer.class.getMethods();
        best = pickPositionTexShaderMethod(declared, best, bestScore);
        bestScore = best == null ? -1 : scorePositionTexShaderMethod(best);
        best = pickPositionTexShaderMethod(publics, best, bestScore);
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        positionTexShaderMethod = best;
        return best;
    }

    private static Method getDisableScissorMethod(Class<?> contextClass) {
        if (disableScissorChecked) {
            return disableScissorMethod;
        }
        disableScissorChecked = true;
        Method best = null;
        int bestScore = -1;
        Method[] declared = contextClass.getDeclaredMethods();
        Method[] publics = contextClass.getMethods();
        best = pickDisableScissorMethod(declared, best, bestScore);
        bestScore = best == null ? -1 : scoreDisableScissorMethod(best);
        best = pickDisableScissorMethod(publics, best, bestScore);
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        disableScissorMethod = best;
        return best;
    }

    private static Method getSetScissorMethod(Class<?> contextClass) {
        if (setScissorChecked) {
            return setScissorMethod;
        }
        setScissorChecked = true;
        Method best = null;
        int bestScore = -1;
        Method[] declared = contextClass.getDeclaredMethods();
        Method[] publics = contextClass.getMethods();
        best = pickSetScissorMethod(declared, best, bestScore);
        bestScore = best == null ? -1 : scoreSetScissorMethod(best);
        best = pickSetScissorMethod(publics, best, bestScore);
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        setScissorMethod = best;
        return best;
    }

    private static Method pickSetScissorMethod(Method[] methods, Method current, int currentScore) {
        Method best = current;
        int bestScore = currentScore;
        for (Method method : methods) {
            if (method.getParameterCount() != 4 || method.getReturnType() != void.class) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[0] != int.class || params[1] != int.class || params[2] != int.class || params[3] != int.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("scissor")) {
                continue;
            }
            int score = scoreSetScissorMethod(method);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static int scoreSetScissorMethod(Method method) {
        String name = method.getName().toLowerCase();
        int score = 0;
        if (name.contains("set")) {
            score += 3;
        }
        if (name.contains("enable")) {
            score += 2;
        }
        if (name.contains("scissor")) {
            score += 2;
        }
        return score;
    }

    private static Method pickDisableScissorMethod(Method[] methods, Method current, int currentScore) {
        Method best = current;
        int bestScore = currentScore;
        for (Method method : methods) {
            if (method.getParameterCount() != 0 || method.getReturnType() != void.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("scissor")) {
                continue;
            }
            int score = scoreDisableScissorMethod(method);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static int scoreDisableScissorMethod(Method method) {
        String name = method.getName().toLowerCase();
        int score = 0;
        if (name.contains("disable")) {
            score += 3;
        }
        if (name.contains("scissor")) {
            score += 2;
        }
        return score;
    }

    private static Method pickPositionTexShaderMethod(Method[] methods, Method current, int currentScore) {
        Method best = current;
        int bestScore = currentScore;
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.getReturnType() != ShaderProgram.class) {
                continue;
            }
            int score = scorePositionTexShaderMethod(method);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static int scorePositionTexShaderMethod(Method method) {
        String name = method.getName().toLowerCase();
        int score = 0;
        if (name.contains("position")) {
            score += 5;
        }
        if (name.contains("tex")) {
            score += 5;
        }
        if (name.contains("shader") || name.contains("program")) {
            score += 2;
        }
        if (name.contains("get")) {
            score += 1;
        }
        return score;
    }

    private static int countIntsBeforeFloat(Class<?>[] params, int startIndex) {
        int count = 0;
        for (int i = startIndex; i < params.length; i++) {
            Class<?> type = params[i];
            if (type == float.class || type == Float.class) {
                break;
            }
            if (type == int.class || type == Integer.class) {
                count++;
            }
        }
        return count;
    }

    private static int[] buildBeforeValues(int intCount, int x, int y, int width, int height) {
        if (intCount <= 0) {
            return new int[0];
        }
        int[] values = new int[intCount];
        if (intCount >= 1) {
            values[0] = x;
        }
        if (intCount >= 2) {
            values[1] = y;
        }
        if (intCount >= 3) {
            values[2] = intCount == 3 ? 0 : width;
        }
        if (intCount >= 4) {
            values[3] = height;
        }
        for (int i = 4; i < intCount; i++) {
            values[i] = 0;
        }
        return values;
    }

    private static int[] buildAfterValues(int intCountBeforeFloats, int width, int height,
                                          int textureWidth, int textureHeight) {
        int[] values = new int[7];
        int index = 0;
        if (intCountBeforeFloats >= 4) {
            values[index++] = textureWidth;
            values[index++] = textureHeight;
        } else {
            values[index++] = width;
            values[index++] = height;
        }
        values[index++] = textureWidth;
        values[index++] = textureHeight;
        values[index++] = textureWidth;
        values[index++] = textureHeight;
        values[index] = COLOR_WHITE;
        return values;
    }

    private static boolean hasFloatParam(Method method) {
        if (method == null) {
            return false;
        }
        for (Class<?> type : method.getParameterTypes()) {
            if (type == float.class || type == Float.class) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInt(Class<?> type) {
        return type == int.class || type == Integer.class;
    }

    private static boolean isFloat(Class<?> type) {
        return type == float.class || type == Float.class;
    }


    private static Object getGuiTextureFunction() {
        if (guiTextureFunctionChecked) {
            return guiTextureFunction;
        }
        guiTextureFunctionChecked = true;
        if (DRAW_TEXTURE_FUNCTION == null) {
            return null;
        }
        try {
            Class<?> renderLayer = Class.forName(RENDER_LAYER_CLASS);
            if (isMinecraft1214OrBelow()) {
                Object legacy = getGuiTextureFunctionLegacy(renderLayer);
                if (legacy != null) {
                    guiTextureFunction = legacy;
                    return guiTextureFunction;
                }
            }
            Identifier sample = id("minecraft", "textures/gui/options_background.png");
            Object best = findBestFunctionField(renderLayer, sample);
            if (best == null) {
                best = buildFunctionFromMethod(renderLayer, sample);
            }
            guiTextureFunction = best;
            return guiTextureFunction;
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Object getGuiTextureFunctionLegacy(Class<?> renderLayer) {
        Method method = resolveGuiTexturedMethodLegacy(renderLayer);
        if (method == null) {
            return null;
        }
        Method resolved = method;
        return (Function<Identifier, Object>) id -> {
            try {
                return invokeLegacyRenderLayerMethod(resolved, id);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        };
    }

    private static Method resolveGuiTexturedMethodLegacy(Class<?> renderLayer) {
        if (guiTexturedMethodChecked) {
            return guiTexturedMethodLegacy;
        }
        guiTexturedMethodChecked = true;
        if (renderLayer == null) {
            return null;
        }
        Method best = findLegacyGuiTexturedByName(renderLayer);
        Identifier sample = id("minecraft", "textures/gui/options_background.png");
        if (best == null) {
            best = findGuiTexturedMethodLegacy(renderLayer, renderLayer.getDeclaredMethods(), sample);
        }
        if (best == null) {
            best = findGuiTexturedMethodLegacy(renderLayer, renderLayer.getMethods(), sample);
        }
        if (best == null) {
            best = findBestLegacyRenderLayerMethod(renderLayer, renderLayer.getDeclaredMethods(), sample);
        }
        if (best == null) {
            best = findBestLegacyRenderLayerMethod(renderLayer, renderLayer.getMethods(), sample);
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        guiTexturedMethodLegacy = best;
        return best;
    }

    private static Method findLegacyGuiTexturedByName(Class<?> renderLayer) {
        String[] names = {"method_62277", "getGuiTextured"};
        Method match = findLegacyGuiTexturedByName(renderLayer, renderLayer.getDeclaredMethods(), names);
        if (match == null) {
            match = findLegacyGuiTexturedByName(renderLayer, renderLayer.getMethods(), names);
        }
        return match;
    }

    private static Method findLegacyGuiTexturedByName(Class<?> renderLayer, Method[] methods, String[] names) {
        for (Method method : methods) {
            if (!isLegacyRenderLayerMethod(renderLayer, method)) {
                continue;
            }
            String name = method.getName();
            for (String target : names) {
                if (target.equals(name)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Method findGuiTexturedMethodLegacy(Class<?> renderLayer, Method[] methods, Identifier sample) {
        for (Method method : methods) {
            if (!isLegacyRenderLayerMethod(renderLayer, method)) {
                continue;
            }
            if (matchesGuiTexturedLegacy(method, sample)) {
                return method;
            }
        }
        return null;
    }

    private static boolean isLegacyRenderLayerMethod(Class<?> renderLayer, Method method) {
        if (method == null) {
            return false;
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        if (method.getReturnType() != renderLayer) {
            return false;
        }
        Class<?>[] params = method.getParameterTypes();
        if (params.length < 1 || params.length > 4) {
            return false;
        }
        if (params[0] != Identifier.class) {
            return false;
        }
        for (int i = 1; i < params.length; i++) {
            Class<?> type = params[i];
            if (type != boolean.class && type != float.class && type != int.class) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesGuiTexturedLegacy(Method method, Identifier sample) {
        if (sample == null) {
            return false;
        }
        try {
            Object layer = invokeLegacyRenderLayerMethod(method, sample);
            String name = resolveLayerName(layer);
            if (name == null) {
                return false;
            }
            String lower = name.toLowerCase();
            if (!lower.contains("gui_textured")) {
                return false;
            }
            return !lower.contains("overlay") && !lower.contains("opaque");
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private static Object invokeLegacyRenderLayerMethod(Method method, Identifier id) throws ReflectiveOperationException {
        if (method == null) {
            return null;
        }
        Class<?>[] params = method.getParameterTypes();
        Object[] args = new Object[params.length];
        args[0] = id;
        for (int i = 1; i < params.length; i++) {
            Class<?> type = params[i];
            if (type == boolean.class) {
                args[i] = false;
            } else if (type == float.class) {
                args[i] = 0.0f;
            } else if (type == int.class) {
                args[i] = 0;
            } else {
                args[i] = null;
            }
        }
        return method.invoke(null, args);
    }

    private static Method findBestLegacyRenderLayerMethod(Class<?> renderLayer, Method[] methods, Identifier sample) {
        Method best = null;
        int bestScore = -1;
        for (Method method : methods) {
            if (!isLegacyRenderLayerMethod(renderLayer, method)) {
                continue;
            }
            try {
                Object layer = invokeLegacyRenderLayerMethod(method, sample);
                String name = resolveLayerName(layer);
                if (isMinecraft1214OrBelow()) {
                    if (name.contains("overlay") || name.contains("opaque")) {
                        continue;
                    }
                }
                int score = scoreLayerName(name);
                if (score > bestScore) {
                    bestScore = score;
                    best = method;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (bestScore < MIN_LAYER_SCORE) {
            return null;
        }
        return best;
    }

    private static Object findBestFunctionField(Class<?> renderLayer, Identifier sample) {
        if (renderLayer == null || sample == null) {
            return null;
        }
        Object best = null;
        int bestScore = -1;
        String bestName = "";
        Object overlayCandidate = null;
        String overlayName = "";
        for (Field field : renderLayer.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!Function.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object function = field.get(null);
                String name = resolveLayerName(applyFunction(function, sample));
                if (isMinecraft1214OrBelow() && name != null && name.toLowerCase().contains("overlay")) {
                    continue;
                }
                if ("gui_textured".equals(name)) {
                    return function;
                }
                if ("gui_textured_overlay".equals(name)) {
                    overlayCandidate = function;
                    overlayName = name;
                }
                int score = scoreLayerName(name);
                if (score > bestScore) {
                    bestScore = score;
                    best = function;
                    bestName = name;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (overlayCandidate != null && !isMinecraft1214OrBelow()) {
            return overlayCandidate;
        }
        if (bestScore < MIN_LAYER_SCORE) {
            return null;
        }
        return best;
    }

    private static Object buildFunctionFromMethod(Class<?> renderLayer, Identifier sample) {
        Method best = null;
        int bestScore = -1;
        String bestName = "";
        Method overlayMethod = null;
        String overlayName = "";
        for (Method method : renderLayer.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getReturnType() != renderLayer) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || params[0] != Identifier.class) {
                continue;
            }
            try {
                Object layer = method.invoke(null, sample);
                String name = resolveLayerName(layer);
                if (isMinecraft1214OrBelow() && name != null && name.toLowerCase().contains("overlay")) {
                    continue;
                }
                if ("gui_textured".equals(name)) {
                    return buildFunctionFromMethod(method);
                }
                if ("gui_textured_overlay".equals(name)) {
                    overlayMethod = method;
                    overlayName = name;
                }
                int score = scoreLayerName(name);
                if (score > bestScore) {
                    bestScore = score;
                    best = method;
                    bestName = name;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (overlayMethod != null && !isMinecraft1214OrBelow()) {
            return buildFunctionFromMethod(overlayMethod);
        }
        if (bestScore < MIN_LAYER_SCORE || best == null) {
            return null;
        }
        return buildFunctionFromMethod(best);
    }

    private static Function<Identifier, Object> buildFunctionFromMethod(Method method) {
        Method resolved = method;
        return id -> {
            try {
                return resolved.invoke(null, id);
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        };
    }

    private static Object applyFunction(Object function, Identifier sample) {
        if (!(function instanceof Function<?, ?> func)) {
            return null;
        }
        try {
            return ((Function<Object, Object>) func).apply(sample);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String resolveLayerName(Object layer) {
        if (layer == null) {
            return "";
        }
        String name = callStringMethod(layer, "method_68484");
        if (name == null) {
            name = callStringMethod(layer, "getName");
        }
        if (name == null) {
            name = callAnyStringGetter(layer);
        }
        if (name == null) {
            name = layer.toString();
        }
        return name == null ? "" : name.toLowerCase();
    }

    private static String callStringMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            if (method.getReturnType() != String.class || method.getParameterCount() != 0) {
                return null;
            }
            return (String) method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static String callAnyStringGetter(Object target) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != String.class) {
                continue;
            }
            if (method.getDeclaringClass() == Object.class || "toString".equals(method.getName())) {
                continue;
            }
            try {
                return (String) method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static int scoreLayerName(String name) {
        if (name == null || name.isBlank()) {
            return 0;
        }
        int score = 0;
        if ("gui_textured".equals(name)) {
            return 200;
        }
        if ("gui_textured_overlay".equals(name)) {
            return 150;
        }
        if (name.contains("gui_textured")) {
            score += 80;
        }
        if (name.contains("gui")) {
            score += 50;
        }
        if (name.contains("textured")) {
            score += 40;
        }
        if (name.contains("overlay")) {
            score += 5;
        }
        if (name.contains("background")) {
            score -= 5;
        }
        return score;
    }

    private static Method findConnectMethod() {
        Class<?> connectScreenClass = findConnectScreenClass();
        if (connectScreenClass == null) {
            return null;
        }
        Method best = null;
        int bestScore = -1;
        Method[] declared = connectScreenClass.getDeclaredMethods();
        Method[] publics = connectScreenClass.getMethods();
        best = pickConnectMethod(declared, best, bestScore);
        bestScore = best == null ? -1 : scoreConnectMethod(best);
        best = pickConnectMethod(publics, best, bestScore);
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        return best;
    }

    private static Method pickConnectMethod(Method[] methods, Method current, int currentScore) {
        Method best = current;
        int bestScore = currentScore;
        for (Method method : methods) {
            if (!isConnectMethodCandidate(method)) {
                continue;
            }
            int score = scoreConnectMethod(method);
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        return best;
    }

    private static boolean isConnectMethodCandidate(Method method) {
        if (method == null) {
            return false;
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        if (method.getReturnType() != void.class) {
            return false;
        }
        Class<?>[] params = method.getParameterTypes();
        if (params.length < 3) {
            return false;
        }
        boolean hasClient = false;
        boolean hasInfo = false;
        boolean hasAddress = false;
        for (Class<?> type : params) {
            if (MinecraftClient.class.isAssignableFrom(type)) {
                hasClient = true;
            } else if (ServerInfo.class.isAssignableFrom(type)) {
                hasInfo = true;
            } else if (ServerAddress.class.isAssignableFrom(type)
                    || InetSocketAddress.class.isAssignableFrom(type)
                    || type == String.class) {
                hasAddress = true;
            }
        }
        return hasClient && hasInfo && hasAddress;
    }

    private static int scoreConnectMethod(Method method) {
        Class<?>[] params = method.getParameterTypes();
        int score = 0;
        for (Class<?> type : params) {
            if (Screen.class.isAssignableFrom(type)) {
                score += 5;
            } else if (MinecraftClient.class.isAssignableFrom(type)) {
                score += 5;
            } else if (ServerAddress.class.isAssignableFrom(type)) {
                score += 5;
            } else if (ServerInfo.class.isAssignableFrom(type)) {
                score += 5;
            } else if (isCookieStorageType(type)) {
                score += 3;
            } else if (InetSocketAddress.class.isAssignableFrom(type)) {
                score += 2;
            } else if (type == String.class) {
                score += 1;
            } else if (type == boolean.class || type == Boolean.class) {
                score += 1;
            } else if (type == int.class || type == Integer.class) {
                score += 1;
            }
        }
        if (params.length >= 4) {
            score += 1;
        }
        return score;
    }

    private static Class<?> findConnectScreenClass() {
        Class<?> resolved = tryLoadClass(CONNECT_SCREEN_CLASS);
        if (resolved != null) {
            return resolved;
        }
        resolved = tryLoadClass(CONNECT_SCREEN_CLASS_INTERMEDIARY);
        if (resolved != null) {
            return resolved;
        }
        return tryLoadClass(CONNECT_SCREEN_FALLBACK);
    }

    private static Class<?> tryLoadClass(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static ServerInfo createServerInfo(String name, String address) {
        for (Constructor<?> ctor : ServerInfo.class.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length < 2 || params[0] != String.class || params[1] != String.class) {
                continue;
            }
            Object[] args = new Object[params.length];
            args[0] = name;
            args[1] = address;
            for (int i = 2; i < params.length; i++) {
                Class<?> type = params[i];
                if (type == boolean.class || type == Boolean.class) {
                    args[i] = false;
                } else if (type.isEnum()) {
                    Object[] values = type.getEnumConstants();
                    args[i] = findEnumConstant(values, "OTHER");
                    if (args[i] == null && values.length > 0) {
                        args[i] = values[0];
                    }
                } else {
                    args[i] = null;
                }
            }
            try {
                return (ServerInfo) ctor.newInstance(args);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Object findEnumConstant(Object[] values, String name) {
        for (Object value : values) {
            if (value instanceof Enum<?> enumValue && enumValue.name().equals(name)) {
                return value;
            }
        }
        return null;
    }

    private static void applyResourcePackPolicy(ServerInfo info) {
        if (info == null) {
            return;
        }
        if (applyResourcePackPolicyMethod(info)) {
            return;
        }
        applyResourcePackPolicyField(info);
    }

    private static boolean applyResourcePackPolicyMethod(ServerInfo info) {
        Method method = findResourcePackPolicyMethod(info.getClass());
        if (method == null) {
            return false;
        }
        Class<?> paramType = method.getParameterTypes()[0];
        Object value;
        if (paramType == boolean.class || paramType == Boolean.class) {
            value = true;
        } else if (paramType.isEnum()) {
            value = pickResourcePackEnum(paramType.getEnumConstants());
        } else {
            return false;
        }
        if (value == null) {
            return false;
        }
        try {
            method.invoke(info, value);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static void applyResourcePackPolicyField(ServerInfo info) {
        Field field = findResourcePackPolicyField(info.getClass());
        if (field == null) {
            return;
        }
        try {
            Class<?> type = field.getType();
            Object value;
            if (type == boolean.class || type == Boolean.class) {
                value = true;
            } else if (type.isEnum()) {
                value = pickResourcePackEnum(type.getEnumConstants());
            } else {
                return;
            }
            if (value == null) {
                return;
            }
            field.set(info, value);
        } catch (IllegalAccessException | RuntimeException ignored) {
        }
    }

    private static Method findResourcePackPolicyMethod(Class<?> infoClass) {
        if (infoClass == null) {
            return null;
        }
        Method best = null;
        int bestScore = -1;
        for (Method method : infoClass.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("resource") || !name.contains("pack")) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (!(param.isEnum() || param == boolean.class || param == Boolean.class)) {
                continue;
            }
            int score = 0;
            if (name.contains("set")) {
                score += 3;
            }
            if (name.contains("policy")) {
                score += 3;
            }
            if (name.contains("resource")) {
                score += 2;
            }
            if (name.contains("pack")) {
                score += 2;
            }
            if (score > bestScore) {
                bestScore = score;
                best = method;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        return best;
    }

    private static Field findResourcePackPolicyField(Class<?> infoClass) {
        if (infoClass == null) {
            return null;
        }
        Field best = null;
        int bestScore = -1;
        for (Field field : infoClass.getDeclaredFields()) {
            String name = field.getName().toLowerCase();
            if (!name.contains("resource") || !name.contains("pack")) {
                continue;
            }
            Class<?> type = field.getType();
            if (!(type.isEnum() || type == boolean.class || type == Boolean.class)) {
                continue;
            }
            int score = 0;
            if (name.contains("policy")) {
                score += 3;
            }
            if (name.contains("resource")) {
                score += 2;
            }
            if (name.contains("pack")) {
                score += 2;
            }
            if (score > bestScore) {
                bestScore = score;
                best = field;
            }
        }
        if (best != null) {
            try {
                best.setAccessible(true);
            } catch (RuntimeException ignored) {
            }
        }
        return best;
    }

    private static Object pickResourcePackEnum(Object[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        Object preferred = findResourcePackEnumByName(values, "ENABLED");
        if (preferred != null) {
            return preferred;
        }
        preferred = findResourcePackEnumByName(values, "ALWAYS");
        if (preferred != null) {
            return preferred;
        }
        preferred = findResourcePackEnumByName(values, "ACCEPTED");
        if (preferred != null) {
            return preferred;
        }
        preferred = findResourcePackEnumByName(values, "ACCEPT");
        if (preferred != null) {
            return preferred;
        }
        for (Object value : values) {
            if (!(value instanceof Enum<?> enumValue)) {
                continue;
            }
            String name = enumValue.name().toUpperCase();
            if (name.contains("DENY") || name.contains("DISABLE") || name.contains("DECLINE")
                    || name.contains("PROMPT") || name.contains("ASK")) {
                continue;
            }
            if (name.contains("ENABLE") || name.contains("ALWAY") || name.contains("ACCEPT")) {
                return value;
            }
        }
        return null;
    }

    private static String resolveServerHost(ServerAddress parsed, String fallback) {
        String host = tryGetServerHost(parsed);
        if (host == null || host.isBlank()) {
            host = parseHostFromAddress(fallback);
        }
        if (host == null || host.isBlank()) {
            return "localhost";
        }
        return host;
    }

    private static int resolveServerPort(ServerAddress parsed, String fallback) {
        Integer port = tryGetServerPort(parsed);
        if (port == null || port <= 0) {
            port = parsePortFromAddress(fallback);
        }
        if (port == null || port <= 0) {
            return 25565;
        }
        return port;
    }

    private static String tryGetServerHost(ServerAddress parsed) {
        if (parsed == null) {
            return null;
        }
        for (Method method : parsed.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != String.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("address") && !name.contains("host")) {
                continue;
            }
            try {
                return (String) method.invoke(parsed);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static Integer tryGetServerPort(ServerAddress parsed) {
        if (parsed == null) {
            return null;
        }
        for (Method method : parsed.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            Class<?> type = method.getReturnType();
            if (type != int.class && type != Integer.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.contains("port")) {
                continue;
            }
            try {
                Object value = method.invoke(parsed);
                if (value instanceof Integer port) {
                    return port;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static String parseHostFromAddress(String address) {
        if (address == null) {
            return null;
        }
        String trimmed = address.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("[")) {
            int end = trimmed.indexOf(']');
            if (end > 1) {
                return trimmed.substring(1, end);
            }
        }
        int first = trimmed.indexOf(':');
        int last = trimmed.lastIndexOf(':');
        if (first > 0 && first == last) {
            return trimmed.substring(0, first);
        }
        return trimmed;
    }

    private static Integer parsePortFromAddress(String address) {
        if (address == null) {
            return null;
        }
        String trimmed = address.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("[")) {
            int end = trimmed.indexOf(']');
            if (end > 0 && end + 1 < trimmed.length() && trimmed.charAt(end + 1) == ':') {
                return parsePortValue(trimmed.substring(end + 2));
            }
            return null;
        }
        int first = trimmed.indexOf(':');
        int last = trimmed.lastIndexOf(':');
        if (first > 0 && first == last) {
            return parsePortValue(trimmed.substring(first + 1));
        }
        return null;
    }

    private static Integer parsePortValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int port = 0;
        boolean hasDigit = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                hasDigit = true;
                port = port * 10 + (ch - '0');
                if (port > 65535) {
                    return null;
                }
            } else {
                break;
            }
        }
        return hasDigit ? port : null;
    }

    private static Object findResourcePackEnumByName(Object[] values, String name) {
        for (Object value : values) {
            if (value instanceof Enum<?> enumValue && enumValue.name().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
