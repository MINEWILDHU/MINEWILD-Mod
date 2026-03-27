package hu.ColorsASD.minewild.installer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OwnModUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger("MinewildOwnModUpdater");
    private static final Gson GSON = new GsonBuilder().create();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final String OWN_MOD_ID = "minewild";
    private static final String GITHUB_LATEST_RELEASE_URL =
            "https://api.github.com/repos/MINEWILDHU/MINEWILD-Mod/releases/latest";
    private static final String MODS_DIR = "mods";
    private static final String SHADERPACKS_DIR = "shaderpacks";
    private static final int EXIT_WAIT_TIMEOUT_MS = 300_000;
    private static final int EXIT_WAIT_POLL_MS = 500;
    private static final int DELETE_RETRY_COUNT = 20;
    private static final int DELETE_RETRY_DELAY_MS = 500;
    private static final int DOWNLOAD_RETRY_COUNT = 5;
    private static final int DOWNLOAD_RETRY_DELAY_MS = 1_500;

    private OwnModUpdater() {
    }

    public static boolean beginUpdateIfNeeded() {
        if (!STARTED.compareAndSet(false, true)) {
            return false;
        }

        Optional<ModContainer> ownMod = FabricLoader.getInstance().getModContainer(OWN_MOD_ID);
        if (ownMod.isEmpty()) {
            return false;
        }

        String currentVersion = normalizeVersion(ownMod.get().getMetadata().getVersion().getFriendlyString());
        if (currentVersion == null) {
            return false;
        }

        GitHubRelease latestRelease = fetchLatestRelease();
        if (latestRelease == null || latestRelease.tag_name == null || latestRelease.tag_name.isBlank()) {
            return false;
        }

        String latestVersion = normalizeVersion(latestRelease.tag_name);
        if (latestVersion == null || compareVersions(latestVersion, currentVersion) <= 0) {
            return false;
        }

        GitHubReleaseAsset asset = pickJarAsset(latestRelease);
        if (asset == null || asset.browser_download_url == null || asset.browser_download_url.isBlank()
                || asset.name == null || asset.name.isBlank()) {
            LOGGER.warn("Nem található letölthető saját mod fájl a legfrissebb GitHub release-ben: {}", latestRelease.tag_name);
            return false;
        }

        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path modsDir = gameDir.resolve(MODS_DIR);
        Path shaderpacksDir = gameDir.resolve(SHADERPACKS_DIR);
        Path targetJar = modsDir.resolve(asset.name);

        if (!startPowerShellUpdateProcess(ProcessHandle.current().pid(), modsDir, shaderpacksDir, targetJar,
                asset.browser_download_url)) {
            return false;
        }

        LOGGER.info("Saját mod frissítés ütemezve: {} -> {}", currentVersion, latestVersion);
        return true;
    }

    private static GitHubRelease fetchLatestRelease() {
        URI uri;
        try {
            uri = new URI(GITHUB_LATEST_RELEASE_URL);
        } catch (URISyntaxException e) {
            LOGGER.warn("Érvénytelen GitHub release URL.", e);
            return null;
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MinewildOwnModUpdater/1.0")
                .build();

        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                LOGGER.warn("A GitHub release ellenőrzés sikertelen (HTTP {}).", response.statusCode());
                return null;
            }
            return GSON.fromJson(response.body(), GitHubRelease.class);
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült lekérdezni a legfrissebb GitHub release-t.", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("A GitHub release ellenőrzés megszakadt.", e);
            return null;
        }
    }

    private static GitHubReleaseAsset pickJarAsset(GitHubRelease release) {
        if (release == null || release.assets == null || release.assets.isEmpty()) {
            return null;
        }
        for (GitHubReleaseAsset asset : release.assets) {
            if (isOwnModJarAsset(asset)) {
                return asset;
            }
        }
        for (GitHubReleaseAsset asset : release.assets) {
            if (asset != null && asset.name != null
                    && asset.name.toLowerCase().endsWith(".jar")
                    && !asset.name.toLowerCase().endsWith("-sources.jar")) {
                return asset;
            }
        }
        return null;
    }

    private static boolean isOwnModJarAsset(GitHubReleaseAsset asset) {
        if (asset == null || asset.name == null) {
            return false;
        }
        String name = asset.name.trim().toLowerCase();
        return name.startsWith("minewild-fabric-")
                && name.endsWith(".jar")
                && !name.endsWith("-sources.jar");
    }

    private static String normalizeVersion(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        while (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.isBlank() ? null : trimmed;
    }

    private static int compareVersions(String left, String right) {
        List<Integer> leftParts = extractVersionParts(left);
        List<Integer> rightParts = extractVersionParts(right);
        if (leftParts.isEmpty() && rightParts.isEmpty()) {
            String leftNormalized = left == null ? "" : left;
            String rightNormalized = right == null ? "" : right;
            return leftNormalized.compareToIgnoreCase(rightNormalized);
        }
        int max = Math.max(leftParts.size(), rightParts.size());
        for (int i = 0; i < max; i++) {
            int leftValue = i < leftParts.size() ? leftParts.get(i) : 0;
            int rightValue = i < rightParts.size() ? rightParts.get(i) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static List<Integer> extractVersionParts(String value) {
        List<Integer> parts = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return parts;
        }
        String[] tokens = value.split("[^0-9]+");
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                parts.add(Integer.parseInt(token));
            } catch (NumberFormatException e) {
                LOGGER.debug("Nem sikerült verziószám-részt parse-olni: {}", token);
            }
        }
        return parts;
    }

    private static boolean startPowerShellUpdateProcess(long pid, Path modsDir, Path shaderpacksDir, Path targetJar,
                                                        String downloadUrl) {
        Path scriptPath = null;
        try {
            scriptPath = Files.createTempFile("minewild-own-mod-update-", ".ps1");
            Files.writeString(scriptPath,
                    buildPowerShellUpdateScript(pid, modsDir, shaderpacksDir, targetJar, downloadUrl),
                    StandardCharsets.UTF_8);
            new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden",
                    "-File", scriptPath.toString()
            ).start();
            return true;
        } catch (IOException e) {
            LOGGER.warn("Nem sikerült elindítani a saját mod PowerShell frissítését.", e);
            deleteQuietly(scriptPath);
            return false;
        }
    }

    private static String buildPowerShellUpdateScript(long pid, Path modsDir, Path shaderpacksDir, Path targetJar,
                                                      String downloadUrl) {
        StringBuilder builder = new StringBuilder();
        builder.append("$targetPid=").append(pid).append("\n");
        builder.append("$modsDir='").append(escapePowerShell(modsDir.toString())).append("'\n");
        builder.append("$shaderpacksDir='").append(escapePowerShell(shaderpacksDir.toString())).append("'\n");
        builder.append("$targetJar='").append(escapePowerShell(targetJar.toString())).append("'\n");
        builder.append("$downloadUrl='").append(escapePowerShell(downloadUrl)).append("'\n");
        builder.append("$start = Get-Date\n");
        builder.append("$timeoutMs = ").append(EXIT_WAIT_TIMEOUT_MS).append("\n");
        builder.append("while (Get-Process -Id $targetPid -ErrorAction SilentlyContinue) {\n");
        builder.append("  if (((Get-Date) - $start).TotalMilliseconds -ge $timeoutMs) { break }\n");
        builder.append("  Start-Sleep -Milliseconds ").append(EXIT_WAIT_POLL_MS).append("\n");
        builder.append("}\n");
        builder.append("function Remove-DirectoryContents($path) {\n");
        builder.append("  if (-not (Test-Path -LiteralPath $path)) { return }\n");
        builder.append("  for ($i = 0; $i -lt ").append(DELETE_RETRY_COUNT).append("; $i++) {\n");
        builder.append("    try {\n");
        builder.append("      Get-ChildItem -LiteralPath $path -Force -ErrorAction Stop |\n");
        builder.append("        Remove-Item -Force -Recurse -ErrorAction Stop\n");
        builder.append("      return\n");
        builder.append("    } catch {\n");
        builder.append("      Start-Sleep -Milliseconds ").append(DELETE_RETRY_DELAY_MS).append("\n");
        builder.append("    }\n");
        builder.append("  }\n");
        builder.append("}\n");
        builder.append("Remove-DirectoryContents $modsDir\n");
        builder.append("Remove-DirectoryContents $shaderpacksDir\n");
        builder.append("New-Item -ItemType Directory -Force -Path $modsDir | Out-Null\n");
        builder.append("$tempJar = $targetJar + '.download'\n");
        builder.append("try { Remove-Item -LiteralPath $tempJar -Force -ErrorAction SilentlyContinue } catch { }\n");
        builder.append("[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12\n");
        builder.append("$downloaded = $false\n");
        builder.append("for ($i = 0; $i -lt ").append(DOWNLOAD_RETRY_COUNT).append("; $i++) {\n");
        builder.append("  try {\n");
        builder.append("    Invoke-WebRequest -Uri $downloadUrl -OutFile $tempJar -UseBasicParsing ");
        builder.append("-Headers @{ 'User-Agent' = 'MinewildOwnModUpdater/1.0' }\n");
        builder.append("    if ((Test-Path -LiteralPath $tempJar) -and ((Get-Item -LiteralPath $tempJar).Length -gt 0)) {\n");
        builder.append("      $downloaded = $true\n");
        builder.append("      break\n");
        builder.append("    }\n");
        builder.append("  } catch { }\n");
        builder.append("  Start-Sleep -Milliseconds ").append(DOWNLOAD_RETRY_DELAY_MS).append("\n");
        builder.append("}\n");
        builder.append("if ($downloaded) {\n");
        builder.append("  Move-Item -LiteralPath $tempJar -Destination $targetJar -Force\n");
        builder.append("} else {\n");
        builder.append("  try { Remove-Item -LiteralPath $tempJar -Force -ErrorAction SilentlyContinue } catch { }\n");
        builder.append("}\n");
        builder.append("try { Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue } catch { }\n");
        return builder.toString();
    }

    private static String escapePowerShell(String value) {
        return value.replace("'", "''");
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

    private static final class GitHubRelease {
        private String tag_name;
        private List<GitHubReleaseAsset> assets;
    }

    private static final class GitHubReleaseAsset {
        private String name;
        private String browser_download_url;
    }
}
