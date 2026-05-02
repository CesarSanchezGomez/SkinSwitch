package com.cesarcosmico.switchskin.config;

import com.cesarcosmico.switchskin.item.component.CustomModelDataApplier;
import com.cesarcosmico.switchskin.item.component.TooltipDisplayApplier;
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

    public SkinConfig(ConfigurationSection root, Logger logger) {
        final Map<String, SkinDefinition> map = new LinkedHashMap<>();
        final ConfigurationSection skinsSection = root.getConfigurationSection("skins");
        if (skinsSection != null) {
            for (String id : skinsSection.getKeys(false)) {
                final SkinDefinition def = parseSkin(id, skinsSection, logger);
                if (def != null) {
                    map.put(id, def);
                }
            }
        }
        this.skins = Collections.unmodifiableMap(map);
        this.defaultBracketColor = root.getString("default-bracket-color", "gray");
    }

    private SkinDefinition parseSkin(String id, ConfigurationSection root, Logger logger) {
        final String itemModelRaw;
        final String name;
        final List<String> lore;
        final String icon;
        final String iconActive;
        final String iconInactive;
        final String bracketColor;
        final String bracketColorDefault;
        final String tooltipStyleRaw;
        final CustomModelDataConfig customModelData;
        final TooltipDisplayConfig tooltipDisplay;

        if (root.isConfigurationSection(id)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            itemModelRaw = section.getString("item_model", "");
            name = section.getString("name", null);
            lore = section.isList("lore") ? section.getStringList("lore") : List.of();
            icon = section.getString("icon", id);
            iconActive = section.getString("icon-active", null);
            iconInactive = section.getString("icon-inactive", null);
            bracketColor = section.getString("bracket-color", null);
            bracketColorDefault = section.getString("bracket-color-default", null);
            tooltipStyleRaw = section.getString("tooltip_style", null);
            customModelData = parseCustomModelData(section.getConfigurationSection("custom_model_data"));
            tooltipDisplay = parseTooltipDisplay(id, section.getConfigurationSection("tooltip_display"), logger);
        } else {
            itemModelRaw = root.getString(id, "");
            name = null;
            lore = List.of();
            icon = id;
            iconActive = null;
            iconInactive = null;
            bracketColor = null;
            bracketColorDefault = null;
            tooltipStyleRaw = null;
            customModelData = null;
            tooltipDisplay = null;
        }

        if (itemModelRaw.isEmpty()) {
            logger.warning("Skin '" + id + "' is missing an item_model value.");
            return null;
        }

        final NamespacedKey modelKey = NamespacedKey.fromString(itemModelRaw);
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

        return new SkinDefinition(id, modelKey, name, lore, icon, iconActive, iconInactive,
                bracketColor, bracketColorDefault, tooltipKey,
                customModelData, tooltipDisplay);
    }

    private CustomModelDataConfig parseCustomModelData(ConfigurationSection section) {
        if (section == null) return null;
        final CustomModelDataConfig parsed = CustomModelDataApplier.parse(section);
        return parsed.isEmpty() ? null : parsed;
    }

    private TooltipDisplayConfig parseTooltipDisplay(String skinId, ConfigurationSection section, Logger logger) {
        if (section == null) return null;
        final TooltipDisplayConfig parsed = TooltipDisplayApplier.parse(section);
        for (String componentId : parsed.hiddenComponents()) {
            if (!TooltipDisplayApplier.knowsComponent(componentId)) {
                logger.warning("Skin '" + skinId + "' references unknown component in tooltip_display.hidden_components: "
                        + componentId);
            }
        }
        return parsed;
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
}
