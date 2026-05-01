package com.cesarcosmico.skinswitch.config;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class SkinConfig {

    public static final int CURRENT_VERSION = 3;

    /** Default bracket colors for the slot row in lore (per-skin colors override 'active'). */
    public record SlotColors(String active, String inactive) {}

    private final Map<String, SkinDefinition> skins;
    private final SlotColors slotColors;

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

        ConfigurationSection colorsSection = root.getConfigurationSection("slot-colors");
        String active = colorsSection != null ? colorsSection.getString("active", "aqua") : "aqua";
        String inactive = colorsSection != null ? colorsSection.getString("inactive", "gray") : "gray";
        this.slotColors = new SlotColors(active, inactive);
    }

    private SkinDefinition parseSkin(String id, ConfigurationSection root, Logger logger) {
        String itemModelRaw;
        String display;
        String icon;
        String tooltipStyleRaw;
        String color;

        if (root.isConfigurationSection(id)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            itemModelRaw = section.getString("item_model", "");
            display = section.getString("display", null);
            icon = section.getString("icon", id);
            tooltipStyleRaw = section.getString("tooltip_style", null);
            color = section.getString("color", null);
        } else {
            itemModelRaw = root.getString(id, "");
            display = null;
            icon = id;
            tooltipStyleRaw = null;
            color = null;
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

        return new SkinDefinition(id, modelKey, display, icon, tooltipKey, color);
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

    public SlotColors getSlotColors() {
        return slotColors;
    }
}
