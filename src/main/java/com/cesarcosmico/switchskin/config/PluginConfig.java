package com.cesarcosmico.switchskin.config;

import com.cesarcosmico.switchskin.item.ItemFactory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

public final class PluginConfig {

    public static final int CURRENT_VERSION = 1;

    public record SoundConfig(boolean enabled, String key, float volume, float pitch) {
        public boolean playable() { return enabled && key != null && !key.isEmpty(); }
    }

    public record FeedbackConfig(String mode) {
        public boolean isActionBar() { return "actionbar".equalsIgnoreCase(mode); }
    }

    private final int defaultMaxSlots;
    private final ItemConfig token;
    private final ItemConfig tooltipToken;
    private final SoundConfig switchSound;
    private final FeedbackConfig switchFeedback;
    private final MenuConfig menu;

    public PluginConfig(ConfigurationSection root, ItemFactory itemFactory, Logger logger) {
        final ConfigurationSection defaults = root.getConfigurationSection("defaults");
        this.defaultMaxSlots = Math.max(1, defaults != null ? defaults.getInt("max-slots", 6) : 6);

        this.token = itemFactory.parse(root.getConfigurationSection("token"), "NAME_TAG");
        this.tooltipToken = itemFactory.parse(root.getConfigurationSection("tooltip-token"), "PAPER");

        final ConfigurationSection switchSection = root.getConfigurationSection("switch");
        this.switchSound = readSound(switchSection != null ? switchSection.getConfigurationSection("sound") : null);
        this.switchFeedback = new FeedbackConfig(
                switchSection != null ? switchSection.getString("feedback", "actionbar") : "actionbar");

        this.menu = new MenuConfig(root.getConfigurationSection("menu"), itemFactory, logger);
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

    public int getDefaultMaxSlots() { return defaultMaxSlots; }
    public ItemConfig getToken() { return token; }
    public ItemConfig getTooltipToken() { return tooltipToken; }
    public SoundConfig getSwitchSound() { return switchSound; }
    public FeedbackConfig getSwitchFeedback() { return switchFeedback; }
    public MenuConfig getMenu() { return menu; }
}
