package com.cesarcosmico.switchskin.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public final class ConfigVersionChecker {

    private ConfigVersionChecker() {}

    public static void check(FileConfiguration live, String resourceName,
                             int expectedVersion, JavaPlugin plugin, Logger logger) {
        check(live, resourceName, expectedVersion, plugin, logger, Set.of());
    }

    public static void check(FileConfiguration live, String resourceName,
                             int expectedVersion, JavaPlugin plugin, Logger logger,
                             Set<String> dynamicSections) {
        int current = live.getInt("config-version", 0);
        boolean outdated = current < expectedVersion;

        InputStream stream = plugin.getResource(resourceName);
        if (stream == null) return;

        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));

        List<String> missing = new ArrayList<>();
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) continue;
            if (isUnderDynamicSection(key, dynamicSections)) continue;
            if (!live.isSet(key)) missing.add(key);
        }

        Set<String> defaultKeys = defaults.getKeys(true);
        Set<String> unknownAll = new HashSet<>();
        for (String key : live.getKeys(true)) {
            if (key.equals("config-version")) continue;
            if (isUnderDynamicSection(key, dynamicSections)) continue;
            if (!defaultKeys.contains(key)) unknownAll.add(key);
        }
        List<String> unknown = new ArrayList<>();
        for (String key : unknownAll) {
            if (!hasOrphanAncestor(key, unknownAll)) unknown.add(key);
        }

        if (!outdated && missing.isEmpty() && unknown.isEmpty()) return;

        if (outdated) {
            logger.warning(resourceName + " is outdated (version " + current
                    + ", expected " + expectedVersion + ").");
        }
        if (!missing.isEmpty()) {
            logger.warning(resourceName + " is missing " + missing.size() + " key(s):");
            for (String key : missing) {
                logger.warning("  - " + key);
            }
            logger.warning("Add them manually or regenerate the file.");
        }
        if (!unknown.isEmpty()) {
            logger.warning(resourceName + " has " + unknown.size()
                    + " unknown key(s) (typo or removed?):");
            for (String key : unknown) {
                logger.warning("  - " + key);
            }
        }
    }

    private static boolean isUnderDynamicSection(String key, Set<String> dynamicSections) {
        for (String section : dynamicSections) {
            if (key.startsWith(section + ".")) return true;
        }
        return false;
    }

    private static boolean hasOrphanAncestor(String key, Set<String> orphans) {
        String parent = key;
        while (parent.contains(".")) {
            parent = parent.substring(0, parent.lastIndexOf('.'));
            if (orphans.contains(parent)) return true;
        }
        return false;
    }
}
