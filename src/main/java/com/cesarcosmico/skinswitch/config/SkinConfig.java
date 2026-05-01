package com.cesarcosmico.skinswitch.config;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class SkinConfig {

    public static final int CURRENT_VERSION = 1;

    private final Map<String, SkinDefinition> skins;
    private final String defaultBracketColor;
    private final String inactiveIconColor;

    public SkinConfig(ConfigurationSection root, Logger logger) {
        Map<String, SkinDefinition> map = new LinkedHashMap<>();
        ConfigurationSection skinsSection = root.getConfigurationSection("skins");
        if (skinsSection != null) {
            for (String id : skinsSection.getKeys(false)) {
                SkinDefinition def = parseSkin(id, skinsSection, logger);
                if (def != null) {
                    map.put(id, def);
                }
            }
        }
        this.skins = Collections.unmodifiableMap(map);
        this.defaultBracketColor = root.getString("default-bracket-color", "gray");
        this.inactiveIconColor = root.getString("inactive-icon-color", "white");
    }

    private SkinDefinition parseSkin(String id, ConfigurationSection root, Logger logger) {
        String itemModelRaw;
        String name;
        List<String> lore;
        String icon;
        String iconActive;
        String iconInactive;
        String color;
        String tooltipStyleRaw;

        if (root.isConfigurationSection(id)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            itemModelRaw = section.getString("item_model", "");
            name = section.getString("name", null);
            lore = section.isList("lore") ? section.getStringList("lore") : List.of();
            icon = section.getString("icon", id);
            iconActive = section.getString("icon-active", null);
            iconInactive = section.getString("icon-inactive", null);
            color = section.getString("color", null);
            tooltipStyleRaw = section.getString("tooltip_style", null);
        } else {
            itemModelRaw = root.getString(id, "");
            name = null;
            lore = List.of();
            icon = id;
            iconActive = null;
            iconInactive = null;
            color = null;
            tooltipStyleRaw = null;
        }

        if (itemModelRaw.isEmpty()) {
            logger.warning("Skin '" + id + "' is missing an item_model value.");
            return null;
        }

        NamespacedKey modelKey = NamespacedKey.fromString(itemModelRaw);
        if (modelKey == null) {
            logger.warning("Skin '" + id + "' has an invalid item_model: " + itemModelRaw);
            return null;
        }

        NamespacedKey tooltipKey = null;
        if (tooltipStyleRaw != null && !tooltipStyleRaw.isEmpty()) {
            tooltipKey = NamespacedKey.fromString(tooltipStyleRaw);
            if (tooltipKey == null) {
                logger.warning("Skin '" + id + "' has an invalid tooltip_style: " + tooltipStyleRaw);
            }
        }

        return new SkinDefinition(id, modelKey, name, lore, icon, iconActive, iconInactive, color, tooltipKey);
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

    public String getDefaultBracketColor() {
        return defaultBracketColor;
    }

    public String getInactiveIconColor() {
        return inactiveIconColor;
    }
}
