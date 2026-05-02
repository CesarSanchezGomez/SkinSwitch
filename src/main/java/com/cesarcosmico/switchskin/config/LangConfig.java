package com.cesarcosmico.switchskin.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LangConfig {

    public static final int CURRENT_VERSION = 2;

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String DEFAULT_PREFIX =
            "<white>[<gradient:#B4E488:#7DD031>SwitchSkin</gradient>]</white>";

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

        final String lang = plugin.getConfig().getString("lang", "en_US");
        final File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        saveDefaultLang("en_US");
        saveDefaultLang("es_ES");

        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + lang + ".yml' not found. Falling back to 'en_US'.");
            langFile = new File(langFolder, "en_US.yml");
        }

        final YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
        ConfigVersionChecker.check(config, "lang/" + lang + ".yml",
                CURRENT_VERSION, plugin, plugin.getLogger());

        YamlConfiguration defaults = null;
        final InputStream defaultStream = plugin.getResource("lang/en_US.yml");
        if (defaultStream != null) {
            defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }

        this.prefix = config.getString("prefix", DEFAULT_PREFIX);
        absorb(config);
        if (defaults != null) backfillFromDefaults(defaults);
    }

    private void absorb(YamlConfiguration source) {
        for (String key : source.getKeys(true)) {
            if (source.isConfigurationSection(key)) continue;
            if (source.isList(key)) {
                final List<String> items = source.getStringList(key);
                lists.put(key, List.copyOf(items));
                messages.put(key, String.join("\n", items));
            } else {
                messages.put(key, source.getString(key, key));
            }
        }
    }

    private void backfillFromDefaults(YamlConfiguration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) continue;
            if (messages.containsKey(key)) continue;
            if (defaults.isList(key)) {
                final List<String> items = defaults.getStringList(key);
                lists.put(key, List.copyOf(items));
                messages.put(key, String.join("\n", items));
            } else {
                final String value = defaults.getString(key);
                if (value != null) messages.put(key, value);
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

    public void sendActionBar(Player player, String key, String... placeholders) {
        player.sendActionBar(get(key, placeholders));
    }

    private void saveDefaultLang(String lang) {
        final File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }
    }
}
