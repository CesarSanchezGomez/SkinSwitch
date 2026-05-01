package com.cesarcosmico.skinswitch.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class PluginConfig {

    public static final int CURRENT_VERSION = 1;

    public record TokenConfig(String material, String customName, List<String> lore) {}

    private final int defaultMaxSlots;
    private final TokenConfig token;

    public PluginConfig(ConfigurationSection root, Logger logger) {
        ConfigurationSection defaults = root.getConfigurationSection("defaults");
        this.defaultMaxSlots = Math.max(1, defaults != null ? defaults.getInt("max-slots", 6) : 6);

        ConfigurationSection tokenSection = root.getConfigurationSection("token");
        if (tokenSection == null) {
            this.token = new TokenConfig("NAME_TAG", "", new ArrayList<>());
        } else {
            this.token = new TokenConfig(
                    tokenSection.getString("material", "NAME_TAG"),
                    tokenSection.getString("custom_name", ""),
                    new ArrayList<>(tokenSection.getStringList("lore"))
            );
        }
    }

    public int getDefaultMaxSlots() { return defaultMaxSlots; }
    public TokenConfig getToken() { return token; }
}
