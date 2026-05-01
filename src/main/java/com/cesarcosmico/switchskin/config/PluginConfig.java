package com.cesarcosmico.switchskin.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class PluginConfig {

    public static final int CURRENT_VERSION = 1;

    public record TokenConfig(String material, String customName, List<String> lore) {}

    public record Features(boolean switchName, boolean switchLore) {}

    public record SoundConfig(boolean enabled, String key, float volume, float pitch) {
        public boolean playable() { return enabled && key != null && !key.isEmpty(); }
    }

    public record CooldownConfig(boolean enabled, long millis) {
        public boolean active() { return enabled && millis > 0; }
    }

    public record FeedbackConfig(String mode) {
        public boolean isActionBar() { return "actionbar".equalsIgnoreCase(mode); }
    }

    public record MenuConfig(int rows, String title) {}

    private final int defaultMaxSlots;
    private final TokenConfig token;
    private final TokenConfig tooltipToken;
    private final Features features;
    private final SoundConfig switchSound;
    private final CooldownConfig switchCooldown;
    private final FeedbackConfig switchFeedback;
    private final MenuConfig menu;

    public PluginConfig(ConfigurationSection root, Logger logger) {
        final ConfigurationSection defaults = root.getConfigurationSection("defaults");
        this.defaultMaxSlots = Math.max(1, defaults != null ? defaults.getInt("max-slots", 6) : 6);

        this.token = readToken(root.getConfigurationSection("token"), "NAME_TAG");
        this.tooltipToken = readToken(root.getConfigurationSection("tooltip-token"), "PAPER");

        final ConfigurationSection featuresSection = root.getConfigurationSection("features");
        final boolean switchName = featuresSection == null || featuresSection.getBoolean("switch-name", true);
        final boolean switchLore = featuresSection == null || featuresSection.getBoolean("switch-lore", true);
        this.features = new Features(switchName, switchLore);

        final ConfigurationSection switchSection = root.getConfigurationSection("switch");
        this.switchSound = readSound(switchSection != null ? switchSection.getConfigurationSection("sound") : null);
        this.switchCooldown = readCooldown(switchSection != null ? switchSection.getConfigurationSection("cooldown") : null);
        this.switchFeedback = new FeedbackConfig(
                switchSection != null ? switchSection.getString("feedback", "actionbar") : "actionbar");

        final ConfigurationSection menuSection = root.getConfigurationSection("menu");
        final int rows = Math.clamp(menuSection != null ? menuSection.getInt("rows", 3) : 3, 1, 6);
        final String title = menuSection != null
                ? menuSection.getString("title", "<white><gradient:#B4E488:#7DD031><b>Switch Skin</b></gradient></white>")
                : "<white><gradient:#B4E488:#7DD031><b>Switch Skin</b></gradient></white>";
        this.menu = new MenuConfig(rows, title);
    }

    private TokenConfig readToken(ConfigurationSection section, String defaultMaterial) {
        if (section == null) return new TokenConfig(defaultMaterial, "", new ArrayList<>());
        return new TokenConfig(
                section.getString("material", defaultMaterial),
                section.getString("custom_name", ""),
                new ArrayList<>(section.getStringList("lore"))
        );
    }

    private SoundConfig readSound(ConfigurationSection section) {
        if (section == null) return new SoundConfig(false, "", 1.0f, 1.0f);
        return new SoundConfig(
                section.getBoolean("enabled", true),
                section.getString("key", "minecraft:ui.button.click"),
                (float) section.getDouble("volume", 1.0),
                (float) section.getDouble("pitch", 1.0)
        );
    }

    private CooldownConfig readCooldown(ConfigurationSection section) {
        if (section == null) return new CooldownConfig(true, 500L);
        return new CooldownConfig(
                section.getBoolean("enabled", true),
                Math.max(0, section.getLong("millis", 500L))
        );
    }

    public int getDefaultMaxSlots() { return defaultMaxSlots; }
    public TokenConfig getToken() { return token; }
    public TokenConfig getTooltipToken() { return tooltipToken; }
    public Features getFeatures() { return features; }
    public SoundConfig getSwitchSound() { return switchSound; }
    public CooldownConfig getSwitchCooldown() { return switchCooldown; }
    public FeedbackConfig getSwitchFeedback() { return switchFeedback; }
    public MenuConfig getMenu() { return menu; }
}
