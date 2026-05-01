package com.cesarcosmico.skinswitch.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LangConfig {

    public static final int CURRENT_VERSION = 7;

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final Map<String, String> messages;
    private final Map<String, List<String>> lists;
    private String prefix;

    public LangConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.lists = new HashMap<>();
        load();
    }

    public void load() {
        messages.clear();
        lists.clear();

        String lang = plugin.getConfig().getString("lang", "en_US");
        File langFolder = new File(plugin.getDataFolder(), "lang");

        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        saveDefaultLang("en_US");
        saveDefaultLang("es_ES");

        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + lang + ".yml' not found. Falling back to 'en_US'.");
            langFile = new File(langFolder, "en_US.yml");
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);

        ConfigVersionChecker.check(config, "lang/" + lang + ".yml",
                CURRENT_VERSION, plugin, plugin.getLogger());

        YamlConfiguration defaults = null;
        InputStream defaultStream = plugin.getResource("lang/en_US.yml");
        if (defaultStream != null) {
            defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }

        this.prefix = config.getString("prefix",
                "<white>[<gradient:#C173FF:#950DFF>SkinSwitch</gradient>]</white>");

        for (String key : config.getKeys(true)) {
            if (config.isConfigurationSection(key)) continue;
            if (config.isList(key)) {
                List<String> items = config.getStringList(key);
                lists.put(key, List.copyOf(items));
                messages.put(key, String.join("\n", items));
            } else {
                messages.put(key, config.getString(key, key));
            }
        }

        // Backfill keys present in bundled defaults but missing from the user's
        // file — keeps placeholders working when an outdated lang file is in use.
        if (defaults != null) {
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key)) continue;
                if (messages.containsKey(key)) continue;
                if (defaults.isList(key)) {
                    List<String> items = defaults.getStringList(key);
                    lists.put(key, List.copyOf(items));
                    messages.put(key, String.join("\n", items));
                } else {
                    String value = defaults.getString(key);
                    if (value != null) messages.put(key, value);
                }
            }
        }
    }

    public String getRaw(String key) {
        return messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
    }

    public List<String> getRawList(String key) {
        return lists.getOrDefault(key, List.of());
    }

    public Component get(String key, String... placeholders) {
        String raw = getRaw(key).replace("{prefix}", prefix);

        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace(placeholders[i], placeholders[i + 1]);
        }

        return MINI.deserialize(raw);
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    private void saveDefaultLang(String lang) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }
    }
}
