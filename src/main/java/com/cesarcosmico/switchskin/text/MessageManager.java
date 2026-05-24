package com.cesarcosmico.switchskin.text;

import com.cesarcosmico.switchskin.SwitchSkinPlugin;
import com.cesarcosmico.switchskin.config.ConfigLoader;
import com.cesarcosmico.switchskin.config.ConfigVersionChecker;
import com.cesarcosmico.switchskin.util.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MessageManager {

    public static final int CURRENT_VERSION = 1;

    private final SwitchSkinPlugin plugin;
    private final ConfigLoader configLoader;
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, List<String>> listMessages = new HashMap<>();

    public MessageManager(SwitchSkinPlugin plugin, ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
    }

    public void reload() {
        loadMessages(configLoader.getLanguage());
    }

    private void loadMessages(String language) {
        String resourceName = "translations/" + language + ".yml";
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigVersionChecker.check(config, resourceName, CURRENT_VERSION, plugin, plugin.getLogger());

        messages.clear();
        listMessages.clear();
        for (String key : config.getKeys(true)) {
            if (key.equals("config-version")) continue;
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            } else if (config.isList(key)) {
                listMessages.put(key, config.getStringList(key));
            }
        }
    }

    public Component getMessage(String key) {
        return MessageUtils.parse(rawOrMissing(key));
    }

    public Component getMessage(String key, TagResolver resolver) {
        return MessageUtils.parse(rawOrMissing(key), resolver);
    }

    public Component getPrefixedMessage(String key) {
        return MessageUtils.parse(prefix() + " " + rawOrMissing(key));
    }

    public Component getPrefixedMessage(String key, TagResolver resolver) {
        return MessageUtils.parse(prefix() + " " + rawOrMissing(key), resolver);
    }

    public String getRaw(String key) {
        return messages.get(key);
    }

    public List<String> getRawList(String key) {
        List<String> list = listMessages.get(key);
        if (list != null) return list;
        String single = messages.get(key);
        return single != null ? List.of(single) : List.of();
    }

    private String prefix() {
        return messages.getOrDefault("prefix", "<gray>[SwitchSkin]");
    }

    private String rawOrMissing(String key) {
        return messages.getOrDefault(key, "<red>Missing translation: " + key);
    }
}
