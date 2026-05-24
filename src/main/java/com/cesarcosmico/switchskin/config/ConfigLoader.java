package com.cesarcosmico.switchskin.config;

import com.cesarcosmico.switchskin.SwitchSkinPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class ConfigLoader {

    private static final String[] DEFAULT_LANGUAGES = {"en_EN", "es_ES"};

    private final SwitchSkinPlugin plugin;
    private FileConfiguration config;

    public ConfigLoader(SwitchSkinPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        saveLanguageFiles();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }

    public String getLanguage() {
        String lang = config.getString("language", "en_EN");
        File dir = new File(plugin.getDataFolder(), "translations");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No translation files found. Falling back to 'en_EN'.");
            return "en_EN";
        }

        Set<String> available = new HashSet<>();
        for (File file : files) {
            available.add(file.getName().replace(".yml", ""));
        }

        if (!available.contains(lang)) {
            plugin.getLogger().warning("Language '" + lang + "' not found. Falling back.");
            return available.contains("en_EN") ? "en_EN" : available.iterator().next();
        }
        return lang;
    }

    private void saveLanguageFiles() {
        for (String lang : DEFAULT_LANGUAGES) {
            String path = "translations/" + lang + ".yml";
            File file = new File(plugin.getDataFolder(), path);
            if (!file.exists()) {
                plugin.saveResource(path, false);
            }
        }
    }
}
