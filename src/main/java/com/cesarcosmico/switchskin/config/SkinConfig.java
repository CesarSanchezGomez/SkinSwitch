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
    private final String defaultBracketColorActive;
    private final String defaultBracketColorInactive;
    private final String defaultIconActive;
    private final String defaultIconInactive;

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
        this.defaultBracketColorActive = root.getString("default-bracket-color-active", "gray");
        this.defaultBracketColorInactive = root.getString("default-bracket-color-inactive", "dark_gray");
        this.defaultIconActive = root.getString("default-icon-active", "");
        this.defaultIconInactive = root.getString("default-icon-inactive", "");
    }

    private SkinDefinition parseSkin(String id, ConfigurationSection root, Logger logger) {
        final String itemModelRaw;
        final String name;
        final List<String> lore;
        final String iconActive;
        final String iconInactive;
        final String bracketColorActive;
        final String bracketColorInactive;
        final String tooltipStyleRaw;
        final CustomModelDataConfig customModelData;
        final TooltipDisplayConfig tooltipDisplay;

        if (root.isConfigurationSection(id)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            itemModelRaw = section.getString("item_model", "");
            name = section.getString("name", null);
            lore = section.isList("lore") ? section.getStringList("lore") : List.of();
            iconActive = section.getString("icon-active", null);
            iconInactive = section.getString("icon-inactive", null);
            bracketColorActive = section.getString("bracket-color-active", null);
            bracketColorInactive = section.getString("bracket-color-inactive", null);
            tooltipStyleRaw = section.getString("tooltip_style", null);
            customModelData = parseCustomModelData(section.getConfigurationSection("custom_model_data"));
            tooltipDisplay = parseTooltipDisplay(id, section.getConfigurationSection("tooltip_display"), logger);
        } else {
            itemModelRaw = root.getString(id, "");
            name = null;
            lore = List.of();
            iconActive = null;
            iconInactive = null;
            bracketColorActive = null;
            bracketColorInactive = null;
            tooltipStyleRaw = null;
            customModelData = null;
            tooltipDisplay = null;
        }

        NamespacedKey modelKey = null;
        if (!itemModelRaw.isEmpty()) {
            modelKey = NamespacedKey.fromString(itemModelRaw);
            if (modelKey == null) {
                logger.warning("Skin '" + id + "' has an invalid item_model: " + itemModelRaw);
            }
        }

        NamespacedKey tooltipKey = null;
        if (tooltipStyleRaw != null && !tooltipStyleRaw.isEmpty()) {
            tooltipKey = NamespacedKey.fromString(tooltipStyleRaw);
            if (tooltipKey == null) {
                logger.warning("Skin '" + id + "' has an invalid tooltip_style: " + tooltipStyleRaw);
            }
        }

        return new SkinDefinition(id, modelKey, name, lore, iconActive, iconInactive,
                bracketColorActive, bracketColorInactive, tooltipKey,
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

    public String getDefaultBracketColorActive() { return defaultBracketColorActive; }
    public String getDefaultBracketColorInactive() { return defaultBracketColorInactive; }
    public String getDefaultIconActive() { return defaultIconActive; }
    public String getDefaultIconInactive() { return defaultIconInactive; }
}
