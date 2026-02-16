package hu.ColorsASD.minewild.installer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ShaderPackInstaller {
    private static final Logger LOGGER = LoggerFactory.getLogger("MinewildShaderInstaller");
    private static final Gson GSON = new GsonBuilder().create();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean FOLLOW_UP_WATCH_STARTED = new AtomicBoolean(false);
    private static final String GAME_VERSION = resolveGameVersion();
    private static final String ISMERETLEN_VERZIO = "ismeretlen";
    private static final String MODRINTH_VERSION_URL_PREFIX = "https://api.modrinth.com/v2/project/";
    private static final String SHADER_VERSION_URL_SUFFIX = buildShaderVersionUrlSuffix();
    private static final String SHADER_PROJECT_SLUG = "complementary-unbound";
    private static final String SHADERPACK_DIR = "shaderpacks";
    private static final String IRIS_PROPERTIES = "iris.properties";
    private static final String IRIS_SHADER_PACK_KEY = "shaderPack";
    private static final String IRIS_ENABLE_SHADERS_KEY = "enableShaders";
    private static final String SHADER_CHOICE_PROPERTIES = "minewild-shader.properties";
    private static final String SHADER_CHOICE_KEY = "enabled";
    private static final String SHADER_FILENAME_PREFIX = "complementaryunbound";
    private static final String EUPHORIA_SHADER_NAME_TOKEN = "euphoriapatches";
    private static final String EUPHORIA_PATCH_SUFFIX = " + EuphoriaPatches_1.8.6";
    private static final String SHADER_PROPERTIES_ENTRY = "shaders/shaders.properties";
    private static final String POPULAR_PROFILE_KEY = "profile.POPULAR";
    private static final long PATCHED_SHADER_WAIT_MS = 45_000L;
    private static final long PATCHED_SHADER_POLL_MS = 1_000L;
    private static final long FOLLOW_UP_WATCH_MS = 30L * 60L * 1000L;
    private static final long FOLLOW_UP_POLL_MS = 2_000L;
    private static final Object SHADER_CHOICE_LOCK = new Object();
    private static volatile boolean shaderChoiceLoaded = false;
    private static volatile Boolean cachedShaderEnabledChoice = null;

    private ShaderPackInstaller() {
    }

    public static void beginInstallIfNeeded() {
        Boolean shaderEnabledChoice = getShaderEnabledChoice();
        if (Boolean.FALSE.equals(shaderEnabledChoice)) {
            applyIrisDisabledSettings();
        }
        startInstallWorker();
    }

    public static boolean hasUserPreference() {
        return getShaderEnabledChoice() != null;
    }

    public static void applyUserPreference(boolean enabled) {
        storeShaderEnabledChoice(enabled);
        startInstallWorker();
        if (enabled) {
            applyPreferredInstalledShaderSettings();
            return;
        }
        applyIrisDisabledSettings();
    }

    private static void startInstallWorker() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(ShaderPackInstaller::install, "minewild-shader-installer");
        worker.setDaemon(true);
        worker.start();
    }

    private static void install() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path shaderpacksDir = gameDir.resolve(SHADERPACK_DIR);
        try {
            Files.createDirectories(shaderpacksDir);
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült létrehozni a shaderpacks mappát: {}", shaderpacksDir, e);
            return;
        }

        ShaderLookup lookup = fetchLatestVersion(SHADER_PROJECT_SLUG);
        if (lookup == null || lookup.version == null) {
            Optional<String> existingPatched = findExistingEuphoriaShaderPack(shaderpacksDir);
            if (existingPatched.isPresent()) {
                applyPreferenceForShaderPack(shaderpacksDir, existingPatched.get());
                startFollowUpWatch(shaderpacksDir, existingPatched.get());
                return;
            }
            Optional<String> existing = findExistingShaderPack(shaderpacksDir);
            if (existing.isPresent()) {
                applyPreferenceForShaderPack(shaderpacksDir, existing.get());
                startFollowUpWatch(shaderpacksDir, existing.get());
                return;
            }
            if (lookup != null && lookup.noMatch) {
                LOGGER.warn("Nincs Modrinth verzió ehhez a shaderhez: {}", SHADER_PROJECT_SLUG);
            } else {
                LOGGER.warn("Nem sikerült lekérdezni a shader verzióját: {}", SHADER_PROJECT_SLUG);
            }
            return;
        }

        ShaderFile file = pickPrimaryFile(lookup.version);
        if (file == null || file.url == null || file.filename == null) {
            LOGGER.warn("Nincs letölthető shaderfájl ehhez: {}", SHADER_PROJECT_SLUG);
            return;
        }

        Path target = shaderpacksDir.resolve(file.filename);
        if (Files.notExists(target)) {
            if (!downloadTo(file.url, target)) {
                return;
            }
        }

        String preferredShaderPack = waitForPreferredShaderPack(shaderpacksDir, file.filename);
        applyPreferenceForShaderPack(shaderpacksDir, preferredShaderPack);
        startFollowUpWatch(shaderpacksDir, preferredShaderPack);
    }

    private static void startFollowUpWatch(Path shaderpacksDir, String fallbackShaderPackFilename) {
        if (!FOLLOW_UP_WATCH_STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread watcher = new Thread(
                () -> followUpWatch(shaderpacksDir, fallbackShaderPackFilename),
                "minewild-shader-follow-up"
        );
        watcher.setDaemon(true);
        watcher.start();
    }

    private static void followUpWatch(Path shaderpacksDir, String fallbackShaderPackFilename) {
        long deadline = System.currentTimeMillis() + FOLLOW_UP_WATCH_MS;
        while (System.currentTimeMillis() < deadline) {
            Boolean shaderEnabledChoice = getShaderEnabledChoice();
            if (Boolean.FALSE.equals(shaderEnabledChoice)) {
                applyIrisDisabledSettings();
                return;
            }
            if (Boolean.TRUE.equals(shaderEnabledChoice)) {
                String desired = resolveDesiredShaderPack(shaderpacksDir, fallbackShaderPackFilename);
                if (desired != null && !desired.isBlank() && !isIrisSettingsApplied(desired)) {
                    applyIrisSettingsAndProfileDefaults(shaderpacksDir, desired);
                }
            }
            try {
                Thread.sleep(FOLLOW_UP_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void applyPreferenceForShaderPack(Path shaderpacksDir, String shaderPackFilename) {
        if (shaderPackFilename == null || shaderPackFilename.isBlank()) {
            return;
        }
        Boolean shaderEnabledChoice = getShaderEnabledChoice();
        if (Boolean.TRUE.equals(shaderEnabledChoice)) {
            applyIrisSettingsAndProfileDefaults(shaderpacksDir, shaderPackFilename);
            return;
        }
        if (Boolean.FALSE.equals(shaderEnabledChoice)) {
            applyIrisDisabledSettings();
        }
    }

    private static void applyPreferredInstalledShaderSettings() {
        Path shaderpacksDir = FabricLoader.getInstance().getGameDir().resolve(SHADERPACK_DIR);
        String desired = resolveDesiredShaderPack(shaderpacksDir, null);
        applyPreferenceForShaderPack(shaderpacksDir, desired);
    }

    private static String resolveDesiredShaderPack(Path shaderpacksDir, String fallbackShaderPackFilename) {
        Optional<String> euphoriaPack = findExistingEuphoriaShaderPack(shaderpacksDir);
        if (euphoriaPack.isPresent()) {
            return euphoriaPack.get();
        }
        if (fallbackShaderPackFilename != null && !fallbackShaderPackFilename.isBlank()) {
            return fallbackShaderPackFilename;
        }
        Optional<String> existing = findExistingShaderPack(shaderpacksDir);
        return existing.orElse(null);
    }

    private static boolean isIrisSettingsApplied(String shaderPackFilename) {
        Path irisProperties = FabricLoader.getInstance().getConfigDir().resolve(IRIS_PROPERTIES);
        if (Files.notExists(irisProperties)) {
            return false;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(irisProperties)) {
            properties.load(in);
        } catch (IOException e) {
            return false;
        }
        String currentPack = properties.getProperty(IRIS_SHADER_PACK_KEY, "");
        String enabled = properties.getProperty(IRIS_ENABLE_SHADERS_KEY, "false");
        return shaderPackFilename.equals(currentPack) && "true".equalsIgnoreCase(enabled);
    }

    private static void applyIrisSettingsAndProfileDefaults(Path shaderpacksDir, String shaderPackFilename) {
        applyIrisSettings(shaderPackFilename);
        if (isEuphoriaShaderPackName(shaderPackFilename)) {
            applyPopularProfileDefaults(shaderpacksDir, shaderPackFilename);
        }
    }

    private static String waitForPreferredShaderPack(Path shaderpacksDir, String baseShaderFilename) {
        String expectedPatchedName = buildExpectedPatchedShaderPackName(baseShaderFilename);
        Optional<String> preferred = findPreferredShaderPack(shaderpacksDir, expectedPatchedName);
        if (preferred.isPresent()) {
            return preferred.get();
        }

        long deadline = System.currentTimeMillis() + PATCHED_SHADER_WAIT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(PATCHED_SHADER_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            preferred = findPreferredShaderPack(shaderpacksDir, expectedPatchedName);
            if (preferred.isPresent()) {
                return preferred.get();
            }
        }

        return baseShaderFilename;
    }

    private static Optional<String> findPreferredShaderPack(Path shaderpacksDir, String expectedPatchedName) {
        Path expected = shaderpacksDir.resolve(expectedPatchedName);
        if (isShaderPackPath(expected)) {
            return Optional.of(expectedPatchedName);
        }

        Path expectedZip = shaderpacksDir.resolve(expectedPatchedName + ".zip");
        if (isShaderPackPath(expectedZip)) {
            return Optional.of(expectedZip.getFileName().toString());
        }

        return findExistingEuphoriaShaderPack(shaderpacksDir);
    }

    private static Optional<String> findExistingShaderPack(Path shaderpacksDir) {
        if (Files.notExists(shaderpacksDir)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(shaderpacksDir)) {
            return stream.filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.startsWith(SHADER_FILENAME_PREFIX) && name.endsWith(".zip");
                    })
                    .sorted(Comparator.comparingLong(ShaderPackInstaller::safeLastModified).reversed())
                    .map(path -> path.getFileName().toString())
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> findExistingEuphoriaShaderPack(Path shaderpacksDir) {
        if (Files.notExists(shaderpacksDir)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(shaderpacksDir)) {
            return stream.filter(ShaderPackInstaller::isShaderPackPath)
                    .filter(path -> isEuphoriaShaderPackName(path.getFileName().toString()))
                    .sorted(Comparator.comparingLong(ShaderPackInstaller::safeLastModified).reversed())
                    .map(path -> path.getFileName().toString())
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static boolean isShaderPackPath(Path path) {
        if (Files.isDirectory(path)) {
            return true;
        }
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".zip");
    }

    private static boolean isEuphoriaShaderPackName(String shaderPackName) {
        return shaderPackName != null
                && shaderPackName.toLowerCase(Locale.ROOT).contains(EUPHORIA_SHADER_NAME_TOKEN);
    }

    private static String buildExpectedPatchedShaderPackName(String baseShaderFilename) {
        String baseName = baseShaderFilename;
        if (baseName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        return baseName + EUPHORIA_PATCH_SUFFIX;
    }

    private static void applyPopularProfileDefaults(Path shaderpacksDir, String shaderPackFilename) {
        Map<String, String> popularSettings = loadPopularProfileSettings(shaderpacksDir, shaderPackFilename);
        if (popularSettings.isEmpty()) {
            LOGGER.warn("Nem sikerült kiolvasni a POPULAR profil beállításait ehhez: {}", shaderPackFilename);
            return;
        }

        Path profileSettingsFile = shaderpacksDir.resolve(shaderPackFilename + ".txt");
        Properties properties = new Properties();
        if (Files.exists(profileSettingsFile)) {
            try (InputStream in = Files.newInputStream(profileSettingsFile)) {
                properties.load(in);
            } catch (IOException e) {
                LOGGER.warn("Nem sikerült beolvasni a shaderprofil beállításait: {}", profileSettingsFile, e);
            }
        }

        popularSettings.forEach(properties::setProperty);

        try (OutputStream out = Files.newOutputStream(profileSettingsFile)) {
            properties.store(out, "Minewild Euphoria alapértelmezett beállítások");
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült menteni a shaderprofil beállításait: {}", profileSettingsFile, e);
        }
    }

    private static Map<String, String> loadPopularProfileSettings(Path shaderpacksDir, String shaderPackFilename) {
        Path shaderPackPath = resolveShaderPackPath(shaderpacksDir, shaderPackFilename);
        if (shaderPackPath == null) {
            return Map.of();
        }

        Optional<String> popularProfile = readPopularProfileValue(shaderPackPath);
        if (popularProfile.isEmpty() || popularProfile.get().isBlank()) {
            return Map.of();
        }

        return parseProfileSettings(popularProfile.get());
    }

    private static Path resolveShaderPackPath(Path shaderpacksDir, String shaderPackFilename) {
        Path direct = shaderpacksDir.resolve(shaderPackFilename);
        if (isShaderPackPath(direct)) {
            return direct;
        }

        Path withZip = shaderpacksDir.resolve(shaderPackFilename + ".zip");
        if (isShaderPackPath(withZip)) {
            return withZip;
        }

        return null;
    }

    private static Optional<String> readPopularProfileValue(Path shaderPackPath) {
        if (Files.isDirectory(shaderPackPath)) {
            Path propertiesPath = shaderPackPath.resolve("shaders").resolve("shaders.properties");
            if (Files.notExists(propertiesPath)) {
                return Optional.empty();
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(propertiesPath)) {
                properties.load(in);
                return Optional.ofNullable(properties.getProperty(POPULAR_PROFILE_KEY));
            } catch (IOException e) {
                return Optional.empty();
            }
        }

        if (!Files.isRegularFile(shaderPackPath)) {
            return Optional.empty();
        }

        try (ZipFile zip = new ZipFile(shaderPackPath.toFile())) {
            ZipEntry entry = zip.getEntry(SHADER_PROPERTIES_ENTRY);
            if (entry == null) {
                return Optional.empty();
            }
            Properties properties = new Properties();
            try (InputStream in = zip.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(reader)) {
                properties.load(bufferedReader);
                return Optional.ofNullable(properties.getProperty(POPULAR_PROFILE_KEY));
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Map<String, String> parseProfileSettings(String profileSettingsLine) {
        Map<String, String> values = new LinkedHashMap<>();
        String[] tokens = profileSettingsLine.trim().split("\\s+");
        for (String token : tokens) {
            if (token == null || token.isBlank() || token.startsWith("profile.")) {
                continue;
            }
            if (token.startsWith("!") && token.length() > 1) {
                values.put(token.substring(1), "false");
                continue;
            }
            int equalsIndex = token.indexOf('=');
            if (equalsIndex > 0) {
                String key = token.substring(0, equalsIndex);
                String value = token.substring(equalsIndex + 1);
                values.put(key, value);
                continue;
            }
            values.put(token, "true");
        }
        return values;
    }

    private static long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static void applyIrisSettings(String shaderPackFilename) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült létrehozni a konfigurációs mappát: {}", configDir, e);
            return;
        }

        Path irisProperties = configDir.resolve(IRIS_PROPERTIES);
        Properties properties = new Properties();
        if (Files.exists(irisProperties)) {
            try (InputStream in = Files.newInputStream(irisProperties)) {
                properties.load(in);
            } catch (IOException e) {
                LOGGER.warn("Nem sikerült beolvasni az Iris beállításait: {}", irisProperties, e);
            }
        }

        properties.setProperty(IRIS_SHADER_PACK_KEY, shaderPackFilename);
        properties.setProperty(IRIS_ENABLE_SHADERS_KEY, "true");

        try (OutputStream out = Files.newOutputStream(irisProperties)) {
            properties.store(out, "Minewild Iris alapértelmezett beállítások");
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült menteni az Iris beállításait: {}", irisProperties, e);
        }
    }

    private static void applyIrisDisabledSettings() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült létrehozni a konfigurációs mappát: {}", configDir, e);
            return;
        }

        Path irisProperties = configDir.resolve(IRIS_PROPERTIES);
        Properties properties = new Properties();
        if (Files.exists(irisProperties)) {
            try (InputStream in = Files.newInputStream(irisProperties)) {
                properties.load(in);
            } catch (IOException e) {
                LOGGER.warn("Nem sikerült beolvasni az Iris beállításait: {}", irisProperties, e);
            }
        }

        properties.setProperty(IRIS_ENABLE_SHADERS_KEY, "false");

        try (OutputStream out = Files.newOutputStream(irisProperties)) {
            properties.store(out, "Minewild Iris alapértelmezett beállítások");
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült menteni az Iris beállításait: {}", irisProperties, e);
        }
    }

    private static Boolean getShaderEnabledChoice() {
        if (shaderChoiceLoaded) {
            return cachedShaderEnabledChoice;
        }
        synchronized (SHADER_CHOICE_LOCK) {
            if (shaderChoiceLoaded) {
                return cachedShaderEnabledChoice;
            }
            cachedShaderEnabledChoice = readShaderEnabledChoice();
            shaderChoiceLoaded = true;
            return cachedShaderEnabledChoice;
        }
    }

    private static void storeShaderEnabledChoice(boolean enabled) {
        synchronized (SHADER_CHOICE_LOCK) {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                LOGGER.warn("Nem sikerült létrehozni a konfigurációs mappát: {}", configDir, e);
            }

            Path choiceFile = configDir.resolve(SHADER_CHOICE_PROPERTIES);
            Properties properties = new Properties();
            if (Files.exists(choiceFile)) {
                try (InputStream in = Files.newInputStream(choiceFile)) {
                    properties.load(in);
                } catch (IOException e) {
                    LOGGER.warn("Nem sikerült beolvasni a shader választást: {}", choiceFile, e);
                }
            }

            properties.setProperty(SHADER_CHOICE_KEY, Boolean.toString(enabled));

            try (OutputStream out = Files.newOutputStream(choiceFile)) {
                properties.store(out, "Minewild shader választás");
            } catch (IOException e) {
                LOGGER.warn("Nem sikerült menteni a shader választást: {}", choiceFile, e);
            }

            cachedShaderEnabledChoice = enabled;
            shaderChoiceLoaded = true;
        }
    }

    private static Boolean readShaderEnabledChoice() {
        Path choiceFile = FabricLoader.getInstance().getConfigDir().resolve(SHADER_CHOICE_PROPERTIES);
        if (Files.notExists(choiceFile)) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(choiceFile)) {
            properties.load(in);
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült beolvasni a shader választást: {}", choiceFile, e);
            return null;
        }

        String value = properties.getProperty(SHADER_CHOICE_KEY);
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static ShaderLookup fetchLatestVersion(String slug) {
        URI uri;
        try {
            uri = new URI(MODRINTH_VERSION_URL_PREFIX + slug + SHADER_VERSION_URL_SUFFIX);
        } catch (URISyntaxException e) {
            LOGGER.warn("Érvénytelen Modrinth URL ehhez: {}", slug, e);
            return ShaderLookup.failed();
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "MinewildShaderInstaller/1.0")
                .build();

        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                LOGGER.warn("Modrinth keresés sikertelen ehhez: {} (HTTP {})", slug, response.statusCode());
                return ShaderLookup.failed();
            }
            ShaderVersion[] versions = GSON.fromJson(response.body(), ShaderVersion[].class);
            if (versions == null || versions.length == 0) {
                return ShaderLookup.noMatch();
            }
            return ShaderLookup.success(versions[0]);
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült lekérdezni a Modrinth-et ehhez: {}", slug, e);
            return ShaderLookup.failed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("A Modrinth-keresés megszakadt ehhez: {}", slug, e);
            return ShaderLookup.failed();
        }
    }

    private static ShaderFile pickPrimaryFile(ShaderVersion version) {
        if (version == null || version.files == null || version.files.isEmpty()) {
            return null;
        }
        for (ShaderFile file : version.files) {
            if (file != null && file.primary) {
                return file;
            }
        }
        for (ShaderFile file : version.files) {
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private static boolean downloadTo(String url, Path target) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            LOGGER.warn("Érvénytelen URL: {}", url, e);
            return false;
        }

        Path temp = target.resolveSibling(target.getFileName() + ".download");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "MinewildShaderInstaller/1.0")
                .build();

        try {
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                LOGGER.warn("A letöltés sikertelen (HTTP {}): {}", response.statusCode(), url);
                return false;
            }
            try (InputStream body = new BufferedInputStream(response.body())) {
                Files.copy(body, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(temp) == 0) {
                LOGGER.warn("A letöltött shaderfájl üres: {}", url);
                return false;
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült letölteni: {}", url, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("A letöltés megszakadt: {}", url, e);
            return false;
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException e) {
            }
        }
    }

    private static String resolveGameVersion() {
        Optional<ModContainer> minecraft = FabricLoader.getInstance().getModContainer("minecraft");
        if (minecraft.isPresent()) {
            String version = minecraft.get().getMetadata().getVersion().getFriendlyString();
            if (version != null && !version.isBlank()) {
                return normalizeGameVersion(version);
            }
        }
        return ISMERETLEN_VERZIO;
    }

    private static String normalizeGameVersion(String version) {
        String trimmed = version.trim();
        int plusIndex = trimmed.indexOf('+');
        if (plusIndex > 0) {
            trimmed = trimmed.substring(0, plusIndex);
        }
        return trimmed;
    }

    private static String buildShaderVersionUrlSuffix() {
        if (GAME_VERSION == null || GAME_VERSION.isBlank() || ISMERETLEN_VERZIO.equals(GAME_VERSION)) {
            return "/version?loaders=[%22iris%22]";
        }
        return "/version?loaders=[%22iris%22]&game_versions=[%22" + GAME_VERSION + "%22]";
    }

    private static final class ShaderVersion {
        private List<ShaderFile> files;
    }

    private static final class ShaderLookup {
        private final ShaderVersion version;
        private final boolean noMatch;

        private ShaderLookup(ShaderVersion version, boolean noMatch) {
            this.version = version;
            this.noMatch = noMatch;
        }

        private static ShaderLookup success(ShaderVersion version) {
            return new ShaderLookup(version, false);
        }

        private static ShaderLookup noMatch() {
            return new ShaderLookup(null, true);
        }

        private static ShaderLookup failed() {
            return new ShaderLookup(null, false);
        }
    }

    private static final class ShaderFile {
        private String url;
        private String filename;
        private boolean primary;
    }
}
