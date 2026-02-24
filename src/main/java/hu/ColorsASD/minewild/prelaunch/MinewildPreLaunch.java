package hu.ColorsASD.minewild.prelaunch;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MinewildPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("MinewildPreLaunch");
    private static final String DH_CONFIG_FILE = "DistantHorizons.toml";
    private static final String DH_AUTO_UPDATER_TABLE = "[client.advanced.autoUpdater]";

    @Override
    public void onPreLaunch() {
        disableDhUpdaterBeforeModsInit();
    }

    private static void disableDhUpdaterBeforeModsInit() {
        Path configPath = FabricLoader.getInstance().getGameDir().resolve("config").resolve(DH_CONFIG_FILE);
        if (Files.notExists(configPath)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            int sectionStart = -1;
            int sectionEnd = lines.size();
            for (int i = 0; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                    continue;
                }
                if (sectionStart >= 0) {
                    sectionEnd = i;
                    break;
                }
                if (DH_AUTO_UPDATER_TABLE.equals(trimmed)) {
                    sectionStart = i;
                }
            }

            boolean changed = false;
            if (sectionStart < 0) {
                if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) {
                    lines.add("");
                }
                lines.add(DH_AUTO_UPDATER_TABLE);
                lines.add("\tenableAutoUpdater = false");
                lines.add("\tenableSilentUpdates = false");
                changed = true;
            } else {
                boolean foundAutoUpdater = false;
                boolean foundSilentUpdates = false;
                for (int i = sectionStart + 1; i < sectionEnd; i++) {
                    String line = lines.get(i);
                    String trimmed = line.trim();
                    if (trimmed.startsWith("enableAutoUpdater")) {
                        foundAutoUpdater = true;
                        String replacement = leadingWhitespace(line) + "enableAutoUpdater = false";
                        if (!line.equals(replacement)) {
                            lines.set(i, replacement);
                            changed = true;
                        }
                        continue;
                    }
                    if (trimmed.startsWith("enableSilentUpdates")) {
                        foundSilentUpdates = true;
                        String replacement = leadingWhitespace(line) + "enableSilentUpdates = false";
                        if (!line.equals(replacement)) {
                            lines.set(i, replacement);
                            changed = true;
                        }
                    }
                }
                int insertAt = sectionEnd;
                if (!foundAutoUpdater) {
                    lines.add(insertAt++, "\tenableAutoUpdater = false");
                    changed = true;
                }
                if (!foundSilentUpdates) {
                    lines.add(insertAt, "\tenableSilentUpdates = false");
                    changed = true;
                }
            }

            if (changed) {
                Files.write(configPath, lines, StandardCharsets.UTF_8);
                LOGGER.info("PreLaunch: Distant Horizons updater letiltva: {}", configPath);
            }
        } catch (IOException e) {
            LOGGER.warn("PreLaunch: Nem sikerült frissíteni a DH configot: {}", configPath, e);
        }
    }

    private static String leadingWhitespace(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        int index = 0;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        return line.substring(0, index);
    }
}
