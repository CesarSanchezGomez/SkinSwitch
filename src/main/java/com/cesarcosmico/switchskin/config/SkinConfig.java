package com.cesarcosmico.switchskin.config;

import com.cesarcosmico.switchskin.config.loader.SkinConfigLoader;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class SkinConfig {

    private static final String DEFAULT_BRACKET_COLOR_ACTIVE = "<gray>";
    private static final String DEFAULT_BRACKET_COLOR_INACTIVE = "<dark_gray>";

    private final Logger logger;
    private final SkinConfigLoader loader;

    private Map<String, SkinDefinition> skins = Map.of();
    private String defaultBracketColorActive = stripWrap(DEFAULT_BRACKET_COLOR_ACTIVE);
    private String defaultBracketColorInactive = stripWrap(DEFAULT_BRACKET_COLOR_INACTIVE);
    private String defaultIconActive = "";
    private String defaultIconInactive = "";

    public SkinConfig(ConfigurationSection root, Logger logger) {
        this.logger = logger;
        this.loader = new SkinConfigLoader(logger);
        load(root);
    }

    /** Reloads all skin definitions and defaults in place so held references stay valid. */
    public void load(ConfigurationSection root) {
        this.skins = Map.copyOf(loader.load(root.getConfigurationSection("skins")));
        this.defaultBracketColorActive = parseGlobalColor(root, "default-bracket-color-active",
                DEFAULT_BRACKET_COLOR_ACTIVE, logger);
        this.defaultBracketColorInactive = parseGlobalColor(root, "default-bracket-color-inactive",
                DEFAULT_BRACKET_COLOR_INACTIVE, logger);
        this.defaultIconActive = root.getString("default-icon-active", "");
        this.defaultIconInactive = root.getString("default-icon-inactive", "");
    }

    private static String parseGlobalColor(ConfigurationSection root, String key, String fallback, Logger logger) {
        final String raw = root.getString(key, fallback);
        if (isValidColorTag(raw)) return stripWrap(raw);
        logger.warning(key + " must be a MiniMessage color tag like '<gray>' or '<#FCBDE3>', got '"
                + raw + "'. Using bundled default '" + fallback + "'.");
        return stripWrap(fallback);
    }

    private static boolean isValidColorTag(String raw) {
        return raw != null && raw.length() >= 2
                && raw.charAt(0) == '<' && raw.charAt(raw.length() - 1) == '>';
    }

    private static String stripWrap(String wrapped) {
        return wrapped.substring(1, wrapped.length() - 1);
    }

    public Optional<SkinDefinition> get(String id) {
        return Optional.ofNullable(skins.get(id));
    }

    public boolean exists(String id) {
        return skins.containsKey(id);
    }

    public Map<String, SkinDefinition> all() {
        return skins;
    }

    public String getDefaultBracketColorActive() { return defaultBracketColorActive; }
    public String getDefaultBracketColorInactive() { return defaultBracketColorInactive; }
    public String getDefaultIconActive() { return defaultIconActive; }
    public String getDefaultIconInactive() { return defaultIconInactive; }
}
