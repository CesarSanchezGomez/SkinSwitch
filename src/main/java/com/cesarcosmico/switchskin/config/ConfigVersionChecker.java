package com.cesarcosmico.switchskin.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class ConfigVersionChecker {

    private ConfigVersionChecker() {}

    public static void check(FileConfiguration live, String resourceName,
                             int expectedVersion, JavaPlugin plugin, Logger logger,
                             String... ignoredKeyPrefixes) {
        final int current = live.getInt("config-version", 0);
        if (current >= expectedVersion) return;

        logger.warning(resourceName + " is outdated (version " + current
                + ", expected " + expectedVersion + ").");

        final InputStream stream = plugin.getResource(resourceName);
        if (stream == null) return;

        final YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));

        final List<String> missing = new ArrayList<>();
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) continue;
            if (isIgnored(key, ignoredKeyPrefixes)) continue;
            if (!live.isSet(key)) missing.add(key);
        }
        if (missing.isEmpty()) return;

        logger.warning(resourceName + " is missing " + missing.size() + " key(s) introduced by the new version:");
        for (String key : missing) {
            logger.warning("  - " + key);
        }
        logger.warning("Add them manually or regenerate the file.");
    }

    private static boolean isIgnored(String key, String[] prefixes) {
        for (String prefix : prefixes) {
            if (key.equals(prefix) || key.startsWith(prefix + ".")) return true;
        }
        return false;
    }
}
