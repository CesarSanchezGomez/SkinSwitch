package com.cesarcosmico.switchskin.config;

import com.cesarcosmico.switchskin.item.component.CustomModelDataApplier;
import com.cesarcosmico.switchskin.item.component.TooltipDisplayApplier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class SkinConfig {

    public static final int CURRENT_VERSION = 1;

    private static final String DEFAULT_BRACKET_COLOR_ACTIVE = "<gray>";
    private static final String DEFAULT_BRACKET_COLOR_INACTIVE = "<dark_gray>";

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
        this.defaultBracketColorActive = parseGlobalColor(root, "default-bracket-color-active",
                DEFAULT_BRACKET_COLOR_ACTIVE, logger);
        this.defaultBracketColorInactive = parseGlobalColor(root, "default-bracket-color-inactive",
                DEFAULT_BRACKET_COLOR_INACTIVE, logger);
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
        final TokenVisualConfig tokenSkin;
        final TokenVisualConfig tokenTooltip;
        final Set<Material> compatibleMaterials;

        if (root.isConfigurationSection(id)) {
            final ConfigurationSection section = root.getConfigurationSection(id);
            itemModelRaw = section.getString("item_model", "");
            name = section.getString("name", null);
            lore = section.isList("lore") ? section.getStringList("lore") : List.of();
            iconActive = section.getString("icon-active", null);
            iconInactive = section.getString("icon-inactive", null);
            bracketColorActive = parsePerSkinColor(section, "bracket-color-active", id, logger);
            bracketColorInactive = parsePerSkinColor(section, "bracket-color-inactive", id, logger);
            tooltipStyleRaw = section.getString("tooltip_style", null);
            customModelData = parseCustomModelData(section.getConfigurationSection("custom_model_data"));
            tooltipDisplay = parseTooltipDisplay(id, section.getConfigurationSection("tooltip_display"), logger);
            tokenSkin = parseTokenVisual(id, "token-skin", section.getConfigurationSection("token-skin"), logger);
            tokenTooltip = parseTokenVisual(id, "token-tooltip", section.getConfigurationSection("token-tooltip"), logger);
            compatibleMaterials = parseAppliesTo(id, section.getStringList("applies-to"), logger);
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
            tokenSkin = null;
            tokenTooltip = null;
            compatibleMaterials = Set.of();
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
                customModelData, tooltipDisplay, tokenSkin, tokenTooltip,
                compatibleMaterials);
    }

    private Set<Material> parseAppliesTo(String skinId, List<String> entries, Logger logger) {
        if (entries == null || entries.isEmpty()) return Set.of();
        final EnumSet<Material> resolved = EnumSet.noneOf(Material.class);
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) continue;
            final String trimmed = entry.trim();
            if (trimmed.startsWith("#")) {
                resolveTag(skinId, trimmed.substring(1), resolved, logger);
            } else {
                final Material material = Material.matchMaterial(trimmed);
                if (material == null) {
                    logger.warning("Skin '" + skinId + "' has an unknown material in applies-to: " + trimmed);
                    continue;
                }
                resolved.add(material);
            }
        }
        return resolved.isEmpty() ? Set.of() : resolved;
    }

    private void resolveTag(String skinId, String tagId, Set<Material> sink, Logger logger) {
        final NamespacedKey key = NamespacedKey.fromString(tagId);
        if (key == null) {
            logger.warning("Skin '" + skinId + "' has an invalid tag in applies-to: #" + tagId);
            return;
        }
        final Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
        if (tag == null) {
            logger.warning("Skin '" + skinId + "' references an unknown item tag in applies-to: #" + tagId);
            return;
        }
        sink.addAll(tag.getValues());
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

    private TokenVisualConfig parseTokenVisual(String skinId, String fieldName,
                                               ConfigurationSection section, Logger logger) {
        if (section == null) return null;
        NamespacedKey itemModel = null;
        final String itemModelRaw = section.getString("item_model", "");
        if (!itemModelRaw.isEmpty()) {
            itemModel = NamespacedKey.fromString(itemModelRaw);
            if (itemModel == null) {
                logger.warning("Skin '" + skinId + "' has an invalid " + fieldName + ".item_model: " + itemModelRaw);
            }
        }
        final CustomModelDataConfig cmd = parseCustomModelData(section.getConfigurationSection("custom_model_data"));
        final TokenVisualConfig visual = new TokenVisualConfig(itemModel, cmd);
        return visual.isEmpty() ? null : visual;
    }

    private String parsePerSkinColor(ConfigurationSection section, String key, String skinId, Logger logger) {
        final String raw = section.getString(key, null);
        if (raw == null) return null;
        if (isValidColorTag(raw)) return stripWrap(raw);
        logger.warning("Skin '" + skinId + "' has invalid " + key + ": '" + raw
                + "'. Use a MiniMessage color tag like '<gray>' or '<#FCBDE3>'. Falling back to global default.");
        return null;
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
