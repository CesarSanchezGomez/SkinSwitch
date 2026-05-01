package com.cesarcosmico.skinswitch.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class PluginConfig {

    public static final int CURRENT_VERSION = 2;

    public record TokenConfig(String material, String customName, List<String> lore) {}

    private final int defaultMaxSlots;
    private final TokenConfig token;
    private final TokenConfig tooltipToken;

    public PluginConfig(ConfigurationSection root, Logger logger) {
        ConfigurationSection defaults = root.getConfigurationSection("defaults");
        this.defaultMaxSlots = Math.max(1, defaults != null ? defaults.getInt("max-slots", 6) : 6);

        this.token = readToken(root.getConfigurationSection("token"), "NAME_TAG");
        this.tooltipToken = readToken(root.getConfigurationSection("tooltip-token"), "PAPER");
    }

    private TokenConfig readToken(ConfigurationSection section, String defaultMaterial) {
        if (section == null) return new TokenConfig(defaultMaterial, "", new ArrayList<>());
        return new TokenConfig(
                section.getString("material", defaultMaterial),
                section.getString("custom_name", ""),
                new ArrayList<>(section.getStringList("lore"))
        );
    }

    public int getDefaultMaxSlots() { return defaultMaxSlots; }
    public TokenConfig getToken() { return token; }
    public TokenConfig getTooltipToken() { return tooltipToken; }
}
