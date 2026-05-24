package com.cesarcosmico.switchskin.config.loader;

import com.cesarcosmico.switchskin.config.CustomModelDataConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.config.TokenVisualConfig;
import com.cesarcosmico.switchskin.config.TooltipDisplayConfig;
import com.cesarcosmico.switchskin.items.TooltipDisplaySupport;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Parses the {@code skins} section of skins.yml into {@link SkinDefinition}s,
 * doing deep per-entry validation via {@link SchemaKeys}. Both the flat
 * ({@code id: model:path}) and section skin formats are supported.
 */
public final class SkinConfigLoader {

    private static final String FILE = "skins.yml";

    private static final Set<String> SKIN_ALLOWED = Set.of(
            "item_model", "name", "lore", "icon-active", "icon-inactive",
            "bracket-color-active", "bracket-color-inactive", "tooltip_style",
            "custom_model_data", "tooltip_display", "token-skin", "token-tooltip", "applies-to");
    private static final Set<String> CMD_ALLOWED = Set.of("floats", "flags", "strings", "colors");
    private static final Set<String> TOOLTIP_DISPLAY_ALLOWED = Set.of("hide_tooltip", "hidden_components");
    private static final Set<String> TOKEN_VISUAL_ALLOWED = Set.of("item_model", "custom_model_data");

    private final Logger logger;

    public SkinConfigLoader(Logger logger) {
        this.logger = logger;
    }

    public Map<String, SkinDefinition> load(ConfigurationSection skinsSection) {
        final Map<String, SkinDefinition> map = new LinkedHashMap<>();
        if (skinsSection == null) return map;
        for (String id : skinsSection.getKeys(false)) {
            final SkinDefinition def = parseSkin(id, skinsSection);
            if (def != null) {
                map.put(id, def);
            }
        }
        return map;
    }

    private SkinDefinition parseSkin(String id, ConfigurationSection root) {
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
            SchemaKeys.checkKeys(logger, FILE, "skins." + id, section, SKIN_ALLOWED, Set.of());
            itemModelRaw = section.getString("item_model", "");
            name = section.getString("name", null);
            lore = section.isList("lore") ? section.getStringList("lore") : List.of();
            iconActive = section.getString("icon-active", null);
            iconInactive = section.getString("icon-inactive", null);
            bracketColorActive = parsePerSkinColor(section, "bracket-color-active", id);
            bracketColorInactive = parsePerSkinColor(section, "bracket-color-inactive", id);
            tooltipStyleRaw = section.getString("tooltip_style", null);
            customModelData = parseCustomModelData(id, section.getConfigurationSection("custom_model_data"));
            tooltipDisplay = parseTooltipDisplay(id, section.getConfigurationSection("tooltip_display"));
            tokenSkin = parseTokenVisual(id, "token-skin", section.getConfigurationSection("token-skin"));
            tokenTooltip = parseTokenVisual(id, "token-tooltip", section.getConfigurationSection("token-tooltip"));
            compatibleMaterials = parseAppliesTo(id, section.getStringList("applies-to"));
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

    private Set<Material> parseAppliesTo(String skinId, List<String> entries) {
        if (entries == null || entries.isEmpty()) return Set.of();
        final EnumSet<Material> resolved = EnumSet.noneOf(Material.class);
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) continue;
            final String trimmed = entry.trim();
            if (trimmed.startsWith("#")) {
                resolveTag(skinId, trimmed.substring(1), resolved);
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

    private void resolveTag(String skinId, String tagId, Set<Material> sink) {
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

    private CustomModelDataConfig parseCustomModelData(String skinId, ConfigurationSection section) {
        if (section == null) return null;
        SchemaKeys.checkKeys(logger, FILE, "skins." + skinId + ".custom_model_data", section, CMD_ALLOWED, Set.of());
        final CustomModelDataConfig parsed = new CustomModelDataConfig(
                section.getFloatList("floats"),
                section.getBooleanList("flags"),
                section.getStringList("strings"),
                section.getIntegerList("colors"));
        return parsed.isEmpty() ? null : parsed;
    }

    private TooltipDisplayConfig parseTooltipDisplay(String skinId, ConfigurationSection section) {
        if (section == null) return null;
        SchemaKeys.checkKeys(logger, FILE, "skins." + skinId + ".tooltip_display", section,
                TOOLTIP_DISPLAY_ALLOWED, Set.of());
        final TooltipDisplayConfig parsed = new TooltipDisplayConfig(
                section.getBoolean("hide_tooltip", false),
                section.getStringList("hidden_components"));
        for (String componentId : parsed.hiddenComponents()) {
            if (!TooltipDisplaySupport.knowsComponent(componentId)) {
                logger.warning("Skin '" + skinId
                        + "' references unknown component in tooltip_display.hidden_components: " + componentId);
            }
        }
        return parsed;
    }

    private TokenVisualConfig parseTokenVisual(String skinId, String fieldName, ConfigurationSection section) {
        if (section == null) return null;
        SchemaKeys.checkKeys(logger, FILE, "skins." + skinId + "." + fieldName, section,
                TOKEN_VISUAL_ALLOWED, Set.of());
        NamespacedKey itemModel = null;
        final String itemModelRaw = section.getString("item_model", "");
        if (!itemModelRaw.isEmpty()) {
            itemModel = NamespacedKey.fromString(itemModelRaw);
            if (itemModel == null) {
                logger.warning("Skin '" + skinId + "' has an invalid " + fieldName + ".item_model: " + itemModelRaw);
            }
        }
        final CustomModelDataConfig cmd = parseCustomModelData(skinId, section.getConfigurationSection("custom_model_data"));
        final TokenVisualConfig visual = new TokenVisualConfig(itemModel, cmd);
        return visual.isEmpty() ? null : visual;
    }

    private String parsePerSkinColor(ConfigurationSection section, String key, String skinId) {
        final String raw = section.getString(key, null);
        if (raw == null) return null;
        if (isValidColorTag(raw)) return stripWrap(raw);
        logger.warning("Skin '" + skinId + "' has invalid " + key + ": '" + raw
                + "'. Use a MiniMessage color tag like '<gray>' or '<#FCBDE3>'. Falling back to global default.");
        return null;
    }

    private static boolean isValidColorTag(String raw) {
        return raw != null && raw.length() >= 2
                && raw.charAt(0) == '<' && raw.charAt(raw.length() - 1) == '>';
    }

    private static String stripWrap(String wrapped) {
        return wrapped.substring(1, wrapped.length() - 1);
    }
}
