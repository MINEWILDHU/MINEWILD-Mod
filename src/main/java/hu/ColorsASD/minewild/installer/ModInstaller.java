package hu.ColorsASD.minewild.installer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ModInstaller {
    private static final Logger LOGGER = LoggerFactory.getLogger("MinewildInstaller");
    private static final Gson GSON = new GsonBuilder().create();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final String ISMERETLEN_VERZIO = "ismeretlen";

    private static final String GAME_VERSION = resolveGameVersion();
    private static final String MODRINTH_VERSION_URL_PREFIX = "https://api.modrinth.com/v2/project/";
    private static final String MODRINTH_VERSION_URL_SUFFIX =
            "/version?loaders=[%22fabric%22]&game_versions=[%22" + GAME_VERSION + "%22]";

    private static final String OWN_MOD_ID = "minewild";
    private static final List<RequiredMod> REQUIRED_MODS = List.of(
            new RequiredMod("fabric-api", "fabric-api"),
            new RequiredMod("badoptimizations", "badoptimizations"),
            new RequiredMod("better-block-entities", "betterblockentities"),
            new RequiredMod("cloth-config", "cloth-config"),
            new RequiredMod("polytone", "polytone"),
            new RequiredMod("continuity", "continuity"),
            new RequiredMod("distanthorizons", "distanthorizons"),
            new RequiredMod("entityculling", "entityculling"),
            new RequiredMod("entitytexturefeatures", "entity_texture_features"),
            new RequiredMod("entity-model-features", "entity_model_features"),
            new RequiredMod("ferrite-core", "ferritecore"),
            new RequiredMod("immediatelyfast", "immediatelyfast"),
            new RequiredMod("indium", "indium"),
            new RequiredMod("iris", "iris"),
            new RequiredMod("euphoria-patches", "euphoria_patcher"),
            new RequiredMod("lithium", "lithium"),
            new RequiredMod("modernfix-mvus", "modernfix"),
            new RequiredMod("moreculling", "moreculling"),
            new RequiredMod("noxesium", "noxesium"),
            new RequiredMod("reeses-sodium-options", "reeses-sodium-options"),
            new RequiredMod("scalablelux", "scalablelux"),
            new RequiredMod("sodium", "sodium"),
            new RequiredMod("sodium-extra", "sodium-extra")
    );
    private static final Set<String> ALLOWED_MOD_IDS = buildAllowedModIds();
    private static final int DELETE_RETRY_COUNT = 20;
    private static final int DELETE_RETRY_DELAY_MS = 500;
    private static final int DELETE_WAIT_TIMEOUT_MS = 300_000;
    private static final int DELETE_WAIT_POLL_MS = 500;
    private static final int DELETE_ERROR_EXIT_MS = 60_000;
    private static final DateTimeFormatter TASK_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile boolean restartRequired = false;
    private static volatile boolean downloadInProgress = false;
    private static volatile int downloadTotal = 0;
    private static volatile int downloadDone = 0;
    private static volatile boolean downloadFailed = false;
    private static volatile boolean extraModsDetected = false;

    private ModInstaller() {
    }

    public static void beginInstallIfNeeded() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        ShaderPackInstaller.beginInstallIfNeeded();

        downloadTotal = 0;
        downloadDone = 0;
        downloadInProgress = false;
        downloadFailed = false;
        extraModsDetected = false;

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        boolean hasExtraMods = hasExtraMods(modsDir);
        if (hasExtraMods) {
            extraModsDetected = true;
        }
        if (areAllModsLoaded() && !hasExtraMods) {
            restartRequired = false;
            return;
        }

        restartRequired = true;

        Thread worker = new Thread(() -> downloadMissingMods(modsDir), "minewild-mod-installer");
        worker.setDaemon(true);
        worker.start();
    }

    public static boolean isRestartRequired() {
        return restartRequired;
    }

    public static boolean isDownloadInProgress() {
        return downloadInProgress;
    }

    public static float getDownloadProgress() {
        int total = downloadTotal;
        if (total <= 0) {
            return downloadInProgress ? 0.0f : 1.0f;
        }
        return Math.min(1.0f, downloadDone / (float) total);
    }

    public static boolean hasDownloadFailed() {
        return downloadFailed;
    }

    public static boolean hasExtraModsDetected() {
        return extraModsDetected;
    }

    public static void requestExtraModDeletion() {
        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        removeExtraMods(modsDir);
    }

    private static boolean areAllModsLoaded() {
        for (RequiredMod mod : REQUIRED_MODS) {
            if (!FabricLoader.getInstance().isModLoaded(mod.modId)) {
                return false;
            }
        }
        return true;
    }

    private static void downloadMissingMods(Path modsDir) {
        downloadTotal = 0;
        downloadDone = 0;
        downloadInProgress = false;
        downloadFailed = false;

        try {
            Files.createDirectories(modsDir);
        } catch (IOException e) {
            LOGGER.error("Nem sikerült létrehozni a modok mappáját: {}", modsDir, e);
            downloadFailed = true;
            return;
        }

        Set<String> existingIds = scanInstalledModIds(modsDir);
        List<RequiredMod> missingMods = new ArrayList<>();
        for (RequiredMod mod : REQUIRED_MODS) {
            if (FabricLoader.getInstance().isModLoaded(mod.modId)) {
                continue;
            }
            if (existingIds.contains(mod.modId)) {
                continue;
            }
            missingMods.add(mod);
        }

        if (missingMods.isEmpty()) {
            return;
        }

        downloadTotal = missingMods.size();
        downloadDone = 0;
        downloadInProgress = true;
        boolean anyInstallable = false;
        try {
            for (RequiredMod mod : missingMods) {
                try {
                    ModrinthLookup lookup = fetchLatestVersion(mod.slug);
                    if (lookup == null || lookup.version == null) {
                        if (lookup != null && lookup.noMatch) {
                            continue;
                        }
                        downloadFailed = true;
                        continue;
                    }
                    anyInstallable = true;
                    ModrinthVersion version = lookup.version;
                    ModrinthFile file = pickPrimaryFile(version);
                    if (file == null || file.url == null || file.filename == null) {
                        LOGGER.warn("Nincs letölthető fájl ehhez: {}", mod.slug);
                        downloadFailed = true;
                        continue;
                    }
                    Path target = modsDir.resolve(file.filename);
                    if (Files.exists(target)) {
                        continue;
                    }
                    if (!downloadTo(file.url, target)) {
                        downloadFailed = true;
                    }
                } finally {
                    downloadDone++;
                }
            }
        } finally {
            downloadInProgress = false;
            if (!downloadFailed && !extraModsDetected && !anyInstallable) {
                restartRequired = false;
            }
        }
    }

    private static boolean hasExtraMods(Path modsDir) {
        if (Files.notExists(modsDir)) {
            return false;
        }
        Set<Path> selfPaths = getSelfPaths();
        try (Stream<Path> stream = Files.list(modsDir)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .anyMatch(path -> isExtraModFile(path, selfPaths));
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült beolvasni a modok mappáját: {}", modsDir, e);
            return false;
        }
    }

    private static void removeExtraMods(Path modsDir) {
        List<Path> extraMods = findExtraModFiles(modsDir);
        if (extraMods.isEmpty()) {
            extraModsDetected = false;
            return;
        }
        extraModsDetected = true;
        List<Path> pending = new ArrayList<>();
        for (Path path : extraMods) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                pending.add(path);
            }
        }
        if (!pending.isEmpty()) {
            LOGGER.warn("Extra modok nem törölhetők futás közben, kilépés után törlődnek: {}",
                    formatFileList(pending));
            scheduleDeleteAfterExit(pending);
        }
        extraModsDetected = !findExtraModFiles(modsDir).isEmpty();
    }

    private static List<Path> findExtraModFiles(Path modsDir) {
        List<Path> extraMods = new ArrayList<>();
        if (Files.notExists(modsDir)) {
            return extraMods;
        }
        Set<Path> selfPaths = getSelfPaths();
        try (Stream<Path> stream = Files.list(modsDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> isExtraModFile(path, selfPaths))
                    .forEach(extraMods::add);
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült beolvasni a modok mappáját: {}", modsDir, e);
        }
        return extraMods;
    }

    private static boolean isExtraModFile(Path jarPath, Set<Path> selfPaths) {
        Path normalized = jarPath.toAbsolutePath().normalize();
        if (selfPaths.contains(normalized)) {
            return false;
        }
        Optional<Set<String>> ids = readModIds(jarPath);
        if (ids.isEmpty()) {
            return true;
        }
        for (String id : ids.get()) {
            if (ALLOWED_MOD_IDS.contains(id)) {
                return false;
            }
        }
        return true;
    }

    private static Set<Path> getSelfPaths() {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(OWN_MOD_ID);
        if (container.isEmpty()) {
            return Set.of();
        }
        Set<Path> paths = new HashSet<>();
        for (Path path : container.get().getOrigin().getPaths()) {
            paths.add(path.toAbsolutePath().normalize());
        }
        return paths;
    }

    private static Set<String> scanInstalledModIds(Path modsDir) {
        Set<String> found = new HashSet<>();
        if (Files.notExists(modsDir)) {
            return found;
        }
        try (Stream<Path> stream = Files.list(modsDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .forEach(path -> readModIds(path).ifPresent(found::addAll));
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült beolvasni a modok mappáját: {}", modsDir, e);
        }
        return found;
    }

    private static Optional<Set<String>> readModIds(Path jarPath) {
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            if (entry == null) {
                return Optional.empty();
            }
            try (InputStream in = zip.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                FabricModJson mod = GSON.fromJson(reader, FabricModJson.class);
                if (mod == null || mod.id == null || mod.id.isBlank()) {
                    return Optional.empty();
                }
                Set<String> ids = new HashSet<>();
                ids.add(mod.id);
                if (mod.provides != null) {
                    for (String provided : mod.provides) {
                        if (provided != null && !provided.isBlank()) {
                            ids.add(provided);
                        }
                    }
                }
                return Optional.of(ids);
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static void scheduleDeleteAfterExit(List<Path> paths) {
        if (paths.isEmpty()) {
            return;
        }
        long pid = ProcessHandle.current().pid();
        try {
            if (isWindows()) {
                if (scheduleDeleteWithTaskScheduler(pid, paths)) {
                    return;
                }
                String script = buildPowerShellDeleteScript(pid, paths);
                new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-WindowStyle", "Hidden", "-Command", script).start();
            } else {
                String script = buildShDeleteScript(pid, paths);
                new ProcessBuilder("sh", "-c", script).start();
            }
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült ütemezni az extra modok törlését.", e);
        }
    }

    private static boolean isWindows() {
        String name = System.getProperty("os.name");
        return name != null && name.toLowerCase().contains("win");
    }

    private static boolean scheduleDeleteWithTaskScheduler(long pid, List<Path> paths) {
        String taskName = "MinewildDelete_" + pid + "_" + System.currentTimeMillis();
        Path scriptPath = null;
        try {
            scriptPath = writePowerShellDeleteScript(pid, paths, taskName);
            String startTime = getTaskStartTime();
            String taskCommand = "powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File \"" + scriptPath + "\"";
            Process create = new ProcessBuilder("schtasks", "/Create", "/TN", taskName, "/TR", taskCommand,
                    "/SC", "ONCE", "/ST", startTime, "/F").start();
            int exit = create.waitFor();
            if (exit != 0) {
                LOGGER.warn("Nem sikerült ütemezett feladatot létrehozni (kilépési kód: {}).", exit);
                deleteQuietly(scriptPath);
                return false;
            }
            new ProcessBuilder("schtasks", "/Run", "/TN", taskName).start();
            return true;
        } catch (IOException e) {
            LOGGER.warn("Ütemezett törlés feladat létrehozása sikertelen.", e);
            deleteQuietly(scriptPath);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Ütemezett törlés feladat megszakítva.", e);
            deleteQuietly(scriptPath);
            return false;
        }
    }

    private static Path writePowerShellDeleteScript(long pid, List<Path> paths, String taskName) throws IOException {
        Path scriptPath = Files.createTempFile("minewild-delete-", ".ps1");
        StringBuilder builder = new StringBuilder();
        builder.append("$ErrorActionPreference = 'SilentlyContinue'\n");
        builder.append("$targetPid = ").append(pid).append("\n");
        builder.append("$paths = @(");
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("'").append(escapePowerShell(paths.get(i).toString())).append("'");
        }
        builder.append(")\n");
        builder.append("$taskName = '").append(escapePowerShell(taskName)).append("'\n");
        builder.append("$start = Get-Date\n");
        builder.append("$timeoutMs = ").append(DELETE_WAIT_TIMEOUT_MS).append("\n");
        builder.append("$errorStart = $null\n");
        builder.append("function StartErrorTimeout { if ($null -eq $errorStart) { $errorStart = Get-Date } }\n");
        builder.append("function ErrorTimeoutReached { ");
        builder.append("return ($null -ne $errorStart -and ((Get-Date) - $errorStart).TotalMilliseconds -ge ")
                .append(DELETE_ERROR_EXIT_MS).append(") }\n");
        builder.append("while (Get-Process -Id $targetPid -ErrorAction SilentlyContinue) {\n");
        builder.append("  if (((Get-Date) - $start).TotalMilliseconds -ge $timeoutMs) {\n");
        builder.append("    break\n");
        builder.append("  }\n");
        builder.append("  Start-Sleep -Milliseconds ").append(DELETE_WAIT_POLL_MS).append("\n");
        builder.append("}\n");
        builder.append("foreach ($p in $paths) {\n");
        builder.append("  $deleted = $false\n");
        builder.append("  for ($i = 0; $i -lt ").append(DELETE_RETRY_COUNT).append("; $i++) {\n");
        builder.append("    if (ErrorTimeoutReached) { exit 0 }\n");
        builder.append("    if (-not (Test-Path -LiteralPath $p)) { $deleted = $true; break }\n");
        builder.append("    try { Remove-Item -LiteralPath $p -Force -ErrorAction Stop; $deleted = $true; break }");
        builder.append(" catch { StartErrorTimeout; Start-Sleep -Milliseconds ").append(DELETE_RETRY_DELAY_MS).append(" }\n");
        builder.append("  }\n");
        builder.append("  if (-not $deleted) { StartErrorTimeout }\n");
        builder.append("  if (ErrorTimeoutReached) { exit 0 }\n");
        builder.append("}\n");
        builder.append("try { schtasks /Delete /TN $taskName /F | Out-Null } catch { }\n");
        builder.append("try { Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue } catch { }\n");
        Files.writeString(scriptPath, builder.toString(), StandardCharsets.UTF_8);
        return scriptPath;
    }

    private static String getTaskStartTime() {
        LocalTime now = LocalTime.now();
        LocalTime start = now.plusMinutes(1);
        if (start.isBefore(now)) {
            start = now;
        }
        return TASK_TIME_FORMAT.format(start);
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
        }
    }

    private static String buildPowerShellDeleteScript(long pid, List<Path> paths) {
        StringBuilder builder = new StringBuilder();
        builder.append("$targetPid=").append(pid).append("; ");
        builder.append("$start=Get-Date; ");
        builder.append("$timeoutMs=").append(DELETE_WAIT_TIMEOUT_MS).append("; ");
        builder.append("$errorStart=$null; ");
        builder.append("function StartErrorTimeout { if ($null -eq $errorStart) { $errorStart=Get-Date } }; ");
        builder.append("function ErrorTimeoutReached { return ($null -ne $errorStart -and ");
        builder.append("((Get-Date)-$errorStart).TotalMilliseconds -ge ").append(DELETE_ERROR_EXIT_MS).append(") }; ");
        builder.append("while (Get-Process -Id $targetPid -ErrorAction SilentlyContinue) { ");
        builder.append("if (((Get-Date) - $start).TotalMilliseconds -ge $timeoutMs) { break }; ");
        builder.append("Start-Sleep -Milliseconds ").append(DELETE_WAIT_POLL_MS).append(" }; ");
        builder.append("$paths=@(");
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("'").append(escapePowerShell(paths.get(i).toString())).append("'");
        }
        builder.append("); ");
        builder.append("foreach ($p in $paths) { ");
        builder.append("$deleted=$false; ");
        builder.append("for ($i=0; $i -lt ").append(DELETE_RETRY_COUNT).append("; $i++) { ");
        builder.append("if (ErrorTimeoutReached) { exit }; ");
        builder.append("if (-not (Test-Path -LiteralPath $p)) { $deleted=$true; break }; ");
        builder.append("try { Remove-Item -LiteralPath $p -Force -ErrorAction Stop; $deleted=$true; break } ");
        builder.append("catch { StartErrorTimeout; Start-Sleep -Milliseconds ").append(DELETE_RETRY_DELAY_MS).append(" } ");
        builder.append("}; ");
        builder.append("if (-not $deleted) { StartErrorTimeout }; ");
        builder.append("if (ErrorTimeoutReached) { exit }; ");
        builder.append("};");
        return builder.toString();
    }

    private static String buildShDeleteScript(long pid, List<Path> paths) {
        StringBuilder builder = new StringBuilder();
        builder.append("start=$(date +%s); ");
        builder.append("timeout=").append(DELETE_WAIT_TIMEOUT_MS / 1000).append("; ");
        builder.append("while kill -0 ").append(pid).append(" 2>/dev/null; do ");
        builder.append("now=$(date +%s); ");
        builder.append("if [ $((now-start)) -ge $timeout ]; then break; fi; ");
        builder.append("sleep 0.5; ");
        builder.append("done; ");
        builder.append("rm -f");
        for (Path path : paths) {
            builder.append(" '").append(escapeSh(path.toString())).append("'");
        }
        return builder.toString();
    }

    private static String escapePowerShell(String value) {
        return value.replace("'", "''");
    }

    private static String escapeSh(String value) {
        return value.replace("'", "'\\''");
    }

    private static String formatFileList(List<Path> paths) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(paths.get(i).getFileName());
        }
        return builder.toString();
    }

    private static Set<String> buildAllowedModIds() {
        Set<String> ids = new HashSet<>();
        ids.add(OWN_MOD_ID);
        for (RequiredMod mod : REQUIRED_MODS) {
            ids.add(mod.modId);
        }
        return Set.copyOf(ids);
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

    private static ModrinthLookup fetchLatestVersion(String slug) {
        URI uri;
        try {
            uri = new URI(MODRINTH_VERSION_URL_PREFIX + slug + MODRINTH_VERSION_URL_SUFFIX);
        } catch (URISyntaxException e) {
            LOGGER.warn("Érvénytelen Modrinth URL ehhez: {}", slug, e);
            return ModrinthLookup.failed();
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "MinewildInstaller/1.0")
                .build();

        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                LOGGER.warn("Modrinth kérés sikertelen ehhez: {} (HTTP {})", slug, response.statusCode());
                return ModrinthLookup.failed();
            }
            ModrinthVersion[] versions = GSON.fromJson(response.body(), ModrinthVersion[].class);
            if (versions == null || versions.length == 0) {
                LOGGER.warn("Nincs Modrinth verzió ehhez: {}", slug);
                return ModrinthLookup.noMatch();
            }
            return ModrinthLookup.success(versions[0]);
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült lekérdezni a Modrinth-et ehhez: {}", slug, e);
            return ModrinthLookup.failed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("A Modrinth kérés megszakadt ehhez: {}", slug, e);
            return ModrinthLookup.failed();
        }
    }

    private static ModrinthFile pickPrimaryFile(ModrinthVersion version) {
        if (version == null || version.files == null || version.files.isEmpty()) {
            return null;
        }
        for (ModrinthFile file : version.files) {
            if (file != null && file.primary) {
                return file;
            }
        }
        for (ModrinthFile file : version.files) {
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
                .header("User-Agent", "MinewildInstaller/1.0")
                .build();

        try {
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                LOGGER.warn("Letöltés sikertelen (HTTP {}): {}", response.statusCode(), url);
                return false;
            }
            try (InputStream body = new BufferedInputStream(response.body())) {
                Files.copy(body, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(temp) == 0) {
                LOGGER.warn("A letöltött fájl üres: {}", url);
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

    private static final class RequiredMod {
        private final String slug;
        private final String modId;

        private RequiredMod(String slug, String modId) {
            this.slug = slug;
            this.modId = modId;
        }
    }

    private static final class ModrinthVersion {
        private List<ModrinthFile> files;
    }

    private static final class ModrinthLookup {
        private final ModrinthVersion version;
        private final boolean noMatch;

        private ModrinthLookup(ModrinthVersion version, boolean noMatch) {
            this.version = version;
            this.noMatch = noMatch;
        }

        private static ModrinthLookup success(ModrinthVersion version) {
            return new ModrinthLookup(version, false);
        }

        private static ModrinthLookup noMatch() {
            return new ModrinthLookup(null, true);
        }

        private static ModrinthLookup failed() {
            return new ModrinthLookup(null, false);
        }
    }

    private static final class ModrinthFile {
        private String url;
        private String filename;
        private boolean primary;
    }

    private static final class FabricModJson {
        private String id;
        private List<String> provides;
    }
}
