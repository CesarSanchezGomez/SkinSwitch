package com.cesarcosmico.skinswitch.config;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class SkinConfig {

    public static final int CURRENT_VERSION = 1;

    private final Map<String, SkinDefinition> skins;

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
    }

    private SkinDefinition parseSkin(String id, ConfigurationSection root, Logger logger) {
        String itemModelRaw;
        String display;

        if (root.isConfigurationSection(id)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            itemModelRaw = section.getString("item_model", "");
            display = section.getString("display", id);
        } else {
            itemModelRaw = root.getString(id, "");
            display = id;
        }

        if (itemModelRaw.isEmpty()) {
            logger.warning("Skin '" + id + "' is missing an item_model value.");
            return null;
        }

        NamespacedKey key = NamespacedKey.fromString(itemModelRaw);
        if (key == null) {
            logger.warning("Skin '" + id + "' has an invalid item_model: " + itemModelRaw);
            return null;
        }

        return new SkinDefinition(id, key, display);
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
}
